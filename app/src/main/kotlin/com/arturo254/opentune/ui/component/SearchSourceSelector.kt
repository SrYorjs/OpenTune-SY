package com.arturo254.opentune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.SearchSource

@Composable
fun SearchSourceSelector(
    selected: SearchSource,
    onSelected: (SearchSource) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outerShape = RoundedCornerShape(24.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .clip(outerShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f))
            .border(
                width = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f),
                shape = outerShape,
            )
            .padding(4.dp),
    ) {
        SearchSource.entries.forEach { source ->
            val isSelected = source == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.95f else 1f,
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 680f),
                label = "providerPress",
            )
            val container by animateColorAsState(
                targetValue =
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    else Color.Transparent,
                animationSpec = tween(140),
                label = "providerContainer",
            )
            val contentColor by animateColorAsState(
                targetValue =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(140),
                label = "providerContent",
            )

            val icon =
                when (source) {
                    SearchSource.UNIVERSAL -> R.drawable.search
                    SearchSource.LOCAL -> R.drawable.library_music
                    SearchSource.ONLINE -> R.drawable.language
                    SearchSource.DEEZER -> R.drawable.graphic_eq
                    SearchSource.SPOTIFY -> R.drawable.spotify_icon
                }
            val label =
                stringResource(
                    when (source) {
                        SearchSource.UNIVERSAL -> R.string.filter_all
                        SearchSource.LOCAL -> R.string.provider_local
                        SearchSource.ONLINE -> R.string.provider_youtube
                        SearchSource.DEEZER -> R.string.provider_deezer
                        SearchSource.SPOTIFY -> R.string.provider_spotify
                    }
                )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .background(container)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                    ) { onSelected(source) }
                    .padding(horizontal = 11.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint =
                        if (source == SearchSource.SPOTIFY) {
                            Color.Unspecified
                        } else {
                            contentColor
                        },
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}
