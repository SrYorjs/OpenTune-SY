package com.arturo254.opentune.playback

import androidx.datastore.preferences.core.edit
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.MediaNotification
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.constants.AutoSkipNextOnErrorKey
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.constants.PermanentShuffleKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.constants.SkipSilenceKey
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.setOffloadEnabled
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber

internal object MusicServiceComposition {
    data class Callbacks(
        val ensureScopesActive: () -> Unit,
        val recoverSong:
            suspend (
                String,
                YTPlayerUtils.PlaybackData?,
            ) -> Unit,
        val installNotificationProvider:
            (MediaNotification.Provider) -> Unit,
    )

    data class Components(
        val player: ExoPlayer,
        val sleepTimer: SleepTimer,
        val streamResolver: StreamResolver,
        val audioEffectsController:
            AudioEffectsController,
        val queueController: QueueController,
        val togetherController:
            TogetherController,
        val errorRecoveryController:
            ErrorRecoveryController,
        val automixRadioController:
            AutomixRadioController,
        val playbackPersistenceController:
            PlaybackPersistenceController,
        val audioFocusController:
            AudioFocusController,
        val playbackLifecycleController:
            PlaybackLifecycleController,
        val libraryActionsController:
            LibraryActionsController,
        val presenceHistoryController:
            PresenceHistoryController,
        val playbackEventCoordinator:
            PlaybackEventCoordinator,
        val mediaSessionNotificationController:
            MediaSessionNotificationController,
        val playbackRuntimeController:
            PlaybackRuntimeController,
    )

