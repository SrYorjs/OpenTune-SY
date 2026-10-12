package com.arturo254.opentune.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerSwipePolicyTest {
    @Test
    fun sensitivityUsesUnchangedLogisticThresholds() {
        assertEquals(600, MiniPlayerSwipePolicy.autoSwipeThreshold(0f))
        assertEquals(579, MiniPlayerSwipePolicy.autoSwipeThreshold(0.5f))
        assertEquals(368, MiniPlayerSwipePolicy.autoSwipeThreshold(0.75f))
        assertEquals(50, MiniPlayerSwipePolicy.autoSwipeThreshold(1f))
    }

    @Test
    fun rtlMirrorsDragAndSkipAvailabilityBlocksInvalidDirections() {
        assertEquals(-18f, MiniPlayerSwipePolicy.adjustedDrag(18f, isRtl = true), 0f)
        assertEquals(18f, MiniPlayerSwipePolicy.adjustedDrag(18f, isRtl = false), 0f)
        assertTrue(MiniPlayerSwipePolicy.canConsumeDrag(-10f, false, true))
        assertTrue(MiniPlayerSwipePolicy.canConsumeDrag(10f, true, false))
        assertFalse(MiniPlayerSwipePolicy.canConsumeDrag(-10f, true, false))
        assertFalse(MiniPlayerSwipePolicy.canConsumeDrag(10f, false, true))
        assertFalse(MiniPlayerSwipePolicy.canConsumeDrag(0f, true, true))
    }

    @Test
    fun shortFastSwipeNeedsStrictlyMoreThanFiftyPixelsAndEnoughVelocity() {
        // Original behavior: 100 px in 10 ms exceeds the 8.5 px/ms threshold at sensitivity 0.
        assertTrue(MiniPlayerSwipePolicy.shouldChangeSong(100f, 100f, 10L, 0f))
        assertFalse(MiniPlayerSwipePolicy.shouldChangeSong(50f, 500f, 10L, 0f))
        assertFalse(MiniPlayerSwipePolicy.shouldChangeSong(100f, 100f, 50L, 0f))
        assertFalse(MiniPlayerSwipePolicy.shouldChangeSong(100f, 100f, 0L, 0f))
    }

    @Test
    fun longSlowSwipeUsesAutoThresholdAndMirrorsPreviousDirection() {
        assertFalse(MiniPlayerSwipePolicy.shouldChangeSong(600f, 600f, 1000L, 0f))
        assertTrue(MiniPlayerSwipePolicy.shouldChangeSong(601f, 601f, 1000L, 0f))
        assertTrue(MiniPlayerSwipePolicy.shouldChangeSong(-601f, 601f, 1000L, 0f))
        assertTrue(MiniPlayerSwipePolicy.shouldChangeSong(51f, 51f, 1000L, 1f))
    }

    @Test
    fun gestureDecisionNeverNeedsPlayerOrComposeRuntime() {
        val policy = MiniPlayerSwipePolicy
        assertFalse(policy.canConsumeDrag(20f, false, false))
        assertFalse(policy.shouldChangeSong(0f, 0f, 1L, 0.5f))
    }
}
