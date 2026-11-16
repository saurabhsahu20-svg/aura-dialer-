package com.scall.phone.telecom

import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
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
    val accountLabel: String?,
    val audioRouteLabel: String,
    val availableAudioRoutes: List<CallAudioRoute>,
    val audioRouteError: String?
)

/**
 * Lightweight bridge between Android Telecom's live Call objects and Compose.
 * The call objects remain entirely under Android Telecom control.
 */
internal object CallBridge {
    private var service: InCallService? = null
    private var latestEndpoints: List<CallEndpoint> = emptyList()
    private var latestEndpoint: CallEndpoint? = null
    private var latestMute: Boolean? = null
    private var routeError: String? = null
    private val callbacks = linkedMapOf<Call, Call.Callback>()
    private val mutable = MutableStateFlow<CallSnapshot?>(null)
    val current = mutable.asStateFlow()

    fun bind(inCallService: InCallService) {
        if (service !== inCallService && service != null) {
            // A replacement Telecom binding must not inherit callbacks from
            // the older service. The old service's onDestroy can arrive later.
            callbacks.toMap().forEach { (call, callback) ->
                call.unregisterCallback(callback)
            }
            callbacks.clear()
        }
        service = inCallService
        latestEndpoints = emptyList()
        latestEndpoint = null
        latestMute = null
        routeError = null
        refreshFromTelecom()
    }

    fun unbind(inCallService: InCallService) {
        // An older binding's onDestroy MUST NOT erase the current call.
        if (!isCurrentTelecomBinding(service, inCallService)) return
        service = null
        latestEndpoints = emptyList()
        latestEndpoint = null
        latestMute = null
        routeError = null
        callbacks.toMap().forEach { (call, callback) ->
            call.unregisterCallback(callback)
        }
        callbacks.clear()
        mutable.value = null
    }

