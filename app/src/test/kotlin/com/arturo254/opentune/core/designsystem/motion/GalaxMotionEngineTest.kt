package com.arturo254.opentune.core.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.SnapSpec
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalaxMotionEngineTest {
    @Test fun reducedMotionSkipsAllDecorativeRouteTransitions() {
        val reduced = GalaxMotionPolicy(reducedMotion = true)
        for (top in listOf(true, false)) {
            for (pop in listOf(true, false)) {
                assertEquals(EnterTransition.None, galaxEnterTransition(reduced, top, 1, pop))
                assertEquals(ExitTransition.None, galaxExitTransition(reduced, top, -1, pop))
            }
        }
        assertEquals(1f, reduced.pressScale)
        assertTrue(reduced.navigationBarSpec is SnapSpec<Dp>)
        assertTrue(reduced.pressSpec is SnapSpec<Float>)
    }

    @Test fun defaultMotionRetainsOriginalNavSpringAndPressScale() {
        val standard = GalaxMotionPolicy()
        assertFalse(standard.reducedMotion)
        assertEquals(0.97f, standard.pressScale)
        val bar = standard.navigationBarSpec as SpringSpec<Dp>
        assertEquals(0.86f, bar.dampingRatio, 0.0001f)
        assertEquals(520f, bar.stiffness, 0.0001f)
        val press = standard.pressSpec as SpringSpec<Float>
        assertEquals(0.78f, press.dampingRatio, 0.0001f)
        assertEquals(620f, press.stiffness, 0.0001f)
        assertTrue(galaxEnterTransition(standard, true, 1) != EnterTransition.None)
        assertTrue(galaxExitTransition(standard, false, 1) != ExitTransition.None)
    }

    @Test fun swipeDecisionHonorsDistanceVelocityAndRtlWithoutChangingPlayback() {
        val p = GalaxMotionPolicy()
        assertEquals(GalaxSwipeAction.NONE, galaxSwipeAction(10f, 100f, 300f, false, p))
        assertEquals(GalaxSwipeAction.PREVIOUS, galaxSwipeAction(80f, 0f, 300f, false, p))
        assertEquals(GalaxSwipeAction.NEXT, galaxSwipeAction(-80f, 0f, 300f, false, p))
        assertEquals(GalaxSwipeAction.NEXT, galaxSwipeAction(80f, 0f, 300f, true, p))
        assertEquals(GalaxSwipeAction.PREVIOUS, galaxSwipeAction(-80f, 0f, 300f, true, p))
        assertEquals(GalaxSwipeAction.NEXT, galaxSwipeAction(3f, -1200f, 300f, false, p))
        assertEquals(GalaxSwipeAction.NONE, galaxSwipeAction(Float.NaN, 999f, 300f, false, p))
        assertEquals(GalaxSwipeAction.NONE, galaxSwipeAction(100f, 999f, 0f, false, p))
    }

    @Test fun reducedMotionDoesNotDisableFunctionalSwipeDecisions() {
        val reduced = GalaxMotionPolicy(reducedMotion = true)
        assertEquals(GalaxSwipeAction.PREVIOUS, galaxSwipeAction(90f, 0f, 300f, false, reduced))
    }
}
