package com.example

import com.example.data.model.ContactEntity
import com.example.util.CarrierLookupEngine
import com.example.util.T9SearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBangladeshCarrierPrefixes() {
        val gp1 = CarrierLookupEngine.lookup("01712345678")
        assertEquals("Grameenphone", gp1.name)
        assertEquals("Bangladesh", gp1.country)

        val gp2 = CarrierLookupEngine.lookup("+8801301998877")
        assertEquals("Grameenphone", gp2.name)

        val bl1 = CarrierLookupEngine.lookup("01911223344")
        assertEquals("Banglalink", bl1.name)

        val bl2 = CarrierLookupEngine.lookup("01400112233")
        assertEquals("Banglalink", bl2.name)

        val robi = CarrierLookupEngine.lookup("01819876543")
        assertEquals("Robi Axiata", robi.name)

        val airtel = CarrierLookupEngine.lookup("01622445566")
        assertEquals("Airtel Bangladesh", airtel.name)

        val teletalk = CarrierLookupEngine.lookup("01552334455")
        assertEquals("Teletalk", teletalk.name)

        val landline = CarrierLookupEngine.lookup("029876543")
        assertEquals("BTCL Landline", landline.name)
    }

    @Test
    fun testInternationalCarrierPrefixes() {
        val usa = CarrierLookupEngine.lookup("+1 415 555 0199")
        assertEquals("North America Telecom", usa.name)

        val uk = CarrierLookupEngine.lookup("+44 7911 123456")
        assertEquals("UK Mobile / Landline", uk.name)

        val india = CarrierLookupEngine.lookup("+91 98765 43210")
        assertEquals("Indian Telecom", india.name)

        val uae = CarrierLookupEngine.lookup("+971 50 123 4567")
        assertEquals("Etisalat / du", uae.name)

        val ksa = CarrierLookupEngine.lookup("+966 50 123 4567")
        assertEquals("STC / Mobily", ksa.name)
    }

    @Test
    fun testPhoneNumberFormatting() {
        val formattedBd11 = CarrierLookupEngine.formatDisplayNumber("01712345678")
        assertEquals("01712-345678", formattedBd11)

        val formattedIntl = CarrierLookupEngine.formatDisplayNumber("+8801712345678")
        assertEquals("+880 171-234-5678", formattedIntl)
    }

    @Test
    fun testT9KeypadSearchEngine() {
        val contacts = listOf(
            ContactEntity(name = "Sheikh Farid", phoneNumber = "01712345678"),
            ContactEntity(name = "Rubel Sheikh", phoneNumber = "01911223344"),
            ContactEntity(name = "Sadia Islam", phoneNumber = "01301998877"),
            ContactEntity(name = "Dr. Nusrat Jahan", phoneNumber = "01622445566")
        )

        // "327" -> "FAR" (F=3, A=2, R=7) matches Sheikh Farid
        val faridMatches = T9SearchEngine.filterContacts("327", contacts)
        assertTrue(faridMatches.any { it.contact.name == "Sheikh Farid" })

        // "782" -> "RUB" (R=7, U=8, B=2) matches Rubel Sheikh
        val rubelMatches = T9SearchEngine.filterContacts("782", contacts)
        assertTrue(rubelMatches.any { it.contact.name == "Rubel Sheikh" })

        // "687" -> "NUS" (N=6, U=8, S=7) matches Dr. Nusrat Jahan
        val nusratMatches = T9SearchEngine.filterContacts("687", contacts)
        assertTrue(nusratMatches.any { it.contact.name == "Dr. Nusrat Jahan" })

        // Direct phone number search
        val numMatches = T9SearchEngine.filterContacts("1911", contacts)
        assertEquals(1, numMatches.size)
        assertEquals("Rubel Sheikh", numMatches[0].contact.name)

        // Nonexistent query returns empty
        val none = T9SearchEngine.filterContacts("99999", contacts)
        assertTrue(none.isEmpty())
    }
}
