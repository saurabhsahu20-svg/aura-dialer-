package com.scall.phone

import android.provider.CallLog
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.scall.phone.data.RecentEntry
import com.scall.phone.ui.CallDetailsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Recents detail surface must display call-log facts, not only contact photo. */
@RunWith(AndroidJUnit4::class)
class SCallDetailsUiTest {
    @get:Rule val rule = createComposeRule()

    @Test fun selectedCallShowsDirectionTimestampAndDuration() {
        val entry = RecentEntry(
            id = 120L,
            number = "9876543210",
            name = "Saved Caller",
            type = CallLog.Calls.INCOMING_TYPE,
            timestamp = 1730500000000L,
            duration = 92L,
            photo = null
        )
        rule.setContent {
            MaterialTheme {
                CallDetailsScreen(
                    selected = entry,
                    relatedCalls = listOf(entry),
                    note = "",
                    title = null,
                    photo = null,
                    onSelectCall = {},
                    onCall = {},
                    onMessage = {}
                )
            }
        }
        rule.onNodeWithText("Saved Caller").assertExists()
        rule.onNodeWithText("Incoming call recorded at").assertExists()
        rule.onNodeWithText("Call duration").assertExists()
        rule.onNodeWithText("1m 32s").assertExists()
    }
}
