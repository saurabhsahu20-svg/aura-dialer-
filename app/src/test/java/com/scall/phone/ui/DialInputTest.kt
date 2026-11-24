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

    @Test fun keypadExpandsOnTallPhone() {
        assertEquals(112f, adaptiveDialRowHeight(460f, 0), 0.01f)
    }

    @Test fun keypadKeepsTapTargetsOnCompactPhone() {
        assertEquals(72f, adaptiveDialRowHeight(235f, 0), 0.01f)
    }

    @Test fun keypadReducesRowHeightForContactSuggestions() {
        val normal = adaptiveDialRowHeight(460f, 0)
        val withMatches = adaptiveDialRowHeight(460f, 3)
        org.junit.Assert.assertTrue(withMatches < normal)
        org.junit.Assert.assertTrue(withMatches >= 72f)
    }

    @Test fun keypadRespectsHeightLimit() {
        assertEquals(136f, adaptiveDialRowHeight(2000f, 0), 0.01f)
    }

    @Test fun invalidHeightStillCreatesUsableButtons() {
        assertEquals(72f, adaptiveDialRowHeight(Float.NaN, 0), 0.01f)
    }
    @Test fun fixedKeypadHeightDoesNotDependOnContactMatches() {
        assertEquals(60f, stableDialRowHeight(240f), 0.01f)
        assertEquals(52f, stableDialRowHeight(160f), 0.01f)
        assertEquals(90f, stableDialRowHeight(540f), 0.01f)
        assertEquals(52f, stableDialRowHeight(Float.NaN), 0.01f)
    }

}
