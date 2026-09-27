package com.arturo254.opentune.utils

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AppFontKey
import com.arturo254.opentune.constants.AppFontStyle

/**
 * Resolves the FontFamily the whole app should use, based on the user's
 * Ajustes > Apariencia > Tema > Fuente preference.
 *
 * Call this ONCE in your root theme composable (e.g. OpenTuneTheme in
 * Theme.kt) and pass the result into [Typography.withAppFont] — every
 * screen then inherits it automatically through MaterialTheme, no need
 * to read the preference anywhere else in the app.
 *
 * Returns null for [AppFontStyle.SYSTEM] so Compose falls back to the
 * platform's default system font.
 */
@Composable
fun rememberAppFontFamily(): FontFamily? {
    // rememberEnumPreference already exists in this file/package (used by
    // PlayerBackgroundStyleKey elsewhere in the project) — reusing it here.
    val (appFont) = rememberEnumPreference(AppFontKey, AppFontStyle.OPEN_TUNE)
    return when (appFont) {
        AppFontStyle.SYSTEM -> null
        AppFontStyle.MONOCRAFT -> FontFamily(Font(R.font.monocraft))
        // TODO: if your default typeface resource isn't sfprodisplaybold,
        // point this at whatever font OpenTune currently uses by default.
        AppFontStyle.OPEN_TUNE -> FontFamily(Font(R.font.sfprodisplaybold))
    }
}

/**
 * Returns a copy of this [Typography] with [fontFamily] applied to every
 * text style at once, so you don't have to touch each of the ~15 styles
 * individually wherever Typography is defined.
 *
 * Usage in Theme.kt:
 * ```
 * val fontFamily = rememberAppFontFamily()
 * MaterialTheme(
 *     colorScheme = colorScheme,
 *     typography = AppTypography.withAppFont(fontFamily), // was: typography = AppTypography
 *     content = content,
 * )
 * ```
 */
fun Typography.withAppFont(fontFamily: FontFamily?): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
)