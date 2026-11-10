package com.arturo254.opentune.ui.visual.player

import android.view.animation.AnimationUtils
import androidx.compose.runtime.Stable
import androidx.media3.common.Player
import kotlin.math.abs
import kotlin.math.exp

private const val SnapMs = 250.0

private const val PullTimeMs = 60.0

private const val StaleMs = 200L

private const val CarryMaxMs = 2000L

@Stable
class PlaybackClock(
    private val player: () -> Player?,
    private val offsetMs: () -> Long = { 0L },
) : () -> Long {
    private var frameMs = Long.MIN_VALUE
    private var smoothed = 0.0

    private var reportedMs = Long.MIN_VALUE
    private var reportedAtMs = 0L

    override fun invoke(): Long {
        val now = AnimationUtils.currentAnimationTimeMillis()
        if (now != frameMs) advance(now)
        return smoothed.toLong() + offsetMs()
    }

    private fun advance(now: Long) {
        val p = player() ?: return
        val playing = p.isPlaying
        val speed = p.playbackParameters.speed
        val fresh = frameMs == Long.MIN_VALUE || now - frameMs !in 1..StaleMs
        val elapsed = (now - frameMs).toDouble()
        frameMs = now
        val heard = carried(p.currentPosition.coerceAtLeast(0L), now, elapsed, fresh, playing, speed)
        if (fresh) {
            smoothed = heard
            return
        }
        val predicted = if (playing) smoothed + elapsed * speed else smoothed
        val gap = heard - predicted
        smoothed = if (abs(gap) > SnapMs) heard else predicted + gap * (1.0 - exp(-elapsed / PullTimeMs))
    }

    private fun carried(reported: Long, now: Long, elapsed: Double, fresh: Boolean, playing: Boolean, speed: Float): Double {
        if (fresh || reported != reportedMs) {
            reportedMs = reported
            reportedAtMs = if (fresh) now else now - (elapsed / 2.0).toLong()
        }
        if (!playing) {
            reportedAtMs = now
            return reported.toDouble()
        }
        return reported + (now - reportedAtMs).coerceAtMost(CarryMaxMs) * speed.toDouble()
    }
}
