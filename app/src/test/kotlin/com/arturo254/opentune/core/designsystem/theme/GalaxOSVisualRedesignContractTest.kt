package com.arturo254.opentune.core.designsystem.theme

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxOSVisualRedesignContractTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")

    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun everyNavHostDestinationSharesGalacticAppearanceWithoutFullScreenBlur() {
        val host = source("MainActivityScaffold.kt")
        val style = source("core/designsystem/theme/GalaxScreenStyle.kt")
        assertTrue(host.contains("GalaxDestinationTheme {"))
        assertTrue(style.contains("Box(Modifier.fillMaxSize().galaxScreenBackground())"))
        assertTrue(style.contains("drawWithCache"))
        assertTrue(style.contains("surfaceContainerHighest"))
        assertFalse(style.contains(".blur("))
        assertTrue(style.contains("reduceMotion = reducedMotion"))
    }

    @Test
    fun editorialHomeHasRealPlaybackAndNoFullscreenBlur() {
        val home = source("ui/screens/HomeScreen.kt")
        val composition = source("ui/screens/PulseHomeDesign.kt")
        val atmosphere = source("ui/screens/GalaxMainPages.kt")
        assertTrue(home.contains("PulseFeature("))
        assertTrue(home.contains("viewModel.playGalaxShuffle(player)"))
        assertTrue(composition.contains("artwork"))
        assertTrue(composition.contains("galaxLiquidEffect("))
        assertTrue(composition.contains("rememberNovaPhase(active)"))
        assertTrue(atmosphere.contains("galaxMotion.reducedMotion"))
        assertFalse(composition.contains(".blur("))
    }

    @Test
    fun musicCollectionsKeepTheirFunctionalRoutesWhileReceivingNewHeader() {
        val media = source("ui/screens/GalaxMediaHeader.kt")
        val discovery = source("ui/screens/PrismDiscoveryScreen.kt")
        assertTrue(media.contains("val wide = maxWidth >= 560.dp"))
        assertTrue(media.contains("GalaxMediaCoverStage(artworkUrls, artist") && media.contains("GalaxCoverMosaic("))
        assertTrue(media.contains("if (artist) CircleShape"))
        assertTrue(media.contains("GalaxMediaFacts(facts)"))
        assertTrue(media.contains("supporting()"))
        assertTrue(media.contains("actions()"))
        assertTrue(discovery.contains("PrismDiscoveryMasthead("))
        assertTrue(discovery.contains("onClick = onOpenSearch"))
        val portal = source("ui/screens/search/PrismSearchBar.kt")
        assertTrue(portal.contains("onSearch(query.text)"))
        assertTrue(discovery.contains("galaxDiscoveryGenreRowKey("))
    }

    @Test
    fun navigationAndLiquidTuningRemainUntouchedByVisualChange() {
        com.arturo254.opentune.contracts.OriginalNavigationContracts.routesAndStacks()
        com.arturo254.opentune.contracts.OriginalNavigationContracts.singleGlassRenderer()
        val host = source("ui/original/adapters/ActivityGlassHost.kt")
        for (key in listOf("GalaxLiquidCornerRadiusKey", "GalaxLiquidBlurKey", "GalaxLiquidRefractionHeightKey", "GalaxLiquidRefractionAmountKey", "GalaxLiquidChromaticKey", "GalaxLiquidTintKey")) assertTrue(host.contains(key))
        assertTrue(host.contains(".bounded()"))
    }
}
