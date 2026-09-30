package com.arturo254.opentune.playback

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Pure transition math used by the playback crossfade engine.
 *
 * Keep this free of Android/Media3 dependencies so its behavior can be locked
 * down with fast JVM unit tests before the playback service is refactored.
 */
internal object MixTransitionMath {
    fun requiredStartBufferMs(fadeMs: Int): Long =
        (fadeMs.toLong() + 2_000L).coerceIn(3_000L, 10_000L)

    fun gains(
        progress: Float,
        curve: MixTransitionCurve,
    ): Pair<Float, Float> {
        val t = progress.coerceIn(0f, 1f)

        return when (curve) {
            MixTransitionCurve.FADE ->
                (1f - t) to t

            MixTransitionCurve.BLEND -> {
                val radians = t.toDouble() * (PI / 2.0)
                cos(radians).toFloat().coerceIn(0f, 1f) to
                    sin(radians).toFloat().coerceIn(0f, 1f)
            }

            MixTransitionCurve.RISE -> {
                val outgoing = (1f - t).pow(1.30f).coerceIn(0f, 1f)
                val incoming = t.pow(0.62f).coerceIn(0f, 1f)
                outgoing to incoming
            }

            MixTransitionCurve.MELT -> {
                val outgoing = (1f - t).pow(0.72f).coerceIn(0f, 1f)
                val incoming = t.pow(1.18f).coerceIn(0f, 1f)
                outgoing to incoming
            }
        }
    }
}
