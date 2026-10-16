package com.arturo254.opentune.ui.player.skin

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source contracts: these do not replace device screenshot regression. */
class VisualSkinIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun fullPlayerUsesOneVisualSkinForColorsBackdropsAndPalette() {
        val p = source("ui/player/Player.kt")
        listOf(
            "val visualSkin = rememberVisualSkin(",
            "val TextBackgroundColor = visualSkin.textBackgroundColor",
            "val icBackgroundColor = visualSkin.iconBackgroundColor",
            "val textButtonColor = visualSkin.textButtonColor",
            "val iconButtonColor = visualSkin.iconButtonColor",
            "visualSkin.usesArtworkPalette",
            "visualSkin.sheetBackgroundColor(",
            "visualSkin.playerBackgroundColor(",
            "visualSkin.queueOnBackgroundColor",
            "visualSkin.queueTextButtonColor",
        ).forEach { token -> assertTrue("missing: $token", p.contains(token)) }
    }

    @Test
    fun pureVisualSkinDoesNotOwnPlaybackOrPreferencePersistence() {
        val s = source("ui/player/skin/VisualSkin.kt")
        assertFalse(s.contains("import com.arturo254.opentune.playback.PlayerConnection"))
        assertFalse(s.contains("MusicService("))
        assertFalse(s.contains("rememberPreference("))
        assertFalse(s.contains("LaunchedEffect("))
        assertFalse(s.contains("PlayerActions("))
        assertTrue(s.contains("MaterialTheme.colorScheme"))
        assertTrue(s.contains("fun resolveVisualSkin("))
    }

    @Test
    fun playerLayoutAndControlsRemainCompatible() {
        val p = source("ui/player/Player.kt")
        val layout = source("ui/player/layout/PlayerLayout.kt")
        assertTrue(p.contains("PlayerLayout("))
        assertTrue(p.contains("V7PlayerBackdrop("))
        assertTrue(p.contains("V8PlayerBackdrop("))
        assertTrue(p.contains("playerActions.seekTo("))
        assertTrue(p.contains("displayedTrackId = mediaMetadata?.id"))
        assertTrue(p.contains("PlayerLyricsPanel("))
        assertTrue(p.contains("Queue("))
        assertTrue(layout.contains("classicPortrait: @Composable () -> Unit"))
    }
}
