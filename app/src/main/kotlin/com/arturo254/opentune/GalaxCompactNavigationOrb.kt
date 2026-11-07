package com.arturo254.opentune

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.glass.galaxGlassEffect
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import kotlinx.coroutines.launch

/**
 * A single circular destination, not a squeezed duplicate of the full tab bar.
 * Tap to choose Inicio / Buscar / Biblioteca. The current tab animates in place.
 */
@Composable
internal fun GalaxCompactNavigationOrb(
    navigationState: MainActivityNavigationState,
    pureBlack: Boolean,
) {
    val items = navigationState.navigationItems
    val currentPage = navigationState.pagerState.currentPage.coerceIn(0, items.lastIndex)
    val current = items[currentPage]
    val colors = MaterialTheme.colorScheme
    val glass = LocalEnableLiquidGlass.current
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(62.dp)
                .clip(CircleShape)
                .background(
                    if (glass) Color.Transparent
                    else if (pureBlack) Color.Black.copy(alpha = .94f)
                    else colors.surfaceContainerHigh.copy(alpha = .94f)
                )
                .galaxGlassEffect(shape = CircleShape, pureBlack = pureBlack)
                .border(
                    1.dp,
                    colors.primary.copy(alpha = if (glass) .35f else .22f),
                    CircleShape,
                )
                .clickable { expanded = true },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(targetState = currentPage, label = "Galax destination orb") { page ->
                Icon(
                    painterResource(
                        if (page == currentPage) items[page].iconIdActive
                        else items[page].iconIdInactive
                    ),
                    contentDescription = stringResource(items[page].titleId),
                    modifier = Modifier.size(23.dp),
                    tint = colors.primary,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEachIndexed { index, screen ->
                DropdownMenuItem(
                    text = { Text(stringResource(screen.titleId)) },
                    leadingIcon = {
                        Icon(
                            painterResource(if (index == currentPage) screen.iconIdActive else screen.iconIdInactive),
                            contentDescription = null,
                            tint = if (index == currentPage) colors.primary else colors.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        expanded = false
                        navigationState.onActiveChange(false)
                        scope.launch {
                            if (reducedMotion) navigationState.pagerState.scrollToPage(index)
                            else navigationState.pagerState.animateScrollToPage(index)
                        }
                    },
                )
            }
        }
    }
}
