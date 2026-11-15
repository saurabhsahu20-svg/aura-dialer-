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

    @Test fun contactMustMoveMoreThanHalfOfEntireRow() {
        val widthPx = 400f
        val threshold = requiredSlideDistance(widthPx, CONTACT_SLIDE_FRACTION)
        assertEquals(272f, threshold, 0.01f)
        assertEquals(SwipeAction.NONE, chooseSwipeAction(260f, threshold))
        assertEquals(SwipeAction.POSITIVE, chooseSwipeAction(275f, threshold))
        assertEquals(SwipeAction.NEGATIVE, chooseSwipeAction(-275f, threshold))
    }

    @Test fun incomingSliderRequiresMostOfItsAvailableTravel() {
        val travelPx = 150f
        val threshold = requiredSlideDistance(travelPx, INCOMING_SLIDE_FRACTION)
        assertEquals(123f, threshold, 0.01f)
        assertEquals(SwipeAction.NONE, chooseSwipeAction(120f, threshold))
        assertEquals(SwipeAction.POSITIVE, chooseSwipeAction(124f, threshold))
        assertEquals(SwipeAction.NEGATIVE, chooseSwipeAction(-124f, threshold))
    }

    @Test fun malformedGestureGeometryNeverTriggersCall() {
        assertEquals(SwipeAction.NONE, chooseSwipeAction(300f, requiredSlideDistance(Float.NaN, CONTACT_SLIDE_FRACTION)))
        assertEquals(SwipeAction.NONE, chooseSwipeAction(300f, requiredSlideDistance(300f, 0.5f)))
    }
}
