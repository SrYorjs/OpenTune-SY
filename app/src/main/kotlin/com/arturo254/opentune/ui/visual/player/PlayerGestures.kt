package com.arturo254.opentune.ui.visual.player

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

@Composable
fun Modifier.tapOrHold(
    enabled: Boolean,
    onTap: () -> Unit,
    onHoldStart: (() -> Unit)? = null,
    onHoldEnd: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
): Modifier {
    val tap by rememberUpdatedState(onTap)
    val holdStart by rememberUpdatedState(onHoldStart)
    val holdEnd by rememberUpdatedState(onHoldEnd)
    val haptic = LocalHapticFeedback.current
    val holdable = onHoldStart != null
    return this
        .semantics {
            role = Role.Button
            if (enabled) {
                onClick { tap(); true }
                if (holdable) onLongClick { true }
            } else {
                disabled()
            }
        }
        .pointerInput(enabled, holdable, interactionSource) {
            if (!enabled) return@pointerInput
            var holding = false
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource?.emit(press)
                    val released = tryAwaitRelease()
                    interactionSource?.emit(
                        if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press)
                    )
                    if (holding) {
                        holding = false
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        holdEnd?.invoke()
                    }
                },
                onLongPress = if (holdable) {
                    {
                        holding = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        holdStart?.invoke()
                    }
                } else {
                    null
                },
                onTap = { tap() },
            )
        }
}

class TransportHold(
    val onPreviousHoldStart: () -> Unit,
    val onNextHoldStart: () -> Unit,
    val onHoldEnd: () -> Unit,
)
