package com.example

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CallLog
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.CallLogDao
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.repository.CallHistoryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallHistoryRepositoryRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var callLogDao: CallLogDao
    private lateinit var repository: CallHistoryRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        callLogDao = database.callLogDao()
        repository = CallHistoryRepository(
            callLogDao = callLogDao,
            appDao = database.appDao(),
            context = context
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun callLogDaoStoresQueriesCountsAndSumsDuration() = runBlocking {
        val now = System.currentTimeMillis()
        callLogDao.insertCallLog(
            CallLogEntity(
                number = "+8801712345678",
                cachedName = "Sheikh Farid",
                type = CallType.INCOMING,
                timestamp = now - 10_000L,
                durationSeconds = 95L,
                simSlot = 1
            )
        )
        callLogDao.insertCallLog(
            CallLogEntity(
                number = "+8801911223344",
                cachedName = "Rubel Sheikh",
                type = CallType.OUTGOING,
                timestamp = now,
                durationSeconds = 180L,
                simSlot = 2
            )
        )

        assertEquals(2, repository.callLogCount.first())
        assertEquals(275L, repository.totalDurationSeconds.first())
        assertEquals(now, callLogDao.getLatestCallTimestamp())
        assertEquals(1, repository.getCallLogsForNumber("1712345678").first().size)
    }

    @Test
    fun syncWithSystemCallLogInsertsDeduplicatesAndUpdatesDuration() = runBlocking {
        val app = Shadows.shadowOf(context as Application)
        app.grantPermissions(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )

        val now = System.currentTimeMillis()
        var callDurationFromSystem = 0L
        var cachedNameFromSystem = ""

        val fakeProvider = object : ContentProvider() {
            override fun onCreate() = true
            override fun query(
                uri: Uri,
                projection: Array<out String>?,
                selection: String?,
                selectionArgs: Array<out String>?,
                sortOrder: String?
            ): Cursor {
                val cols = arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.PHONE_ACCOUNT_ID
                )
                return MatrixCursor(cols).apply {
                    addRow(
                        arrayOf(
                            101L,
                            "+8801711002233",
                            cachedNameFromSystem,
                            CallLog.Calls.OUTGOING_TYPE,
                            now - 30_000L,
                            callDurationFromSystem,
                            "sim2"
                        )
                    )
                }
            }

            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
        }
        fakeProvider.onCreate()
        ShadowContentResolver.registerProviderInternal(CallLog.AUTHORITY, fakeProvider)

        // First sync imports the row with 0s duration & systemCallId 101
        val firstSync = repository.syncWithSystemCallLog()
        assertTrue(firstSync.permissionGranted)
        assertEquals(1, firstSync.insertedCount)
        assertEquals(0, firstSync.updatedCount)

        val stored1 = callLogDao.findBySystemCallId(101L)
        assertNotNull(stored1)
        assertEquals("+8801711002233", stored1!!.number)
        assertEquals(0L, stored1.durationSeconds)
        assertEquals(2, stored1.simSlot)

        // System updates row 101 with finalized 142s duration and contact name
        callDurationFromSystem = 142L
        cachedNameFromSystem = "Finalized Contact"

        val secondSync = repository.syncWithSystemCallLog()
        assertEquals(0, secondSync.insertedCount)
        assertEquals(1, secondSync.updatedCount)

        val stored2 = callLogDao.findBySystemCallId(101L)
        assertNotNull(stored2)
        assertEquals(142L, stored2!!.durationSeconds)
        assertEquals("Finalized Contact", stored2.cachedName)
        assertEquals(1, callLogDao.getAllCallLogsSnapshot().size)
    }
}
