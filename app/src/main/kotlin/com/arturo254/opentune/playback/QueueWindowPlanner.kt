package com.arturo254.opentune.playback

/**
 * Pure queue-window calculation used to load large queues without blocking the
 * main thread. Kept Android-free so the behavior is protected by JVM tests.
 */
internal data class QueueWindow(
    val start: Int,
    val endExclusive: Int,
    val relativeIndex: Int,
)

internal object QueueWindowPlanner {
    private const val ITEMS_BEFORE_CURRENT = 20
    private const val ITEMS_AFTER_CURRENT = 50

    fun plan(
        itemCount: Int,
        currentIndex: Int,
    ): QueueWindow? {
        if (itemCount <= 0) return null

        val safeIndex = currentIndex.coerceIn(0, itemCount - 1)
        val start = (safeIndex - ITEMS_BEFORE_CURRENT).coerceAtLeast(0)
        val endExclusive = (safeIndex + ITEMS_AFTER_CURRENT).coerceAtMost(itemCount)

        return QueueWindow(
            start = start,
            endExclusive = endExclusive,
            relativeIndex = safeIndex - start,
        )
    }
}
