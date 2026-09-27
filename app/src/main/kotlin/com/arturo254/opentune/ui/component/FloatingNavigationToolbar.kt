/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarScrollBehavior
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
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
    val barShape = RoundedCornerShape(32.dp)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val showSelectedLabels = maxWidth >= 360.dp

        Row(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .height(64.dp)
                .clip(barShape)
                .background(
                    if (liquidGlass) {
                        Color.Transparent
                    } else {
                        if (pureBlack) Color.Black.copy(alpha = 0.90f)
                        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.94f)
                    }
                )
                .liquidGlassSurface(
                    shape = barShape,
                    pureBlack = pureBlack,
                    blurRadius = 8,
                    refraction = 0.075f,
                    curve = 0.16f,
                    dispersion = 0.004f,
                    edge = 0.11f,
                )
                .padding(horizontal = 5.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ToolbarItemsContainer(
                items = items,
                pureBlack = pureBlack,
                showSelectedLabels = showSelectedLabels,
                selectedRoute = selectedRoute,
                onItemClick = onItemClick,
                modifier = Modifier.weight(1f),
            )

            if ((onShuffleClick != null && shuffleIconRes != null) ||
                (onFabClick != null && fabIconRes != null)
            ) {
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(1.dp)
                        .height(28.dp)
                        .background(Color.White.copy(alpha = if (pureBlack) 0.10f else 0.14f))
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
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { screen ->
            val selected = selectedRoute == screen.route
            FloatingNavigationToolbarItem(
                screen = screen,
                selected = selected,
                showSelectedLabel = showSelectedLabels,
                pureBlack = pureBlack,
                onClick = { onItemClick(screen, selected) },
                modifier = Modifier.weight(1f),
            )
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

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(
                        Color.White.copy(
                            alpha = if (pureBlack) 0.13f else 0.18f
                        )
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
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(24.dp),
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
    pureBlack: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(25.dp)
    val showLabel = selected && showSelectedLabel && screen.route != Screens.Search.route

    val selectedContainer by animateColorAsState(
        targetValue = if (selected) {
            Color.White.copy(alpha = if (pureBlack) 0.15f else 0.20f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "selectedContainer",
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (selected && showLabel) 13.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "itemPadding",
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
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
            if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface
        } else {
            if (pureBlack) Color.White.copy(alpha = 0.74f)
            else MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier = modifier
            .height(50.dp)
            .scale(pressScale)
            .clip(shape)
            .background(selectedContainer)
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
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
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
            enter = fadeIn(
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            ) + expandHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                expandFrom = Alignment.Start,
            ),
            exit = fadeOut(
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
            ) + shrinkHorizontally(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                shrinkTowards = Alignment.Start,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = stringResource(screen.titleId),
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
