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
    fun homeEditorialHeroUsesRealTrackAndIndependentGlassSource() {
        val home = source("ui/screens/HomeScreen.kt")
        val hero = source("ui/screens/GalaxFeaturedTrack.kt")
        val actions = source("ui/screens/GalaxHomeActions.kt")
        assertTrue(home.contains("GalaxFeaturedTrack(featured"))
        assertTrue(home.contains("GalaxHomeActions("))
        assertTrue(hero.contains("song.song.title"))
        assertTrue(hero.contains("Palette.from(bitmap)"))
        assertTrue(hero.contains(".size(96).allowHardware(false)"))
        assertTrue(hero.contains(".galaxLiquidChrome(GalaxScreenStyle.pillShape"))
        assertFalse(hero.contains("rememberLayerBackdrop()"))
        assertTrue(home.contains("onRecognition ="))
        assertFalse(actions.contains(".blur("))
    }

    @Test
    fun musicCollectionsKeepTheirFunctionalRoutesWhileReceivingNewHeader() {
        val media = source("ui/screens/GalaxMediaHeader.kt")
        val discovery = source("ui/screens/GalaxMainPages.kt")
        assertTrue(media.contains("val wide = maxWidth >= 560.dp"))
        assertTrue(media.contains("GalaxCoverMosaic(artworkUrls, artist"))
        assertTrue(media.contains("if (artist) CircleShape"))
        assertTrue(media.contains("GalaxMediaFacts(facts)"))
        assertTrue(media.contains("supporting()"))
        assertTrue(media.contains("actions()"))
        assertTrue(discovery.contains("GalaxDiscoverPortal("))
        assertTrue(discovery.contains("onSearch = onOpenSearch"))
        val portal = source("ui/screens/GalaxDiscoverExperience.kt")
        assertTrue(portal.contains(".clickable(role = Role.Button, onClick = onSearch)"))
    }

    @Test
    fun navigationAndLiquidTuningRemainUntouchedByVisualChange() {
        val nav = source("ui/screens/Screens.kt")
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val tabs = source("ui/screens/GalaxMainPages.kt")
        assertTrue(nav.contains("val MainScreens = listOf(Home, Search, Library)"))
        assertTrue(tabs.contains("HorizontalPager(state = pager"))
        assertTrue(liquid.contains("LocalGalaxLiquidTuning"))
        assertTrue(liquid.contains("LocalGalaxLiquidBackdrop"))
    }
}
