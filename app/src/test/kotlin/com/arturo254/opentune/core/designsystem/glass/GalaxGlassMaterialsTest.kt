package com.arturo254.opentune.core.designsystem.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxGlassMaterialsTest {
    @Test fun frostedIsMoreOpaqueThanClearInBothThemes() {
        for (dark in listOf(true, false)) {
            val frosted = galaxGlassMaterial(GlassMode.FROSTED, dark)
            val clear = galaxGlassMaterial(GlassMode.CLEAR, dark)
            assertTrue(frosted.surfaceTint!!.alpha > clear.surfaceTint!!.alpha)
            assertNotEquals(frosted.borderTint, clear.borderTint)
        }
    }

    @Test fun classicAndDisabledKeepTheirExistingMaterialPolicy() {
        for (mode in listOf(GlassMode.CLASSIC, GlassMode.DISABLED)) {
            val material = galaxGlassMaterial(mode, true)
            assertNull(material.surfaceTint)
            assertNull(material.borderTint)
        }
    }

    @Test fun blurHasSoftDiffusionAndLessEmphasizedEdgesThanClear() {
        for (dark in listOf(true, false)) {
            val blur = galaxGlassMaterial(GlassMode.BLUR, dark)
            val clear = galaxGlassMaterial(GlassMode.CLEAR, dark)
            assertTrue(blur.surfaceTint!!.alpha > clear.surfaceTint!!.alpha)
            assertTrue(blur.borderTint!!.alpha < clear.borderTint!!.alpha)
        }
        assertTrue(galaxGlassOptics(GlassMode.BLUR).blurRadius >
            galaxGlassOptics(GlassMode.FROSTED).blurRadius)
        assertEquals(0f, galaxGlassOptics(GlassMode.BLUR).dispersion, 0f)
    }

    @Test fun clearRetainsAVisibleOutlineWhileFrostedRetainsDiffusion() {
        val clear = galaxGlassMaterial(GlassMode.CLEAR, true)
        val frosted = galaxGlassMaterial(GlassMode.FROSTED, true)
        assertTrue(clear.borderTint!!.alpha > clear.surfaceTint!!.alpha)
        assertEquals(0.20f, frosted.surfaceTint!!.alpha, 0.0001f)
        assertTrue(galaxGlassOptics(GlassMode.FROSTED).blurRadius >
            galaxGlassOptics(GlassMode.CLEAR).blurRadius)
        assertTrue(galaxGlassOptics(GlassMode.CLEAR).refraction >
            galaxGlassOptics(GlassMode.FROSTED).refraction)
    }
}
