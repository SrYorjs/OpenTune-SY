package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidVariant
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.GalaxGlyphFrame
import com.arturo254.opentune.core.designsystem.theme.galaxNebulaSurface
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit

// GalaxOT compatibility for operation 03; Mochy has no equivalent public helpers.
@Composable
fun Modifier.bottomSheetDraggable(
    state: BottomSheetState,
    onDismiss: (() -> Unit)? = null,
): Modifier {
    return this.pointerInput(state) {
        val velocityTracker = VelocityTracker()

        detectVerticalDragGestures(
            onVerticalDrag = { change, dragAmount ->
                velocityTracker.addPointerInputChange(change)
                state.dispatchRawDelta(dragAmount)
            },
            onDragCancel = {
                velocityTracker.resetTracking()
                state.snapTo(state.collapsedBound)
            },
            onDragEnd = {
                val velocity = -velocityTracker.calculateVelocity().y
                velocityTracker.resetTracking()
                state.performFling(velocity, onDismiss)
            },
        )
    }
}

@Composable
fun NewActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val activeGlass = LocalGalaxLiquidVariant.current != GalaxLiquidVariant.DISABLED
    val shape = GalaxVisualKit.tileShape
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem.galaxMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) motion.pressScale else 1f,
        animationSpec = motion.pressSpec,
        label = "galaxActionTilePress",
    )
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else .46f
            }
            .clip(shape)
            .then(
                if (activeGlass) Modifier.galaxLiquidChrome(shape, emphasis = .82f)
                else Modifier.galaxNebulaSurface(shape, accent = backgroundColor, intensity = .85f),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(19.dp),
        ) {
            GalaxGlyphFrame(
                size = 46.dp,
                accent = accent,
            ) {
                Box(Modifier.size(25.dp), contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Text(
                text = text,
                color = if (activeGlass) MaterialTheme.colorScheme.onSurface else contentColor,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 18.dp)
                .size(6.dp)
                .background(accent.copy(alpha = .75f), CircleShape),
        )
    }
}
