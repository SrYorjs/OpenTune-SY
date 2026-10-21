package com.arturo254.opentune.ui.screens.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class GalaxPartSevenScreenAtlasTest {
    private val root = listOf(
        File("src/main/kotlin"),
        File("app/src/main/kotlin"),
    ).firstOrNull(File::isDirectory) ?: error("Missing Kotlin source root")

    private fun source(name: String): String =
        File(root, "com/arturo254/opentune/$name").readText()

    @Test
    fun atlasCoversEveryKotlinScreenAndSupportFile() {
        val atlas = listOf(
            File("docs/GALAXOT_SCREEN_ATLAS.md"),
            File("../docs/GALAXOT_SCREEN_ATLAS.md"),
        ).firstOrNull(File::isFile)?.readText() ?: error("Atlas missing")
        val screensDir = File(root, "com/arturo254/opentune/ui/screens")
        val screens = screensDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue(screens.size >= 97)
        screens.forEach { file ->
            val relative = file.relativeTo(screensDir).invariantSeparatorsPath
            assertTrue("Missing file in screen atlas: $relative", atlas.contains("`$relative`"))
        }
        assertTrue(atlas.contains("ui/player/GalaxStudioPlayer/"))
        assertTrue(atlas.contains("ui/player/LyricsScreen.kt"))
        assertTrue(atlas.contains("ui/player/Queue.kt"))
    }

    @Test
    fun sixSettingsPagesHaveTheirOwnVisualIdentityButOriginalPreferences() {
        val areas = mapOf(
            "AppearanceSettings.kt" to "APPEARANCE",
            "PlayerSettings.kt" to "PLAYER",
            "ContentSettings.kt" to "CONTENT",
            "StorageSettings.kt" to "STORAGE",
            "PrivacySettings.kt" to "PRIVACY",
            "IntegrationScreen.kt" to "INTEGRATIONS",
        )
        areas.forEach { (file, area) ->
            val screen = source("ui/screens/settings/$file")
            assertTrue(screen.contains("GalaxSettingsSectionHero("))
            assertTrue(screen.contains("GalaxSettingsArea.$area"))
            assertTrue(screen.contains("windowInsetsPadding("))
        }
        assertTrue(source("ui/screens/settings/AppearanceSettings.kt").contains("rememberPreference("))
        assertTrue(source("ui/screens/settings/PlayerSettings.kt").contains("AudioQualityKey"))
        assertTrue(source("ui/screens/settings/PrivacySettings.kt").contains("PauseListenHistoryKey"))
    }

    @Test
    fun mainTabNavigationAndPlayerRoutesRemainPreserved() {
        val screens = source("ui/screens/Screens.kt")
        val navigation = source("ui/screens/NavigationBuilder.kt")
        val layout = source("ui/screens/settings/SettingsLayout.kt")
        assertTrue(screens.contains("MainScreens = listOf(Home, Search, Library)"))
        assertTrue(navigation.contains("settings/appearance/player_studio"))
        assertTrue(navigation.contains("settings/appearance/galax_liquid"))
        assertTrue(navigation.contains("UniversalSearchScreen("))
        assertTrue(navigation.contains("LibraryScreen(") || navigation.contains("GalaxMainPages("))
        assertTrue(layout.contains("SettingsProfileHeader()"))
        assertTrue(layout.contains("SettingsGroupCard("))
        assertTrue(source("core/designsystem/liquid/GalaxLiquidModel.kt").contains("LocalGalaxLiquidVariant"))
    }

    @Test
    fun exploreAlternativeNavigatesOnlyToRegisteredChartsRoute() {
        val explore = source("ui/screens/ExploreScreen.kt")
        val navigation = source("ui/screens/NavigationBuilder.kt")
        assertTrue(navigation.contains("composable(\"charts_screen\")"))
        assertTrue(explore.contains("navController.navigate(\"charts_screen\")"))
        assertFalse(explore.contains("navController.navigate(\"charts\")"))
    }

    @Test
    fun creativeShortcutsReachExistingSettingsScreens() {
        val layout = source("ui/screens/settings/SettingsLayout.kt")
        val settings = source("ui/screens/settings/SettingsScreen.kt")
        val shortcuts = source("ui/screens/settings/GalaxSettingsCreativeShortcuts.kt")
        val nav = source("ui/screens/NavigationBuilder.kt")
        assertTrue(layout.contains("GalaxSettingsCreativeShortcuts("))
        assertTrue(layout.contains("state.onPlayerStudioClick"))
        assertTrue(layout.contains("state.onLiquidGlassClick"))
        assertTrue(layout.contains("state.onPaletteClick"))
        assertTrue(settings.contains("settings/appearance/player_studio"))
        assertTrue(settings.contains("settings/appearance/galax_liquid"))
        assertTrue(settings.contains("settings/appearance/palette_picker"))
        assertTrue(shortcuts.contains("onClick = onPlayerStudio"))
        assertTrue(shortcuts.contains("onClick = onLiquidGlass"))
        assertTrue(shortcuts.contains("onClick = onPalette"))
        assertTrue(nav.contains("settings/appearance/player_studio"))
    }

}
