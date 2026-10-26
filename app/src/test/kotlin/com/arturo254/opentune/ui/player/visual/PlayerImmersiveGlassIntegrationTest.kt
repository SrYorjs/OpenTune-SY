package com.arturo254.opentune.ui.player.visual

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerImmersiveGlassIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun fullscreenGlassUsesOwnSourceAndLeavesMiniPlayerNavigationBackdropIntact() {
        val player = source("ui/player/Player.kt")
        assertTrue(player.contains("val playerArtworkBackdrop = rememberLayerBackdrop()"))
        assertTrue(player.contains("activePlayerGlass -> playerArtworkBackdrop"))
        assertTrue(player.contains("LocalGalaxCanvasLive provides liveCanvasGlass"))
        assertTrue(player.contains("PlayerArtworkAtmosphere("))
        assertTrue(player.contains("modifier = playerSourceModifier"))
        assertTrue(player.contains("LocalGalaxLiquidBackdrop provides"))
        assertTrue(player.contains("playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null"))
        assertTrue(player.contains("usesArtworkPalette = visualSkin.usesArtworkPalette ||"))
        assertTrue(player.contains("LocalEnableLiquidGlass.current ||"))
        assertTrue(player.contains("playerDesignStyle == PlayerDesignStyle.V8 ||"))
        assertTrue(player.contains("compactLandscape = {"))
        assertTrue(player.contains("compactPortrait = {"))
        assertTrue(player.contains("if (activePlayerGlass) Color.White"))
        assertTrue(player.split("PlayerArtworkAtmosphere(").size >= 3)
        assertTrue(player.contains("resolvePlayerLayout(playerDesignStyle, false).hasSharedBackdrop"))

    }

    @Test fun allExpandedFamiliesCaptureBackgroundOnlyAndNeverSampleTheirOwnControls() {
        for (scene in listOf("ui/player/v7/V7PlayerScene.kt", "ui/player/v8/V8PlayerScene.kt")) {
            val src = source(scene)
            assertTrue(src.contains("if (immersiveGlass"))
            assertTrue(src.contains("PlayerArtworkAtmosphere("))
            assertTrue(src.contains("Modifier.layerBackdrop(playerBackdrop)"))
            assertTrue(src.indexOf("PlayerArtworkAtmosphere(") < src.indexOf("controls()"))
        }
        val v8 = source("ui/player/v8/V8PlayerScene.kt")
        assertTrue(v8.contains("!canvasArtwork?.preferredAnimationUrl.isNullOrBlank()"))
        assertTrue(v8.contains("V8PlayerBackdrop("))
        val atmosphere = source("ui/player/visual/PlayerArtworkAtmosphere.kt")
        assertTrue(atmosphere.contains("targetState = artworkUrl"))
        assertTrue(atmosphere.contains("ContentScale.Crop"))
        assertTrue(atmosphere.contains("squareHeight"))
        assertFalse(atmosphere.contains("Modifier.blur("))
        assertTrue(atmosphere.contains("BlendMode.DstIn"))
        val canvasPlayer = source("ui/player/CanvasArtworkPlayer.kt")
        assertTrue(canvasPlayer.contains("R.layout.galax_canvas_texture_player"))
        val model = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        assertTrue(model.contains("LocalGalaxCanvasLive"))
        val liquid = source("core/designsystem/liquid/GalaxLiquidRenderer.kt")
        assertFalse(liquid.contains("if (LocalGalaxCanvasLive.current)"))
        assertFalse(canvasPlayer.contains("view.getBitmap("))
        assertTrue(canvasPlayer.contains("copyCanvasFrame(view)"))
        val frameCopy = source("ui/player/CanvasFrameCopy.kt")
        assertTrue(frameCopy.contains("PixelCopy.request("))
        assertTrue(frameCopy.contains("surface.release()"))
        assertTrue(v8.contains("bitmap = frame"))
        assertTrue(v8.contains("liquidCanvas = immersiveGlass"))
        assertFalse(atmosphere.contains("drawBackdrop("))
        assertFalse(atmosphere.contains("galaxLiquidEffect("))
    }

    @Test fun bottomTabsAndPagesShareContinuousPagerPosition() {
        val toolbar = source("ui/component/FloatingNavigationToolbar.kt")
        val pages = source("ui/screens/GalaxMainPages.kt")
        val scaffold = source("MainActivityScaffold.kt")
        assertTrue(pages.contains("HorizontalPager("))
        assertTrue(toolbar.contains("pagerState.currentPage + pagerState.currentPageOffsetFraction"))
        assertTrue(toolbar.contains("pagerState.dispatchRawDelta("))
        assertTrue(toolbar.contains("pagerState.animateScrollToPage("))
        assertFalse(scaffold.contains("var pageDrag"))
        assertFalse(toolbar.contains("FloatingNavigationToolbarItem("))
    }
}