    fun create(
        service: MusicService,
        scopeProvider: () -> CoroutineScope,
        ioScopeProvider: () -> CoroutineScope,
        audioFocusVolumeFactor:
            MutableStateFlow<Float>,
        audioQualityProvider: () -> AudioQuality,
        preferredStreamClientProvider:
            () -> PlayerStreamClient,
        callbacks: Callbacks,
    ): Components {
        val dataStore =
            service.dataStore

        var playerRef:
            ExoPlayer? = null
        var errorRecoveryRef:
            ErrorRecoveryController? = null
        var presenceRef:
            PresenceHistoryController? = null
        var mediaSessionRef:
            MediaSessionNotificationController? =
            null

        callbacks.ensureScopesActive()

        val authRecoveryUiController =
            AuthRecoveryUiController(service)

        val streamResolver =
            StreamResolver(
                context = service,
                database = service.database,
                playerCache =
                    service.playerCache,
                downloadCache =
                    service.downloadCache,
                scopeProvider = scopeProvider,
                playerProvider = {
                    requireNotNull(playerRef)
                },
                audioQualityProvider =
                    audioQualityProvider,
                preferredStreamClientProvider =
                    preferredStreamClientProvider,
                errorRecoveryControllerProvider = {
                    requireNotNull(
                        errorRecoveryRef,
                    )
                },
                recoverSong =
                    callbacks.recoverSong,
                promptLoginRecovery =
                    authRecoveryUiController
                        ::promptLoginRecovery,
            )

        val playbackEngine =
            PlaybackEngine(
                context = service,
                streamResolver =
                    streamResolver,
            )

        val player =
            playbackEngine.createPlayer()

        playerRef = player

        val sleepTimer =
            SleepTimer(
                scopeProvider(),
                player,
            )

        player.apply {
            addListener(service)
            addListener(sleepTimer)
            addAnalyticsListener(
                PlaybackStatsListener(
                    false,
                    service,
                ),
            )
            setOffloadEnabled(false)
        }

        val audioEffectsController =
            AudioEffectsController(
                context = service,
                playerProvider = {
                    player
                },
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                ioScopeProvider =
                    ioScopeProvider,
            )

        val queueController =
            QueueController(
                player = player,
                scopeProvider = scopeProvider,
                hideExplicitProvider = {
                    dataStore.get(
                        HideExplicitKey,
                        false,
                    )
                },
                hideVideoProvider = {
                    dataStore.get(
                        HideVideoKey,
                        false,
                    )
                },
                permanentShuffleProvider = {
                    dataStore.get(
                        PermanentShuffleKey,
                        false,
                    )
                },
                onCurrentMetadataChanged = {
                    service
                        .currentMediaMetadata
                        .value =
                        player.currentMetadata
                },
                onQueueRestored = {
                    mediaSessionRef
                        ?.updateCustomLayout()
                },
                onDeferredLoadError = {
                    error ->
                    Timber.e(
                        error,
                        "Failed to load deferred queue items",
                    )
                },
            )

        val togetherController =
            TogetherController(
                context = service,
                player = player,
                queueController =
                    queueController,
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                ioScopeProvider =
                    ioScopeProvider,
                ensureScopesActive =
                    callbacks.ensureScopesActive,
                onError = {
                    service.reportException(it)
                },
            )

        val errorRecoveryController =
            ErrorRecoveryController(
                player = player,
                scopeProvider = scopeProvider,
                downloadCache =
                    service.downloadCache,
                playerCache =
                    service.playerCache,
                preferredStreamClientProvider =
                    preferredStreamClientProvider,
                autoSkipNextOnErrorProvider = {
                    dataStore.get(
                        AutoSkipNextOnErrorKey,
                        false,
                    )
                },
                skipSilenceEnabledProvider = {
                    dataStore.get(
                        SkipSilenceKey,
                        false,
                    )
                },
                disableSkipSilence = {
                    dataStore.edit {
                        settings ->
                        settings[
                            SkipSilenceKey
                        ] = false
                    }
                },
            )

        errorRecoveryRef =
            errorRecoveryController

        val automixRadioController =
            AutomixRadioController(
                context = service,
                player = player,
                queueController =
                    queueController,
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                addToQueue = {
                    items: List<MediaItem> ->
                    service.addToQueue(items)
                },
                playNext = {
                    items: List<MediaItem> ->
                    service.playNext(items)
                },
            )

        val playbackPersistenceController =
            PlaybackPersistenceController(
                context = service,
                player = player,
                queueController =
                    queueController,
                automixRadioController =
                    automixRadioController,
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                playerVolume =
                    service.playerVolume,
                currentMediaMetadata =
                    service
                        .currentMediaMetadata,
                onRestored = {
                    mediaSessionRef
                        ?.updateCustomLayout()
                },
                onError = {
                    service.reportException(it)
                },
            )

        val audioFocusController =
            AudioFocusController(
                context = service,
                player = player,
                audioFocusVolumeFactor =
                    audioFocusVolumeFactor,
            )

        val playbackLifecycleController =
            PlaybackLifecycleController(
                service = service,
                player = player,
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                isTogetherIdle = {
                    togetherController
                        .sessionState
                        .value is
                        com.arturo254.opentune.together.TogetherSessionState.Idle
                },
                isTogetherGuest = {
                    togetherController
                        .isGuestSession()
                },
                isHostSessionActive = {
                    togetherController
                        .isHostSessionActive()
                },
                stopTogether = {
                    togetherController.stop()
                },
                markTogetherIdle = {
                    togetherController
                        .markIdle()
                },
                savePlaybackStateBlocking = {
                    playbackPersistenceController
                        .saveBlockingIfEnabled()
                },
                stopAndClearPlayback = {
                    service
                        .stopAndClearPlayback()
                },
                clearPresenceOnTaskRemoved = {
                    presenceRef
                        ?.clearOnTaskRemoved()
                },
                foregroundNotificationProvider = {
                    requireNotNull(
                        mediaSessionRef,
                    ).createForegroundNotification()
                },
                onError = {
                    service.reportException(it)
                },
            )

        val libraryActionsController =
            LibraryActionsController(
                context = service,
                database = service.database,
                dataStore = dataStore,
                syncUtils =
                    service.syncUtils,
                currentMediaMetadata =
                    service
                        .currentMediaMetadata,
                scope = scopeProvider(),
            )

        val presenceHistoryController =
            PresenceHistoryController(
                context = service,
                player = player,
                database = service.database,
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                ioScopeProvider =
                    ioScopeProvider,
                currentSong =
                    libraryActionsController
                        .currentSong,
                onCurrentSongChanged = {
                    service
                        .refreshPlaybackNotification()
                },
                onError = {
                    service.reportException(it)
                },
            )

        presenceRef =
            presenceHistoryController

        val mediaSessionNotificationController =
            MediaSessionNotificationController(
                service = service,
                player = player,
                mediaLibrarySessionCallback =
                    service
                        .mediaLibrarySessionCallback,
                scopeProvider = scopeProvider,
                lifecycleController =
                    playbackLifecycleController,
                isCurrentSongLiked = {
                    libraryActionsController
                        .currentSong
                        .value
                        ?.song
                        ?.liked == true
                },
                hasCurrentSong = {
                    libraryActionsController
                        .currentSong
                        .value != null
                },
                toggleLike = {
                    libraryActionsController
                        .toggleLike()
                },
                toggleLibrary = {
                    libraryActionsController
                        .toggleLibrary()
                },
                toggleStartRadio = {
                    service.toggleStartRadio()
                },
                installNotificationProvider =
                    callbacks
                        .installNotificationProvider,
                onError = {
                    service.reportException(it)
                },
            )

        mediaSessionRef =
            mediaSessionNotificationController

        val playbackRuntimeController =
            PlaybackRuntimeController(
                context = service,
                player = player,
                database = service.database,
                dataStore = dataStore,
                lyricsHelper =
                    service.lyricsHelper,
                streamResolver =
                    streamResolver,
                playbackEngine =
                    playbackEngine,
                audioEffectsController =
                    audioEffectsController,
                errorRecoveryController =
                    errorRecoveryController,
                presenceHistoryController =
                    presenceHistoryController,
                persistenceController =
                    playbackPersistenceController,
                mediaSessionNotificationController =
                    mediaSessionNotificationController,
                lifecycleController =
                    playbackLifecycleController,
                currentMediaMetadata =
                    service
                        .currentMediaMetadata,
                playerVolume =
                    service.playerVolume,
                audioFocusVolumeFactor =
                    audioFocusVolumeFactor,
                scopeProvider = scopeProvider,
                ioScopeProvider =
                    ioScopeProvider,
            )

        val playbackEventCoordinator =
            PlaybackEventCoordinator(
                player = player,
                dataStore = dataStore,
                scopeProvider = scopeProvider,
                currentMediaMetadata =
                    service
                        .currentMediaMetadata,
                errorRecoveryController =
                    errorRecoveryController,
                crossfadeAudioProvider = {
                    playbackRuntimeController
                        .crossfadeAudio
                },
                lyricsPreloadManagerProvider = {
                    playbackRuntimeController
                        .lyricsPreloadManager
                },
                streamResolver =
                    streamResolver,
                togetherController =
                    togetherController,
                automixRadioController =
                    automixRadioController,
                queueController =
                    queueController,
                persistenceController =
                    playbackPersistenceController,
                presenceHistoryController =
                    presenceHistoryController,
                lifecycleController =
                    playbackLifecycleController,
                audioEffectsController =
                    audioEffectsController,
                requestAudioFocus = {
                    audioFocusController
                        .requestFocus()
                },
                updateNotification = {
                    mediaSessionNotificationController
                        .updateCustomLayout()
                },
                refreshNotification = {
                    service
                        .refreshPlaybackNotification()
                },
                updateWidgets = {
                    mediaSessionNotificationController
                        .updateWidgets()
                },
            )

        return Components(
            player = player,
            sleepTimer = sleepTimer,
            streamResolver =
                streamResolver,
            audioEffectsController =
                audioEffectsController,
            queueController =
                queueController,
            togetherController =
                togetherController,
            errorRecoveryController =
                errorRecoveryController,
            automixRadioController =
                automixRadioController,
            playbackPersistenceController =
                playbackPersistenceController,
            audioFocusController =
                audioFocusController,
            playbackLifecycleController =
                playbackLifecycleController,
            libraryActionsController =
                libraryActionsController,
            presenceHistoryController =
                presenceHistoryController,
            playbackEventCoordinator =
                playbackEventCoordinator,
            mediaSessionNotificationController =
                mediaSessionNotificationController,
            playbackRuntimeController =
                playbackRuntimeController,
        )
    }
}
