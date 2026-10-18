package com.arturo254.opentune.ui.player.visual

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 4.5 Player safety gate: fullscreen controls must sample the current song's
 * background, never the NavHost route two screens behind the player.
 */
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
        assertTrue(player.contains("usesArtworkPalette = visualSkin.usesArtworkPalette || LocalEnableLiquidGlass.current"))
        assertTrue(player.contains("compactLandscape = {"))
        assertTrue(player.contains("compactPortrait = {"))
        assertTrue(player.contains("if (activePlayerGlass) Color.White"))
        assertTrue(player.split("PlayerArtworkAtmosphere(").size >= 4)

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
        assertTrue(atmosphere.contains("64.dp"))
        assertTrue(atmosphere.contains("BlendMode.DstIn"))
        val canvasPlayer = source("ui/player/CanvasArtworkPlayer.kt")
        assertTrue(canvasPlayer.contains("R.layout.galax_canvas_texture_player"))
        val model = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        assertTrue(model.contains("LocalGalaxCanvasLive"))
        val liquid = source("core/designsystem/liquid/GalaxLiquidRenderer.kt")
        assertTrue(liquid.contains("if (LocalGalaxCanvasLive.current)"))
        assertTrue(v8.contains("liquidCanvas = immersiveGlass"))
        assertFalse(atmosphere.contains("drawBackdrop("))
        assertFalse(atmosphere.contains("galaxLiquidEffect("))
    }

    @Test fun bottomTabsHandleSwipesAtClickableItemNotOnlyOuterToolbar() {
        val toolbar = source("ui/component/FloatingNavigationToolbar.kt")
        assertTrue(toolbar.contains("onSwipe: (Float) -> Unit"))
        assertTrue(toolbar.contains("onItemClick(items[next], false)"))
        assertTrue(toolbar.contains("val drag = itemDrag"))
        assertTrue(toolbar.contains("detectHorizontalDragGestures("))
        assertTrue(toolbar.contains("liquidTabIndicatorX"))
        assertTrue(toolbar.contains("latestOnProgress(itemDrag)"))
        assertTrue(toolbar.contains("latestOnProgress(0f)"))
        assertTrue(toolbar.contains("liquidTabDragFeedback"))
        assertTrue(toolbar.contains(".pointerInput(Unit)"))
        assertTrue(toolbar.contains("rememberUpdatedState(onSwipe)"))
    }
}
