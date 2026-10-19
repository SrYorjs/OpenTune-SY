package com.arturo254.opentune.core.designsystem.theme

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Structural contracts: runtime colors and animations still require phone testing. */
class GalaxThemeWiringTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun brandThemeUsesCanonicalMaterialAndDesignSystemPipeline() {
        val theme = source("ui/theme/Theme.kt")
        val provider = source("core/designsystem/theme/DesignSystemTheme.kt")
        assertTrue(theme.contains("val DefaultThemeColor = GalaxPalette.Galaxy"))
        assertTrue(theme.contains("useGalaxBrandPalette: Boolean = false"))
        assertTrue(theme.contains("useGalaxBrandPalette -> remember(darkTheme) { galaxColorScheme(darkTheme) }"))
        assertTrue(theme.contains("seedPalette != null"))
        assertTrue(theme.contains("dynamicDarkColorScheme(context)"))
        assertTrue(theme.contains("baseColorScheme.premiumMusicSurfaces(darkTheme)"))
        assertTrue(theme.contains("pureBlack(true)"))
        assertTrue(theme.contains("ProvideOpenTuneDesignSystem("))
        assertTrue(provider.contains("LocalGalaxDesignTokens provides galax"))
        assertTrue(provider.contains("createOpenTuneColorRoles(colorScheme)"))
        assertTrue(provider.contains("createGalaxDesignTokens(colorScheme, materialTypography, artworkRadiusDp)"))
    }

    @Test fun oldSavedThemeColorsAndFontEnumRemainSupported() {
        val host = source("MainActivityThemeHost.kt")
        val settings = source("ui/screens/settings/AppearanceSettings.kt")
        assertTrue(host.contains("rememberPreference(DynamicThemeKey, defaultValue = false)"))
        assertTrue(host.contains("useGalaxBrandPalette = !artworkThemeActive && !enableDynamicTheme && customThemeColorValue == \"default\""))
        assertTrue(host.contains("ThemeSeedPaletteCodec.decodeFromPreference("))
        assertTrue(host.contains("ThemePalettes"))
        assertTrue(host.contains("CustomThemeColorKey"))
        assertTrue(settings.contains("defaultValue = false"))
        assertTrue(settings.contains("onDynamicThemeChange"))
        assertTrue(settings.contains("AppFontStyle.OPEN_TUNE -> \"GalaxOT\""))
        assertTrue(settings.contains("AppFontStyle.SYSTEM"))
        assertTrue(settings.contains("AppFontStyle.MONOCRAFT"))
        assertFalse(host.contains("applicationId ="))
    }
}
