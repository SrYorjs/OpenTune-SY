package com.arturo254.opentune.core.designsystem.navigation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.glass.GlassPreset
import com.arturo254.opentune.core.designsystem.glass.GlassSurface

/** Navigation dimensions preserve the existing 58dp floating toolbar. */
object OpenTuneNavigationDimensions {
    val floatingHeight: Dp = 58.dp
    val floatingRadius: Dp = 30.dp
    val floatingHorizontalPadding: Dp = 5.dp
    val floatingVerticalPadding: Dp = 4.dp
    val compactLabelThreshold: Dp = 320.dp
}

/** Presentation-only routing; callers own NavController and back-stack. */
data class OpenTuneDestination(
    val route: String,
    val label: String,
    @DrawableRes val icon: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenTuneAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

@Composable
fun OpenTuneNavigationBar(
    destinations: List<OpenTuneDestination>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = destination.route == selectedRoute,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
fun OpenTuneNavigationRail(
    destinations: List<OpenTuneDestination>,
    selectedRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(modifier = modifier) {
        destinations.forEach { destination ->
            NavigationRailItem(
                selected = destination.route == selectedRoute,
                onClick = { onNavigate(destination.route) },
                icon = {
                    Icon(
                        painter = painterResource(destination.icon),
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

/** Optional new consumer toolbar; existing route-aware toolbar remains supported. */
@Composable
fun OpenTuneFloatingToolbar(
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.height(OpenTuneNavigationDimensions.floatingHeight),
        shape = RoundedCornerShape(OpenTuneNavigationDimensions.floatingRadius),
        pureBlack = pureBlack,
        preset = GlassPreset.SOFT,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = OpenTuneNavigationDimensions.floatingHorizontalPadding,
                vertical = OpenTuneNavigationDimensions.floatingVerticalPadding,
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
