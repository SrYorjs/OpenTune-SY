package com.arturo254.opentune.core.designsystem.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem

/** Semantic UI feedback; delegates to the existing app-level haptic preference
 * via LocalHapticFeedback. Never performs a vibration outside a user action. */
enum class GalaxHapticCue { SELECTION, CONFIRM, LONG_PRESS }

@Composable
fun rememberGalaxHapticFeedback(): (GalaxHapticCue) -> Unit {
    val feedback = LocalHapticFeedback.current
    return remember(feedback) {
        { cue: GalaxHapticCue ->
            feedback.performHapticFeedback(
                when (cue) {
                    GalaxHapticCue.SELECTION -> HapticFeedbackType.TextHandleMove
                    GalaxHapticCue.CONFIRM, GalaxHapticCue.LONG_PRESS ->
                        HapticFeedbackType.LongPress
                },
            )
        }
    }
}

/** Reusable low-amplitude button press animation. The gesture and click event
 * still belong to the button; reduced motion only removes the transform. */
@Composable
fun Modifier.galaxPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val policy = OpenTuneDesignSystem.galaxMotion
    if (policy.reducedMotion || !enabled) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) policy.pressScale else 1f,
        animationSpec = policy.pressSpec,
        label = "galaxSharedPress",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
