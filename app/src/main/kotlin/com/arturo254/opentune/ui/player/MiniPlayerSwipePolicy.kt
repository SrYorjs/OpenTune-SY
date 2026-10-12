package com.arturo254.opentune.ui.player

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * The mini-player's ORIGINAL horizontal-swipe policy, moved to pure Kotlin
 * so the exact legacy behavior can be regression-tested in JVM tests.
 *
 * Do not clamp sensitivity or change comparison operators during 4.1:
 * those adjustments would be behavior changes and belong to a later phase.
 */
internal object MiniPlayerSwipePolicy {
    fun autoSwipeThreshold(swipeSensitivity: Float): Int =
        (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()

    fun adjustedDrag(dragAmount: Float, isRtl: Boolean): Float =
        if (isRtl) -dragAmount else dragAmount

    fun canConsumeDrag(
        adjustedDragAmount: Float,
        canSkipPrevious: Boolean,
        canSkipNext: Boolean,
    ): Boolean =
        (adjustedDragAmount < 0 && canSkipNext) ||
            (adjustedDragAmount > 0 && canSkipPrevious)

    fun shouldChangeSong(
        offsetX: Float,
        totalDragDistance: Float,
        dragDurationMs: Long,
        swipeSensitivity: Float,
    ): Boolean {
        val velocity = if (dragDurationMs > 0L) totalDragDistance / dragDurationMs else 0f
        val minDistanceThreshold = 50f
        val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f
        return (abs(offsetX) > minDistanceThreshold && velocity > velocityThreshold) ||
            (abs(offsetX) > autoSwipeThreshold(swipeSensitivity))
    }
}
