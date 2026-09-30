package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class MixTransitionMathTest {
    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.0001f) {
        assertEquals(expected.toDouble(), actual.toDouble(), tolerance.toDouble())
    }

    @Test
    fun requiredStartBuffer_clampsToExistingPlaybackPolicy() {
        assertEquals(3_000L, MixTransitionMath.requiredStartBufferMs(0))
        assertEquals(3_500L, MixTransitionMath.requiredStartBufferMs(1_500))
        assertEquals(8_000L, MixTransitionMath.requiredStartBufferMs(6_000))
        assertEquals(10_000L, MixTransitionMath.requiredStartBufferMs(20_000))
    }

    @Test
    fun everyCurve_startsWithOutgoingOnly_andEndsWithIncomingOnly() {
        MixTransitionCurve.entries.forEach { curve ->
            val start = MixTransitionMath.gains(0f, curve)
            val end = MixTransitionMath.gains(1f, curve)

            assertClose(1f, start.first)
            assertClose(0f, start.second)
            assertClose(0f, end.first)
            assertClose(1f, end.second)
        }
    }

    @Test
    fun progress_isClampedBeforeCalculatingGains() {
        MixTransitionCurve.entries.forEach { curve ->
            assertEquals(MixTransitionMath.gains(0f, curve), MixTransitionMath.gains(-5f, curve))
            assertEquals(MixTransitionMath.gains(1f, curve), MixTransitionMath.gains(5f, curve))
        }
    }

    @Test
    fun fade_midpointIsLinear() {
        val gains = MixTransitionMath.gains(0.5f, MixTransitionCurve.FADE)
        assertClose(0.5f, gains.first)
        assertClose(0.5f, gains.second)
    }

    @Test
    fun blend_midpointPreservesEqualPowerInvariant() {
        val gains = MixTransitionMath.gains(0.5f, MixTransitionCurve.BLEND)
        val expected = (sqrt(2.0) / 2.0).toFloat()

        assertClose(expected, gains.first)
        assertClose(expected, gains.second)

        val power = gains.first * gains.first + gains.second * gains.second
        assertClose(1f, power, 0.0002f)
    }

    @Test
    fun allCurves_remainInsideSafeNormalizedRange() {
        val progressValues = listOf(-1f, 0f, 0.1f, 0.25f, 0.5f, 0.75f, 0.9f, 1f, 2f)

        MixTransitionCurve.entries.forEach { curve ->
            progressValues.forEach { progress ->
                val (outgoing, incoming) = MixTransitionMath.gains(progress, curve)
                assertTrue(outgoing in 0f..1f)
                assertTrue(incoming in 0f..1f)
            }
        }
    }
}
