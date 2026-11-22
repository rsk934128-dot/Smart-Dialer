package com.example

import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.remote.CloudBlockedNumber
import com.example.data.remote.CloudCallLog
import com.example.data.remote.CloudContact
import com.example.data.remote.CloudSpamReport
import com.example.data.remote.CloudSyncRepository
import com.example.data.remote.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncModelAndIdTest {

    private val validIdRegex = Regex("^[a-zA-Z0-9_\\-]{1,128}$")

    @Test
    fun deterministicFirestoreDocumentIdsMatchRulesRegex() {
        val logId1 = CloudSyncRepository.buildCallLogDocId(
            CallLogEntity(
                number = "+880 1712-345678",
                type = CallType.INCOMING,
                timestamp = 1732280000123L
            )
        )
        val logId2 = CloudSyncRepository.buildCallLogDocId(
            CallLogEntity(
                number = "*121#",
                type = CallType.OUTGOING,
                timestamp = 1732280000456L
            )
        )
        val contactId = CloudSyncRepository.buildContactDocId(
            ContactEntity(
                name = "Sheikh Farid (Tech Hero)",
                phoneNumber = "+880 1911-223344"
            )
        )
        val blockedId = CloudSyncRepository.buildBlockedDocId("+880-1899-001122")
        val spamId = CloudSyncRepository.buildSpamDocId("+880 1899 001122")

        listOf(logId1, logId2, contactId, blockedId, spamId).forEach { id ->
            assertTrue("ID '$id' must match firestore.rules isValidId regex", validIdRegex.matches(id))
        }
    }

    @Test
    fun cloudModelsHaveNoArgDefaultsAndMatchingFieldNames() {
        val profile = UserProfile()
        val log = CloudCallLog()
        val contact = CloudContact()
        val blocked = CloudBlockedNumber()
        val spam = CloudSpamReport()

        assertEquals("", profile.id)
        assertEquals(false, log.spamFlag)
        assertEquals(1, log.simSlot)
        assertEquals(false, contact.favorite)
        assertEquals("Blocked by user", blocked.reason)
        assertEquals(1, spam.reportsCount)
    }
}
