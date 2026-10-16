/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.EnableHapticFeedbackKey
import com.arturo254.opentune.constants.MiniPlayerHeight
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.motion
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.ui.player.actions.rememberPlayerActions
import com.arturo254.opentune.ui.player.controls.PlayerSkipDirection
import com.arturo254.opentune.ui.player.controls.playerPlaybackIconRes
import com.arturo254.opentune.ui.player.controls.playerFavoriteIconRes
import com.arturo254.opentune.ui.player.controls.playerSkipIconRes
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.together.TogetherSessionState
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import com.arturo254.opentune.utils.rememberHaptic
import com.arturo254.opentune.utils.rememberPreference
import kotlin.math.absoluteValue


@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    trackId: String?,
    playbackIndex: Int,
    queueSize: Int,
    isPlayerExpanded: Boolean,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    content: @Composable (Float, Int) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    var pendingSwipeDirection by remember { mutableIntStateOf(0) }
    var previousTrackId by remember(playerConnection) { mutableStateOf(trackId) }
    var previousPlaybackIndex by remember(playerConnection) { mutableIntStateOf(playbackIndex) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    val playerActions = rememberPlayerActions(playerConnection)
    val animationSpec = OpenTuneDesignSystem.motion.miniPlayerSwipeSettle

    fun settleOffset() {
        settleJob?.cancel()
        val startOffset = offsetX
        if (startOffset == 0f) return

        settleJob = coroutineScope.launch {
            val settleAnimation = Animatable(startOffset)
            settleAnimation.animateTo(
                targetValue = 0f,
                animationSpec = animationSpec
            ) {
                offsetX = value
            }
            offsetX = 0f
        }
    }

    val autoSwipeThreshold = MiniPlayerSwipePolicy.autoSwipeThreshold(swipeSensitivity)

    // Mini and full Player observe the same track snapshot. On any Media3
    // transition (including remote, notification, crossfade or radio) cancel a
    // stale swipe translation rather than carrying it into the next artwork.
    LaunchedEffect(trackId, playbackIndex, isPlayerExpanded, playerConnection) {
        if (trackId != previousTrackId) {
            transitionDirection = miniPlayerTrackTransitionDirection(
                previousIndex = previousPlaybackIndex,
                currentIndex = playbackIndex,
                itemCount = queueSize,
                requestedSwipeDirection = pendingSwipeDirection,
            )
            settleJob?.cancel()
            settleJob = null
            offsetX = 0f
            totalDragDistance = 0f
            pendingSwipeDirection = 0
            previousTrackId = trackId
        }
        previousPlaybackIndex = playbackIndex
        if (isPlayerExpanded) {
            settleJob?.cancel()
            settleJob = null
            offsetX = 0f
            totalDragDistance = 0f
            pendingSwipeDirection = 0
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier.background(
                        if (pureBlack) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    baseModifier.padding(horizontal = 12.dp)
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(swipeSensitivity, layoutDirection, playerConnection) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                settleJob?.cancel()
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                settleOffset()
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    MiniPlayerSwipePolicy.adjustedDrag(
                                        dragAmount,
                                        isRtl = layoutDirection == LayoutDirection.Rtl,
                                    )
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                if (MiniPlayerSwipePolicy.canConsumeDrag(
                                        adjustedDragAmount,
                                        canSkipPrevious,
                                        canSkipNext,
                                    )) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    // Update synchronously with the pointer event. The previous
                                    // implementation launched a coroutine for every drag frame.
                                    offsetX += adjustedDragAmount
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val currentOffset = offsetX
                                val shouldChangeSong = MiniPlayerSwipePolicy.shouldChangeSong(
                                    offsetX = currentOffset,
                                    totalDragDistance = totalDragDistance,
                                    dragDurationMs = dragDuration,
                                    swipeSensitivity = swipeSensitivity,
                                )

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    transitionDirection = if (isRightSwipe) -1 else 1
                                    pendingSwipeDirection = transitionDirection
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerActions.previous()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerActions.next()
                                    }
                                }

                                settleOffset()
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        content(offsetX, transitionDirection)

        // Visual indicator
        if (offsetX.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetX > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetX > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetX.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata,
    transitionDirection: Int,
) {
    val motion = OpenTuneDesignSystem.motion
    AnimatedContent(
        targetState = mediaMetadata,
        contentKey = { it.id },
        transitionSpec = {
            (
                slideInHorizontally(
                    animationSpec = motion.miniPlayerInfoSlide,
                    initialOffsetX = { width -> transitionDirection * width / 3 },
                ) + fadeIn(tween(motion.durations.miniInfoFadeInMs))
            ) togetherWith (
                slideOutHorizontally(
                    animationSpec = tween(motion.durations.miniInfoSlideOutMs),
                    targetOffsetX = { width -> -transitionDirection * width / 4 },
                ) + fadeOut(tween(motion.durations.miniInfoFadeOutMs))
            )
        },
        label = "miniInfoSlide",
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
    ) { metadata ->
        Column(
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = metadata.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                text = metadata.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee(),
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    progressFraction: Float,
    isLoading: Boolean,
    transitionDirection: Int,
    modifier: Modifier = Modifier
) {
    val motion = OpenTuneDesignSystem.motion
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(44.dp)
    ) {
        if (isLoading) {
            CircularWavyProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        } else {
            CircularWavyProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        }

        AnimatedContent(
            targetState = mediaMetadata,
            contentKey = { it?.id },
            transitionSpec = {
                (
                    slideInHorizontally(
                        animationSpec = motion.miniPlayerArtworkSlide,
                        initialOffsetX = { width -> transitionDirection * width / 2 },
                    ) + fadeIn(tween(motion.durations.miniArtworkFadeInMs))
                ) togetherWith (
                    slideOutHorizontally(
                        animationSpec = tween(motion.durations.miniArtworkSlideOutMs),
                        targetOffsetX = { width -> -transitionDirection * width / 2 },
                    ) + fadeOut(tween(motion.durations.miniArtworkFadeOutMs))
                )
            },
            label = "miniArtworkSlide",
        ) { metadata ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                val thumbnailUrl = metadata?.thumbnailUrl
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.opentune),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerTransportButton(
    iconResId: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val liquidGlass = LocalEnableLiquidGlass.current
    val containerColor = when {
        liquidGlass && isPrimary -> Color.White.copy(alpha = 0.18f)
        isPrimary -> MaterialTheme.colorScheme.surface
        else -> Color.Transparent
    }
    val borderColor = when {
        liquidGlass && enabled -> Color.White.copy(alpha = 0.22f)
        enabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    }
    val iconTint =
        if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(modifier)
            .size(if (isPrimary) 40.dp else 36.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(if (isPrimary) 22.dp else 18.dp)
        )
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun MiniPlayerTransportControls(
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playerConnection: PlayerConnection
) {
    val haptic = rememberHaptic()
    val playerActions = rememberPlayerActions(playerConnection)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniPlayerTransportButton(
            iconResId = playerSkipIconRes(PlayerSkipDirection.PREVIOUS),
            contentDescription = null,
            onClick = playerActions::previous,
            enabled = canSkipPrevious
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            MiniPlayerTransportButton(
                iconResId = playerPlaybackIconRes(playbackState, isPlaying),
                contentDescription = stringResource(
                    if (playbackState == Player.STATE_ENDED || !isPlaying) R.string.play else R.string.play
                ).let {
                    if (isPlaying && playbackState != Player.STATE_ENDED) "Pause" else it
                },
                onClick = {
                    haptic.click()
                    playerActions.playPause()
                },
                isPrimary = true
            )
        }

        MiniPlayerTransportButton(
            iconResId = playerSkipIconRes(PlayerSkipDirection.NEXT),
            contentDescription = null,
            onClick = playerActions::next,
            enabled = canSkipNext
        )
    }
}

@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    playerState: PlayerState,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    transitionDirection: Int,
) {
    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()

    // The enclosing full Player already maps all connection flows into this
    // immutable PlayerState. Do not subscribe again and risk a one-frame mismatch.
    val playerActions = rememberPlayerActions(playerConnection)
    val playbackState = playerState.playbackState
    val canSkipPrevious = playerState.canSkipPrevious
    val canSkipNext = playerState.canSkipNext
    val mediaMetadata = playerState.mediaMetadata
    val isPlaying = playerState.isPlaying
    val isLiked = playerState.isLiked
    // MiniPlayer has no seek preview: keep buffering-only loading semantics.
    val isLoading = playerState.isBuffering

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        MiniPlayerArtwork(
            mediaMetadata = mediaMetadata,
            progressFraction = playerState.progressFraction,
            isLoading = isLoading,
            transitionDirection = transitionDirection,
        )

        Spacer(modifier = Modifier.width(12.dp))

        mediaMetadata?.let {
            MiniPlayerInfo(
                mediaMetadata = it,
                transitionDirection = transitionDirection,
            )
        } ?: Spacer(Modifier.weight(1f))

        if (togetherSessionState !is TogetherSessionState.Idle) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.all_inclusive),
                        contentDescription = stringResource(R.string.music_together),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // ── Ícono de Persona (acceso al perfil del artista) ─────────
        IconButton(
            onClick = {
                val firstArtist = mediaMetadata?.artists?.firstOrNull()
                val artistId = firstArtist?.id
                if (!artistId.isNullOrBlank()) {
                    navController.navigate("artist/$artistId")
                    state.collapseSoft()
                }
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.person),
                contentDescription = stringResource(R.string.artists),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── Ícono de Corazón (guardar en favoritos) ────────────────
        IconButton(
            onClick = playerActions::toggleFavorite,
            modifier = Modifier.size(40.dp)
        ) {
            val heartScale by animateFloatAsState(
                targetValue = if (isLiked) 1.25f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "heartScale"
            )
            Icon(
                painter = painterResource(
                    playerFavoriteIconRes(isLiked)
                ),
                contentDescription = null,
                tint = if (isLiked)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = heartScale
                        scaleY = heartScale
                    }
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // ── Botón Play/Pause principal (Cookie6Sided) ─────────────────────
        MiniPlayerPlayPauseButton(
            isPlaying = isPlaying,
            isLoading = isLoading,
            playerConnection = playerConnection
        )

        Spacer(modifier = Modifier.width(4.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MiniPlayerPlayPauseButton(
    isPlaying: Boolean,
    isLoading: Boolean,
    playerConnection: PlayerConnection,
) {
    val (enableHaptic) = rememberPreference(EnableHapticFeedbackKey, true)
    val haptic = rememberHaptic(enabled = enableHaptic)
    val playerActions = rememberPlayerActions(playerConnection)

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "miniPlayPressScale",
    )

    val liquidGlass = LocalEnableLiquidGlass.current
    val containerColor = if (liquidGlass) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.primary
    }
    val iconColor = if (liquidGlass) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .scale(buttonScale)
            .clip(CircleShape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = if (liquidGlass) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        Color.Transparent
    },
                shape = CircleShape,
            )
            .clickable(
                enabled = !isLoading,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
            ) {
                haptic.click()
                playerActions.playPause()
            },
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(19.dp),
                color = if (liquidGlass) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
                strokeWidth = 2.dp,
            )
        } else {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(130)) +
                            scaleIn(
                                initialScale = 0.65f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                )
                            )).togetherWith(
                            fadeOut(animationSpec = tween(90)) +
                                    scaleOut(
                                        targetScale = 0.75f,
                                        animationSpec = tween(90),
                                    )
                        )
                },
                label = "playPauseIcon",
            ) { playing ->
                Icon(
                    painter = painterResource(
                        playerPlaybackIconRes(Player.STATE_READY, playing)
                    ),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}
