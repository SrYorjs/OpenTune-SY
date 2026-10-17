package com.arturo254.opentune.core.designsystem.glass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxGlassContractsTest {
    private fun caps(
        sdk: Int = 35,
        backdrop: Boolean = true,
        powerSave: Boolean = false,
        lowRam: Boolean = false,
    ) = GlassCapabilities(
        sdkInt = sdk,
        hasLiveBackdrop = backdrop,
        isPowerSaveMode = powerSave,
        isLowRamDevice = lowRam,
    )

    @Test fun fiveModesMatchTheGalaxRoadmapWithoutChangingClassicPreset() {
        assertEquals(
            listOf("FROSTED", "CLEAR", "BLUR", "CLASSIC", "DISABLED"),
            GlassMode.entries.map { it.name },
        )
        assertEquals(GlassPreset.STANDARD.optics, galaxGlassOptics(GlassMode.CLASSIC))
        assertTrue(galaxGlassOptics(GlassMode.BLUR).blurRadius >
            galaxGlassOptics(GlassMode.CLEAR).blurRadius)
    }

    @Test fun selectedMaterialOverridesLegacyPresetsWithoutBreakingClassic() {
        val legacyNav = GlassSurfaceSpec(
            mode = GlassMode.CLASSIC,
            surface = GlassSurfaceType.NAVIGATION,
            optics = GlassPreset.SOFT.optics,
        )
        assertEquals(legacyNav, resolveGalaxGlassSpec(legacyNav, null))
        assertEquals(legacyNav, resolveGalaxGlassSpec(legacyNav, GlassMode.CLASSIC))
        for (mode in listOf(GlassMode.FROSTED, GlassMode.CLEAR, GlassMode.BLUR)) {
            val resolved = resolveGalaxGlassSpec(legacyNav, mode)
            assertEquals(mode, resolved.mode)
            assertEquals(null, resolved.optics)
            assertEquals(
                galaxGlassOptics(mode),
                resolveGalaxGlassPlan(true, resolved, caps()).optics,
            )
        }
        assertEquals(GlassPreset.SOFT.optics, resolveGalaxGlassPlan(true, legacyNav, caps()).optics)
    }

    @Test fun explicitSelectionHasPriorityOverLegacySwitch() {
        assertFalse(isGalaxGlassEnabled(false, null))
        assertTrue(isGalaxGlassEnabled(true, null))
        assertFalse(isGalaxGlassEnabled(true, GlassMode.DISABLED))
        assertTrue(isGalaxGlassEnabled(false, GlassMode.CLEAR))
        assertTrue(isGalaxGlassEnabled(false, GlassMode.CLASSIC))
    }

    @Test fun disabledAndLegacyOffAreAlwaysOpaqueRegardlessOfCapabilities() {
        val d = resolveGalaxGlassPlan(true, GlassSurfaceSpec(mode = GlassMode.DISABLED), caps())
        assertEquals(GlassRenderMode.OPAQUE, d.renderer)
        assertEquals(GlassFallbackStrategy.OPAQUE, d.fallback)
        assertFalse(d.hasExpensiveBackdrop)
        for (mode in GlassMode.entries) {
            val off = resolveGalaxGlassPlan(false, GlassSurfaceSpec(mode = mode), caps())
            assertEquals(GlassRenderMode.OPAQUE, off.renderer)
        }
    }

    @Test fun liveBackdropIsUsedOnlyWhenSuppliedAndQualityAllowsIt() {
        val high = resolveGalaxGlassPlan(true, GlassSurfaceSpec(), caps())
        assertEquals(GlassRenderMode.LIVE_BACKDROP, high.renderer)
        assertEquals(GlassPerformanceTier.HIGH, high.performanceTier)
        assertEquals(GlassFallbackStrategy.NONE, high.fallback)
        assertTrue(high.hasExpensiveBackdrop)
        val without = resolveGalaxGlassPlan(true, GlassSurfaceSpec(), caps(backdrop = false))
        assertEquals(GlassRenderMode.STATIC_FALLBACK, without.renderer)
        assertEquals(GlassFallbackStrategy.STATIC_SHADER, without.fallback)
    }

    @Test fun batterySaverLowRamAndScrollingItemsUseCheapTintNotShaders() {
        val cases = listOf(
            caps(powerSave = true) to GlassSurfaceType.NAVIGATION,
            caps(lowRam = true) to GlassSurfaceType.PANEL,
            caps() to GlassSurfaceType.SCROLLING_ITEM,
        )
        for ((device, surface) in cases) {
            val plan = resolveGalaxGlassPlan(true,
                GlassSurfaceSpec(mode = GlassMode.CLEAR, surface = surface), device)
            assertEquals(GlassPerformanceTier.LOW, plan.performanceTier)
            assertEquals(GlassRenderMode.STATIC_FALLBACK, plan.renderer)
            assertEquals(GlassFallbackStrategy.TINT_AND_OUTLINE, plan.fallback)
            assertFalse(plan.hasExpensiveBackdrop)
        }
    }

    @Test fun frostedAndBlurFallBackWithoutFakeStaticRefraction() {
        for (mode in listOf(GlassMode.FROSTED, GlassMode.BLUR)) {
            val plan = resolveGalaxGlassPlan(
                true, GlassSurfaceSpec(mode = mode), caps(backdrop = false),
            )
            assertEquals(GlassRenderMode.STATIC_FALLBACK, plan.renderer)
            assertEquals(GlassFallbackStrategy.TINT_AND_OUTLINE, plan.fallback)
        }
        val clear = resolveGalaxGlassPlan(
            true, GlassSurfaceSpec(mode = GlassMode.CLEAR), caps(backdrop = false),
        )
        assertEquals(GlassFallbackStrategy.STATIC_SHADER, clear.fallback)
    }

    @Test fun androidOldApiFallbackDoesNotPromiseUnsupportedShaders() {
        assertFalse(caps(sdk = 30).hasRuntimeShader)
        assertFalse(caps(sdk = 30).hasRenderEffect)
        assertTrue(caps(sdk = 31).hasRenderEffect)
        assertFalse(caps(sdk = 32).hasRuntimeShader)
        assertTrue(caps(sdk = 33).hasRuntimeShader)
        val old = resolveGalaxGlassPlan(true, GlassSurfaceSpec(), caps(sdk = 30, backdrop = false))
        assertEquals(GlassRenderMode.STATIC_FALLBACK, old.renderer)
        assertEquals(GlassFallbackStrategy.TINT_AND_OUTLINE, old.fallback)
    }

    @Test fun reducedMotionKeepsInputCommandsAndCapsOpticalWork() {
        val selected = GlassSurfaceSpec(mode = GlassMode.FROSTED)
        val plan = resolveGalaxGlassPlan(true, selected, caps(), reducedMotion = true)
        assertEquals(GlassPerformanceTier.BALANCED, plan.performanceTier)
        assertEquals(GlassRenderMode.LIVE_BACKDROP, plan.renderer)
        assertTrue(plan.optics.blurRadius <= 6)
        assertEquals(0f, plan.optics.dispersion, 0f)
        assertEquals(GlassMode.FROSTED, plan.mode)
    }
}
