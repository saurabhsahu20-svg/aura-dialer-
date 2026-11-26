package com.scall.phone.telecom

import android.telecom.Call
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallAlertSignatureTest {
    private val ringing = CallAlertSignature(1, Call.STATE_RINGING, "S Caller")

    @Test fun firstIncomingCallShowsAlert() {
        assertTrue(needsCallNotificationUpdate(null, ringing))
    }

    @Test fun repeatedStateChecksMustNotRetriggerTheSameAlert() {
        assertFalse(needsCallNotificationUpdate(ringing, ringing))
    }

    @Test fun newCallAndCallerNameChangesRefreshOneExistingAlert() {
        assertTrue(needsCallNotificationUpdate(ringing, ringing.copy(callIdentity = 2)))
        assertTrue(needsCallNotificationUpdate(ringing, ringing.copy(caller = "Saved Contact")))
        assertTrue(needsCallNotificationUpdate(ringing,
            ringing.copy(state = Call.STATE_ACTIVE)))
    }
}
