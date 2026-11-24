package com.scall.phone.telecom

import android.telecom.Call
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallProximityPolicyTest {
    @Test fun activeEarpieceCallIsProtected() {
        assertTrue(shouldGuardCallTouches(Call.STATE_ACTIVE, "earpiece"))
    }

    @Test fun outgoingEarpieceRingingIsProtected() {
        assertTrue(shouldGuardCallTouches(Call.STATE_DIALING, "earpiece"))
        assertTrue(shouldGuardCallTouches(Call.STATE_CONNECTING, "earpiece"))
    }

    @Test fun incomingCallRemainsAnswerable() {
        assertFalse(shouldGuardCallTouches(Call.STATE_RINGING, "earpiece"))
    }

    @Test fun speakerAndHeadsetsAlwaysRemainTouchable() {
        for (route in listOf("speaker", "bluetooth", "wired", "streaming", "other")) {
            assertFalse(shouldGuardCallTouches(Call.STATE_ACTIVE, route))
        }
    }

    @Test fun holdEndedAndMissingCallsNeverKeepTheScreenOff() {
        assertFalse(shouldGuardCallTouches(Call.STATE_HOLDING, "earpiece"))
        assertFalse(shouldGuardCallTouches(Call.STATE_DISCONNECTED, "earpiece"))
        assertFalse(shouldGuardCallTouches(null, "earpiece"))
        assertFalse(shouldGuardCallTouches(Call.STATE_ACTIVE, null))
    }
}
