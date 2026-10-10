package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TypographyTokensTest {
    @Test
    fun universalSearchRolesPreserveExistingMaterialTextStyles() {
        val material = Typography()
        val roles = createOpenTuneTypographyTokens(material)

        assertEquals(material.titleMedium.copy(fontWeight = FontWeight.Bold), roles.songTitle)
        assertEquals(material.bodyMedium, roles.songSubtitle)
        assertEquals(material.labelSmall, roles.metadata)
        assertEquals(material.labelSmall, roles.badge)
    }

    @Test
    fun titlesAndPlayerRoleReuseMaterialScaleWithoutNewSizes() {
        val material =
            Typography(
                headlineLarge =
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 34.sp,
                        lineHeight = 41.sp,
                    ),
                titleLarge =
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    ),
            )
        val roles = createOpenTuneTypographyTokens(material)

        assertEquals(material.headlineLarge, roles.screenTitle)
        assertEquals(material.titleLarge, roles.sectionTitle)
        assertEquals(material.titleLarge, roles.playerTitle)
    }

    @Test
    fun semanticRolesFollowActiveFontPreferenceAndCustomMetrics() {
        val system =
            Typography(
                titleMedium =
                    TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    ),
                bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif),
                labelSmall = TextStyle(fontFamily = FontFamily.SansSerif),
            )
        val custom =
            Typography(
                titleMedium =
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        letterSpacing = 0.2.sp,
                    ),
                bodyMedium = TextStyle(fontFamily = FontFamily.Monospace),
                labelSmall = TextStyle(fontFamily = FontFamily.Monospace),
            )

        val before = createOpenTuneTypographyTokens(system)
        val after = createOpenTuneTypographyTokens(custom)

        assertNotEquals(before.songTitle, after.songTitle)
        assertEquals(custom.titleMedium.copy(fontWeight = FontWeight.Bold), after.songTitle)
        assertEquals(custom.bodyMedium, after.songSubtitle)
        assertEquals(custom.labelSmall, after.metadata)
        assertEquals(custom.labelSmall, after.badge)
    }
}
