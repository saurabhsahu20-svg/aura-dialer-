package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalChatLinksTest {
    @Test fun indianNationalFormatsBecomeInternational() {
        assertEquals("917224858148", chatPhoneDigits("72248 58148"))
        assertEquals("917224858148", chatPhoneDigits("07224858148"))
        assertEquals("917224858148", chatPhoneDigits("+91 72248 58148"))
        assertEquals("917224858148", chatPhoneDigits("91-7224858148"))
    }

    @Test fun linksUseDocumentedPublicSchemes() {
        assertEquals("https://wa.me/917224858148",
            externalChatLink("7224858148", ExternalChatApp.WHATSAPP))
        assertEquals("https://t.me/+917224858148",
            externalChatLink("+91 7224858148", ExternalChatApp.TELEGRAM))
    }

    @Test fun malformedAndUnknownCountryNumbersAreRejected() {
        assertNull(chatPhoneDigits("123"))
        assertNull(chatPhoneDigits(" "))
        assertNull(chatPhoneDigits("*123#"))
        assertNull(chatPhoneDigits("00447700900123"))
        assertNull(chatPhoneDigits("7224858148;123"))
        assertNull(chatPhoneDigits("++917224858148"))
        assertNull(chatPhoneDigits("12345678901234567890"))
    }

    @Test fun explicitlyInternationalNumberOtherCountryIsPreserved() {
        assertEquals("447700900123", chatPhoneDigits("+44 7700 900123"))
    }
}
