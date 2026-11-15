package com.scall.phone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SimCallRouteTest {
    @Test fun savedAccountIsUsedPrecisely() {
        assertEquals(CallRoute.SPECIFIC_ACCOUNT, chooseCallRoute("sim2", listOf("sim1", "sim2"), 2))
    }
    @Test fun missingAccountRequiresPicker() {
        assertEquals(CallRoute.ASK_USER, chooseCallRoute("gone", listOf("sim1", "sim2"), 2))
    }
    @Test fun missingAccountNeverFallsBackToAnotherSIM() {
        assertEquals(CallRoute.SETUP_REQUIRED, chooseCallRoute("gone", emptyList(), 2))
    }
    @Test fun hiddenTelecomAccountsWithTwoSIMsRequireSetup() {
        assertEquals(CallRoute.SETUP_REQUIRED, chooseCallRoute(SimPreferences.ASK, emptyList(), 2))
    }
    @Test fun askOnTwoAccountsShowsPicker() {
        assertEquals(CallRoute.ASK_USER, chooseCallRoute(SimPreferences.ASK, listOf("sim1", "sim2"), 2))
    }
    @Test fun systemDefaultIsAnExplicitChoice() {
        assertEquals(CallRoute.ANDROID_DEFAULT, chooseCallRoute(SimPreferences.SYSTEM, listOf("sim1", "sim2"), 2))
    }
    @Test fun onlyOneAccountRoutesWithoutPicker() {
        assertEquals(CallRoute.SPECIFIC_ACCOUNT, chooseCallRoute(SimPreferences.ASK, listOf("sim1"), 1))
    }
}
