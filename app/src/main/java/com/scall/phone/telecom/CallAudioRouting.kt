package com.scall.phone.telecom

import android.telecom.CallAudioState

/**
 * A route offered by Android Telecom. A key is valid only while this route
 * is present in the latest available-devices callback.
 */
internal data class CallAudioRoute(
    val key: String,
    val title: String,
    val kind: String
)

internal fun legacyAudioRoutes(mask: Int): List<CallAudioRoute> = buildList {
    if (mask and CallAudioState.ROUTE_EARPIECE != 0)
        add(CallAudioRoute("legacy:earpiece", "Phone earpiece", "earpiece"))
    if (mask and CallAudioState.ROUTE_WIRED_HEADSET != 0)
        add(CallAudioRoute("legacy:wired", "Wired headset", "wired"))
    if (mask and CallAudioState.ROUTE_BLUETOOTH != 0)
        add(CallAudioRoute("legacy:bluetooth", "Bluetooth headset", "bluetooth"))
    if (mask and CallAudioState.ROUTE_SPEAKER != 0)
        add(CallAudioRoute("legacy:speaker", "Speakerphone", "speaker"))
}

/** Wired headphones remain the safest destination when speaker is switched off. */
internal fun nonSpeakerFallback(routes: List<CallAudioRoute>): CallAudioRoute? =
    routes.firstOrNull { it.kind == "wired" }
        ?: routes.firstOrNull { it.kind == "earpiece" }
        ?: routes.firstOrNull { it.kind == "bluetooth" }

internal fun routeKindLabel(kind: String): String = when (kind) {
    "earpiece" -> "Earpiece"
    "wired" -> "Wired headset"
    "bluetooth" -> "Bluetooth"
    "speaker" -> "Speaker"
    "streaming" -> "External audio"
    else -> "Phone audio"
}
