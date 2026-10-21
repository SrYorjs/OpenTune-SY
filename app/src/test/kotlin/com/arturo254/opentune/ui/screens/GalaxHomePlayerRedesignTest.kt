package com.arturo254.opentune.ui.screens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxHomePlayerRedesignTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Source root not found")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun orbitalCanvasHonorsReducedMotionAndPause() {
        val orbit = source("core/designsystem/component/GalaxOrbitField.kt")
        assertTrue(orbit.contains("galaxMotion.reducedMotion"))
        assertTrue(orbit.contains("active && !reducedMotion"))
        assertTrue(orbit.contains("rememberInfiniteTransition"))
        assertTrue(orbit.contains("drawArc("))
        assertFalse(orbit.contains(".blur("))
        assertFalse(orbit.contains("cloudy("))
    }

    @Test
    fun homeStreamsAllSectionsWithoutManualExpansion() {
        val home = source("ui/screens/HomeScreen.kt")
        val cards = source("ui/screens/GalaxAetherHome.kt")
        assertTrue(home.contains("HorizonOpening("))
        assertTrue(home.contains("HorizonOpening("))
        assertTrue(home.contains("HorizonCover("))
        assertTrue(home.contains("NovaMusicRow("))
        assertTrue(home.contains("viewModel.loadMoreYouTubeItems(next)"))
        assertTrue(home.contains("catalog?.sections.orEmpty().forEachIndexed"))
        assertFalse(home.contains("Ver más"))
        assertFalse(home.contains("Ver menos"))
        assertTrue(cards.contains("AetherGlassPlate("))
    }

    @Test
    fun homeKeepsRealPlaybackShuffleMenusAndRemoteSections() {
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(home.contains("ListQueue("))
        assertTrue(home.contains("queue.map { it.toMediaItem() }"))
        assertTrue(home.contains("viewModel.playGalaxShuffle(player)"))
        assertTrue(home.contains("SongMenu("))
        assertTrue(home.contains("onDismiss = menus::dismiss"))
        assertTrue(home.contains("viewModel.playSourceVariant("))
        assertTrue(home.contains("HorizonCatalog("))
    }

    @Test
    fun searchTabUsesUniqueGenreKeysAndPreservesUniversalPlayback() {
        val pages = source("ui/screens/GalaxDiscoverExperience.kt")
        val results = source("ui/screens/search/UniversalSearchScreen.kt")
        val hero = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(pages.contains("ObservatoryLens("))
        assertTrue(pages.contains("ObservatorySpectrum("))
        assertTrue(pages.contains("itemsIndexed("))
        assertTrue(pages.contains("galaxDiscoveryGenreRowKey("))
        assertTrue(pages.contains("distinctBy { it.endpoint.browseId +"))
        assertTrue(results.contains("viewModel.playFirstAvailable("))
        assertTrue(results.contains("UniversalVersionQualityDialog("))
        assertTrue(results.contains(".enqueueOffline(result)"))
        assertTrue(results.contains("itemsIndexed("))
        assertTrue(hero.contains("SignalHeader("))
        assertTrue(hero.contains("SignalHeader("))
    }

    @Test
    fun playerStylesUseNewScenesWithoutReplacingPlaybackEngine() {
        val player = source("ui/player/Player.kt")
        val classic = source("ui/player/layout/GalaxPlayerClassicScene.kt")
        val v7 = source("ui/player/v7/V7PlayerScene.kt")
        val v8 = source("ui/player/v8/V8PlayerScene.kt")
        val overlay = source("ui/player/visual/GalaxPlayerSceneOverlay.kt")
        assertTrue(player.contains("GalaxPlayerClassicScene("))
        assertTrue(player.contains("PlayerLayout("))
        assertTrue(player.contains("queueSheetState"))
        assertTrue(player.contains("rememberPlayerSeekSession"))
        assertTrue(classic.contains("controls: @Composable ColumnScope.() -> Unit"))
        assertTrue(classic.contains("artwork: @Composable BoxScope.() -> Unit"))
        assertTrue(v7.contains("V7PlayerBackdrop("))
        assertTrue(v8.contains("V8PlayerBackdrop("))
        assertTrue(overlay.contains("GalaxOrbitField("))
    }

    @Test
    fun existingBottomNavigationAndGlassChoicesStayAvailable() {
        val pages = source("ui/screens/GalaxMainPages.kt")
        val liquid = source("core/designsystem/liquid/GalaxLiquidModel.kt")
        val host = source("ui/player/host/PlayerHost.kt")
        assertTrue(pages.contains("HorizontalPager("))
        assertTrue(pages.contains("state = pager"))
        assertTrue(liquid.contains("LocalGalaxLiquidVariant"))
        assertTrue(host.contains("BottomSheetPlayer("))
    }
}
