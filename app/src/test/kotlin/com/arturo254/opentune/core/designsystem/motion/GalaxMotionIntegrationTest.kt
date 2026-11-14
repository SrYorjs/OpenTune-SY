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
        val host = source("MainActivity.kt")
        val graph = source("ui/original/screens/NavigationBuilder.kt")
        val motion = source("ui/original/utils/AppMotion.kt")
        assertTrue(host.contains("createLifecycleAwareWindowRecomposer("))
        assertTrue(host.contains("AppMotion.durationScale"))
        assertTrue(host.split("defaultMotionEnter {").size == 3)
        assertTrue(host.split("defaultMotionExit {").size == 3)
        assertTrue(graph.split("motionEnter {").size == 3)
        assertTrue(graph.split("motionExit {").size == 3)
        assertTrue(motion.contains("if (AppMotion.enabled) transition() else EnterTransition.None"))
        assertTrue(motion.contains("if (AppMotion.enabled) transition() else ExitTransition.None"))
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
        val host = source("ui/original/adapters/ActivityThemeHost.kt")
        val interaction = source("core/designsystem/motion/GalaxInteractions.kt")
        val controls = source("core/designsystem/component/ControlComponents.kt")
        val nav = source("ui/original/component/AppNavigation.kt")
        assertTrue(host.contains("rememberPreference(EnableHapticFeedbackKey, true)"))
        assertTrue(host.contains("LocalHapticFeedback provides guardedHaptics"))
        assertTrue(host.contains("if (hapticsEnabled) platformHaptics.performHapticFeedback"))
        assertTrue(interaction.contains("LocalHapticFeedback.current"))
        assertTrue(controls.contains("galaxPressFeedback(interactionSource, enabled)"))
        assertTrue(controls.contains("rememberGalaxHapticFeedback()"))
        assertTrue(nav.contains("rememberGalaxHapticFeedback()"))
        assertTrue(nav.contains("GalaxHapticCue.SELECTION"))
        assertTrue(nav.contains("onItemClick(screen, isSelected)"))
        assertFalse(interaction.contains("Vibrator("))
    }
}
