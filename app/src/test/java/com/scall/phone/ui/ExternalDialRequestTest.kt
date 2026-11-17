package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/** A repeated ACTION_DIAL with identical data must be treated as a new request. */
class ExternalDialRequestTest {
    @Test fun repeatedNumberCanTriggerTwoIndependentRequests() {
        val first = nextExternalDialRequestId(0)
        val second = nextExternalDialRequestId(first)
        assertEquals(1, first)
        assertEquals(2, second)
    }

    @Test fun wrapAroundAlwaysProducesAnActionableRequest() {
        assertEquals(1, nextExternalDialRequestId(Int.MAX_VALUE))
        assertEquals(1, nextExternalDialRequestId(-1))
    }

    @Test fun requestIdIsMonotonicWithinNormalLifetime() {
        assertEquals(34, nextExternalDialRequestId(33))
    }
}
