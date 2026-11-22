package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.data.db.AppDao
import com.example.data.db.CallLogDao
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.SpamRiskLevel
import com.example.util.CarrierLookupEngine
import com.example.util.SimManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class CallLogSyncResult(
    val insertedCount: Int = 0,
    val updatedCount: Int = 0,
    val permissionGranted: Boolean = false
) {
    val totalChanged: Int
        get() = insertedCount + updatedCount
}

class CallHistoryRepository(
    private val callLogDao: CallLogDao,
    private val appDao: AppDao,
    private val context: Context
) {
    val allCallLogs: Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()
    val callLogCount: Flow<Int> = callLogDao.getCallLogCount()
    val totalDurationSeconds: Flow<Long?> = callLogDao.getTotalDurationSeconds()

    fun getCallLogsByType(type: CallType): Flow<List<CallLogEntity>> =
        callLogDao.getCallLogsByType(type.name)

    fun getCallLogsForNumber(number: String): Flow<List<CallLogEntity>> =
        callLogDao.getCallLogsForNumber(number)

    fun searchCallLogs(query: String): Flow<List<CallLogEntity>> =
        callLogDao.searchCallLogs(query)

    suspend fun getLatestCallTimestamp(): Long? = withContext(Dispatchers.IO) {
        callLogDao.getLatestCallTimestamp()
    }

    suspend fun insertCallLog(entry: CallLogEntity): Long = withContext(Dispatchers.IO) {
        callLogDao.insertCallLog(entry)
    }

    suspend fun deleteCallLog(id: Long) = withContext(Dispatchers.IO) {
        callLogDao.deleteCallLog(id)
    }

    suspend fun clearAllCallLogs() = withContext(Dispatchers.IO) {
        callLogDao.clearAllCallLogs()
    }

    fun hasCallLogPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Queries Android's system CallLog.Calls.CONTENT_URI for phone number, timestamp,
     * duration, call type, cached name, and SIM slot account ID, then synchronizes new
     * and updated entries into the local Room call_logs table.
     */
    suspend fun syncWithSystemCallLog(limit: Int = 300): CallLogSyncResult = withContext(Dispatchers.IO) {
        if (!hasCallLogPermission()) {
            return@withContext CallLogSyncResult(permissionGranted = false)
        }

        try {
            val sims = SimManager.getAvailableSims(context)
            val existingLogs = callLogDao.getAllCallLogsSnapshot()

            val existingBySystemId = existingLogs
                .mapNotNull { log -> log.systemCallId?.let { it to log } }
                .toMap()
                .toMutableMap()

            val existingByKey = existingLogs.associateBy {
                normalizeDigits(it.number) to (it.timestamp / 1000L)
            }.toMutableMap()

            val contacts = appDao.getAllContactsSnapshot()
            val contactNameByDigits = contacts.associate {
                normalizeDigits(it.phoneNumber).takeLast(10) to it.name
            }

            val insertedLogs = mutableListOf<CallLogEntity>()
            var updatedCount = 0

            val projection = arrayOf(
                CallLog.Calls._ID,
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
                val idIdx = c.getColumnIndex(CallLog.Calls._ID)
                val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
                val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
                val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                var scanned = 0
                while (c.moveToNext() && scanned < limit) {
                    val rawNumber = if (numberIdx >= 0) c.getString(numberIdx).orEmpty().trim() else ""
                    if (rawNumber.isBlank()) continue
                    scanned++

                    val sysId = if (idIdx >= 0 && !c.isNull(idIdx)) c.getLong(idIdx) else null
                    val timestamp = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()
                    val duration = if (durationIdx >= 0) c.getLong(durationIdx).coerceAtLeast(0L) else 0L
                    val cachedNameRaw = if (nameIdx >= 0) c.getString(nameIdx).orEmpty().trim() else ""
                    val androidType = if (typeIdx >= 0) c.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                    val phoneAccountId = if (accountIdIdx >= 0) c.getString(accountIdIdx) else null

                    val dedupKey = normalizeDigits(rawNumber) to (timestamp / 1000L)
                    val existing = (sysId?.let { existingBySystemId[it] }) ?: existingByKey[dedupKey]

                    if (existing != null) {
                        val needsDurationUpdate = existing.durationSeconds == 0L && duration > 0L
                        val needsNameUpdate = existing.cachedName.isBlank() && cachedNameRaw.isNotBlank()
                        val needsSystemIdLink = existing.systemCallId == null && sysId != null
                        if (needsDurationUpdate || needsNameUpdate || needsSystemIdLink) {
                            val updated = existing.copy(
                                durationSeconds = if (needsDurationUpdate) duration else existing.durationSeconds,
                                cachedName = if (needsNameUpdate) cachedNameRaw else existing.cachedName,
                                systemCallId = existing.systemCallId ?: sysId
                            )
                            callLogDao.updateCallLog(updated)
                            sysId?.let { existingBySystemId[it] = updated }
                            existingByKey[dedupKey] = updated
                            updatedCount++
                        }
                        continue
                    }

                    val mappedType = mapSystemCallType(androidType)
                    val simSlot = SimManager.resolveSimSlotFromList(phoneAccountId, sims)
                    val carrierInfo = CarrierLookupEngine.lookup(rawNumber)
                    val spamReport = appDao.lookupSpamByNumber(rawNumber)
                        ?: normalizeDigits(rawNumber).takeIf { it.isNotEmpty() }
                            ?.let { appDao.lookupSpamByNumber(it) }

                    val resolvedName = cachedNameRaw.ifBlank {
                        contactNameByDigits[normalizeDigits(rawNumber).takeLast(10)]
                            ?: spamReport?.reportedName
                            ?: ""
                    }

                    val entity = CallLogEntity(
                        number = rawNumber,
                        cachedName = resolvedName,
                        type = mappedType,
                        timestamp = timestamp,
                        durationSeconds = duration,
                        carrier = carrierInfo.name,
                        location = if (carrierInfo.circle.isNotEmpty()) carrierInfo.circle else carrierInfo.country,
                        isSpam = spamReport != null && (spamReport.riskLevel == SpamRiskLevel.HIGH_RISK || spamReport.riskLevel == SpamRiskLevel.SUSPICIOUS),
                        spamCategory = spamReport?.category.orEmpty(),
                        simSlot = simSlot,
                        systemCallId = sysId
                    )
                    insertedLogs.add(entity)
                    sysId?.let { existingBySystemId[it] = entity }
                    existingByKey[dedupKey] = entity
                }
            }

            if (insertedLogs.isNotEmpty()) {
                callLogDao.insertCallLogs(insertedLogs)
            }

            CallLogSyncResult(
                insertedCount = insertedLogs.size,
                updatedCount = updatedCount,
                permissionGranted = true
            )
        } catch (_: SecurityException) {
            CallLogSyncResult(permissionGranted = false)
        } catch (_: Exception) {
            CallLogSyncResult(permissionGranted = true)
        }
    }

    /**
     * Emits whenever Android's system CallLog.Calls content provider changes
     * (when READ_CALL_LOG permission is granted).
     */
    fun observeSystemCallLogChanges(): Flow<Unit> = callbackFlow {
        if (!hasCallLogPermission()) {
            close()
            return@callbackFlow
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }

        try {
            context.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
        } catch (_: SecurityException) {
            close()
            return@callbackFlow
        }

        awaitClose {
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        fun normalizeDigits(number: String): String = number.filter { it.isDigit() }

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
