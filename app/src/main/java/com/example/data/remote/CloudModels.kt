package com.example.data.remote

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val backedUpLogsCount: Int = 0,
    val backedUpContactsCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudCallLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val number: String = "",
    val cachedName: String = "",
    val callType: String = "INCOMING",
    val callTimestampMs: Long = 0L,
    val durationSeconds: Long = 0L,
    val carrier: String = "",
    val location: String = "",
    val spamFlag: Boolean = false,
    val spamCategory: String = "",
    val simSlot: Int = 1,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudContact(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val company: String = "",
    val label: String = "Mobile",
    val favorite: Boolean = false,
    val notes: String = "",
    val avatarColorIndex: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudBlockedNumber(
    @DocumentId val id: String = "",
    val userId: String = "",
    val number: String = "",
    val name: String = "",
    val reason: String = "Blocked by user",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSpamReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val number: String = "",
    val reportedName: String = "",
    val category: String = "",
    val reportsCount: Int = 1,
    val riskLevel: String = "SUSPICIOUS",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class CloudBackupSummary(
    val profile: UserProfile? = null,
    val callLogs: List<CloudCallLog> = emptyList(),
    val contacts: List<CloudContact> = emptyList(),
    val blockedNumbers: List<CloudBlockedNumber> = emptyList(),
    val spamReports: List<CloudSpamReport> = emptyList()
)
