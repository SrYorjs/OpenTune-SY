package com.arturo254.opentune.ui.player.host

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration contracts for 4.12. Animations, window visibility, orientation,
 * audio focus and device-level playback still require hardware regression.
 */
class PlayerPresentationSyncIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun miniAndExpandedPlayerUseOneImmutableSnapshotAndTicker() {
        val full = source("ui/player/Player.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        val controls = source("ui/player/MiniPlayerComponents.kt")
        val state = source("ui/player/state/PlayerState.kt")
        assertTrue(full.contains("val playerState = rememberPlayerState("))
        assertTrue(full.contains("MiniPlayer("))
        assertTrue(full.contains("playerState = playerState"))
        assertTrue(mini.contains("playerState: PlayerState"))
        assertTrue(mini.contains("NewMiniPlayerContent("))
        assertTrue(controls.contains("playerState: PlayerState"))
        assertTrue(controls.contains("progressFraction = playerState.progressFraction"))
        assertTrue(controls.contains("artworkUrl = playerState.artworkUrl"))
        assertTrue(controls.contains("MiniPlayerArtworkFrame(mediaMetadata, artworkUrl)"))
        assertTrue(controls.contains("frame.artworkUrl ?: frame.metadata?.thumbnailUrl"))
        assertTrue(controls.contains("isLoading = playerState.isBuffering"))
        assertFalse(controls.contains("PlayerStateInput("))
        assertFalse(controls.contains("rememberPlayerState("))
        assertFalse(mini.contains("position: Long"))
        assertFalse(mini.contains("duration: Long"))
        assertTrue(full.contains("delay(100)"))
        assertFalse(mini.contains("LaunchedEffect("))
        assertFalse(state.contains("ExoPlayer("))
    }

    @Test fun transitionsCancelStaleGestureButKeepExistingGesturesAndAnimations() {
        val controls = source("ui/player/MiniPlayerComponents.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        assertTrue(controls.contains("LaunchedEffect(trackId, playbackIndex, isPlayerExpanded, playerConnection)"))
        assertTrue(controls.contains("miniPlayerTrackTransitionDirection("))
        assertTrue(controls.contains("settleJob?.cancel()"))
        assertTrue(controls.contains("offsetX = 0f"))
        assertTrue(controls.contains("MiniPlayerSwipePolicy.shouldChangeSong("))
        assertTrue(controls.contains("playerActions.previous()"))
        assertTrue(controls.contains("playerActions.next()"))
        assertTrue(controls.contains("AnimatedContent("))
        assertTrue(mini.contains("playerVisualGlassEffect("))
    }

    @Test fun hostControlsFullscreenRestorationInsetsAndListenerLifetime() {
        val host = source("ui/player/host/PlayerHost.kt")
        val policy = source("ui/player/host/PlayerHostPolicy.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(host.contains("settledHostPlayerAnchor("))
        assertTrue(host.contains("remember(playerBottomSheetState)"))
        assertTrue(host.contains("settledAnchor != null"))
        assertTrue(host.contains("setSavedMiniPlayerAnchor(settledAnchor)"))
        assertTrue(host.contains("shouldPersistHostAnchor("))
        assertTrue(host.contains("queueRestoreCompleted.first { it }"))
        assertTrue(host.contains(") ?: savedMiniPlayerAnchor"))
        assertTrue(host.contains("            windowsInsets,"))
        assertTrue(host.contains("player.addListener(listener)"))
        assertTrue(host.contains("player.removeListener(listener)"))
        assertTrue(host.contains("latestSystemBarsVisibility("))
        assertTrue(policy.contains("else -> null"))
        assertTrue(player.contains("PlayerHostBackHandler("))
    }
}
