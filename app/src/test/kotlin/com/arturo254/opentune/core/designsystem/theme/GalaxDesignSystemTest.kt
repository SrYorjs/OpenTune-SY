package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxDesignSystemTest {
    @Test fun darkBrandUsesSpaceSurfacesAndHaloPrimary() {
        val s = galaxColorScheme(dark = true)
        assertEquals(GalaxPalette.DeepSpace, s.background)
        assertEquals(GalaxPalette.Void, s.surfaceContainerLowest)
        assertEquals(GalaxPalette.Halo, s.primary)
        assertEquals(GalaxPalette.Nebula, s.primaryContainer)
        assertEquals(GalaxPalette.Ice, s.onPrimaryContainer)
    }

    @Test fun lightBrandIsIndependentAndKeepsMaterialContrast() {
        val s = galaxColorScheme(dark = false)
        assertEquals(GalaxPalette.LightCanvas, s.background)
        assertEquals(Color(0xFF155AAA), s.primary)
        assertEquals(Color.White, s.onPrimary)
        assertFalse(s.background == galaxColorScheme(dark = true).background)
    }

    @Test fun primaryAndTextPairsMeetWcagAaForNormalText() {
        listOf(galaxColorScheme(true), galaxColorScheme(false)).forEach { s ->
            for ((foreground, background) in listOf(
                s.onPrimary to s.primary,
                s.onPrimaryContainer to s.primaryContainer,
                s.onBackground to s.background,
                s.onSurface to s.surface,
                s.onSurfaceVariant to s.surfaceVariant,
                s.onSecondary to s.secondary,
                s.onTertiary to s.tertiary,
            )) {
                assertTrue(
                    "Contrast too low: $foreground on $background",
                    galaxContrastRatio(foreground, background) >= 4.5,
                )
            }
        }
    }

    @Test fun semanticSurfacesFollowCurrentMaterialTheme() {
        val s = galaxColorScheme(true)
        val t = createGalaxDesignTokens(s, Typography(), 16f)
        assertEquals(s.background, t.colors.cosmicBackground)
        assertEquals(s.surfaceContainerLow, t.colors.nebulaSurface)
        assertEquals(s.primary, t.colors.haloAccent)
        assertEquals(s.onPrimary, t.colors.onHaloAccent)
        assertEquals(s.onSurfaceVariant, t.colors.textSecondary)
        assertEquals(48.dp, t.iconography.minimumTouchTarget)
        assertEquals(24.dp, t.iconography.standard)
        assertEquals(32.dp, t.geometry.heroCard.topStart)
    }

    @Test fun typeAndArtworkShapesInheritCurrentPreferences() {
        val material = Typography()
        val a = createGalaxDesignTokens(galaxColorScheme(true), material, 0f)
        val b = createGalaxDesignTokens(galaxColorScheme(false), material, 42f)
        assertEquals(material.headlineLarge, a.typography.headline)
        assertEquals(material.titleMedium, a.typography.track)
        assertEquals(0.dp, a.geometry.artwork.topStart)
        assertEquals(42.dp, b.geometry.artwork.topStart)
        assertEquals(material.displayMedium.fontFamily, a.typography.hero.fontFamily)
    }

    @Test fun dynamicOrCustomSchemesDeriveGalaxRolesWithoutHardcodedBlue() {
        val custom = androidx.compose.material3.darkColorScheme(
            primary = Color(0xFFDBA6EB),
            onPrimary = Color(0xFF35133C),
            background = Color(0xFF18101E),
        )
        val tokens = createGalaxDesignTokens(custom, Typography(), 16f)
        assertEquals(custom.primary, tokens.colors.haloAccent)
        assertEquals(custom.onPrimary, tokens.colors.onHaloAccent)
        assertEquals(custom.background, tokens.colors.cosmicBackground)
        assertFalse(tokens.colors.haloAccent == GalaxPalette.Halo)
    }
}
