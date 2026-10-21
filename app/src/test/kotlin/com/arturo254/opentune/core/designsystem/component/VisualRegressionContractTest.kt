package com.arturo254.opentune.core.designsystem.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.theme.createOpenTuneShapeTokens
import java.io.File
import org.junit.Assert.assertEquals
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
        assertTrue(search.contains("OpenTuneLoadingState("))
        assertTrue(search.contains("GalaxUniversalNoResults("))
        assertTrue(albums.contains("OpenTuneEmptyState("))
        assertTrue(artists.contains("OpenTuneEmptyState("))
        assertTrue(settings.contains("OpenTuneEmptyPresentation.SETTINGS"))
    }

    @Test
    fun playbackNavigationAndGlassBoundaryRemainsUnchanged() {
        val glass = source("core/designsystem/glass/GlassEngine.kt")
        val toolbar = source("ui/component/FloatingNavigationToolbar.kt")
        val mini = source("ui/player/MiniPlayer.kt")
        val search = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(glass.contains("chooseGlassRenderMode("))
        assertTrue(toolbar.contains("onItemClick: (Screens, Boolean) -> Unit"))
        assertTrue(mini.contains(".clip(miniPlayerShape)"))
        assertTrue(search.contains("UniversalVersionQualityDialog("))
        assertTrue(search.contains("viewModel.loadMore()"))
        assertEquals(RoundedCornerShape(32.dp), createOpenTuneShapeTokens(Shapes()).miniPlayer)
    }
}
