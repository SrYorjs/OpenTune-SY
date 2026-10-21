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
        assertTrue(home.contains("AetherHomeMasthead("))
        assertTrue(home.contains("AetherOpeningStage("))
        assertTrue(home.contains("AetherMusicLine("))
        assertTrue(home.contains("AetherCoverTile("))
        assertFalse(home.contains("GalaxHomeImmersiveFeature("))
        assertFalse(home.contains("GalaxHomePoster("))
        assertFalse(home.contains("GalaxHomeCompactTrack("))
    }

    @Test
    fun newHomeCatalogDoesNotRenderTheOldHomePageSections() {
        val home = source("ui/screens/HomeScreen.kt")
        val catalog = source("ui/screens/GalaxAetherCatalog.kt")
        assertTrue(home.contains("AetherCatalogHeading("))
        assertTrue(home.contains("AetherCatalogShelf("))
        assertFalse(home.contains("HomePageSectionTitle("))
        assertFalse(home.contains("HomePageSectionContent("))
        assertTrue(catalog.contains("is SongItem ->"))
        assertTrue(catalog.contains("is AlbumItem ->"))
        assertTrue(catalog.contains("is ArtistItem ->"))
        assertTrue(catalog.contains("is PlaylistItem ->"))
    }

    @Test
    fun catalogRetainsEveryMusicActionIncludingContextMenus() {
        val catalog = source("ui/screens/GalaxAetherCatalog.kt")
        assertTrue(catalog.contains("YouTubeSongMenu("))
        assertTrue(catalog.contains("YouTubeAlbumMenu("))
        assertTrue(catalog.contains("YouTubeArtistMenu("))
        assertTrue(catalog.contains("YouTubePlaylistMenu("))
        assertTrue(catalog.contains("YouTubeQueue("))
        assertTrue(catalog.contains("online_playlist/"))
        assertTrue(catalog.contains("navController.navigate(\"album/"))
        assertTrue(catalog.contains("navController.navigate(\"artist/"))
    }

    @Test
    fun homeLoadsAdditionalCatalogPagesWithoutAButton() {
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(home.contains("snapshotFlow {"))
        assertTrue(home.contains("loadMoreYouTubeItems(continuation)"))
        assertTrue(home.contains("homePage?.sections.orEmpty().forEachIndexed"))
        assertFalse(home.contains("showAllRecommendations"))
        assertFalse(home.contains("Ver más"))
        assertFalse(home.contains("Ver menos"))
    }

    @Test
    fun searchIsNowAnIndependentAtlasNotAnOldDiscoveryWrapper() {
        val screen = source("ui/screens/GalaxAtlasScreen.kt")
        val host = source("ui/screens/GalaxMainPages.kt")
        assertTrue(host.contains("GalaxDiscoverScreen("))
        assertTrue(screen.contains("AtlasLaunchStage("))
        assertTrue(screen.contains("AtlasGenreTile("))
        assertTrue(screen.contains("AtlasGateway("))
        assertFalse(screen.contains("GalaxDiscoveryHero("))
        assertFalse(screen.contains("GalaxDiscoveryCategory("))
    }

    @Test
    fun everyGenreRowKeyIsUniqueEvenWithRepeatedBrowseIds() {
        val keys = (0 until 150).map { galaxDiscoveryGenreRowKey(it, false) } +
            (0 until 150).map { galaxDiscoveryGenreRowKey(it, true) }
        assertEquals(300, keys.distinct().size)
        assertNotEquals(galaxDiscoveryGenreRowKey(5, false), galaxDiscoveryGenreRowKey(5, true))
        val screen = source("ui/screens/GalaxAtlasScreen.kt")
        assertTrue(screen.contains("distinctBy { it.endpoint.browseId +"))
        assertTrue(screen.contains("key = { index, _ -> galaxDiscoveryGenreRowKey("))
    }

    @Test
    fun universalResultsKeepUniqueKeysAndMultipleProviders() {
        val keys = (0 until 85).map { universalResultRowKey(it, "repeated-isrc") }
        assertEquals(keys.size, keys.distinct().size)
        val results = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(results.contains("viewModel.selectVariant("))
        assertTrue(results.contains("viewModel.playFirstAvailable("))
        assertTrue(results.contains("UniversalVersionQualityDialog("))
        assertTrue(results.contains("viewModel.loadMore()"))
        assertTrue(results.contains(".enqueueOffline(result)"))
    }

    @Test
    fun searchHasNewAudioSpectrumLoadingAndOfflineStates() {
        val header = source("ui/screens/search/GalaxSearchExperience.kt")
        val frames = source("ui/screens/search/GalaxAtlasResultsFrame.kt")
        val card = source("ui/screens/search/GalaxUniversalResultCard.kt")
        assertTrue(header.contains("SearchFrequencySpectrum("))
        assertTrue(frames.contains("AtlasResultsSkeleton("))
        assertTrue(frames.contains("AtlasResultsWarning("))
        assertTrue(card.contains("downloaded ->"))
        assertTrue(card.contains("downloading ->"))
        assertTrue(card.contains("onClick = onVersions"))
    }

    @Test
    fun liquidGlassComesFromExistingEngineWithoutCustomGlassShaders() {
        val visuals = source("ui/screens/GalaxAetherVisuals.kt")
        val results = source("ui/screens/search/GalaxUniversalResultCard.kt")
        assertTrue(visuals.contains("galaxLiquidChrome("))
        assertTrue(results.contains("galaxLiquidChrome("))
        assertFalse(visuals.contains("RenderEffect.createBlurEffect"))
        assertFalse(visuals.contains("rememberLayerBackdrop("))
        assertFalse(results.contains("rememberLayerBackdrop("))
    }

    @Test
    fun ambientAnimationStopsWhileTheTabIsInactive() {
        val visuals = source("ui/screens/GalaxAetherVisuals.kt")
        val pages = source("ui/screens/GalaxMainPages.kt")
        assertTrue(visuals.contains("active && !reduced"))
        assertTrue(pages.contains("ambientActive = pager.currentPage == 0"))
        assertTrue(pages.contains("ambientActive = pager.currentPage == 1"))
        assertTrue(pages.contains("ambientActive = pager.currentPage == 2"))
        assertFalse(visuals.contains("preferredFrameRate(120f)"))
    }

    @Test
    fun playerAndLibraryEntryPointsRemainWhereTheyWere() {
        val pages = source("ui/screens/GalaxMainPages.kt")
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(pages.contains("HorizontalPager(state = pager"))
        assertTrue(pages.contains("LibraryScreen("))
        assertTrue(home.contains("viewModel.playGalaxShuffle(player)"))
        assertTrue(home.contains("SongMenu("))
        assertTrue(home.contains("ListQueue("))
    }

    @Test
    fun searchBackgroundStillProtectsMiniBarAgainstTransparentNavigation() {
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        val pages = source("ui/screens/GalaxMainPages.kt")
        assertTrue(search.contains("if (pureBlack) Color.Black else MaterialTheme.colorScheme.background"))
        assertTrue(pages.contains(".background(MaterialTheme.colorScheme.background)"))
        assertTrue(search.contains("liquidGlassScreenBackground(pureBlack = pureBlack)"))
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
