package com.scall.phone.telecom

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        fun createIntent(context: Context): Intent =
            Intent(context, CallUiActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        setContent {
            val call by CallBridge.current.collectAsState()
            LaunchedEffect(call) {
                if (call == null) {
                    delay(1600) // Give Telecom time to attach after placing a call.
                    if (CallBridge.current.value == null) finish()
                }
            }
            CallScreen(call = call, onClose = { finish() })
        }
    }
}
