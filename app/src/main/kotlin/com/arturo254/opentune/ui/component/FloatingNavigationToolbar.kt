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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
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
    val glassEnabled = LocalEnableLiquidGlass.current
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }
    // Stable-width tabs keep indicator positions invariant while selection
    // changes. The old dynamic selected-label width made the pill jump/fade.
    val tabWidth = if (showSelectedLabels) 78.dp else 56.dp
    var horizontalDrag by remember { mutableFloatStateOf(0f) }
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val indicatorX by animateDpAsState(
        targetValue = tabWidth * selectedIndex.coerceAtLeast(0),
        animationSpec = if (reducedMotion) snap() else
            spring(dampingRatio = .82f, stiffness = 280f),
        label = "liquidTabIndicatorX",
    )
    val dragStretch by animateFloatAsState(
        targetValue = if (reducedMotion) 0f else horizontalDrag.coerceIn(-150f, 150f) * .42f,
        animationSpec = spring(dampingRatio = .74f, stiffness = 530f),
        label = "liquidTabDragFeedback",
    )
    // The gesture detector lives on individual tabs. Capturing the entire
    // capsule with a second pointerInput raced with clickable tab content.
    Box(
        modifier = modifier.wrapContentWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (glassEnabled && selectedIndex >= 0) {
            Box(
                Modifier
                    .offset { IntOffset((indicatorX.toPx() + dragStretch).roundToInt(), 0) }
                    .width(tabWidth)
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
                    onSwipeProgress = { drag -> horizontalDrag = drag },
                    onSwipe = { horizontalDelta ->
                        // Gestures begin on the clickable items themselves.
                        // A parent-only detector lost the race to clickable(),
                        // so dragging a tab previously did nothing.
                        val index = items.indexOfFirst { it.route == selectedRoute }
                        if (index >= 0) {
                            val next = if (horizontalDelta < 0f) index + 1 else index - 1
                            if (next in items.indices) {
                                haptic(GalaxHapticCue.SELECTION)
                                onItemClick(items[next], false)
                            }
                        }
                    },
                    modifier = Modifier.width(tabWidth),
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
    onSwipe: (Float) -> Unit,
    onSwipeProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    // No selected-only label expansion: constant geometry makes a real
    // uninterrupted iOS-style sliding capsule possible.
    val showLabel = showSelectedLabel

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
    val latestOnSwipe by rememberUpdatedState(onSwipe)
    val latestOnProgress by rememberUpdatedState(onSwipeProgress)
    var itemDrag by remember { mutableFloatStateOf(0f) }
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
            .scale(pressScale)
            // On the SAME target as clickable: horizontal swipes are recognized
            // before taps complete, while vertical scroll is untouched.
            // Stable across drag-driven recomposition: callbacks can change
            // without restarting the gesture recognizer mid-swipe.
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        itemDrag = (itemDrag + amount).coerceIn(-220f, 220f)
                        latestOnProgress(itemDrag)
                        change.consume()
                    },
                    onDragEnd = {
                        val drag = itemDrag
                        itemDrag = 0f
                        latestOnProgress(0f)
                        if (kotlin.math.abs(drag) > 24.dp.toPx()) latestOnSwipe(drag)
                    },
                    onDragCancel = {
                        itemDrag = 0f
                        latestOnProgress(0f)
                    },
                )
            }
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
            .padding(horizontal = 2.dp, vertical = 3.dp),
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
