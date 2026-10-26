package com.arturo254.opentune.ui.screens.library

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxArchiveRebuildTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source files not found")
    private fun read(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun libraryDoesNotMountAnyRetiredPresentation() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        listOf("ArchiveHero(", "ArchiveHeader(", "ArchiveSectionTabs(", "LibraryMixScreen(",
            "LibrarySongsScreen(", "LibraryAlbumsScreen(", "LibraryArtistsScreen(").forEach {
            assertFalse("Retired presentation still mounted: $it", screen.contains(it))
        }
        assertTrue(screen.contains("VaultHeader("))
        assertTrue(screen.contains("VaultCategories("))
        assertTrue(read("ui/screens/library/LibraryMixScreen.kt").contains("LibraryScreen("))
    }

    @Test fun libraryStillUsesTheDatabasePlaybackAndContextMenus() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        listOf("database.allSongs()", "viewModel.albums", "viewModel.artists", "viewModel.playlists",
            "ListQueue(", "SongMenu(", "AlbumMenu(", "ArtistMenu(", "PlaylistMenu(", "CreatePlaylistDialog(").forEach {
            assertTrue("Lost integration: $it", screen.contains(it))
        }
    }

    @Test fun filtersOfflineAndSynchronizationRemainAvailable() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        listOf("VaultSearch(", "query.trim()", "LibraryFilter.SONGS", "LibraryFilter.ALBUMS",
            "LibraryFilter.ARTISTS", "LibraryFilter.PLAYLISTS", "LibraryFilter.SPOTIFY", "playlistIdsByTags(",
            "onRefresh = viewModel::syncAllLibrary", "auto_playlist/downloaded", "cache_playlist/cached").forEach {
            assertTrue("Lost library action: $it", screen.contains(it))
        }
    }

    @Test fun reorderingCannotWriteAFilteredPlaylistSubset() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        assertTrue(screen.contains("query.isBlank() && tags.isEmpty()"))
        assertTrue(screen.contains("playlistSort == PlaylistSortType.CUSTOM"))
        assertTrue(screen.contains("moveLibraryEntry("))
        assertTrue(screen.contains("setPlaylistCustomOrder("))
        assertTrue(screen.contains("!reorderState.isAnyItemDragging"))
    }

    @Test fun searchRootsAreOpaqueAndKeepUniversalPlayback() {
        val screen = read("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(screen.contains("if (pureBlack) Color.Black else MaterialTheme.colorScheme.background"))
        assertTrue(screen.contains("viewModel.playFirstAvailable("))
        assertTrue(screen.contains("UniversalVersionQualityDialog("))
        val pages = read("ui/screens/GalaxMainPages.kt")
        assertTrue(pages.contains("modifier.background(base)"))
        assertTrue(pages.contains("Lifecycle.State.RESUMED"))
    }

    @Test fun libraryKeysUseUniqueEntitiesRatherThanChangingPositions() {
        val screen = read("ui/screens/library/LibraryScreen.kt")
        assertTrue(screen.contains("distinctBy { it.id }"))
        listOf("vault/song/", "vault/artist/", "vault/albums/", "vault/playlists/").forEach {
            assertTrue(screen.contains(it))
        }
    }
}
