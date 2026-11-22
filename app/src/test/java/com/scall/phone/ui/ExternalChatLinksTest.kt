package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalChatLinksTest {
    @Test fun indianNumbers() {
        assertEquals("919876543210", chatPhoneDigits("9876543210"))
        assertEquals("919876543210", chatPhoneDigits("09876543210"))
        assertEquals("919876543210", chatPhoneDigits("+91 98765 43210"))
    }

    @Test fun invalidNumbersDoNotLaunch() {
        assertNull(chatPhoneDigits(""))
        assertNull(chatPhoneDigits("1234"))
        assertNull(chatPhoneDigits("+91+9876543210"))
        assertNull(chatPhoneDigits("tel:9876543210"))
        assertNull(chatPhoneDigits("9999999999;DROP"))
    }

    @Test fun directLinks() {
        assertEquals("https://wa.me/919876543210", externalChatLink("9876543210", ExternalChatApp.WHATSAPP))
        assertEquals("https://t.me/+919876543210", externalChatLink("9876543210", ExternalChatApp.TELEGRAM))
    }
}
