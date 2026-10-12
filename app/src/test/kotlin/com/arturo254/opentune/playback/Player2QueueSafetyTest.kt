package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Additional 4.1 regression cases for the existing queue-window planner. */
class Player2QueueSafetyTest {
    @Test
    fun emptyAndNegativeItemCountHaveNoWindow() {
        assertNull(QueueWindowPlanner.plan(0, 0))
        assertNull(QueueWindowPlanner.plan(-5, 0))
    }

    @Test
    fun singleItemKeepsIndexZeroAndEndExclusiveOne() {
        assertEquals(QueueWindow(0, 1, 0), QueueWindowPlanner.plan(1, 0))
        assertEquals(QueueWindow(0, 1, 0), QueueWindowPlanner.plan(1, 999))
    }

    @Test
    fun currentTrackWithinWindowAndEdgesStayInBounds() {
        for (itemCount in listOf(1, 2, 20, 21, 52, 100, 500)) {
            for (index in listOf(-10, 0, 1, 20, 50, itemCount - 1, itemCount + 10)) {
                val w = QueueWindowPlanner.plan(itemCount, index)
                    ?: error("Missing window for itemCount=$itemCount index=$index")
                val expectedCurrent = index.coerceIn(0, itemCount - 1)
                assertEquals(expectedCurrent, w.start + w.relativeIndex)
                org.junit.Assert.assertTrue(w.start >= 0)
                org.junit.Assert.assertTrue(w.endExclusive <= itemCount)
                org.junit.Assert.assertTrue(w.relativeIndex in 0 until (w.endExclusive - w.start))
            }
        }
    }

    @Test
    fun largeQueueMaintainsTwentyBeforeAndUpToFiftyAhead() {
        assertEquals(QueueWindow(80, 150, 20), QueueWindowPlanner.plan(1000, 100))
        assertEquals(QueueWindow(0, 51, 1), QueueWindowPlanner.plan(1000, 1))
    }
}
