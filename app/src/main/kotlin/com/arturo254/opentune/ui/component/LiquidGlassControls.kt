package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small reusable design system for the app-wide Liquid Glass redesign.
 * These components intentionally reuse the same optical material as the
 * navigation bar and mini-player instead of creating nested blur stacks.
 */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    pureBlack: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalEnableLiquidGlass.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "liquidControlPress",
    )
    val shape = RoundedCornerShape(50)
    val baseColor =
        if (glass) {
            if (selected) Color.White.copy(alpha = 0.13f)
            else Color.Black.copy(alpha = if (pureBlack) 0.08f else 0.025f)
        } else {
            if (selected) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .preferredFrameRate(120f)
            .scale(scale)
            .clip(shape)
            .background(baseColor)
            .then(
                if (glass) {
                    Modifier.liquidGlassSurface(
                        shape = shape,
                        pureBlack = pureBlack,
                        blurRadius = 3,
                        refraction = 0.038f,
                        curve = 0.08f,
                        dispersion = 0.001f,
                        edge = 0f,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
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
    val shape = RoundedCornerShape(30.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .preferredFrameRate(120f)
            .clip(shape)
            .background(
                if (glass) {
                    Color.Black.copy(alpha = if (pureBlack) 0.075f else 0.025f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                }
            )
            .then(
                if (glass) {
                    Modifier.liquidGlassSurface(
                        shape = shape,
                        pureBlack = pureBlack,
                        blurRadius = 4,
                        refraction = 0.045f,
                        curve = 0.10f,
                        dispersion = 0.0015f,
                        edge = 0f,
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
    cornerRadius: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = LocalEnableLiquidGlass.current
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (glass) {
                    Color.Black.copy(alpha = if (pureBlack) 0.085f else 0.045f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.90f)
                }
            )
            .then(
                if (glass) {
                    // Cards can appear many times in a scrolling grid/list. Keep
                    // them translucent and outlined instead of launching a
                    // backdrop shader for every visible cell.
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.11f),
                        shape = shape,
                    )
                } else {
                    Modifier
                }
            ),
        content = content,
    )
}


/**
 * Lightweight shared background for the redesigned top-level screens.
 * A single gradient replaces several stacked radial draw passes, keeping the
 * interface visually rich without adding continuous blur work while scrolling.
 */
@Composable
fun Modifier.liquidGlassScreenBackground(
    pureBlack: Boolean = false,
): Modifier {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background

    return this.background(
        Brush.verticalGradient(
            colors = listOf(
                primary.copy(alpha = if (pureBlack) 0.07f else 0.11f),
                surface.copy(alpha = 0.98f),
                tertiary.copy(alpha = if (pureBlack) 0.035f else 0.055f),
                surface,
            )
        )
    )
}
