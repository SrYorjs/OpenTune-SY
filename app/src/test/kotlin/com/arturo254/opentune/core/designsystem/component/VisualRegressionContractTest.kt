package com.arturo254.opentune.core.designsystem.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Structural regression contract for the incremental Phase 3 migration.
 * Device screenshots are intentionally not claimed here.
 */
class VisualRegressionContractTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Source root missing")
    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun legacyEmptyPlaceholderHasNoDuplicateVisualImplementation() {
        val old = source("ui/component/EmptyPlaceholder.kt")
        assertTrue(old.contains("OpenTuneEmptyState("))
        assertTrue(!old.contains("Image("))
        assertTrue(!old.contains("Text("))
        assertTrue(!old.contains("Box("))
    }

    @Test
    fun searchHomeLibrarySettingsShareTheSameDesignSystemPrimitives() {
        val home = source("ui/screens/HomeScreenComponents.kt")
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        val albums = source("ui/screens/library/LibraryAlbumsScreen.kt")
        val artists = source("ui/screens/library/LibraryArtistsScreen.kt")
        val settings = source("ui/screens/settings/SettingsComponents.kt")
        assertTrue(home.contains("OpenTuneShimmerState("))
        assertTrue(home.contains("Artwork("))
        assertTrue(search.contains("PrismResultLoading("))
        assertTrue(search.contains("PrismStatus("))
        assertTrue(albums.contains("OpenTuneEmptyState("))
        assertTrue(artists.contains("OpenTuneEmptyState("))
        assertTrue(settings.contains("OpenTuneEmptyPresentation.SETTINGS"))
    }

    @Test
    fun playbackNavigationAndGlassUseOriginalComponents() {
        val glass = source("ui/visual/component/GlassKit.kt")
        val toolbar = source("ui/visual/component/AppNavigation.kt")
        val mini = source("ui/visual/player/OriginalMiniPlayer.kt")
        val shell = source("MainActivityScaffold.kt")
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(glass.contains("fun Modifier.glassRoot("))
        assertTrue(glass.contains("GlassPillBackground("))
        assertTrue(toolbar.contains("fun AppNavigationBar("))
        assertTrue(toolbar.contains("FrostedChromeGlass("))
        assertTrue(mini.contains("RoundedCornerShape(20.dp)"))
        assertTrue(mini.contains("FrostedChromeGlass("))
        assertTrue(shell.contains(".glassRoot(originalGlassState)"))
        assertTrue(search.contains("originalRecordingOptions("))
        assertTrue(search.contains("viewModel.loadMore()"))
    }
}
