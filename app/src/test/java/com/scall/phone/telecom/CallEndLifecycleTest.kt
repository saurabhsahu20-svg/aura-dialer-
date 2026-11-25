package com.scall.phone.telecom

import android.telecom.Call
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Regression: a disconnected/removed Telecom call must not keep the call UI open. */
class CallEndLifecycleTest {
    @Test fun liveActiveCallIsRetained() {
        assertTrue(retainTelecomCallCallback(true, Call.STATE_ACTIVE))
    }

    @Test fun connectingCallIsNotMistakenForACompletedHangUp() {
        assertTrue(retainTelecomCallCallback(true, Call.STATE_CONNECTING))
    }

    @Test fun telecomDisconnectionClearsCallback() {
        assertFalse(retainTelecomCallCallback(true, Call.STATE_DISCONNECTED))
    }

    @Test fun missingTelecomCallCannotKeepAnOldScreenAlive() {
        assertFalse(retainTelecomCallCallback(false, Call.STATE_ACTIVE))
        assertFalse(retainTelecomCallCallback(false, Call.STATE_DIALING))
    }

    @Test fun transientDisconnectingCallMayRemainUntilTelecomRemovesIt() {
        assertTrue(retainTelecomCallCallback(true, Call.STATE_DISCONNECTING))
    }
}
