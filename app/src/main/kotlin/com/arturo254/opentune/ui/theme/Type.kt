/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R

/**
 * Premium music typography: clean body copy with stronger display faces.
 *
 * The project already ships both families, so this does not add font assets or
 * runtime downloads. Headings intentionally have tighter tracking while body
 * text stays highly readable at small sizes.
 */
private val AppFontFamily =
    FontFamily(
        Font(R.font.poppins, weight = FontWeight.Normal),
        Font(R.font.sfprodisplaybold, weight = FontWeight.Bold),
    )

private fun buildTypography(fontFamily: FontFamily) =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 54.sp,
                lineHeight = 58.sp,
                letterSpacing = (-0.8).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp,
                lineHeight = 49.sp,
                letterSpacing = (-0.65).sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
                lineHeight = 41.sp,
                letterSpacing = (-0.45).sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.42).sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 27.sp,
                lineHeight = 33.sp,
                letterSpacing = (-0.34).sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 23.sp,
                lineHeight = 29.sp,
                letterSpacing = (-0.24).sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                lineHeight = 27.sp,
                letterSpacing = (-0.18).sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.05).sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.05.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.03.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                letterSpacing = 0.05.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                letterSpacing = 0.02.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.08.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.12.sp,
            ),
    )

val AppTypography = buildTypography(AppFontFamily)
val SystemTypography = buildTypography(FontFamily.Default)
