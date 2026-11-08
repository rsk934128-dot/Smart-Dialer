package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- Contacts ---
    @Query("SELECT * FROM contacts ORDER BY isFavorite DESC, name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE phoneNumber = :number OR phoneNumber LIKE '%' || :number || '%' OR :number LIKE '%' || phoneNumber || '%' LIMIT 1")
    suspend fun findContactByNumber(number: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    // --- Call Logs ---
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE type = :type ORDER BY timestamp DESC")
    fun getCallLogsByType(type: String): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs WHERE number LIKE '%' || :query || '%' OR cachedName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchCallLogs(query: String): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(logs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLog(id: Long)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllCallLogs()

    // --- Spam Registry ---
    @Query("SELECT * FROM spam_registry ORDER BY reportsCount DESC")
    fun getAllSpamReports(): Flow<List<SpamReportEntity>>

    @Query("SELECT * FROM spam_registry WHERE number = :number OR number LIKE '%' || :number || '%' OR :number LIKE '%' || number || '%' LIMIT 1")
    suspend fun lookupSpamByNumber(number: String): SpamReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamReport(report: SpamReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamReports(reports: List<SpamReportEntity>)

    // --- Blocked Numbers ---
    @Query("SELECT * FROM blocked_numbers ORDER BY blockedAt DESC")
    fun getAllBlockedNumbers(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_numbers WHERE number = :number OR number LIKE '%' || :number || '%' OR :number LIKE '%' || number || '%' LIMIT 1)")
    suspend fun isNumberBlocked(number: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun blockNumber(blocked: BlockedNumberEntity): Long

    @Query("DELETE FROM blocked_numbers WHERE number = :number")
    suspend fun unblockNumber(number: String)
}
