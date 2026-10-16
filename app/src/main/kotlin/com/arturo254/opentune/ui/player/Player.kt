@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



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

    val playerDesignStyle by rememberEnumPreference(
        key = PlayerDesignStyleKey,
        defaultValue = PlayerDesignStyle.V8
    )

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    // Custom background preferences (image + effects)
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

    // System bars / fullscreen are already owned by the single PlayerHost
    // inside MainActivity, where they can also be restored on collapse.
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

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    // VisualSkin is the single presentation palette; existing preferences and
    // MaterialTheme still own dark mode, dynamic colors and pure black.
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

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.Standard)

    var position by rememberSaveable(mediaMetadata?.id) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(mediaMetadata?.id) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    val seekSession = rememberPlayerSeekSession(mediaMetadata?.id)
    val sliderPosition = seekSession.pendingSeekMs

    // One normalized presentation snapshot for full Player and MiniPlayer.
    // Reuses the existing position ticker; no second playback/timing engine.
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
    // Loading still means buffering or a pending seek, just as before 4.2.
    val isLoading = playerState.isLoading

    // Visual engine owns Canvas lookup and palette extraction per song.
    // It is presentation-only; MusicService and PlayerConnection are unchanged.
    val playerVisuals = rememberPlayerVisualState(
        metadata = mediaMetadata,
        downloadRevision = download?.state,
        offlineArtworkUrl = offlineCoverUri,
        offlineAnimationLookup = downloadUtil::getOfflineAnimationUri,
        usesArtworkPalette = visualSkin.usesArtworkPalette,
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

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },

            icon = {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(28.dp)
                    )
                }
            },

            title = {
                Text(
                    text = stringResource(R.string.sleep_timer),
                    style = MaterialTheme.typography.headlineSmallEmphasized
                )
            },

            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = sleepTimerValue.roundToInt().toString(),
                        style = MaterialTheme.typography.displayMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(20.dp))

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null
                        )

                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))

                        Text(stringResource(R.string.end_of_song))
                    }
                }
            },

            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        ButtonGroupDefaults.ConnectedSpaceBetween
                    )
                ) {

                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            showSleepTimerDialog = false
                        },
                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                    ) {
                        Text(stringResource(android.R.string.cancel))
                    }

                    ToggleButton(
                        checked = false,
                        onCheckedChange = {
                            showSleepTimerDialog = false

                            playerConnection.service.sleepTimer.start(
                                sleepTimerValue.roundToInt()
                            )
                        },
                        shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null
                        )

                        Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))

                        Text(stringResource(android.R.string.ok))
                    }
                }
            },

            confirmButton = {}
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
                position = position,
                duration = duration,
                pureBlack = pureBlack,
                navController = navController,
                state = state
            )
        },
    ) {
        val onSliderValueChange: (Long) -> Unit = seekSession::onSeekPreview
        val onSliderValueChangeFinished: () -> Unit = {
            seekSession.finish { targetPosition ->
                // Crossfade adjustment remains owned by PlayerActions.
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
                // One source of Up Next order; no repeated rows for repeat-all/one.
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
            PlayerBackground(
                playerBackground = playerBackground,
                mediaMetadata = mediaMetadata,
                gradientColors = gradientColors,
                disableBlur = disableBlur,
                blurRadius = blurRadius,
                playerCustomImageUri = playerCustomImageUri,
                playerCustomBlur = playerCustomBlur,
                playerCustomContrast = playerCustomContrast,
                playerCustomBrightness = playerCustomBrightness
            )
        }

// distance

        // Player 2.0: one layout dispatcher; all existing compositions remain
        // intact behind typed orientation/family slots (4.13–4.16 migrate them).
        PlayerLayout(
            style = playerDesignStyle,
            isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE,
            compactLandscape = {
                    val littleBackground = MaterialTheme.colorScheme.primaryContainer
                    val littleTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                    val progress = calculatePlayerProgress(position, duration, sliderPosition)
                    val progressFraction = progress.fraction
                    val progressOverlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(littleBackground),
                    ) {
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
                                    .littlePlayerOverlayGestures(
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
                                LittlePlayerContent(
                                    mediaMetadata = metadata,
                                    sliderPosition = sliderPosition,
                                    positionMs = position,
                                    durationMs = duration,
                                    textColor = littleTextColor,
                                    liked = currentSongLiked,
                                    onCollapse = state::collapseSoft,
                                    onToggleLike = playerConnection::toggleLike,
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        V7PlayerBackdrop(
                            thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            disableBlur = disableBlur,
                            label = "v7BackdropLandscape",
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = queueSheetState.collapsedBound)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                        ) {
                            enrichedMetadata?.let {
                                controlsContent(it)
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                
            },
            immersiveLandscape = {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        V8PlayerBackdrop(
                            thumbnailUrl = playerVisuals.artworkUrl,
                            canvasArtwork = canvasArtwork,
                            isPlaying = isPlaying,
                            disableBlur = disableBlur,
                            label = "v8BackdropPortrait"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = queueSheetState.collapsedBound)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                        ) {
                            enrichedMetadata?.let {
                                V8PlayerControlsContent(
                                    mediaMetadata = it,
                                    playerDesignStyle = playerDesignStyle,
                                    sliderStyle = SliderStyle.Thick,
                                    playbackState = playbackState,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    repeatMode = repeatMode,
                                    canSkipPrevious = canSkipPrevious,
                                    canSkipNext = canSkipNext,
                                    textButtonColor = Color.White,
                                    iconButtonColor = Color.Black,
                                    textBackgroundColor = Color.White,
                                    icBackgroundColor = Color.Black,
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
                                    currentFormat = currentFormat,
                                    onResetTimer = { visualLifecycle.onInteraction() },
                                    nextUpMetadata = nextUpMetadata,
                                    onExpandQueue = { queueSheetState.expandSoft() },
                                    playerVolume = playerVolume.value,
                                    onVolumeChange = { newVolume ->
                                        playerConnection.service.playerVolume.value = newVolume
                                    }
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                
            },
            classicLandscape = {
                    Row(
                        modifier =
                            Modifier
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .padding(bottom = queueSheetState.collapsedBound + 48.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            val screenWidth = LocalConfiguration.current.screenWidthDp
                            val thumbnailSize = (screenWidth * 0.4).dp
                            Thumbnail(
                                sliderPositionProvider = { seekSession.pendingSeekMs },
                                modifier = Modifier.size(thumbnailSize),
                                isPlayerExpanded = state.isExpanded
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .windowInsetsPadding(
                                        WindowInsets.systemBars.only(
                                            WindowInsetsSides.Top
                                        )
                                    ),
                        ) {
                            Spacer(Modifier.weight(1f))

                            enrichedMetadata?.let {
                                controlsContent(it)
                            }

                            Spacer(Modifier.weight(1f))
                        }
                    }
                
            },
            compactPortrait = {
                    val littleBackground = MaterialTheme.colorScheme.primaryContainer
                    val littleTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                    val progress = calculatePlayerProgress(position, duration, sliderPosition)
                    val progressFraction = progress.fraction
                    val progressOverlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    val seekEnabled = progress.canSeek

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(littleBackground),
                    ) {
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
                                    .littlePlayerOverlayGestures(
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
                                LandscapeLikeBox(modifier = Modifier.fillMaxSize()) {
                                    LittlePlayerContent(
                                        mediaMetadata = metadata,
                                        sliderPosition = sliderPosition,
                                        positionMs = position,
                                        durationMs = duration,
                                        textColor = littleTextColor,
                                        liked = currentSongLiked,
                                        onCollapse = state::collapseSoft,
                                        onToggleLike = playerConnection::toggleLike,
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        V7PlayerBackdrop(
                            thumbnailUrl = mediaMetadata?.thumbnailUrl,
                            disableBlur = disableBlur,
                            label = "v7BackdropPortrait",
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = queueSheetState.collapsedBound)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                        ) {
                            enrichedMetadata?.let {
                                controlsContent(it)
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }
                
            },
            immersivePortrait = {
                    // V8 - Apple Music Style (Portrait)
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        V8PlayerBackdrop(
                            thumbnailUrl = playerVisuals.artworkUrl,
                            canvasArtwork = canvasArtwork,
                            isPlaying = isPlaying,
                            disableBlur = disableBlur,
                            label = "v8BackdropPortrait"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = queueSheetState.collapsedBound)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                        ) {
                            enrichedMetadata?.let {
                                V8PlayerControlsContent(
                                    mediaMetadata = it,
                                    playerDesignStyle = playerDesignStyle,
                                    sliderStyle = SliderStyle.Thick,
                                    playbackState = playbackState,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    repeatMode = repeatMode,
                                    canSkipPrevious = canSkipPrevious,
                                    canSkipNext = canSkipNext,
                                    textButtonColor = Color.White,
                                    iconButtonColor = Color.Black,
                                    textBackgroundColor = Color.White,
                                    icBackgroundColor = Color.Black,
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
                                    currentFormat = currentFormat,
                                    onResetTimer = { visualLifecycle.onInteraction() },
                                    nextUpMetadata = nextUpMetadata,
                                    onExpandQueue = { queueSheetState.expandSoft() },
                                    playerVolume = playerVolume.value,
                                    onVolumeChange = { newVolume ->
                                        playerConnection.service.playerVolume.value = newVolume
                                    }
                                )
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }
                
            },
            classicPortrait = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .windowInsetsPadding(
                                    WindowInsets.systemBars.only(
                                        WindowInsetsSides.Horizontal
                                    )
                                )
                                .padding(bottom = queueSheetState.collapsedBound),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.weight(1f),
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { seekSession.pendingSeekMs },
                                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
                                isPlayerExpanded = state.isExpanded
                            )
                        }

                        enrichedMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.height(30.dp))
                    }
                
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

        // Player 2.0: one sheet integration; existing LyricsScreen stays the owner.
        PlayerLyricsPanel(
            mediaMetadata = mediaMetadata,
            sheetState = lyricsSheetState,
            navController = navController,
            lyricsSyncOffset = lyricsSyncOffset,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LittlePlayerContent(
    mediaMetadata: MediaMetadata,
    sliderPosition: Long?,
    positionMs: Long,
    durationMs: Long,
    textColor: Color,
    liked: Boolean,
    onCollapse: () -> Unit,
    onToggleLike: () -> Unit,
    onExpandQueue: () -> Unit,
    onMenuClick: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val titleColor = textColor.copy(alpha = 0.95f)
        val secondaryColor = textColor.copy(alpha = 0.6f)
        val timeColor = textColor.copy(alpha = 0.85f)

        val scale =
            minOf(maxWidth / 420.dp, maxHeight / 260.dp)
                .coerceIn(0.78f, 1.15f)

        val titleSize = (56f * scale).sp
        val timeSize = (44f * scale).sp
        val iconSize = (26f * scale).dp
        val collapseIconSize = (28f * scale).dp
        val horizontalPadding = (18f * scale).dp
        val verticalPadding = (10f * scale).dp

        val progress = calculatePlayerProgress(positionMs, durationMs, sliderPosition)
        val displayPositionMs = progress.displayPositionMs

        val timeText = remember(displayPositionMs, durationMs) {
            val positionText = makeTimeString(displayPositionMs)
            val durationText = if (durationMs != C.TIME_UNSET) makeTimeString(durationMs) else ""
            if (durationText.isBlank()) positionText else "$positionText/$durationText"
        }

        val artistsText = remember(mediaMetadata.artists) {
            mediaMetadata.artists.joinToString(separator = ", ") { artist -> artist.name }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = mediaMetadata.title,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "little_title",
                    ) { title ->
                        Text(
                            text = title,
                            color = titleColor,
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(),
                        )
                    }

                    Spacer(Modifier.height((10f * scale).dp))

                    mediaMetadata.album?.title?.takeIf { it.isNotBlank() }?.let { albumTitle ->
                        AnimatedContent(
                            targetState = albumTitle,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "little_album",
                        ) { album ->
                            Text(
                                text = album,
                                color = secondaryColor,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }

                    artistsText.takeIf { it.isNotBlank() }?.let { artists ->
                        AnimatedContent(
                            targetState = artists,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "little_artists",
                        ) { artistLine ->
                            Text(
                                text = "by - $artistLine",
                                color = secondaryColor,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }
                }

                Spacer(Modifier.width((16f * scale).dp))

                Text(
                    text = timeText,
                    color = timeColor,
                    fontSize = timeSize,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = (140f * scale).dp),
                )
            }

            Spacer(Modifier.height((14f * scale).dp))

            Spacer(Modifier.height((6f * scale).dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.8f),
                    modifier =
                        Modifier
                            .size(collapseIconSize)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCollapse,
                            ),
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    painter = painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = null,
                    tint =
                        if (liked) MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                        else textColor.copy(alpha = 0.78f),
                    modifier =
                        Modifier
                            .size(iconSize)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleLike,
                            ),
                )

                Spacer(Modifier.width((18f * scale).dp))

                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.78f),
                    modifier =
                        Modifier
                            .size(iconSize)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onExpandQueue,
                            ),
                )

                Spacer(Modifier.width((18f * scale).dp))

                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.78f),
                    modifier =
                        Modifier
                            .size(iconSize)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onMenuClick,
                            ),
                )
            }
        }
    }
}

