package com.arturo254.opentune.ui.visual.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.utils.resize
import com.arturo254.opentune.ui.visual.turntable.OriginalTurntableArtwork

@Composable
fun OriginalWidePlayerContent(
    player: Player,
    state: PlayerState,
    track: MediaMetadata?,
    carousel: SongCarousel,
    actions: PlayerActions,
    nextTrack: MediaMetadata?,
    lyricPreview: String?,
    width: Dp,
    height: Dp,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMenu: () -> Unit,
    onStyles: () -> Unit,
    preview: Boolean,
    onSongInfo: () -> Unit,
    turntable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val artworkSize = (height - 92.dp).coerceAtLeast(120.dp).coerceAtMost((width * 0.39f).coerceAtLeast(120.dp))
    val artPx = with(density) { artworkSize.toPx() }
    val next = nextTrack

    Row(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(22.dp))
                .then(if (turntable) Modifier else Modifier.originalCoverGestures(
                    enabled = !preview,
                    carousel = carousel,
                    spacingPx = artPx + with(density) { 20.dp.toPx() },
                    canPrevious = state.canSkipPrevious,
                    canNext = state.canSkipNext,
                    actions = actions,
                    onLongPress = onStyles,
                )),
        ) {
            if (turntable) {
                OriginalTurntableArtwork(
                    mediaId = state.trackId.orEmpty(),
                    title = state.title.orEmpty(),
                    artworkUrl = state.artworkUrl,
                    positionMs = state.positionMs,
                    durationMs = state.durationMs,
                    isPlaying = state.isPlaying,
                    active = true,
                    actions = actions,
                    modifier = Modifier.fillMaxSize(),
                )
            } else CarouselPictures(
                carousel = carousel,
                fallbackUrl = state.artworkUrl,
                cardGap = 20.dp,
                cardShape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxSize(),
            ) { url, childModifier ->
                AsyncImage(
                    model = url?.resize(900, 900),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = childModifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            OriginalPlayerSongInfoRow(
                title = state.title.orEmpty(),
                artist = track?.artists?.joinToString { it.name }.orEmpty(),
                liked = state.isLiked,
                onLike = actions::toggleFavorite,
                onExpandMenu = onMenu,
                onSongInfoClick = onSongInfo,
            )
            if (turntable) Spacer(Modifier.height(52.dp))
            else OriginalPlayerProgressSlider(
                player = player,
                mediaId = state.trackId,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onSeek = { actions.seekTo(it, displayedTrackId = state.trackId) },
            )
            OriginalPlayerTransportRow(
                player = player,
                isPlaying = state.isPlaying,
                playbackState = state.playbackState,
                canSkipPrevious = state.canSkipPrevious,
                canSkipNext = state.canSkipNext,
                onPrevious = actions::previous,
                onPlayPause = actions::playPause,
                onNext = actions::next,
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = actions::toggleShuffle) {
                    Icon(
                        painter = painterResource(if (state.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = "Aleatorio",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onLyrics) {
                    Icon(painterResource(R.drawable.lyrics), "Letras", tint = Color.White)
                }
                IconButton(onClick = onQueue) {
                    Icon(painterResource(R.drawable.queue_music), "Cola", tint = Color.White)
                }
                IconButton(onClick = actions::toggleRepeat) {
                    Icon(
                        painter = painterResource(
                            if (state.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one_on
                            else if (state.repeatMode == Player.REPEAT_MODE_ALL) R.drawable.repeat_on
                            else R.drawable.repeat
                        ),
                        contentDescription = "Repetición",
                        tint = Color.White,
                    )
                }
            }
            OriginalPlayerVolumeBar(
                volume = volume,
                onVolumeChange = onVolumeChange,
            )
            OriginalPlayerBottomCards(
                nextTitle = next?.title,
                nextArtist = next?.artists?.joinToString { it.name },
                nextArtwork = next?.thumbnailUrl,
                lyricPreview = lyricPreview,
                onOpenLyrics = onLyrics,
                onOpenQueue = onQueue,
                onLongPress = onStyles,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
