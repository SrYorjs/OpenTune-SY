package com.arturo254.opentune.ui.player.queue

import androidx.media3.common.Player
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueuePlaybackStateTest {
    @Test fun emptyAndInvalidCurrentNeverExposePhantomUpNext() {
        val empty = calculateQueuePlaybackState(0, -1, false, Player.REPEAT_MODE_OFF)
        assertEquals(0, empty.itemCount)
        assertFalse(empty.hasCurrent)
        assertTrue(empty.upNextIndices.isEmpty())
        assertNull(empty.nextIndex)

        val invalid = calculateQueuePlaybackState(4, 99, true, Player.REPEAT_MODE_ALL)
        assertEquals(-1, invalid.currentIndex)
        assertTrue(invalid.upNextIndices.isEmpty())
        assertNull(invalid.nextIndex)
    }

    @Test fun upNextTracksFollowTheAlreadyOrderedQueueWithoutAnotherShuffle() {
        // Media3 is responsible for supplying the shuffled queue order itself.
        val snapshot = calculateQueuePlaybackState(5, 2, true, Player.REPEAT_MODE_OFF)
        assertEquals(listOf(0, 1), snapshot.playedIndices)
        assertEquals(listOf(3, 4), snapshot.upNextIndices)
        assertEquals(2, snapshot.upNextCount)
        assertEquals(3, snapshot.nextIndex)
        assertTrue(snapshot.shuffleModeEnabled)
    }

    @Test fun repeatModesDoNotDuplicateUiRows() {
        val single = calculateQueuePlaybackState(3, 2, false, Player.REPEAT_MODE_ONE)
        val all = calculateQueuePlaybackState(3, 2, false, Player.REPEAT_MODE_ALL)
        val off = calculateQueuePlaybackState(3, 2, false, Player.REPEAT_MODE_OFF)
        assertEquals(2, single.nextIndex)
        assertEquals(0, all.nextIndex)
        assertNull(off.nextIndex)
        assertTrue(single.upNextIndices.isEmpty())
        assertTrue(all.upNextIndices.isEmpty())
    }

    @Test fun moveInShuffleUsesDisplayedPhysicalIndicesAndKeepsAllItems() {
        assertArrayEquals(intArrayOf(4, 2, 0, 3), movedQueueOrder(listOf(0, 4, 2, 3), 0, 2))
        assertArrayEquals(intArrayOf(0, 4, 2, 3), movedQueueOrder(listOf(0, 4, 2, 3), 2, 2))
        assertNull(movedQueueOrder(listOf(0, 1, 2), 3, 1))
        assertNull(movedQueueOrder(listOf(0, 1, 2), -1, 1))
        assertNull(movedQueueOrder(listOf(1, 1, 2), 0, 1))
    }
}
