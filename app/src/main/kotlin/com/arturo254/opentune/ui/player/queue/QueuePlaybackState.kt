package com.arturo254.opentune.ui.player.queue

import androidx.media3.common.Player

/**
 * Presentation-only queue snapshot. The caller passes PlayerConnection.queueWindows,
 * which is ALREADY in Media3 playback order (including shuffle).
 * Never create another playlist, shuffle order, ticker, or source of playback truth.
 */
data class QueuePlaybackState(
    val itemCount: Int,
    val currentIndex: Int,
    val shuffleModeEnabled: Boolean,
    val repeatMode: Int,
    val playedIndices: List<Int>,
    val upNextIndices: List<Int>,
    val nextIndex: Int?,
) {
    val hasCurrent: Boolean get() = currentIndex in 0 until itemCount
    val upNextCount: Int get() = upNextIndices.size
}

/**
 * Up Next contains the as-yet-unplayed portion of the *visible Media3 order*.
 * Repeat-one and repeat-all affect the next playback position, but they do not
 * duplicate UI rows or create an infinite repeated "Up Next" list.
 */
fun calculateQueuePlaybackState(
    itemCount: Int,
    currentIndex: Int,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
): QueuePlaybackState {
    val count = itemCount.coerceAtLeast(0)
    val validIndex = currentIndex.takeIf { it in 0 until count } ?: -1
    val previous = if (validIndex < 0) emptyList() else (0 until validIndex).toList()
    val upcoming = if (validIndex < 0) emptyList() else (validIndex + 1 until count).toList()
    val next = when {
        validIndex < 0 -> null
        repeatMode == Player.REPEAT_MODE_ONE -> validIndex
        validIndex + 1 < count -> validIndex + 1
        repeatMode == Player.REPEAT_MODE_ALL -> 0
        else -> null
    }
    return QueuePlaybackState(
        itemCount = count,
        currentIndex = validIndex,
        shuffleModeEnabled = shuffleModeEnabled,
        repeatMode = repeatMode,
        playedIndices = previous,
        upNextIndices = upcoming,
        nextIndex = next,
    )
}

/**
 * Apply a drag to indices in Media3's currently displayed order. This is only
 * used for the existing shuffle-reorder path; unshuffled reorder uses moveMediaItem.
 */
fun movedQueueOrder(indices: List<Int>, from: Int, to: Int): IntArray? {
    if (from !in indices.indices || to !in indices.indices) return null
    if (indices.toSet().size != indices.size || indices.any { it < 0 }) return null
    val reordered = indices.toMutableList()
    val moved = reordered.removeAt(from)
    reordered.add(to, moved)
    return reordered.toIntArray()
}
