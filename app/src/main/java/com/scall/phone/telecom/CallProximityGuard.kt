package com.scall.phone.telecom

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.telecom.Call

/**
 * Only hide/guard the screen while a voice call uses the built-in earpiece.
 * Incoming ringing, speaker, wired and Bluetooth calls must remain touchable.
 */
internal fun shouldGuardCallTouches(state: Int?, routeKind: String?): Boolean =
    routeKind == "earpiece" && when (state) {
        Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING -> true
        else -> false
    }

/**
 * Scoped strictly to the visible in-call Activity.
 *
 * A supported proximity screen-off wakelock lets Android turn the display off
 * at the ear. The sensor listener additionally consumes accidental in-app
 * touches when an OEM has no working proximity screen-off wakelock.
 *
 * This does not change system Do Not Disturb settings or block deliberate
 * access to Android's notification shade.
 */
internal class CallProximityGuard(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val proximityWakeLock: PowerManager.WakeLock? by lazy {
        try {
            if (proximitySensor != null &&
                powerManager?.isWakeLockLevelSupported(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
                ) == true
            ) {
                powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "com.scall.phone:active-call-proximity"
                ).apply { setReferenceCounted(false) }
            } else null
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallProximity", "Proximity screen-off unavailable", error)
            null
        }
    }

    private var visible = false
    private var guarding = false
    private var listening = false
    private var sensorNear = false

    val shouldSuppressTouch: Boolean
        get() = guarding && sensorNear

    fun start(snapshot: CallSnapshot?) {
        visible = true
        update(snapshot)
    }

    fun update(snapshot: CallSnapshot?) {
        val required = visible && proximitySensor != null &&
            shouldGuardCallTouches(snapshot?.state, snapshot?.audioRouteKind)
        if (required == guarding) return
        guarding = required
        if (required) {
            sensorNear = false
            listening = try {
                sensorManager?.registerListener(
                    this, proximitySensor, SensorManager.SENSOR_DELAY_UI
                ) == true
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallProximity", "Unable to watch proximity sensor", error)
                false
            }
            try {
                proximityWakeLock?.let { if (!it.isHeld) it.acquire() }
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallProximity", "Unable to protect screen at ear", error)
            }
        } else {
            release()
        }
    }

    fun stop() {
        visible = false
        guarding = false
        release()
    }

    private fun release() {
        if (listening) {
            try {
                sensorManager?.unregisterListener(this)
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallProximity", "Sensor cleanup failed", error)
            }
            listening = false
        }
        sensorNear = false
        try {
            proximityWakeLock?.let { if (it.isHeld) it.release() }
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallProximity", "Proximity wake lock cleanup failed", error)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!guarding || event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val distance = event.values.firstOrNull() ?: return
        val range = proximitySensor?.maximumRange ?: return
        sensorNear = distance.isFinite() && range > 0f && distance >= 0f && distance < range
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
