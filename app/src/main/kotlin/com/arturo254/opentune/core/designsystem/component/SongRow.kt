package com.arturo254.opentune.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import com.arturo254.opentune.core.designsystem.theme.sizes
import com.arturo254.opentune.core.designsystem.theme.typography

@Composable
fun SongRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    metadata: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = OpenTuneDesignSystem.galaxMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.pressSpec,
        label = "galaxSongRowPress",
    )
    val sizes = OpenTuneDesignSystem.sizes
    val typography = OpenTuneDesignSystem.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(GalaxVisualKit.tileShape)
            .background(
                if (selected) Brush.horizontalGradient(
                    listOf(
                        scheme.primary.copy(alpha = .17f),
                        scheme.tertiary.copy(alpha = .07f),
                        Color.Transparent,
                    ),
                ) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
            )
            .clickable(
                enabled = onClick != null,
                interactionSource = interaction,
                indication = LocalIndication.current,
            ) { onClick?.invoke() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (selected) scheme.primary else Color.Transparent),
        )
        Artwork(
            imageUrl = artworkUrl,
            modifier = Modifier.size(sizes.listThumbnailSize),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = typography.songTitle.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) scheme.primary else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = typography.songSubtitle,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!metadata.isNullOrBlank()) {
                Text(
                    text = metadata,
                    style = typography.metadata,
                    color = scheme.primary.copy(alpha = .74f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}
