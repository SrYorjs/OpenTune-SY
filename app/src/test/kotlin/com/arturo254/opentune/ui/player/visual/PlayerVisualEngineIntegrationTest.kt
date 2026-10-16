package com.arturo254.opentune.ui.player.visual

import com.arturo254.opentune.core.designsystem.glass.GlassOptics
import com.arturo254.opentune.core.designsystem.glass.GlassPreset
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integration contracts; source checks are not device video/screenshot tests. */
class PlayerVisualEngineIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source root missing")
    private fun source(p: String): String =
        File(root, "com/arturo254/opentune/$p").readText()

    @Test fun fullPlayerUsesOneCanvasAndPaletteOwner() {
        val player = source("ui/player/Player.kt")
        val engine = source("ui/player/visual/PlayerVisualState.kt")
        assertTrue(player.contains("val playerVisuals = rememberPlayerVisualState("))
        assertTrue(player.contains("val gradientColors = playerVisuals.gradientColors"))
        assertTrue(player.contains("val canvasArtwork = playerVisuals.canvasArtwork"))
        assertTrue(player.contains("thumbnailUrl = playerVisuals.artworkUrl"))
        assertFalse(player.contains("fetchCanvasArtworkForPlayback("))
        assertFalse(player.contains("PlayerColorExtractor.extractGradientColors("))
        assertTrue(engine.contains("downloadRevision"))
        assertTrue(engine.contains("LaunchedEffect(trackId, downloadRevision)"))
        assertTrue(engine.contains("CancellationException"))
        assertTrue(engine.contains("getOfflineAnimationUri") ||
            engine.contains("latestOfflineLookup(track.id)"))
        assertTrue(engine.contains("paletteCache[key]"))
    }

    @Test fun v7BackdropMovedButItsLayeredRenderingStillExists() {
        val p = source("ui/player/Player.kt")
        val v7 = source("ui/player/visual/V7PlayerBackdrop.kt")
        assertTrue(p.contains("V7PlayerBackdrop("))
        assertTrue(v7.contains("internal fun V7PlayerBackdrop("))
        assertTrue(v7.contains(".cloudy(radius = cloudyRadius)"))
        assertTrue(v7.contains("CompositingStrategy.Offscreen"))
        assertTrue(v7.contains("BlendMode.DstIn"))
        assertTrue(v7.contains("AnimatedContent("))
    }

    @Test fun miniPlayerGlassUsesTheExactLegacySoftPreset() {
        val mini = source("ui/player/MiniPlayer.kt")
        val adapter = source("ui/player/visual/PlayerGlassEffect.kt")
        assertTrue(mini.contains(".playerVisualGlassEffect("))
        assertTrue(mini.contains(".clip(miniPlayerShape)"))
        assertTrue(mini.contains("surfaceTokens.borders.miniPlayerOpaqueWidth"))
        assertTrue(adapter.contains("openTuneGlassEffect("))
        assertFalse(adapter.contains("liquidGlassSurface("))
        assertEquals(
            GlassOptics(
                blurRadius = 4,
                refraction = 0.045f,
                curve = 0.10f,
                dispersion = 0.0015f,
                edge = 0f,
            ),
            GlassPreset.SOFT.optics,
        )
    }

    @Test fun canvasVideoRendererStillOwnsAndReleasesItsMutedPlayer() {
        val renderer = source("ui/player/CanvasArtworkPlayer.kt")
        val visual = source("ui/player/visual/PlayerVisualState.kt")
        assertTrue(renderer.contains("exoPlayer.release()"))
        assertTrue(renderer.contains("volume = 0f"))
        assertTrue(renderer.contains("setShutterBackgroundColor("))
        assertFalse(visual.contains("ExoPlayer.Builder("))
        assertFalse(visual.contains("togglePlayPause("))
        assertFalse(visual.contains("MusicService("))
    }
}
