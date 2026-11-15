package com.scall.phone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormattingTest {
    @Test fun stripsLocalFormatting() {
        assertEquals("9876543210", normalizeNumber("(987) 654-3210"))
    }

    @Test fun preservesInternationalPrefix() {
        assertEquals("+919876543210", normalizeNumber("+91 98765-43210"))
    }
}
