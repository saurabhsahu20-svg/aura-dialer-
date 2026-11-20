package com.scall.phone.ui

import com.scall.phone.data.ContactEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialedContactMatchTest {
    private fun contact(number: String, name: String = "Saved Person", others: List<String> = listOf(number)) =
        ContactEntry(3L, name, number, null, false, allNumbers = others)

    @Test fun exactTenDigitNumberDisplaysContact() {
        assertEquals("Saved Person", findDialedContact("7224858148", listOf(contact("7224858148")))?.name)
    }
    @Test fun indiaCountryCodeMatchesLocalNumberBothWays() {
        assertEquals("Saved Person", findDialedContact("+91 72248 58148",
            listOf(contact("7224858148")))?.name)
        assertEquals("Saved Person", findDialedContact("7224858148",
            listOf(contact("+91-72248-58148")))?.name)
    }
    @Test fun alternateNumbersOnContactAreRecognized() {
        assertEquals("Saved Person", findDialedContact("7224858148",
            listOf(contact("9876543210", others = listOf("9876543210", "+91 7224858148"))))?.name)
    }
    @Test fun partialNumberDoesNotFalselyClaimContact() {
        assertNull(findDialedContact("72248", listOf(contact("7224858148"))))
        assertNull(findDialedContact("24858148", listOf(contact("7224858148"))))
        assertNull(findDialedContact("7224858149", listOf(contact("7224858148"))))
    }
    @Test fun blankAndStarCodesNotMatched() {
        assertNull(findDialedContact("", listOf(contact("7224858148"))))
        assertNull(findDialedContact("*123#", listOf(contact("7224858148"))))
    }
}