    /**
     * Recover live Android Telecom calls after activity/service recreation.
     * Never invent a call: when Telecom reports none, show the user an honest
     * reconnect state instead of a fake connected-call screen.
     */
    fun refreshFromTelecom() {
        val live = try { service?.calls?.toList().orEmpty() }
            catch (_: Exception) { emptyList() }
        live.forEach { call ->
            if (call.state != Call.STATE_DISCONNECTED && !callbacks.containsKey(call)) {
                addCall(call)
            }
        }
        refresh()
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

    private fun primaryCall(): Call? {
        val available = callbacks.keys.filter {
            it.state != Call.STATE_DISCONNECTED && it.state != Call.STATE_DISCONNECTING
        }
        return available.firstOrNull { it.state == Call.STATE_RINGING }
            ?: available.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: available.firstOrNull()
    }

    fun refresh() {
        try {
            refreshSnapshot()
        } catch (error: RuntimeException) {
            // Vendor Telecom implementations can report an incomplete call while binding.
            // Never terminate the phone process because a snapshot is not ready yet.
            android.util.Log.e("SCallBridge", "Unable to read Telecom call snapshot", error)
            mutable.value = null
        }
    }

    private fun refreshSnapshot() {
        val call = primaryCall()
        if (call == null) {
            mutable.value = null
            return
        }
        val details = call.details
        @Suppress("DEPRECATION")
        val audio = try { service?.callAudioState } catch (_: Exception) { null }
        val modern = Build.VERSION.SDK_INT >= 34
        val endpoint = if (modern) latestEndpoint ?: try { service?.currentCallEndpoint }
            catch (_: Exception) { null }
            else null
        val routes = if (modern) {
            latestEndpoints.map { option ->
                CallAudioRoute(
                    key = "modern:${option.identifier}",
                    title = option.endpointName.toString().ifBlank { "Audio device" },
                    kind = when (option.endpointType) {
                        CallEndpoint.TYPE_EARPIECE -> "earpiece"
                        CallEndpoint.TYPE_WIRED_HEADSET -> "wired"
                        CallEndpoint.TYPE_BLUETOOTH -> "bluetooth"
                        CallEndpoint.TYPE_SPEAKER -> "speaker"
                        CallEndpoint.TYPE_STREAMING -> "streaming"
                        else -> "other"
                    }
                )
            }
        } else legacyAudioRoutes(audio?.supportedRouteMask ?: 0)
        val activeKind = if (modern) when (endpoint?.endpointType) {
            CallEndpoint.TYPE_EARPIECE -> "earpiece"
            CallEndpoint.TYPE_WIRED_HEADSET -> "wired"
            CallEndpoint.TYPE_BLUETOOTH -> "bluetooth"
            CallEndpoint.TYPE_SPEAKER -> "speaker"
            CallEndpoint.TYPE_STREAMING -> "streaming"
            else -> "other"
        } else when (audio?.route) {
            CallAudioState.ROUTE_EARPIECE -> "earpiece"
            CallAudioState.ROUTE_WIRED_HEADSET -> "wired"
            CallAudioState.ROUTE_BLUETOOTH -> "bluetooth"
            CallAudioState.ROUTE_SPEAKER -> "speaker"
            else -> "other"
        }
        val routeLabel = if (modern) endpoint?.endpointName?.toString()?.takeIf { it.isNotBlank() }
            ?: routeKindLabel(activeKind)
            else routeKindLabel(activeKind)
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
            isMuted = if (modern) latestMute ?: audio?.isMuted ?: false
                else audio?.isMuted ?: false,
            speakerOn = activeKind == "speaker",
            bluetoothAvailable = routes.any { it.kind == "bluetooth" },
            connectedAtMillis = if (connecting) {
                details.connectTimeMillis.takeIf { it > 0 }
                    ?: earlier?.connectedAtMillis?.takeIf { it > 0 }
                    ?: System.currentTimeMillis()
            } else 0L,
            accountLabel = accountLabel,
            audioRouteLabel = routeLabel,
            availableAudioRoutes = routes,
            audioRouteError = routeError
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

    fun toggleMute() {
        val state = mutable.value ?: return
        try {
            service?.setMuted(!state.isMuted)
        } catch (_: Exception) {
            routeError = "Unable to change microphone mute; try again"
            refresh()
        }
    }

    /**
     * Use Android Telecom rather than AudioManager: the OS owns the cellular
     * microphone, audio focus, Bluetooth profile and speaker routing.
     */
    fun changeAudioRoute(key: String) {
        val owner = service ?: return
        val state = mutable.value ?: return
        if (state.availableAudioRoutes.none { it.key == key }) {
            routeError = "This audio device disconnected. Choose another."
            refresh()
            return
        }
        routeError = null
        if (Build.VERSION.SDK_INT >= 34) {
            val endpoint = latestEndpoints.firstOrNull {
                key == "modern:${it.identifier}"
            }
            if (endpoint == null) {
                routeError = "Audio route not ready. Try again."
                refresh()
                return
            }
            try {
                owner.requestCallEndpointChange(
                    endpoint, owner.mainExecutor,
                    object : OutcomeReceiver<Void, CallEndpointException> {
                        override fun onResult(result: Void?) {
                            routeError = null
                            refresh()
                        }
                        override fun onError(error: CallEndpointException) {
                            routeError = "Audio route switch failed. Try another output."
                            refresh()
                        }
                    }
                )
            } catch (_: Exception) {
                routeError = "Unable to switch audio. Try another output."
            }
        } else {
            val choice = state.availableAudioRoutes.firstOrNull { it.key == key }
            val route = when (choice?.kind) {
                "earpiece" -> CallAudioState.ROUTE_EARPIECE
                "wired" -> CallAudioState.ROUTE_WIRED_HEADSET
                "bluetooth" -> CallAudioState.ROUTE_BLUETOOTH
                "speaker" -> CallAudioState.ROUTE_SPEAKER
                else -> null
            }
            if (route != null) {
                try {
                    @Suppress("DEPRECATION")
                    owner.setAudioRoute(route)
                } catch (_: Exception) {
                    routeError = "Unable to switch audio. Try another output."
                }
            }
        }
        refresh()
    }

    fun toggleSpeaker() {
        val state = mutable.value ?: return
        val destination = if (state.speakerOn) nonSpeakerFallback(state.availableAudioRoutes)
            else state.availableAudioRoutes.firstOrNull { it.kind == "speaker" }
        if (destination != null) changeAudioRoute(destination.key)
        else {
            routeError = "No supported audio output is available"
            refresh()
        }
    }

    fun selectBluetooth() {
        val destination = mutable.value?.availableAudioRoutes
            ?.firstOrNull { it.kind == "bluetooth" }
        if (destination != null) changeAudioRoute(destination.key)
        else {
            routeError = "Connect a Bluetooth headset to use Bluetooth audio"
            refresh()
        }
    }

    fun clearAudioRouteError() {
        routeError = null
        refresh()
    }

    fun updateAvailableEndpoints(endpoints: List<CallEndpoint>) {
        latestEndpoints = endpoints.toList()
        routeError = null
        refresh()
    }

    fun updateSelectedEndpoint(endpoint: CallEndpoint) {
        latestEndpoint = endpoint
        routeError = null
        refresh()
    }

    fun updateMutedState(muted: Boolean) {
        latestMute = muted
        refresh()
    }

    fun dtmf(digit: Char) {
        primaryCall()?.let { call ->
            call.playDtmfTone(digit)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                call.stopDtmfTone()
            }, 130)
        }
    }
}
