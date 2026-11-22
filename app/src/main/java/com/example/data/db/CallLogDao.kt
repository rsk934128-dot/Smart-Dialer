package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogsSnapshot(): List<CallLogEntity>

    @Query("SELECT * FROM call_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getCallLogsByType(type: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE number LIKE '%' || :number || '%' ORDER BY timestamp DESC")
    fun getCallLogsForNumber(number: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE number LIKE '%' || :query || '%' OR cachedName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchCallLogs(query: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE systemCallId = :systemCallId LIMIT 1")
    suspend fun findBySystemCallId(systemCallId: Long): CallLogEntity?

    @Query("SELECT MAX(timestamp) FROM call_logs")
    suspend fun getLatestCallTimestamp(): Long?

    @Query("SELECT COUNT(*) FROM call_logs")
    fun getCallLogCount(): Flow<Int>

    @Query("SELECT SUM(durationSeconds) FROM call_logs")
    fun getTotalDurationSeconds(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(logs: List<CallLogEntity>)

    @Update
    suspend fun updateCallLog(log: CallLogEntity)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()
}
