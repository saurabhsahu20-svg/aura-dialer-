package com.scall.phone

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SCallCrashDiagnosticsTest {
    @Test fun savedDiagnosticContainsStackButNotExceptionMessage() {
        val secretNumber = "9999988888"
        val error = IllegalArgumentException("Call to $secretNumber failed")
        val report = SCallCrashDiagnostics.render(error, 35)
        assertTrue(report.contains("IllegalArgumentException"))
        assertTrue(report.contains("Android API: 35"))
        assertFalse(report.contains(secretNumber))
    }

    @Test fun diagnosticKeepsRootCauseClassWithoutPrivateMessage() {
        val cause = IllegalStateException("Contact number 7777777777")
        val error = RuntimeException("Private name", cause)
        val report = SCallCrashDiagnostics.render(error, 33)
        assertTrue(report.contains("IllegalStateException"))
        assertFalse(report.contains("Private name"))
        assertFalse(report.contains("7777777777"))
    }
}