@Composable
private fun LandscapeLikeBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.graphicsLayer { clip = true },
    ) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) {}
        } else {
            val swappedConstraints =
                Constraints(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight,
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth,
                )

            val placeable = measurable.measure(swappedConstraints)
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val rotatedWidth = placeable.height
            val rotatedHeight = placeable.width

            val x = ((width - rotatedWidth) / 2).coerceAtLeast(0)
            val y = ((height - rotatedHeight) / 2).coerceAtLeast(0)

            layout(width, height) {
                placeable.placeWithLayer(x, y) {
                    transformOrigin = TransformOrigin(0f, 0f)
                    rotationZ = 90f
                    translationX = placeable.height.toFloat()
                }
            }
        }
    }
}

private fun Modifier.littlePlayerOverlayGestures(
    seekEnabled: Boolean,
    durationMs: Long,
    progressFraction: Float,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onSeekToPositionMs: (Long) -> Unit,
    onSeekFinished: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
): Modifier {
    return pointerInput(seekEnabled, durationMs, canSkipPrevious, canSkipNext) {
        var lastTapUptimeMs = 0L
        var lastTapPosition: Offset? = null
        val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis.toLong()
        val touchSlop = viewConfiguration.touchSlop

        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = true)
            val pointerId = down.id

            var upPosition = down.position
            val minOverlayHeightPx = 24.dp.toPx()
            val overlayHeightPx =
                (progressFraction * size.height).coerceAtLeast(minOverlayHeightPx)
            val seekAllowedFromDown =
                seekEnabled &&
                        durationMs > 0L &&
                        durationMs != C.TIME_UNSET &&
                        down.position.y <= overlayHeightPx

            var isSeeking = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == pointerId } ?: continue
                upPosition = change.position

                if (!change.pressed) break

                if (!isSeeking && seekAllowedFromDown) {
                    val distanceFromDown = (change.position - down.position).getDistance()
                    if (distanceFromDown > touchSlop) isSeeking = true
                }

                if (isSeeking) {
                    val fraction =
                        if (size.height > 0) (change.position.y / size.height.toFloat()) else 0f
                    val clampedFraction = fraction.coerceIn(0f, 1f)

                    val targetMs =
                        (durationMs.toDouble() * clampedFraction.toDouble()).roundToLong().coerceIn(0L, durationMs)
                    onSeekToPositionMs(targetMs)
                    change.consume()
                }
            }

            if (isSeeking) {
                onSeekFinished()
                lastTapUptimeMs = 0L
                lastTapPosition = null
            } else {
                val now = SystemClock.uptimeMillis()
                val previousTapPosition = lastTapPosition
                val isDoubleTap =
                    previousTapPosition != null &&
                            (now - lastTapUptimeMs) <= doubleTapTimeoutMs &&
                            (upPosition - previousTapPosition).getDistance() <= (touchSlop * 2f)

                if (isDoubleTap) {
                    val isTopSide = upPosition.y < size.height / 2f
                    if (isTopSide) {
                        if (canSkipPrevious) onSkipPrevious()
                    } else {
                        if (canSkipNext) onSkipNext()
                    }
                    lastTapUptimeMs = 0L
                    lastTapPosition = null
                } else {
                    lastTapUptimeMs = now
                    lastTapPosition = upPosition
                }
            }
        }
    }
}
