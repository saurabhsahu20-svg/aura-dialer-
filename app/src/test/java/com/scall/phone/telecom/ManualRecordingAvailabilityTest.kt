package com.scall.phone.telecom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualRecordingAvailabilityTest {
    @Test fun noActiveCallMeansNoCapture() {
        assertEquals(ManualRecordAccess.NEED_ACTIVE_CALL,
            manualRecordingAccess(false, true, true))
    }

    @Test fun normalThirdPartyAppNeverClaimsNativeCellularCapture() {
        assertEquals(ManualRecordAccess.SYSTEM_AUDIO_UNAVAILABLE,
            manualRecordingAccess(true, false, true))
    }

    @Test fun privilegedSystemBuildStillNeedsMicrophonePermission() {
        assertEquals(ManualRecordAccess.NEED_MIC_PERMISSION,
            manualRecordingAccess(true, true, false))
    }

    @Test fun captureEnabledOnlyAfterEveryPrecondition() {
        assertEquals(ManualRecordAccess.AVAILABLE,
            manualRecordingAccess(true, true, true))
    }

    @Test fun recordingStatusDefaultsToIdleNotRecording() {
        val state = RecordingStatus()
        assertEquals(RecordingStatus.Phase.IDLE, state.phase)
        assertEquals(0L, state.startedElapsedMs)
        assertFalse(state.phase == RecordingStatus.Phase.RECORDING)
    }
}
