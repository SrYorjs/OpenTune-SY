package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

/**
 * Four genuine interactive control geometries inspired by Kyant's kmp catalog.
 *
 * Button stretches on press; toggle and slider have MOVING glass thumbs;
 * tabs use an animated capsule highlight. The old generic Material Slider
 * and ON/OFF text row never modeled these behaviors.
 *
 * Deliberately sample only the parent-provided (independent) Backdrop.
 * These components never create an inner LayerBackdrop or capture themselves.
 */
@Composable
fun GalaxLiquidButton(
    text: String,
    onClick: () -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_BUTTON,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val expansion by animateFloatAsState(
        targetValue = if (pressed) 1.075f else 1f,
        animationSpec = spring(dampingRatio = .57f, stiffness = 360f),
        label = "liquidButtonPress",
    )
    Box(
        modifier
            .height(44.dp)
            .graphicsLayer { scaleX = expansion; scaleY = 1f + (expansion - 1f) * .55f }
            .galaxLiquidEffect(variant = variant, tuning = tuning, backdrop = backdrop)
            .clickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun GalaxLiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_TOGGLE,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    val position by animateDpAsState(
        targetValue = if (checked) 36.dp else 3.dp,
        animationSpec = spring(dampingRatio = .62f, stiffness = 300f),
        label = "liquidToggleThumb",
    )
    Box(
        modifier.width(78.dp).height(40.dp)
            .galaxLiquidEffect(variant = variant, tuning = tuning, backdrop = backdrop)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier.align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .width(70.dp).height(26.dp)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.primary.copy(alpha = .40f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = .13f),
                ),
        )
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset(x = position)
                .size(34.dp)
                .galaxLiquidEffect(
                    variant = GalaxLiquidVariant.KYANT_TOGGLE,
                    tuning = tuning.copy(blurRadiusDp = if (checked) 1f else 7f),
                    backdrop = backdrop,
                    shapeOverride = RoundedCornerShape(19.dp),
                )
                .clip(CircleShape)
                .background(Color.White.copy(alpha = .12f)),
        )
    }
}

@Composable
fun GalaxLiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_SLIDER,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    val fraction = value.coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier.height(44.dp)
            .pointerInput(onValueChange) {
                detectTapGestures { onValueChange((it.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(onValueChange) {
                // Horizontal drag adjusts the demo; a vertical drag continues
                // scrolling the entire Glass Studio instead of locking it.
                detectHorizontalDragGestures { change, _ ->
                    onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                    change.consume()
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = (maxWidth - 34.dp).coerceAtLeast(0.dp)
        val thumbX by animateDpAsState(
            targetValue = trackWidth * fraction,
            animationSpec = spring(dampingRatio = .72f, stiffness = 530f),
            label = "liquidSliderThumb",
        )
        Box(
            Modifier.fillMaxWidth().height(9.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .20f)),
        )
        Box(
            Modifier.fillMaxWidth(fraction).height(9.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .76f)),
        )
        Box(
            Modifier.offset(x = thumbX)
                .size(34.dp)
                .galaxLiquidEffect(
                    variant = GalaxLiquidVariant.KYANT_SLIDER,
                    tuning = tuning.copy(blurRadiusDp = 2f),
                    backdrop = backdrop,
                    shapeOverride = RoundedCornerShape(18.dp),
                ),
        )
    }
}

@Composable
fun GalaxLiquidBottomTabs(
    selected: Int,
    labels: List<String>,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_TABS,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    if (labels.isEmpty()) return
    BoxWithConstraints(
        modifier.height(50.dp)
            .galaxLiquidEffect(variant = variant, tuning = tuning, backdrop = backdrop)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val tabWidth = maxWidth / labels.size
        val position by animateDpAsState(
            targetValue = tabWidth * selected.coerceIn(0, labels.lastIndex),
            animationSpec = spring(dampingRatio = .72f, stiffness = 420f),
            label = "liquidTabsIndicator",
        )
        Box(
            Modifier.offset(x = position)
                .width(tabWidth)
                .height(41.dp)
                .galaxLiquidEffect(
                    variant = GalaxLiquidVariant.KYANT_TABS,
                    tuning = tuning.copy(blurRadiusDp = 2f),
                    backdrop = backdrop,
                    shapeOverride = RoundedCornerShape(26.dp),
                )
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = .13f)),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            labels.forEachIndexed { index, label ->
                Box(
                    Modifier.weight(1f).height(42.dp)
                        .clickable(role = Role.Tab) { onSelected(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
