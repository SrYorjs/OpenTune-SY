/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.foundation.border
import kotlin.math.roundToInt
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.arturo254.opentune.core.designsystem.glass.GlassMode
import com.arturo254.opentune.core.designsystem.glass.GlassOptics
import com.arturo254.opentune.core.designsystem.glass.GlassSurfaceSpec
import com.arturo254.opentune.core.designsystem.glass.GlassSurfaceType
import com.arturo254.opentune.core.designsystem.glass.galaxGlassEffect
import com.arturo254.opentune.core.designsystem.component.OpenTuneContextMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.navigation.OpenTuneNavigationDimensions
import com.arturo254.opentune.core.designsystem.motion.GalaxHapticCue
import com.arturo254.opentune.core.designsystem.motion.rememberGalaxHapticFeedback
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    onFabClick: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String = "",
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    scrollBehavior: FloatingToolbarScrollBehavior? = null,
    selectedRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    // scrollBehavior is kept in the public API because the host screen owns
    // the hide/show animation. The custom bar itself is intentionally static:
    // fewer animated layout passes means smoother scrolling.
    val liquidGlass = LocalEnableLiquidGlass.current
    val liquidRadius = com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidTuning.current?.cornerRadiusDp
    val barShape = RoundedCornerShape(
        if (liquidGlass && liquidRadius != null) liquidRadius.dp else OpenTuneNavigationDimensions.floatingRadius
    )
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    var previousSelectedIndex by remember { mutableIntStateOf(selectedIndex) }
    val slideFromEnd = selectedIndex >= previousSelectedIndex

    LaunchedEffect(selectedIndex) {
        previousSelectedIndex = selectedIndex
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val showSelectedLabels = maxWidth >= OpenTuneNavigationDimensions.compactLabelThreshold

        Row(
            modifier = Modifier
                .wrapContentWidth()
                .height(OpenTuneNavigationDimensions.floatingHeight)
                .preferredFrameRate(120f)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 520f,
                    )
                )
                .clip(barShape)
                .background(
                    if (liquidGlass) {
                        Color.Transparent
                    } else {
                        if (pureBlack) Color.Black.copy(alpha = 0.90f)
                        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                    }
                )
                .galaxGlassEffect(
                    shape = barShape,
                    pureBlack = pureBlack,
                    spec = GlassSurfaceSpec(
                        mode = GlassMode.CLASSIC,
                        surface = GlassSurfaceType.NAVIGATION,
                        optics = GlassOptics(
                            blurRadius = 4,
                            refraction = 0.045f,
                            curve = 0.10f,
                            dispersion = 0.0015f,
                            edge = 0f,
                        ),
                    ),
                )
                .padding(
                    horizontal = OpenTuneNavigationDimensions.floatingHorizontalPadding,
                    vertical = OpenTuneNavigationDimensions.floatingVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ToolbarItemsContainer(
                items = items,
                pureBlack = pureBlack,
                showSelectedLabels = showSelectedLabels,
                selectedRoute = selectedRoute,
                slideFromEnd = slideFromEnd,
                onItemClick = onItemClick,
                modifier = Modifier,
            )

            if ((onShuffleClick != null && shuffleIconRes != null) ||
                (onFabClick != null && fabIconRes != null)
            ) {
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(Color.White.copy(alpha = if (pureBlack) 0.07f else 0.09f))
                )

                FloatingToolbarAction(
                    pureBlack = pureBlack,
                    onFabClick = onFabClick,
                    fabIconRes = fabIconRes,
                    fabContentDescription = fabContentDescription,
                    onShuffleClick = onShuffleClick,
                    shuffleIconRes = shuffleIconRes,
                    shuffleContentDescription = shuffleContentDescription,
                    onMusicRecognitionClick = onMusicRecognitionClick,
                    musicRecognitionContentDescription = musicRecognitionContentDescription,
                )
            }
        }
    }
}

