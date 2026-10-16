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
        assertEquals(
            1,
            Regex("""(?m)^\s*PlayerLayout\(""").findAll(fullPlayer).count(),
        )
    }

    @Test
    fun allSpecialVisualsAndActionsRemainInsideOriginalConsumer() {
        val player = source("ui/player/Player.kt")
        val router = source("ui/player/layout/PlayerLayout.kt")
        assertTrue(player.contains("V7PlayerBackdrop("))
        assertTrue(player.contains("V8PlayerBackdrop("))
        assertTrue(player.contains("V8PlayerControlsContent("))
        assertTrue(player.contains("LittlePlayerContent("))
        assertTrue(player.contains("LandscapeLikeBox("))
        assertTrue(player.contains("playerActions.seekTo(it, displayedTrackId = mediaMetadata?.id)"))
        assertTrue(player.contains("CanvasArtwork("))
        assertTrue(player.contains("LyricsScreen("))
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
