@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player.v8

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import me.saket.squiggles.SquigglySlider
import com.arturo254.opentune.R
import com.arturo254.opentune.canvas.models.CanvasArtwork
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.player.actions.rememberPlayerActions
import com.arturo254.opentune.ui.player.v2.V2PlayerTopActions
import com.arturo254.opentune.ui.player.v2.V2PlayerTransport
import com.arturo254.opentune.ui.player.v5.V5PlayerTopActions
import com.arturo254.opentune.ui.player.v5.V5PlayerTransport
import com.arturo254.opentune.ui.player.v7.V7PlayerTopActions
import com.arturo254.opentune.ui.player.v7.V7PlayerTransport
import com.arturo254.opentune.ui.player.panels.playerFormatBadgeLabel
import com.arturo254.opentune.ui.player.progress.calculatePlayerProgress
import com.arturo254.opentune.ui.player.controls.PlayerSharedPlaybackContent
import com.arturo254.opentune.ui.player.controls.PlayerSharedSkipIcon
import com.arturo254.opentune.ui.player.controls.PlayerSkipDirection
import com.arturo254.opentune.ui.player.controls.playerPlaybackIconRes
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes
import com.arturo254.opentune.ui.player.controls.playerRepeatIconRes
import com.arturo254.opentune.ui.player.controls.sharePlayerTrack
import com.arturo254.opentune.ui.component.BottomSheetPageState
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.component.LiquidGlassControlCluster
import com.arturo254.opentune.ui.component.LiquidGlassIconButton
import com.arturo254.opentune.ui.component.PlayerSliderTrack
import com.arturo254.opentune.ui.component.ResizableIconButton
import com.arturo254.opentune.ui.player.panels.showPlayerPanelMenu
import com.arturo254.opentune.ui.theme.PlayerBackgroundColorUtils
import com.arturo254.opentune.ui.component.PlayerSliderColors
import com.arturo254.opentune.ui.component.V8DeviceSelector
import com.arturo254.opentune.ui.utils.ShowMediaInfo
import com.arturo254.opentune.ui.utils.highRes
import com.arturo254.opentune.utils.makeTimeString
import com.skydoves.cloudy.cloudy
import timber.log.Timber
import com.arturo254.opentune.ui.player.CanvasArtworkPlayer
import com.arturo254.opentune.ui.player.PlayerTimeLabel

// ============================================================
// V8 - Apple Music Style Components
// ============================================================

@Composable
fun V8PlayerBackdrop(
    thumbnailUrl: String?,
    canvasArtwork: CanvasArtwork? = null,
    isPlaying: Boolean = false,
    disableBlur: Boolean,
    label: String,
    modifier: Modifier = Modifier,
) {
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.055f else 1.025f,
        animationSpec = spring(
            dampingRatio = 0.92f,
            stiffness = 120f,
        ),
        label = "premiumArtworkBreathing",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AnimatedContent(
            targetState = thumbnailUrl,
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
            },
            label = label,
        ) { artworkUrl ->
            if (artworkUrl != null) {
                Box(Modifier.fillMaxSize()) {
                    if (canvasArtwork != null && !canvasArtwork.preferredAnimationUrl.isNullOrBlank()) {
                        CanvasArtworkPlayer(
                            primaryUrl = canvasArtwork.animated,
                            fallbackUrl = canvasArtwork.videoUrl,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize(),
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                        )
                    } else {
                        AsyncImage(
                            model = artworkUrl.highRes(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = artworkScale
                                    scaleY = artworkScale
                                    alpha = 0.90f
                                },
                        )
                    }
                }
            }
        }

        // Apple Music-like lower fade: the artwork stays visible up top while
        // controls sit on a deep, slightly tinted cinematic floor.
        val accent = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.08f),
                            0.30f to Color.Transparent,
                            0.52f to Color.Black.copy(alpha = 0.12f),
                            0.70f to accent.copy(alpha = 0.055f),
                            0.82f to Color.Black.copy(alpha = 0.62f),
                            1f to Color.Black.copy(alpha = 0.94f),
                        )
                    )
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f),
                        )
                    )
                ),
        )
    }
}

