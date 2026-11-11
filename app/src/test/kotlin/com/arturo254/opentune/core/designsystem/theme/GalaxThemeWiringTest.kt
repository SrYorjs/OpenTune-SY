package com.arturo254.opentune.core.designsystem.theme

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test fun originalThemeHostsDesignTokensAndPreservesPlaybackPreferences() {
        val host = source("MainActivityThemeHost.kt")
        assertTrue(host.contains("import com.arturo254.opentune.foundation.theme.OriginalTheme"))
        assertTrue(host.contains("OriginalTheme(pureBlack = pureBlack)"))
        assertTrue(host.indexOf("OriginalTheme(pureBlack = pureBlack)") < host.indexOf("ProvideOpenTuneDesignSystem("))
        assertTrue(host.contains("rememberPreference(PureBlackKey, defaultValue = false)"))
        assertTrue(host.contains("rememberPreference(LyricsSyncOffsetKey, defaultValue = 0)"))
        assertTrue(host.contains("rememberPreference(GalaxReduceMotionKey, defaultValue = false)"))
        assertTrue(host.contains("artworkRadiusDp = artworkCornerRadius"))
        assertTrue(host.contains("reduceMotion = reduceMotion"))
        assertTrue(host.contains("content(pureBlack, lyricsSyncOffset)"))
        assertTrue(host.contains("setSystemBarAppearance(ForceDarkTheme)"))
        assertFalse(host.contains("useGalaxBrandPalette ="))
        assertFalse(host.contains("applicationId ="))
    }

    @Test fun savedThemePreferenceKeysAndFontOptionsRemainAvailable() {
        val preferences = source("constants/PreferenceKeys.kt")
        val settings = source("ui/screens/settings/AppearanceSettings.kt")
        assertTrue(preferences.contains("val DynamicThemeKey = booleanPreferencesKey(\"dynamicTheme\")"))
        assertTrue(preferences.contains("val CustomThemeColorKey = stringPreferencesKey(\"customThemeColor\")"))
        assertTrue(settings.contains("AppFontStyle.OPEN_TUNE"))
        assertTrue(settings.contains("AppFontStyle.SYSTEM"))
        assertTrue(settings.contains("AppFontStyle.MONOCRAFT"))
    }
}
