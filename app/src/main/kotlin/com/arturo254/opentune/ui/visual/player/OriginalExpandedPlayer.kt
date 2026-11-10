package com.arturo254.opentune.ui.visual.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.visual.component.LocalMiniPillFrame
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.player.actions.PlayerActions
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.utils.resize

@Composable
fun OriginalExpandedPlayer(
    sheet: BottomSheetState,
    player: Player,
    state: PlayerState,
    track: MediaMetadata?,
    actions: PlayerActions,
    queueWindows: List<Timeline.Window>,
    currentIndex: Int,
    palette: List<Color>,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onMenu: () -> Unit,
    onSongInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val carousel = rememberOriginalSongCarousel(queueWindows, currentIndex, track)
    val handle = rememberPlayerHandleState()
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val open = sheet.progress.coerceIn(0f, 1f)
    val playingMagnification by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.05f else 1f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "originalCoverMagnification",
    )
    val background = palette.firstOrNull() ?: Color(0xFF202020)
    val edgeToEdge = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val next = queueWindows.getOrNull(currentIndex + 1)?.mediaItem?.metadata

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val coverSide = (maxWidth - 48.dp).coerceAtMost((maxHeight - 520.dp).coerceAtLeast(140.dp))
        val artRegion = maxHeight * 0.63f
        val artRegionPx = with(density) { artRegion.toPx() }
        val coverSidePx = with(density) { coverSide.toPx() }
        val miniFrame = LocalMiniPillFrame.current
        val placement = originalCoverPlacement(
            progress = open,
            fullWidth = coverSide,
            fullCenterX = maxWidth / 2f,
            fullCenterY = statusTop + 47.dp + coverSide / 2f,
            miniCenterX = miniFrame.start + 32.dp,
            miniCenterY = 32.dp,
            growLate = true,
        )
        val gapPx = with(density) { 20.dp.toPx() }
        CarouselCrossfade(
            carousel = carousel,
            fallbackUrl = state.artworkUrl,
            modifier = Modifier.fillMaxSize(),
        ) { url ->
            AsyncImage(
                model = url?.resize(260, 260),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(66.dp).graphicsLayer { alpha = 0.46f },
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        background.copy(alpha = 0.48f),
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.88f),
                        Color.Black,
                    )
                )
            ),
        )
        if (edgeToEdge) {
            CarouselPictures(
                carousel = carousel,
                fallbackUrl = state.artworkUrl,
                cardGap = 0.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(artRegion)
                    .graphicsLayer {
                        val widthPx = size.width
                        val growing = open
                        val miniSizePx = 48.dp.toPx()
                        scaleX = (miniSizePx + (widthPx - miniSizePx) * growing) / widthPx.coerceAtLeast(1f)
                        scaleY = (miniSizePx + (artRegionPx - miniSizePx) * growing) / artRegionPx.coerceAtLeast(1f)
                        translationX = ((miniFrame.start + 32.dp - maxWidth / 2f) * (1f - growing)).toPx()
                        translationY = ((32.dp - artRegion / 2f) * (1f - growing)).toPx()
                        val magnify = 1f + (playingMagnification - 1f) * growing
                        scaleX *= magnify
                        scaleY *= magnify
                        alpha = (open / 0.5f).coerceIn(0f, 1f)
                    }
                    .originalCoverGestures(
                        enabled = open > 0.95f,
                        carousel = carousel,
                        spacingPx = with(density) { maxWidth.toPx() },
                        canPrevious = state.canSkipPrevious,
                        canNext = state.canSkipNext,
                        actions = actions,
                        onLongPress = onMenu,
                    ),
            ) { url, childModifier ->
                AsyncImage(
                    model = url?.resize(1080, 1080),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = childModifier.fillMaxSize(),
                )
            }
            CoverReflection(
                url = state.artworkUrl,
                coverHeight = { artRegionPx },
                magnify = { playingMagnification },
                alpha = { open * 0.92f },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = artRegion)
                    .fillMaxWidth()
                    .height(maxHeight - artRegion),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(artRegion)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.65f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.42f),
                        )
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusTop + 47.dp, bottom = navigationBottom + 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!edgeToEdge) {
            Box(
                modifier = Modifier
                    .size(coverSide)
                    .graphicsLayer {
                        val magnify = 1f + (playingMagnification - 1f) * open
                        scaleX = placement.scale * magnify
                        scaleY = placement.scale * magnify
                        translationX = placement.translationX.toPx()
                        translationY = placement.translationY.toPx()
                        alpha = (open / 0.52f).coerceIn(0f, 1f)
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .originalCoverGestures(
                        enabled = open > 0.95f,
                        carousel = carousel,
                        spacingPx = coverSidePx + gapPx,
                        canPrevious = state.canSkipPrevious,
                        canNext = state.canSkipNext,
                        actions = actions,
                        onLongPress = onMenu,
                    ),
            ) {
                CarouselPictures(
                    carousel = carousel,
                    fallbackUrl = state.artworkUrl,
                    cardShape = RoundedCornerShape(22.dp),
                    cardGap = 20.dp,
                    modifier = Modifier.fillMaxSize(),
                ) { url, childModifier ->
                    AsyncImage(
                        model = url?.resize(1080, 1080),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = childModifier.fillMaxSize(),
                    )
                }
            }
            CoverReflection(
                url = state.artworkUrl,
                coverHeight = { coverSidePx },
                magnify = { 1f },
                alpha = { open * 0.7f },
                modifier = Modifier.fillMaxWidth().height(32.dp),
            )
            }
            Spacer(Modifier.weight(1f))
            OriginalPlayerSongInfoRow(
                title = state.title.orEmpty(),
                artist = track?.artists?.joinToString { it.name }.orEmpty(),
                liked = state.isLiked,
                onLike = actions::toggleFavorite,
                onExpandMenu = onMenu,
                onSongInfoClick = onSongInfo,
                menuExpanded = false,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            OriginalPlayerProgressSlider(
                player = player,
                mediaId = state.trackId,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onSeek = { actions.seekTo(it, displayedTrackId = state.trackId) },
            )
            Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 43.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.IconButton(onClick = actions::toggleShuffle) {
                    Icon(
                        painter = painterResource(if (state.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = "Aleatorio",
                        tint = if (state.shuffleModeEnabled) Color.White else Color.White.copy(alpha = 0.55f),
                    )
                }
                androidx.compose.material3.IconButton(onClick = onLyrics) {
                    Icon(
                        painter = painterResource(R.drawable.lyrics),
                        contentDescription = "Letras",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
                androidx.compose.material3.IconButton(onClick = onQueue) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Cola",
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
                androidx.compose.material3.IconButton(onClick = actions::toggleRepeat) {
                    Icon(
                        painter = painterResource(
                            when (state.repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = "Repetición",
                        tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) Color.White.copy(alpha = 0.55f) else Color.White,
                    )
                }
            }
            OriginalPlayerVolumeBar(
                volume = volume,
                onVolumeChange = onVolumeChange,
            )
            Spacer(Modifier.height(7.dp))
            OriginalPlayerBottomCards(
                nextTitle = next?.title,
                nextArtist = next?.artists?.joinToString { it.name },
                nextArtwork = next?.thumbnailUrl,
                lyricPreview = null,
                onOpenLyrics = onLyrics,
                onOpenQueue = onQueue,
                onLongPress = onMenu,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        PlayerHandle(
            state = handle,
            sheet = sheet,
            statusBarTop = statusTop,
            enabled = { sheet.progress > 0.12f },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}
