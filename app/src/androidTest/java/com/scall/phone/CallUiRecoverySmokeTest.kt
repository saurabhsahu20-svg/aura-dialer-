package com.scall.phone

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scall.phone.telecom.CallUiActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The no-call screen must recover gracefully without a Telecom service.
 * This test does NOT simulate or initiate any cellular phone call.
 */
@RunWith(AndroidJUnit4::class)
class CallUiRecoverySmokeTest {
    @get:Rule val rule = createAndroidComposeRule<CallUiActivity>()

    @Test fun noLiveTelecomCallShowsRecoveryInsteadOfInfiniteSpinner() {
        rule.waitUntil(timeoutMillis = 12_000) {
            rule.onAllNodesWithText("Phone service not connected")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Retry call screen").assertExists()
        rule.onNodeWithText("Close").assertExists()
    }

    @Test fun retryStillDoesNotPretendCallIsConnected() {
        rule.waitUntil(timeoutMillis = 12_000) {
            rule.onAllNodesWithText("Retry call screen")
                .fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Retry call screen").performClick()
        rule.onNodeWithText("Opening call screen…").assertExists()
        rule.waitUntil(timeoutMillis = 12_000) {
            rule.onAllNodesWithText("Phone service not connected")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
