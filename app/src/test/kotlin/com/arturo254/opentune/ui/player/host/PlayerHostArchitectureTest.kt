package com.arturo254.opentune.ui.player.host

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-contract tests; real lifecycle/window behaviors require device checks. */
class PlayerHostArchitectureTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source root missing")
    private fun source(relative: String) =
        File(root, "com/arturo254/opentune/$relative").readText()

    @Test fun activityDelegatesPlayerUiLifecycleToSingleHost() {
        val shell = source("MainActivityScaffold.kt")
        val bridge = source("MainActivityPlayerUiState.kt")
        val host = source("ui/player/host/PlayerHost.kt")
        assertTrue(shell.contains("PlayerHost("))
        assertTrue(!shell.contains("BottomSheetPlayer("))
        assertTrue(bridge.contains("rememberPlayerHostState("))
        assertTrue(!bridge.contains("player.addListener("))
        assertTrue(!bridge.contains("LaunchedEffect("))
        assertTrue(host.contains("rememberBottomSheetState("))
        assertTrue(host.contains("queueRestoreCompleted.first { it }"))
        assertTrue(host.contains("player.addListener(listener)"))
        assertTrue(host.contains("player.removeListener(listener)"))
        assertTrue(host.contains("latestSystemBarsVisibility("))
        assertTrue(host.contains("fun PlayerHost("))
        assertTrue(host.contains("BottomSheetPlayer("))
    }

    @Test fun fullscreenAndAodEffectsHaveOneVisualOwner() {
        val player = source("ui/player/Player.kt")
        val lifecycle = source("ui/player/host/PlayerContentLifecycle.kt")
        assertTrue(player.contains("rememberPlayerContentLifecycle("))
        assertTrue(player.contains("PlayerHostBackHandler("))
        assertTrue(player.contains("visualLifecycle.onInteraction()"))
        assertTrue(!player.contains("WindowCompat.getInsetsController("))
        assertTrue(!player.contains("navController.navigate(\"always_on_display\")") ||
            lifecycle.contains("latestOnTimeout()"))
        assertTrue(lifecycle.contains("delay(remaining)"))
        assertTrue(lifecycle.contains("playerAodRemainingDelayMs("))
        assertTrue(!lifecycle.contains("while (isActive)"))
        assertTrue(lifecycle.contains("latestOnTimeout()"))
        assertTrue(lifecycle.contains("playerHostBackDestination("))
        assertTrue(player.contains("playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)"))
    }

    @Test fun media3QueueAndArtworkOwnershipStayInTheirExistingLayers() {
        val host = source("ui/player/host/PlayerHost.kt")
        val lifecycle = source("ui/player/host/PlayerContentLifecycle.kt")
        assertTrue(!host.contains("ExoPlayer.Builder("))
        assertTrue(!host.contains("service.playQueue("))
        assertTrue(!lifecycle.contains("seekToNext("))
        val player = source("ui/player/Player.kt")
        // Phase 4.7: the Canvas model is now owned by the visual engine,
        // while Player keeps consuming its current Canvas snapshot.
        val visual = source("ui/player/visual/PlayerVisualState.kt")
        assertTrue(visual.contains("CanvasArtwork("))
        assertTrue(player.contains("val canvasArtwork = playerVisuals.canvasArtwork"))
        assertTrue(player.contains("PlayerLyricsPanel("))
        assertTrue(source("ui/player/preferences/PlayerPresentationPreferences.kt").contains("PlayerDesignStyle.V8"))
    }
}
