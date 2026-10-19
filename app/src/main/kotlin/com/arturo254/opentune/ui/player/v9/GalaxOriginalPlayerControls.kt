package com.arturo254.opentune.ui.player.v9

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes
import com.arturo254.opentune.ui.player.controls.playerPlaybackIconRes
import com.arturo254.opentune.ui.player.controls.playerRepeatIconRes
import com.arturo254.opentune.ui.player.progress.calculatePlayerProgress
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.utils.makeTimeString

@Composable
fun GalaxOriginalPlayerControls(
    style: PlayerDesignStyle,
    state: PlayerState,
    actions: PlayerActions,
    accent: Color,
    positionMs: Long,
    durationMs: Long,
    pendingSeekMs: Long?,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onMenu: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = Color.White
    val progress = calculatePlayerProgress(positionMs, durationMs, pendingSeekMs)
    val config = originalPlayerStyleConfig(style)
    val shape = RoundedCornerShape(config.cornerRadius.dp)
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 21.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.mediaMetadata?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-.4).sp,
                )
                Text(
                    text = state.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color.copy(alpha = .63f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GalaxPlayerControlIcon(
                icon = playerFavoriteIconRes(state.isLiked),
                description = if (state.isLiked) "Quitar de favoritos" else "Añadir a favoritos",
                onClick = actions::toggleFavorite,
                accent = accent,
                highlighted = state.isLiked,
                size = 40.dp,
                shape = shape,
            )
            GalaxPlayerControlIcon(
                icon = R.drawable.more_horiz,
                description = "Más opciones",
                onClick = onMenu,
                accent = accent,
                size = 40.dp,
                shape = shape,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Slider(
                value = progress.sliderValue,
                valueRange = progress.sliderRange,
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                enabled = progress.canSeek,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = Color.White.copy(alpha = .2f),
                ),
                modifier = Modifier.fillMaxWidth().height(38.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = makeTimeString(progress.displayPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = .7f),
                )
                Text(
                    text = if (progress.canSeek) makeTimeString(progress.durationMs) else "—:—",
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = .7f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            GalaxPlayerControlIcon(
                icon = R.drawable.shuffle,
                description = "Aleatorio",
                onClick = actions::toggleShuffle,
                accent = accent,
                highlighted = state.shuffleModeEnabled,
                size = 39.dp,
                shape = shape,
            )
            GalaxPlayerControlIcon(
                icon = R.drawable.skip_previous,
                description = "Anterior",
                onClick = actions::previous,
                accent = accent,
                enabled = state.canSkipPrevious,
                size = 48.dp,
                shape = shape,
            )
            Box(
                modifier = Modifier.size(69.dp)
                    .clip(if (config.roundPlay) CircleShape else shape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent, accent.copy(alpha = .76f)),
                        ),
                    )
                    .clickable(role = Role.Button, onClick = actions::playPause),
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black.copy(alpha = .88f),
                        strokeWidth = 2.5.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            playerPlaybackIconRes(state.playbackState, state.isPlaying),
                        ),
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                        tint = Color.Black.copy(alpha = .88f),
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            GalaxPlayerControlIcon(
                icon = R.drawable.skip_next,
                description = "Siguiente",
                onClick = actions::next,
                accent = accent,
                enabled = state.canSkipNext,
                size = 48.dp,
                shape = shape,
            )
            GalaxPlayerControlIcon(
                icon = playerRepeatIconRes(state.repeatMode),
                description = "Repetición",
                onClick = actions::toggleRepeat,
                accent = accent,
                highlighted = state.repeatMode != Player.REPEAT_MODE_OFF,
                size = 39.dp,
                shape = shape,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GalaxPlayerTextAction(
                icon = R.drawable.music_note,
                text = "LETRAS",
                onClick = onLyrics,
                tint = accent,
            )
            Text(
                text = config.subtitle,
                color = color.copy(alpha = .48f),
                fontSize = 9.sp,
                letterSpacing = .65.sp,
                maxLines = 1,
            )
            GalaxPlayerTextAction(
                icon = R.drawable.queue_music,
                text = "COLA",
                onClick = onQueue,
                tint = accent,
            )
        }
    }
}

@Composable
private fun GalaxPlayerControlIcon(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    enabled: Boolean = true,
    size: Dp = 42.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape,
) {
    Box(
        modifier = modifier.size(size)
            .clip(shape)
            .background(
                if (highlighted) accent.copy(alpha = .27f)
                else Color.White.copy(alpha = .075f),
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(if (size > 44.dp) 26.dp else 20.dp),
            tint = if (!enabled) Color.White.copy(alpha = .25f)
                else if (highlighted) accent else Color.White.copy(alpha = .93f),
        )
    }
}

@Composable
private fun GalaxPlayerTextAction(
    icon: Int,
    text: String,
    onClick: () -> Unit,
    tint: Color,
) {
    Row(
        modifier = Modifier.clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(text, color = Color.White.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall)
    }
}
