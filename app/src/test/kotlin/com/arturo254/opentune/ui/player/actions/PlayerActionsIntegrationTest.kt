package com.arturo254.opentune.ui.player.actions

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural checks only: real Media3 transport + Together require a device. */
class PlayerActionsIntegrationTest {
    private val sourceRoot =
        listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Android Kotlin source root missing")

    private fun source(relativePath: String): String =
        File(sourceRoot, "com/arturo254/opentune/$relativePath").readText()

    @Test fun fullPlayerAndAllStyleControlsUseCommonActions() {
        val player = source("ui/player/Player.kt")
        val controls = source("ui/player/PlayerComponents.kt")
        assertTrue(player.contains("rememberPlayerActions("))
        assertTrue(player.contains("playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)"))
        assertTrue(player.contains("playerActions::next"))
        assertTrue(player.contains("playerActions::previous"))
        assertTrue(controls.contains("playerActions.playPause()"))
        assertTrue(controls.contains("playerActions.toggleFavorite()"))
        assertTrue(controls.contains("playerActions::next"))
        assertTrue(controls.contains("playerActions::previous"))
        assertTrue(!player.contains("playerConnection.player.togglePlayPause()"))
        assertTrue(!controls.contains("playerConnection.player.togglePlayPause()"))
        assertTrue(!controls.contains("playerConnection.toggleLike()"))
    }

    @Test fun miniPlayerSwipeAndQueueUseCommonIntentRouting() {
        val mini = source("ui/player/MiniPlayerComponents.kt")
        val queue = source("ui/player/Queue.kt")
        assertTrue(mini.contains("MiniPlayerSwipePolicy.shouldChangeSong("))
        assertTrue(mini.contains("playerActions.previous()"))
        assertTrue(mini.contains("playerActions.next()"))
        assertTrue(mini.contains("playerActions::toggleFavorite"))
        val queueActions = source("ui/player/queue/QueueActions.kt")
        assertTrue(queue.contains("queueActions.select("))
        assertTrue(queueActions.contains("override fun togglePlayPause() = playerActions.playPause()"))
        assertTrue(queue.contains("playerActions.toggleFavorite()"))
        assertTrue(!queue.contains("playerConnection.player.togglePlayPause()"))
        assertTrue(!mini.contains("playerConnection.player.seekToNext()"))
    }

    @Test fun togetherAndCrossfadeRetainTheirOwners() {
        val actions = source("ui/player/actions/PlayerActions.kt")
        val connection = source("playback/PlayerConnection.kt")
        assertTrue(actions.contains("override fun next() { connection.seekToNext() }"))
        assertTrue(actions.contains("override fun previous() { connection.seekToPrevious() }"))
        assertTrue(actions.contains("override fun toggleFavorite() { connection.toggleLike() }"))
        assertTrue(actions.contains("player.seekToNext()"))
        assertTrue(connection.contains("ControlAction.SkipNext"))
        assertTrue(connection.contains("ControlAction.SkipPrevious"))
        assertTrue(connection.contains("TogetherRole.Guest"))
        assertTrue(connection.contains("DiscordPresenceManager.restart()"))
        assertTrue(!actions.contains("rememberPreference("))
        assertTrue(!actions.contains("LaunchedEffect("))
    }
}
