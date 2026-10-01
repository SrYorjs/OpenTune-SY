/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



@file:Suppress("DEPRECATION")

package com.arturo254.opentune.playback

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.MediaCodecList
import android.media.audiofx.Virtualizer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.ExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.SongItem
import com.arturo254.opentune.lyrics.LyricsPreloadManager
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.MainActivity
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AudioNormalizationKey
import com.arturo254.opentune.constants.AudioOffload
import com.arturo254.opentune.constants.AudioCrossfadeDurationKey
import com.arturo254.opentune.constants.AudioQualityKey
import com.arturo254.opentune.constants.AutoSkipNextOnErrorKey
import com.arturo254.opentune.constants.EqualizerBandLevelsMbKey
import com.arturo254.opentune.constants.EqualizerBassBoostEnabledKey
import com.arturo254.opentune.constants.EqualizerBassBoostStrengthKey
import com.arturo254.opentune.constants.EqualizerEnabledKey
import com.arturo254.opentune.constants.EqualizerOutputGainEnabledKey
import com.arturo254.opentune.constants.EqualizerOutputGainMbKey
import com.arturo254.opentune.constants.EqualizerSelectedProfileIdKey
import com.arturo254.opentune.constants.EqualizerVirtualizerEnabledKey
import com.arturo254.opentune.constants.EqualizerVirtualizerStrengthKey
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleLike
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleStartRadio
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleShuffle
import com.arturo254.opentune.constants.PermanentShuffleKey
import com.arturo254.opentune.constants.PersistentQueueKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.constants.PlayerStreamClientKey
import com.arturo254.opentune.constants.PlayerVolumeKey
import com.arturo254.opentune.constants.RepeatModeKey
import com.arturo254.opentune.constants.ShowLyricsKey
import com.arturo254.opentune.constants.SkipSilenceKey
import com.arturo254.opentune.constants.MaxSongCacheSizeKey
import com.arturo254.opentune.constants.SmartTrimmerKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.db.entities.RelatedSongMap
import com.arturo254.opentune.di.DownloadCache
import com.arturo254.opentune.di.PlayerCache
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.collect
import com.arturo254.opentune.extensions.collectLatest
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.directorySizeBytes
import com.arturo254.opentune.extensions.findNextMediaItemById
import com.arturo254.opentune.extensions.mediaItems
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.setOffloadEnabled
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.lyrics.LyricsHelper
import com.arturo254.opentune.models.PersistQueue
import com.arturo254.opentune.models.PersistPlayerState
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.queues.Queue
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.playback.queues.filterExplicit
import com.arturo254.opentune.playback.queues.filterVideo
import com.arturo254.opentune.utils.SyncUtils
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.StreamClientUtils
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.enumPreference
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.reportException
import com.arturo254.opentune.utils.NetworkConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint
import com.arturo254.opentune.constants.TogetherClientIdKey
import com.arturo254.jossredconnect.JossRedClient
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.Request
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds
import timber.log.Timber
import com.arturo254.opentune.constants.JossRedMultimediaKey

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService :
    MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var syncUtils: SyncUtils

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private lateinit var audioFocusController: AudioFocusController
    private lateinit var playbackLifecycleController:
        PlaybackLifecycleController
    private lateinit var mediaSessionNotificationController:
        MediaSessionNotificationController
    private lateinit var libraryActionsController:
        LibraryActionsController
    private lateinit var presenceHistoryController:
        PresenceHistoryController
    private lateinit var playbackEventCoordinator:
        PlaybackEventCoordinator

    private var scopeJob = Job()
    private var scope = CoroutineScope(Dispatchers.Main + scopeJob)
    private var ioScope = CoroutineScope(Dispatchers.IO + scopeJob)
    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    private lateinit var errorRecoveryController: ErrorRecoveryController

    val waitingForNetworkConnection
        get() = errorRecoveryController.waitingForNetworkConnection

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.arturo254.opentune.constants.AudioQuality.AUTO
    )
    private val preferredStreamClient by enumPreference(
        this,
        PlayerStreamClientKey,
        PlayerStreamClient.ANDROID_VR
    )
    private lateinit var streamResolver: StreamResolver
    private lateinit var playbackEngine: PlaybackEngine
    private lateinit var playbackPersistenceController:
        PlaybackPersistenceController

    private lateinit var queueController: QueueController

    private var currentQueue: Queue
        get() = queueController.currentQueue
        set(value) {
            queueController.currentQueue = value
        }

    var queueTitle: String?
        get() = queueController.queueTitle
        set(value) {
            queueController.queueTitle = value
        }

    private var suppressAutoPlayback: Boolean
        get() = queueController.suppressAutoPlayback
        set(value) {
            queueController.suppressAutoPlayback = value
        }

    @Volatile
    private var lastLoginRecoveryPrompt:
        Pair<String, Long>? = null

    val currentMediaMetadata =
        MutableStateFlow<com.arturo254.opentune.models.MediaMetadata?>(null)

    val queueRestoreCompleted
        get() =
            playbackPersistenceController.queueRestoreCompleted
    private val currentFormat =
        currentMediaMetadata.flatMapLatest { mediaMetadata ->
            database.format(mediaMetadata?.id)
        }.flowOn(Dispatchers.IO)

    private val normalizeFactor = MutableStateFlow(1f)
    var playerVolume = MutableStateFlow(1f)
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    private val playbackFadeFactor = MutableStateFlow(1f)
    private val crossfadeDurationMs = MutableStateFlow(0)
    private val audioNormalizationEnabled = MutableStateFlow(true)
    private var crossfadeAudio: CrossfadeAudio? = null
    private var lyricsPreloadManager: LyricsPreloadManager? = null

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        return appProcesses.any { processInfo ->
            processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    processInfo.processName == packageName
        }
    }

    private fun promptLoginRecovery(mediaId: String, targetUrl: String) {
        if (!isAppInForeground()) return

        val now = System.currentTimeMillis()
        val lastPrompt = lastLoginRecoveryPrompt
        if (lastPrompt?.first == mediaId && now - lastPrompt.second < 10000L) return
        lastLoginRecoveryPrompt = mediaId to now

        val deepLink = Uri.parse("OpenTune://login?url=${Uri.encode(targetUrl)}")
        val intent = Intent(Intent.ACTION_VIEW, deepLink, this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        runCatching {
            startActivity(intent)
        }.onFailure {
            Timber.e(it, "Failed to open login recovery for %s", mediaId)
        }
    }

    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: Cache

    @Inject
    @DownloadCache
    lateinit var downloadCache: Cache

    lateinit var player: ExoPlayer

    private lateinit var audioEffectsController: AudioEffectsController

    val eqCapabilities
        get() = audioEffectsController.eqCapabilities

    private lateinit var automixRadioController: AutomixRadioController

    val automixItems
        get() = automixRadioController.automixItems

    val automixLoading
        get() = automixRadioController.automixLoading

    val automixError
        get() = automixRadioController.automixError

    val autoAddedMediaIds
        get() = automixRadioController.autoAddedMediaIds

    val maxSafeGainFactor = 1.414f // +3 dB

    private lateinit var togetherController: TogetherController

    val togetherSessionState
        get() = togetherController.sessionState

    private fun isTogetherApplyingRemote(): Boolean =
        togetherController.isApplyingRemote()

    private fun showTogetherNotice(
        message: String,
        key: String? = null,
    ) {
        togetherController.showNotice(
            message,
            key,
        )
    }

    private fun ensureStartedAsForeground() {
        playbackLifecycleController
            .ensureStartedAsForeground()
    }

    override fun onCreate() {
        super.onCreate()
        ensureScopesActive()

        streamResolver =
            StreamResolver(
                context = this,
                database = database,
                playerCache = playerCache,
                downloadCache = downloadCache,
                scopeProvider = { scope },
                playerProvider = { player },
                audioQualityProvider = { audioQuality },
                preferredStreamClientProvider = {
                    preferredStreamClient
                },
                errorRecoveryControllerProvider = {
                    errorRecoveryController
                },
                recoverSong = { mediaId, playbackData ->
                    recoverSong(mediaId, playbackData)
                },
                promptLoginRecovery = ::promptLoginRecovery,
            )

        playbackEngine =
            PlaybackEngine(
                context = this,
                streamResolver = streamResolver,
            )

        player =
            playbackEngine
                .createPlayer()
                .apply {
                    addListener(this@MusicService)
                    sleepTimer = SleepTimer(scope, this)
                    addListener(sleepTimer)
                    addAnalyticsListener(
                        PlaybackStatsListener(
                            false,
                            this@MusicService,
                        ),
                    )
                    setOffloadEnabled(false)
                }

        audioEffectsController =
            AudioEffectsController(
                context = this,
                playerProvider = { player },
                dataStore = dataStore,
                scopeProvider = { scope },
                ioScopeProvider = { ioScope },
            )

        queueController =
            QueueController(
                player = player,
                scopeProvider = { scope },
                hideExplicitProvider = { dataStore.get(HideExplicitKey, false) },
                hideVideoProvider = { dataStore.get(HideVideoKey, false) },
                permanentShuffleProvider = { dataStore.get(PermanentShuffleKey, false) },
                onCurrentMetadataChanged = {
                    currentMediaMetadata.value = player.currentMetadata
                },
                onQueueRestored = ::updateNotification,
                onDeferredLoadError = { error ->
                    Timber.e(error, "Failed to load deferred queue items")
                },
            )

        togetherController =
            TogetherController(
                context = this,
                player = player,
                queueController = queueController,
                dataStore = dataStore,
                scopeProvider = { scope },
                ioScopeProvider = { ioScope },
                ensureScopesActive =
                    ::ensureScopesActive,
                onError = ::reportException,
            )

        errorRecoveryController =
            ErrorRecoveryController(
                player = player,
                scopeProvider = { scope },
                downloadCache = downloadCache,
                playerCache = playerCache,
                preferredStreamClientProvider = { preferredStreamClient },
                autoSkipNextOnErrorProvider = {
                    dataStore.get(AutoSkipNextOnErrorKey, false)
                },
                skipSilenceEnabledProvider = {
                    dataStore.get(SkipSilenceKey, false)
                },
                disableSkipSilence = {
                    dataStore.edit { settings ->
                        settings[SkipSilenceKey] = false
                    }
                },
            )

        automixRadioController =
            AutomixRadioController(
                context = this,
                player = player,
                queueController = queueController,
                dataStore = dataStore,
                scopeProvider = { scope },
                addToQueue = ::addToQueue,
                playNext = ::playNext,
            )

        playbackPersistenceController =
            PlaybackPersistenceController(
                context = this,
                player = player,
                queueController = queueController,
                automixRadioController =
                    automixRadioController,
                dataStore = dataStore,
                scopeProvider = { scope },
                playerVolume = playerVolume,
                currentMediaMetadata =
                    currentMediaMetadata,
                onRestored = ::updateNotification,
                onError = ::reportException,
            )

        audioFocusController =
            AudioFocusController(
                context = this,
                player = player,
                audioFocusVolumeFactor = audioFocusVolumeFactor,
            )
        playbackLifecycleController =
            PlaybackLifecycleController(
                service = this,
                player = player,
                dataStore = dataStore,
                scopeProvider = { scope },
                isTogetherIdle = {
                    togetherSessionState.value is
                        com.arturo254.opentune.together.TogetherSessionState.Idle
                },
                isTogetherGuest = {
                    togetherController.isGuestSession()
                },
                isHostSessionActive = {
                    togetherController.isHostSessionActive()
                },
                stopTogether = {
                    togetherController.stop()
                },
                markTogetherIdle = {
                    togetherController.markIdle()
                },
                savePlaybackStateBlocking = {
                    playbackPersistenceController
                        .saveBlockingIfEnabled()
                },
                stopAndClearPlayback = {
                    stopAndClearPlayback()
                },
                clearPresenceOnTaskRemoved = {
                    presenceHistoryController
                        .clearOnTaskRemoved()
                },
                foregroundNotificationProvider = {
                    mediaSessionNotificationController
                        .createForegroundNotification()
                },
                onError = ::reportException,
            )

        libraryActionsController =
            LibraryActionsController(
                context = this,
                database = database,
                dataStore = dataStore,
                syncUtils = syncUtils,
                currentMediaMetadata =
                    currentMediaMetadata,
                scope = scope,
            )

        presenceHistoryController =
            PresenceHistoryController(
                context = this,
                player = player,
                database = database,
                dataStore = dataStore,
                scopeProvider = { scope },
                ioScopeProvider = { ioScope },
                currentSong =
                    libraryActionsController
                        .currentSong,
                onCurrentSongChanged = {
                    refreshPlaybackNotification()
                },
                onError = ::reportException,
            )

        playbackEventCoordinator =
            PlaybackEventCoordinator(
                player = player,
                dataStore = dataStore,
                scopeProvider = { scope },
                currentMediaMetadata =
                    currentMediaMetadata,
                errorRecoveryController =
                    errorRecoveryController,
                crossfadeAudioProvider = {
                    crossfadeAudio
                },
                lyricsPreloadManagerProvider = {
                    lyricsPreloadManager
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
                    requestAudioFocus()
                },
                updateNotification = {
                    updateNotification()
                },
                refreshNotification = {
                    refreshPlaybackNotification()
                },
                updateWidgets = {
                    updatePlayerWidgets()
                },
            )

        mediaSessionNotificationController =
            MediaSessionNotificationController(
                service = this,
                player = player,
                mediaLibrarySessionCallback =
                    mediaLibrarySessionCallback,
                scopeProvider = { scope },
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
                toggleLike = ::toggleLike,
                toggleLibrary = ::toggleLibrary,
                toggleStartRadio = ::toggleStartRadio,
                installNotificationProvider = { provider ->
                    setMediaNotificationProvider(provider)
                },
                onError = ::reportException,
            )

        mediaSessionNotificationController.start()
        playbackLifecycleController.start()

        scope.launch(Dispatchers.IO) {
            val prefs = dataStore.data.first()
            val repeatMode = prefs[RepeatModeKey] ?: REPEAT_MODE_OFF
            val volume = (prefs[PlayerVolumeKey] ?: 1f).coerceIn(0f, 1f)
            val offload = prefs[AudioOffload] ?: false
            withContext(Dispatchers.Main) {
                player.repeatMode = repeatMode
                playerVolume.value = volume
                playbackEngine.updateAudioOffload(player, offload)
            }
        }

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                errorRecoveryController.onNetworkConnectivityChanged(isConnected)
            }
        }

        combine(playerVolume, normalizeFactor, audioFocusVolumeFactor, playbackFadeFactor) { playerVolume, normalizeFactor, audioFocusVolumeFactor, playbackFadeFactor ->
            playerVolume * normalizeFactor * audioFocusVolumeFactor * playbackFadeFactor
        }.collectLatest(scope) { finalVolume ->
            player.volume = finalVolume
        }

        playerVolume.debounce(1000).collect(ioScope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(ioScope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id)
                    .first() == null
            ) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        combine(
            dataStore.data
                .map { it[AudioOffload] ?: false }
                .distinctUntilChanged(),
            audioEffectsController.internalDspActive,
        ) { requestedOffload, dspActive ->
            requestedOffload to dspActive
        }.collectLatest(scope) { (requestedOffload, dspActive) ->
            val effectiveOffload = requestedOffload && !dspActive
            playbackEngine.updateAudioOffload(player, effectiveOffload)

            if (effectiveOffload) {
                val skipSilenceEnabled =
                    dataStore.get(SkipSilenceKey, false)
                if (skipSilenceEnabled) {
                    dataStore.edit { it[SkipSilenceKey] = false }
                    player.skipSilenceEnabled = false
                }

                val crossfadeSeconds =
                    dataStore.get(AudioCrossfadeDurationKey, 0)
                if (crossfadeSeconds != 0) {
                    dataStore.edit {
                        it[AudioCrossfadeDurationKey] = 0
                    }
                }
            }
        }

        dataStore.data
            .map { (it[AudioCrossfadeDurationKey] ?: 0) * 1000 }
            .distinctUntilChanged()
            .collectLatest(scope) {
                crossfadeDurationMs.value = it
            }

        crossfadeAudio =
            CrossfadeAudio(
                player = player,
                database = database,
                crossfadeDurationMs = crossfadeDurationMs,
                playbackFadeFactor = playbackFadeFactor,
                playerVolume = playerVolume,
                audioFocusVolumeFactor = audioFocusVolumeFactor,
                audioNormalizationEnabled = audioNormalizationEnabled,
                maxSafeGainFactor = maxSafeGainFactor,
                overlapPlayerFactory = {
                    playbackEngine.createOverlapPlayer()
                },
                onCrossfadeStart = { mediaItem ->
                    currentMediaMetadata.value =
                        mediaItem.metadata
                    presenceHistoryController
                        .onCrossfadeStart(
                            mediaItem,
                        )
                }
            ).also { it.start(scope) }

        // Initialize lyrics pre-load manager
        lyricsPreloadManager = LyricsPreloadManager(
            context = this,
            database = database,
            networkConnectivity = connectivityObserver,
        )

        audioEffectsController.start()

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            audioNormalizationEnabled.value = normalizeAudio
            Timber.tag("AudioNormalization").d("Audio normalization enabled: $normalizeAudio")
            Timber.tag("AudioNormalization").d("Format loudnessDb: ${format?.loudnessDb}, perceptualLoudnessDb: ${format?.perceptualLoudnessDb}")

            normalizeFactor.value =
                if (normalizeAudio) {
                    // Use loudnessDb if available, otherwise fall back to perceptualLoudnessDb
                    val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb

                    if (loudness != null) {
                        val loudnessDb = loudness.toFloat()
                        var factor = 10f.pow(-loudnessDb / 20)

                        Timber.tag("AudioNormalization").d("Calculated raw normalization factor: $factor (from loudness: $loudnessDb)")

                        if (factor > 1f) {
                            factor = min(factor, maxSafeGainFactor)
                            Timber.tag("AudioNormalization").d("Factor capped at maxSafeGainFactor: $factor")
                        }

                        Timber.tag("AudioNormalization").i("Applying normalization factor: $factor")
                        factor
                    } else {
                        Timber.tag("AudioNormalization").w("Normalization enabled but no loudness data available - no normalization applied")
                        1f
                    }
                } else {
                    Timber.tag("AudioNormalization").d("Normalization disabled - using factor 1.0")
                    1f
                }
        }

        dataStore.data
            .map { prefs ->
                (prefs[SmartTrimmerKey] ?: false) to (prefs[MaxSongCacheSizeKey] ?: 1024)
            }
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest(ioScope) { (enabled, maxSongCacheSizeMb) ->
                if (!enabled) return@collectLatest
                if (maxSongCacheSizeMb <= 0 || maxSongCacheSizeMb == -1) return@collectLatest
                val bytesPerMb = 1024L * 1024L
                val safeSizeMb = maxSongCacheSizeMb.toLong().coerceAtMost(Long.MAX_VALUE / bytesPerMb)
                val limitBytes = safeSizeMb * bytesPerMb
                streamResolver.trimPlayerCacheToBytes(limitBytes)
            }

        presenceHistoryController.start()

        playbackPersistenceController.start()
    }

    private fun ensureScopesActive() {
        if (!scopeJob.isActive) {
            scopeJob = Job()
        }
        if (!scope.isActive) {
            scope = CoroutineScope(Dispatchers.Main + scopeJob)
        }
        if (!ioScope.isActive) {
            ioScope = CoroutineScope(Dispatchers.IO + scopeJob)
        }
    }

    private fun requestAudioFocus(): Boolean =
        audioFocusController.requestFocus()

    private fun abandonAudioFocus() {
        audioFocusController.abandonFocus()
    }

    fun hasAudioFocusForPlayback(): Boolean =
        audioFocusController.hasFocus()

    private fun updateNotification() {
        mediaSessionNotificationController
            .updateCustomLayout()
    }

    fun refreshPlaybackNotification() {
        updateNotification()
        runCatching {
            super.onUpdateNotification(
                mediaSessionNotificationController
                    .session,
                player.isPlaying,
            )
        }.onFailure {
            reportException(it)
        }
    }

    private suspend fun recoverSong(
        mediaId: String,
        playbackData: YTPlayerUtils.PlaybackData? = null
    ) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId)
                .getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint =
                YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint
                    ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(
        queue: Queue,
        playWhenReady: Boolean = true,
    ) {
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (!isTogetherApplyingRemote() && joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
            if (!joined.roomState.settings.allowGuestsToControlPlayback) {
                showTogetherNotice(getString(R.string.not_allowed), key = "GUEST_PLAYQUEUE_DISABLED")
                return
            }
            ensureScopesActive()
            scope.launch(SilentHandler) {
                val initialStatus =
                    withContext(Dispatchers.IO) {
                        queue.getInitialStatus()
                            .filterExplicit(dataStore.get(HideExplicitKey, false))
                            .filterVideo(dataStore.get(HideVideoKey, false))
                    }

                val targetItem =
                    initialStatus.items.getOrNull(initialStatus.mediaItemIndex)
                        ?: queue.preloadItem?.toMediaItem()

                val meta = targetItem?.metadata
                val trackId =
                    meta?.id?.trim().orEmpty().ifBlank {
                        targetItem?.mediaId?.trim().orEmpty()
                    }
                if (trackId.isBlank()) {
                    showTogetherNotice(getString(R.string.not_allowed), key = "GUEST_PLAYQUEUE_NO_TRACK")
                    return@launch
                }

                val track =
                    com.arturo254.opentune.together.TogetherTrack(
                        id = trackId,
                        title = meta?.title ?: trackId,
                        artists = meta?.artists?.map { it.name }.orEmpty(),
                        durationSec = meta?.duration ?: -1,
                        thumbnailUrl = meta?.thumbnailUrl,
                    )

                val ops =
                    com.arturo254.opentune.together.TogetherGuestPlaybackPlanner.planPlayTrackNow(
                        roomState = joined.roomState,
                        track = track,
                        positionMs = initialStatus.position,
                        playWhenReady = playWhenReady,
                    )

                if (ops.isEmpty()) {
                    showTogetherNotice(getString(R.string.not_allowed), key = "GUEST_PLAYQUEUE_BLOCKED")
                    return@launch
                }

                showTogetherNotice(getString(R.string.together_requesting_song_change), key = "GUEST_PLAYQUEUE_REQUEST")
                ops.forEach { op ->
                    when (op) {
                        is com.arturo254.opentune.together.TogetherGuestOp.Control -> requestTogetherControl(op.action)
                        is com.arturo254.opentune.together.TogetherGuestOp.AddTrack -> requestTogetherAddTrack(op.track, op.mode)
                    }
                }
            }
            return
        }
        if (playWhenReady) {
            playbackLifecycleController
                .prepareForPlaybackStart()
        }
        ensureScopesActive()

        queueController.playQueue(
            queue = queue,
            playWhenReady = playWhenReady,
            clearAuxiliaryQueueState = {
                automixRadioController.resetForNewQueue()
            },
        )
    }

    fun startRadioSeamlessly() {
        val joined =
            togetherSessionState.value as?
                com.arturo254.opentune.together.TogetherSessionState.Joined

        if (
            !isTogetherApplyingRemote() &&
            joined?.role is
                com.arturo254.opentune.together.TogetherRole.Guest
        ) {
            if (
                !joined.roomState.settings
                    .allowGuestsToControlPlayback
            ) {
                showTogetherNotice(
                    getString(R.string.not_allowed),
                    key = "GUEST_RADIO_DISABLED",
                )
                return
            }

            showTogetherNotice(
                getString(R.string.not_allowed),
                key = "GUEST_RADIO_UNSUPPORTED",
            )
            return
        }

        automixRadioController.startRadioSeamlessly()
    }

    fun getAutomixAlbum(albumId: String) {
        automixRadioController.getAutomixAlbum(albumId)
    }

    fun getAutomix(playlistId: String) {
        automixRadioController.getAutomix(playlistId)
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixRadioController.addToQueueAutomix(
            item,
            position,
        )
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixRadioController.playNextAutomix(
            item,
            position,
        )
    }

    fun clearAutomix() {
        automixRadioController.clearAutomix()
    }

    fun onInfiniteQueueDisabled() {
        automixRadioController.onInfiniteQueueDisabled()
    }

    fun onInfiniteQueueEnabled() {
        automixRadioController.onInfiniteQueueEnabled()
    }

    fun stopAndClearPlayback() {
        queueController.clearState(suppressAutoPlayback = true)
        clearAutomix()
        errorRecoveryController.resetForPlaybackStop()
        currentMediaMetadata.value = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        abandonAudioFocus()
        audioEffectsController.closeSession()
    }

    fun playNext(items: List<MediaItem>) {
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
            if (!joined.roomState.settings.allowGuestsToAddTracks) {
                return
            }
            val tracks =
                items.mapNotNull { it.metadata }.map { meta ->
                    com.arturo254.opentune.together.TogetherTrack(
                        id = meta.id,
                        title = meta.title,
                        artists = meta.artists.map { it.name },
                        durationSec = meta.duration,
                        thumbnailUrl = meta.thumbnailUrl,
                    )
                }
            tracks.asReversed().forEach { track ->
                requestTogetherAddTrack(track, com.arturo254.opentune.together.AddTrackMode.PLAY_NEXT)
            }
            return
        }
        queueController.playNext(items)
    }

    fun addToQueue(items: List<MediaItem>) {
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
            if (!joined.roomState.settings.allowGuestsToAddTracks) {
                return
            }
            val tracks =
                items.mapNotNull { it.metadata }.map { meta ->
                    com.arturo254.opentune.together.TogetherTrack(
                        id = meta.id,
                        title = meta.title,
                        artists = meta.artists.map { it.name },
                        durationSec = meta.duration,
                        thumbnailUrl = meta.thumbnailUrl,
                    )
                }
            tracks.forEach { track ->
                requestTogetherAddTrack(track, com.arturo254.opentune.together.AddTrackMode.ADD_TO_QUEUE)
            }
            return
        }
        queueController.addToQueue(items)
    }

    fun startTogetherHost(
        port: Int,
        displayName: String,
        settings:
            com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        togetherController.startTogetherHost(
            port,
            displayName,
            settings,
        )
    }

    fun startTogetherOnlineHost(
        displayName: String,
        settings:
            com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        togetherController.startTogetherOnlineHost(
            displayName,
            settings,
        )
    }

    fun joinTogether(
        rawLink: String,
        displayName: String,
    ) {
        togetherController.joinTogether(
            rawLink,
            displayName,
        )
    }

    fun joinTogetherOnline(
        code: String,
        displayName: String,
    ) {
        togetherController.joinTogetherOnline(
            code,
            displayName,
        )
    }

    fun leaveTogether() {
        togetherController.leaveTogether()
    }

    fun updateTogetherSettings(
        settings:
            com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        togetherController.updateTogetherSettings(
            settings,
        )
    }

    fun approveTogetherParticipant(
        participantId: String,
        approved: Boolean,
    ) {
        togetherController.approveTogetherParticipant(
            participantId,
            approved,
        )
    }

    fun kickTogetherParticipant(
        participantId: String,
        reason: String? = null,
    ) {
        togetherController.kickTogetherParticipant(
            participantId,
            reason,
        )
    }

    fun banTogetherParticipant(
        participantId: String,
        reason: String? = null,
    ) {
        togetherController.banTogetherParticipant(
            participantId,
            reason,
        )
    }

    fun requestTogetherControl(
        action:
            com.arturo254.opentune.together.ControlAction,
    ) {
        togetherController.requestTogetherControl(
            action,
        )
    }

    fun requestTogetherAddTrack(
        track:
            com.arturo254.opentune.together.TogetherTrack,
        mode:
            com.arturo254.opentune.together.AddTrackMode,
    ) {
        togetherController.requestTogetherAddTrack(
            track,
            mode,
        )
    }

    private fun toggleLibrary() {
        libraryActionsController.toggleLibrary()
    }

    fun toggleLike() {
        libraryActionsController.toggleLike()
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    fun previewEqEnabled(enabled: Boolean) {
        audioEffectsController.previewEnabled(enabled)
    }

    fun previewEqBandLevels(levelsMb: List<Int>) {
        audioEffectsController.previewBandLevels(levelsMb)
    }

    fun previewEqOutputGain(
        enabled: Boolean,
        gainMb: Int,
    ) {
        audioEffectsController.previewOutputGain(
            enabled,
            gainMb,
        )
    }

    fun previewEqBassBoost(
        enabled: Boolean,
        strength: Int,
    ) {
        audioEffectsController.previewBassBoost(
            enabled,
            strength,
        )
    }

    fun previewEqVirtualizer(
        enabled: Boolean,
        strength: Int,
    ) {
        audioEffectsController.previewVirtualizer(
            enabled,
            strength,
        )
    }

    fun applyEqFlatPreset() {
        audioEffectsController.applyFlatPreset()
    }

    fun applySystemEqPreset(presetIndex: Int) {
        audioEffectsController.applySystemPreset(presetIndex)
    }

    fun applyEqProfile(
        profileId: String,
        bandLevelsMb: List<Int>,
        outputGainMb: Int,
        bassBoostStrength: Int,
        virtualizerStrength: Int,
    ) {
        audioEffectsController.applyProfile(
            profileId = profileId,
            bandLevelsMb = bandLevelsMb,
            outputGainMb = outputGainMb,
            bassBoostStrength = bassBoostStrength,
            virtualizerStrength = virtualizerStrength,
        )
    }


    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        super.onMediaItemTransition(
            mediaItem,
            reason,
        )
        playbackEventCoordinator
            .onMediaItemTransition(
                mediaItem,
                reason,
            )
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        super.onPlaybackStateChanged(
            playbackState,
        )
        playbackEventCoordinator
            .onPlaybackStateChanged(
                playbackState,
            )
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        playbackEventCoordinator
            .onEvents(events)
    }

    override fun onShuffleModeEnabledChanged(
        shuffleModeEnabled: Boolean,
    ) {
        playbackEventCoordinator
            .onShuffleModeEnabledChanged(
                shuffleModeEnabled,
            )
    }

    override fun onRepeatModeChanged(
        repeatMode: Int,
    ) {
        playbackEventCoordinator
            .onRepeatModeChanged(
                repeatMode,
            )
    }

    override fun onPlayerError(
        error: PlaybackException,
    ) {
        super.onPlayerError(error)
        playbackEventCoordinator
            .onPlayerError(error)
    }

    fun retryCurrentFromFreshStream() {
        errorRecoveryController
            .retryCurrentFromFreshStream()
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        presenceHistoryController
            .onPlaybackStatsReady(
                eventTime,
                playbackStats,
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            scope.launch { togetherController.stop() }
        } catch (_: Exception) {}
        try {
            presenceHistoryController.release()
        } catch (_: Exception) {}
        try {
            connectivityObserver.unregister()
        } catch (_: Exception) {}
        abandonAudioFocus()
        try {
            audioEffectsController.release()
        } catch (_: Exception) {}
        try {
            playbackPersistenceController
                .saveBlockingIfEnabled()
        } catch (_: Exception) {}

        try {
            crossfadeAudio?.release()
            crossfadeAudio = null
        } catch (_: Exception) {}
        try {
            mediaSessionNotificationController.release()
        } catch (_: Exception) {}
        try {
            playbackLifecycleController.release()
        } catch (_: Exception) {}
        try {
            player.removeListener(this)
            player.removeListener(sleepTimer)
            player.release()
        } catch (_: Exception) {}

        scopeJob.cancel()
    }

    override fun onBind(
        intent: Intent?,
    ): android.os.IBinder? {
        playbackLifecycleController.onBound()

        val result =
            super.onBind(intent) ?: binder

        if (
            player.mediaItemCount > 0 &&
            player.currentMediaItem != null
        ) {
            currentMediaMetadata.value =
                player.currentMetadata

            scope.launch {
                delay(50)
                updateNotification()
            }
        }

        return result
    }

    override fun onUnbind(
        intent: Intent?,
    ): Boolean {
        playbackLifecycleController.onUnbound()
        return super.onUnbind(intent)
    }

    override fun onRebind(
        intent: Intent?,
    ) {
        playbackLifecycleController.onRebound()
        super.onRebind(intent)
    }

    override fun onTaskRemoved(
        rootIntent: Intent?,
    ) {
        super.onTaskRemoved(rootIntent)
        playbackLifecycleController
            .onTaskRemoved()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ) = mediaSessionNotificationController.session

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        mediaSessionNotificationController
            .handleWidgetAction(intent?.action)

        super.onStartCommand(
            intent,
            flags,
            startId,
        )

        return START_NOT_STICKY
    }

    private fun updatePlayerWidgets() {
        mediaSessionNotificationController
            .updateWidgets()
    }

    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean,
    ) {
        if (startInForegroundRequired) {
            ensureStartedAsForeground()
        }

        runCatching {
            super.onUpdateNotification(
                session,
                startInForegroundRequired,
            )
        }.onFailure {
            reportException(it)
        }
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        internal fun shouldStopServiceOnTaskRemoved(
            stopMusicOnTaskClearEnabled: Boolean,
            isHostSessionActive: Boolean,
            isPlaybackInactive: Boolean,
        ): Boolean =
            PlaybackLifecyclePolicy
                .shouldStopServiceOnTaskRemoved(
                    stopMusicOnTaskClearEnabled =
                        stopMusicOnTaskClearEnabled,
                    isHostSessionActive =
                        isHostSessionActive,
                    isPlaybackInactive =
                        isPlaybackInactive,
                )

        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val ERROR_CODE_NO_STREAM = 1000001
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MIN_PRESENCE_UPDATE_INTERVAL = 20_000L
    }
}