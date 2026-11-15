package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class DialInputTest {
    @Test fun digitsAppendWithoutLoss() {
        assertEquals("9876543210", appendDialKey("987654321", "0"))
    }

    @Test fun longPressZeroCanAddCountryCodePrefix() {
        assertEquals("+91", appendDialKey("+9", "1"))
    }

    @Test fun clipboardPasteKeepsDialCharactersOnly() {
        assertEquals("+91 9876-543210", cleanDialNumber("Call: +91 9876-543210"))
    }

    @Test fun maliciouslyLongClipboardTextIsCapped() {
        assertEquals(40, cleanDialNumber("8".repeat(900)).length)
    }

    @Test fun starAndHashRemainUsable() {
        assertEquals("*123#", cleanDialNumber("*123#"))
    }
}
