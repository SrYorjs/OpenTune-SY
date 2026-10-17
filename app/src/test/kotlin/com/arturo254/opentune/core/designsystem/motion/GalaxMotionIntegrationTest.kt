package com.arturo254.opentune.core.designsystem.motion

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source contracts; verify the visual transitions and haptics on real Android. */
class GalaxMotionIntegrationTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Kotlin source root missing")
    private fun source(path: String): String =
        File(root, "com/arturo254/opentune/$path").readText()

    @Test fun appNavigationUsesOneSharedMotionSourceForAllTransitions() {
        val scaffold = source("MainActivityScaffold.kt")
        assertTrue(scaffold.contains("val galaxMotion = OpenTuneDesignSystem.galaxMotion"))
        assertTrue(scaffold.contains("animationSpec = galaxMotion.navigationBarSpec"))
        assertTrue(scaffold.split("galaxEnterTransition(").size == 3)
        assertTrue(scaffold.split("galaxExitTransition(").size == 3)
        assertFalse(scaffold.contains("animationSpec = NavigationBarAnimationSpec"))
    }

    @Test fun motionPreferenceIsSavedAndMaterialAndGlassRespectIt() {
        val keys = source("constants/PreferenceKeys.kt")
        val settings = source("ui/screens/settings/AppearanceSettings.kt")
        val host = source("MainActivityThemeHost.kt")
        val theme = source("ui/theme/Theme.kt")
        val provider = source("core/designsystem/theme/DesignSystemTheme.kt")
        val glass = source("ui/component/LiquidGlassControls.kt")
        assertTrue(keys.contains("booleanPreferencesKey(\"galaxReduceMotion\")"))
        assertTrue(settings.contains("onGalaxReduceMotionChange"))
        assertTrue(settings.contains("galax_reduce_motion"))
        assertTrue(host.contains("rememberPreference(GalaxReduceMotionKey, defaultValue = false)"))
        assertTrue(host.contains("reduceMotion = reduceMotion"))
        assertTrue(theme.contains("rememberCoroutineScope().coroutineContext[MotionDurationScale]"))
        assertTrue(provider.contains("rememberCoroutineScope().coroutineContext[MotionDurationScale]"))
        assertTrue(theme.contains("if (effectiveReducedMotion) colorScheme else updateTransition(colorScheme)"))
        assertTrue(provider.contains("LocalGalaxMotionPolicy provides galaxMotion"))
        assertTrue(glass.contains("if (policy.reducedMotion) return this"))
    }

    @Test fun sharedButtonsDispatchThroughExistingAppHapticPreference() {
        val scaffold = source("MainActivityScaffold.kt")
        val interaction = source("core/designsystem/motion/GalaxInteractions.kt")
        val controls = source("core/designsystem/component/ControlComponents.kt")
        assertTrue(scaffold.contains("LocalHapticFeedback provides customHaptic"))
        assertTrue(scaffold.contains("EnableHapticFeedbackKey"))
        assertTrue(interaction.contains("LocalHapticFeedback.current"))
        assertTrue(interaction.contains("GalaxHapticCue.SELECTION"))
        assertTrue(controls.contains("galaxPressFeedback(interactionSource, enabled)"))
        assertTrue(controls.contains("rememberGalaxHapticFeedback()"))
        val toolbar = source("ui/component/FloatingNavigationToolbar.kt")
        assertTrue(toolbar.contains("haptic(GalaxHapticCue.SELECTION)"))
        assertTrue(toolbar.contains("onItemClick(screen, selected)"))
        assertFalse(interaction.contains("Vibrator("))
    }
}
