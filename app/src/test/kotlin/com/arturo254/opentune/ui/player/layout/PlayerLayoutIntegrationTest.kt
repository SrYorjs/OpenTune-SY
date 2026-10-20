package com.arturo254.opentune.ui.player.layout

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Static integration contracts; not pixel-by-pixel screenshot tests. */
class PlayerLayoutIntegrationTest {
    private val root =
        listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Source root missing")
    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun oneRouterOwnsAllEightPortraitAndLandscapeSlots() {
        val router = source("ui/player/layout/PlayerLayout.kt")
        val fullPlayer = source("ui/player/Player.kt")
        assertTrue(fullPlayer.contains("PlayerLayout("))
        assertFalse(fullPlayer.contains("when (LocalConfiguration.current.orientation)"))
        listOf(
            "classicPortrait", "classicLandscape",
            "compactPortrait", "compactLandscape",
            "cinematicPortrait", "cinematicLandscape",
            "immersivePortrait", "immersiveLandscape",
        ).forEach { slot ->
            assertTrue("slot missing in shared router: $slot", router.contains("$slot: @Composable () -> Unit"))
            assertTrue("slot missing in Player usage: $slot", fullPlayer.contains("$slot = {"))
        }
        assertTrue(fullPlayer.contains("resolvePlayerLayout(playerDesignStyle, false).hasSharedBackdrop"))
        assertTrue(fullPlayer.contains("GalaxPlayerClassicScene("))
        assertTrue(fullPlayer.contains("AppleMusicPlayerScene("))
        assertTrue(fullPlayer.contains("VinJaxPlayerScene("))
        assertEquals(
            1,
            Regex("""(?m)^\s*PlayerLayout\(""").findAll(fullPlayer).count(),
        )
    }

    @Test
    fun allSpecialVisualsAndActionsRemainInsideOriginalConsumer() {
        val player = source("ui/player/Player.kt")
        val router = source("ui/player/layout/PlayerLayout.kt")
        assertTrue(player.contains("V7PlayerScene("))
        assertTrue(source("ui/player/v7/V7PlayerScene.kt").contains("V7PlayerBackdrop("))
        assertTrue(player.contains("StudioThemeV16("))
        assertTrue(source("ui/player/v8/V8PlayerScene.kt").contains("V8PlayerBackdrop("))
        assertTrue(player.contains("V8PlayerControlsContent("))
        assertTrue(player.contains("V5PlayerContent("))
        assertTrue(source("ui/player/v5/V5PlayerContent.kt").contains("fun V5PlayerContent("))
        assertTrue(player.contains("V5LandscapeLikeBox("))
        assertTrue(source("ui/player/v5/V5PlayerContent.kt").contains("fun V5LandscapeLikeBox("))
        assertTrue(player.contains("playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)"))
        // Phase 4.7: the Canvas model is now owned by the visual engine,
        // while Player keeps consuming its current Canvas snapshot.
        val visual = source("ui/player/visual/PlayerVisualState.kt")
        assertTrue(visual.contains("CanvasArtwork("))
        assertTrue(player.contains("val canvasArtwork = playerVisuals.canvasArtwork"))
        assertTrue(player.contains("PlayerLyricsPanel("))
        assertFalse(router.contains("import com.arturo254.opentune.playback.PlayerConnection"))
        assertFalse(router.contains("PlayerConnection("))
        assertFalse(router.contains("import com.arturo254.opentune.service.MusicService"))
        assertFalse(router.contains("MusicService("))
        assertFalse(router.contains("LaunchedEffect("))
        assertFalse(router.contains("DisposableEffect("))
        assertFalse(router.contains("rememberPreference("))
    }

    @Test
    fun playerHostStillMountsExactlyOneFullPlayer() {
        val host = source("ui/player/host/PlayerHost.kt")
        val bridge = source("MainActivityPlayerUiState.kt")
        assertTrue(host.contains("BottomSheetPlayer("))
        assertTrue(bridge.contains("rememberPlayerHostState("))
    }
}
