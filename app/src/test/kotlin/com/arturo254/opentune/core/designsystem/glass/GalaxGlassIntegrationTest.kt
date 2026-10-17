package com.arturo254.opentune.core.designsystem.glass

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level ownership guards; visual shaders still need device validation. */
class GalaxGlassIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin root not found")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun oneRealKyantBackendIsUsedByOldAndNewSurfaces() {
        val engine = source("core/designsystem/glass/GlassEngine.kt")
        val facade = source("core/designsystem/glass/GalaxGlassSurface.kt")
        val plan = source("core/designsystem/glass/GalaxGlassContracts.kt")
        assertTrue(facade.contains("fun Modifier.galaxGlassEffect("))
        assertTrue(facade.contains("fun GalaxGlassSurface("))
        assertTrue(facade.contains("openTuneGlassEffect("))
        assertTrue(facade.contains("GlassSurface("))
        assertTrue(engine.contains("fun Modifier.openTuneGlassEffect("))
        assertTrue(engine.contains("renderModeOverride: GlassRenderMode? = null"))
        assertTrue(engine.contains("allowStaticShader && Build.VERSION.SDK_INT"))
        assertTrue(engine.contains("drawBackdrop("))
        assertTrue(plan.contains("enum class GlassMode"))
        assertFalse(facade.contains("ExoPlayer("))
        assertFalse(plan.contains("MediaSession("))
        assertFalse(plan.contains("MusicService("))
    }

    @Test fun navigationAndMiniPlayerUseCentralCapabilityPlanner() {
        val nav = source("ui/component/FloatingNavigationToolbar.kt")
        val mini = source("ui/player/visual/PlayerGlassEffect.kt")
        assertTrue(nav.contains(".galaxGlassEffect("))
        assertTrue(nav.contains("GlassSurfaceType.NAVIGATION"))
        assertTrue(mini.contains("galaxGlassEffect("))
        assertTrue(mini.contains("GlassSurfaceType.MINI_PLAYER"))
        assertTrue(mini.contains("GlassPreset.SOFT.optics"))
        val facade = source("core/designsystem/glass/GalaxGlassSurface.kt")
        assertTrue(facade.contains("resolveGalaxGlassPlan("))
        assertTrue(facade.contains("LocalEnableLiquidGlass.current"))
        assertTrue(facade.contains("LocalGalaxGlassMode.current"))
        assertTrue(facade.contains("motion.reducedMotion"))
    }

    @Test fun newModesRouteThroughOneRendererWithoutBreakingClassic() {
        val engine = source("core/designsystem/glass/GlassEngine.kt")
        val facade = source("core/designsystem/glass/GalaxGlassSurface.kt")
        val materials = source("core/designsystem/glass/GalaxGlassMaterials.kt")
        val contracts = source("core/designsystem/glass/GalaxGlassContracts.kt")
        assertTrue(facade.contains("resolveGalaxGlassSpec(spec, globalMode)"))
        assertTrue(facade.contains("isGalaxGlassEnabled("))
        assertTrue(facade.contains("material = material"))
        assertTrue(engine.contains("material?.enableVibrancy != false"))
        assertTrue(engine.contains("enableLens && material?.enableLens != false"))
        assertTrue(engine.contains("material?.fallbackTint ?: fallbackSurface"))
        assertTrue(engine.contains("if (!shaderAllowed)"))
        assertTrue(engine.contains("brush = Brush.linearGradient("))
        assertTrue(materials.contains("GlassMode.CLASSIC, GlassMode.DISABLED"))
        assertTrue(contracts.contains("spec.copy(mode = globalMode, optics = null)"))
        assertFalse(facade.contains("spec.copy(mode = selectedMode)"))
    }

    @Test fun globalModesAreNotSilentlyWrittenToDataStoreBeforeFourFiveSeven() {
        val plan = source("core/designsystem/glass/GalaxGlassContracts.kt")
        assertTrue(plan.contains("staticCompositionLocalOf<GlassMode?> { null }"))
        assertFalse(plan.contains("stringPreferencesKey("))
        assertFalse(plan.contains("SharedPreferences"))
    }
}
