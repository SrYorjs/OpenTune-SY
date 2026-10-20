package com.arturo254.opentune.ui.player.v8

import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.layout.PlayerLayoutFamily
import com.arturo254.opentune.ui.player.layout.resolvePlayerLayout
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural contracts: Canvas rendering and Glass touch must be tested on device. */
class V8PlayerMigrationIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun bothOrientationsUseOneImmersiveSceneAndOneControlSlot() {
        val player = source("ui/player/Player.kt")
        val scene = source("ui/player/v8/V8PlayerScene.kt")
        assertEquals(PlayerLayoutFamily.GALAX_THEME, resolvePlayerLayout(PlayerDesignStyle.V16, false).family)
        assertEquals(PlayerLayoutFamily.APPLE_MUSIC, resolvePlayerLayout(PlayerDesignStyle.V8, false).family)
        assertEquals(PlayerLayoutFamily.GALAX_THEME, resolvePlayerLayout(PlayerDesignStyle.V16, true).family)
        assertEquals(PlayerLayoutFamily.APPLE_MUSIC, resolvePlayerLayout(PlayerDesignStyle.V8, true).family)
        assertEquals(2, Regex("""V8PlayerScene\(""").findAll(player).count())
        assertEquals(1, Regex("""V8PlayerControlsContent\(""").findAll(player).count())
        assertTrue(player.contains("val v8ControlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit"))
        assertTrue(scene.contains("V8PlayerBackdrop("))
        assertTrue(scene.contains("label = \"v8BackdropPortrait\""))
        assertTrue(scene.contains("WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom"))
        assertTrue(scene.contains("Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))"))
        assertTrue(scene.contains(".nestedScroll(nestedScrollConnection)"))
        assertFalse(player.contains("V8PlayerBackdrop("))
    }

    @Test fun canvasBackdropAndGlassVisualsRemainInOneModule() {
        val v8 = source("ui/player/v8/V8PlayerComponents.kt")
        val legacy = source("ui/player/PlayerComponents.kt")
        assertTrue(v8.contains("fun V8PlayerBackdrop("))
        assertTrue(v8.contains("premiumArtworkBreathing"))
        assertTrue(v8.contains("CanvasArtworkPlayer("))
        assertTrue(v8.contains("canvasArtwork.preferredAnimationUrl"))
        assertTrue(v8.contains("artworkUrl.highRes()"))
        assertTrue(v8.contains("Brush.verticalGradient("))
        assertTrue(v8.contains("fun GlassTrack("))
        assertTrue(v8.contains("LiquidGlassControlCluster("))
        assertTrue(v8.contains("LiquidGlassIconButton("))
        assertFalse(legacy.contains("fun V8PlayerBackdrop("))
        assertFalse(legacy.contains("fun V8PlayerControlsContent("))
        assertFalse(v8.contains("MusicService("))
    }

    @Test fun transportAndQualityAreFedByExistingPlayerActionsAndFormat() {
        val v8 = source("ui/player/v8/V8PlayerComponents.kt")
        val player = source("ui/player/Player.kt")
        val queue = source("ui/player/Queue.kt")
        assertTrue(v8.contains("fun V8PlayerControlsContent("))
        assertTrue(v8.contains("playerFormatBadgeLabel(format.mimeType)"))
        assertTrue(v8.contains("calculatePlayerProgress(position, duration, sliderPosition)"))
        assertTrue(v8.contains("label = \"v8PlayPause\""))
        assertTrue(v8.contains("nextUpMetadata"))
        assertTrue(v8.contains("V8VolumeSlider("))
        assertTrue(v8.contains("onVolumeChange = onVolumeChange"))
        assertTrue(v8.contains("onToggleShuffle = playerActions::toggleShuffle"))
        assertTrue(v8.contains("onToggleRepeat = playerActions::toggleRepeat"))
        assertTrue(v8.contains("onPrevious = playerActions::previous"))
        assertTrue(v8.contains("onNext = playerActions::next"))
        assertTrue(queue.contains("import com.arturo254.opentune.ui.player.v8.QueueCollapsedContentV8"))
        assertTrue(player.contains("rememberPlayerSeekSession("))
        assertEquals(0, Regex("""rememberPlayerActions\(""").findAll(v8).count())
        assertTrue(v8.contains("playerActions: PlayerActions"))
        assertFalse(v8.contains("playerConnection.currentSong.collectAsState("))
        assertFalse(v8.contains("ExoPlayer.Builder("))
    }
}