@Composable
private fun ToolbarItemsContainer(
    items: List<Screens>,
    pureBlack: Boolean,
    showSelectedLabels: Boolean,
    selectedRoute: String?,
    slideFromEnd: Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberGalaxHapticFeedback()
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }
    val itemBounds = remember(items) { mutableStateMapOf<String, Pair<Int, Int>>() }
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val indicatorBounds = selectedRoute?.let { itemBounds[it] }
    val targetX = indicatorBounds?.first?.toFloat() ?: 0f
    val targetWidth = indicatorBounds?.second?.toFloat() ?: 0f
    val indicatorX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = .78f, stiffness = 380f),
        label = "liquidTabIndicatorX",
    )
    val indicatorWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = if (reducedMotion) snap() else spring(dampingRatio = .83f, stiffness = 430f),
        label = "liquidTabIndicatorWidth",
    )
    val dragStretch by animateFloatAsState(
        targetValue = if (reducedMotion) 0f else horizontalDrag.coerceIn(-100f, 100f) * .07f,
        animationSpec = spring(dampingRatio = .74f, stiffness = 530f),
        label = "liquidTabDragFeedback",
    )
    Box(
        modifier = modifier
            .wrapContentWidth()
            .pointerInput(items, selectedIndex, onItemClick) {
                // Horizontal swipe belongs only to the navigation capsule,
                // never to page content, playlists or edge-back gestures.
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val current = selectedIndex
                        val destination = when {
                            horizontalDrag < -36.dp.toPx() -> current + 1
                            horizontalDrag > 36.dp.toPx() -> current - 1
                            else -> -1
                        }
                        horizontalDrag = 0f
                        if (current >= 0 && destination in items.indices) {
                            haptic(GalaxHapticCue.SELECTION)
                            onItemClick(items[destination], false)
                        }
                    },
                    onDragCancel = { horizontalDrag = 0f },
                    onHorizontalDrag = { change, amount ->
                        horizontalDrag = (horizontalDrag + amount).coerceIn(-150f, 150f)
                        change.consume()
                    },
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (indicatorBounds != null && indicatorWidth > 0f) {
            Box(
                Modifier
                    .offset { IntOffset((indicatorX + dragStretch).roundToInt(), 0) }
                    .width(with(density) { indicatorWidth.toDp() })
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = if (pureBlack) .20f else .15f,
                        ),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = .32f),
                        RoundedCornerShape(24.dp),
                    ),
            )
        }
        Row(
            modifier = Modifier.wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { screen ->
                val selected = selectedRoute == screen.route
                FloatingNavigationToolbarItem(
                    screen = screen,
                    selected = selected,
                    showSelectedLabel = showSelectedLabels,
                    slideFromEnd = slideFromEnd,
                    pureBlack = pureBlack,
                    onClick = {
                        haptic(GalaxHapticCue.SELECTION)
                        onItemClick(screen, selected)
                    },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        val newValue = coordinates.positionInParent().x.roundToInt() to coordinates.size.width
                        if (itemBounds[screen.route] != newValue) {
                            itemBounds[screen.route] = newValue
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarAction(
    pureBlack: Boolean,
    onFabClick: (() -> Unit)?,
    fabIconRes: Int?,
    fabContentDescription: String,
    onShuffleClick: (() -> Unit)?,
    shuffleIconRes: Int?,
    shuffleContentDescription: String,
    onMusicRecognitionClick: (() -> Unit)?,
    musicRecognitionContentDescription: String,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val isOverflow = onShuffleClick != null && shuffleIconRes != null
    val actionIcon = if (isOverflow) R.drawable.more_horiz else fabIconRes

    Box {
        if (actionIcon != null) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.90f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "toolbarActionScale",
            )

            val liquidGlass = LocalEnableLiquidGlass.current

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        if (liquidGlass) {
                            Color.Black.copy(alpha = if (pureBlack) 0.10f else 0.055f)
                        } else {
                            Color.White.copy(alpha = if (pureBlack) 0.11f else 0.14f)
                        }
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        role = Role.Button,
                    ) {
                        if (isOverflow) expanded = !expanded
                        else onFabClick?.invoke()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(actionIcon),
                    contentDescription = if (isOverflow) {
                        shuffleContentDescription.ifEmpty { stringResource(R.string.more) }
                    } else {
                        fabContentDescription
                    },
                    tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        if (isOverflow) {
            OpenTuneContextMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = OpenTuneDesignSystem.shapes.glassCard,
                containerColor = if (pureBlack) {
                    Color.Black.copy(alpha = 0.96f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
                },
                tonalElevation = 0.dp,
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.music_recognition)) },
                    onClick = {
                        expanded = false
                        onMusicRecognitionClick?.invoke()
                    },
                    leadingIcon = {
                        ActionMenuIcon(
                            pureBlack = pureBlack,
                            iconRes = R.drawable.mic,
                        )
                    },
                    enabled = onMusicRecognitionClick != null,
                    colors = actionMenuColors(pureBlack),
                )

                if (onShuffleClick != null && shuffleIconRes != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.shuffle)) },
                        onClick = {
                            expanded = false
                            onShuffleClick()
                        },
                        leadingIcon = {
                            ActionMenuIcon(
                                pureBlack = pureBlack,
                                iconRes = shuffleIconRes,
                            )
                        },
                        colors = actionMenuColors(pureBlack),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionMenuIcon(
    pureBlack: Boolean,
    iconRes: Int,
) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = if (pureBlack) 0.10f else 0.12f),
        contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun actionMenuColors(pureBlack: Boolean) =
    MenuDefaults.itemColors(
        textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.84f) else MaterialTheme.colorScheme.onSurfaceVariant,
        disabledTextColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        disabledLeadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    )

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    showSelectedLabel: Boolean,
    slideFromEnd: Boolean,
    pureBlack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val showLabel = selected && showSelectedLabel

    val selectedContainer by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(
                alpha = if (pureBlack) 0.16f else 0.13f
            )
        } else {
            Color.Transparent
        },
        animationSpec = spring(dampingRatio = .83f, stiffness = 480f),
        label = "selectedContainer",
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (showLabel) 12.dp else 10.dp,
        animationSpec = spring(dampingRatio = .84f, stiffness = 470f),
        label = "itemPadding",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.07f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "iconScale",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pressScale",
    )

    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            if (pureBlack) Color.White.copy(alpha = 0.70f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier = modifier
            .height(48.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = 560f,
                )
            )
            .scale(pressScale)
            .elasticLiquidPress(
                interactionSource = interactionSource,
                enabled = true,
            )
            .clip(shape)
            // A single spring-driven capsule slides behind the selected tab.
            // Keep per-tab tint understated to avoid duplicate opaque pills.
            .background(if (LocalEnableLiquidGlass.current) Color.Transparent else selectedContainer)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Crossfade(
            targetState = selected,
            animationSpec = tween(durationMillis = 150),
            label = "iconCrossfade",
            modifier = Modifier.scale(iconScale),
        ) { isSelected ->
            Icon(
                painter = painterResource(
                    if (isSelected) screen.iconIdActive else screen.iconIdInactive
                ),
                contentDescription = stringResource(screen.titleId),
                tint = contentColor,
                modifier = Modifier.size(22.dp),
            )
        }

        AnimatedVisibility(
            visible = showLabel,
            enter =
                fadeIn(tween(130)) +
                    slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = 620f,
                        ),
                        initialOffsetX = { width ->
                            if (slideFromEnd) width / 2 else -width / 2
                        },
                    ) +
                    expandHorizontally(
                        animationSpec = tween(175),
                        expandFrom = if (slideFromEnd) Alignment.End else Alignment.Start,
                    ),
            exit =
                fadeOut(tween(110)) +
                    slideOutHorizontally(
                        animationSpec = tween(90),
                        targetOffsetX = { width ->
                            if (slideFromEnd) -width / 3 else width / 3
                        },
                    ) +
                    shrinkHorizontally(
                        animationSpec = tween(90),
                        shrinkTowards = if (slideFromEnd) Alignment.Start else Alignment.End,
                    ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(screen.titleId),
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
