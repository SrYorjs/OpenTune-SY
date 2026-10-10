package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/**
 * Motion presets preserve the existing spring parameters and transition timing.
 * Compose respects the platform's animator duration scale automatically.
 * Use default values here instead of inventing per-component easing.
 */
data class OpenTuneMotionDurations(
    val miniInfoFadeInMs: Int = 110,
    val miniInfoSlideOutMs: Int = 140,
    val miniInfoFadeOutMs: Int = 100,
    val miniArtworkFadeInMs: Int = 90,
    val miniArtworkSlideOutMs: Int = 120,
    val miniArtworkFadeOutMs: Int = 90,
)

data class OpenTuneMotionTokens(
    val navigationBar: AnimationSpec<Dp>,
    val bottomSheet: AnimationSpec<Dp>,
    val bottomSheetSoft: AnimationSpec<Dp>,
    val miniPlayerSwipeSettle: AnimationSpec<Float>,
    val miniPlayerInfoSlide: FiniteAnimationSpec<IntOffset>,
    val miniPlayerArtworkSlide: FiniteAnimationSpec<IntOffset>,
    val glassPress: AnimationSpec<Float>,
    val durations: OpenTuneMotionDurations,
)

/** Immutable-by-convention specs shared with legacy public animation constants. */
val DefaultOpenTuneMotionTokens =
    OpenTuneMotionTokens(
        navigationBar = spring(dampingRatio = 0.86f, stiffness = 520f),
        bottomSheet = spring(dampingRatio = 0.88f, stiffness = 430f),
        bottomSheetSoft =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        miniPlayerSwipeSettle =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        miniPlayerInfoSlide = spring(dampingRatio = 0.86f, stiffness = 520f),
        miniPlayerArtworkSlide = spring(dampingRatio = 0.86f, stiffness = 560f),
        glassPress = spring(dampingRatio = 0.78f, stiffness = 620f),
        durations = OpenTuneMotionDurations(),
    )

internal val LocalOpenTuneMotionTokens =
    staticCompositionLocalOf<OpenTuneMotionTokens> {
        error("OpenTune motion tokens are not installed in this composition")
    }

val OpenTuneDesignSystem.motion: OpenTuneMotionTokens
    @Composable
    @ReadOnlyComposable
    get() = LocalOpenTuneMotionTokens.current