@Composable
fun V8PlayerControlsContent(
    mediaMetadata: MediaMetadata,
    playerDesignStyle: PlayerDesignStyle,
    sliderStyle: SliderStyle,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    clipboardManager: ClipboardManager,
    context: Context,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    currentFormat: FormatEntity? = null,
    onResetTimer: () -> Unit = {},
    nextUpMetadata: MediaMetadata? = null,
    onExpandQueue: () -> Unit = {},
    onShowLyrics: () -> Unit = {},
    playerVolume: Float,
    onVolumeChange: (Float) -> Unit,
) {
    val playerActions = rememberPlayerActions(playerConnection)
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // === TOP ROW: Title + Like/Menu (mismo layout/medidas que V7) ===
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Título
                AnimatedContent(
                    targetState = mediaMetadata.title,
                    transitionSpec = {
                        (
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.88f,
                                    stiffness = 520f,
                                ),
                                initialOffsetY = { it / 3 },
                            ) + fadeIn(tween(120))
                        ) togetherWith (
                            slideOutVertically(
                                animationSpec = tween(140),
                                targetOffsetY = { -it / 4 },
                            ) + fadeOut(tween(100))
                        )
                    },
                    label = "v8_title"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textBackgroundColor,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Artista
                val artistsText = remember(mediaMetadata.artists) {
                    mediaMetadata.artists.joinToString(separator = ", ") { it.name }
                }
                AnimatedContent(
                    targetState = artistsText,
                    transitionSpec = {
                        (
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = 0.90f,
                                    stiffness = 560f,
                                ),
                                initialOffsetY = { it / 3 },
                            ) + fadeIn(tween(130))
                        ) togetherWith (
                            slideOutVertically(
                                animationSpec = tween(140),
                                targetOffsetY = { -it / 4 },
                            ) + fadeOut(tween(100))
                        )
                    },
                    label = "v8_artist"
                ) { artists ->
                    Text(
                        text = artists,
                        style = MaterialTheme.typography.titleMedium,
                        color = textBackgroundColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Top actions share the same transparent Liquid Glass language as the bars.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassIconButton(
                    onClick = { playerActions.toggleFavorite() },
                    size = 44.dp,
                    selected = currentSongLiked,
                ) {
                    Icon(
                        painter = painterResource(
                            playerFavoriteIconRes(currentSongLiked, v8 = true)
                        ),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (currentSongLiked) 1f else 0.78f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                LiquidGlassIconButton(
                    onClick = {
                        menuState.showPlayerPanelMenu(
                            metadata = mediaMetadata,
                            navController = navController,
                            playerBottomSheetState = state,
                            bottomSheetPageState = bottomSheetPageState,
                        )
                    },
                    size = 44.dp,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = 0.78f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val progress = calculatePlayerProgress(position, duration, sliderPosition)
        Slider(
            value = progress.sliderValue,
            valueRange = progress.sliderRange,
            onValueChange = { onSliderValueChange(it.toLong()) },
            onValueChangeFinished = onSliderValueChangeFinished,
            colors = PlayerSliderColors.thickSliderColors(textBackgroundColor),
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                    .coerceIn(0f, 1f)
                GlassTrack(
                    fraction = fraction,
                    trackHeight = 10.dp,
                    tint = textBackgroundColor,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding)
        )

        Spacer(Modifier.height(4.dp))

        // === TIME ROW: tiempo transcurrido / restante + formato (igual que V7) ===
        PlayerTimeLabel(
            sliderPosition = sliderPosition,
            position = position,
            duration = duration,
            textBackgroundColor = textBackgroundColor,
            showRemainingTime = true,
            centerContent = currentFormat?.let { format ->
                {
                    val label = playerFormatBadgeLabel(format.mimeType)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = textBackgroundColor.copy(alpha = 0.12f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.graphic_eq),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = textBackgroundColor.copy(alpha = 0.8f),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = textBackgroundColor.copy(alpha = 0.8f),
                            )
                        }
                    }
                }
            }
        )

        // === A CONTINUACIÓN: preview del siguiente track, rasgo clásico de Apple Music ===
        if (nextUpMetadata != null) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onExpandQueue() }
                    .padding(horizontal = PlayerHorizontalPadding, vertical = 2.dp)
            ) {
                AsyncImage(
                    model = nextUpMetadata.thumbnailUrl?.highRes(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = buildString {
                        append("A continuación: ")
                        append(nextUpMetadata.title)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = textBackgroundColor.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee()
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // === PLAYBACK CONTROLS: tamaños de V7 reducidos ~12% ===
        V8PlaybackControls(
            playbackState = playbackState,
            isPlaying = isPlaying,
            isLoading = isLoading,
            repeatMode = repeatMode,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            textBackgroundColor = textBackgroundColor,
            playerConnection = playerConnection,
            shuffleModeEnabled = shuffleModeEnabled
        )

        Spacer(Modifier.height(14.dp))

        // === VOLUME SLIDER (más corta en los lados que el slider de tiempo) ===
        V8VolumeSlider(
            volume = playerVolume,
            onVolumeChange = onVolumeChange,
            activeColor = textBackgroundColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding + 8.dp)
        )

    }
}

@Composable
fun V8VolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassControlCluster(
        modifier = modifier,
        horizontalSpacing = 10.dp,
    ) {
        Icon(
            painter = painterResource(R.drawable.volume_off),
            contentDescription = null,
            tint = activeColor.copy(alpha = 0.66f),
            modifier = Modifier.size(16.dp)
        )

        Slider(
            value = volume,
            valueRange = 0f..1f,
            onValueChange = onVolumeChange,
            colors = PlayerSliderColors.thickSliderColors(activeColor),
            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
            track = { sliderState ->
                val fraction = ((sliderState.value - sliderState.valueRange.start) /
                        (sliderState.valueRange.endInclusive - sliderState.valueRange.start))
                    .coerceIn(0f, 1f)
                GlassTrack(
                    fraction = fraction,
                    trackHeight = 8.dp,
                    tint = activeColor,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = null,
            tint = activeColor.copy(alpha = 0.72f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun V8PlaybackControls(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textBackgroundColor: Color,
    playerConnection: PlayerConnection,
    shuffleModeEnabled: Boolean,
) {
    val playerActions = rememberPlayerActions(playerConnection)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiquidGlassIconButton(
                onClick = playerActions::previous,
                enabled = canSkipPrevious,
                size = 58.dp,
            ) {
                PlayerSharedSkipIcon(
                    direction = PlayerSkipDirection.PREVIOUS,
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 0.96f else 0.34f),
                    modifier = Modifier.size(30.dp),
                )
            }

            LiquidGlassIconButton(
                onClick = {
                    playerActions.playPause()
                },
                enabled = !isLoading,
                selected = isPlaying,
                size = 72.dp,
            ) {
                if (isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(31.dp),
                        color = textBackgroundColor,
                    )
                } else {
                    AnimatedContent(
                        targetState = isPlaying,
                        transitionSpec = {
                            fadeIn(tween(110)) togetherWith fadeOut(tween(80))
                        },
                        label = "v8PlayPause",
                    ) { playing ->
                        Icon(
                            painter =
                                painterResource(
                                    playerPlaybackIconRes(playbackState, playing)
                                ),
                            contentDescription = null,
                            tint = textBackgroundColor,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
            }

            LiquidGlassIconButton(
                onClick = playerActions::next,
                enabled = canSkipNext,
                size = 58.dp,
            ) {
                PlayerSharedSkipIcon(
                    direction = PlayerSkipDirection.NEXT,
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipNext) 0.96f else 0.34f),
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiquidGlassIconButton(
                onClick = {
                    playerActions.toggleShuffle()
                },
                selected = shuffleModeEnabled,
                size = 42.dp,
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (shuffleModeEnabled) 1f else 0.72f),
                    modifier = Modifier.size(19.dp),
                )
            }

            LiquidGlassIconButton(
                onClick = { playerActions.toggleRepeat() },
                selected = repeatMode != Player.REPEAT_MODE_OFF,
                size = 42.dp,
            ) {
                Icon(
                    painter =
                        painterResource(
                            playerRepeatIconRes(repeatMode)
                        ),
                    contentDescription = null,
                    tint =
                        textBackgroundColor.copy(
                            alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.72f else 1f
                        ),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
fun QueueCollapsedContentV8(
    showCodecOnPlayer: Boolean,
    currentFormat: FormatEntity?,
    textBackgroundColor: Color,
    onShowLyrics: () -> Unit,
    onExpandQueue: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerHorizontalPadding),
    ) {
        LiquidGlassIconButton(
            onClick = onShowLyrics,
            size = 44.dp,
        ) {
            Icon(
                painter = painterResource(R.drawable.lyrics_apple),
                contentDescription = "Lyrics",
                tint = textBackgroundColor.copy(alpha = 0.90f),
                modifier = Modifier.size(20.dp),
            )
        }

        V8DeviceSelector(
            textBackgroundColor = textBackgroundColor,
            modifier = Modifier.size(44.dp),
        )

        LiquidGlassIconButton(
            onClick = onExpandQueue,
            size = 44.dp,
        ) {
            Icon(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = "Queue",
                tint = textBackgroundColor.copy(alpha = 0.90f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}


/**
 * Track con efecto "glass" (vidrio esmerilado translúcido), estilo Apple Music.
 * Capa base translúcida + blur sutil + borde tenue + relleno de progreso con highlight.
 */
@Composable
fun GlassTrack(
    fraction: Float,
    trackHeight: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .height(trackHeight)
            .clip(shape)
    ) {
        // Lightweight optical track. Avoid a live blur here because this layer
        // redraws continuously while playback/dragging updates the fraction.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(tint.copy(alpha = 0.14f))
                .border(
                    width = 0.75.dp,
                    color = tint.copy(alpha = 0.24f),
                    shape = shape,
                )
        )

        // Relleno de progreso (vidrio más opaco + highlight superior), sin blur
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tint.copy(alpha = 0.95f),
                            tint.copy(alpha = 0.75f),
                        )
                    )
                )
        )
    }
}