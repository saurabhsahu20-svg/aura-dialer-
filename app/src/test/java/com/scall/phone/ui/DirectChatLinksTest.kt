package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectChatLinksTest {
    @Test fun whatsappDeepLinkTargetsSelectedNumber() {
        assertEquals("whatsapp://send?phone=919876543210",
            externalChatDirectUri("9876543210", ExternalChatApp.WHATSAPP))
    }
    @Test fun telegramDeepLinkTargetsSelectedNumber() {
        assertEquals("tg://resolve?phone=919876543210",
            externalChatDirectUri("+91 98765 43210", ExternalChatApp.TELEGRAM))
    }
    @Test fun malformedNumbersAreRejected() {
        assertNull(externalChatDirectUri("*123#", ExternalChatApp.WHATSAPP))
        assertNull(externalChatDirectUri("", ExternalChatApp.TELEGRAM))
    }
    @Test fun officialFallbackWebLinksAreRetained() {
        assertEquals("https://wa.me/919876543210",
            externalChatLink("9876543210", ExternalChatApp.WHATSAPP))
        assertEquals("https://t.me/+919876543210",
            externalChatLink("9876543210", ExternalChatApp.TELEGRAM))
    }
}
