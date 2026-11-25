package com.scall.phone.telecom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import android.os.Build
import android.view.WindowManager
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.isActive
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.scall.phone.ui.CallScreen
import kotlinx.coroutines.delay

// End Call is explicitly allowed even if an OEM's proximity sensor stays
// near after the phone was moved away from the ear.
internal fun isHangUpButtonTouch(x: Float, y: Float, bounds: Rect?): Boolean =
    bounds?.contains(Offset(x, y)) == true

class CallUiActivity : ComponentActivity() {
    private val accidentalTouchGuard by lazy { CallProximityGuard(this) }
    private var endButtonBounds: Rect? = null
    private var deliberateHangUpGesture = false
    private var suppressCurrentGesture = false

    // Protect other controls against near-ear touches, but always deliver
    // deliberate taps inside the red End Call button. A stuck near-sensor
    // previously consumed ACTION_DOWN and the call could only be ended remotely.
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            deliberateHangUpGesture = isHangUpButtonTouch(
                event.x, event.y, endButtonBounds
            )
            suppressCurrentGesture =
                accidentalTouchGuard.shouldSuppressTouch && !deliberateHangUpGesture
        }
        val blocked = suppressCurrentGesture ||
            (accidentalTouchGuard.shouldSuppressTouch && !deliberateHangUpGesture)
        val result = if (blocked) true else super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL) {
            suppressCurrentGesture = false
            deliberateHangUpGesture = false
        }
        return result
    }

    override fun onStart() {
        super.onStart()
        accidentalTouchGuard.start(CallBridge.current.value)
    }

    override fun onStop() {
        // Stop protecting the display immediately when the call UI leaves.
        accidentalTouchGuard.stop()
        endButtonBounds = null
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.statusBars())
        super.onStop()
    }

    override fun onDestroy() {
        accidentalTouchGuard.stop()
        super.onDestroy()
    }

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
        // Hides the quick-settings entry while the call UI is foreground.
        // Android may still show system controls on a deliberate edge swipe.
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }
        CallBridge.refreshFromTelecom()
        accidentalTouchGuard.update(CallBridge.current.value)
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
            // Protect only real earpiece voice calls, not speaker or Bluetooth.
            LaunchedEffect(call?.state, call?.audioRouteKind) {
                accidentalTouchGuard.update(call)
            }
            var retryAttempt by remember { androidx.compose.runtime.mutableIntStateOf(0) }
            var noCallTimeout by remember { mutableStateOf(false) }
            var hadLiveCall by remember { mutableStateOf(false) }
            val endingProgress by CallBridge.endProgress.collectAsState()

            // Some Android manufacturers omit or delay onCallRemoved while
            // ending a call. Recheck actual Telecom calls for a bounded period
            // after an End tap so the activity does not retain stale UI state.
            LaunchedEffect(endingProgress) {
                if (endingProgress != EndCallProgress.IDLE) {
                    repeat(20) {
                        if (!isActive || CallBridge.current.value == null) return@LaunchedEffect
                        CallBridge.refreshFromTelecom()
                        delay(250)
                    }
                }
            }

            // Dismiss automatically once Telecom no longer reports a live call.
            // Do not close the screen merely because a disconnect was requested:
            // the call may genuinely still be connected on a delayed network.
            LaunchedEffect(call != null, retryAttempt) {
                if (call != null) {
                    hadLiveCall = true
                    noCallTimeout = false
                } else if (hadLiveCall) {
                    delay(200)
                    CallBridge.refreshFromTelecom()
                    if (CallBridge.current.value == null) {
                        accidentalTouchGuard.stop()
                        finish()
                    }
                } else {
                    noCallTimeout = false
                    CallBridge.refreshFromTelecom()
                    delay(2500)
                    noCallTimeout = CallBridge.current.value == null
                    // A stale incoming-call notification might launch this UI
                    // AFTER Telecom already ended the call. Give delayed Telecom
                    // binding a grace period, then dismiss the orphaned screen.
                    if (noCallTimeout) {
                        delay(1500)
                        CallBridge.refreshFromTelecom()
                        if (CallBridge.current.value == null) {
                            accidentalTouchGuard.stop()
                            finish()
                        }
                    }
                }
            }
            CallScreen(
                call = call,
                onClose = { finish() },
                onEndButtonBoundsChanged = { bounds -> endButtonBounds = bounds },
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
