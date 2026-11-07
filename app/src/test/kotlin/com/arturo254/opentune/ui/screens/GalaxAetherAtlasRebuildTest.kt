package com.arturo254.opentune.ui.screens

import com.arturo254.opentune.ui.screens.search.universalResultRowKey
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxAetherAtlasRebuildTest {
    private val sourceRoot = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Source root missing")

    private fun source(path: String): String =
        File(sourceRoot, "com/arturo254/opentune/$path").readText()

    @Test
    fun homeHasDistinctLayoutWithoutReusingThePreviousEditorialScreen() {
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(home.contains("PulseFeature("))
        assertTrue(home.contains("PulseSearch("))
        assertTrue(home.contains("PulseScreenFrame("))
        assertTrue(home.contains("PulseCover("))
        assertFalse(home.contains("GalaxHomeImmersiveFeature("))
        assertFalse(home.contains("GalaxHomePoster("))
        assertFalse(home.contains("GalaxHomeCompactTrack("))
    }

    @Test
    fun newHomeCatalogDoesNotRenderTheOldHomePageSections() {
        val home = source("ui/screens/HomeScreen.kt")
        val catalog = source("ui/screens/HomeScreen.kt")
        assertTrue(home.contains("PulseHeading("))
        assertTrue(home.contains("PulseCatalog("))
        assertFalse(home.contains("HomePageSectionTitle("))
        assertFalse(home.contains("HomePageSectionContent("))
        assertTrue(catalog.contains("is SongItem ->"))
        assertTrue(catalog.contains("is AlbumItem ->"))
        assertTrue(catalog.contains("is ArtistItem ->"))
        assertTrue(catalog.contains("is PlaylistItem ->"))
    }

    @Test
    fun catalogRetainsEveryMusicActionIncludingContextMenus() {
        val catalog = source("ui/screens/HomeScreen.kt")
        assertTrue(catalog.contains("YouTubeSongMenu("))
        assertTrue(catalog.contains("YouTubeAlbumMenu("))
        assertTrue(catalog.contains("YouTubeArtistMenu("))
        assertTrue(catalog.contains("YouTubePlaylistMenu("))
        assertTrue(catalog.contains("YouTubeQueue("))
        assertTrue(catalog.contains("online_playlist/"))
        assertTrue(catalog.contains("nav.navigate(\"album/"))
        assertTrue(catalog.contains("nav.navigate(\"artist/"))
    }

    @Test
    fun homeLoadsAdditionalCatalogPagesWithoutAButton() {
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(home.contains("snapshotFlow {"))
        assertTrue(home.contains("loadMoreYouTubeItems(next)"))
        assertTrue(home.contains("catalog?.sections.orEmpty().forEachIndexed"))
        assertFalse(home.contains("showAllRecommendations"))
        assertFalse(home.contains("Ver más"))
        assertFalse(home.contains("Ver menos"))
    }

    @Test
    fun searchHasItsOwnCompositionWithoutReusingTheHomeFeature() {
        val screen = source("ui/screens/PrismDiscoveryScreen.kt")
        val host = source("ui/screens/GalaxMainPages.kt")
        assertTrue(host.contains("GalaxDiscoverScreen("))
        assertTrue(screen.contains("PrismScreenFrame("))
        assertTrue(screen.contains("PrismGenreTile("))
        assertTrue(screen.contains("charts_screen"))
        assertTrue(screen.contains("new_release"))
        assertFalse(screen.contains("PulseFeature("))
        assertFalse(screen.contains("ObservatoryLens("))
    }

    @Test
    fun everyGenreRowKeyIsUniqueEvenWithRepeatedBrowseIds() {
        val keys = (0 until 150).map { galaxDiscoveryGenreRowKey(it, false) } +
            (0 until 150).map { galaxDiscoveryGenreRowKey(it, true) }
        assertEquals(300, keys.distinct().size)
        assertNotEquals(galaxDiscoveryGenreRowKey(5, false), galaxDiscoveryGenreRowKey(5, true))
        val screen = source("ui/screens/PrismDiscoveryScreen.kt")
        assertTrue(screen.contains("distinctBy { it.endpoint.browseId +"))
        assertTrue(screen.contains("key = { index, _ -> galaxDiscoveryGenreRowKey("))
    }

    @Test
    fun universalResultsKeepUniqueKeysAndMultipleProviders() {
        val keys = (0 until 85).map { universalResultRowKey(it, "repeated-isrc") }
        assertEquals(keys.size, keys.distinct().size)
        val results = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(results.contains("originalRecordingOptions("))
        assertTrue(results.contains("viewModel.playSearchResults("))
        assertFalse(results.contains("UniversalVersionQualityDialog("))
        assertTrue(results.contains("viewModel.loadMore()"))
        assertTrue(results.contains(".enqueueOffline(result)"))
    }

    @Test
    fun searchKeepsLoadingWarningsAndOfflineStatesInItsNewPresentation() {
        val screen = source("ui/screens/search/UniversalSearchScreen.kt")
        val card = source("ui/screens/search/OrbitResultsExperience.kt")
        assertTrue(screen.contains("PrismResultLoading("))
        assertFalse(screen.contains("PrismSourceNotice("))
        assertTrue(screen.contains("OrbitRecordingRow("))
        assertTrue(card.contains("if (downloaded)"))
        assertTrue(card.contains("if (downloading)"))
        assertTrue(card.contains("onClick = onDownload"))
    }

    @Test
    fun liquidGlassComesFromExistingEngineWithoutCustomGlassShaders() {
        val visuals = source("ui/screens/search/PrismSearchDesign.kt")
        val results = source("ui/screens/search/PrismResultComponents.kt")
        assertTrue(visuals.contains("galaxLiquidEffect("))
        assertTrue(visuals.contains("LocalGalaxLiquidBackdrop provides"))
        assertFalse(visuals.contains("RenderEffect.createBlurEffect"))
        assertFalse(results.contains("rememberLayerBackdrop("))
    }

    @Test
    fun ambientAnimationStopsWhileTheTabIsInactive() {
        val visuals = source("ui/screens/GalaxAetherVisuals.kt")
        val pages = source("ui/screens/GalaxMainPages.kt")
        assertTrue(visuals.contains("active && !reduced"))
        assertTrue(pages.contains("pager.settledPage == page"))
        assertTrue(pages.contains("!pager.isScrollInProgress"))
        assertTrue(pages.contains("ambientActive = active"))
        assertFalse(visuals.contains("preferredFrameRate(120f)"))
    }

    @Test
    fun playerAndLibraryEntryPointsRemainWhereTheyWere() {
        val pages = source("ui/screens/GalaxMainPages.kt")
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(pages.contains("HorizontalPager("))
        assertTrue(pages.contains("LibraryScreen("))
        assertTrue(home.contains("viewModel.playGalaxShuffle(player)"))
        assertTrue(home.contains("SongMenu("))
        assertTrue(home.contains("ListQueue("))
    }

    @Test
    fun searchBackgroundStillProtectsMiniBarAgainstTransparentNavigation() {
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        val frame = source("ui/screens/search/PrismSearchDesign.kt")
        val pages = source("ui/screens/GalaxMainPages.kt")
        assertTrue(search.contains("PrismScreenFrame("))
        assertTrue(frame.contains("Color.Black"))
        assertTrue(frame.contains(".background("))
        assertTrue(pages.contains(".background(MaterialTheme.colorScheme.background)"))
        assertTrue(search.contains("pureBlack = pureBlack"))
    }

    @Test
    fun legacyLibraryShortcutApiIsAvailableWithoutRestoringThePreviousHomeDesign() {
        val shared = source("ui/screens/GalaxCollectionTileCompat.kt")
        val main = source("ui/screens/GalaxMainPages.kt")
        assertTrue(shared.contains("fun GalaxCollectionTile("))
        assertTrue(shared.contains("AetherGlassPlate("))
        assertFalse(main.contains("fun GalaxCollectionTile("))
    }
}
