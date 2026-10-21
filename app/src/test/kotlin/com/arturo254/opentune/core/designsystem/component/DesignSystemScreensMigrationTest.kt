package com.arturo254.opentune.core.designsystem.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemScreensMigrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source root missing")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun homeUsesCanonicalShimmerWithoutChangingItsSkeleton() {
        val s = source("ui/screens/HomeScreenComponents.kt")
        assertTrue(s.contains("OpenTuneShimmerState(modifier = modifier)"))
        assertTrue(s.contains("Artwork("))
        assertTrue(s.contains("GridItemPlaceHolder()"))
        assertTrue(s.contains("TextPlaceholder("))
    }

    @Test
    fun searchUsesCanonicalLoadingAndEmptyStatesWithoutChangingPaging() {
        val s = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(s.contains("AtlasResultsSkeleton("))
        assertTrue(s.contains("GalaxUniversalNoResults("))
        assertTrue(s.contains("viewModel.loadMore()"))
        assertTrue(s.contains("UniversalVersionQualityDialog("))
    }

    @Test
    fun libraryAndSettingsAdoptStatesWithoutLosingOriginalActions() {
        val albums = source("ui/screens/library/LibraryAlbumsScreen.kt")
        val artists = source("ui/screens/library/LibraryArtistsScreen.kt")
        val settings = source("ui/screens/settings/SettingsComponents.kt")
        assertTrue(albums.contains("OpenTuneEmptyState("))
        assertTrue(artists.contains("OpenTuneEmptyState("))
        assertTrue(albums.contains("OpenTuneDesignSystem.sizes.gridThumbnailHeight"))
        assertTrue(artists.contains("OpenTuneDesignSystem.sizes.gridThumbnailHeight"))
        assertTrue(albums.contains("LibraryAlbumGridItem("))
        assertTrue(artists.contains("LibraryArtistGridItem("))
        assertTrue(settings.contains("OpenTuneEmptyPresentation.SETTINGS"))
        assertTrue(settings.contains("SettingsDimensions.GroupCardCornerRadius"))
    }
}
