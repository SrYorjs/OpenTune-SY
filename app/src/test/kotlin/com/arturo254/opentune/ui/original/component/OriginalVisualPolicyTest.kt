package com.arturo254.opentune.ui.original.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import com.arturo254.opentune.ui.original.utils.rubberBand

class OriginalVisualPolicyTest {
    @Test
    fun ambientMotionStopsForEveryInactiveOrReducedMotionCondition() {
        assertTrue(originalAmbientMotionAllowed(true, true, true, 1f))
        assertFalse(originalAmbientMotionAllowed(false, true, true, 1f))
        assertFalse(originalAmbientMotionAllowed(true, false, true, 1f))
        assertFalse(originalAmbientMotionAllowed(true, true, false, 1f))
        assertFalse(originalAmbientMotionAllowed(true, true, true, 0f))
    }

    @Test
    fun originalOverscrollIsSymmetricAndBounded() {
        assertEquals(0f, rubberBand(0f, 100f), 0f)
        for (distance in listOf(1f, 10f, 100f, 1000f)) {
            val positive = rubberBand(distance, 100f)
            assertTrue(positive > 0f && positive < 100f)
            assertEquals(-positive, rubberBand(-distance, 100f), 0.0001f)
        }
    }
}
