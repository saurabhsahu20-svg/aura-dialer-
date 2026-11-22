package com.scall.phone.telecom

import android.telecom.Call
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndCallPriorityTest {
    @Test fun endButtonTargetsConnectedConversationBeforeWaitingCaller() {
        assertTrue(endCallPriority(Call.STATE_ACTIVE) < endCallPriority(Call.STATE_RINGING))
    }

    @Test fun placingCallAndHeldCallAreValidEndTargets() {
        assertTrue(endCallPriority(Call.STATE_DIALING) < endCallPriority(Call.STATE_HOLDING))
        assertEquals(endCallPriority(Call.STATE_DIALING),
            endCallPriority(Call.STATE_CONNECTING))
    }

    @Test fun disconnectedCallsAreNeverPreferred() {
        assertTrue(endCallPriority(Call.STATE_RINGING) <
            endCallPriority(Call.STATE_DISCONNECTED))
        assertEquals(99, endCallPriority(Call.STATE_DISCONNECTING))
    }
}
