package com.arturo254.opentune.ui.screens.search

import com.arturo254.opentune.core.model.AlbumIdentity
import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultCategoriesTest {
    @Test
    fun categoriesContainOnlySongsPlaylistsAndAlbums() {
        assertEquals(
            listOf("Todo", "Canciones", "Listas", "Álbumes"),
            SearchResultCategory.entries.map { it.title },
        )
    }

    @Test
    fun albumsAreGroupedByNameAndArtistAndIgnoreUnknownAlbums() {
        val first = song("Azul", "Zoé", album = "Aztlán")
        val second = song("Tema Dos", "Zoé", album = "AZTLÁN")
        val other = song("Otra", "J Balvin", album = "Aztlán")
        val missing = song("Sin álbum", "Zoé")
        val groups = groupSearchAlbums(listOf(first, second, other, missing))

        assertEquals(2, groups.size)
        assertEquals(2, groups.first().tracks.size)
        assertEquals(1, groups.last().tracks.size)
    }

    @Test
    fun searchScreenKeepsQualityPreferencesInVersionsWithoutHeaderButtons() {
        val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
            .firstOrNull(File::isDirectory) ?: error("Source root missing")
        val screen = File(root, "com/arturo254/opentune/ui/screens/search/UniversalSearchScreen.kt")
            .readText()
        assertTrue(screen.contains("OrbitResultsCategories("))
        assertTrue(screen.contains("UniversalVersionQualityDialog("))
        assertTrue(screen.contains("playSearchResults("))
        assertFalse(screen.contains("signal/featured/artists"))
        assertFalse(screen.contains("SearchResultCategory.ARTISTS"))
        assertTrue(screen.contains("OrbitResultsHero("))
        assertFalse(screen.contains("SignalQuality("))
        assertFalse(screen.contains("TODOS LOS CATÁLOGOS"))
        assertFalse(screen.contains("canciones disponibles · Carga en bloques de 50"))
    }

    private fun song(
        title: String,
        artist: String,
        album: String? = null,
    ): UnifiedTrackSearchResult =
        UnifiedTrackSearchResult(
            identity = TrackIdentity(
                title = title,
                artists = listOf(ArtistIdentity(artist)),
                album = album?.let { AlbumIdentity(it) },
            ),
            variants = emptyList(),
            recommendedVariant = null,
        )
}
