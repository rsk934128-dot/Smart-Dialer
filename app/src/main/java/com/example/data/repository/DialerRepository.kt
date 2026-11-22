package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.db.AppDao
import com.example.data.db.CallLogDao
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.model.SpamRiskLevel
import com.example.util.CarrierLookupEngine
import com.example.util.SimManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class NumberLookupResult(
    val number: String,
    val displayName: String,
    val isKnownContact: Boolean,
    val contact: ContactEntity?,
    val isSpam: Boolean,
    val spamReport: SpamReportEntity?,
    val isBlocked: Boolean,
    val carrierName: String,
    val location: String,
    val riskLevel: SpamRiskLevel
)

class DialerRepository(
    private val appDao: AppDao,
    private val context: Context,
    private val callLogDao: CallLogDao? = null
) {

    val callHistoryRepository: CallHistoryRepository? =
        callLogDao?.let { CallHistoryRepository(it, appDao, context) }

    val allContacts: Flow<List<ContactEntity>> = appDao.getAllContacts()
    val favoriteContacts: Flow<List<ContactEntity>> = appDao.getFavoriteContacts()
    val allCallLogs: Flow<List<CallLogEntity>> = callLogDao?.getAllCallLogs() ?: appDao.getAllCallLogs()
    val allSpamReports: Flow<List<SpamReportEntity>> = appDao.getAllSpamReports()
    val blockedNumbers: Flow<List<BlockedNumberEntity>> = appDao.getAllBlockedNumbers()

    suspend fun lookupNumber(rawNumber: String): NumberLookupResult = withContext(Dispatchers.IO) {
        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")
        val carrier = CarrierLookupEngine.lookup(rawNumber)

        val isBlocked = appDao.isNumberBlocked(rawNumber) ||
            (cleanNumber.isNotEmpty() && appDao.isNumberBlocked(cleanNumber))

        val contact = appDao.findContactByNumber(rawNumber)
            ?: (if (cleanNumber.isNotEmpty()) appDao.findContactByNumber(cleanNumber) else null)
        val spam = appDao.lookupSpamByNumber(rawNumber)
            ?: (if (cleanNumber.isNotEmpty()) appDao.lookupSpamByNumber(cleanNumber) else null)

        val displayName = when {
            contact != null -> contact.name
            spam != null -> spam.reportedName
            else -> CarrierLookupEngine.formatDisplayNumber(rawNumber)
        }

        val riskLevel = when {
            spam != null -> spam.riskLevel
            isBlocked -> SpamRiskLevel.SUSPICIOUS
            else -> SpamRiskLevel.SAFE
        }

        NumberLookupResult(
            number = rawNumber,
            displayName = displayName,
            isKnownContact = contact != null,
            contact = contact,
            isSpam = spam != null && (spam.riskLevel == SpamRiskLevel.HIGH_RISK || spam.riskLevel == SpamRiskLevel.SUSPICIOUS),
            spamReport = spam,
            isBlocked = isBlocked,
            carrierName = carrier.name,
            location = if (carrier.circle.isNotEmpty()) carrier.circle else carrier.country,
            riskLevel = riskLevel
        )
    }

    suspend fun addContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        appDao.insertContact(contact)
    }

    suspend fun updateContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        appDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        appDao.deleteContact(contact)
    }

    suspend fun logCall(
        number: String,
        cachedName: String,
        type: CallType,
        durationSeconds: Long = 0,
        carrier: String = "",
        location: String = "",
        isSpam: Boolean = false,
        spamCategory: String = "",
        simSlot: Int = 1,
        timestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val entry = CallLogEntity(
            number = number,
            cachedName = cachedName,
            type = type,
            timestamp = timestamp,
            durationSeconds = durationSeconds,
            carrier = carrier,
            location = location,
            isSpam = isSpam,
            spamCategory = spamCategory,
            simSlot = simSlot.coerceIn(1, 2)
        )
        if (callHistoryRepository != null) {
            callHistoryRepository.insertCallLog(entry)
        } else {
            appDao.insertCallLog(entry)
        }
    }

    suspend fun blockNumber(number: String, name: String = "", reason: String = "Blocked by user") =
        withContext(Dispatchers.IO) {
            appDao.blockNumber(
                BlockedNumberEntity(
                    number = number,
                    name = name,
                    reason = reason
                )
            )
        }

    suspend fun unblockNumber(number: String) = withContext(Dispatchers.IO) {
        appDao.unblockNumber(number)
    }

    suspend fun reportSpam(
        number: String,
        name: String,
        category: String,
        notes: String = "",
        autoBlock: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val existing = appDao.lookupSpamByNumber(number)
        val reports = (existing?.reportsCount ?: 0) + 1
        val report = SpamReportEntity(
            number = number,
            reportedName = name,
            category = category,
            reportsCount = reports,
            riskLevel = SpamRiskLevel.HIGH_RISK,
            isBlockedByUser = autoBlock,
            notes = notes
        )
        appDao.insertSpamReport(report)
        if (autoBlock) {
            appDao.blockNumber(
                BlockedNumberEntity(
                    number = number,
                    name = name,
                    reason = "Reported as $category"
                )
            )
        }
    }

    suspend fun deleteCallLog(id: Long) = withContext(Dispatchers.IO) {
        if (callHistoryRepository != null) {
            callHistoryRepository.deleteCallLog(id)
        } else {
            appDao.deleteCallLog(id)
        }
    }

    suspend fun clearCallLogs() = withContext(Dispatchers.IO) {
        if (callHistoryRepository != null) {
            callHistoryRepository.clearAllCallLogs()
        } else {
            appDao.clearAllCallLogs()
        }
    }

    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Reads real device call history from Android's CallLog.Calls.CONTENT_URI,
     * resolves SIM 1 / SIM 2 from PHONE_ACCOUNT_ID, enriches carrier & spam status,
     * deduplicates against existing records, and backs them up into Room.
     *
     * @return Number of newly backed-up call logs imported.
     */
    suspend fun syncDeviceCallLogsIfAvailable(limit: Int = 300): Int = withContext(Dispatchers.IO) {
        if (callHistoryRepository != null) {
            return@withContext callHistoryRepository.syncWithSystemCallLog(limit).totalChanged
        }
        if (!hasCallLogPermission()) return@withContext 0
        try {
            val sims = SimManager.getAvailableSims(context)
            val existingLogs = appDao.getAllCallLogsSnapshot()
            val existingKeys = existingLogs.map {
                normalizeDigits(it.number) to (it.timestamp / 1000L)
            }.toHashSet()

            val contacts = appDao.getAllContactsSnapshot()
            val contactNameByDigits = contacts.associate {
                normalizeDigits(it.phoneNumber).takeLast(10) to it.name
            }

            val importedLogs = mutableListOf<CallLogEntity>()
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use { c ->
                val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
                val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                var count = 0
                while (c.moveToNext() && count < limit) {
                    val rawNumber = if (numberIdx >= 0) c.getString(numberIdx).orEmpty().trim() else ""
                    if (rawNumber.isBlank()) continue

                    val timestamp = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                    val dedupKey = normalizeDigits(rawNumber) to (timestamp / 1000L)
                    if (!existingKeys.add(dedupKey)) {
                        continue
                    }

                    val cachedNameRaw = if (nameIdx >= 0) c.getString(nameIdx).orEmpty().trim() else ""
                    val androidType = if (typeIdx >= 0) c.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val duration = if (durationIdx >= 0) c.getLong(durationIdx).coerceAtLeast(0L) else 0L
                    val phoneAccountId = if (accountIdIdx >= 0) c.getString(accountIdIdx) else null

                    val mappedType = mapSystemCallType(androidType)
                    val simSlot = SimManager.resolveSimSlotFromList(phoneAccountId, sims)
                    val carrierInfo = CarrierLookupEngine.lookup(rawNumber)
                    val spamReport = appDao.lookupSpamByNumber(rawNumber)
                        ?: normalizeDigits(rawNumber).takeIf { it.isNotEmpty() }?.let { appDao.lookupSpamByNumber(it) }

                    val resolvedName = cachedNameRaw.ifBlank {
                        contactNameByDigits[normalizeDigits(rawNumber).takeLast(10)]
                            ?: spamReport?.reportedName
                            ?: ""
                    }

                    importedLogs.add(
                        CallLogEntity(
                            number = rawNumber,
                            cachedName = resolvedName,
                            type = mappedType,
                            timestamp = timestamp,
                            durationSeconds = duration,
                            carrier = carrierInfo.name,
                            location = if (carrierInfo.circle.isNotEmpty()) carrierInfo.circle else carrierInfo.country,
                            isSpam = spamReport != null && (spamReport.riskLevel == SpamRiskLevel.HIGH_RISK || spamReport.riskLevel == SpamRiskLevel.SUSPICIOUS),
                            spamCategory = spamReport?.category.orEmpty(),
                            simSlot = simSlot
                        )
                    )
                    count++
                }
            }

            if (importedLogs.isNotEmpty()) {
                appDao.insertCallLogs(importedLogs)
            }
            importedLogs.size
        } catch (_: SecurityException) {
            0
        } catch (_: Exception) {
            0
        }
    }

    // Attempt to sync device contacts if permission was granted (with deduplication)
    suspend fun syncDeviceContactsIfAvailable(limit: Int = 500): Int = withContext(Dispatchers.IO) {
        if (!hasContactsPermission()) return@withContext 0
        try {
            val existingNumbers = appDao.getAllContactsSnapshot()
                .map { normalizeDigits(it.phoneNumber).takeLast(10) }
                .filter { it.isNotEmpty() }
                .toHashSet()

            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            var insertedCount = 0
            cursor?.use {
                val list = mutableListOf<ContactEntity>()
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && list.size < limit) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex).orEmpty().ifBlank { "Unknown" } else "Unknown"
                    val num = if (numberIndex >= 0) it.getString(numberIndex).orEmpty().trim() else ""
                    val key = normalizeDigits(num).takeLast(10)
                    if (num.isNotBlank() && key.isNotEmpty() && existingNumbers.add(key)) {
                        list.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = num,
                                label = "Device",
                                avatarColorIndex = (list.size % 8)
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    appDao.insertContacts(list)
                    insertedCount = list.size
                }
            }
            insertedCount
        } catch (_: SecurityException) {
            0
        } catch (_: Exception) {
            0
        }
    }

    companion object {
        fun normalizeDigits(number: String): String {
            return number.filter { it.isDigit() }
        }

        fun mapSystemCallType(systemType: Int): CallType {
            return when (systemType) {
                CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
                else -> CallType.INCOMING
            }
        }
    }
}
