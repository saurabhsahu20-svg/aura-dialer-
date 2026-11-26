package com.scall.phone.telecom

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.telecom.Call

/**
 * Only respond to an actual "screen up -> face down" change, held steadily
 * for 750ms; not to a brief movement or a phone already face-down at start.
 */
internal class FaceDownSilenceDetector(private val holdMillis: Long = 750L) {
    private var observedFaceUp = false
    private var downSinceMillis: Long? = null
    private var completed = false

    fun onReading(z: Float, millis: Long): Boolean {
        if (completed || !z.isFinite()) return false
        if (z > -5f) {
            observedFaceUp = true
            downSinceMillis = null
            return false
        }
        if (z > -7f || !observedFaceUp) {
            downSinceMillis = null
            return false
        }
        val started = downSinceMillis
        if (started == null || millis < started) {
            downSinceMillis = millis
            return false
        }
        if (millis - started < holdMillis) return false
        completed = true
        return true
    }
}

/**
 * Listens ONLY while an enabled SIM call is RINGING. Never changes the
 * device-wide ringer mode or Do Not Disturb setting.
 */
internal class FlipToSilenceController(
    context: Context,
    private val silence: () -> Unit
) : SensorEventListener {
    private val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var monitoredCall: Call? = null
    private var detector = FaceDownSilenceDetector()
    private var registered = false

    fun update(call: Call?, enabled: Boolean) {
        val ringing = if (enabled && call?.state == Call.STATE_RINGING) call else null
        if (ringing === monitoredCall) return
        stop()
        if (ringing == null || accelerometer == null) return
        detector = FaceDownSilenceDetector()
        val success = try {
            manager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL) == true
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallFlip", "Accelerometer unavailable", error)
            false
        }
        if (success) {
            monitoredCall = ringing
            registered = true
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!registered || monitoredCall?.state != Call.STATE_RINGING ||
            event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (event.values.size < 3) return
        if (detector.onReading(event.values[2], event.timestamp / 1_000_000L)) {
            try {
                // This silences ONLY the current ringtone; it never rejects a call.
                silence()
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallFlip", "Cannot silence ringer", error)
            }
            stop()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    fun stop() {
        if (registered) {
            try { manager?.unregisterListener(this) } catch (error: RuntimeException) {
                android.util.Log.w("SCallFlip", "Sensor release failed", error)
            }
        }
        registered = false
        monitoredCall = null
    }
}
