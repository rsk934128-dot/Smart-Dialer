package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDao
import com.example.data.db.AppDatabase
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.model.SpamRiskLevel
import com.example.data.repository.DialerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CallLogAndSpamRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: AppDao
    private lateinit var repository: DialerRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.appDao()
        repository = DialerRepository(dao, context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testLogCallAndRetrieveCallLogs() = runBlocking {
        repository.logCall(
            number = "+880 1712-345678",
            cachedName = "Sheikh Farid",
            type = CallType.INCOMING,
            durationSeconds = 120,
            carrier = "Grameenphone",
            location = "Dhaka, BD",
            isSpam = false
        )

        repository.logCall(
            number = "+880 1899-001122",
            cachedName = "Fake Bank KYC",
            type = CallType.BLOCKED,
            durationSeconds = 0,
            carrier = "Robi Axiata",
            location = "National, BD",
            isSpam = true,
            spamCategory = "Scammer"
        )

        val logs = repository.allCallLogs.first()
        assertEquals(2, logs.size)

        val firstLog = logs[0]
        assertEquals("+880 1899-001122", firstLog.number)
        assertEquals(CallType.BLOCKED, firstLog.type)
        assertTrue(firstLog.isSpam)
        assertEquals("Scammer", firstLog.spamCategory)

        val secondLog = logs[1]
        assertEquals("+880 1712-345678", secondLog.number)
        assertEquals(CallType.INCOMING, secondLog.type)
        assertEquals(120L, secondLog.durationSeconds)
        assertFalse(secondLog.isSpam)
    }

    @Test
    fun testSpamLookupAndAutoBlock() = runBlocking {
        // Pre-insert a known scammer in the registry
        dao.insertSpamReport(
            SpamReportEntity(
                number = "+880 1899-001122",
                reportedName = "Fake Bank KYC Agent",
                category = "Scammer",
                reportsCount = 1420,
                riskLevel = SpamRiskLevel.HIGH_RISK
            )
        )

        // Lookup before blocking
        val lookup1 = repository.lookupNumber("+880 1899-001122")
        assertTrue(lookup1.isSpam)
        assertEquals("Fake Bank KYC Agent", lookup1.displayName)
        assertEquals(SpamRiskLevel.HIGH_RISK, lookup1.riskLevel)

        // Now report and auto-block
        repository.reportSpam(
            number = "+880 1600-443322",
            name = "Aggressive Telemarketer",
            category = "Telemarketer",
            notes = "Unsolicited promotional calls",
            autoBlock = true
        )

        val lookupBlocked = repository.lookupNumber("+880 1600-443322")
        assertTrue(lookupBlocked.isSpam)
        assertTrue(lookupBlocked.isBlocked)
        assertEquals("Aggressive Telemarketer", lookupBlocked.displayName)

        val blockedList = repository.blockedNumbers.first()
        assertEquals(1, blockedList.size)
        assertEquals("+880 1600-443322", blockedList[0].number)
    }

    @Test
    fun testBlockAndUnblockNumber() = runBlocking {
        val testNumber = "+880 1999-887766"

        assertFalse(dao.isNumberBlocked(testNumber))

        repository.blockNumber(testNumber, name = "Nuisance Caller", reason = "Harassment")
        assertTrue(dao.isNumberBlocked(testNumber))

        val blockedEntries = repository.blockedNumbers.first()
        assertEquals(1, blockedEntries.size)
        assertEquals(testNumber, blockedEntries[0].number)

        repository.unblockNumber(testNumber)
        assertFalse(dao.isNumberBlocked(testNumber))
        assertTrue(repository.blockedNumbers.first().isEmpty())
    }

    @Test
    fun testClearAllCallLogs() = runBlocking {
        repository.logCall("01711111111", "User A", CallType.OUTGOING, 45)
        repository.logCall("01822222222", "User B", CallType.MISSED, 0)

        assertEquals(2, repository.allCallLogs.first().size)

        repository.clearCallLogs()
        assertEquals(0, repository.allCallLogs.first().size)
    }

    @Test
    fun testContactCreationAndNumberLookup() = runBlocking {
        val contact = ContactEntity(
            name = "Sheikh Farid",
            phoneNumber = "+880 1712-345678",
            email = "farid.dev@smartdialer.io",
            company = "NoorAI Systems",
            label = "Work"
        )
        repository.addContact(contact)

        val lookup = repository.lookupNumber("+880 1712-345678")
        assertTrue(lookup.isKnownContact)
        assertEquals("Sheikh Farid", lookup.displayName)
        assertFalse(lookup.isSpam)
        assertEquals(SpamRiskLevel.SAFE, lookup.riskLevel)
        assertEquals("Grameenphone", lookup.carrierName)
    }

    @Test
    fun testDefaultDialerManager() {
        val isDefault = com.example.util.DefaultDialerManager.isDefaultDialer(context)
        // In Robolectric environment, default is false
        assertFalse(isDefault)

        val intent = com.example.util.DefaultDialerManager.createDefaultDialerIntent(context)
        assertNotNull(intent)
        assertNotNull(intent.action)

        val settingsIntent = com.example.util.DefaultDialerManager.createDefaultAppsSettingsIntent(context)
        assertNotNull(settingsIntent)
        assertNotNull(settingsIntent.action)

        val overlayIntent = com.example.util.DefaultDialerManager.createOverlayPermissionIntent(context)
        assertNotNull(overlayIntent)
        assertEquals(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION, overlayIntent.action)
        assertEquals("package:${context.packageName}", overlayIntent.dataString)
    }

    @Test
    fun testCallStateAnsweredAndDuration() {
        // Initial state
        val state = com.example.ui.viewmodel.ActiveCallState(
            isActive = true,
            isIncomingRinging = false,
            isCallAnswered = false,
            durationSeconds = 0
        )
        assertFalse(state.isCallAnswered)
        assertEquals(0L, state.durationSeconds)

        // After call is answered/received:
        val answeredState = state.copy(
            isCallAnswered = true,
            durationSeconds = 12
        )
        assertTrue(answeredState.isCallAnswered)
        assertEquals(12L, answeredState.durationSeconds)

        // Test CallStateManager event emission
        com.example.service.CallStateManager.emit(
            com.example.service.CallStateEvent.CallAnswered
        )
    }
}
