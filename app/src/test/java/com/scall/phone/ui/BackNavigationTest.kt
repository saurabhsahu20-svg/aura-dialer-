package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackNavigationTest {
    @Test fun keypadBackReturnsDirectlyToRecents() {
        val history = pushPageHistory(emptyList(), "Recents", "Keypad")
        assertTrue(history.isEmpty())
        assertEquals("Recents" to emptyList<String>(), backPage(history, "Keypad"))
    }

    @Test fun contactsAndFavoritesNeverReplayEarlierTabs() {
        val one = pushPageHistory(emptyList(), "Recents", "Contacts")
        val two = pushPageHistory(one, "Contacts", "Favorites")
        val three = pushPageHistory(two, "Favorites", "Keypad")
        assertTrue(three.isEmpty())
        assertEquals("Recents" to emptyList<String>(), backPage(three, "Keypad"))
    }

    @Test fun settingsBackReturnsToTheCorrectCurrentTab() {
        val history = pushPageHistory(emptyList(), "Contacts", "Settings")
        assertEquals(listOf("Contacts"), history)
        assertEquals("Contacts" to emptyList<String>(), backPage(history, "Settings"))
        assertEquals("Recents" to emptyList<String>(), backPage(emptyList(), "Contacts"))
    }

    @Test fun nestedSimSettingsBackReturnsToSettingsThenKeypadThenRecents() {
        val parent = pushPageHistory(emptyList(), "Keypad", "Settings")
        val history = pushPageHistory(parent, "Settings", "SIM Manager")
        val first = backPage(history, "SIM Manager")
        assertEquals("Settings", first.first)
        val second = backPage(first.second, first.first)
        assertEquals("Keypad", second.first)
        val third = backPage(second.second, second.first)
        assertEquals("Recents" to emptyList<String>(), third)
    }

    @Test fun pickingAnyBottomTabClearsNestedHistory() {
        val nested = listOf("Recents", "Settings")
        for (tab in primaryCallTabs) {
            assertTrue(pushPageHistory(nested, "SIM Manager", tab).isEmpty())
        }
    }

    @Test fun oldSavedHistoryCannotCauseRepeatedTabBackNavigation() {
        val previousVersionHistory = listOf("Recents", "Contacts", "Favorites")
        assertEquals(
            "Recents" to emptyList<String>(),
            backPage(previousVersionHistory, "Keypad")
        )
    }

    @Test fun samePageDoesNotDuplicateHistory() {
        assertEquals(listOf("Recents"),
            pushPageHistory(listOf("Recents"), "Keypad", "Keypad"))
    }

    @Test fun nestedHistorySizeIsBounded() {
        var history = emptyList<String>()
        repeat(60) {
            history = pushPageHistory(history, "Settings", "SIM Manager")
        }
        assertEquals(8, history.size)
    }

    @Test fun missingNestedHistoryFallsBackToRoot() {
        assertEquals("Recents" to emptyList<String>(), backPage(emptyList(), "Settings"))
    }
}
