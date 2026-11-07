package com.arturo254.opentune.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidTuning
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.player.visual.playerVisualGlassEffect

/** Exactly one horizontal line; artwork, track, play and skip stay actionable. */
@Composable
internal fun GalaxCompactMiniPlayer(
    state: PlayerState,
    pureBlack: Boolean,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val liquid = LocalEnableLiquidGlass.current
    val radius = LocalGalaxLiquidTuning.current?.cornerRadiusDp ?: 32f
    val shape = RoundedCornerShape(if (liquid) radius.coerceAtLeast(28f).dp else 34.dp)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .height(62.dp)
            .clip(shape)
            .background(
                if (liquid) Color.Transparent
                else if (pureBlack) Color.Black.copy(alpha = .94f)
                else colors.surfaceContainerHigh.copy(alpha = .95f)
            )
            .playerVisualGlassEffect(shape = shape, pureBlack = pureBlack)
            .border(
                0.7.dp,
                if (liquid) colors.onSurface.copy(alpha = .15f) else colors.outlineVariant.copy(alpha = .38f),
                shape,
            ),
    ) {
        Row(
            Modifier.fillMaxSize().clickable(onClick = onOpen)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(43.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.primary.copy(alpha = .12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = .65f),
                    modifier = Modifier.size(20.dp),
                )
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = state.title.orEmpty(),
                color = colors.onSurface,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                modifier = Modifier.size(36.dp),
                onClick = onPlayPause,
            ) {
                Icon(
                    painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                    tint = colors.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            if (state.canSkipNext) {
                IconButton(modifier = Modifier.size(32.dp), onClick = onNext) {
                    Icon(
                        painterResource(R.drawable.skip_next),
                        contentDescription = "Siguiente canción",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomStart)
                .padding(horizontal = 17.dp)
                .fillMaxWidth()
                .height(1.5.dp)
                .background(colors.onSurface.copy(alpha = .09f)),
        )
        Box(
            Modifier.align(Alignment.BottomStart)
                .padding(start = 17.dp)
                .fillMaxWidth(state.progressFraction.coerceIn(0f, 1f))
                .height(1.5.dp)
                .background(colors.primary.copy(alpha = .78f)),
        )
    }
}
