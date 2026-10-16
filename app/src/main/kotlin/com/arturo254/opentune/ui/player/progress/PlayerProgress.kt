package com.arturo254.opentune.ui.player.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import kotlin.math.abs

/**
 * One pure progress snapshot for PlayerState, normal/wavy sliders, V5 overlay,
 * V8 Glass track and MiniPlayer circular progress.
 *
 * It never touches Media3 or performs a seek.
 */
data class PlayerProgress(
    val positionMs: Long,
    val durationMs: Long,
    val displayPositionMs: Long,
    val canSeek: Boolean,
    val fraction: Float,
    val sliderValue: Float,
    val sliderRange: ClosedFloatingPointRange<Float>,
) {
    val remainingMs: Long get() = if (canSeek) (durationMs - displayPositionMs).coerceAtLeast(0L) else 0L
}

fun calculatePlayerProgress(
    positionMs: Long,
    durationMs: Long,
    pendingSeekMs: Long? = null,
): PlayerProgress {
    val canSeek = durationMs > 0L && durationMs != C.TIME_UNSET
    val validDuration = if (canSeek) durationMs else 0L
    val rawPosition = positionMs.coerceAtLeast(0L)
    val safePosition = if (canSeek) rawPosition.coerceAtMost(validDuration) else rawPosition
    val rawDisplay = (pendingSeekMs ?: positionMs).coerceAtLeast(0L)
    val display = if (canSeek) rawDisplay.coerceAtMost(validDuration) else rawDisplay
    val fraction = if (canSeek) {
        (display.toDouble() / validDuration.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    val upper = if (canSeek) validDuration.toFloat().coerceAtLeast(1f) else 1f
    return PlayerProgress(
        positionMs = safePosition,
        durationMs = validDuration,
        displayPositionMs = display,
        canSeek = canSeek,
        fraction = fraction,
        sliderValue = if (canSeek) display.toFloat().coerceIn(0f, upper) else 0f,
        sliderRange = 0f..upper,
    )
}

/** The original 1.5-second seek completion tolerance is shared by all skins. */
const val PLAYER_SEEK_SETTLE_TOLERANCE_MS = 1_500L

fun shouldSettlePlayerSeek(
    requestedMs: Long?,
    currentMs: Long,
    durationMs: Long,
    toleranceMs: Long = PLAYER_SEEK_SETTLE_TOLERANCE_MS,
): Boolean {
    if (requestedMs == null || currentMs < 0L) return false
    val clamped = if (durationMs > 0L && durationMs != C.TIME_UNSET) {
        requestedMs.coerceIn(0L, durationMs)
    } else requestedMs.coerceAtLeast(0L)
    // Subtraction via doubles avoids signed Long overflow on malformed metadata.
    return abs(currentMs.toDouble() - clamped.toDouble()) <= toleranceMs.coerceAtLeast(0L).toDouble()
}

/**
 * Shared drag/preview/settle state for the full Player and its V5 gesture
 * overlay. remember(trackId) prevents a seek on track A leaking into track B.
 * The existing Player.kt 100ms ticker calls settle() without creating a second.
 */
@Stable
class PlayerSeekSession internal constructor() {
    var pendingSeekMs by mutableStateOf<Long?>(null)
        private set
    var isDragging by mutableStateOf(false)
        private set

    fun onSeekPreview(positionMs: Long) {
        isDragging = true
        pendingSeekMs = positionMs.coerceAtLeast(0L)
    }

    /** The caller still routes the command through PlayerActions (crossfade). */
    fun finish(onCommit: (Long) -> Unit) {
        try {
            pendingSeekMs?.let(onCommit)
        } finally {
            isDragging = false
        }
    }

    fun settle(currentMs: Long, durationMs: Long) {
        if (!isDragging && shouldSettlePlayerSeek(pendingSeekMs, currentMs, durationMs)) {
            pendingSeekMs = null
        }
    }
}

@Composable
fun rememberPlayerSeekSession(trackId: String?): PlayerSeekSession =
    remember(trackId) { PlayerSeekSession() }
