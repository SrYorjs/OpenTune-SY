package com.arturo254.opentune.core.designsystem.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GlassEngineTest {
    @Test
    fun glassModeSelectsSafeFallbacks() {
        assertEquals(GlassRenderMode.OPAQUE, chooseGlassRenderMode(false, false))
        assertEquals(GlassRenderMode.OPAQUE, chooseGlassRenderMode(false, true))
        assertEquals(GlassRenderMode.STATIC_FALLBACK, chooseGlassRenderMode(true, false))
        assertEquals(GlassRenderMode.LIVE_BACKDROP, chooseGlassRenderMode(true, true))
    }

    @Test
    fun presetsMatchPreviousControlsAndMiniPlayer() {
        val soft = GlassPreset.SOFT.optics
        assertEquals(4, soft.blurRadius)
        assertEquals(0.045f, soft.refraction, 0f)
        assertEquals(0.10f, soft.curve, 0f)
        assertEquals(0.0015f, soft.dispersion, 0f)
        assertEquals(0f, soft.edge, 0f)

        val interactive = GlassPreset.INTERACTIVE.optics
        assertEquals(6, interactive.blurRadius)
        assertEquals(0.078f, interactive.refraction, 0f)
        assertEquals(0.004f, interactive.dispersion, 0f)
        assertEquals(0.48f, interactive.edge, 0f)

        val cluster = GlassPreset.CONTROL_CLUSTER.optics
        assertEquals(7, cluster.blurRadius)
        assertEquals(0.072f, cluster.refraction, 0f)
    }

    @Test
    fun legacyLocalsDelegateToCanonicalRuntimeLocals() {
        assertSame(LocalSky, com.arturo254.opentune.ui.component.LocalSky)
        assertSame(LocalEnableLiquidGlass, com.arturo254.opentune.ui.component.LocalEnableLiquidGlass)
        assertSame(LocalLiquidBackdrop, com.arturo254.opentune.ui.component.LocalLiquidBackdrop)
    }
}
