package com.arturo254.opentune.ui.visual.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.arturo254.opentune.ui.visual.component.LocalOriginalGlassState
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.ui.visual.utils.stableSystemBars

@Immutable
class MiniPillFrame(
    val start: Dp,
    val end: Dp,
    
    val merged: Boolean,
    
    val withPrevious: Boolean,
)

val PhoneMiniPill = MiniPillFrame(start = 24.dp, end = 24.dp, merged = false, withPrevious = false)

val LocalMiniPillFrame = staticCompositionLocalOf { PhoneMiniPill }

private val WideTabWidth = 64.dp

private val WideMiniWidth = 340.dp
private val WideMiniMinWidth = 240.dp

private val WideBarMargin = 24.dp

val WideBarPillGap = 8.dp

@Immutable
class WideBar(
    val pillStart: Dp,
    val miniWidth: Dp,
    val tabsWidth: Dp,
    val screenWidth: Dp,
    
    val tabsAloneStart: Dp,
    
    val miniAloneStart: Dp,
) {
    val tabsStart: Dp get() = pillStart + miniWidth
    val pillWidth: Dp get() = miniWidth + tabsWidth

    val miniFrame = MiniPillFrame(
        start = pillStart,
        end = screenWidth - pillStart - miniWidth,
        merged = true,
        withPrevious = true,
    )

    val miniAloneFrame = MiniPillFrame(
        start = miniAloneStart,
        end = screenWidth - miniAloneStart - miniWidth,
        merged = false,
        withPrevious = true,
    )
}

@Composable
fun rememberWideBar(screenWidth: Dp, tabCount: Int): WideBar {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val sides = WindowInsets.stableSystemBars.union(WindowInsets.displayCutout)
    val safeStart = with(density) { sides.getLeft(density, direction).toDp() }
    val safeEnd = with(density) { sides.getRight(density, direction).toDp() }
    return remember(screenWidth, tabCount, safeStart, safeEnd) {
        val room = screenWidth - safeStart - safeEnd
        val tabsWidth = WideTabWidth * tabCount + NavRowPadding * 2
        val miniWidth = WideMiniWidth
            .coerceAtMost(room - WideBarMargin * 2 - tabsWidth)
            .coerceAtLeast(WideMiniMinWidth)
        WideBar(
            pillStart = safeStart + (room - miniWidth - tabsWidth) / 2,
            miniWidth = miniWidth,
            tabsWidth = tabsWidth,
            screenWidth = screenWidth,
            tabsAloneStart = safeStart + (room - tabsWidth) / 2,
            miniAloneStart = safeStart + (room - miniWidth) / 2,
        )
    }
}

@Composable
fun WideBarBackground(
    bar: WideBar,
    withPlayer: () -> Float,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val glassState = LocalOriginalGlassState.current ?: remember { GlassState() }
    val accentWash = rememberChromeAccentWash()
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .offset { IntOffset(lerp(bar.tabsAloneStart, bar.pillStart, withPlayer()).roundToPx(), WideBarPillGap.roundToPx()) }
                
                .layout { measurable, _ ->
                    val width = lerp(bar.tabsWidth, bar.pillWidth, withPlayer()).roundToPx()
                    val height = NavBarHeight.roundToPx()
                    val placeable = measurable.measure(Constraints.fixed(width, height))
                    layout(width, height) { placeable.place(0, 0) }
                }
                .dropShadow(NavBarShape, ChromeShadow)
                .clip(NavBarShape)
                .drawWithContent {
                    drawContent()
                    drawChromeOutline(Offset.Zero, size, cornerRadius = size.height / 2f)
                },
        ) {
            FrostedChromeGlass(
                state = glassState,
                pureBlack = pureBlack,
                accentWash = accentWash,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun WideBarTabs(
    bar: WideBar,
    withPlayer: () -> Float,
    navigationItems: List<Screens>,
    
    selectedTabRoute: String,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    
    onSearchDoubleTap: (() -> Unit)? = null,
) {
    val selectedIndex = navigationItems.indexOfFirst { it.route == selectedTabRoute }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .offset { IntOffset(lerp(bar.tabsAloneStart, bar.tabsStart, withPlayer()).roundToPx(), WideBarPillGap.roundToPx()) }
                .width(bar.tabsWidth)
                .height(NavBarHeight)
                .padding(horizontal = NavRowPadding)
                .navHighlight(selectedIndex, navigationItems.size),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            navigationItems.forEach { screen ->
                NavTab(
                    screen = screen,
                    isSelected = screen.route == selectedTabRoute,
                    onItemClick = onItemClick,
                    onDoubleTap = onSearchDoubleTap.takeIf { screen == Screens.Search },
                )
            }
        }
    }
}
