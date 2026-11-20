package com.scall.phone

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
    fun cleanHistoryOpensRealKeypadFromBottomNavigation() {
        // The Ready to connect banner was removed by design in v1.9.6.
        rule.onNodeWithText("Ready to connect?").assertDoesNotExist()
        rule.onNodeWithText("Call history").assertExists()
        rule.onNodeWithContentDescription("Keypad", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Enter a number").assertExists()
    }

    @Test
    fun contactsTabHasRealAddEditorAndContactActions() {
        rule.onNodeWithContentDescription("Contacts", useUnmergedTree = true).performClick()
        rule.onNodeWithContentDescription("Add contact").performClick()
        rule.onNodeWithText("New contact").assertExists()
        rule.onNodeWithText("First name").assertExists()
        rule.onNodeWithText("Last name").assertExists()
        rule.onNodeWithText("+ Add more details").assertExists()
        rule.onNodeWithContentDescription("Cancel contact editor").performClick()
        rule.onNodeWithText("Import VCF").assertExists()
        rule.onNodeWithText("Export VCF").assertExists()
    }

    @Test
    fun contactsTabsShowFavoritesAndGroups() {
        rule.onNodeWithContentDescription("Contacts", useUnmergedTree = true).performClick()
        rule.onNodeWithText("Favourites").performClick()
        rule.onNodeWithText("Groups").performClick()
        rule.onNodeWithText("Choose contact group").assertExists()
    }

    @Test
    fun keypadDoesNotShiftOrSelectSuggestedContactsWhenTyping() {
        rule.onNodeWithContentDescription("Keypad", useUnmergedTree = true).performClick()
        rule.onNodeWithContentDescription("8, TUV").performClick()
        rule.onNodeWithContentDescription("0, +").performClick()
        rule.onNodeWithContentDescription("8, TUV").performClick()
        rule.onNodeWithContentDescription("5, JKL").performClick()
        rule.onNodeWithText("Matching contacts").assertDoesNotExist()
        rule.onNodeWithText("Call").assertExists()
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
        val lastKeyBottom = rule.onNodeWithContentDescription("#")
            .fetchSemanticsNode().boundsInRoot.bottom
        val callTop = rule.onNodeWithText("Call").fetchSemanticsNode().boundsInRoot.top
        val gapDp = (callTop - lastKeyBottom) / rule.activity.resources.displayMetrics.density
        org.junit.Assert.assertTrue(
            "Too much unused space below keypad: ${gapDp}dp",
            gapDp >= 0f && gapDp < 100f
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
