package com.scall.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeDecisionTest {
    @Test fun insufficientMovementNeverCalls() {
        assertEquals(SwipeAction.NONE, chooseSwipeAction(0f, 100f))
        assertEquals(SwipeAction.NONE, chooseSwipeAction(99.9f, 100f))
        assertEquals(SwipeAction.NONE, chooseSwipeAction(-99.9f, 100f))
    }

    @Test fun thresholdCrossingHasCorrectDirection() {
        assertEquals(SwipeAction.POSITIVE, chooseSwipeAction(100f, 100f))
        assertEquals(SwipeAction.POSITIVE, chooseSwipeAction(140f, 100f))
        assertEquals(SwipeAction.NEGATIVE, chooseSwipeAction(-100f, 100f))
        assertEquals(SwipeAction.NEGATIVE, chooseSwipeAction(-140f, 100f))
    }

    @Test fun invalidValuesNeverTriggerActions() {
        assertEquals(SwipeAction.NONE, chooseSwipeAction(Float.NaN, 100f))
        assertEquals(SwipeAction.NONE, chooseSwipeAction(Float.POSITIVE_INFINITY, 100f))
        assertEquals(SwipeAction.NONE, chooseSwipeAction(110f, 0f))
    }
}
