package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.model.CallType
import com.example.data.model.SpamRiskLevel

class Converters {
    @TypeConverter
    fun fromCallType(value: CallType): String = value.name

    @TypeConverter
    fun toCallType(value: String): CallType = try {
        CallType.valueOf(value)
    } catch (_: Exception) {
        CallType.INCOMING
    }

    @TypeConverter
    fun fromSpamRiskLevel(value: SpamRiskLevel): String = value.name

    @TypeConverter
    fun toSpamRiskLevel(value: String): SpamRiskLevel = try {
        SpamRiskLevel.valueOf(value)
    } catch (_: Exception) {
        SpamRiskLevel.SUSPICIOUS
    }
}
