package com.scall.phone.telecom

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallConnectionLifecycleTest {
    @Test fun oldServiceCannotClearReplacementCall() {
        val oldService = Any()
        val activeService = Any()
        assertFalse(isCurrentTelecomBinding(activeService, oldService))
    }

    @Test fun activeServiceMayCleanupOwnCalls() {
        val service = Any()
        assertTrue(isCurrentTelecomBinding(service, service))
    }

    @Test fun noActiveServiceCannotClearAnything() {
        assertFalse(isCurrentTelecomBinding(null, Any()))
    }
}
