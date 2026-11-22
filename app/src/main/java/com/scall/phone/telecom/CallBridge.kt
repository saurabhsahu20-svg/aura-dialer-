package com.scall.phone.telecom

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class EndCallProgress { IDLE, ENDING, STILL_ACTIVE }

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
    private val endProgressMutable = MutableStateFlow(EndCallProgress.IDLE)
    val endProgress = endProgressMutable.asStateFlow()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var endingTarget: Call? = null
    private var endRequestSequence = 0
    private var endingSinceMillis = 0L

    private fun safelyUnregister(call: Call, callback: Call.Callback) {
        try {
            call.unregisterCallback(callback)
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallBridge", "Call callback already removed", error)
        }
    }

    private inline fun safeCallAction(name: String, action: () -> Unit) {
        try {
            action()
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallBridge", "Telecom action failed: $name", error)
            refresh()
        }
    }

    fun bind(inCallService: InCallService) {
        if (service !== inCallService && service != null) {
            // A replacement Telecom binding must not inherit callbacks from
            // the older service. The old service's onDestroy can arrive later.
            callbacks.toMap().forEach { (call, callback) ->
                safelyUnregister(call, callback)
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
            safelyUnregister(call, callback)
        }
        callbacks.clear()
        clearEnding()
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
            try {
                if (call.state != Call.STATE_DISCONNECTED && !callbacks.containsKey(call)) {
                    addCall(call)
                }
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallBridge", "Unable to register active Telecom call", error)
            }
        }
        refresh()
    }


    fun addCall(call: Call) {
        val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                if (call === endingTarget &&
                    (state == Call.STATE_DISCONNECTING || state == Call.STATE_DISCONNECTED)) {
                    clearEnding()
                }
                refresh()
            }
            override fun onDetailsChanged(call: Call, details: Call.Details) = refresh()
            override fun onCallDestroyed(call: Call) = removeCall(call)
            override fun onChildrenChanged(call: Call, children: MutableList<Call>) = refresh()
        }
        if (!callbacks.containsKey(call)) {
            try {
                call.registerCallback(callback)
                callbacks[call] = callback
            } catch (error: RuntimeException) {
                android.util.Log.w("SCallBridge", "Unable to observe Telecom call", error)
            }
        }
        refresh()
    }

    fun removeCall(call: Call) {
        callbacks.remove(call)?.let { callback -> safelyUnregister(call, callback) }
        if (call === endingTarget) clearEnding()
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
            // A temporarily unreadable OEM call snapshot must not blank a live call UI.
            if (callbacks.isEmpty()) mutable.value = null
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

    // Android Telecom transitions asynchronously. Ignore accidental rapid repeat
    // actions for the SAME call, but never block a different incoming call.
    private var lastRingingActionCall: Call? = null
    private var lastRingingActionAt = 0L

    private inline fun ringingAction(action: (Call) -> Unit) {
        val call = primaryCall()?.takeIf { it.state == Call.STATE_RINGING } ?: return
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastRingingActionCall === call && now - lastRingingActionAt < 1200L) return
        lastRingingActionCall = call
        lastRingingActionAt = now
        try {
            action(call)
        } catch (error: RuntimeException) {
            lastRingingActionCall = null
            android.util.Log.w("SCallBridge", "Ringing action failed", error)
            refresh()
        }
    }

    fun answer() = ringingAction { it.answer(VideoProfile.STATE_AUDIO_ONLY) }

    fun reject() = ringingAction { it.reject(false, null) }

    /**
     * Some OEM Telecom stacks acknowledge disconnect asynchronously.
     * Try immediately, retry ONLY this same call once, and show a manual
     * retry if Android still reports an active call. Never fake an end state.
     */
    fun disconnect() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { disconnect() }
            return
        }
        val liveCalls = (runCatching { service?.calls?.toList().orEmpty() }
            .getOrDefault(emptyList()) + callbacks.keys)
            .distinct()
            .filter { it.state != Call.STATE_DISCONNECTED && it.state != Call.STATE_DISCONNECTING }
        val call = liveCalls.firstOrNull { it.state == Call.STATE_ACTIVE }
            ?: liveCalls.firstOrNull {
                it.state == Call.STATE_DIALING || it.state == Call.STATE_CONNECTING
            }
            ?: liveCalls.firstOrNull { it.state == Call.STATE_HOLDING }
            ?: liveCalls.firstOrNull { it.state == Call.STATE_RINGING }
            ?: liveCalls.firstOrNull()
        if (call == null) {
            refreshFromTelecom()
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (endingTarget === call && endProgressMutable.value == EndCallProgress.ENDING &&
            now - endingSinceMillis < 2800L) return

        endingTarget = call
        endingSinceMillis = now
        endProgressMutable.value = EndCallProgress.ENDING
        val requestId = ++endRequestSequence

        fun tryDisconnect(): Boolean = try {
            if (call.state == Call.STATE_DISCONNECTING ||
                call.state == Call.STATE_DISCONNECTED) {
                clearEnding()
                true
            } else {
                call.disconnect()
                true
            }
        } catch (error: RuntimeException) {
            android.util.Log.w("SCallBridge", "Telecom did not accept End Call", error)
            false
        }

        if (!tryDisconnect()) {
            endProgressMutable.value = EndCallProgress.STILL_ACTIVE
            refreshFromTelecom()
            return
        }

        mainHandler.postDelayed({
            if (requestId != endRequestSequence || endingTarget !== call) return@postDelayed
            if (call.state == Call.STATE_DISCONNECTED ||
                call.state == Call.STATE_DISCONNECTING) {
                clearEnding()
                return@postDelayed
            }
            // A delayed retry must never disconnect another incoming call.
            val stillPresent = runCatching { service?.calls?.contains(call) == true }
                .getOrDefault(false) || callbacks.containsKey(call)
            if (!stillPresent) {
                clearEnding()
                return@postDelayed
            }
            if (!tryDisconnect()) {
                endProgressMutable.value = EndCallProgress.STILL_ACTIVE
            }
            refresh()
        }, 900L)

        mainHandler.postDelayed({
            if (requestId != endRequestSequence || endingTarget !== call) return@postDelayed
            val stillPresent = runCatching { service?.calls?.contains(call) == true }
                .getOrDefault(false) || callbacks.containsKey(call)
            if (call.state == Call.STATE_DISCONNECTED ||
                call.state == Call.STATE_DISCONNECTING || !stillPresent) {
                clearEnding()
            } else {
                endProgressMutable.value = EndCallProgress.STILL_ACTIVE
                refresh()
            }
        }, 2300L)
    }

    private fun clearEnding() {
        endRequestSequence++
        endingTarget = null
        endProgressMutable.value = EndCallProgress.IDLE
    }

    fun holdOrUnhold() = safeCallAction("hold") {
        primaryCall()?.let { call ->
            when (call.state) {
                Call.STATE_ACTIVE -> if (call.details.can(Call.Details.CAPABILITY_HOLD)) call.hold()
                Call.STATE_HOLDING -> call.unhold()
            }
        }
    }

    fun merge() = safeCallAction("merge") {
        val call = primaryCall() ?: return@safeCallAction
        val other = call.conferenceableCalls.firstOrNull()
        if (other != null) call.conference(other) else call.mergeConference()
    }

    fun addCall() = safeCallAction("add call") {
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

    fun dtmf(digit: Char) = safeCallAction("DTMF") {
        primaryCall()?.let { call ->
            call.playDtmfTone(digit)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    call.stopDtmfTone()
                } catch (error: RuntimeException) {
                    android.util.Log.w("SCallBridge", "Call ended during DTMF tone", error)
                }
            }, 130)
        }
    }
}
