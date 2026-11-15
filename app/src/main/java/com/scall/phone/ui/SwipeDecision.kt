package com.scall.phone.ui

/** Strict travel distances; neither slide triggers before most of its available path. */
internal const val CONTACT_SLIDE_FRACTION = 0.68f
internal const val INCOMING_SLIDE_FRACTION = 0.82f

/** Required movement in px, capped to (0, travel]. Invalid geometry never triggers a swipe. */
internal fun requiredSlideDistance(travelPx: Float, fraction: Float): Float =
    if (!travelPx.isFinite() || travelPx <= 0f || !fraction.isFinite() ||
        fraction <= 0.5f || fraction > 1f) Float.POSITIVE_INFINITY
    else travelPx * fraction

/** Single deterministic threshold rule shared by contact and incoming-call gestures. */
internal enum class SwipeAction { NONE, POSITIVE, NEGATIVE }

internal fun chooseSwipeAction(offsetPx: Float, thresholdPx: Float): SwipeAction = when {
    !offsetPx.isFinite() || !thresholdPx.isFinite() || thresholdPx <= 0f -> SwipeAction.NONE
    offsetPx >= thresholdPx -> SwipeAction.POSITIVE
    offsetPx <= -thresholdPx -> SwipeAction.NEGATIVE
    else -> SwipeAction.NONE
}
