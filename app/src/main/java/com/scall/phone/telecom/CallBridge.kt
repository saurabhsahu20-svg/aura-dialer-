package com.scall.phone.telecom

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class CallSnapshot(
    val number: String,
    val displayName: String,
    val state: Int,
    val canHold: Boolean,
    val canAddCall: Boolean,
    val canMerge: Boolean,
    val isMuted: Boolean,
    val speakerOn: Boolean,
    val bluetoothAvailable: Boolean,
    val connectedAtMillis: Long,
    val accountLabel: String?
)

/**
 * Lightweight bridge between Android Telecom's live Call objects and Compose.
 * The call objects remain entirely under Android Telecom control.
 */
internal object CallBridge {
    private var service: InCallService? = null
    private val callbacks = linkedMapOf<Call, Call.Callback>()
    private val mutable = MutableStateFlow<CallSnapshot?>(null)
    val current = mutable.asStateFlow()

    fun bind(inCallService: InCallService) {
        service = inCallService
        refresh()
    }

    fun unbind(inCallService: InCallService) {
        if (service === inCallService) service = null
        callbacks.keys.toList().forEach { removeCall(it) }
        mutable.value = null
    }

    fun addCall(call: Call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) = refresh()
            override fun onDetailsChanged(call: Call, details: Call.Details) = refresh()
            override fun onCallDestroyed(call: Call) = removeCall(call)
            override fun onChildrenChanged(call: Call, children: MutableList<Call>) = refresh()
        }
        if (!callbacks.containsKey(call)) {
            callbacks[call] = callback
            call.registerCallback(callback)
        }
        refresh()
    }

    fun removeCall(call: Call) {
        callbacks.remove(call)?.let(call::unregisterCallback)
        refresh()
    }

    private fun primaryCall(): Call? = callbacks.keys.firstOrNull { it.state == Call.STATE_RINGING }
        ?: callbacks.keys.firstOrNull { it.state == Call.STATE_ACTIVE }
        ?: callbacks.keys.firstOrNull()

    fun refresh() {
        val call = primaryCall()
        if (call == null) {
            mutable.value = null
            return
        }
        val details = call.details
        val audio = service?.callAudioState
        val earlier = mutable.value
        val connecting = call.state == Call.STATE_ACTIVE
        val accountLabel = try {
            val manager = service?.getSystemService(android.content.Context.TELECOM_SERVICE)
                as? android.telecom.TelecomManager
            details.accountHandle?.let { manager?.getPhoneAccount(it)?.label?.toString() }
        } catch (_: Exception) { null }
        mutable.value = CallSnapshot(
            number = details.handle?.schemeSpecificPart.orEmpty(),
            displayName = details.callerDisplayName?.toString().orEmpty(),
            state = call.state,
            canHold = details.can(Call.Details.CAPABILITY_HOLD),
            canAddCall = service?.canAddCall() == true,
            canMerge = details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE),
            isMuted = audio?.isMuted ?: false,
            speakerOn = audio?.route == CallAudioState.ROUTE_SPEAKER,
            bluetoothAvailable = (audio?.supportedRouteMask ?: 0) and CallAudioState.ROUTE_BLUETOOTH != 0,
            connectedAtMillis = if (connecting) {
                details.connectTimeMillis.takeIf { it > 0 }
                    ?: earlier?.connectedAtMillis?.takeIf { it > 0 }
                    ?: System.currentTimeMillis()
            } else 0L,
            accountLabel = accountLabel
        )
    }

    fun answer() { primaryCall()?.takeIf { it.state == Call.STATE_RINGING }
        ?.answer(VideoProfile.STATE_AUDIO_ONLY) }

    fun reject() { primaryCall()?.takeIf { it.state == Call.STATE_RINGING }
        ?.reject(false, null) }

    fun disconnect() { primaryCall()?.disconnect() }

    fun holdOrUnhold() {
        primaryCall()?.let { call ->
            when (call.state) {
                Call.STATE_ACTIVE -> if (call.details.can(Call.Details.CAPABILITY_HOLD)) call.hold()
                Call.STATE_HOLDING -> call.unhold()
            }
        }
    }

    fun merge() { primaryCall()?.conferenceableCalls?.firstOrNull()?.let { other ->
        primaryCall()?.conference(other)
    } ?: primaryCall()?.mergeConference() }

    fun addCall() {
        service?.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun toggleMute() { service?.setMuted(!(mutable.value?.isMuted ?: false)) }

    fun toggleSpeaker() {
        val enable = !(mutable.value?.speakerOn ?: false)
        service?.setAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
    }

    fun selectBluetooth() { service?.setAudioRoute(CallAudioState.ROUTE_BLUETOOTH) }

    fun dtmf(digit: Char) {
        primaryCall()?.let { call ->
            call.playDtmfTone(digit)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                call.stopDtmfTone()
            }, 130)
        }
    }
}
