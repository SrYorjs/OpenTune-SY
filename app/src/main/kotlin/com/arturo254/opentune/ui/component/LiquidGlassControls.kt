package com.arturo254.opentune.ui.component

import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.OpenTuneShapeRadii
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.core.designsystem.theme.surfaces
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidTuning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
internal fun Modifier.elasticLiquidPress(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
): Modifier {

    val policy = OpenTuneDesignSystem.galaxMotion
    if (policy.reducedMotion) return this
    val pressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = policy.pressSpec,
        label = "liquidElasticPress",
    )
    var touchOffset by remember { mutableStateOf(Offset.Zero) }

    return this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                touchOffset = down.position - center

                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull()
                    if (change != null) {
                        touchOffset = change.position - center
                    }
                } while (event.changes.any { it.pressed })

                touchOffset = Offset.Zero
            }
        }
        .graphicsLayer {
            val width = size.width.coerceAtLeast(1f)
            val height = size.height.coerceAtLeast(1f)
            val maxOffset = size.minDimension.coerceAtLeast(1f)

            val baseScale = lerp(
                1f,
                1f + 4.dp.toPx() / height,
                progress,
            )

            val dx = touchOffset.x * progress
            val dy = touchOffset.y * progress

            translationX =
                maxOffset * tanh(0.05f * dx / maxOffset)
            translationY =
                maxOffset * tanh(0.05f * dy / maxOffset)

            val maxDragScale = 4.dp.toPx() / height
            val offsetAngle = atan2(dy, dx)

            scaleX =
                baseScale +
                    maxDragScale *
                        abs(cos(offsetAngle) * dx / size.maxDimension.coerceAtLeast(1f)) *
                        (width / height).fastCoerceAtMost(1f)

            scaleY =
                baseScale +
                    maxDragScale *
                        abs(sin(offsetAngle) * dy / size.maxDimension.coerceAtLeast(1f)) *
                        (height / width).fastCoerceAtMost(1f)
        }
}

@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    pureBlack: Boolean = false,
    accessibilityLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalEnableLiquidGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = OpenTuneDesignSystem.shapes.pill
    val surfaceTokens = OpenTuneDesignSystem.surfaces
    val baseColor =
        if (glass) {
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = surfaceTokens.opacity.selectedGlassIcon)
            } else {
                Color.Transparent
            }
        } else {
            if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .preferredFrameRate(120f)
            .elasticLiquidPress(
                interactionSource = interactionSource,
                enabled = enabled,
            )
            .clip(shape)
            .background(baseColor)
            .then(
                if (glass) {
                    Modifier.liquidGlassSurface(
                        shape = shape,
                        pureBlack = pureBlack,
                        blurRadius = 6,
                        refraction = 0.078f,
                        curve = 0.17f,
                        dispersion = 0.004f,
                        edge = 0.48f,
                    )
                } else {
                    Modifier
                }
            )
            .then(
                if (accessibilityLabel != null) {
                    Modifier.semantics { contentDescription = accessibilityLabel }
                } else Modifier
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = if (glass) null else LocalIndication.current,
                onClick = onClick,
            ),
        content = content,
    )
}

@Composable
fun LiquidGlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    pureBlack: Boolean = false,
    horizontalPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalEnableLiquidGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val shape = OpenTuneDesignSystem.shapes.pill
    val surfaceTokens = OpenTuneDesignSystem.surfaces
    val baseColor =
        when {
            glass && selected -> MaterialTheme.colorScheme.primary.copy(alpha = surfaceTokens.opacity.selectedGlassPill)
            glass -> Color.Transparent
            selected -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .preferredFrameRate(120f)
            .elasticLiquidPress(
                interactionSource = interactionSource,
                enabled = enabled,
            )
            .clip(shape)
            .background(baseColor)
            .then(
                if (glass) {
                    Modifier.liquidGlassSurface(
                        shape = shape,
                        pureBlack = pureBlack,
                        blurRadius = 6,
                        refraction = 0.078f,
                        curve = 0.17f,
                        dispersion = 0.004f,
                        edge = 0.48f,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = if (glass) null else LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding),
        content = content,
    )
}

@Composable
fun LiquidGlassControlCluster(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    horizontalSpacing: Dp = 8.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalEnableLiquidGlass.current
    val shape = OpenTuneDesignSystem.shapes.glassControlCluster
    val surfaceTokens = OpenTuneDesignSystem.surfaces

    Row(
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .preferredFrameRate(120f)
            .clip(shape)
            .background(
                if (glass) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = surfaceTokens.opacity.glassClusterOpaque)
                }
            )
            .then(
                if (glass) {
                    Modifier.liquidGlassSurface(
                        shape = shape,
                        pureBlack = pureBlack,
                        blurRadius = 7,
                        refraction = 0.072f,
                        curve = 0.16f,
                        dispersion = 0.0035f,
                        edge = 0.36f,
                    )
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    cornerRadius: Dp = OpenTuneShapeRadii.glassCard,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalEnableLiquidGlass.current
    val appliedRadius = if (glass) LocalGalaxLiquidTuning.current?.cornerRadiusDp?.dp else null
    val shape = if (appliedRadius != null) {
        RoundedCornerShape(appliedRadius)
    } else if (cornerRadius == OpenTuneShapeRadii.glassCard) {
        OpenTuneDesignSystem.shapes.glassCard
    } else {
        RoundedCornerShape(cornerRadius)
    }

    val surfaceTokens = OpenTuneDesignSystem.surfaces
    Box(
        modifier = modifier.then(
            if (glass) {
                Modifier.galaxLiquidChrome(shape, emphasis = 1.08f)
            } else {
                Modifier
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainer,
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                                    alpha = surfaceTokens.opacity.glassCardOpaqueGradientEnd,
                                ),
                            ),
                        ),
                    )
                    .border(
                        surfaceTokens.borders.glassCardWidth,
                        surfaceTokens.borders.glassCardOpaqueColor,
                        shape,
                    )
            },
        ),
        content = content,
    )
}

@Composable
fun Modifier.liquidGlassScreenBackground(
    pureBlack: Boolean = false,
): Modifier {
    return this.galaxScreenBackground(pureBlack)
}
