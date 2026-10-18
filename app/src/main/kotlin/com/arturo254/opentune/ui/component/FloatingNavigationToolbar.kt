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
import kotlinx.coroutines.launch
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import kotlin.math.roundToInt
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.sp
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
    pagerState: androidx.compose.foundation.pager.PagerState,
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
                .fillMaxWidth()
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
                modifier = Modifier.weight(1f),
                pagerState = pagerState,
            )


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
    pagerState: androidx.compose.foundation.pager.PagerState,
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val haptic = rememberGalaxHapticFeedback()
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val direction = if (androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl) -1 else 1
    BoxWithConstraints(modifier) {
        val tabWidth = maxWidth / items.size
        val widthPx = with(LocalDensity.current) { tabWidth.toPx() }
        val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction).coerceIn(0f, (items.size - 1).toFloat())
        var dragPosition by remember { mutableFloatStateOf(0f) }
        Box(Modifier.fillMaxWidth().height(56.dp).pointerInput(widthPx, direction) {
            detectHorizontalDragGestures(
                onDragStart = { dragPosition = pagerState.currentPage + pagerState.currentPageOffsetFraction },
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    dragPosition = (dragPosition + direction * delta / widthPx).coerceIn(0f, (items.size - 1).toFloat())
                    val actual = pagerState.currentPage + pagerState.currentPageOffsetFraction
                    pagerState.dispatchRawDelta((dragPosition - actual) * pagerState.layoutInfo.pageSize)
                },
                onDragEnd = {
                    scope.launch { pagerState.animateScrollToPage(dragPosition.roundToInt()) }
                },
                onDragCancel = {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage) }
                },
            )
        }) {
            // One permanent optical capsule. Position comes from the actual pages,
            // never from a route transition or a selected-only child animation.
            Box(Modifier.offset(x = tabWidth * position).width(tabWidth).height(56.dp)
                .galaxGlassEffect(shape = RoundedCornerShape(26.dp), pureBlack = pureBlack)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .36f), RoundedCornerShape(26.dp)))
            Row(Modifier.fillMaxWidth()) {
                items.forEachIndexed { index, screen ->
                    val selected = pagerState.currentPage == index
                    Column(
                        Modifier.weight(1f).height(56.dp)
                            .selectable(selected = selected, role = Role.Tab) {
                                haptic(GalaxHapticCue.SELECTION)
                                if (reducedMotion) scope.launch { pagerState.scrollToPage(index) }
                                else onItemClick(screen, selected)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive), null,
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp))
                        Text(stringResource(screen.titleId), style = MaterialTheme.typography.labelSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

