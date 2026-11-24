package com.scall.phone

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scall.phone.data.ContactEntry
import com.scall.phone.ui.KeypadScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Regression for matching contacts pushing digit buttons beneath the finger. */
@RunWith(AndroidJUnit4::class)
class SCallKeypadStabilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun matchingContactsNeverMoveDigitButtonsOrSelectAccidentally() {
        val contacts = listOf(ContactEntry(1L, "Test Contact", "2212345", null, false))
        rule.setContent {
            var number by remember { mutableStateOf("") }
            MaterialTheme {
                KeypadScreen(
                    value = number,
                    onValueChange = { number = it },
                    contacts = contacts,
                    onCall = {},
                    onMessage = {},
                    onChat = { _, _ -> },
                    onSimSettings = {},
                    onBack = {},
                    simLabel = "Test SIM"
                )
            }
        }
        rule.waitForIdle()
        val before = rule.onNodeWithContentDescription("2, ABC")
            .fetchSemanticsNode().boundsInRoot
        rule.onNodeWithContentDescription("2, ABC").performClick()
        rule.onNodeWithContentDescription("2, ABC").performClick()

        rule.waitUntil(timeoutMillis = 3000) {
            rule.onAllNodesWithText("Phone number • 1 contacts (tap to view)")
                .fetchSemanticsNodes().isNotEmpty()
        }
        val after = rule.onNodeWithContentDescription("2, ABC")
            .fetchSemanticsNode().boundsInRoot
        // Search results may change, but the key must remain stationary.
        assertEquals(before.top, after.top, 1f)
        assertEquals(before.bottom, after.bottom, 1f)
        // The contact is NOT a clickable replacement for any digit.
        rule.onNodeWithText("Test Contact").assertDoesNotExist()
        rule.onNodeWithText("Phone number • 1 contacts (tap to view)").performClick()
        rule.onNodeWithText("Test Contact").assertExists()
        rule.onNodeWithText("Close").performClick()
        rule.onNodeWithContentDescription("Dialed phone number 22").assertExists()
    }
    @Test fun completeSavedNumberShowsNameWithoutMovingKeys() {
        val digitState = mutableStateOf("")
        val contacts = listOf(
            ContactEntry(10L, "Saved Person", "+91 98765 43210", null, false)
        )
        rule.setContent {
            MaterialTheme {
                KeypadScreen(
                    value = digitState.value,
                    onValueChange = { digitState.value = it },
                    contacts = contacts,
                    onCall = {}, onMessage = {}, onChat = { _, _ -> },
                    onSimSettings = {}, onBack = {}, simLabel = "Test SIM"
                )
            }
        }
        rule.waitForIdle()
        val before = rule.onNodeWithContentDescription("2, ABC")
            .fetchSemanticsNode().boundsInRoot
        rule.runOnIdle { digitState.value = "9876543210" }
        rule.waitUntil(timeoutMillis = 3000) {
            rule.onAllNodesWithText("Saved Person • Saved contact")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Saved Person • Saved contact").assertExists()
        rule.onNodeWithContentDescription("Dialed phone number 9876543210").assertExists()
        val after = rule.onNodeWithContentDescription("2, ABC")
            .fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 1f)
        assertEquals(before.bottom, after.bottom, 1f)

        // A nonmatching extra key must immediately remove the old caller name.
        rule.onNodeWithContentDescription("1").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Saved Person • Saved contact").assertDoesNotExist()
        rule.onNodeWithContentDescription("Dialed phone number 98765432101").assertExists()
    }

}
