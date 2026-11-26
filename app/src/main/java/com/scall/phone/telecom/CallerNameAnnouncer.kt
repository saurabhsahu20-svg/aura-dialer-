package com.scall.phone.telecom

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telecom.Call

/** Never announce raw private phone numbers; opt-in and saved names only. */
internal fun shouldAnnounceSavedCaller(
    enabled: Boolean, ringing: Boolean, savedName: String?, alreadyAnnounced: Boolean
): Boolean =
    enabled && ringing && !alreadyAnnounced && !savedName.isNullOrBlank()

/**
 * Optional, privacy-conscious caller name announcement via installed Android
 * Text-to-Speech engine. Does not access/record cellular call audio.
 * It speaks at most once for each ringing call and is disabled by default.
 */
internal class CallerNameAnnouncer(private val context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private var engine: TextToSpeech? = null
    private var engineReady = false
    private var disposed = false
    private val announced = mutableSetOf<Call>()
    private var waiting: Pair<Call, String>? = null

    fun onIncoming(call: Call, savedName: String?, enabled: Boolean) {
        if (!shouldAnnounceSavedCaller(
                enabled, call.state == Call.STATE_RINGING,
                savedName, call in announced
            )) return
        announced.add(call)
        val cleanName = savedName!!.trim().take(80)
        waiting = call to cleanName
        if (engine == null) {
            try {
                engine = TextToSpeech(context.applicationContext) { result ->
                    main.post {
                        if (!disposed) {
                            engineReady = result == TextToSpeech.SUCCESS
                            if (engineReady) playPending()
                        }
                    }
                }
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallAnnounce", "Speech engine unavailable", error)
                waiting = null
            }
        } else if (engineReady) playPending()
    }

    private fun playPending() {
        val (call, name) = waiting ?: return
        waiting = null
        if (disposed || call.state != Call.STATE_RINGING) return
        try {
            engine?.speak(
                "Incoming call from $name",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "s_call_incoming_${System.identityHashCode(call)}"
            )
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallAnnounce", "Cannot announce saved caller", error)
        }
    }

    fun stopForCall(call: Call) {
        if (waiting?.first === call) waiting = null
        try { engine?.stop() } catch (_: RuntimeException) {}
    }

    fun onCallRemoved(call: Call) {
        announced.remove(call)
        if (waiting?.first === call) waiting = null
        try { engine?.stop() } catch (_: RuntimeException) {}
    }

    fun close() {
        disposed = true
        waiting = null
        announced.clear()
        try {
            engine?.stop()
            engine?.shutdown()
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallAnnounce", "Speech cleanup unavailable", error)
        }
        engine = null
        engineReady = false
    }
}
