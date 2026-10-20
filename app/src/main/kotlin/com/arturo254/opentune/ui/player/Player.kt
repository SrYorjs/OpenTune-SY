@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.arturo254.opentune.ui.player

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.arturo254.opentune.ui.player.visual.PlayerArtworkAtmosphere
import com.arturo254.opentune.ui.component.LocalEnableLiquidGlass
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxCanvasLive
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Surface
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.ui.player.actions.rememberPlayerActions
import com.arturo254.opentune.ui.player.queue.calculateQueuePlaybackState
import com.arturo254.opentune.ui.player.progress.calculatePlayerProgress
import com.arturo254.opentune.ui.player.progress.rememberPlayerSeekSession
import com.arturo254.opentune.ui.player.host.PlayerHostBackHandler
import com.arturo254.opentune.ui.player.v7.V7PlayerScene
import com.arturo254.opentune.ui.player.v8.V8PlayerScene
import com.arturo254.opentune.ui.player.v8.V8PlayerControlsContent
import com.arturo254.opentune.ui.player.v5.V5PlayerContent
import com.arturo254.opentune.ui.player.v5.V5LandscapeLikeBox
import com.arturo254.opentune.ui.player.v5.v5OverlayGestures
import com.arturo254.opentune.ui.player.layout.GalaxPlayerClassicScene
import com.arturo254.opentune.ui.player.layout.GalaxPlayerLandscapeScene
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import com.arturo254.opentune.ui.player.GalaxStudioPlayer.GalaxOriginalPlayerControls
import com.arturo254.opentune.ui.player.GalaxStudioPlayer.StudioVisualScene
import com.arturo254.opentune.ui.player.layout.GalaxLegacyStyleScene
import com.arturo254.opentune.ui.player.GalaxStudioPlayer.AppleMusicPlayerScene
import com.arturo254.opentune.ui.player.v17.VinJaxPlayerScene
import com.arturo254.opentune.ui.player.layout.PlayerLayout
import com.arturo254.opentune.ui.player.layout.resolvePlayerLayout
import com.arturo254.opentune.ui.player.skin.rememberVisualSkin
import com.arturo254.opentune.ui.player.visual.rememberPlayerVisualState
import com.arturo254.opentune.ui.player.visual.V7PlayerBackdrop
import com.arturo254.opentune.ui.player.host.rememberPlayerContentLifecycle
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.constants.UseNewMiniPlayerDesignKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.PlayerCustomImageUriKey
import com.arturo254.opentune.constants.PlayerCustomBlurKey
import com.arturo254.opentune.constants.PlayerCustomContrastKey
import com.arturo254.opentune.constants.PlayerCustomBrightnessKey
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerButtonsStyleKey
import com.arturo254.opentune.constants.QueuePeekHeight
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.player.state.PlayerStateInput
import com.arturo254.opentune.ui.player.state.rememberPlayerState
import com.arturo254.opentune.ui.player.preferences.rememberPlayerPresentationPreferences
import com.arturo254.opentune.ui.component.BottomSheet
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.LocalBottomSheetPageState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.ui.player.panels.showPlayerPanelMenu
import com.arturo254.opentune.ui.player.panels.PlayerLyricsPanel
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.utils.ShowMediaInfo
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import androidx.datastore.preferences.core.booleanPreferencesKey
import coil3.compose.AsyncImage
import com.arturo254.opentune.constants.AodAutoActivationKey
import com.arturo254.opentune.constants.AodFullscreenKey
import com.arturo254.opentune.constants.BlurRadiusKey
import com.arturo254.opentune.constants.SeekExtraSeconds
import com.arturo254.opentune.ui.component.COLLAPSED_ANCHOR
import com.my.kizzy.gateway.entities.presence.Activity
import com.skydoves.cloudy.cloudy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    lyricsSyncOffset: Int,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val menuState = LocalMenuState.current

    val bottomSheetPageState = LocalBottomSheetPageState.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val playerActions = rememberPlayerActions(playerConnection)

    val presentation = rememberPlayerPresentationPreferences()
    val playerDesignStyle = presentation.designStyle
    val playerBackground = presentation.backgroundStyle
    val playerButtonsStyle = presentation.buttonStyle
    val sliderStyle = presentation.sliderStyle

    val (playerCustomImageUri) = rememberPreference(PlayerCustomImageUriKey, "")
    val (playerCustomBlur) = rememberPreference(PlayerCustomBlurKey, 0f)
    val (playerCustomContrast) = rememberPreference(PlayerCustomContrastKey, 1f)
    val (playerCustomBrightness) = rememberPreference(PlayerCustomBrightnessKey, 1f)

    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val (blurRadius) = rememberPreference(BlurRadiusKey, 36f)
    val (showCodecOnPlayer) = rememberPreference(booleanPreferencesKey("show_codec_on_player"), false)
    val (incrementalSeekSkipEnabled) = rememberPreference(SeekExtraSeconds, defaultValue = false)
    var keyboardSkipMultiplier by remember { mutableStateOf(1) }
    var lastKeyboardTapTime by remember { mutableLongStateOf(0L) }
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val downloadUtil = LocalDownloadUtil.current
    val download by downloadUtil
        .getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)
    val offlineCoverUri =
        remember(mediaMetadata?.id, download?.state) {
            mediaMetadata?.id?.let(downloadUtil::getOfflineCoverUri)
        }

    val (aodAutoTimeoutSeconds) = rememberPreference(AodAutoActivationKey, 0)
    val visualLifecycle = rememberPlayerContentLifecycle(
        isExpanded = state.isExpanded,
        aodAutoTimeoutSeconds = aodAutoTimeoutSeconds,
        onAodTimeout = {
            navController.navigate("always_on_display") {
                launchSingleTop = true
            }
        },
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val visualSkin = rememberVisualSkin(
        designStyle = playerDesignStyle,
        backgroundStyle = playerBackground,
        buttonStyle = playerButtonsStyle,
        useDarkTheme = useDarkTheme,
        pureBlack = pureBlack,
    )
    val onBackgroundColor = visualSkin.onBackgroundColor
    val useBlackBackground = visualSkin.useBlackBackground
    val backgroundColor = visualSkin.playerBackgroundColor(
        progress = ((state.value - state.collapsedBound) /
            (state.expandedBound - state.collapsedBound)).coerceIn(0f, 1f),
        aboveCollapsed = state.value > state.collapsedBound,
    )

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val playerVolume = playerConnection.service.playerVolume.collectAsState()

    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var position by rememberSaveable(mediaMetadata?.id) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(mediaMetadata?.id) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    val seekSession = rememberPlayerSeekSession(mediaMetadata?.id)
    val sliderPosition = seekSession.pendingSeekMs

    val playerState = rememberPlayerState(
        PlayerStateInput(
            mediaMetadata = mediaMetadata,
            playbackState = playbackState,
            isPlaying = isPlaying,
            positionMs = position,
            durationMs = duration,
            pendingSeekMs = sliderPosition,
            offlineArtworkUrl = offlineCoverUri,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            isLiked = currentSongLiked,
            shuffleModeEnabled = shuffleModeEnabled,
            repeatMode = repeatMode,
        )
    )

    val isLoading = playerState.isLoading

    val playerVisuals = rememberPlayerVisualState(
        metadata = mediaMetadata,
        downloadRevision = download?.state,
        offlineArtworkUrl = offlineCoverUri,
        offlineAnimationLookup = downloadUtil::getOfflineAnimationUri,
        usesArtworkPalette = visualSkin.usesArtworkPalette || LocalEnableLiquidGlass.current,
    )
    val canvasArtwork = playerVisuals.canvasArtwork
    val gradientColors = playerVisuals.gradientColors

    val changeBound = state.expandedBound / 3

    val TextBackgroundColor = visualSkin.textBackgroundColor
    val icBackgroundColor = visualSkin.iconBackgroundColor
    val textButtonColor = visualSkin.textButtonColor
    val iconButtonColor = visualSkin.iconButtonColor

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = { minutes ->
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer.start(minutes)
            },
            onEndOfSong = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer.start(-1)
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(mediaMetadata?.id, playbackState) {
        val startTime = SystemClock.elapsedRealtime()
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                val isTransitioning = playerConnection.player.currentMediaItem?.mediaId != mediaMetadata?.id
                val currentPlayerPosition = playerConnection.player.currentPosition
                val currentPlayerDuration = playerConnection.player.duration

                if (isTransitioning) {
                    val elapsedSinceStart = SystemClock.elapsedRealtime() - startTime
                    position = elapsedSinceStart
                    mediaMetadata?.let {
                        val metaDuration = it.duration.toLong() * 1000
                        duration = if (metaDuration > 0) metaDuration else 0L
                    }
                } else {
                    position = currentPlayerPosition
                    duration = currentPlayerDuration
                    seekSession.settle(currentPlayerPosition, currentPlayerDuration)
                }
            }
        } else {
            mediaMetadata?.let {
                val metaDuration = it.duration.toLong() * 1000
                duration = if (metaDuration > 0) metaDuration else 0L
            }
            val currentPlayerPosition = playerConnection.player.currentPosition
            if (seekSession.pendingSeekMs == null && currentPlayerPosition > 0L) {
                position = currentPlayerPosition
            }
        }
    }

    val dynamicQueuePeekHeight =
        if (playerDesignStyle == PlayerDesignStyle.V5) {
            0.dp
        } else if (showCodecOnPlayer) {
            88.dp
        } else {
            QueuePeekHeight
        }

    val dismissedBound = dynamicQueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = dismissedBound,
        initialAnchor = COLLAPSED_ANCHOR
    )

    val lyricsSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 0.dp,
        initialAnchor = 1
    )

    PlayerHostBackHandler(
        playerSheet = state,
        queueSheet = queueSheetState,
        lyricsSheet = lyricsSheetState,
    )

    val focusRequester = remember { FocusRequester() }

    val activePlayerGlass = LocalEnableLiquidGlass.current && !state.isCollapsed
    val liveCanvasGlass = activePlayerGlass &&
        playerDesignStyle == PlayerDesignStyle.V16 &&
        !canvasArtwork?.preferredAnimationUrl.isNullOrBlank()
    val inheritedBackdrop = LocalGalaxLiquidBackdrop.current
    val playerArtworkBackdrop = rememberLayerBackdrop()
    val playerSourceModifier = if (activePlayerGlass)
        Modifier.layerBackdrop(playerArtworkBackdrop) else Modifier

    BottomSheet(

        state = state,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    visualLifecycle.onInteraction()
                }

                if (keyEvent.type != KeyEventType.KeyDown || state.isCollapsed) return@onKeyEvent false

                when (keyEvent.key) {
                    Key.DirectionLeft -> {
                        val now = SystemClock.uptimeMillis()
                        if (incrementalSeekSkipEnabled && now - lastKeyboardTapTime < 1000) {
                            keyboardSkipMultiplier++
                        } else {
                            keyboardSkipMultiplier = 1
                        }
                        lastKeyboardTapTime = now
                        val skipAmount = 5000L * keyboardSkipMultiplier
                        playerActions.seekTo(
                            (playerConnection.player.currentPosition - skipAmount).coerceAtLeast(
                                0
                            )
                        )
                        true
                    }

                    Key.DirectionRight -> {
                        val now = SystemClock.uptimeMillis()
                        if (incrementalSeekSkipEnabled && now - lastKeyboardTapTime < 1000) {
                            keyboardSkipMultiplier++
                        } else {
                            keyboardSkipMultiplier = 1
                        }
                        lastKeyboardTapTime = now
                        val skipAmount = 5000L * keyboardSkipMultiplier
                        playerActions.seekTo(
                            (playerConnection.player.currentPosition + skipAmount).coerceAtMost(
                                playerConnection.player.duration
                            )
                        )
                        true
                    }

                    Key.DirectionUp -> {
                        playerConnection.service.playerVolume.value =
                            (playerConnection.service.playerVolume.value + 0.05f).coerceAtMost(1f)
                        true
                    }

                    Key.DirectionDown -> {
                        playerConnection.service.playerVolume.value =
                            (playerConnection.service.playerVolume.value - 0.05f).coerceAtLeast(0f)
                        true
                    }

                    Key.Spacebar -> {
                        playerActions.playPause()
                        true
                    }

                    Key.N -> {
                        if (keyEvent.isShiftPressed) {
                            playerActions.next()
                            true
                        } else false
                    }

                    Key.P -> {
                        if (keyEvent.isShiftPressed) {
                            playerActions.previous()
                            true
                        } else false
                    }

                    Key.L -> {
                        playerActions.toggleFavorite()
                        true
                    }

                    else -> false
                }
            },
        backgroundColor = visualSkin.sheetBackgroundColor(
            progress = ((state.value - state.collapsedBound) /
                (state.expandedBound - state.collapsedBound)).coerceIn(0f, 1f),
        ),
        onDismiss = {
            playerConnection.service.stopAndClearPlayback()
        },
        collapsedContent = {
            MiniPlayer(
                playerState = playerState,
                playbackIndex = currentWindowIndex,
                queueSize = queueWindows.size,
                pureBlack = pureBlack,
                navController = navController,
                state = state
            )
        },
    ) {
        CompositionLocalProvider(
            LocalGalaxLiquidBackdrop provides when {
                activePlayerGlass -> playerArtworkBackdrop
                else -> inheritedBackdrop
            },
            LocalGalaxCanvasLive provides liveCanvasGlass,
        ) {
        val onSliderValueChange: (Long) -> Unit = seekSession::onSeekPreview
        val onSliderValueChangeFinished: () -> Unit = {
            seekSession.finish { targetPosition ->

                playerActions.seekTo(targetPosition, displayedTrackId = mediaMetadata?.id)
                position = targetPosition
            }
        }
        val seekEnabled = playerState.canSeek
        val updatedOnSliderValueChange by rememberUpdatedState(onSliderValueChange)
        val updatedOnSliderValueChangeFinished by rememberUpdatedState(onSliderValueChangeFinished)

        val nextUpMetadata =
            remember(queueWindows, currentWindowIndex, shuffleModeEnabled, repeatMode) {
                val playbackOrder = calculateQueuePlaybackState(
                    itemCount = queueWindows.size,
                    currentIndex = currentWindowIndex,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                )

                playbackOrder.upNextIndices.firstOrNull()
                    ?.let { queueWindows.getOrNull(it)?.mediaItem?.metadata }
            }

        val enrichedMetadata = remember(mediaMetadata, currentSong) {
            val meta = mediaMetadata ?: return@remember null
            if (meta.album != null) return@remember meta
            val dbAlbum = currentSong?.album
            val dbAlbumId = currentSong?.song?.albumId
            when {
                dbAlbum != null -> meta.copy(
                    album = MediaMetadata.Album(id = dbAlbum.id, title = dbAlbum.title)
                )
                dbAlbumId != null -> meta.copy(
                    album = MediaMetadata.Album(
                        id = dbAlbumId,
                        title = currentSong?.song?.albumName.orEmpty()
                    )
                )
                else -> meta
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            PlayerControlsContent(
                mediaMetadata = mediaMetadata,
                playerDesignStyle = playerDesignStyle,
                sliderStyle = sliderStyle,
                playbackState = playbackState,
                isPlaying = isPlaying,
                isLoading = isLoading,
                repeatMode = repeatMode,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                textButtonColor = textButtonColor,
                iconButtonColor = iconButtonColor,
                textBackgroundColor = TextBackgroundColor,
                icBackgroundColor = icBackgroundColor,
                sliderPosition = sliderPosition,
                position = position,
                duration = duration,
                playerConnection = playerConnection,
                navController = navController,
                state = state,
                menuState = menuState,
                bottomSheetPageState = bottomSheetPageState,
                clipboardManager = clipboardManager,
                context = context,
                onSliderValueChange = onSliderValueChange,
                onSliderValueChangeFinished = onSliderValueChangeFinished,
                currentFormat = if (playerDesignStyle == PlayerDesignStyle.V7) currentFormat else null,
                onResetTimer = { visualLifecycle.onInteraction() },
            )
        }

        if (!state.isCollapsed && resolvePlayerLayout(playerDesignStyle, false).hasSharedBackdrop) {
            if (activePlayerGlass) {
                PlayerArtworkAtmosphere(
                    artworkUrl = playerVisuals.artworkUrl,
                    gradientColors = gradientColors,
                    isPlaying = isPlaying,
                    disableBlur = disableBlur,
                    modifier = playerSourceModifier,
                )
            } else {
                PlayerBackground(
                    playerBackground = playerBackground,
                    mediaMetadata = mediaMetadata,
                    gradientColors = gradientColors,
                    disableBlur = disableBlur,
                    blurRadius = blurRadius,
                    playerCustomImageUri = playerCustomImageUri,
                    playerCustomBlur = playerCustomBlur,
                    playerCustomContrast = playerCustomContrast,
                    playerCustomBrightness = playerCustomBrightness,
                )
            }
        }

        val v8ControlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { metadata ->
            V8PlayerControlsContent(

                                    mediaMetadata = metadata,
                                    playerActions = playerActions,
                                    liked = playerState.isLiked,
                                    shuffleModeEnabled = playerState.shuffleModeEnabled,
                                    playbackState = playbackState,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    repeatMode = repeatMode,
                                    canSkipPrevious = canSkipPrevious,
                                    canSkipNext = canSkipNext,
                                    textBackgroundColor = Color.White,
                                    sliderPosition = sliderPosition,
                                    position = position,
                                    duration = duration,
                                    navController = navController,
                                    state = state,
                                    menuState = menuState,
                                    bottomSheetPageState = bottomSheetPageState,
                                    onSliderValueChange = onSliderValueChange,
                                    onSliderValueChangeFinished = onSliderValueChangeFinished,
                                    currentFormat = currentFormat,
                                    nextUpMetadata = nextUpMetadata,
                                    onExpandQueue = { queueSheetState.expandSoft() },
                                    playerVolume = playerVolume.value,
                                    onVolumeChange = { newVolume ->
                                        playerConnection.service.playerVolume.value = newVolume
                                    }

            )
        }

        val galaxOriginalControls: @Composable () -> Unit = {
            GalaxOriginalPlayerControls(
                style = playerDesignStyle,
                state = playerState,
                actions = playerActions,
                accent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                positionMs = position,
                durationMs = duration,
                pendingSeekMs = sliderPosition,
                onSeek = updatedOnSliderValueChange,
                onSeekFinished = updatedOnSliderValueChangeFinished,
                onMenu = {
                    enrichedMetadata?.let { metadata ->
                        menuState.showPlayerPanelMenu(
                            metadata = metadata,
                            navController = navController,
                            playerBottomSheetState = state,
                            bottomSheetPageState = bottomSheetPageState,
                        )
                    }
                },
                onQueue = { queueSheetState.expandSoft() },
                onLyrics = { lyricsSheetState.expandSoft() },
            )
        }

        PlayerLayout(
            style = playerDesignStyle,
            applePortrait = {
                AppleMusicPlayerScene(
                    isLandscape = false,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    palette = gradientColors,
                    playback = playerState,
                    actions = playerActions,
                    positionMs = position,
                    durationMs = duration,
                    pendingSeekMs = sliderPosition,
                    onSeek = updatedOnSliderValueChange,
                    onSeekFinished = updatedOnSliderValueChangeFinished,
                    volume = playerVolume.value,
                    onVolumeChange = { playerConnection.service.playerVolume.value = it },
                    onLyrics = { lyricsSheetState.expandSoft() },
                    onQueue = { queueSheetState.expandSoft() },
                    onMenu = {
                        enrichedMetadata?.let { metadata ->
                            menuState.showPlayerPanelMenu(
                                metadata = metadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                bottomSheetPageState = bottomSheetPageState,
                            )
                        }
                    },
                    artworkUrl = playerVisuals.artworkUrl,
                    canvasArtwork = canvasArtwork,
                    disableBlur = disableBlur,
                    immersiveGlass = activePlayerGlass,
                    playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null,
                    nestedScrollConnection = state.preUpPostDownNestedScrollConnection,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize(),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                )
            },
            appleLandscape = {
                AppleMusicPlayerScene(
                    isLandscape = true,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    palette = gradientColors,
                    playback = playerState,
                    actions = playerActions,
                    positionMs = position,
                    durationMs = duration,
                    pendingSeekMs = sliderPosition,
                    onSeek = updatedOnSliderValueChange,
                    onSeekFinished = updatedOnSliderValueChangeFinished,
                    volume = playerVolume.value,
                    onVolumeChange = { playerConnection.service.playerVolume.value = it },
                    onLyrics = { lyricsSheetState.expandSoft() },
                    onQueue = { queueSheetState.expandSoft() },
                    onMenu = {
                        enrichedMetadata?.let { metadata ->
                            menuState.showPlayerPanelMenu(
                                metadata = metadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                bottomSheetPageState = bottomSheetPageState,
                            )
                        }
                    },
                    artworkUrl = playerVisuals.artworkUrl,
                    canvasArtwork = canvasArtwork,
                    disableBlur = disableBlur,
                    immersiveGlass = activePlayerGlass,
                    playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null,
                    nestedScrollConnection = state.preUpPostDownNestedScrollConnection,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize(),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                )
            },
            vinylPortrait = {
                VinJaxPlayerScene(
                    isLandscape = false,
                    isPlaying = isPlaying,
                    progress = playerState.progressFraction,
                    accent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize(),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                    controls = galaxOriginalControls,
                )
            },
            vinylLandscape = {
                VinJaxPlayerScene(
                    isLandscape = true,
                    isPlaying = isPlaying,
                    progress = playerState.progressFraction,
                    accent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize(),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                    controls = galaxOriginalControls,
                )
            },

            isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
            galaxPortrait = {
                StudioVisualScene(
                    style = playerDesignStyle,
                    isLandscape = false,
                    isPlaying = isPlaying,
                    colors = gradientColors,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    progress = playerState.progressFraction,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize().nestedScroll(
                                state.preUpPostDownNestedScrollConnection,
                            ),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                    controls = galaxOriginalControls,
                )
            },
            galaxLandscape = {
                StudioVisualScene(
                    style = playerDesignStyle,
                    isLandscape = true,
                    isPlaying = isPlaying,
                    colors = gradientColors,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    progress = playerState.progressFraction,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize(),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                    controls = galaxOriginalControls,
                )
            },
            compactLandscape = {
                    val littleBackground = MaterialTheme.colorScheme.primaryContainer
                    val littleTextColor = if (activePlayerGlass) Color.White
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    val progress = calculatePlayerProgress(position, duration, sliderPosition)
                    val progressFraction = progress.fraction
                    val progressOverlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(if (activePlayerGlass) Color.Black else littleBackground),
                    ) {
                        if (activePlayerGlass) {
                            PlayerArtworkAtmosphere(
                                artworkUrl = playerVisuals.artworkUrl,
                                gradientColors = gradientColors,
                                isPlaying = isPlaying,
                                disableBlur = disableBlur,
                                modifier = playerSourceModifier,
                            )
                        }
                        GalaxOrbitField(
                            modifier = Modifier.fillMaxSize(),
                            active = isPlaying,
                            intensity = .27f,
                            accent = MaterialTheme.colorScheme.primary,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progressFraction)
                                    .align(Alignment.TopStart)
                                    .background(progressOverlayColor),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .v5OverlayGestures(
                                        seekEnabled = seekEnabled,
                                        durationMs = duration,
                                        progressFraction = progressFraction,
                                        canSkipPrevious = canSkipPrevious,
                                        canSkipNext = canSkipNext,
                                        onSeekToPositionMs = updatedOnSliderValueChange,
                                        onSeekFinished = updatedOnSliderValueChangeFinished,
                                        onSkipPrevious = playerActions::previous,
                                        onSkipNext = playerActions::next,
                                    )
                                    .windowInsetsPadding(
                                        WindowInsets.systemBars.only(
                                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
                                        )
                                    ),
                        ) {
                            enrichedMetadata?.let { metadata ->
                                V5PlayerContent(
                                    mediaMetadata = metadata,
                                    sliderPosition = sliderPosition,
                                    positionMs = position,
                                    durationMs = duration,
                                    textColor = littleTextColor,
                                    liked = currentSongLiked,
                                    onCollapse = state::collapseSoft,
                                    onToggleLike = playerActions::toggleFavorite,
                                    onExpandQueue = queueSheetState::expandSoft,
                                    onMenuClick = {
                                        menuState.showPlayerPanelMenu(
                                            metadata = metadata,
                                            navController = navController,
                                            playerBottomSheetState = state,
                                            bottomSheetPageState = bottomSheetPageState,
                                        )
                                    },
                                )
                            }
                        }
                    }

            },
            cinematicLandscape = {
                    V7PlayerScene(
                        isLandscape = true,
                        thumbnailUrl = if (activePlayerGlass) playerVisuals.artworkUrl
                            else mediaMetadata?.thumbnailUrl,
                        isPlaying = isPlaying,
                        disableBlur = disableBlur,
                        queuePeekHeight = queueSheetState.collapsedBound,
                        playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null,
                        immersiveGlass = activePlayerGlass,
                        gradientColors = gradientColors,
                        nestedScrollConnection = state.preUpPostDownNestedScrollConnection,
                    ) {
                        enrichedMetadata?.let { controlsContent(it) }
                    }
            },

            immersiveLandscape = {
                    V8PlayerScene(
                        isLandscape = true,
                        artworkUrl = playerVisuals.artworkUrl,
                        canvasArtwork = canvasArtwork,
                        isPlaying = isPlaying,
                        disableBlur = disableBlur,
                        queuePeekHeight = queueSheetState.collapsedBound,
                        playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null,
                        immersiveGlass = activePlayerGlass,
                        gradientColors = gradientColors,
                        nestedScrollConnection = state.preUpPostDownNestedScrollConnection,
                    ) {
                        enrichedMetadata?.let { v8ControlsContent(it) }
                    }
            },
            classicLandscape = {
                GalaxLegacyStyleScene(
                    style = playerDesignStyle,
                    isLandscape = true,
                    isPlaying = isPlaying,
                    accent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.fillMaxSize(),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                    controls = { enrichedMetadata?.let { controlsContent(it) } },
                )
            },
            compactPortrait = {
                    val littleBackground = MaterialTheme.colorScheme.primaryContainer
                    val littleTextColor = if (activePlayerGlass) Color.White
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    val progress = calculatePlayerProgress(position, duration, sliderPosition)
                    val progressFraction = progress.fraction
                    val progressOverlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    val seekEnabled = progress.canSeek

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(if (activePlayerGlass) Color.Black else littleBackground),
                    ) {
                        if (activePlayerGlass) {
                            PlayerArtworkAtmosphere(
                                artworkUrl = playerVisuals.artworkUrl,
                                gradientColors = gradientColors,
                                isPlaying = isPlaying,
                                disableBlur = disableBlur,
                                modifier = playerSourceModifier,
                            )
                        }
                        GalaxOrbitField(
                            modifier = Modifier.fillMaxSize(),
                            active = isPlaying,
                            intensity = .27f,
                            accent = MaterialTheme.colorScheme.primary,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progressFraction)
                                    .align(Alignment.TopStart)
                                    .background(progressOverlayColor),
                        )
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .v5OverlayGestures(
                                        seekEnabled = seekEnabled,
                                        durationMs = duration,
                                        progressFraction = progressFraction,
                                        canSkipPrevious = canSkipPrevious,
                                        canSkipNext = canSkipNext,
                                        onSeekToPositionMs = updatedOnSliderValueChange,
                                        onSeekFinished = updatedOnSliderValueChangeFinished,
                                        onSkipPrevious = playerActions::previous,
                                        onSkipNext = playerActions::next,
                                    )
                                    .windowInsetsPadding(
                                        WindowInsets.systemBars.only(
                                            WindowInsetsSides.Horizontal + WindowInsetsSides.Top + WindowInsetsSides.Bottom
                                        )
                                    ),
                        ) {
                            enrichedMetadata?.let { metadata ->
                                V5LandscapeLikeBox(modifier = Modifier.fillMaxSize()) {
                                    V5PlayerContent(
                                        mediaMetadata = metadata,
                                        sliderPosition = sliderPosition,
                                        positionMs = position,
                                        durationMs = duration,
                                        textColor = littleTextColor,
                                        liked = currentSongLiked,
                                        onCollapse = state::collapseSoft,
                                        onToggleLike = playerActions::toggleFavorite,
                                        onExpandQueue = queueSheetState::expandSoft,
                                        onMenuClick = {
                                            menuState.showPlayerPanelMenu(
                                                metadata = metadata,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                bottomSheetPageState = bottomSheetPageState,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }

            },
            cinematicPortrait = {
                    V7PlayerScene(
                        isLandscape = false,
                        thumbnailUrl = if (activePlayerGlass) playerVisuals.artworkUrl
                            else mediaMetadata?.thumbnailUrl,
                        isPlaying = isPlaying,
                        disableBlur = disableBlur,
                        queuePeekHeight = queueSheetState.collapsedBound,
                        playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null,
                        immersiveGlass = activePlayerGlass,
                        gradientColors = gradientColors,
                        nestedScrollConnection = state.preUpPostDownNestedScrollConnection,
                    ) {
                        enrichedMetadata?.let { controlsContent(it) }
                    }
            },

            immersivePortrait = {
                    V8PlayerScene(
                        isLandscape = false,
                        artworkUrl = playerVisuals.artworkUrl,
                        canvasArtwork = canvasArtwork,
                        isPlaying = isPlaying,
                        disableBlur = disableBlur,
                        queuePeekHeight = queueSheetState.collapsedBound,
                        playerBackdrop = if (activePlayerGlass) playerArtworkBackdrop else null,
                        immersiveGlass = activePlayerGlass,
                        gradientColors = gradientColors,
                        nestedScrollConnection = state.preUpPostDownNestedScrollConnection,
                    ) {
                        enrichedMetadata?.let { v8ControlsContent(it) }
                    }
            },
            classicPortrait = {
                GalaxLegacyStyleScene(
                    style = playerDesignStyle,
                    isLandscape = false,
                    isPlaying = isPlaying,
                    accent = gradientColors.firstOrNull() ?: MaterialTheme.colorScheme.primary,
                    queuePeekHeight = queueSheetState.collapsedBound,
                    artwork = {
                        Thumbnail(
                            sliderPositionProvider = { seekSession.pendingSeekMs },
                            modifier = Modifier.nestedScroll(
                                state.preUpPostDownNestedScrollConnection,
                            ),
                            isPlayerExpanded = state.isExpanded,
                        )
                    },
                    controls = { enrichedMetadata?.let { controlsContent(it) } },
                )
            },
        )

        val queueOnBackgroundColor = visualSkin.queueOnBackgroundColor
        val queueSurfaceColor = visualSkin.queueSurfaceColor
        val queueTextButtonColor = visualSkin.queueTextButtonColor
        val queueIconButtonColor = visualSkin.queueIconButtonColor

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            onBackgroundColor = queueOnBackgroundColor,
            TextBackgroundColor = TextBackgroundColor,
            textButtonColor = textButtonColor,
            iconButtonColor = iconButtonColor,
            onShowLyrics = { lyricsSheetState.expandSoft() },
            pureBlack = pureBlack,
        )

        PlayerLyricsPanel(
            mediaMetadata = mediaMetadata,
            sheetState = lyricsSheetState,
            navController = navController,
            lyricsSyncOffset = lyricsSyncOffset,
            modifier = modifier,
        )
        }
    }
}
