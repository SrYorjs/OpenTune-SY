package com.arturo254.opentune.core.designsystem.glass

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/** Structural safety gates for the new, independent v2 liquid glass pipeline. */
class GalaxGlassIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin root not found")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun newRendererDoesNotCallLegacyGlassEngine() {
        val facade = source("core/designsystem/glass/GalaxGlassSurface.kt")
        val renderer = source("core/designsystem/liquid/GalaxLiquidRenderer.kt")
        assertTrue(facade.contains("galaxLiquidEffect("))
        assertTrue(facade.contains("GalaxLiquidSurface("))
        assertFalse(facade.contains("openTuneGlassEffect("))
        assertTrue(renderer.contains("import com.kyant.backdrop.drawBackdrop"))
        assertTrue(renderer.contains("lens("))
        assertTrue(renderer.contains("blur("))
        assertTrue(renderer.contains("vibrancy()"))
        assertFalse(renderer.contains("com.skydoves.cloudy"))
        assertFalse(renderer.contains("ExoPlayer("))
    }

    @Test fun actualRouteCaptureIsScopeSeparatedFromConsumingOverlays() {
        val shell = source("MainActivityScaffold.kt")
        assertTrue(shell.contains("rememberLayerBackdrop()"))
        assertTrue(shell.contains("Modifier.layerBackdrop(liveLiquidBackdrop)"))
        assertTrue(shell.contains("LocalGalaxLiquidBackdrop provides liveLiquidBackdrop"))
        assertTrue(shell.contains("LocalGalaxLiquidBackdrop provides null"))
        assertTrue(shell.contains("MainActivityFloatingToolbar("))
        assertFalse(shell.contains("LocalLiquidBackdrop provides liveLiquidBackdrop"))
    }

    @Test fun playgroundCapturesOnlyBackgroundSibling() {
        val playground = source("core/designsystem/liquid/GalaxLiquidPlayground.kt")
        assertTrue(playground.contains("rememberLayerBackdrop()"))
        assertTrue(playground.contains(".layerBackdrop(backdrop)"))
        assertTrue(playground.contains(".galaxLiquidEffect("))
        assertTrue(playground.contains("LabSlider("))
        assertFalse(playground.contains("NavHost("))
    }

    @Test fun existingNavigationAndMiniPlayerUseNewFacade() {
        assertTrue(source("ui/component/FloatingNavigationToolbar.kt").contains(".galaxGlassEffect("))
        assertTrue(source("ui/player/visual/PlayerGlassEffect.kt").contains("galaxGlassEffect("))
    }
}
