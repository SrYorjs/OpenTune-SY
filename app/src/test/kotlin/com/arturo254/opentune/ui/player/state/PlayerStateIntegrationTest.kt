package com.arturo254.opentune.ui.player.state

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerStateIntegrationTest {
    private val root =
        listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Android Kotlin sources unavailable")
    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun bothPlayersUseCommonPresentationState() {
        val player = source("ui/player/Player.kt")
        val mini = source("ui/player/MiniPlayerComponents.kt")
        assertTrue(player.contains("rememberPlayerState("))
        assertTrue(player.contains("PlayerStateInput("))
        assertTrue(player.contains("rememberPlayerVisualState("))
        assertTrue(player.contains("thumbnailUrl = playerVisuals.artworkUrl"))
        assertTrue(player.contains("offlineArtworkUrl = offlineCoverUri"))
        assertTrue(mini.contains("rememberPlayerState("))
        assertTrue(mini.contains("PlayerStateInput("))
        assertTrue(mini.contains("playerState.isBuffering"))
    }

    @Test
    fun existingTickerCrossfadeAndSeekCommandsRemainOwnedByPlayer() {
        val player = source("ui/player/Player.kt")
        assertTrue(player.contains("delay(100)"))
        // 4.3 moved the command to PlayerActions, preserving crossfade first.
        val actions = source("ui/player/actions/PlayerActions.kt")
        assertTrue(player.contains("playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)"))
        assertTrue(actions.contains("backend.activeMediaId != displayedTrackId"))
        assertTrue(actions.contains("backend.advanceCrossfadeTarget()"))
        assertTrue(actions.contains("backend.seekTo(positionMs)"))
        assertTrue(player.contains("seekSession.settle("))
        assertTrue(player.contains("seekSession.finish {"))
        val state = source("ui/player/state/PlayerState.kt")
        assertTrue(!state.contains("seekTo("))
        assertTrue(!state.contains("LaunchedEffect("))
        assertTrue(!state.contains("PlayerConnection("))
    }
}
