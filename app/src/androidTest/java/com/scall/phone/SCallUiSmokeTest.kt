package com.scall.phone

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Non-telephony UI smoke tests run on an Android emulator.
 * These never place a real phone call or change the default dialer role.
 */
@RunWith(AndroidJUnit4::class)
class SCallUiSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesWithHistoryAndBottomNavigation() {
        rule.onNodeWithText("Call history").assertExists()
        rule.onNodeWithContentDescription("Keypad", useUnmergedTree = true).assertExists()
    }

    @Test
    fun keypadBackArrowReturnsToRecentsWithoutExitingApp() {
        rule.onNodeWithContentDescription("Keypad", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Enter a number").assertExists()
        rule.onNodeWithContentDescription("Return to previous screen").performClick()
        rule.onNodeWithText("Call history").assertExists()
    }

    @Test
    fun keypadDigitsClearAndReturnToHistory() {
        rule.onNodeWithContentDescription("Keypad", useUnmergedTree = true).performClick()
        rule.onNodeWithContentDescription("2, ABC").performClick()
        rule.onNodeWithText("Clear").assertExists()
        rule.onNodeWithText("Clear").performClick()
        rule.onNodeWithText("Paste").assertExists()
        rule.onNodeWithContentDescription("Return to previous screen").performClick()
        rule.onNodeWithText("Call history").assertExists()
    }

    @Test
    fun simSettingsAccessibleWithoutHomeBanner() {
        rule.onNodeWithContentDescription("SIM and network preferences").performClick()
        rule.onNodeWithText("SIM & network").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Call history").assertExists()
    }
}
