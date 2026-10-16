package com.arturo254.opentune.ui.player.queue

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source boundaries: real timeline, shuffle and Together require device regression. */
class QueueActionsIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun queueAndPlayerReadMedia3OrderedUpNextFromSameProjection() {
        val queue = source("ui/player/Queue.kt")
        val player = source("ui/player/Player.kt")
        val projection = source("ui/player/queue/QueuePlaybackState.kt")
        assertTrue(queue.contains("calculateQueuePlaybackState("))
        assertTrue(player.contains("calculateQueuePlaybackState("))
        assertTrue(player.contains("playbackOrder.upNextIndices.firstOrNull()"))
        assertTrue(projection.contains("repeatMode == Player.REPEAT_MODE_ONE"))
        assertTrue(projection.contains("repeatMode == Player.REPEAT_MODE_ALL"))
        assertFalse(projection.contains("setMediaItems("))
    }

    @Test fun playerQueueMutationsAreRoutedThroughSingleAdapter() {
        val ui = source("ui/player/Queue.kt")
        val actions = source("ui/player/queue/QueueActions.kt")
        listOf(
            "queueActions.reorder(",
            "queueActions.removeAt(",
            "queueActions.restore(",
            "queueActions.select(",
            "queueActions::toggleShuffle",
            "queueActions::toggleRepeat",
        ).forEach { token -> assertTrue("missing: $token", ui.contains(token)) }
        assertFalse(ui.contains("DefaultShuffleOrder("))
        assertFalse(ui.contains("playerConnection.player.moveMediaItem("))
        assertFalse(ui.contains("playerConnection.player.removeMediaItem("))
        assertFalse(ui.contains("playerConnection.player.toggleRepeatMode()"))
        assertTrue(actions.contains("playerActions.toggleShuffle()"))
        assertTrue(actions.contains("playerActions.toggleRepeat()"))
        assertTrue(actions.contains("ControlAction.SeekToTrack("))
        assertTrue(actions.contains("TogetherRole.Guest"))
    }

    @Test fun currentMedia3QueueAndRadioOwnershipArePreserved() {
        val controller = source("playback/QueueController.kt")
        val service = source("playback/MusicService.kt")
        val auto = source("playback/AutomixRadioController.kt")
        val connection = source("playback/PlayerConnection.kt")
        assertTrue(controller.contains("fun playNext(items: List<MediaItem>)"))
        assertTrue(controller.contains("fun addToQueue(items: List<MediaItem>)"))
        assertTrue(controller.contains("scheduleDeferredQueueLoad("))
        assertTrue(controller.contains("fun applyCurrentFirstShuffleOrder()"))
        assertTrue(service.contains("queueController.playNext(items)"))
        assertTrue(service.contains("queueController.addToQueue(items)"))
        assertTrue(auto.contains("queueController.currentQueue = radioQueue"))
        assertTrue(connection.contains("player.getQueueWindows()"))
        assertTrue(connection.contains("onShuffleModeEnabledChanged("))
        assertTrue(connection.contains("onRepeatModeChanged("))
    }
}
