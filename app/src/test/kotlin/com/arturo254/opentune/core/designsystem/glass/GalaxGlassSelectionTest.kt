package com.arturo254.opentune.core.designsystem.glass

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Deterministic migration and source-ownership checks for the 4.5.7 selector. */
class GalaxGlassSelectionTest {
    private val root = listOf(File("src/main"), File("app/src/main"))
        .firstOrNull(File::isDirectory) ?: error("Android main source root unavailable")

    private fun source(path: String) = File(root, "kotlin/com/arturo254/opentune/$path").readText()
    private fun resource(path: String) = File(root, "res/$path").readText()

    @Test fun everyPersistedModeRoundTrips() {
        for (mode in GlassMode.entries) {
            assertEquals(mode, parseGalaxGlassMode(mode.name))
            assertEquals(mode, parseGalaxGlassMode(mode.name.lowercase()))
            assertEquals(mode, displayGalaxGlassMode(mode.name, legacyEnabled = false))
        }
        assertEquals(5, GlassMode.entries.size)
    }

    @Test fun missingOrCorruptSelectionPreservesLegacyPreference() {
        for (invalid in listOf(null, "", "invalid", "SOME_OBSOLETE_MODE")) {
            assertNull(parseGalaxGlassMode(invalid))
            assertEquals(GlassMode.CLASSIC, displayGalaxGlassMode(invalid, legacyEnabled = true))
            assertEquals(GlassMode.DISABLED, displayGalaxGlassMode(invalid, legacyEnabled = false))
        }
    }

    @Test fun explicitSelectionAlwaysOverridesLegacyBoolean() {
        for (mode in GlassMode.entries) {
            assertEquals(
                mode != GlassMode.DISABLED,
                isGalaxGlassEnabled(false, mode),
            )
            assertEquals(
                mode != GlassMode.DISABLED,
                isGalaxGlassEnabled(true, mode),
            )
        }
        assertTrue(isGalaxGlassEnabled(true, null))
        assertFalse(isGalaxGlassEnabled(false, null))
    }

    @Test fun appearanceUsesOneFiveModeSelectorAndPersistsLegacyCompatibleBoolean() {
        val preferences = source("constants/PreferenceKeys.kt")
        val appearance = source("ui/screens/settings/AppearanceSettings.kt")
        assertTrue(preferences.contains("GalaxGlassModeKey = stringPreferencesKey(\"galax_glass_mode\")"))
        assertTrue(appearance.contains("selectedValue = selectedGalaxGlassMode"))
        assertTrue(appearance.contains("preferencesContext.dataStore.edit { settings ->"))
        assertTrue(appearance.contains("settings[GalaxGlassModeKey] = mode.name"))
        assertTrue(appearance.contains("settings[LiquidGlassNavBarKey] = mode != GlassMode.DISABLED"))
        assertTrue(appearance.contains("GlassMode.entries") ||
            appearance.contains("EnumListPreference("))
        assertFalse(appearance.contains("checked = liquidGlassNavBar,"))
    }

    @Test fun kyantCanvasBackdropIsolatedFromNavHostRenderNodes() {
        val scaffold = source("MainActivityScaffold.kt")
        val canvas = source("core/designsystem/glass/GalaxCanvasBackdrop.kt")
        assertTrue(scaffold.contains("parseGalaxGlassMode(storedGalaxGlassMode)"))
        assertTrue(scaffold.contains("LocalGalaxGlassMode provides selectedGlassMode"))
        assertTrue(scaffold.contains("LocalEnableLiquidGlass provides enableGalaxGlass"))
        assertTrue(scaffold.contains("LocalLiquidBackdrop provides if (enableAmbientBackdrop)"))
        assertTrue(scaffold.contains("LocalGalaxSafeCanvasBackdrop provides enableAmbientBackdrop"))
        assertTrue(scaffold.contains("rememberGalaxCanvasBackdrop(pureBlack)"))
        assertTrue(canvas.contains("rememberCanvasBackdrop(onDraw = drawAmbient)"))
        assertFalse(scaffold.contains("rememberLayerBackdrop("))
        assertFalse(scaffold.contains("Modifier.layerBackdrop("))
        assertFalse(canvas.contains("Modifier.layerBackdrop("))
        assertFalse(canvas.contains("rememberLayerBackdrop("))
    }

    @Test fun englishAndSpanishOptionsAreAllPresent() {
        val keys = listOf("galax_glass_mode", "galax_glass_frosted",
            "galax_glass_clear", "galax_glass_blur", "galax_glass_classic",
            "galax_glass_disabled")
        for (language in listOf("values", "values-es")) {
            val strings = resource("$language/opentune_strings.xml")
            for (key in keys) assertTrue("$language is missing $key",
                strings.contains("name=\"$key\""))
        }
    }
}
