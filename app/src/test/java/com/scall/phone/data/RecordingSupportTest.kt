package com.scall.phone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingSupportTest {
    @Test fun simAutoRecordingNeverFalselyAdvertised() {
        assertFalse(canAutomaticallyRecordCellularCalls())
    }
    @Test fun retentionKeepForeverNeverExpires() {
        assertFalse(recordingRetentionExpired(1L, Long.MAX_VALUE, 0))
    }
    @Test fun retentionChecks30DayCutoff() {
        val day = 24L * 60 * 60 * 1000
        val now = 100L * day
        assertFalse(recordingRetentionExpired(now - 29L * day, now, 30))
        assertTrue(recordingRetentionExpired(now - 31L * day, now, 30))
    }
    @Test fun futureImportedFileNotAccidentallyDeleted() {
        assertFalse(recordingRetentionExpired(300_000, 200_000, 30))
    }
    @Test fun titleIsCleanAndBounded() {
        assertEquals("Imported recording", safeRecordingTitle("   "))
        assertEquals(90, safeRecordingTitle("a".repeat(300)).length)
    }
}
