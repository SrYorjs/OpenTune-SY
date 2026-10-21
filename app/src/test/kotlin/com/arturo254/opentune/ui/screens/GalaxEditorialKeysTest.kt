package com.arturo254.opentune.ui.screens

import com.arturo254.opentune.ui.screens.search.universalResultRowKey
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxEditorialKeysTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin root missing")

    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test
    fun repeatedBrowseIdentifiersNeverReuseLazyKeys() {
        val keys = (0 until 100).map { index ->
            galaxDiscoveryGenreRowKey(index, fallback = false)
        }
        assertEquals(100, keys.distinct().size)
        assertNotEquals(keys.first(), keys.last())
        assertTrue(keys.all { it.startsWith("discovery-genre:") })
    }

    @Test
    fun fallbackCategoriesNeverCollideWithProviderCategories() {
        val provider = (0 until 30).map {
            galaxDiscoveryGenreRowKey(it, fallback = false)
        }
        val fallback = (0 until 30).map {
            galaxDiscoveryGenreRowKey(it, fallback = true)
        }
        assertEquals(60, (provider + fallback).distinct().size)
        assertTrue(fallback.all { it.startsWith("discovery-fallback:") })
    }

    @Test
    fun homeSectionHeaderAndContentsRemainDistinctAsPagesGrow() {
        val keys = (0 until 150).flatMap { index ->
            listOf(
                galaxHomeSectionRowKey(index, heading = true),
                galaxHomeSectionRowKey(index, heading = false),
            )
        }
        assertEquals(300, keys.distinct().size)
        assertNotEquals(
            galaxHomeSectionRowKey(10, true),
            galaxHomeSectionRowKey(10, false),
        )
    }

    @Test
    fun identityDuplicatesCannotCrashUniversalResults() {
        val duplicateIdentity = "same-recording"
        val keys = (0 until 80).map {
            universalResultRowKey(it, duplicateIdentity)
        }
        assertEquals(80, keys.distinct().size)
        assertTrue(keys.all { it.contains(duplicateIdentity) })
        assertNotEquals(
            universalResultRowKey(0, duplicateIdentity),
            universalResultRowKey(1, duplicateIdentity),
        )
    }

    @Test
    fun sourceListsAreBoundToUniqueRowKeyFactories() {
        val discover = source("ui/screens/GalaxAtlasScreen.kt")
        val home = source("ui/screens/HomeScreen.kt")
        val results = source("ui/screens/search/UniversalSearchScreen.kt")
        assertTrue(discover.contains("key = { index, _ -> galaxDiscoveryGenreRowKey("))
        assertTrue(discover.contains("distinctBy { it.endpoint.browseId +"))
        assertTrue(results.contains("key = { index, result -> universalResultRowKey("))
        assertTrue(home.contains("item(key = galaxHomeSectionRowKey(index, true))"))
        assertTrue(home.contains("item(key = galaxHomeSectionRowKey(index, false))"))
        assertFalse(discover.contains("joinToString(\":\") { it.endpoint.browseId }"))
    }

    @Test
    fun loadingAndAutomaticPaginationRequireNoManualExpansion() {
        val home = source("ui/screens/HomeScreen.kt")
        assertTrue(home.contains("snapshotFlow {"))
        assertTrue(home.contains(".distinctUntilChanged()"))
        assertTrue(home.contains(".filter { (continuation, nearEnd) -> continuation != null && nearEnd }"))
        assertTrue(home.contains("loadMoreYouTubeItems(continuation)"))
        assertFalse(home.contains("Ver más"))
        assertFalse(home.contains("Ver menos"))
        assertFalse(home.contains("showAllRecommendations"))
    }

    @Test
    fun newScreenMaterialsReuseOriginalLiquidEngine() {
        val visuals = source("ui/screens/GalaxAetherVisuals.kt")
        val home = source("ui/screens/GalaxAetherHome.kt")
        val discover = source("ui/screens/GalaxAtlasDiscovery.kt")
        val search = source("ui/screens/search/GalaxUniversalResultCard.kt")
        assertTrue(visuals.contains("galaxLiquidChrome("))
        assertTrue(home.contains("AetherGlassPlate("))
        assertTrue(discover.contains("AetherGlassPlate("))
        assertTrue(search.contains("galaxLiquidChrome("))
        assertFalse(visuals.contains("rememberLayerBackdrop("))
    }

    @Test
    fun particleClocksHonorCurrentPageAndReducedMotion() {
        val ambient = source("ui/screens/GalaxEditorialAtmosphere.kt")
        val pages = source("ui/screens/GalaxMainPages.kt")
        assertTrue(ambient.contains("active && !reducedMotion"))
        assertTrue(ambient.contains("durationMillis = 32000"))
        assertTrue(ambient.contains("durationMillis = 44000"))
        assertTrue(pages.contains("ambientActive = pager.currentPage == 0"))
        assertTrue(pages.contains("ambientActive = pager.currentPage == 1"))
        assertFalse(ambient.contains(".blur("))
        assertFalse(ambient.contains(".cloudy("))
    }
}
