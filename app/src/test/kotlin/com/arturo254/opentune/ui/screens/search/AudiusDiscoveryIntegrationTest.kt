package com.arturo254.opentune.ui.screens.search

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiusDiscoveryIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source root missing")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun searchTabsDisplayRealAudiusArtistsAndPlaylists() {
        val screen = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(screen.contains("audiusState.artists"))
        assertTrue(screen.contains("audiusState.playlists"))
        assertTrue(screen.contains("viewModel.openAudiusArtist(artist)"))
        assertTrue(screen.contains("viewModel.openAudiusPlaylist(playlist)"))
        assertFalse(screen.contains("La búsqueda de listas de Audius estará disponible"))
    }

    @Test
    fun collectionsHaveDetailNavigationAndFullQueuePlayback() {
        val screen = source("ui/screens/search/UniversalSearchScreen.kt")
        val coordinator = source("sources/playback/SourcePlaybackCoordinator.kt")
        assertTrue(screen.contains("selectedCollection.description"))
        assertTrue(screen.contains("viewModel.closeAudiusCollection()"))
        assertTrue(screen.contains("viewModel.playAudiusCollection("))
        assertTrue(screen.contains("viewModel::loadMoreAudiusTracks"))
        assertTrue(coordinator.contains("ListQueue(title = queueTitle, items = playable"))
    }

    @Test
    fun discoveryUsesOfficialReadOnlyEndpointsAndNoSecret() {
        val discovery = source("sources/audius/AudiusDiscoveryRepository.kt")
        assertTrue(discovery.contains("/users/search"))
        assertTrue(discovery.contains("/playlists/search"))
        assertTrue(discovery.contains("/tracks"))
        assertFalse(discovery.contains("Bearer "))
        assertFalse(discovery.contains("CLIENT_SECRET"))
    }
}
