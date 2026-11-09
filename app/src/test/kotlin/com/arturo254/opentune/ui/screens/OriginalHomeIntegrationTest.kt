package com.arturo254.opentune.ui.screens

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginalHomeIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("No Kotlin sources")
    private fun source(relative: String): String =
        File(root, "com/arturo254/opentune/$relative").readText()

    @Test
    fun originalHomeIsTheActivePageAndItsSectionLinksAreRegistered() {
        val pages = source("ui/screens/GalaxMainPages.kt")
        val home = source("ui/screens/OriginalHomeScreen.kt")
        val routes = source("ui/screens/NavigationBuilder.kt")
        val detail = source("ui/screens/OriginalHomeSectionDetailScreen.kt")
        assertTrue(pages.contains("0 -> OriginalHomeScreen("))
        assertTrue(routes.contains("route = \"section_detail/{sectionId}\""))
        assertTrue(detail.contains("ListQueue("))
        assertTrue(detail.contains("HomeTrackRow("))
        assertTrue(home.contains("navController.navigate(\"section_detail/recently_played\")"))
        assertTrue(home.contains("navController.navigate(\"section_detail/most_played\")"))
        assertTrue(home.contains("navController.navigate(\"section_detail/speed_dial\")"))
        assertTrue(home.contains("navController.navigate(\"section_detail/forgotten_favorites\")"))
        assertFalse(pages.contains("0 -> HomeScreen("))
    }

    @Test
    fun originalCoverMotionAndCardSourceAreIntegrated() {
        val home = source("ui/screens/OriginalHomeScreen.kt")
        val hero = source("ui/visual/component/OriginalHomeHero.kt")
        val cards = source("ui/visual/component/HomeCards.kt")
        val motion = source("ui/visual/component/ScrollAwayCover.kt")
        assertTrue(home.contains("HomeSectionTitle("))
        assertTrue(home.contains("HomeCoverCard("))
        assertTrue(home.contains("HomeTrackRow("))
        assertTrue(home.contains("HomeVideoCard("))
        assertTrue(home.contains("HomeHopInCard("))
        assertTrue(hero.contains("ScrollAwayCover("))
        assertTrue(hero.contains("PageWipe("))
        assertTrue(hero.contains("wipeCurve ="))
        assertTrue(motion.contains("CoverZoom = 0.22f"))
        assertTrue(motion.contains("CoverSoftRadius = 30f"))
        assertTrue(cards.contains("val HomeContentInset = 36.dp"))
        assertTrue(cards.contains("val HomeCardSpacing = 15.dp"))
    }

    @Test
    fun noSecondPlayerProviderOrFakeSectionIsCreated() {
        val home = source("ui/screens/OriginalHomeScreen.kt")
        val detail = source("ui/screens/OriginalHomeSectionDetailScreen.kt")
        assertTrue(home.contains("LocalPlayerConnection.current"))
        assertTrue(home.contains("viewModel.playSourceVariant("))
        assertTrue(detail.contains("player?.playQueue("))
        assertFalse(home.contains("ExoPlayer.Builder("))
        assertFalse(detail.contains("ExoPlayer.Builder("))
        assertFalse(home.contains("mockSong"))
        assertFalse(detail.contains("mockSong"))
    }
}
