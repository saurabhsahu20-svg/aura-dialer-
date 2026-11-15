package com.scall.phone.ui

/** Single deterministic threshold rule shared by contact and incoming-call gestures. */
internal enum class SwipeAction { NONE, POSITIVE, NEGATIVE }

internal fun chooseSwipeAction(offsetPx: Float, thresholdPx: Float): SwipeAction = when {
    !offsetPx.isFinite() || !thresholdPx.isFinite() || thresholdPx <= 0f -> SwipeAction.NONE
    offsetPx >= thresholdPx -> SwipeAction.POSITIVE
    offsetPx <= -thresholdPx -> SwipeAction.NEGATIVE
    else -> SwipeAction.NONE
}
