package com.scall.phone.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactSearchTest {
    @Test fun caseInsensitiveNameMatches() {
        assertTrue(matchesContactSearch("Saurabh Sahu", "+91 98765-43210", "sAhU"))
    }

    @Test fun formattedPhoneMatchesTypedDigits() {
        assertTrue(matchesContactSearch("Amit", "+91 98765-43210", "9876543210"))
        assertTrue(matchesContactSearch("Amit", "+91 (98765) 43210", "98765"))
    }

    @Test fun blankQueryShowsAllContacts() {
        assertTrue(matchesContactSearch("Amit", "9876543210", "  "))
    }

    @Test fun textMustNotAccidentallyMatchNumberSuffix() {
        assertFalse(matchesContactSearch("Amit", "+91 98765-43210", "abc43210"))
        assertFalse(matchesContactSearch("Amit", "+91 98765-43210", "Prakash"))
        assertFalse(matchesContactSearch("Amit", "+91 98765-43210", "#*"))
    }
}
