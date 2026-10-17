package com.arturo254.opentune.core.designsystem.motion

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.DefaultOpenTuneMotionTokens

/** Presentation-only motion language. Media3, seek sessions and playback clocks
 * must never depend on these decorative animation preferences. */
@Immutable
data class GalaxMotionPolicy(
    val reducedMotion: Boolean = false,
    val navigationFadeMs: Int = 140,
    val surfaceFadeMs: Int = 200,
    val gentleSlideDivisor: Int = 7,
    val outgoingSlideDivisor: Int = 9,
    val swipeDistanceFraction: Float = 0.22f,
    val swipeMinVelocityPxPerSecond: Float = 850f,
) {
    val navigationBarSpec: AnimationSpec<Dp>
        get() = if (reducedMotion) snap() else DefaultOpenTuneMotionTokens.navigationBar

    val pressSpec: AnimationSpec<Float>
        get() = if (reducedMotion) snap() else DefaultOpenTuneMotionTokens.glassPress

    val pressScale: Float get() = if (reducedMotion) 1f else 0.97f
}

internal val LocalGalaxMotionPolicy = staticCompositionLocalOf { GalaxMotionPolicy() }

val OpenTuneDesignSystem.galaxMotion: GalaxMotionPolicy
    @Composable @ReadOnlyComposable get() = LocalGalaxMotionPolicy.current

/** Stateless, testable motion contract shared by all app navigation routes. */
fun galaxEnterTransition(
    policy: GalaxMotionPolicy,
    topLevel: Boolean,
    direction: Int,
    popping: Boolean = false,
): EnterTransition {
    if (policy.reducedMotion) return EnterTransition.None
    if (topLevel) {
        return slideInHorizontally(
            animationSpec = spring(dampingRatio = 0.90f, stiffness = 560f),
            initialOffsetX = { width -> direction.coerceIn(-1, 1) * width / policy.gentleSlideDivisor },
        ) + fadeIn(animationSpec = tween(policy.navigationFadeMs))
    }
    return fadeIn(animationSpec = tween(300)) + scaleIn(
        initialScale = if (popping) 0.98f else 0.95f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
    )
}

fun galaxExitTransition(
    policy: GalaxMotionPolicy,
    topLevel: Boolean,
    direction: Int,
    popping: Boolean = false,
): ExitTransition {
    if (policy.reducedMotion) return ExitTransition.None
    if (topLevel) {
        return slideOutHorizontally(
            animationSpec = tween(150),
            targetOffsetX = { width -> -direction.coerceIn(-1, 1) * width / policy.outgoingSlideDivisor },
        ) + fadeOut(animationSpec = tween(120))
    }
    return fadeOut(animationSpec = tween(policy.surfaceFadeMs)) + scaleOut(
        targetScale = if (popping) 0.95f else 0.98f,
        animationSpec = tween(policy.surfaceFadeMs),
    )
}

enum class GalaxSwipeAction { NONE, PREVIOUS, NEXT }

/**
 * A reusable decision helper: gesture recognizers can call this after a drag.
 * This function does NOT seek, skip, consume pointers or change any song.
 * Signed velocity or displacement and RTL use physical screen direction.
 */
fun galaxSwipeAction(
    displacementPx: Float,
    velocityPxPerSecond: Float,
    widthPx: Float,
    isRtl: Boolean,
    policy: GalaxMotionPolicy = GalaxMotionPolicy(),
): GalaxSwipeAction {
    if (!widthPx.isFinite() || widthPx <= 0f ||
        !displacementPx.isFinite() || !velocityPxPerSecond.isFinite()
    ) return GalaxSwipeAction.NONE
    val distanceEnough = kotlin.math.abs(displacementPx) >=
        widthPx * policy.swipeDistanceFraction
    val velocityEnough = kotlin.math.abs(velocityPxPerSecond) >=
        policy.swipeMinVelocityPxPerSecond
    if (!distanceEnough && !velocityEnough) return GalaxSwipeAction.NONE
    val signed = if (velocityEnough) velocityPxPerSecond else displacementPx
    val logical = if (isRtl) -signed else signed
    return if (logical > 0f) GalaxSwipeAction.PREVIOUS else GalaxSwipeAction.NEXT
}
