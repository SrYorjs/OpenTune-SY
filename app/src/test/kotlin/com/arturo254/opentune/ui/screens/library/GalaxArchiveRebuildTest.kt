package com.arturo254.opentune.ui.screens.library

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxArchiveRebuildTest {
    private val sourceRoot = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source files not found")

    private fun read(path: String): String =
        File(sourceRoot, "com/arturo254/opentune/$path").readText()

    @Test
    fun libraryReplacesOldScreensRatherThanRenderingThemInsideAWrapper() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        for (oldScreen in listOf(
            "LibraryMixScreen(",
            "LibrarySongsScreen(",
            "LibraryAlbumsScreen(",
            "LibraryArtistsScreen(",
            "LibraryPlaylistsScreen(",
            "LibrarySpotifyPlaylistsScreen(",
        )) {
            assertFalse("Old presentation still rendered: $oldScreen", screen.contains(oldScreen))
        }
        assertTrue(screen.contains("ArchiveHeader("))
        assertTrue(screen.contains("ArchiveHero("))
        assertTrue(screen.contains("ArchiveSectionTabs("))
        assertTrue(screen.contains("ArchiveCollectionRow("))
    }

    @Test
    fun libraryUsesCurrentMusicDatabaseAndExistingPlayback() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        assertTrue(screen.contains("database.allSongs().collectAsState("))
        assertTrue(screen.contains("viewModel.albums.collectAsState()"))
        assertTrue(screen.contains("viewModel.artists.collectAsState()"))
        assertTrue(screen.contains("viewModel.playlists.collectAsState()"))
        assertTrue(screen.contains("ListQueue("))
        assertTrue(screen.contains("player?.playQueue("))
        assertTrue(screen.contains("SongMenu("))
        assertTrue(screen.contains("AlbumMenu("))
        assertTrue(screen.contains("navController.navigate("))
    }

    @Test
    fun libraryHasRealFilteringSearchAndSynchronisation() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        assertTrue(screen.contains("OutlinedTextField("))
        assertTrue(screen.contains("searchText.isBlank()"))
        assertTrue(screen.contains("ignoreCase = true"))
        assertTrue(screen.contains("onRefresh = viewModel::syncAllLibrary"))
        assertTrue(screen.contains("ArchiveSection.SONGS"))
        assertTrue(screen.contains("ArchiveSection.ALBUMS"))
        assertTrue(screen.contains("ArchiveSection.ARTISTS"))
        assertTrue(screen.contains("ArchiveSection.PLAYLISTS"))
        assertTrue(screen.contains("SpotifyLibraryViewModel"))
    }

    @Test
    fun visualSystemReusesSelectedLiquidGlassWithoutEditingTheEngine() {
        val design = read("ui/screens/library/GalaxArchiveDesign.kt")
        val screen = read("ui/screens/library/LibraryScreen.kt")
        val main = read("ui/screens/GalaxMainPages.kt")
        assertTrue(design.contains("galaxLiquidChrome("))
        assertTrue(design.contains("GalaxEditorialBackdrop("))
        assertTrue(design.contains("GalaxEditorialOrb("))
        assertTrue(design.contains("ArchiveSpotifyCard("))
        assertTrue(screen.contains("ambientActive"))
        assertTrue(main.contains("ambientActive = pager.currentPage == 2"))
        assertFalse(design.contains(".blur("))
        assertFalse(design.contains("rememberLayerBackdrop("))
    }

    @Test
    fun searchHasOpaqueBackingBelowContentAndMiniPlayerCapture() {
        val search = read("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(search.contains("Box("))
        assertTrue(search.contains("if (pureBlack) Color.Black else MaterialTheme.colorScheme.background"))
        assertTrue(search.contains(".liquidGlassScreenBackground(pureBlack = pureBlack)"))
        assertTrue(search.contains("viewModel.playFirstAvailable("))
        assertTrue(search.contains("UniversalVersionQualityDialog("))
    }

    @Test
    fun noAutoLibraryCollectionHasDuplicateLazyKeys() {
        val lib = read("ui/screens/library/LibraryScreen.kt")
        assertTrue(lib.contains("key = { index, _ -> \"archive_song:\" + index }"))
        assertTrue(lib.contains("key = { index, _ -> \"archive_album_row:\" + index }"))
        assertTrue(lib.contains("key = { index, _ -> \"archive_artist_row:\" + index }"))
        assertTrue(lib.contains("key = { index, _ -> \"archive_playlist_row:\" + index }"))
        assertTrue(lib.contains("distinctBy { it.id }"))
    }
}
