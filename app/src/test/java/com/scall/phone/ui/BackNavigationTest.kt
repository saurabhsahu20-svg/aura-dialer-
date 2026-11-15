package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BackNavigationTest {
    @Test fun keypadBackReturnsToRecents() {
        val history = pushPageHistory(emptyList(), "Recents", "Keypad")
        assertEquals("Recents", popPageHistory(history).first)
    }

    @Test fun simSettingsBackReturnsToKeypad() {
        val trail = pushPageHistory(
            pushPageHistory(emptyList(), "Recents", "Keypad"), "Keypad", "SIM Manager"
        )
        val first = popPageHistory(trail)
        assertEquals("Keypad", first.first)
        assertEquals("Recents", popPageHistory(first.second).first)
    }

    @Test fun multipleTabsPopInVisitOrder() {
        val one = pushPageHistory(emptyList(), "Recents", "Contacts")
        val two = pushPageHistory(one, "Contacts", "Favorites")
        val three = popPageHistory(two)
        assertEquals("Contacts", three.first)
        assertEquals("Recents", popPageHistory(three.second).first)
    }

    @Test fun samePageDoesNotDuplicateHistory() {
        assertEquals(listOf("Recents"),
            pushPageHistory(listOf("Recents"), "Keypad", "Keypad"))
    }

    @Test fun historySizeIsBounded() {
        var history = emptyList<String>()
        repeat(60) { history = pushPageHistory(history, "Recents", "Keypad") }
        assertEquals(16, history.size)
    }
}
