package com.arturo254.opentune.ui.player.v7

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards V7 cinematic ownership without faking screenshot or touch tests. */
class V7PlayerMigrationIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android source root missing")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun cinematicPortraitAndLandscapeUseOneV7SceneWithoutBackdropDuplication() {
        val p = source("ui/player/Player.kt")
        val scene = source("ui/player/v7/V7PlayerScene.kt")
        val backdrop = source("ui/player/visual/V7PlayerBackdrop.kt")
        assertEquals(2, Regex("""V7PlayerScene\(""").findAll(p).count())
        assertTrue(p.contains("isLandscape = true"))
        assertTrue(p.contains("isLandscape = false"))
        assertTrue(scene.contains("V7PlayerBackdrop("))
        assertTrue(scene.contains("v7BackdropLandscape"))
        assertTrue(scene.contains("v7BackdropPortrait"))
        assertTrue(scene.contains("WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom"))
        assertTrue(scene.contains("Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))"))
        assertTrue(scene.contains(".nestedScroll(nestedScrollConnection)"))
        assertTrue(backdrop.contains("internal fun V7PlayerBackdrop("))
        assertFalse(scene.contains("ExoPlayer("))
        assertFalse(scene.contains("PlayerConnection("))
    }

    @Test fun v7OriginalFortyTwoAndSixtyFourSeventyTwoControlGeometryRemains() {
        val v7 = source("ui/player/v7/V7PlayerComponents.kt")
        val shared = source("ui/player/PlayerComponents.kt")
        assertTrue(shared.contains("V7PlayerTopActions("))
        assertTrue(shared.contains("V7PlayerTransport("))
        assertTrue(v7.contains("Arrangement.spacedBy(10.dp)"))
        assertTrue(v7.contains(".size(42.dp)"))
        assertTrue(v7.contains("playerFavoriteIconRes(liked)"))
        assertTrue(v7.contains(".size(22.dp)"))
        assertTrue(v7.contains(".size(24.dp)"))
        assertEquals(2, Regex("""modifier = Modifier.size\(64.dp\)""").findAll(v7).count())
        assertTrue(v7.contains("modifier = Modifier.size(72.dp)"))
        assertTrue(v7.contains("iconSize = 52.dp"))
        assertTrue(v7.contains("loadingSize = 44.dp"))
        assertEquals(2, Regex("""modifier = Modifier.size\(44.dp\)""").findAll(v7).count())
        assertFalse(v7.contains("animateFloatAsState("))
        assertFalse(v7.contains("Spring.DampingRatio"))
        assertTrue(v7.contains("PlayerSharedSkipIcon("))
        assertTrue(v7.contains("PlayerSharedPlaybackContent("))
    }

    @Test fun cinematicMenusAndFormatStillRouteThroughSharedPlayer() {
        val c = source("ui/player/PlayerComponents.kt")
        val p = source("ui/player/Player.kt")
        val v7 = source("ui/player/v7/V7PlayerComponents.kt")
        assertTrue(c.contains("onToggleFavorite = playerActions::toggleFavorite"))
        assertTrue(c.contains("onOpenMenu = {"))
        assertTrue(c.contains("menuState.showPlayerPanelMenu("))
        assertTrue(c.contains("onPrevious = playerActions::previous"))
        assertTrue(c.contains("onPlayPause = playerActions::playPause"))
        assertTrue(c.contains("onNext = playerActions::next"))
        assertTrue(c.contains("showRemainingTime = playerDesignStyle == PlayerDesignStyle.V7"))
        assertTrue(c.contains("playerFormatBadgeLabel(currentFormat.mimeType)"))
        assertTrue(p.contains("PlayerLayout("))
        assertTrue(p.contains("rememberPlayerVisualState("))
        assertFalse(v7.contains("MusicService("))
    }
}
