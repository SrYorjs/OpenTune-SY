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
        assertTrue(player.contains("playerState.artworkUrl"))
        assertTrue(mini.contains("rememberPlayerState("))
        assertTrue(mini.contains("PlayerStateInput("))
        assertTrue(mini.contains("playerState.isBuffering"))
    }

    @Test
    fun existingTickerCrossfadeAndSeekCommandsRemainOwnedByPlayer() {
        val player = source("ui/player/Player.kt")
        assertTrue(player.contains("delay(100)"))
        assertTrue(player.contains("playerConnection.player.currentMediaItem?.mediaId != mediaMetadata?.id"))
        assertTrue(player.contains("playerConnection.player.seekTo(it)"))
        assertTrue(player.contains("isUserSeeking"))
        val state = source("ui/player/state/PlayerState.kt")
        assertTrue(!state.contains("seekTo("))
        assertTrue(!state.contains("LaunchedEffect("))
        assertTrue(!state.contains("PlayerConnection("))
    }
}
