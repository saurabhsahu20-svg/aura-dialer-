package com.scall.phone.telecom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.scall.phone.ui.CallScreen
import kotlinx.coroutines.delay

class CallUiActivity : ComponentActivity() {
    companion object {
        const val ACTION_ANSWER = "com.scall.phone.action.ANSWER_CALL"

        fun createIntent(context: Context): Intent =
            Intent(context, CallUiActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )

        fun createAnswerIntent(context: Context): Intent =
            createIntent(context).setAction(ACTION_ANSWER)
    }

    private fun handleCallAction(intent: Intent?) {
        CallBridge.refreshFromTelecom()
        if (intent?.action == ACTION_ANSWER) {
            CallBridge.answer()
            // Do not answer a different future call if the original notification
            // action is replayed or this Activity is recreated.
            intent.action = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallAction(intent)
    }

    override fun onResume() {
        super.onResume()
        CallBridge.refreshFromTelecom()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 8.0 (API 26) requires legacy lock-screen flags; the Activity
        // convenience methods were introduced only in Android 8.1 (API 27).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        handleCallAction(intent)
        setContent {
            val call by CallBridge.current.collectAsState()
            var retryAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            var noCallTimeout by remember { mutableStateOf(false) }
            var hadLiveCall by remember { mutableStateOf(false) }
            LaunchedEffect(call, retryAttempt) {
                if (call != null) {
                    hadLiveCall = true
                    noCallTimeout = false
                } else if (hadLiveCall) {
                    // Android Telecom has removed the call; dismiss the controls.
                    delay(600)
                    if (CallBridge.current.value == null) finish()
                } else {
                    noCallTimeout = false
                    CallBridge.refreshFromTelecom()
                    delay(2500)
                    noCallTimeout = CallBridge.current.value == null
                }
            }
            CallScreen(
                call = call,
                onClose = { finish() },
                phoneServiceUnavailable = noCallTimeout,
                onRetry = {
                    noCallTimeout = false
                    CallBridge.refreshFromTelecom()
                    retryAttempt++
                }
            )
        }
    }
}
