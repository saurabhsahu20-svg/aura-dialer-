package com.scall.phone.telecom

import android.telecom.CallAudioState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRoutingTest {
    @Test fun onlyAvailableHardwareRoutesAreShown() {
        val mask = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER
        val choices = legacyAudioRoutes(mask)
        assertEquals(listOf("earpiece", "speaker"), choices.map { it.kind })
        assertFalse(choices.any { it.kind == "bluetooth" })
    }

    @Test fun wiredOutputIsPreferredWhenDisablingSpeaker() {
        val mask = CallAudioState.ROUTE_EARPIECE or
            CallAudioState.ROUTE_WIRED_HEADSET or CallAudioState.ROUTE_SPEAKER
        val choice = nonSpeakerFallback(legacyAudioRoutes(mask))
        assertEquals("wired", choice?.kind)
    }

    @Test fun disconnectedBluetoothIsNotOffered() {
        assertNull(nonSpeakerFallback(legacyAudioRoutes(CallAudioState.ROUTE_SPEAKER)))
    }

    @Test fun correctLabelsForHardware() {
        assertEquals("Bluetooth", routeKindLabel("bluetooth"))
        assertEquals("Wired headset", routeKindLabel("wired"))
        assertEquals("Earpiece", routeKindLabel("earpiece"))
    }

    @Test fun neverClaimAiActiveOnSimCalls() {
        val label = cellularAudioProcessingLabel()
        assertTrue(label.contains("carrier controlled"))
        assertFalse(label.contains("AI ON"))
    }
}
