package com.scall.phone.telecom

import android.telecom.Call
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression tests for ringing calls vibrating without any visible caller UI. */
class IncomingCallNotificationPolicyTest {
    @Test fun initialStateNeverPretendsToBeAnOngoingCall() {
        assertEquals(VisibleCallAlert.NOT_READY, visibleCallAlert(Call.STATE_NEW))
        assertEquals(VisibleCallAlert.NOT_READY, visibleCallAlert(Call.STATE_DISCONNECTED))
        assertEquals(VisibleCallAlert.NOT_READY, visibleCallAlert(Call.STATE_DISCONNECTING))
        assertNull(primaryCallNotificationIndex(listOf(Call.STATE_NEW)))
    }

    @Test fun ringingMustBeAnIncomingCallNotification() {
        assertEquals(VisibleCallAlert.INCOMING, visibleCallAlert(Call.STATE_RINGING))
        assertEquals(0, primaryCallNotificationIndex(
            listOf(Call.STATE_RINGING, Call.STATE_ACTIVE)
        ))
    }

    @Test fun ringingAlwaysWinsOverActiveAndStaleDisconnectedCalls() {
        assertEquals(2, primaryCallNotificationIndex(
            listOf(Call.STATE_DISCONNECTED, Call.STATE_ACTIVE, Call.STATE_RINGING)
        ))
    }

    @Test fun activeCallCanDisplayOngoingNotification() {
        assertEquals(VisibleCallAlert.ONGOING, visibleCallAlert(Call.STATE_ACTIVE))
        assertEquals(VisibleCallAlert.ONGOING, visibleCallAlert(Call.STATE_DIALING))
        assertEquals(VisibleCallAlert.ONGOING, visibleCallAlert(Call.STATE_CONNECTING))
        assertEquals(VisibleCallAlert.ONGOING, visibleCallAlert(Call.STATE_HOLDING))
    }

    @Test fun hiddenDisconnectedCallNeverCancelsVisibleSecondCall() {
        assertEquals(1, primaryCallNotificationIndex(
            listOf(Call.STATE_DISCONNECTED, Call.STATE_ACTIVE)
        ))
        assertNull(primaryCallNotificationIndex(listOf(
            Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING
        )))
    }

    @Test fun delayedRingingWillBeRecognizedOnNextStateCheck() {
        assertNull(primaryCallNotificationIndex(listOf(Call.STATE_NEW)))
        assertEquals(0, primaryCallNotificationIndex(listOf(Call.STATE_RINGING)))
    }
}
