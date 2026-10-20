package com.arturo254.opentune.ui.player.preferences

import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.ui.player.layout.PlayerLayoutFamily
import com.arturo254.opentune.ui.player.layout.resolvePlayerLayout
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Persistence contracts only; re-launch with saved settings on a real device. */
class PlayerPresentationPreferencesIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String) =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun originalKeysDefaultsAndEnumValuesStayCompatible() {
        val keys = source("constants/PreferenceKeys.kt")
        val prefs = source("ui/player/preferences/PlayerPresentationPreferences.kt")
        assertTrue(keys.contains("stringPreferencesKey(\"playerDesignStyle\")"))
        assertTrue(keys.contains("stringPreferencesKey(\"playerBackgroundStyle\")"))
        assertTrue(keys.contains("stringPreferencesKey(\"player_buttons_style\")"))
        assertTrue(keys.contains("stringPreferencesKey(\"sliderStyle\")"))
        assertEquals(listOf("V1", "V2", "V3", "V4", "V5", "V6", "V7", "V8",
            "V9", "V10", "V11", "V12", "V13", "V14", "V15", "V16", "V17"),
            PlayerDesignStyle.entries.map { it.name })
        assertEquals(PlayerDesignStyle.V8, PlayerPresentationDefaults.designStyle)
        assertEquals(PlayerBackgroundStyle.DEFAULT, PlayerPresentationDefaults.backgroundStyle)
        assertEquals(PlayerButtonsStyle.DEFAULT, PlayerPresentationDefaults.buttonStyle)
        assertEquals(SliderStyle.Standard, PlayerPresentationDefaults.sliderStyle)
        assertTrue(prefs.contains("rememberEnumPreference("))
        listOf("PlayerDesignStyleKey", "PlayerBackgroundStyleKey",
            "PlayerButtonsStyleKey", "SliderStyleKey").forEach {
            assertTrue(prefs.contains("key = $it"))
        }
        assertFalse(prefs.contains("stringPreferencesKey("))
    }

    @Test fun settingsSelectorsAndFullPlayerSharePersistentSourceOfTruth() {
        val settings = source("ui/screens/settings/AppearanceSettings.kt")
        val selectors = source("ui/player/preferences/PlayerPresentationSelectors.kt")
        val player = source("ui/player/Player.kt")
        assertTrue(settings.contains("settings/appearance/player_studio"))
        assertTrue(source("ui/screens/settings/GalaxPlayerStyleStudioScreen.kt").contains("onSelected(style)"))
        assertTrue(settings.contains("PlayerBackgroundSkinSelector("))
        assertTrue(settings.contains("PlayerButtonsSkinSelector("))
        assertTrue(source("ui/screens/settings/GalaxPlayerStyleStudioScreen.kt").contains("PlayerDesignStyleKey"))
        assertTrue(settings.contains("onSelected = onPlayerBackgroundChange"))
        assertTrue(settings.contains("onSelected = onPlayerButtonsStyleChange"))
        assertTrue(selectors.contains("selectedValue = selected"))
        assertTrue(selectors.contains("onValueSelected = onSelected"))
        listOf("V1","V2","V3","V4","V5","V6","V7","V8").forEach { name ->
            assertTrue(selectors.contains("PlayerDesignStyle.$name ->"))
        }
        assertTrue(player.contains("val presentation = rememberPlayerPresentationPreferences()"))
        assertTrue(player.contains("val playerDesignStyle = presentation.designStyle"))
        assertTrue(player.contains("val playerBackground = presentation.backgroundStyle"))
        assertTrue(player.contains("val playerButtonsStyle = presentation.buttonStyle"))
        assertTrue(player.contains("val sliderStyle = presentation.sliderStyle"))
        assertTrue(player.contains("rememberVisualSkin("))
    }

    @Test fun savedV1AndV3StillRouteToClassicAndRetainTheirControls() {
        val c = source("ui/player/PlayerComponents.kt")
        val settings = source("ui/screens/settings/AppearanceSettings.kt")
        for (style in listOf(PlayerDesignStyle.V1, PlayerDesignStyle.V3)) {
            for (landscape in listOf(false, true)) {
                val route = resolvePlayerLayout(style, landscape)
                assertEquals(style, route.style)
                assertEquals(PlayerLayoutFamily.CLASSIC, route.family)
                assertTrue(route.hasSharedBackdrop)
            }
        }
        assertTrue(c.contains("PlayerDesignStyle.V1 -> {"))
        assertTrue(c.contains("PlayerDesignStyle.V3 -> {"))
        assertTrue(settings.contains("PlayerDesignStyleKey"))
        assertTrue(settings.contains("PlayerBackgroundStyleKey"))
        assertTrue(settings.contains("PlayerButtonsStyleKey"))
        assertTrue(settings.contains("SliderStyleKey"))
        assertTrue(settings.contains("showSliderOptionDialog"))
        assertTrue(settings.contains("playerFullscreen"))
    }
}
