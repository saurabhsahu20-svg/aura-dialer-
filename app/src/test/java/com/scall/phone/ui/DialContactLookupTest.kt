package com.scall.phone.ui

import com.scall.phone.data.ContactEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialContactLookupTest {
    private val contacts = listOf(
        ContactEntry(1L, "Saved Person", "+91 98765 43210", null, false),
        ContactEntry(2L, "Foreign Number", "+44 98765 43210", null, false),
        ContactEntry(3L, "Other Person", "8876543210", null, false)
    )

    @Test fun fullNationalNumberShowsName() {
        assertEquals("Saved Person", exactDialContact("9876543210", contacts)?.name)
    }

    @Test fun leadingZeroAndIndianCountryCodeShowSameName() {
        assertEquals("Saved Person", exactDialContact("09876543210", contacts)?.name)
        assertEquals("Saved Person", exactDialContact("+919876543210", contacts)?.name)
        assertEquals("Saved Person", exactDialContact("919876543210", contacts)?.name)
    }

    @Test fun partialTypedNumbersMustNotShowName() {
        assertNull(exactDialContact("98", contacts))
        assertNull(exactDialContact("987654321", contacts))
    }

    @Test fun anExtraDigitMustNotMatchAnExistingName() {
        assertNull(exactDialContact("98765432109", contacts))
    }

    @Test fun doNotMatchDifferentInternationalCountryCodes() {
        assertEquals("Foreign Number", exactDialContact("+449876543210", contacts)?.name)
        assertNull(exactDialContact("+339876543210", contacts))
    }

    @Test fun unrecognizedNumberMustNotShowStaleSavedName() {
        assertNull(exactDialContact("7777777777", contacts))
    }

    @Test fun formattedContactStillMatchesExactly() {
        assertEquals("Saved Person", exactDialContact("(98765) 43210", contacts)?.name)
    }
}
