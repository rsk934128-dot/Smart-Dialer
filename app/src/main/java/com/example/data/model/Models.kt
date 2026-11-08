package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    BLOCKED
}

enum class SpamRiskLevel {
    SAFE,
    SUSPICIOUS,
    HIGH_RISK
}

data class CarrierInfo(
    val name: String,
    val country: String,
    val countryCode: String,
    val circle: String = "",
    val networkType: String = "4G/5G",
    val primaryColorHex: Long = 0xFF2563EB
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val email: String = "",
    val company: String = "",
    val label: String = "Mobile",
    val isFavorite: Boolean = false,
    val notes: String = "",
    val avatarColorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val cachedName: String = "",
    val type: CallType,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val carrier: String = "",
    val location: String = "",
    val isSpam: Boolean = false,
    val spamCategory: String = "",
    val simSlot: Int = 1
)

@Entity(tableName = "spam_registry")
data class SpamReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val reportedName: String,
    val category: String, // e.g., Telemarketer, Scammer, Loan Fraud, Robocall, Fake Courier
    val reportsCount: Int = 1,
    val riskLevel: SpamRiskLevel = SpamRiskLevel.SUSPICIOUS,
    val country: String = "Bangladesh",
    val isBlockedByUser: Boolean = false,
    val notes: String = ""
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val number: String,
    val name: String = "",
    val reason: String = "User Blocked",
    val blockedAt: Long = System.currentTimeMillis()
)
