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
        val composition = source("ui/screens/GalaxAetherHome.kt")
        val atmosphere = source("ui/screens/GalaxAetherVisuals.kt")
        assertTrue(home.contains("AetherOpeningStage("))
        assertTrue(home.contains("viewModel.playGalaxShuffle(player)"))
        assertTrue(composition.contains("song.thumbnailUrl"))
        assertTrue(composition.contains("AetherGlassPlate("))
        assertTrue(atmosphere.contains("galaxMotion.reducedMotion"))
        assertTrue(atmosphere.contains("galaxLiquidChrome("))
        assertFalse(atmosphere.contains(".blur("))
    }

    @Test
    fun musicCollectionsKeepTheirFunctionalRoutesWhileReceivingNewHeader() {
        val media = source("ui/screens/GalaxMediaHeader.kt")
        val discovery = source("ui/screens/GalaxAtlasScreen.kt")
        assertTrue(media.contains("val wide = maxWidth >= 560.dp"))
        assertTrue(media.contains("GalaxMediaCoverStage(artworkUrls, artist") && media.contains("GalaxCoverMosaic("))
        assertTrue(media.contains("if (artist) CircleShape"))
        assertTrue(media.contains("GalaxMediaFacts(facts)"))
        assertTrue(media.contains("supporting()"))
        assertTrue(media.contains("actions()"))
        assertTrue(discovery.contains("AtlasLaunchStage("))
        assertTrue(discovery.contains("onSearch = onOpenSearch"))
        val portal = source("ui/screens/GalaxAtlasDiscovery.kt")
        assertTrue(portal.contains("onClick = onSearch"))
        assertTrue(discovery.contains("galaxDiscoveryGenreRowKey("))
    }

    @Test
    fun navigationAndLiquidTuningRemainUntouchedByVisualChange() {
        val nav = source("ui/screens/Screens.kt")
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val tabs = source("ui/screens/GalaxMainPages.kt")
        assertTrue(nav.contains("val MainScreens = listOf(Home, Search, Library)"))
        assertTrue(tabs.contains("HorizontalPager("))
        assertTrue(tabs.contains("state = pager"))
        assertTrue(liquid.contains("LocalGalaxLiquidTuning"))
        assertTrue(liquid.contains("LocalGalaxLiquidBackdrop"))
    }
}
