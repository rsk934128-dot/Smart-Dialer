package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.db.AppDao
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.model.SpamRiskLevel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlin.math.absoluteValue

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing cloud sync.")
    }

    private fun userDocRef(userId: String) = db.collection("users").document(userId)

    private fun callLogsCollection(userId: String) =
        userDocRef(userId).collection("call_logs")

    private fun contactsCollection(userId: String) =
        userDocRef(userId).collection("contacts")

    private fun blockedNumbersCollection(userId: String) =
        userDocRef(userId).collection("blocked_numbers")

    private fun spamReportsCollection(userId: String) =
        userDocRef(userId).collection("spam_reports")

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return userDocRef(userId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else mapUserProfile(snapshot)
            }
    }

    fun observeCloudCallLogs(userId: String): Flow<List<CloudCallLog>> {
        return callLogsCollection(userId)
            .orderBy("callTimestampMs", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc -> mapCloudCallLog(doc) }
            }
    }

    fun observeCloudContacts(userId: String): Flow<List<CloudContact>> {
        return contactsCollection(userId)
            .orderBy("name", Query.Direction.ASCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc -> mapCloudContact(doc) }
            }
    }

    fun observeCloudBlockedNumbers(userId: String): Flow<List<CloudBlockedNumber>> {
        return blockedNumbersCollection(userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc -> mapCloudBlockedNumber(doc) }
            }
    }

    fun observeCloudSpamReports(userId: String): Flow<List<CloudSpamReport>> {
        return spamReportsCollection(userId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc -> mapCloudSpamReport(doc) }
            }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val uid = user.uid
        val path = "users/$uid"
        return try {
            val docRef = userDocRef(uid)
            val existing = docRef.get().await()
            val safeName = (user.displayName?.trim().takeIf { !it.isNullOrBlank() }
                ?: user.email?.substringBefore('@')?.trim()
                ?: "Smart Dialer User").take(80)
            val safeEmail = user.email?.trim()?.take(120).orEmpty()
            val safePhoto = user.photoUrl?.toString()?.trim()?.take(500).orEmpty()

            if (!existing.exists()) {
                val createPayload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "backedUpLogsCount" to 0,
                    "backedUpContactsCount" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, path, auth)
            Result.failure(e)
        }
    }

    suspend fun backupAllToCloud(
        contacts: List<ContactEntity>,
        callLogs: List<CallLogEntity>,
        blockedNumbers: List<BlockedNumberEntity>,
        spamReports: List<SpamReportEntity>
    ): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        var totalSynced = 0
        return try {
            val existingLogIds = callLogsCollection(uid).get().await().documents.map { it.id }.toHashSet()
            callLogs.take(500).chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { log ->
                    val numberTrimmed = log.number.trim().take(40)
                    if (numberTrimmed.isNotEmpty()) {
                        val docId = buildCallLogDocId(log)
                        val ref = callLogsCollection(uid).document(docId)
                        if (existingLogIds.contains(docId)) {
                            batch.update(
                                ref,
                                mapOf(
                                    "cachedName" to log.cachedName.trim().take(100),
                                    "durationSeconds" to log.durationSeconds.coerceAtLeast(0L),
                                    "spamFlag" to log.isSpam,
                                    "spamCategory" to log.spamCategory.trim().take(60),
                                    "simSlot" to log.simSlot.coerceIn(1, 2),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        } else {
                            batch.set(
                                ref,
                                mapOf(
                                    "userId" to uid,
                                    "number" to numberTrimmed,
                                    "cachedName" to log.cachedName.trim().take(100),
                                    "callType" to log.type.name,
                                    "callTimestampMs" to log.timestamp.coerceAtLeast(0L),
                                    "durationSeconds" to log.durationSeconds.coerceAtLeast(0L),
                                    "carrier" to log.carrier.trim().take(80),
                                    "location" to log.location.trim().take(80),
                                    "spamFlag" to log.isSpam,
                                    "spamCategory" to log.spamCategory.trim().take(60),
                                    "simSlot" to log.simSlot.coerceIn(1, 2),
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            existingLogIds.add(docId)
                        }
                        totalSynced++
                    }
                }
                batch.commit().await()
            }

            val existingContactIds = contactsCollection(uid).get().await().documents.map { it.id }.toHashSet()
            contacts.take(500).chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { contact ->
                    val nameTrimmed = contact.name.trim().take(100).ifBlank { "Unnamed" }
                    val phoneTrimmed = contact.phoneNumber.trim().take(40)
                    if (phoneTrimmed.isNotEmpty()) {
                        val docId = buildContactDocId(contact)
                        val ref = contactsCollection(uid).document(docId)
                        if (existingContactIds.contains(docId)) {
                            batch.update(
                                ref,
                                mapOf(
                                    "name" to nameTrimmed,
                                    "phoneNumber" to phoneTrimmed,
                                    "email" to contact.email.trim().take(120),
                                    "company" to contact.company.trim().take(100),
                                    "label" to contact.label.trim().take(40).ifBlank { "Mobile" },
                                    "favorite" to contact.isFavorite,
                                    "notes" to contact.notes.trim().take(300),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        } else {
                            batch.set(
                                ref,
                                mapOf(
                                    "userId" to uid,
                                    "name" to nameTrimmed,
                                    "phoneNumber" to phoneTrimmed,
                                    "email" to contact.email.trim().take(120),
                                    "company" to contact.company.trim().take(100),
                                    "label" to contact.label.trim().take(40).ifBlank { "Mobile" },
                                    "favorite" to contact.isFavorite,
                                    "notes" to contact.notes.trim().take(300),
                                    "avatarColorIndex" to contact.avatarColorIndex.coerceIn(0, 20),
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            existingContactIds.add(docId)
                        }
                        totalSynced++
                    }
                }
                batch.commit().await()
            }

            val existingBlockedIds = blockedNumbersCollection(uid).get().await().documents.map { it.id }.toHashSet()
            blockedNumbers.take(300).chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { blocked ->
                    val numTrimmed = blocked.number.trim().take(40)
                    if (numTrimmed.isNotEmpty()) {
                        val docId = buildBlockedDocId(blocked.number)
                        val ref = blockedNumbersCollection(uid).document(docId)
                        val safeReason = blocked.reason.trim().take(200).ifBlank { "Blocked by user" }
                        val safeName = blocked.name.trim().take(100)
                        if (existingBlockedIds.contains(docId)) {
                            batch.update(
                                ref,
                                mapOf(
                                    "name" to safeName,
                                    "reason" to safeReason,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        } else {
                            batch.set(
                                ref,
                                mapOf(
                                    "userId" to uid,
                                    "number" to numTrimmed,
                                    "name" to safeName,
                                    "reason" to safeReason,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            existingBlockedIds.add(docId)
                        }
                        totalSynced++
                    }
                }
                batch.commit().await()
            }

            val existingSpamIds = spamReportsCollection(uid).get().await().documents.map { it.id }.toHashSet()
            spamReports.take(300).chunked(350).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { report ->
                    val numTrimmed = report.number.trim().take(40)
                    if (numTrimmed.isNotEmpty()) {
                        val docId = buildSpamDocId(report.number)
                        val ref = spamReportsCollection(uid).document(docId)
                        val safeReportedName = report.reportedName.trim().take(100).ifBlank { "Unknown Caller" }
                        val safeCategory = report.category.trim().take(60).ifBlank { "Spam" }
                        val safeReportsCount = report.reportsCount.coerceAtLeast(1)
                        val safeNotes = report.notes.trim().take(300)
                        if (existingSpamIds.contains(docId)) {
                            batch.update(
                                ref,
                                mapOf(
                                    "reportedName" to safeReportedName,
                                    "category" to safeCategory,
                                    "reportsCount" to safeReportsCount,
                                    "riskLevel" to report.riskLevel.name,
                                    "notes" to safeNotes,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        } else {
                            batch.set(
                                ref,
                                mapOf(
                                    "userId" to uid,
                                    "number" to numTrimmed,
                                    "reportedName" to safeReportedName,
                                    "category" to safeCategory,
                                    "reportsCount" to safeReportsCount,
                                    "riskLevel" to report.riskLevel.name,
                                    "notes" to safeNotes,
                                    "createdAt" to FieldValue.serverTimestamp(),
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            existingSpamIds.add(docId)
                        }
                        totalSynced++
                    }
                }
                batch.commit().await()
            }

            // Update profile summary counts
            val profileRef = userDocRef(uid)
            if (profileRef.get().await().exists()) {
                profileRef.update(
                    mapOf(
                        "backedUpLogsCount" to existingLogIds.size,
                        "backedUpContactsCount" to existingContactIds.size,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(totalSynced)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.WRITE, "users/$uid/backup", auth)
            Result.failure(e)
        }
    }

    suspend fun restoreFromCloudToRoom(appDao: AppDao): Result<Int> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }

        return try {
            var restoredCount = 0

            val cloudContacts = contactsCollection(uid).get().await().documents.mapNotNull { mapCloudContact(it) }
            val existingContactDigits = appDao.getAllContactsSnapshot()
                .map { normalizeDigits(it.phoneNumber).takeLast(10) }
                .filter { it.isNotEmpty() }
                .toHashSet()
            val contactsToInsert = cloudContacts.filter { c ->
                val key = normalizeDigits(c.phoneNumber).takeLast(10)
                key.isNotEmpty() && existingContactDigits.add(key)
            }.map { c ->
                ContactEntity(
                    name = c.name,
                    phoneNumber = c.phoneNumber,
                    email = c.email,
                    company = c.company,
                    label = c.label,
                    isFavorite = c.favorite,
                    notes = c.notes,
                    avatarColorIndex = c.avatarColorIndex
                )
            }
            if (contactsToInsert.isNotEmpty()) {
                appDao.insertContacts(contactsToInsert)
                restoredCount += contactsToInsert.size
            }

            val cloudLogs = callLogsCollection(uid).get().await().documents.mapNotNull { mapCloudCallLog(it) }
            val existingLogKeys = appDao.getAllCallLogsSnapshot()
                .map { normalizeDigits(it.number) to (it.timestamp / 1000L) }
                .toHashSet()
            val logsToInsert = cloudLogs.filter { l ->
                val key = normalizeDigits(l.number) to (l.callTimestampMs / 1000L)
                existingLogKeys.add(key)
            }.map { l ->
                CallLogEntity(
                    number = l.number,
                    cachedName = l.cachedName,
                    type = runCatching { CallType.valueOf(l.callType) }.getOrDefault(CallType.INCOMING),
                    timestamp = l.callTimestampMs,
                    durationSeconds = l.durationSeconds,
                    carrier = l.carrier,
                    location = l.location,
                    isSpam = l.spamFlag,
                    spamCategory = l.spamCategory,
                    simSlot = l.simSlot.coerceIn(1, 2)
                )
            }
            if (logsToInsert.isNotEmpty()) {
                appDao.insertCallLogs(logsToInsert)
                restoredCount += logsToInsert.size
            }

            val cloudBlocked = blockedNumbersCollection(uid).get().await().documents.mapNotNull { mapCloudBlockedNumber(it) }
            cloudBlocked.forEach { b ->
                if (!appDao.isNumberBlocked(b.number)) {
                    appDao.blockNumber(
                        BlockedNumberEntity(
                            number = b.number,
                            name = b.name,
                            reason = b.reason
                        )
                    )
                    restoredCount++
                }
            }

            val cloudSpam = spamReportsCollection(uid).get().await().documents.mapNotNull { mapCloudSpamReport(it) }
            cloudSpam.forEach { s ->
                if (appDao.lookupSpamByNumber(s.number) == null) {
                    appDao.insertSpamReport(
                        SpamReportEntity(
                            number = s.number,
                            reportedName = s.reportedName,
                            category = s.category,
                            reportsCount = s.reportsCount,
                            riskLevel = runCatching { SpamRiskLevel.valueOf(s.riskLevel) }
                                .getOrDefault(SpamRiskLevel.SUSPICIOUS),
                            notes = s.notes
                        )
                    )
                    restoredCount++
                }
            }

            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            FirestoreErrorHandler.handleFirestoreError(e, OperationType.LIST, "users/$uid/restore", auth)
            Result.failure(e)
        }
    }

    private fun mapUserProfile(doc: DocumentSnapshot): UserProfile {
        val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: emptyMap()
        return UserProfile(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            displayName = (data["displayName"] as? String).orEmpty(),
            email = (data["email"] as? String).orEmpty(),
            photoUrl = (data["photoUrl"] as? String).orEmpty(),
            backedUpLogsCount = (data["backedUpLogsCount"] as? Number)?.toInt() ?: 0,
            backedUpContactsCount = (data["backedUpContactsCount"] as? Number)?.toInt() ?: 0,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun mapCloudCallLog(doc: DocumentSnapshot): CloudCallLog? {
        val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
        return CloudCallLog(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            number = (data["number"] as? String).orEmpty(),
            cachedName = (data["cachedName"] as? String).orEmpty(),
            callType = (data["callType"] as? String) ?: "INCOMING",
            callTimestampMs = (data["callTimestampMs"] as? Number)?.toLong() ?: 0L,
            durationSeconds = (data["durationSeconds"] as? Number)?.toLong() ?: 0L,
            carrier = (data["carrier"] as? String).orEmpty(),
            location = (data["location"] as? String).orEmpty(),
            spamFlag = (data["spamFlag"] as? Boolean) ?: false,
            spamCategory = (data["spamCategory"] as? String).orEmpty(),
            simSlot = (data["simSlot"] as? Number)?.toInt()?.coerceIn(1, 2) ?: 1,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun mapCloudContact(doc: DocumentSnapshot): CloudContact? {
        val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
        return CloudContact(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            name = (data["name"] as? String).orEmpty(),
            phoneNumber = (data["phoneNumber"] as? String).orEmpty(),
            email = (data["email"] as? String).orEmpty(),
            company = (data["company"] as? String).orEmpty(),
            label = (data["label"] as? String) ?: "Mobile",
            favorite = (data["favorite"] as? Boolean) ?: false,
            notes = (data["notes"] as? String).orEmpty(),
            avatarColorIndex = (data["avatarColorIndex"] as? Number)?.toInt() ?: 0,
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun mapCloudBlockedNumber(doc: DocumentSnapshot): CloudBlockedNumber? {
        val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
        return CloudBlockedNumber(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            number = (data["number"] as? String).orEmpty(),
            name = (data["name"] as? String).orEmpty(),
            reason = (data["reason"] as? String) ?: "Blocked by user",
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    private fun mapCloudSpamReport(doc: DocumentSnapshot): CloudSpamReport? {
        val data = doc.getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: return null
        return CloudSpamReport(
            id = doc.id,
            userId = (data["userId"] as? String).orEmpty(),
            number = (data["number"] as? String).orEmpty(),
            reportedName = (data["reportedName"] as? String).orEmpty(),
            category = (data["category"] as? String).orEmpty(),
            reportsCount = (data["reportsCount"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 1,
            riskLevel = (data["riskLevel"] as? String) ?: "SUSPICIOUS",
            notes = (data["notes"] as? String).orEmpty(),
            createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    companion object {
        private fun normalizeDigits(raw: String): String = raw.filter { it.isDigit() }

        fun buildCallLogDocId(log: CallLogEntity): String {
            val digits = normalizeDigits(log.number).takeLast(12).ifEmpty {
                log.number.hashCode().absoluteValue.toString()
            }
            return "log_${log.timestamp}_${digits}".take(120)
        }

        fun buildContactDocId(contact: ContactEntity): String {
            val digits = normalizeDigits(contact.phoneNumber).takeLast(12).ifEmpty { "num" }
            val nameHash = contact.name.trim().lowercase().hashCode().absoluteValue
            return "cnt_${digits}_${nameHash}".take(120)
        }

        fun buildBlockedDocId(number: String): String {
            val digits = normalizeDigits(number).takeLast(15).ifEmpty {
                number.hashCode().absoluteValue.toString()
            }
            return "blk_${digits}".take(120)
        }

        fun buildSpamDocId(number: String): String {
            val digits = normalizeDigits(number).takeLast(15).ifEmpty {
                number.hashCode().absoluteValue.toString()
            }
            return "spm_${digits}".take(120)
        }
    }
}
