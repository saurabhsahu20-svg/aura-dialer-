package com.scall.phone.telecom

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlipToSilenceDetectorTest {
    @Test fun normalFlipSilencesAfterStableHoldExactlyOnce() {
        val sensor = FaceDownSilenceDetector()
        assertFalse(sensor.onReading(9.8f, 0))
        assertFalse(sensor.onReading(-9.1f, 100))
        assertFalse(sensor.onReading(-9.2f, 700))
        assertTrue(sensor.onReading(-9.3f, 900))
        assertFalse(sensor.onReading(-9.3f, 2000))
    }

    @Test fun initiallyFaceDownPhoneDoesNotAutomaticallySilenceCall() {
        val sensor = FaceDownSilenceDetector()
        assertFalse(sensor.onReading(-9.3f, 100))
        assertFalse(sensor.onReading(-9.3f, 3000))
        assertFalse(sensor.onReading(8.5f, 4000))
        assertFalse(sensor.onReading(-9.1f, 4100))
        assertTrue(sensor.onReading(-9.2f, 4900))
    }

    @Test fun quickFlipOrAmbiguousAngleNeverSilences() {
        val sensor = FaceDownSilenceDetector()
        assertFalse(sensor.onReading(9.1f, 0))
        assertFalse(sensor.onReading(-9.3f, 100))
        assertFalse(sensor.onReading(1.2f, 500))
        assertFalse(sensor.onReading(-9.0f, 600))
        assertFalse(sensor.onReading(-6.2f, 1250))
        assertFalse(sensor.onReading(-9.0f, 1350))
        assertTrue(sensor.onReading(-9.0f, 2150))
    }
}
