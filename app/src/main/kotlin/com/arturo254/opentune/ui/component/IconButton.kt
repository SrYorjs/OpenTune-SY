/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.preferredFrameRate

@Composable
fun ResizableIconButton(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    indication: Indication? = null,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }

    Image(
        painter = painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
            .clickable(
                indication = indication ?: ripple(bounded = false),
                interactionSource = actualInteractionSource,
                enabled = enabled,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.5f),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "globalIconButtonPress",
    )
    val glass = LocalEnableLiquidGlass.current
    val glassShape = androidx.compose.foundation.shape.RoundedCornerShape(50)

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .preferredFrameRate(120f)
            .scale(pressScale)
            .clip(glassShape)
            .background(
                if (glass) Color.Black.copy(alpha = 0.025f)
                else colors.containerColor
            )
            .then(
                if (glass) {
                    Modifier.liquidGlassSurface(
                        shape = glassShape,
                        pureBlack = false,
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = false,
                    radius = 24.dp
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = colors.contentColor
        CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
    }
}
