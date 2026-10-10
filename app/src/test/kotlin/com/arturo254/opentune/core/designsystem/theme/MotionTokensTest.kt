package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.ui.unit.Dp
import com.arturo254.opentune.constants.BottomSheetAnimationSpec
import com.arturo254.opentune.constants.BottomSheetSoftAnimationSpec
import com.arturo254.opentune.constants.NavigationBarAnimationSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MotionTokensTest {
    @Test
    fun legacyNavigationAndSheetSpecsDelegateToTheSameMotionContract() {
        val motion = DefaultOpenTuneMotionTokens

        assertSame(motion.navigationBar, NavigationBarAnimationSpec)
        assertSame(motion.bottomSheet, BottomSheetAnimationSpec)
        assertSame(motion.bottomSheetSoft, BottomSheetSoftAnimationSpec)
        assertSpring(motion.navigationBar as SpringSpec<Dp>, 0.86f, 520f)
        assertSpring(motion.bottomSheet as SpringSpec<Dp>, 0.88f, 430f)
        assertSpring(
            motion.bottomSheetSoft as SpringSpec<Dp>,
            Spring.DampingRatioNoBouncy,
            Spring.StiffnessLow,
        )
    }

    @Test
    fun miniPlayerAndPressSpringsPreserveThePreviousBehavior() {
        val motion = DefaultOpenTuneMotionTokens

        assertSpring(
            motion.miniPlayerSwipeSettle as SpringSpec<Float>,
            Spring.DampingRatioNoBouncy,
            Spring.StiffnessMedium,
        )
        assertSpring(motion.miniPlayerInfoSlide as SpringSpec<Int>, 0.86f, 520f)
        assertSpring(motion.miniPlayerArtworkSlide as SpringSpec<Int>, 0.86f, 560f)
        assertSpring(motion.glassPress as SpringSpec<Float>, 0.78f, 620f)
    }

    @Test
    fun animatedMiniPlayerTextAndArtworkKeepTheirTiming() {
        val d = DefaultOpenTuneMotionTokens.durations

        assertEquals(110, d.miniInfoFadeInMs)
        assertEquals(140, d.miniInfoSlideOutMs)
        assertEquals(100, d.miniInfoFadeOutMs)
        assertEquals(90, d.miniArtworkFadeInMs)
        assertEquals(120, d.miniArtworkSlideOutMs)
        assertEquals(90, d.miniArtworkFadeOutMs)
    }

    private fun <T> assertSpring(spec: SpringSpec<T>, damping: Float, stiffness: Float) {
        assertEquals(damping, spec.dampingRatio, 0.0001f)
        assertEquals(stiffness, spec.stiffness, 0.0001f)
    }
}
