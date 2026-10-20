package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes
import com.arturo254.opentune.ui.player.controls.playerPlaybackIconRes
import com.arturo254.opentune.ui.player.progress.calculatePlayerProgress
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.utils.makeTimeString
import kotlin.math.min


import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.ui.player.v8.V8PlayerScene
import com.kyant.backdrop.backdrops.LayerBackdrop

@Composable
fun AppleMusicPlayerScene(
    isLandscape: Boolean,
    queuePeekHeight: Dp,
    palette: List<Color>,
    playback: PlayerState,
    actions: PlayerActions,
    positionMs: Long,
    durationMs: Long,
    pendingSeekMs: Long?,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMenu: () -> Unit,
    artworkUrl: String?,
    canvasArtwork: CanvasArtwork?,
    disableBlur: Boolean,
    immersiveGlass: Boolean,
    playerBackdrop: LayerBackdrop?,
    nestedScrollConnection: NestedScrollConnection,
) {
    V8PlayerScene(
        isLandscape = isLandscape,
        artworkUrl = artworkUrl,
        canvasArtwork = canvasArtwork,
        isPlaying = playback.isPlaying,
        disableBlur = disableBlur,
        queuePeekHeight = queuePeekHeight,
        nestedScrollConnection = nestedScrollConnection,
        playerBackdrop = playerBackdrop,
        immersiveGlass = immersiveGlass,
        gradientColors = palette,
        bareControls = true,
    ) {
        AppleMusicControls(
            playback = playback,
            actions = actions,
            positionMs = positionMs,
            durationMs = durationMs,
            pendingSeekMs = pendingSeekMs,
            onSeek = onSeek,
            onSeekFinished = onSeekFinished,
            volume = volume,
            onVolumeChange = onVolumeChange,
            onLyrics = onLyrics,
            onQueue = onQueue,
            onMenu = onMenu,
            compact = isLandscape,
        )
    }
}

@Composable
private fun AppleMusicControls(
    playback: PlayerState,
    actions: PlayerActions,
    positionMs: Long,
    durationMs: Long,
    pendingSeekMs: Long?,
    onSeek: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMenu: () -> Unit,
    compact: Boolean,
) {
    val progress = calculatePlayerProgress(positionMs, durationMs, pendingSeekMs)
    val white = Color.White
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (compact) 8.dp else 29.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 13.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = playback.mediaMetadata?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = white,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = playback.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = white.copy(alpha = .72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AppleMusicIcon(
                icon = playerFavoriteIconRes(playback.isLiked, v8 = true),
                description = "Favorito",
                onClick = actions::toggleFavorite,
                highlighted = playback.isLiked,
            )
            AppleMusicIcon(R.drawable.more_horiz, "Más opciones", onMenu)
        }
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Slider(
                value = progress.sliderValue,
                valueRange = progress.sliderRange,
                enabled = progress.canSeek,
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                colors = SliderDefaults.colors(
                    thumbColor = white,
                    activeTrackColor = white.copy(alpha = .97f),
                    inactiveTrackColor = white.copy(alpha = .30f),
                ),
                modifier = Modifier.fillMaxWidth().height(33.dp),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    makeTimeString(progress.displayPositionMs),
                    color = white.copy(alpha = .67f),
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    if (progress.canSeek)
                        "-" + makeTimeString(progress.remainingMs)
                    else "—:—",
                    color = white.copy(alpha = .67f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppleMusicIcon(R.drawable.shuffle, "Aleatorio", actions::toggleShuffle, playback.shuffleModeEnabled)
            AppleMusicIcon(
                R.drawable.skip_previous, "Anterior", actions::previous,
                size = 49.dp, enabled = playback.canSkipPrevious,
            )
            Box(
                modifier = Modifier.size(if (compact) 59.dp else 70.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = actions::playPause),
                contentAlignment = Alignment.Center,
            ) {
                if (playback.isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = white)
                } else {
                    Icon(
                        painter = painterResource(playerPlaybackIconRes(playback.playbackState, playback.isPlaying)),
                        contentDescription = if (playback.isPlaying) "Pausar" else "Reproducir",
                        tint = white,
                        modifier = Modifier.size(if (compact) 35.dp else 43.dp),
                    )
                }
            }
            AppleMusicIcon(
                R.drawable.skip_next, "Siguiente", actions::next,
                size = 49.dp, enabled = playback.canSkipNext,
            )
            AppleMusicIcon(
                R.drawable.repeat, "Repetición", actions::toggleRepeat,
                highlighted = playback.repeatMode != Player.REPEAT_MODE_OFF,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                painterResource(R.drawable.volume_off),
                contentDescription = "Volumen bajo",
                tint = white.copy(alpha = .68f),
                modifier = Modifier.size(17.dp),
            )
            Slider(
                value = volume.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = white,
                    activeTrackColor = white.copy(alpha = .83f),
                    inactiveTrackColor = white.copy(alpha = .28f),
                ),
                modifier = Modifier.weight(1f).height(35.dp),
            )
            Icon(
                painterResource(R.drawable.volume_up),
                contentDescription = "Volumen alto",
                tint = white.copy(alpha = .68f),
                modifier = Modifier.size(19.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppleMusicIcon(R.drawable.lyrics, "Letras", onLyrics, size = 33.dp)
            AppleMusicIcon(R.drawable.queue_music, "Cola", onQueue, size = 33.dp)
            AppleMusicIcon(R.drawable.more_horiz, "Opciones", onMenu, size = 33.dp)
        }
    }
}

@Composable
private fun AppleMusicIcon(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    size: Dp = 36.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier.size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (!enabled) Color.White.copy(alpha = .34f)
                else if (highlighted) Color.White else Color.White.copy(alpha = .82f),
            modifier = Modifier.size(size * .65f),
        )
    }
}
