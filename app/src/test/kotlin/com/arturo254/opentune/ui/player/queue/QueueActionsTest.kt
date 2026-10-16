package com.arturo254.opentune.ui.player.queue

import androidx.media3.common.MediaItem
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueActionsTest {
    private class FakeBackend : QueueActionBackend {
        override var mediaItemCount = 5
        override var guestPermission = QueueGuestPermission.NOT_GUEST
        val commands = mutableListOf<String>()
        var lastOrder: IntArray? = null
        override fun togglePlayPause() { commands += "playPause" }
        override fun toggleShuffle() { commands += "shuffle" }
        override fun toggleRepeat() { commands += "repeat" }
        override fun playAt(physicalIndex: Int) { commands += "play:$physicalIndex" }
        override fun requestTogetherTrack(trackId: String) { commands += "request:$trackId" }
        override fun removeMediaItem(physicalIndex: Int) { commands += "remove:$physicalIndex" }
        override fun restoreMediaItem(item: MediaItem, formerPhysicalIndex: Int) {
            commands += "restore:${item.mediaId}:$formerPhysicalIndex"
        }
        override fun moveMediaItem(from: Int, to: Int) { commands += "move:$from:$to" }
        override fun setShuffleOrder(indices: IntArray) {
            lastOrder = indices
            commands += "shuffleOrder"
        }
    }

    @Test fun shuffleAndRepeatReuseTheSharedPlayerActionPath() {
        val backend = FakeBackend()
        val actions = QueueActions(backend)
        actions.toggleShuffle()
        actions.toggleRepeat()
        assertEquals(listOf("shuffle", "repeat"), backend.commands)
    }

    @Test fun selectingCurrentSongOnlyTogglesPlayback() {
        val backend = FakeBackend()
        val actions = QueueActions(backend)
        assertEquals(QueueSelectionResult.TOGGLED_CURRENT, actions.select(true, 3, "playing"))
        assertEquals(listOf("playPause"), backend.commands)
    }

    @Test fun guestControlNeverSeeksLocalMedia3() {
        val backend = FakeBackend()
        val actions = QueueActions(backend)
        backend.guestPermission = QueueGuestPermission.DENIED
        assertEquals(QueueSelectionResult.DENIED, actions.select(false, 2, "requested"))
        backend.guestPermission = QueueGuestPermission.ALLOWED
        assertEquals(QueueSelectionResult.INVALID, actions.select(false, 2, " "))
        assertEquals(QueueSelectionResult.REQUESTED, actions.select(false, 2, " requested "))
        assertEquals(listOf("request:requested"), backend.commands)
    }

    @Test fun hostSelectionUsesPhysicalIndexAndRejectsInvalidPositions() {
        val backend = FakeBackend()
        val actions = QueueActions(backend)
        assertEquals(QueueSelectionResult.PLAYED, actions.select(false, 4, "track"))
        assertEquals(QueueSelectionResult.INVALID, actions.select(false, 5, "track"))
        assertEquals(listOf("play:4"), backend.commands)
    }

    @Test fun reorderMatchesOldMedia3PathsInBothShuffleStates() {
        val backend = FakeBackend()
        val actions = QueueActions(backend)
        val displayed = listOf(2, 4, 0, 3, 1)
        assertTrue(actions.reorder(0, 2, displayed, shuffled = true))
        assertArrayEquals(intArrayOf(4, 0, 2, 3, 1), backend.lastOrder)
        assertTrue(actions.reorder(1, 3, displayed, shuffled = false))
        assertFalse(actions.reorder(0, 5, displayed, shuffled = false))
        assertFalse(actions.reorder(1, 1, displayed, shuffled = false))
        assertEquals(listOf("shuffleOrder", "move:1:3"), backend.commands)
    }

    @Test fun removalAndUndoAreRoutedThroughTheSameQueueBackend() {
        val backend = FakeBackend()
        val actions = QueueActions(backend)
        assertFalse(actions.removeAt(-1))
        assertFalse(actions.removeAt(5))
        assertTrue(actions.removeAt(1))
        actions.restore(MediaItem.Builder().setMediaId("song").build(), 1)
        assertEquals(listOf("remove:1", "restore:song:1"), backend.commands)
    }
}
