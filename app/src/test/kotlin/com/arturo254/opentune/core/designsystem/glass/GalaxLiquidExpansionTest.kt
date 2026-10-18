package com.arturo254.opentune.core.designsystem.glass

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 4.5 regression: continuous studio scroll, top-level swipe and wider chrome. */
class GalaxLiquidExpansionTest {
    private val root = listOf(File("src/main/kotlin"), File("app/src/main/kotlin"))
        .firstOrNull(File::isDirectory) ?: error("Android Kotlin source root unavailable")
    private fun source(path: String) = File(root, "com/arturo254/opentune/$path").readText()

    @Test fun studioWholeScreenScrollsAndLensDragDoesNotConsumeNormalSwipes() {
        val screen = source("ui/screens/settings/GalaxLiquidSettingsScreen.kt")
        val preview = source("core/designsystem/liquid/GalaxLiquidPlayground.kt")
        assertEquals(1, screen.split(".verticalScroll(rememberScrollState())").size - 1)
        assertTrue(screen.contains("Modifier.fillMaxSize()"))
        assertTrue(preview.contains("detectDragGesturesAfterLongPress"))
        assertFalse(preview.contains("detectDragGestures { change, drag ->"))
        assertTrue(screen.contains("GalaxLiquidPreview("))
        assertTrue(screen.contains("GalaxLiquidOpticsControls("))
    }

    @Test fun navigationSupportsSwipeAndSlidingGlassCapsule() {
        val toolbar = source("ui/component/FloatingNavigationToolbar.kt")
        val motion = source("core/designsystem/motion/GalaxMotionEngine.kt")
        assertTrue(toolbar.contains("detectHorizontalDragGestures("))
        assertTrue(toolbar.contains("val tabWidth = maxWidth / items.size"))
        assertTrue(toolbar.contains("pagerState.currentPage + pagerState.currentPageOffsetFraction"))
        assertTrue(toolbar.contains("pagerState.dispatchRawDelta("))
        assertTrue(toolbar.contains("pagerState.animateScrollToPage(dragPosition.roundToInt())"))
        assertFalse(toolbar.contains("val itemBounds = remember(items)"))
        assertFalse(toolbar.contains("liquidTabDragFeedback"))
        val pages = source("ui/screens/GalaxMainPages.kt")
        assertTrue(pages.contains("HorizontalPager(state = pager"))
        assertTrue(source("ui/screens/Screens.kt").contains("listOf(Home, Search, Library)"))
        assertTrue(toolbar.contains("haptic(GalaxHapticCue.SELECTION)"))
        assertTrue(motion.contains("if (policy.reducedMotion) return EnterTransition.None"))
        assertTrue(motion.contains("if (policy.reducedMotion) return ExitTransition.None"))
    }

    @Test fun oldButtonAndChipEntryPointsNowReachNewRenderer() {
        val compat = source("ui/component/LiquidGlassBackdrop.kt")
        val controls = source("ui/component/LiquidGlassControls.kt")
        val newEngine = source("core/designsystem/liquid/GalaxLiquidRenderer.kt")
        assertTrue(compat.contains("galaxLiquidEffect(shapeOverride = shape"))
        assertFalse(compat.contains("openTuneGlassEffect("))
        assertTrue(controls.contains("Modifier.galaxLiquidChrome(shape"))
        assertTrue(controls.contains("Modifier.liquidGlassSurface("))
        assertTrue(newEngine.contains("com.kyant.backdrop.drawBackdrop"))
    }

    @Test fun stylingCoversCommonButtonsSettingsAndMenusWithoutNestedKyantCapture() {
        val chrome = source("core/designsystem/liquid/GalaxLiquidChrome.kt")
        val buttons = source("core/designsystem/component/ControlComponents.kt")
        val groups = source("ui/component/Material3SettingsGroup.kt")
        val menus = source("ui/component/NewMenuComponents.kt")
        val scaffold = source("MainActivityScaffold.kt")
        assertTrue(chrome.contains("galvanicFinish(variant)"))
        assertFalse(chrome.contains("rememberLayerBackdrop"))
        assertFalse(chrome.contains("drawBackdrop("))
        assertTrue(buttons.contains("Modifier.galaxLiquidChrome("))
        assertTrue(groups.contains("Modifier.galaxLiquidChrome("))
        assertTrue(menus.contains("Modifier.galaxLiquidChrome("))
        assertTrue(scaffold.contains("LocalGalaxLiquidBackdrop provides null"))
        assertTrue(scaffold.contains("Modifier.layerBackdrop(liveLiquidBackdrop)"))
    }
}
