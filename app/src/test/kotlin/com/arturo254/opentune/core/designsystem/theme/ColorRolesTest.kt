package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorRolesTest {
    @Test
    fun darkGlassRolesPreserveCurrentLiquidGlassAppearance() {
        val scheme =
            darkColorScheme(
                primary = Color(0xFF00ABAB),
                tertiary = Color(0xFF80CBC4),
                secondary = Color(0xFF80DEEA),
                background = Color(0xFF101010),
            )

        val roles = createOpenTuneColorRoles(scheme)

        assertEquals(Color.White.copy(alpha = 0.070f), roles.surfaceGlass)
        assertEquals(Color.White.copy(alpha = 0.18f), roles.outlineGlass)
        assertEquals(Color.Black.copy(alpha = 0.18f), roles.shadowGlass)
        assertEquals(scheme.primary.copy(alpha = 0.10f), roles.badgeAccent)
    }

    @Test
    fun lightGlassRolesPreserveCurrentLiquidGlassAppearance() {
        val scheme =
            lightColorScheme(
                primary = Color(0xFF006A6A),
                background = Color(0xFFF9F9F9),
            )

        val roles = createOpenTuneColorRoles(scheme)

        assertEquals(Color.White.copy(alpha = 0.24f), roles.surfaceGlass)
        assertEquals(Color.White.copy(alpha = 0.58f), roles.outlineGlass)
        assertEquals(Color.Black.copy(alpha = 0.10f), roles.shadowGlass)
    }

    @Test
    fun semanticRolesFollowMaterialThemeInsteadOfHardcodedBrandColors() {
        val scheme =
            darkColorScheme(
                primary = Color(0xFF123456),
                primaryContainer = Color(0xFF234567),
                onSurfaceVariant = Color(0xFFABCDEF),
                tertiary = Color(0xFF345678),
                onTertiary = Color(0xFF456789),
                secondary = Color(0xFF56789A),
                onSecondary = Color(0xFF6789AB),
            )

        val roles = createOpenTuneColorRoles(scheme)

        assertEquals(scheme.surfaceContainerLow, roles.surfaceInteractive)
        assertEquals(scheme.surfaceContainerHigh, roles.surfaceInteractiveStrong)
        assertEquals(scheme.surfaceContainerLowest, roles.surfacePlayer)
        assertEquals(scheme.onSurfaceVariant.copy(alpha = 0.76f), roles.textMuted)
        assertEquals(scheme.primaryContainer, roles.badgeAccentStrong)
        assertEquals(scheme.tertiary, roles.accentPositive)
        assertEquals(scheme.onTertiary, roles.onAccentPositive)
        assertEquals(scheme.secondary, roles.accentWarning)
        assertEquals(scheme.onSecondary, roles.onAccentWarning)
    }
}
