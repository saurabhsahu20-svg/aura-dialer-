package com.scall.phone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactLookupKeyTest {
    @Test fun indianMobileVariantsShareIdentity() {
        val national = contactLookupKey("9876543210")
        assertEquals(national, contactLookupKey("+91 98765-43210"))
        assertEquals(national, contactLookupKey("09876543210"))
        assertEquals(national, contactLookupKey("919876543210"))
    }

    @Test fun internationalPrefixesCannotCollide() {
        assertNotEquals(contactLookupKey("+1 9876543210"), contactLookupKey("+91 9876543210"))
        assertNotEquals(contactLookupKey("+1 9876543210"), contactLookupKey("9876543210"))
    }

    @Test fun invalidAndPrivateNumbersDoNotMatch() {
        assertNull(contactLookupKey(""))
        assertNull(contactLookupKey("*123#"))
        assertNull(contactLookupKey("123"))
        assertNull(contactLookupKey("Unknown"))
    }

    @Test fun numberSeparatorsAreIgnored() {
        assertEquals(contactLookupKey("+44 7700 900123"), contactLookupKey("+44(7700)900123"))
    }
}
