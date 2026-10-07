package com.arturo254.opentune.sources

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacySearchArchitectureTest {
    private val sourceRoot: File =
        listOf(
            File("src/main/kotlin"),
            File("app/src/main/kotlin"),
        ).firstOrNull(File::isDirectory)
            ?: error("Unable to locate Android main source root")

    @Test
    fun submittedSearchRouteUsesUniversalSearch() {
        val navigation =
            source(
                "com/arturo254/opentune/ui/screens/NavigationBuilder.kt",
            ).readText()

        assertTrue(navigation.contains("UniversalSearchScreen("))
        assertFalse(navigation.contains("OnlineSearchResult("))
    }

    @Test
    fun providerSpecificSearchScreensAndViewModelsStayRemoved() {
        val retired =
            listOf(
                "com/arturo254/opentune/ui/screens/search/OnlineSearchResult.kt",
                "com/arturo254/opentune/ui/screens/search/OnlineSearchScreen.kt",
                "com/arturo254/opentune/ui/screens/search/LocalSearchScreen.kt",
                "com/arturo254/opentune/ui/screens/search/DeezerSearchScreen.kt",
                "com/arturo254/opentune/ui/screens/search/SpotifySearchScreen.kt",
                "com/arturo254/opentune/ui/screens/search/CatalogSourceSearchScreen.kt",
                "com/arturo254/opentune/viewmodels/OnlineSearchViewModel.kt",
                "com/arturo254/opentune/viewmodels/OnlineSearchSuggestionViewModel.kt",
                "com/arturo254/opentune/viewmodels/LocalSearchViewModel.kt",
                "com/arturo254/opentune/viewmodels/DeezerSearchViewModel.kt",
                "com/arturo254/opentune/viewmodels/SpotifySearchViewModel.kt",
                "com/arturo254/opentune/viewmodels/CatalogProviderSearchViewModels.kt",
            )

        retired.forEach { relativePath ->
            assertFalse(
                "Legacy search implementation returned: " + relativePath,
                source(relativePath).exists(),
            )
        }
    }

    @Test
    fun obsoleteSearchProviderPreferenceStaysRemoved() {
        val preferences =
            source(
                "com/arturo254/opentune/constants/PreferenceKeys.kt",
            ).readText()

        assertFalse(preferences.contains("SearchSourceKey"))
        assertFalse(preferences.contains("enum class SearchSource"))
    }

    private fun source(relativePath: String): File =
        File(sourceRoot, relativePath)
}
