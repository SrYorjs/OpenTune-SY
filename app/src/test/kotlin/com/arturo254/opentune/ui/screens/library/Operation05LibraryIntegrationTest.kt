package com.arturo254.opentune.ui.screens.library

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Operation05LibraryIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source root missing")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun originalLibraryIsMountedAndItsTabsAreReal() {
        val pages = source("ui/screens/GalaxMainPages.kt")
        val home = source("ui/screens/library/OriginalLibraryHomeScreen.kt")
        val tabs = source("ui/visual/library/LibraryTabChips.kt")
        assertTrue(pages.contains("OriginalLibraryHomeScreen("))
        assertTrue(home.contains("LibraryTabChips("))
        assertTrue(tabs.contains("All("))
        assertTrue(tabs.contains("Local("))
        assertTrue(tabs.contains("Cached("))
        assertTrue(tabs.contains("Imported("))
        assertTrue(home.contains("originalLocalTab("))
        assertTrue(home.contains("originalCachedTab("))
        assertTrue(home.contains("originalImportedTab("))
    }

    @Test
    fun localTracksStayLocalAndUseTheExistingMusicDatabase() {
        val importer = source("sources/local/LocalAudioImporter.kt")
        val resolver = source("playback/StreamResolver.kt")
        val home = source("ui/screens/library/OriginalLibraryHomeScreen.kt")
        assertTrue(importer.contains("MediaMetadataRetriever"))
        assertTrue(importer.contains("takePersistableUriPermission("))
        assertTrue(importer.contains("database.withTransaction"))
        assertTrue(importer.contains("isLocal = true"))
        assertFalse(importer.contains("YouTube.artist("))
        assertTrue(resolver.contains("dataSpec.uri.scheme == \"content\""))
        assertTrue(resolver.contains("dataSpec.uri.scheme == \"file\""))
        assertTrue(home.contains("LocalDownloadUtil.current.downloads"))
        assertTrue(home.contains("cacheViewModel.cachedSongs"))
        assertTrue(home.contains("SpotifyLibraryViewModel"))
    }

    @Test
    fun pinnedItemsArePersistentAndUseOriginalTiles() {
        val home = source("ui/screens/library/OriginalLibraryHomeScreen.kt")
        val pins = source("ui/visual/library/LibraryPins.kt")
        assertTrue(home.contains("LibraryPinnedTile("))
        assertTrue(home.contains("LibraryPinMenuItem("))
        assertTrue(home.contains("pinsState.remove("))
        assertTrue(pins.contains("LibraryPinnedEntitiesKey"))
        assertTrue(pins.contains("encodeLibraryPins("))
        assertTrue(pins.contains("decodeLibraryPins("))
    }

    @Test
    fun librarySubscreensStayFilteredByTheirOrigin() {
        val routes = source("ui/screens/NavigationBuilder.kt")
        val category = source("ui/screens/library/OriginalLibraryCategoryScreen.kt")
        assertTrue(routes.contains("library/{category}?shelf={shelf}"))
        assertTrue(category.contains("\"local\" ->"))
        assertTrue(category.contains("\"cached\" ->"))
        assertTrue(category.contains("\"imported\" ->"))
        assertTrue(category.contains("LibraryEntityListScreen("))
        assertTrue(category.contains("LibraryGridScreen("))
    }

    @Test
    fun detailPagesUseOriginalHeaderAndCollapseWithoutReplacingPlayer() {
        val header = source("ui/visual/detail/OriginalLibraryDetailHeader.kt")
        val top = source("ui/visual/detail/DetailCollapsingTopBar.kt")
        assertTrue(header.contains("ScrollAwayCover("))
        assertTrue(header.contains("containerTransformTarget("))
        assertTrue(header.contains("onCoverPositioned("))
        assertTrue(top.contains("ProgressiveGlassBackground("))
        listOf(
            "ui/screens/AlbumScreen.kt",
            "ui/screens/artist/ArtistScreen.kt",
            "ui/screens/playlist/LocalPlaylistScreen.kt",
        ).forEach { path ->
            val screen = source(path)
            assertTrue("Missing original header in $path", screen.contains("OriginalLibraryDetailHeader("))
            assertTrue("Missing original collapse bar in $path", screen.contains("DetailCollapsingTopBar("))
            assertTrue("Missing original cover animation in $path", screen.contains("originalCoverBounds"))
            assertTrue("Missing glass capture in $path", screen.contains(".glassRoot(originalDetailGlass"))
        }
        assertTrue(source("ui/screens/AlbumScreen.kt").contains("LocalAlbumRadio("))
        assertTrue(source("ui/screens/playlist/LocalPlaylistScreen.kt").contains("SelectionSongMenu("))
        assertTrue(source("ui/screens/artist/ArtistScreen.kt").contains("Intent.ACTION_SEND"))
    }
    @Test
    fun localManageRemovesOnlyImportedRecordsAfterConfirmation() {
        val importer = source("sources/local/LocalAudioImporter.kt")
        val manager = source("ui/visual/library/LibraryLocalManageMenu.kt")
        val library = source("ui/screens/library/OriginalLibraryHomeScreen.kt")
        assertTrue(importer.contains("suspend fun removeImported("))
        assertTrue(importer.contains("if (!entry.song.isLocal)"))
        assertTrue(importer.contains("delete(entry.song)"))
        assertTrue(importer.contains("releasePersistableUriPermission("))
        assertFalse(importer.contains(".deleteRecursively()"))
        assertTrue(manager.contains("LocalManagePage.CONFIRMATION"))
        assertTrue(manager.contains("onRemove(toRemove)"))
        assertTrue(library.contains("LibraryLocalManageMenu("))
    }

    @Test
    fun detailRowsAndLocalMusicBadgesUseOriginalVisualComponents() {
        val rows = source("ui/visual/detail/PlaylistTrackRow.kt")
        val marks = source("ui/visual/component/OnDeviceMarks.kt")
        assertTrue(rows.contains("fun PlaylistTrackRow("))
        assertTrue(rows.contains("selectedRowBand"))
        assertTrue(rows.contains("OnDeviceMarks("))
        assertTrue(rows.contains("externalGestures = true"))
        assertTrue(marks.contains("songId.startsWith(\"content://\")"))
        listOf(
            "ui/screens/AlbumScreen.kt",
            "ui/screens/artist/ArtistScreen.kt",
            "ui/screens/playlist/LocalPlaylistScreen.kt",
        ).forEach { path ->
            assertTrue(source(path).contains("OriginalDetailSongRow("))
        }
    }

}
