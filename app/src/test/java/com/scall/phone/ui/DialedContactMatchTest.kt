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
    @Test fun threeDigitFragmentsFindAllMatchingContacts() {
        val contacts = listOf(
            contact("+91 72248 58148", name = "Neha"),
            contact("09300772234", name = "Cake"),
            contact("+91 93403 72252", name = "Gulshan"),
            contact("9876543210", name = "Other")
        )
        val names = findDialContactSuggestions("722", contacts).map { it.contact.name }
        assertEquals(listOf("Neha", "Cake", "Gulshan"), names)
    }

    @Test fun prefixMatchRanksAheadOfMiddleMatch() {
        val matches = findDialContactSuggestions("722", listOf(
            contact("9876722345", name = "Middle"),
            contact("+917224858148", name = "Prefix")
        ))
        assertEquals(listOf("Prefix", "Middle"), matches.map { it.contact.name })
    }

    @Test fun secondaryNumbersHaveCorrectNumberAndNoDuplicate() {
        val c = contact("9876543210", name = "Multi",
            others = listOf("9876543210", "+91 7224858148", "+91 7224858148"))
        val results = findDialContactSuggestions("722", listOf(c))
        assertEquals(1, results.size)
        assertEquals("+91 7224858148", results.first().number)
    }

    @Test fun tinyInputDoesNotShowUnrelatedSuggestions() {
        assertEquals(emptyList<DialContactSuggestion>(),
            findDialContactSuggestions("72", listOf(contact("7224858148"))))
        assertEquals(emptyList<DialContactSuggestion>(),
            findDialContactSuggestions("*#", listOf(contact("7224858148"))))
    }

    @Test fun resultLimitIsRespected() {
        val contacts = (1..50).map { n ->
            contact("722" + n.toString().padStart(7, '0'), name = "Contact $n")
        }
        assertEquals(4, findDialContactSuggestions("722", contacts, limit = 4).size)
    }

    @Test fun blankAndStarCodesNotMatched() {
        assertNull(findDialedContact("", listOf(contact("7224858148"))))
        assertNull(findDialedContact("*123#", listOf(contact("7224858148"))))
    }
}
