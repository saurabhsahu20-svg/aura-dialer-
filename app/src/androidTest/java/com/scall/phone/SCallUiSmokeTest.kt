package com.scall.phone

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun freshQuickDialCardOpensRealKeypad() {
        rule.onNodeWithText("Ready to connect?").assertExists()
        rule.onNodeWithText("Ready to connect?").performClick()
        rule.onNodeWithText("Enter a number").assertExists()
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
    fun keypadDoesNotLeaveAnOversizedGapUnderTheDigits() {
        rule.onNodeWithContentDescription("Keypad", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        // On compact emulator screens the fourth keypad row is intentionally
        // reachable by scrolling. Unscrolled offscreen semantics report the
        // placeholder Rect(0, 0, 0, 0), creating a false 532dp gap.
        rule.onNodeWithContentDescription("#").performScrollTo()
        rule.waitForIdle()
        val first = rule.onNodeWithContentDescription("1").fetchSemanticsNode().boundsInRoot
        val second = rule.onNodeWithContentDescription("4, GHI").fetchSemanticsNode().boundsInRoot
        val third = rule.onNodeWithContentDescription("7, PQRS").fetchSemanticsNode().boundsInRoot
        val last = rule.onNodeWithContentDescription("#").fetchSemanticsNode().boundsInRoot
        val call = rule.onNodeWithText("Call").fetchSemanticsNode().boundsInRoot
        val density = rule.activity.resources.displayMetrics.density
        val gapDp = (call.top - last.bottom) / density
        val metrics = "first=$first second=$second third=$third last=$last call=$call density=$density"
        org.junit.Assert.assertTrue(
            "Too much unused space below keypad: ${gapDp}dp; $metrics",
            last.width > 0f && last.height > 0f && gapDp >= 0f && gapDp < 100f
        )
    }

    @Test
    fun recordingMenusAreGoneAndCallSettingsStillWork() {
        // Removing unsupported call recording must not remove the real phone settings.
        rule.onNodeWithContentDescription("Recordings").assertDoesNotExist()
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Call Recording").assertDoesNotExist()
        rule.onNodeWithText("Incoming call display").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Call history").assertExists()
    }

    @Test
    fun incomingDisplaySettingsAreVisible() {
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Incoming call display").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Call history").assertExists()
    }

    @Test
    fun simSettingsAccessibleWithoutHomeBanner() {
        rule.onNodeWithContentDescription("SIM and network preferences").performClick()
        rule.onNodeWithText("Detected SIM cards").assertExists()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.onNodeWithText("Call history").assertExists()
    }
}
