package com.scall.phone.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoPreferencesSafetyTest {
    @Test fun corruptedNanZoomUsesSafeDefault() {
        assertEquals(1f, boundedPhotoValue(Float.NaN, 1f, 1f, 2.4f), 0f)
    }

    @Test fun infiniteOffsetsCannotReachGraphicsLayer() {
        assertEquals(0f, boundedPhotoValue(Float.POSITIVE_INFINITY, 0f, -0.45f, 0.45f), 0f)
        assertEquals(0f, boundedPhotoValue(Float.NEGATIVE_INFINITY, 0f, -0.45f, 0.45f), 0f)
    }

    @Test fun savedZoomAndDimmingAreClamped() {
        assertEquals(2.4f, boundedPhotoValue(100f, 1f, 1f, 2.4f), 0f)
        assertEquals(0.14f, boundedPhotoValue(-2f, 0.34f, 0.14f, 0.64f), 0f)
    }

    @Test fun validPhotoStyleIsNotAltered() {
        assertEquals(1.4f, boundedPhotoValue(1.4f, 1f, 1f, 2.4f), 0f)
        assertEquals(-0.2f, boundedPhotoValue(-0.2f, 0f, -0.45f, 0.45f), 0f)
    }
}
