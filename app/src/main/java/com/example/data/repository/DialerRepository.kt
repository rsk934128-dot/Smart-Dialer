package com.example.data.repository

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import com.example.data.db.AppDao
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.model.SpamRiskLevel
import com.example.util.CarrierLookupEngine
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
    private val context: Context
) {

    val allContacts: Flow<List<ContactEntity>> = appDao.getAllContacts()
    val favoriteContacts: Flow<List<ContactEntity>> = appDao.getFavoriteContacts()
    val allCallLogs: Flow<List<CallLogEntity>> = appDao.getAllCallLogs()
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
        spamCategory: String = ""
    ) = withContext(Dispatchers.IO) {
        val entry = CallLogEntity(
            number = number,
            cachedName = cachedName,
            type = type,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            carrier = carrier,
            location = location,
            isSpam = isSpam,
            spamCategory = spamCategory
        )
        appDao.insertCallLog(entry)
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
        appDao.deleteCallLog(id)
    }

    suspend fun clearCallLogs() = withContext(Dispatchers.IO) {
        appDao.clearAllCallLogs()
    }

    // Attempt to sync device contacts if permission was granted
    suspend fun syncDeviceContactsIfAvailable() = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )
            cursor?.use {
                val list = mutableListOf<ContactEntity>()
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                var count = 0
                while (it.moveToNext() && count < 50) {
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val num = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    if (!num.isNullOrBlank()) {
                        list.add(
                            ContactEntity(
                                name = name,
                                phoneNumber = num,
                                label = "Device"
                            )
                        )
                        count++
                    }
                }
                if (list.isNotEmpty()) {
                    appDao.insertContacts(list)
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or running in test/sandbox environment
        } catch (_: Exception) {
            // Other provider errors
        }
    }
}
