package com.arturo254.opentune.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueWindowPlannerTest {
    @Test
    fun emptyQueue_hasNoWindow() {
        assertNull(QueueWindowPlanner.plan(itemCount = 0, currentIndex = 0))
    }

    @Test
    fun earlyTrack_keepsQueueStartAndCurrentRelativeIndex() {
        assertEquals(
            QueueWindow(
                start = 0,
                endExclusive = 53,
                relativeIndex = 3,
            ),
            QueueWindowPlanner.plan(
                itemCount = 100,
                currentIndex = 3,
            ),
        )
    }

    @Test
    fun middleTrack_keepsTwentyItemsBeforeCurrent() {
        assertEquals(
            QueueWindow(
                start = 30,
                endExclusive = 100,
                relativeIndex = 20,
            ),
            QueueWindowPlanner.plan(
                itemCount = 120,
                currentIndex = 50,
            ),
        )
    }

    @Test
    fun nearEnd_clampsWindowToQueueSize() {
        assertEquals(
            QueueWindow(
                start = 75,
                endExclusive = 100,
                relativeIndex = 20,
            ),
            QueueWindowPlanner.plan(
                itemCount = 100,
                currentIndex = 95,
            ),
        )
    }

    @Test
    fun invalidIndexes_areClampedToValidQueueBounds() {
        assertEquals(
            QueueWindow(
                start = 0,
                endExclusive = 50,
                relativeIndex = 0,
            ),
            QueueWindowPlanner.plan(
                itemCount = 100,
                currentIndex = -50,
            ),
        )

        assertEquals(
            QueueWindow(
                start = 79,
                endExclusive = 100,
                relativeIndex = 20,
            ),
            QueueWindowPlanner.plan(
                itemCount = 100,
                currentIndex = 500,
            ),
        )
    }
}
