package com.arturo254.opentune.ui.player.visual

import java.io.File
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
        assertTrue(player.contains("artworkUrl = playerVisuals.artworkUrl"))
        assertTrue(source("ui/player/v8/V8PlayerScene.kt").contains("thumbnailUrl = artworkUrl"))
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
        assertTrue(p.contains("V7PlayerScene("))
        assertTrue(source("ui/player/v7/V7PlayerScene.kt").contains("V7PlayerBackdrop("))
        assertTrue(v7.contains("internal fun V7PlayerBackdrop("))
        assertTrue(v7.contains(".cloudy(radius = cloudyRadius)"))
        assertTrue(v7.contains("CompositingStrategy.Offscreen"))
        assertTrue(v7.contains("BlendMode.DstIn"))
        assertTrue(v7.contains("AnimatedContent("))
    }

    @Test fun miniPlayerUsesOriginalCapturedGlassWithoutReplacingPlayerService() {
        val mini = source("ui/player/MiniPlayer.kt")
        val pill = source("ui/visual/player/OriginalMiniPlayer.kt")
        val glass = source("ui/visual/component/GlassKit.kt")
        val host = source("MainActivityScaffold.kt")
        assertTrue(mini.contains("OriginalMiniPlayer("))
        assertTrue(pill.contains("FrostedChromeGlass("))
        assertTrue(pill.contains("LocalOriginalGlassState.current"))
        assertTrue(glass.contains("RenderNode"))
        assertTrue(glass.contains("fun Modifier.glassRoot("))
        assertTrue(host.contains(".glassRoot(originalGlassState)"))
        assertTrue(host.contains("LocalOriginalGlassState provides originalGlassState"))
        assertTrue(mini.contains("onPlayPause = playerActions::playPause"))
        assertTrue(mini.contains("onNext = playerActions::next"))
        assertTrue(mini.contains("onPrevious = playerActions::previous"))
        assertFalse(pill.contains("ExoPlayer.Builder("))
        assertFalse(pill.contains("MusicService("))
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
