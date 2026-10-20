package com.arturo254.opentune.ui.player.layout

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxAppleImmersiveRegressionTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun changingSongsRetainsLastValidGradientUntilArtworkPaletteArrives() {
        val floor = source("ui/player/v8/V8ImmersiveGradientFloor.kt")
        val visual = source("ui/player/visual/PlayerVisualState.kt")
        assertTrue(floor.contains("remember { mutableStateOf<List<Color>>(emptyList()) }"))
        assertTrue(floor.contains("artworkUrl, palette"))
        assertTrue(floor.contains("settledPalette = palette"))
        assertTrue(floor.contains("palette.firstOrNull()"))
        assertTrue(floor.contains("animateColorAsState("))
        assertTrue(visual.contains("remember(trackId, artworkUrl, usesArtworkPalette"))
        assertTrue(visual.contains("LaunchedEffect(trackId, artworkUrl, usesArtworkPalette"))
    }

    @Test
    fun immersiveGradientAlwaysRendersBelowTheControls() {
        val scene = source("ui/player/v8/V8PlayerScene.kt")
        val backdrop = source("ui/player/v8/V8PlayerComponents.kt")
        val floor = source("ui/player/v8/V8ImmersiveGradientFloor.kt")
        val backgroundPosition = scene.indexOf("V8ImmersiveGradientFloor(")
        val controlPosition = scene.indexOf("Column(")
        assertTrue(scene.contains("V8ImmersiveGradientFloor("))
        assertTrue(backgroundPosition > 0)
        assertTrue(controlPosition > backgroundPosition)
        assertFalse(backdrop.contains("Apple Music-like lower fade:"))
        assertTrue(floor.contains(".84f to bottom.copy(alpha = .96f)"))
        assertTrue(floor.contains("1f to bottom"))
        assertTrue(floor.contains("size.height * .71f"))
    }

    @Test
    fun appleMusicCanUseAnimatedCanvasWithoutLosingItsGradient() {
        val scene = source("ui/player/v8/V8PlayerScene.kt")
        val apple = source("ui/player/GalaxStudioPlayer/StudioThemeV8Apple.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(scene.contains("CanvasGlassSource(canvasFrame)"))
        assertTrue(scene.contains("V8ImmersiveGradientFloor("))
        assertTrue(apple.contains("bareControls = true"))
        assertTrue(apple.contains("AppleMusicControls("))
        assertTrue(player.contains("playerDesignStyle == PlayerDesignStyle.V8"))
        assertTrue(player.contains("playerDesignStyle == PlayerDesignStyle.V16"))
    }

    @Test
    fun allPlayersDropTheNowPlayingAndQueueAlbumHeader() {
        val thumbnails = source("ui/player/Thumbnail.kt")
        val menu = source("ui/menu/PlayerMenu.kt")
        assertFalse(thumbnails.contains("text = stringResource(R.string.now_playing)"))
        assertFalse(thumbnails.contains("val playingFrom = queueTitle"))
        assertFalse(menu.contains("text = stringResource(R.string.now_playing)"))
    }

    @Test
    fun vinJaxUsesItsOwnCircularArtworkForStaticAndAnimatedCovers() {
        val vinyl = source("ui/player/GalaxStudioPlayer/StudioThemeV17VinJax.kt")
        val artwork = source("ui/player/GalaxStudioPlayer/StudioVinJaxArtwork.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(vinyl.contains("rotationZ=rotation"))
        assertTrue(vinyl.contains(".clip(CircleShape)"))
        assertTrue(vinyl.contains("needlePosition"))
        assertTrue(artwork.contains("compositingStrategy = CompositingStrategy.Offscreen"))
        assertTrue(artwork.contains("contentScale = ContentScale.Crop"))
        assertTrue(artwork.contains("CanvasArtworkPlayer("))
        assertTrue(artwork.contains("rememberPreference(OpenTuneCanvasKey, false)"))
        assertTrue(player.contains("StudioVinJaxArtwork("))
    }

    @Test
    fun sixthStagePanelsDoNotBreakQueueOrLyricsNavigation() {
        val queue = source("ui/player/Queue.kt")
        val lyrics = source("ui/player/LyricsScreen.kt")
        val header = source("ui/player/panels/GalaxPanelTimelineHeader.kt")
        assertTrue(queue.contains("GalaxPanelTimelineHeader("))
        assertTrue(queue.contains("queueActions.select("))
        assertTrue(lyrics.contains("GalaxPanelTimelineHeader("))
        assertTrue(lyrics.contains("LyricsV2("))
        assertTrue(header.contains("MaterialTheme.colorScheme"))
        assertFalse(header.contains("now_playing"))
    }
}
