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
        com.arturo254.opentune.contracts.OriginalNavigationContracts.backgroundIsSeparate()
        com.arturo254.opentune.contracts.OriginalNavigationContracts.singleGlassRenderer()
    }

    @Test fun searchControlsSampleTheirBackgroundSiblingAndNotTheirOwnViewport() {
        val search = source("ui/original/adapters/SearchPresentation.kt")
        val portal = source("ui/screens/search/PrismSearchBar.kt")
        assertTrue(search.contains("LocalOriginalGlassState provides searchGlass"))
        assertTrue(search.contains("UniversalSearchScreen(query.text, pureBlack, embedded = true)"))
        assertTrue(portal.contains("PrismAtmosphere(true, pureBlack, Modifier.matchParentSize().layerBackdrop(backdrop))"))
        assertTrue(portal.contains("LocalPrismBackdrop provides (if (active) backdrop else null)"))
        assertFalse(search.contains(".layerBackdrop("))
        com.arturo254.opentune.contracts.OriginalNavigationContracts.backgroundIsSeparate()
        com.arturo254.opentune.contracts.OriginalNavigationContracts.singleGlassRenderer()
    }

    @Test fun playgroundCapturesOnlyBackgroundSibling() {
        val playground = source("core/designsystem/liquid/GalaxLiquidPlayground.kt")
        assertTrue(playground.contains("rememberLayerBackdrop()"))
        assertTrue(playground.contains(".layerBackdrop(backdrop)"))
        assertTrue(playground.contains(".galaxLiquidEffect("))
        assertTrue(playground.contains("LiquidTuningSlider("))
        assertTrue(playground.contains("tuning = tuning"))
        assertTrue(playground.contains("onChange(tuning.copy("))
        assertFalse(playground.contains("NavHost("))
        assertFalse(playground.contains("var draft by remember("))
        val studio = source("ui/screens/settings/GalaxLiquidSettingsScreen.kt")
        assertTrue(studio.contains("LaunchedEffect(loaded, state)"))
        assertTrue(studio.contains("GalaxLiquidPreview("))
        assertTrue(studio.contains("GalaxLiquidOpticsControls("))
    }

    @Test fun existingNavigationAndMiniPlayerUseNewFacade() {
        com.arturo254.opentune.contracts.OriginalNavigationContracts.singleGlassRenderer()
    }
}
