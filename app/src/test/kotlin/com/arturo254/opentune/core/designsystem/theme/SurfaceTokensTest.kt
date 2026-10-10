package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SurfaceTokensTest {
    @Test
    fun glassWidthsAndShadowRadiiPreserveExistingLook() {
        val surface = createOpenTuneSurfaceTokens(darkColorScheme())

        assertEquals(0.8.dp, surface.borders.fallbackGlassWidth)
        assertEquals(0.75.dp, surface.borders.activeGlassWidth)
        assertEquals(0.8.dp, surface.borders.glassCardWidth)
        assertEquals(1.dp, surface.borders.artworkPreviewWidth)
        assertEquals(8.dp, surface.shadows.glassOuterRadius)
        assertEquals(4.dp, surface.shadows.glassInnerRadius)
        assertEquals(0.22f, surface.opacity.innerGlassShadowDark, 0f)
        assertEquals(0.14f, surface.opacity.innerGlassShadowLight, 0f)
    }

    @Test
    fun cardAndDialogElevationsPreserveExistingValues() {
        val elevation = createOpenTuneSurfaceTokens(lightColorScheme()).elevations
        assertEquals(0.dp, elevation.flat)
        assertEquals(4.dp, elevation.releaseNotesCard)
        assertEquals(6.dp, elevation.releaseNotesSurfaceTint)
        assertEquals(6.dp, elevation.artworkSettingsDialog)
    }

    @Test
    fun glassControlAndArtworkOpacitiesAreUnchanged() {
        val o = createOpenTuneSurfaceTokens(darkColorScheme()).opacity
        assertEquals(0.10f, o.selectedGlassIcon, 0f)
        assertEquals(0.11f, o.selectedGlassPill, 0f)
        assertEquals(0.92f, o.glassClusterOpaque, 0f)
        assertEquals(0.28f, o.activeArtworkOverlay, 0f)
        assertEquals(0.075f, o.glassCardBlackTop, 0f)
        assertEquals(0.045f, o.glassCardBlackAccent, 0f)
        assertEquals(0.045f, o.glassCardBlackBottom, 0f)
        assertEquals(0.16f, o.glassCardLightTop, 0f)
        assertEquals(0.055f, o.glassCardLightAccent, 0f)
        assertEquals(0.08f, o.glassCardLightSurface, 0f)
        assertEquals(0.82f, o.glassCardOpaqueGradientEnd, 0f)
    }

    @Test
    fun semanticBordersFollowCurrentMaterialScheme() {
        val a = darkColorScheme(
            outlineVariant = Color(0xFF123456),
            outline = Color(0xFF567890),
        )
        val b = lightColorScheme(
            outlineVariant = Color(0xFF334455),
            outline = Color(0xFF778899),
        )
        val dark = createOpenTuneSurfaceTokens(a).borders
        val light = createOpenTuneSurfaceTokens(b).borders
        assertEquals(Color.White.copy(alpha = 0.18f), dark.glassCardPureBlackColor)
        assertEquals(Color.White.copy(alpha = 0.22f), dark.glassCardStandardColor)
        assertEquals(a.outlineVariant.copy(alpha = 0.20f), dark.glassCardOpaqueColor)
        assertEquals(a.outline.copy(alpha = 0.50f), dark.artworkPreviewColor)
        assertEquals(b.outlineVariant.copy(alpha = 0.20f), light.glassCardOpaqueColor)
        assertEquals(b.outline.copy(alpha = 0.50f), light.artworkPreviewColor)
        assertNotEquals(dark.artworkPreviewColor, light.artworkPreviewColor)
    }
}
