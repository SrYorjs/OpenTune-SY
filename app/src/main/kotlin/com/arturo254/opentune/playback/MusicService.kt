/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



@file:Suppress("DEPRECATION")

package com.arturo254.opentune.playback

import android.app.PendingIntent
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothClass
import android.content.pm.PackageManager
import android.database.SQLException
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.MediaCodecList
import android.media.audiofx.Virtualizer
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.os.PowerManager
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
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
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
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
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
import com.arturo254.opentune.constants.AutoLoadMoreKey
import com.arturo254.opentune.constants.AutoDownloadOnLikeKey
import com.arturo254.opentune.constants.AutoSkipNextOnErrorKey
import com.arturo254.opentune.constants.AutoStartOnBluetoothKey
import com.arturo254.opentune.constants.InnerTubeCookieKey
import com.arturo254.opentune.constants.DiscordTokenKey
import com.arturo254.opentune.constants.EqualizerBandLevelsMbKey
import com.arturo254.opentune.constants.EqualizerBassBoostEnabledKey
import com.arturo254.opentune.constants.EqualizerBassBoostStrengthKey
import com.arturo254.opentune.constants.EqualizerEnabledKey
import com.arturo254.opentune.constants.EqualizerOutputGainEnabledKey
import com.arturo254.opentune.constants.EqualizerOutputGainMbKey
import com.arturo254.opentune.constants.EqualizerSelectedProfileIdKey
import com.arturo254.opentune.constants.EqualizerVirtualizerEnabledKey
import com.arturo254.opentune.constants.EqualizerVirtualizerStrengthKey
import com.arturo254.opentune.constants.EnableDiscordRPCKey
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.constants.HistoryDuration
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleLike
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleStartRadio
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.arturo254.opentune.constants.MediaSessionConstants.CommandToggleShuffle
import com.arturo254.opentune.constants.PauseListenHistoryKey
import com.arturo254.opentune.constants.PauseOnDeviceMuteKey
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
import com.arturo254.opentune.constants.StopMusicOnTaskClearKey
import com.arturo254.opentune.constants.WakelockKey
import com.arturo254.opentune.constants.YtmSyncKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.Event
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.db.entities.RelatedSongMap
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.AlbumEntity
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
import com.arturo254.opentune.utils.CoilBitmapLoader
import com.arturo254.opentune.utils.DiscordRPC
import com.arturo254.opentune.ui.screens.settings.DiscordPresenceManager
import com.arturo254.opentune.utils.SyncUtils
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.StreamClientUtils
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.enumPreference
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.getAsync
import com.arturo254.opentune.utils.getPresenceIntervalMillis
import com.arturo254.opentune.utils.reportException
import com.arturo254.opentune.utils.NetworkConnectivityObserver
import dagger.hilt.android.AndroidEntryPoint
import com.arturo254.opentune.ui.screens.settings.ListenBrainzManager
import com.arturo254.opentune.constants.ListenBrainzEnabledKey
import com.arturo254.opentune.constants.ListenBrainzTokenKey
import com.arturo254.opentune.lastfm.LastFM
import com.arturo254.opentune.constants.EnableLastFMScrobblingKey
import com.arturo254.opentune.constants.LastFMUseNowPlaying
import com.arturo254.opentune.constants.ScrobbleDelayPercentKey
import com.arturo254.opentune.constants.ScrobbleMinSongDurationKey
import com.arturo254.opentune.constants.ScrobbleDelaySecondsKey
import com.arturo254.opentune.constants.TogetherClientIdKey
import com.arturo254.opentune.widget.PlayerWidgetActions
import com.arturo254.opentune.widget.PlayerWidgetState
import com.arturo254.opentune.widget.PlayerWidgetUpdater
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
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds
import timber.log.Timber
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Notification
import android.os.Build
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
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
    private var pauseOnDeviceMuteEnabled = false
    private var wasAutoPausedByDeviceMute = false
    private var autoStartOnBluetoothEnabled = false
    private var bluetoothReceiverRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakelockEnabled = false

    private var scopeJob = Job()
    private var scope = CoroutineScope(Dispatchers.Main + scopeJob)
    private var ioScope = CoroutineScope(Dispatchers.IO + scopeJob)
    private val binder = MusicBinder()
    private var hasBoundClients = false
    private var idleStopJob: Job? = null

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

    private val persistentStateLock = Any()
    private var lastPresenceToken: String? = null
    @Volatile
    private var lastPresenceUpdateTime = 0L
    @Volatile
    private var lastLoginRecoveryPrompt: Pair<String, Long>? = null

    val currentMediaMetadata = MutableStateFlow<com.arturo254.opentune.models.MediaMetadata?>(null)
    val queueRestoreCompleted = MutableStateFlow(false)
    private val currentSong =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.flowOn(Dispatchers.IO)
            .stateIn(scope, SharingStarted.Lazily, null)
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
    private lateinit var mediaSession: MediaLibrarySession

    private lateinit var audioEffectsController: AudioEffectsController

    val eqCapabilities
        get() = audioEffectsController.eqCapabilities

    private var discordRpc: DiscordRPC? = null
    private var lastDiscordUpdateTime = 0L

    private var scrobbleManager: com.arturo254.opentune.utils.ScrobbleManager? = null

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
    @Volatile
    private var hasCalledStartForeground = false

    val togetherSessionState = MutableStateFlow<com.arturo254.opentune.together.TogetherSessionState>(
        com.arturo254.opentune.together.TogetherSessionState.Idle,
    )
    private var togetherServer: com.arturo254.opentune.together.TogetherServer? = null
    private var togetherOnlineHost: com.arturo254.opentune.together.TogetherOnlineHost? = null
    private var togetherClient: com.arturo254.opentune.together.TogetherClient? = null
    private var togetherBroadcastJob: Job? = null
    private var togetherOnlineConnectJob: Job? = null
    private var togetherClientEventsJob: Job? = null
    private var togetherHeartbeatJob: Job? = null
    private var togetherClock: com.arturo254.opentune.together.TogetherClock? = null
    private var togetherSelfParticipantId: String? = null
    private var togetherLastAppliedQueueHash: String? = null
    private var togetherIsOnlineSession: Boolean = false
    @Volatile
    private var togetherApplyingRemote: Boolean = false
    @Volatile
    private var togetherSuppressEchoUntilElapsedMs: Long = 0L
    @Volatile
    private var togetherLastAppliedRoomStateSentAtElapsedMs: Long = 0L
    @Volatile
    private var togetherLastRemoteAppliedPlayWhenReady: Boolean? = null
    @Volatile
    private var togetherLastRemoteAppliedIndex: Int = -1
    @Volatile
    private var togetherLastSentControlAtElapsedMs: Long = 0L
    @Volatile
    private var togetherLastSentControlAction: com.arturo254.opentune.together.ControlAction? = null
    @Volatile
    private var togetherPendingGuestControl: TogetherPendingGuestControl? = null

    private fun isTogetherApplyingRemote(): Boolean = togetherApplyingRemote
    private val togetherHostId: String = "host"
    private var lastTogetherNoticeAtElapsedMs: Long = 0L
    private var lastTogetherNoticeKey: String? = null

    private data class TogetherPendingGuestControl(
        val desiredIsPlaying: Boolean? = null,
        val desiredIndex: Int? = null,
        val desiredTrackId: String? = null,
        val requestedAtElapsedMs: Long,
        val expiresAtElapsedMs: Long,
    )

    private fun showTogetherNotice(message: String, key: String? = null) {
        val now = android.os.SystemClock.elapsedRealtime()
        val normalizedKey = key ?: message
        if (normalizedKey == lastTogetherNoticeKey && now - lastTogetherNoticeAtElapsedMs < 1200L) return
        lastTogetherNoticeKey = normalizedKey
        lastTogetherNoticeAtElapsedMs = now
        scope.launch(SilentHandler) {
            Toast.makeText(this@MusicService, message, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun getOrCreateTogetherClientId(): String {
        val existing = dataStore.getAsync(TogetherClientIdKey)?.trim().orEmpty()
        if (existing.isNotBlank()) return existing
        val generated = java.util.UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[TogetherClientIdKey] = generated }
        return generated
    }

    private fun ensureStartedAsForeground() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (hasCalledStartForeground) return

        val notification =
            try {
                val contentIntent =
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )

                NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.opentune_monochrome)
                    .setContentTitle(getString(R.string.music_player))
                    .setContentText(getString(R.string.app_name))
                    .setContentIntent(contentIntent)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
            } catch (e: Exception) {
                reportException(e)
                return
            }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            hasCalledStartForeground = true
        } catch (e: Exception) {
            reportException(e)
        }
    }

    private fun promoteToStartedService() {
        runCatching { startService(Intent(this, MusicService::class.java)) }
            .onFailure { reportException(it) }
    }

    private fun cancelIdleStop() {
        idleStopJob?.cancel()
        idleStopJob = null
    }

    private fun stopForegroundAndSelf() {
        cancelIdleStop()
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
        }
        hasCalledStartForeground = false
        stopSelf()
    }

    private fun scheduleStopIfIdle() {
        if (hasBoundClients) return
        val state = player.playbackState
        val keepAlive =
            player.isPlaying ||
                    (player.playWhenReady && (state == Player.STATE_BUFFERING || state == Player.STATE_READY))
        if (keepAlive) {
            cancelIdleStop()
            return
        }
        val togetherIdle = togetherSessionState.value is com.arturo254.opentune.together.TogetherSessionState.Idle
        if (!togetherIdle) {
            cancelIdleStop()
            return
        }

        val delayMs =
            when (state) {
                Player.STATE_READY -> 5 * 60_000L
                Player.STATE_ENDED, Player.STATE_IDLE -> 30_000L
                else -> 60_000L
            }

        cancelIdleStop()
        idleStopJob =
            scope.launch {
                delay(delayMs)
                if (hasBoundClients) return@launch
                val currentState = player.playbackState
                val shouldKeep =
                    player.isPlaying ||
                            (player.playWhenReady && (currentState == Player.STATE_BUFFERING || currentState == Player.STATE_READY))
                if (shouldKeep) return@launch
                if (togetherSessionState.value !is com.arturo254.opentune.together.TogetherSessionState.Idle) return@launch
                stopForegroundAndSelf()
            }
    }

    override fun onCreate() {
        super.onCreate()
        ensureScopesActive()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = getSystemService(NotificationManager::class.java)
                nm?.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.music_player),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        } catch (e: Exception) {
            reportException(e)
        }

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

        audioFocusController =
            AudioFocusController(
                context = this,
                player = player,
                audioFocusVolumeFactor = audioFocusVolumeFactor,
            )
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OpenTune:Playback")
            .also { it.setReferenceCounted(false) }

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleStartRadio = ::toggleStartRadio
            toggleLibrary = ::toggleLibrary
        }
        mediaSession =
            MediaLibrarySession
                .Builder(this, player, mediaLibrarySessionCallback)
                .setSessionActivity(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE,
                    ),
                ).setBitmapLoader(CoilBitmapLoader(this, scope))
                .build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            ).apply {
                setSmallIcon(R.drawable.opentune)
            }
        )

        updateNotification()
        player.repeatMode = REPEAT_MODE_OFF

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())
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

        currentSong.debounce(300).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                ensurePresenceManager()
            } else {
                discordRpc?.closeRPC()
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

        dataStore.data
            .map { it[PauseOnDeviceMuteKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                pauseOnDeviceMuteEnabled = enabled
                if (!enabled) {
                    wasAutoPausedByDeviceMute = false
                } else {
                    handleDeviceMuteStateChanged()
                }
            }

        dataStore.data
            .map { it[AutoStartOnBluetoothKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                autoStartOnBluetoothEnabled = enabled
                if (enabled) {
                    registerBluetoothReceiver()
                } else {
                    unregisterBluetoothReceiver()
                }
            }

        dataStore.data
            .map { it[AudioOffload] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                playbackEngine.updateAudioOffload(player, enabled)
                if (enabled) {
                    val skipSilenceEnabled = dataStore.get(SkipSilenceKey, false)
                    if (skipSilenceEnabled) {
                        dataStore.edit { it[SkipSilenceKey] = false }
                        player.skipSilenceEnabled = false
                    }
                    val crossfadeSeconds = dataStore.get(AudioCrossfadeDurationKey, 0)
                    if (crossfadeSeconds != 0) {
                        dataStore.edit { it[AudioCrossfadeDurationKey] = 0 }
                    }
                }
            }

        dataStore.data
            .map { (it[AudioCrossfadeDurationKey] ?: 0) * 1000 }
            .distinctUntilChanged()
            .collectLatest(scope) {
                crossfadeDurationMs.value = it
            }

        dataStore.data
            .map { it[WakelockKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) { enabled ->
                wakelockEnabled = enabled
                updateWakeLock()
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
                    val metadata = mediaItem.metadata
                    currentMediaMetadata.value = metadata
                    // immediate update when media item transitions to avoid stale presence
                    scope.launch {
                        try {
                            val token = dataStore.get(DiscordTokenKey, "")
                            if (token.isNotBlank() && DiscordPresenceManager.isRunning()) {
                                val mediaId = mediaItem.mediaId
                                val song = if (mediaId != null) withContext(Dispatchers.IO) { database.song(mediaId).first() } else null
                                val finalSong = song ?: metadata?.let { createTransientSongFromMedia(it) }

                                if (canUpdatePresence()) {
                                    DiscordPresenceManager.updateNow(
                                        context = this@MusicService,
                                        token = token,
                                        song = finalSong,
                                        positionMs = 0L,
                                        isPaused = false
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
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
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest(scope) { (key, enabled) ->
                val newRpc =
                    withContext(Dispatchers.IO) {
                        if (!key.isNullOrBlank() && enabled) {
                            runCatching { DiscordRPC(this@MusicService, key) }
                                .onFailure { Timber.tag("MusicService").e(it, "failed to create DiscordRPC client") }
                                .getOrNull()
                        } else {
                            null
                        }
                    }

                try {
                    if (discordRpc?.isRpcRunning() == true) {
                        withContext(Dispatchers.IO) { discordRpc?.closeRPC() }
                    }
                } catch (_: Exception) {}
                discordRpc = newRpc

                if (discordRpc != null) {
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            ensurePresenceManager()
                        }
                    }
                } else {
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
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

        // Last.fm ScrobbleManager setup
        dataStore.data
            .map { it[EnableLastFMScrobblingKey] ?: false }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { enabled ->
                if (enabled && scrobbleManager == null) {
                    val delayPercent = dataStore.get(ScrobbleDelayPercentKey, LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT)
                    val minSongDuration = dataStore.get(ScrobbleMinSongDurationKey, LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION)
                    val delaySeconds = dataStore.get(ScrobbleDelaySecondsKey, LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS)

                    scrobbleManager = com.arturo254.opentune.utils.ScrobbleManager(
                        ioScope,
                        minSongDuration = minSongDuration,
                        scrobbleDelayPercent = delayPercent,
                        scrobbleDelaySeconds = delaySeconds
                    )
                    scrobbleManager?.useNowPlaying = dataStore.get(LastFMUseNowPlaying, false)
                } else if (!enabled && scrobbleManager != null) {
                    scrobbleManager?.destroy()
                    scrobbleManager = null
                }
            }

        dataStore.data
            .map { it[LastFMUseNowPlaying] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                scrobbleManager?.useNowPlaying = it
            }

        dataStore.data
            .map { prefs ->
                Triple(
                    prefs[ScrobbleDelayPercentKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                    prefs[ScrobbleMinSongDurationKey] ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                    prefs[ScrobbleDelaySecondsKey] ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS
                )
            }
            .distinctUntilChanged()
            .collect(scope) { (delayPercent, minSongDuration, delaySeconds) ->
                scrobbleManager?.let {
                    it.scrobbleDelayPercent = delayPercent
                    it.minSongDuration = minSongDuration
                    it.scrobbleDelaySeconds = delaySeconds
                }
            }

        scope.launch(Dispatchers.IO) {
            if (dataStore.get(PersistentQueueKey, true)) {
                readPersistentObject<PersistQueue>(PERSISTENT_QUEUE_FILE)
                    ?.let { persistedQueue ->
                        restorePersistentQueue(persistedQueue)
                    }
                readPersistentObject<PersistQueue>(PERSISTENT_AUTOMIX_FILE)
                    ?.let { persistedAutomix ->
                        val items = persistedAutomix.items.map { it.toMediaItem() }
                        withContext(Dispatchers.Main) {
                            automixRadioController.restoreAutomix(
                                items = items,
                                currentMediaId =
                                    player.currentMetadata
                                        ?.id
                                        ?.trim()
                                        ?.takeIf {
                                            it.isNotBlank()
                                        },
                            )
                        }
                    }

                readPersistentObject<PersistPlayerState>(PERSISTENT_PLAYER_STATE_FILE)
                    ?.let { playerState ->
                        delay(1000)
                        withContext(Dispatchers.Main) {
                            player.repeatMode = playerState.repeatMode
                            player.shuffleModeEnabled = playerState.shuffleModeEnabled
                            playerVolume.value = playerState.volume

                            if (player.mediaItemCount > 0) {
                                val index =
                                    if (playerState.currentMediaItemIndex in 0 until player.mediaItemCount) {
                                        playerState.currentMediaItemIndex
                                    } else {
                                        player.currentMediaItemIndex.coerceIn(0, player.mediaItemCount - 1)
                                    }
                                player.seekTo(index, playerState.currentPosition)
                            }

                            currentMediaMetadata.value = player.currentMetadata
                            updateNotification()
                        }
                    }
            }
            withContext(Dispatchers.Main) {
                queueRestoreCompleted.value = true
            }
        }

        // Save queue periodically to prevent queue loss from crash or force kill
        // Save queue periodically to prevent queue loss from crash or force kill
        scope.launch {
            while (isActive) {
                val interval = if (player.isPlaying) 10.seconds else 30.seconds
                delay(interval)
                val shouldSave = withContext(Dispatchers.IO) { dataStore.get(PersistentQueueKey, true) }
                if (shouldSave) {
                    saveQueueToDisk()
                }
            }
        }
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

    private suspend fun restorePersistentQueue(persistedQueue: PersistQueue) {
        queueController.restorePersistentQueue(persistedQueue)
    }

    private fun ensurePresenceManager() {
        if (DiscordPresenceManager.isRunning() && lastPresenceToken != null) return

        // Launch in scope to avoid blocking
        scope.launch {
            // Don't start if Discord RPC is disabled in settings
            if (!dataStore.get(EnableDiscordRPCKey, true)) {
                if (DiscordPresenceManager.isRunning()) {
                    Timber.tag("MusicService").d("Discord RPC disabled → stopping presence manager")
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                    lastPresenceToken = null
                }
                return@launch
            }

            val key: String = dataStore.get(DiscordTokenKey, "")
            if (key.isNullOrBlank()) {
                if (DiscordPresenceManager.isRunning()) {
                    Timber.tag("MusicService").d("No Discord token → stopping presence manager")
                    try { DiscordPresenceManager.stop() } catch (_: Exception) {}
                    lastPresenceToken = null
                }
                return@launch
            }

            if (DiscordPresenceManager.isRunning() && lastPresenceToken == key) {
                // try {
                //     if (DiscordPresenceManager.restart()) {
                //         Timber.tag("MusicService").d("Presence manager restarted with same token")
                //     }
                // } catch (ex: Exception) {
                //     Timber.tag("MusicService").e(ex, "Failed to restart presence manager")
                // }
                return@launch
            }

            try {
                DiscordPresenceManager.stop()
                DiscordPresenceManager.start(
                    context = this@MusicService,
                    token = key,
                    songProvider = { player.currentMetadata?.let { createTransientSongFromMedia(it) } ?: currentSong.value },
                    positionProvider = { player.currentPosition },
                    isPausedProvider = { !player.isPlaying },
                    intervalProvider = { getPresenceIntervalMillis(this@MusicService) }
                )
                Timber.tag("MusicService").d("Presence manager started with token=$key")
                lastPresenceToken = key
            } catch (ex: Exception) {
                Timber.tag("MusicService").e(ex, "Failed to start presence manager")
            }
        }
    }

    private fun canUpdatePresence(): Boolean {
        val now = System.currentTimeMillis()
        synchronized(this) {
            return if (now - lastPresenceUpdateTime > MIN_PRESENCE_UPDATE_INTERVAL) {
                lastPresenceUpdateTime = now
                true
            } else false
        }
    }

    private fun requestAudioFocus(): Boolean =
        audioFocusController.requestFocus()

    private fun abandonAudioFocus() {
        audioFocusController.abandonFocus()
    }

    fun hasAudioFocusForPlayback(): Boolean =
        audioFocusController.hasFocus()

    private fun isDeviceMutedNow(): Boolean {
        return player.isDeviceMuted || player.deviceVolume <= 0
    }

    private fun isTogetherGuestSession(): Boolean {
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        return joined?.role is com.arturo254.opentune.together.TogetherRole.Guest
    }

    private fun handleDeviceMuteStateChanged() {
        if (!pauseOnDeviceMuteEnabled || isTogetherGuestSession()) {
            wasAutoPausedByDeviceMute = false
            return
        }

        if (isDeviceMutedNow()) {
            val canPauseNow =
                player.currentMediaItem != null &&
                        player.playWhenReady &&
                        player.playbackState != Player.STATE_IDLE &&
                        player.playbackState != Player.STATE_ENDED

            if (canPauseNow) {
                player.pause()
                wasAutoPausedByDeviceMute = true
            }
            return
        }

        if (!wasAutoPausedByDeviceMute) return

        wasAutoPausedByDeviceMute = false
        val canResumeNow =
            player.currentMediaItem != null &&
                    player.playbackState != Player.STATE_IDLE &&
                    player.playbackState != Player.STATE_ENDED
        if (canResumeNow) {
            player.play()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return
            if (!autoStartOnBluetoothEnabled) return

            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return

            val isAudioDevice = try {
                val majorClass = device.bluetoothClass?.majorDeviceClass
                majorClass == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                        majorClass == BluetoothClass.Device.Major.WEARABLE
            } catch (_: SecurityException) {
                true
            }

            if (!isAudioDevice) return

            scope.launch {
                delay(1500)
                handleBluetoothAutoStart()
            }
        }
    }

    private fun handleBluetoothAutoStart() {
        if (isTogetherGuestSession()) return

        if (player.currentMediaItem != null &&
            player.playbackState != Player.STATE_IDLE &&
            player.playbackState != Player.STATE_ENDED
        ) {
            if (!player.playWhenReady) {
                player.play()
            }
            return
        }

        if (player.mediaItemCount > 0) {
            player.prepare()
            player.play()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerBluetoothReceiver() {
        if (bluetoothReceiverRegistered) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return

        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(bluetoothReceiver, filter)
        }
        bluetoothReceiverRegistered = true
    }

    private fun unregisterBluetoothReceiver() {
        if (!bluetoothReceiverRegistered) return
        try {
            unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
        bluetoothReceiverRegistered = false
    }

    private fun updateNotification() {
        try {
            val customLayout = listOf(
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            },
                        ),
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> R.string.repeat_mode_off
                            },
                        ),
                    ).setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> R.drawable.repeat
                        },
                    ).setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton
                    .Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(R.string.start_radio))
                    .setIconResId(R.drawable.radio)
                    .setSessionCommand(CommandToggleStartRadio)
                    .setEnabled(currentSong.value != null)
                    .build(),
            )
            mediaSession.setCustomLayout(customLayout)
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun refreshPlaybackNotification() {
        updateNotification()
        runCatching { super.onUpdateNotification(mediaSession, player.isPlaying) }
            .onFailure { reportException(it) }
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
            cancelIdleStop()
            promoteToStartedService()
            ensureStartedAsForeground()
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

    private fun applyCurrentFirstShuffleOrder() {
        queueController.applyCurrentFirstShuffleOrder()
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
        settings: com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        ensureScopesActive()
        scope.launch(SilentHandler) {
            togetherSessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = false

            val localIp = getLocalIpv4Address()
            val sessionId = java.util.UUID.randomUUID().toString()
            val sessionKey = java.util.UUID.randomUUID().toString()
            val joinInfo =
                com.arturo254.opentune.together.TogetherJoinInfo(
                    host = localIp ?: "127.0.0.1",
                    port = port,
                    sessionId = sessionId,
                    sessionKey = sessionKey,
                )
            val joinLink = com.arturo254.opentune.together.TogetherLink.encode(joinInfo)

            val server =
                com.arturo254.opentune.together.TogetherServer(
                    scope = ioScope,
                    sessionId = sessionId,
                    sessionKey = sessionKey,
                    hostDisplayName = displayName.trim().ifBlank { getString(R.string.app_name) },
                    initialSettings = settings,
                )

            server.onEvent = { event ->
                ioScope.launch(SilentHandler) {
                    handleTogetherHostEvent(event) { server.currentSettings() }
                }
            }

            server.start(port)
            togetherServer = server

            scope.launch(SilentHandler) {
                togetherSessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Hosting(
                        sessionId = sessionId,
                        joinLink = joinLink,
                        localAddressHint = localIp,
                        port = port,
                        settings = settings,
                        roomState = null,
                    )
            }

            togetherBroadcastJob =
                ioScope.launch(SilentHandler) {
                    while (togetherServer === server) {
                        val state = buildTogetherRoomState(sessionId = sessionId, hostId = togetherHostId)
                        server.broadcastRoomState(state)
                        scope.launch(SilentHandler) {
                            val hosting = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Hosting
                            if (hosting?.sessionId == sessionId) {
                                togetherSessionState.value =
                                    hosting.copy(
                                        settings = server.currentSettings(),
                                        roomState = state.copy(
                                            participants = server.currentParticipants(),
                                            settings = server.currentSettings(),
                                        ),
                                    )
                            }
                        }
                        kotlinx.coroutines.delay(750)
                    }
                }
        }
    }

    private fun togetherOnlineErrorMessage(t: Throwable): String {
        if (t is com.arturo254.opentune.together.TogetherOnlineApiException) {
            val code = t.statusCode
            return when {
                code == 404 -> getString(R.string.together_session_not_found)
                code != null && code in 500..599 -> getString(R.string.together_server_error)
                else -> t.message ?: getString(R.string.network_unavailable)
            }
        }
        val root = generateSequence(t) { it.cause }.lastOrNull() ?: t
        return when (root) {
            is UnknownHostException -> getString(R.string.together_server_unreachable)
            is ConnectException -> getString(R.string.together_server_unreachable)
            is SocketTimeoutException -> getString(R.string.together_connection_timed_out)
            is javax.net.ssl.SSLHandshakeException -> getString(R.string.together_server_unreachable)
            else -> getString(R.string.network_unavailable)
        }
    }

    fun startTogetherOnlineHost(
        displayName: String,
        settings: com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        ensureScopesActive()
        scope.launch(SilentHandler) {
            togetherSessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = true

            val baseUrl = com.arturo254.opentune.together.TogetherOnlineEndpoint.baseUrlOrNull(dataStore)
            if (baseUrl == null) {
                scope.launch(SilentHandler) {
                    togetherSessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = getString(R.string.together_online_not_configured),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val togetherToken = com.arturo254.opentune.BuildConfig.TOGETHER_BEARER_TOKEN.trim().takeIf { it.isNotBlank() }
            if (togetherToken == null) {
                scope.launch(SilentHandler) {
                    togetherSessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = getString(R.string.together_token_missing),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val api = com.arturo254.opentune.together.TogetherOnlineApi(baseUrl = baseUrl, bearerToken = togetherToken)
            val hostName = displayName.trim().ifBlank { getString(R.string.app_name) }

            val created =
                runCatching {
                    api.createSession(
                        hostDisplayName = hostName,
                        settings = settings,
                    )
                }.getOrElse { t ->
                    scope.launch(SilentHandler) {
                        togetherSessionState.value =
                            com.arturo254.opentune.together.TogetherSessionState.Error(
                                message = togetherOnlineErrorMessage(t),
                                recoverable = true,
                            )
                    }
                    reportException(t)
                    return@launch
                }

            val onlineHost =
                com.arturo254.opentune.together.TogetherOnlineHost(
                    externalScope = ioScope,
                    sessionId = created.sessionId,
                    sessionKey = created.hostKey,
                    hostId = togetherHostId,
                    hostDisplayName = hostName,
                    initialSettings = created.settings,
                    clientId = getOrCreateTogetherClientId(),
                    bearerToken = togetherToken,
                )

            onlineHost.onEvent = { event ->
                ioScope.launch(SilentHandler) {
                    handleTogetherHostEvent(event) { onlineHost.currentSettings() }
                }
            }

            togetherOnlineHost = onlineHost

            scope.launch(SilentHandler) {
                togetherSessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.HostingOnline(
                        sessionId = created.sessionId,
                        code = created.code,
                        settings = created.settings,
                        roomState = null,
                    )
            }

            val wsUrl =
                com.arturo254.opentune.together.TogetherOnlineEndpoint.onlineWebSocketUrlOrNull(
                    rawWsUrl = created.wsUrl,
                    baseUrl = baseUrl,
                )
            if (wsUrl == null) {
                scope.launch(SilentHandler) {
                    togetherSessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = "Connection failed: Invalid server websocket URL",
                            recoverable = true,
                        )
                }
                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                return@launch
            }

            togetherOnlineConnectJob?.cancel()
            togetherOnlineConnectJob =
                ioScope.launch(SilentHandler) {
                    onlineHost.connect(wsUrl)
                }

            togetherBroadcastJob =
                ioScope.launch(SilentHandler) {
                    while (togetherOnlineHost === onlineHost) {
                        val state =
                            buildTogetherRoomState(
                                sessionId = created.sessionId,
                                hostId = togetherHostId,
                            )
                        onlineHost.broadcastRoomState(state)
                        scope.launch(SilentHandler) {
                            val hosting =
                                togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.HostingOnline
                            if (hosting?.sessionId == created.sessionId) {
                                val currentSettings = onlineHost.currentSettings()
                                togetherSessionState.value =
                                    hosting.copy(
                                        settings = currentSettings,
                                        roomState =
                                            state.copy(
                                                participants = onlineHost.currentParticipants(),
                                                settings = currentSettings,
                                            ),
                                    )
                            }
                        }
                        kotlinx.coroutines.delay(750)
                    }
                }
        }
    }

    fun joinTogether(
        rawLink: String,
        displayName: String,
    ) {
        ensureScopesActive()
        val joinInfo = com.arturo254.opentune.together.TogetherLink.decode(rawLink)
        if (joinInfo == null) {
            scope.launch(SilentHandler) {
                togetherSessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Error(
                        message = getString(R.string.invalid_link),
                        recoverable = true,
                    )
            }
            return
        }

        scope.launch(SilentHandler) {
            togetherSessionState.value = com.arturo254.opentune.together.TogetherSessionState.Joining(joinInfo.toDeepLink())
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = false
            val client =
                com.arturo254.opentune.together.TogetherClient(
                    ioScope,
                    clientId = getOrCreateTogetherClientId(),
                )
            togetherClient = client
            togetherClock = com.arturo254.opentune.together.TogetherClock()
            togetherSelfParticipantId = null
            togetherLastAppliedQueueHash = null

            togetherClientEventsJob?.cancel()
            togetherClientEventsJob =
                ioScope.launch(SilentHandler) {
                    client.events.collect { event ->
                        when (event) {
                            is com.arturo254.opentune.together.TogetherClientEvent.Welcome -> {
                                togetherSelfParticipantId = event.welcome.participantId
                                scope.launch(SilentHandler) {
                                    val state = togetherSessionState.value
                                    if (state is com.arturo254.opentune.together.TogetherSessionState.Joining) {
                                        val selfName = displayName.trim().ifBlank { getString(R.string.together_role_guest) }
                                        val initial =
                                            com.arturo254.opentune.together.TogetherRoomState(
                                                sessionId = joinInfo.sessionId,
                                                hostId = togetherHostId,
                                                participants =
                                                    listOf(
                                                        com.arturo254.opentune.together.TogetherParticipant(
                                                            id = event.welcome.participantId,
                                                            name = selfName,
                                                            isHost = false,
                                                            isPending = event.welcome.isPending,
                                                            isConnected = true,
                                                        ),
                                                    ),
                                                settings = event.welcome.settings,
                                                queue = emptyList(),
                                                queueHash = "",
                                                currentIndex = 0,
                                                isPlaying = false,
                                                positionMs = 0L,
                                                repeatMode = 0,
                                                shuffleEnabled = false,
                                                sentAtElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                                            )
                                        togetherSessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Joined(
                                                role = com.arturo254.opentune.together.TogetherRole.Guest,
                                                sessionId = joinInfo.sessionId,
                                                selfParticipantId = event.welcome.participantId,
                                                roomState = initial,
                                            )
                                    }
                                }
                                startTogetherHeartbeat(joinInfo.sessionId, client)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.RoomState -> {
                                applyRemoteRoomState(event.state)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.JoinDecision -> {
                                if (!event.decision.approved) {
                                    scope.launch(SilentHandler) {
                                        togetherSessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Error(
                                                message = getString(R.string.not_allowed),
                                                recoverable = true,
                                            )
                                    }
                                    ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.ServerIssue -> {
                                Timber.tag("Together").w("server issue (lan) code=${event.code.orEmpty()} message=${event.message}")
                                when (event.code) {
                                    "GUEST_CONTROL_DISABLED" -> {
                                        showTogetherNotice(event.message, key = "GUEST_CONTROL_DISABLED")
                                        val joined =
                                            togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
                                        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
                                            togetherPendingGuestControl = null
                                            togetherLastSentControlAction = null
                                            scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                                        }
                                    }

                                    "GUEST_ADD_DISABLED" -> {
                                        showTogetherNotice(event.message, key = "GUEST_ADD_DISABLED")
                                    }

                                    "HOST_OFFLINE" -> {
                                        showTogetherNotice(event.message, key = "HOST_OFFLINE")
                                    }

                                    else -> {
                                        scope.launch(SilentHandler) {
                                            togetherSessionState.value =
                                                com.arturo254.opentune.together.TogetherSessionState.Error(
                                                    message = event.message,
                                                    recoverable = true,
                                                )
                                        }
                                        ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                    }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.HeartbeatPong -> {
                                val clock = togetherClock ?: return@collect
                                clock.onPong(
                                    sentAtElapsedMs = event.pong.clientElapsedRealtimeMs,
                                    receivedAtElapsedMs = event.receivedAtElapsedRealtimeMs,
                                    serverElapsedMs = event.pong.serverElapsedRealtimeMs,
                                )
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.Error -> {
                                scope.launch(SilentHandler) {
                                    togetherSessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message = event.message,
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }

                            com.arturo254.opentune.together.TogetherClientEvent.Disconnected -> {
                                val current = togetherSessionState.value
                                if (current is com.arturo254.opentune.together.TogetherSessionState.Idle) return@collect
                                scope.launch(SilentHandler) {
                                    val currentState = togetherSessionState.value
                                    togetherSessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message =
                                                if (currentState is com.arturo254.opentune.together.TogetherSessionState.Joined &&
                                                    currentState.role is com.arturo254.opentune.together.TogetherRole.Guest
                                                ) {
                                                    getString(R.string.together_host_left_session)
                                                } else {
                                                    getString(R.string.network_unavailable)
                                                },
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }
                        }
                    }
                }

            client.connect(joinInfo, displayName.trim().ifBlank { getString(R.string.together_role_guest) })
        }
    }

    fun joinTogetherOnline(
        code: String,
        displayName: String,
    ) {
        ensureScopesActive()
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            scope.launch(SilentHandler) {
                togetherSessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Error(
                        message = getString(R.string.invalid_code),
                        recoverable = true,
                    )
            }
            return
        }

        scope.launch(SilentHandler) {
            togetherSessionState.value = com.arturo254.opentune.together.TogetherSessionState.JoiningOnline(trimmedCode)
        }

        ioScope.launch(SilentHandler) {
            stopTogetherInternal()
            togetherIsOnlineSession = true

            val baseUrl = com.arturo254.opentune.together.TogetherOnlineEndpoint.baseUrlOrNull(dataStore)
            if (baseUrl == null) {
                scope.launch(SilentHandler) {
                    togetherSessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = getString(R.string.together_online_not_configured),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val togetherToken = com.arturo254.opentune.BuildConfig.TOGETHER_BEARER_TOKEN.trim().takeIf { it.isNotBlank() }
            if (togetherToken == null) {
                scope.launch(SilentHandler) {
                    togetherSessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = getString(R.string.together_token_missing),
                            recoverable = true,
                        )
                }
                return@launch
            }

            val api = com.arturo254.opentune.together.TogetherOnlineApi(baseUrl = baseUrl, bearerToken = togetherToken)
            val resolved =
                runCatching { api.resolveCode(trimmedCode) }
                    .getOrElse { t ->
                        scope.launch(SilentHandler) {
                            togetherSessionState.value =
                                com.arturo254.opentune.together.TogetherSessionState.Error(
                                    message = togetherOnlineErrorMessage(t),
                                    recoverable = true,
                                )
                        }
                        reportException(t)
                        return@launch
                    }

            val client =
                com.arturo254.opentune.together.TogetherClient(
                    ioScope,
                    clientId = getOrCreateTogetherClientId(),
                    bearerToken = togetherToken,
                )
            togetherClient = client
            togetherClock = com.arturo254.opentune.together.TogetherClock()
            togetherSelfParticipantId = null
            togetherLastAppliedQueueHash = null

            togetherClientEventsJob?.cancel()
            togetherClientEventsJob =
                ioScope.launch(SilentHandler) {
                    client.events.collect { event ->
                        when (event) {
                            is com.arturo254.opentune.together.TogetherClientEvent.Welcome -> {
                                togetherSelfParticipantId = event.welcome.participantId
                                scope.launch(SilentHandler) {
                                    val state = togetherSessionState.value
                                    if (state is com.arturo254.opentune.together.TogetherSessionState.JoiningOnline) {
                                        val selfName = displayName.trim().ifBlank { getString(R.string.together_role_guest) }
                                        val initial =
                                            com.arturo254.opentune.together.TogetherRoomState(
                                                sessionId = resolved.sessionId,
                                                hostId = togetherHostId,
                                                participants =
                                                    listOf(
                                                        com.arturo254.opentune.together.TogetherParticipant(
                                                            id = event.welcome.participantId,
                                                            name = selfName,
                                                            isHost = false,
                                                            isPending = event.welcome.isPending,
                                                            isConnected = true,
                                                        ),
                                                    ),
                                                settings = event.welcome.settings,
                                                queue = emptyList(),
                                                queueHash = "",
                                                currentIndex = 0,
                                                isPlaying = false,
                                                positionMs = 0L,
                                                repeatMode = 0,
                                                shuffleEnabled = false,
                                                sentAtElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
                                            )
                                        togetherSessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Joined(
                                                role = com.arturo254.opentune.together.TogetherRole.Guest,
                                                sessionId = resolved.sessionId,
                                                selfParticipantId = event.welcome.participantId,
                                                roomState = initial,
                                            )
                                    }
                                }
                                startTogetherHeartbeat(resolved.sessionId, client)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.RoomState -> {
                                applyRemoteRoomState(event.state)
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.JoinDecision -> {
                                if (!event.decision.approved) {
                                    scope.launch(SilentHandler) {
                                        togetherSessionState.value =
                                            com.arturo254.opentune.together.TogetherSessionState.Error(
                                                message = getString(R.string.not_allowed),
                                                recoverable = true,
                                            )
                                    }
                                    ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.ServerIssue -> {
                                Timber.tag("Together").w("server issue (online) code=${event.code.orEmpty()} message=${event.message}")
                                when (event.code) {
                                    "GUEST_CONTROL_DISABLED" -> {
                                        showTogetherNotice(event.message, key = "GUEST_CONTROL_DISABLED")
                                        val joined =
                                            togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
                                        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
                                            togetherPendingGuestControl = null
                                            togetherLastSentControlAction = null
                                            scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                                        }
                                    }

                                    "GUEST_ADD_DISABLED" -> {
                                        showTogetherNotice(event.message, key = "GUEST_ADD_DISABLED")
                                    }

                                    "HOST_OFFLINE" -> {
                                        showTogetherNotice(event.message, key = "HOST_OFFLINE")
                                    }

                                    else -> {
                                        scope.launch(SilentHandler) {
                                            togetherSessionState.value =
                                                com.arturo254.opentune.together.TogetherSessionState.Error(
                                                    message = event.message,
                                                    recoverable = true,
                                                )
                                        }
                                        ioScope.launch(SilentHandler) { stopTogetherInternal() }
                                    }
                                }
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.HeartbeatPong -> {
                                val clock = togetherClock ?: return@collect
                                clock.onPong(
                                    sentAtElapsedMs = event.pong.clientElapsedRealtimeMs,
                                    receivedAtElapsedMs = event.receivedAtElapsedRealtimeMs,
                                    serverElapsedMs = event.pong.serverElapsedRealtimeMs,
                                )
                            }

                            is com.arturo254.opentune.together.TogetherClientEvent.Error -> {
                                scope.launch(SilentHandler) {
                                    togetherSessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message = event.message,
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }

                            com.arturo254.opentune.together.TogetherClientEvent.Disconnected -> {
                                val current = togetherSessionState.value
                                if (current is com.arturo254.opentune.together.TogetherSessionState.Idle) return@collect
                                scope.launch(SilentHandler) {
                                    val currentState = togetherSessionState.value
                                    togetherSessionState.value =
                                        com.arturo254.opentune.together.TogetherSessionState.Error(
                                            message =
                                                if (currentState is com.arturo254.opentune.together.TogetherSessionState.Joined &&
                                                    currentState.role is com.arturo254.opentune.together.TogetherRole.Guest
                                                ) {
                                                    getString(R.string.together_host_left_session)
                                                } else {
                                                    getString(R.string.network_unavailable)
                                                },
                                            recoverable = true,
                                        )
                                }
                                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                            }
                        }
                    }
                }

            val wsUrl =
                com.arturo254.opentune.together.TogetherOnlineEndpoint.onlineWebSocketUrlOrNull(
                    rawWsUrl = resolved.wsUrl,
                    baseUrl = baseUrl,
                )
            if (wsUrl == null) {
                scope.launch(SilentHandler) {
                    togetherSessionState.value =
                        com.arturo254.opentune.together.TogetherSessionState.Error(
                            message = "Connection failed: Invalid server websocket URL",
                            recoverable = true,
                        )
                }
                ioScope.launch(SilentHandler) { stopTogetherInternal() }
                return@launch
            }

            client.connect(
                wsUrl = wsUrl,
                sessionId = resolved.sessionId,
                sessionKey = resolved.guestKey,
                displayName = displayName.trim().ifBlank { getString(R.string.together_role_guest) },
            )
        }
    }

    fun leaveTogether() {
        ensureScopesActive()
        scope.launch(SilentHandler) {
            togetherSessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle
        }
        ioScope.launch(SilentHandler) { stopTogetherInternal() }
    }

    fun updateTogetherSettings(settings: com.arturo254.opentune.together.TogetherRoomSettings) {
        val server = togetherServer
        val onlineHost = togetherOnlineHost
        if (server == null && onlineHost == null) return
        ioScope.launch(SilentHandler) {
            server?.updateSettings(settings)
            onlineHost?.updateSettings(settings)
        }
    }

    fun approveTogetherParticipant(participantId: String, approved: Boolean) {
        val server = togetherServer
        val onlineHost = togetherOnlineHost
        if (server == null && onlineHost == null) return
        ioScope.launch(SilentHandler) {
            server?.approveParticipant(participantId, approved)
            onlineHost?.approveParticipant(participantId, approved)
        }
    }

    fun kickTogetherParticipant(participantId: String, reason: String? = null) {
        val onlineHost = togetherOnlineHost ?: return
        ioScope.launch(SilentHandler) {
            onlineHost.kickParticipant(participantId, reason)
        }
    }

    fun banTogetherParticipant(participantId: String, reason: String? = null) {
        val onlineHost = togetherOnlineHost ?: return
        ioScope.launch(SilentHandler) {
            onlineHost.banParticipant(participantId, reason)
        }
    }

    fun requestTogetherControl(action: com.arturo254.opentune.together.ControlAction) {
        val client =
            togetherClient ?: run {
                showTogetherNotice(getString(R.string.network_unavailable), key = "TOGETHER_CLIENT_MISSING")
                return
            }
        val state = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined ?: return
        if (state.role !is com.arturo254.opentune.together.TogetherRole.Guest) return
        if (!state.roomState.settings.allowGuestsToControlPlayback) {
            Timber.tag("Together").i("control blocked locally (disabled) action=${action::class.java.simpleName}")
            showTogetherNotice(getString(R.string.not_allowed), key = "GUEST_CONTROL_DISABLED_LOCAL")
            return
        }
        val now = android.os.SystemClock.elapsedRealtime()
        val lastAction = togetherLastSentControlAction
        val lastAt = togetherLastSentControlAtElapsedMs
        if (lastAction == action && now - lastAt < 350L) return
        togetherLastSentControlAction = action
        togetherLastSentControlAtElapsedMs = now

        val timeout = if (togetherIsOnlineSession) 5000L else 2000L
        togetherPendingGuestControl =
            when (action) {
                com.arturo254.opentune.together.ControlAction.Play ->
                    TogetherPendingGuestControl(desiredIsPlaying = true, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
                com.arturo254.opentune.together.ControlAction.Pause ->
                    TogetherPendingGuestControl(desiredIsPlaying = false, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
                is com.arturo254.opentune.together.ControlAction.SeekToIndex ->
                    TogetherPendingGuestControl(desiredIndex = action.index.coerceAtLeast(0), requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
                is com.arturo254.opentune.together.ControlAction.SeekToTrack ->
                    TogetherPendingGuestControl(
                        desiredTrackId = action.trackId.trim().ifBlank { null },
                        requestedAtElapsedMs = now,
                        expiresAtElapsedMs = now + timeout,
                    )
                else -> togetherPendingGuestControl
            }
        client.requestControl(state.sessionId, action)
    }

    fun requestTogetherAddTrack(
        track: com.arturo254.opentune.together.TogetherTrack,
        mode: com.arturo254.opentune.together.AddTrackMode,
    ) {
        val client = togetherClient ?: return
        val state = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined ?: return
        if (state.role !is com.arturo254.opentune.together.TogetherRole.Guest) return
        if (!state.roomState.settings.allowGuestsToAddTracks) {
            Timber.tag("Together").i("add blocked locally (disabled) mode=$mode trackId=${track.id}")
            showTogetherNotice(getString(R.string.not_allowed), key = "GUEST_ADD_DISABLED_LOCAL")
            return
        }
        client.requestAddTrack(state.sessionId, track, mode)
    }

    private suspend fun handleTogetherHostEvent(
        event: com.arturo254.opentune.together.TogetherServerEvent,
        currentSettings: suspend () -> com.arturo254.opentune.together.TogetherRoomSettings,
    ) {
        when (event) {
            is com.arturo254.opentune.together.TogetherServerEvent.ControlRequested -> {
                val settings = currentSettings()
                if (!settings.allowGuestsToControlPlayback) return
                applyHostControl(event.request.action)
            }

            is com.arturo254.opentune.together.TogetherServerEvent.AddTrackRequested -> {
                val settings = currentSettings()
                if (!settings.allowGuestsToAddTracks) return
                applyHostAddTrack(event.request.track, event.request.mode)
            }

            is com.arturo254.opentune.together.TogetherServerEvent.Error -> {
                val current = togetherSessionState.value
                if (current is com.arturo254.opentune.together.TogetherSessionState.Idle) return
                togetherSessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Error(
                        message = event.message,
                        recoverable = true,
                    )
                ioScope.launch(SilentHandler) { stopTogetherInternal() }
            }

            else -> Unit
        }
    }

    private suspend fun applyHostControl(action: com.arturo254.opentune.together.ControlAction) {
        withContext(Dispatchers.Main) {
            when (action) {
                com.arturo254.opentune.together.ControlAction.Play -> {
                    if (!player.playWhenReady) {
                        player.prepare()
                        player.playWhenReady = true
                    }
                }

                com.arturo254.opentune.together.ControlAction.Pause -> {
                    if (player.playWhenReady) {
                        player.playWhenReady = false
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SeekTo -> {
                    player.seekTo(action.positionMs.coerceAtLeast(0L))
                    player.prepare()
                }

                com.arturo254.opentune.together.ControlAction.SkipNext -> {
                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                        player.playWhenReady = true
                    }
                }

                com.arturo254.opentune.together.ControlAction.SkipPrevious -> {
                    if (player.hasPreviousMediaItem()) {
                        player.seekToPrevious()
                        player.playWhenReady = true
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SeekToTrack -> {
                    val trackId = action.trackId.trim()
                    if (trackId.isNotBlank()) {
                        val idx =
                            player.mediaItems.indexOfFirst {
                                val metaId = it.metadata?.id
                                it.mediaId == trackId || metaId == trackId
                            }
                        if (idx >= 0 && idx < player.mediaItemCount) {
                            player.seekTo(idx, action.positionMs.coerceAtLeast(0L))
                            player.prepare()
                        }
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SeekToIndex -> {
                    val idx = action.index.coerceAtLeast(0)
                    if (idx < player.mediaItemCount) {
                        player.seekTo(idx, action.positionMs.coerceAtLeast(0L))
                        player.prepare()
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SetRepeatMode -> {
                    if (player.repeatMode != action.repeatMode) {
                        player.repeatMode = action.repeatMode
                    }
                }

                is com.arturo254.opentune.together.ControlAction.SetShuffleEnabled -> {
                    if (player.shuffleModeEnabled != action.shuffleEnabled) {
                        player.shuffleModeEnabled = action.shuffleEnabled
                    }
                }
            }
        }
    }

    private suspend fun applyHostAddTrack(
        track: com.arturo254.opentune.together.TogetherTrack,
        mode: com.arturo254.opentune.together.AddTrackMode,
    ) {
        val mediaItem = track.toMediaMetadata().toMediaItem()
        withContext(Dispatchers.Main) {
            when (mode) {
                com.arturo254.opentune.together.AddTrackMode.PLAY_NEXT -> playNext(listOf(mediaItem))
                com.arturo254.opentune.together.AddTrackMode.ADD_TO_QUEUE -> addToQueue(listOf(mediaItem))
            }
        }
    }

    private suspend fun buildTogetherRoomState(
        sessionId: String,
        hostId: String,
    ): com.arturo254.opentune.together.TogetherRoomState {
        return withContext(Dispatchers.Main) {
            val tracks =
                player.mediaItems.mapNotNull { it.metadata }.map { meta ->
                    com.arturo254.opentune.together.TogetherTrack(
                        id = meta.id,
                        title = meta.title,
                        artists = meta.artists.map { it.name },
                        durationSec = meta.duration,
                        thumbnailUrl = meta.thumbnailUrl,
                    )
                }

            val queueHash = com.arturo254.opentune.utils.md5(tracks.joinToString(separator = "|") { it.id })

            com.arturo254.opentune.together.TogetherRoomState(
                sessionId = sessionId,
                hostId = hostId,
                settings = com.arturo254.opentune.together.TogetherRoomSettings(),
                participants = emptyList(),
                queue = tracks,
                queueHash = queueHash,
                currentIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                repeatMode = player.repeatMode,
                shuffleEnabled = player.shuffleModeEnabled,
                sentAtElapsedRealtimeMs = android.os.SystemClock.elapsedRealtime(),
            )
        }
    }

    private suspend fun applyRemoteRoomState(state: com.arturo254.opentune.together.TogetherRoomState) {
        val pid = togetherSelfParticipantId ?: return
        val now = android.os.SystemClock.elapsedRealtime()

        val pending = togetherPendingGuestControl
        if (pending != null) {
            val currentTrackId = state.queue.getOrNull(state.currentIndex.coerceAtLeast(0))?.id
            val mismatch =
                (pending.desiredIsPlaying != null && state.isPlaying != pending.desiredIsPlaying) ||
                        (pending.desiredIndex != null && state.currentIndex != pending.desiredIndex) ||
                        (pending.desiredTrackId != null && currentTrackId != pending.desiredTrackId)
            if (now >= pending.expiresAtElapsedMs) {
                if ((pending.desiredIndex != null || pending.desiredTrackId != null) &&
                    now - pending.requestedAtElapsedMs >= 1200L &&
                    mismatch
                ) {
                    showTogetherNotice(getString(R.string.together_song_change_failed), key = "GUEST_SEEK_TIMEOUT")
                }
                togetherPendingGuestControl = null
            } else {
                if (mismatch) return
                togetherPendingGuestControl = null
            }
        }

        val lastSentAt = togetherLastAppliedRoomStateSentAtElapsedMs
        val sentAt = state.sentAtElapsedRealtimeMs
        if (sentAt > 0L && lastSentAt > 0L && sentAt <= lastSentAt) return

        val offset = if (togetherIsOnlineSession) 0L else (togetherClock?.snapshot()?.estimatedOffsetMs ?: 0L)
        val correctedSentAt = sentAt + offset
        val estimatedOnlineLatency = if (togetherIsOnlineSession) 1200L else 0L
        val delta = if (togetherIsOnlineSession) estimatedOnlineLatency else (now - correctedSentAt).coerceAtLeast(0L)
        val targetPos =
            if (state.isPlaying) (state.positionMs + delta).coerceAtLeast(0L) else state.positionMs.coerceAtLeast(0L)

        withContext(Dispatchers.Main) {
            togetherApplyingRemote = true
            togetherSuppressEchoUntilElapsedMs = android.os.SystemClock.elapsedRealtime() + 450L
            try {
                val desiredItems = state.queue.map { it.toMediaMetadata().toMediaItem() }
                val desiredIds = state.queue.map { it.id }
                val desiredHash = state.queueHash
                val localIds = player.mediaItems.mapNotNull { it.metadata?.id ?: it.mediaId }.filter { it.isNotBlank() }
                val localHash = if (localIds.isEmpty()) "" else com.arturo254.opentune.utils.md5(localIds.joinToString(separator = "|"))
                val needsRebuild =
                    desiredItems.isNotEmpty() &&
                            (
                                    (desiredHash.isNotBlank() && desiredHash != localHash) ||
                                            (desiredHash.isBlank() && desiredIds != localIds)
                                    )

                if (desiredItems.isNotEmpty() && needsRebuild) {
                    togetherLastAppliedQueueHash = desiredHash.ifBlank { localHash }
                    val startIndex = state.currentIndex.coerceIn(0, desiredItems.lastIndex)
                    suppressAutoPlayback = false
                    currentQueue =
                        com.arturo254.opentune.playback.queues.ListQueue(
                            title = getString(R.string.music_player),
                            items = desiredItems,
                            startIndex = startIndex,
                            position = targetPos,
                        )
                    queueTitle = null
                    player.setMediaItems(desiredItems, startIndex, targetPos)
                    player.prepare()
                    player.repeatMode = state.repeatMode
                    player.shuffleModeEnabled = state.shuffleEnabled
                    player.playWhenReady = state.isPlaying
                    togetherLastRemoteAppliedIndex = startIndex
                } else {
                    val index = state.currentIndex.coerceAtLeast(0)
                    val indexChanged = player.mediaItemCount > 0 && index != player.currentMediaItemIndex
                    val stateChanged =
                        player.repeatMode != state.repeatMode ||
                                player.shuffleModeEnabled != state.shuffleEnabled ||
                                player.playWhenReady != state.isPlaying

                    if (indexChanged) {
                        player.seekTo(index.coerceAtMost(player.mediaItemCount - 1), targetPos)
                        player.prepare()
                        player.playWhenReady = state.isPlaying
                    } else if (stateChanged) {
                        if (player.repeatMode != state.repeatMode) player.repeatMode = state.repeatMode
                        if (player.shuffleModeEnabled != state.shuffleEnabled) player.shuffleModeEnabled = state.shuffleEnabled
                        if (player.playWhenReady != state.isPlaying) {
                            player.playWhenReady = state.isPlaying
                            val drift = kotlin.math.abs(player.currentPosition - targetPos)
                            if (drift > 100) {
                                player.seekTo(targetPos)
                                player.prepare()
                            }
                        }
                    } else {
                        val drift = kotlin.math.abs(player.currentPosition - targetPos)
                        val seekThreshold = if (togetherIsOnlineSession) 4000L else 2000L
                        val threshold = if (state.isPlaying) seekThreshold else 200L

                        if (drift > threshold) {
                            player.seekTo(targetPos)
                            player.prepare()
                        }
                    }
                    togetherLastRemoteAppliedIndex = index
                }
                togetherLastRemoteAppliedPlayWhenReady = state.isPlaying
                togetherLastAppliedRoomStateSentAtElapsedMs = sentAt

                togetherSessionState.value =
                    com.arturo254.opentune.together.TogetherSessionState.Joined(
                        role = com.arturo254.opentune.together.TogetherRole.Guest,
                        sessionId = state.sessionId,
                        selfParticipantId = pid,
                        roomState = state,
                    )
            } finally {
                togetherApplyingRemote = false
            }
        }
    }

    private fun startTogetherHeartbeat(sessionId: String, client: com.arturo254.opentune.together.TogetherClient) {
        togetherHeartbeatJob?.cancel()
        togetherHeartbeatJob =
            ioScope.launch(SilentHandler) {
                var pingId = 0L
                while (togetherClient === client) {
                    val now = android.os.SystemClock.elapsedRealtime()
                    client.sendHeartbeat(sessionId = sessionId, pingId = pingId++, clientElapsedRealtimeMs = now)
                    kotlinx.coroutines.delay(2000)
                }
            }
    }

    private suspend fun stopTogetherInternal() {
        togetherBroadcastJob?.cancel()
        togetherBroadcastJob = null

        togetherOnlineConnectJob?.cancel()
        togetherOnlineConnectJob = null

        togetherClientEventsJob?.cancel()
        togetherClientEventsJob = null

        togetherHeartbeatJob?.cancel()
        togetherHeartbeatJob = null

        togetherClock = null
        togetherSelfParticipantId = null
        togetherLastAppliedQueueHash = null
        togetherIsOnlineSession = false
        togetherApplyingRemote = false
        togetherSuppressEchoUntilElapsedMs = 0L
        togetherLastAppliedRoomStateSentAtElapsedMs = 0L
        togetherLastRemoteAppliedPlayWhenReady = null
        togetherLastRemoteAppliedIndex = -1
        togetherLastSentControlAtElapsedMs = 0L
        togetherLastSentControlAction = null
        togetherPendingGuestControl = null

        try {
            togetherClient?.disconnect()
        } catch (_: Exception) {}
        togetherClient = null

        try {
            togetherOnlineHost?.disconnect()
        } catch (_: Exception) {}
        togetherOnlineHost = null

        try {
            togetherServer?.stop()
        } catch (_: Exception) {}
        togetherServer = null
    }

    private fun com.arturo254.opentune.together.TogetherTrack.toMediaMetadata(): com.arturo254.opentune.models.MediaMetadata {
        return com.arturo254.opentune.models.MediaMetadata(
            id = id,
            title = title,
            artists = artists.map { name -> com.arturo254.opentune.models.MediaMetadata.Artist(id = null, name = name) },
            duration = durationSec,
            thumbnailUrl = thumbnailUrl,
        )
    }

    private fun getLocalIpv4Address(): String? {
        return runCatching {
            java.net.NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<java.net.Inet4Address>()
                .map { it.hostAddress }
                .firstOrNull { it.isNotBlank() && it != "127.0.0.1" }
        }.getOrNull()
    }

    private fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                val song = it.song.toggleLike()
                update(song)
                syncUtils.likeSong(song)

                // Check if auto-download on like is enabled and the song is now liked
                if (dataStore.get(AutoDownloadOnLikeKey, false) && song.liked) {
                    // Trigger download for the liked song
                    val downloadRequest = androidx.media3.exoplayer.offline.DownloadRequest
                        .Builder(song.id, song.id.toUri())
                        .setCustomCacheKey(song.id)
                        .setData(song.title.toByteArray())
                        .build()
                    androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
                        this@MusicService,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false
                    )
                }
            }
        }
    }

    fun toggleStartRadio() {
        startRadioSeamlessly()
    }

    fun applyEqFlatPreset() {
        audioEffectsController.applyFlatPreset()
    }

    fun applySystemEqPreset(presetIndex: Int) {
        audioEffectsController.applySystemPreset(presetIndex)
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        super.onMediaItemTransition(mediaItem, reason)

        errorRecoveryController.onMediaItemTransition(
            mediaItem?.mediaId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: player.currentMediaItem?.mediaId,
        )

        crossfadeAudio?.onMediaItemTransition(mediaItem, reason)

        // Pre-load lyrics for upcoming songs in queue
        val currentIndex = player.currentMediaItemIndex
        // Convert media items to MediaMetadata for lyrics pre-loading
        val queue = player.mediaItems.mapNotNull { it.metadata }
        if (queue.isNotEmpty()) {
            lyricsPreloadManager?.onSongChanged(currentIndex, queue)
        }

        streamResolver.scheduleNextStreamWarmup()

        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest &&
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
        ) {
            if (!joined.roomState.settings.allowGuestsToControlPlayback) {
                scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                return
            }
            val now = android.os.SystemClock.elapsedRealtime()
            val index = player.currentMediaItemIndex.coerceAtLeast(0)
            val isEcho =
                isTogetherApplyingRemote() ||
                        (now < togetherSuppressEchoUntilElapsedMs && togetherLastRemoteAppliedIndex == index)
            if (!isEcho) {
                val trackId = (mediaItem?.metadata ?: player.currentMetadata)?.id?.trim().orEmpty()
                requestTogetherControl(
                    if (trackId.isBlank()) {
                        com.arturo254.opentune.together.ControlAction.SeekToIndex(
                            index = index,
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                        )
                    } else {
                        com.arturo254.opentune.together.ControlAction.SeekToTrack(
                            trackId = trackId,
                            positionMs = player.currentPosition.coerceAtLeast(0L),
                        )
                    },
                )
            }
        }

        val timelineEmpty = player.currentTimeline.isEmpty || player.mediaItemCount == 0 || player.currentMediaItem == null
        currentMediaMetadata.value = if (timelineEmpty) null else (mediaItem?.metadata ?: player.currentMetadata)

        scrobbleManager?.onSongStop()

        automixRadioController.onMediaItemTransition(
            mediaItem = mediaItem,
            reason = reason,
            timelineEmpty = timelineEmpty,
            queueHasNextPage = queueController.hasNextPage(),
        )

        // Auto-load more from queue if available
        if (!suppressAutoPlayback &&
            !timelineEmpty &&
            dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            queueController.hasNextPage() &&
            player.repeatMode == REPEAT_MODE_OFF
        ) {
            scope.launch(SilentHandler) {
                val mediaItems =
                    queueController.nextPage()
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                } else {
                    scope.launch { discordRpc?.stopActivity() }
                }
            }
        }

        if (player.playWhenReady && player.playbackState == Player.STATE_READY) {
            scrobbleManager?.onSongStart(player.currentMetadata, duration = player.duration)
        }

        scope.launch {
            val shouldSave = withContext(Dispatchers.IO) { dataStore.get(PersistentQueueKey, true) }
            if (shouldSave) {
                saveQueueToDisk()
            }
        }
        ensurePresenceManager()
        updatePlayerWidgets()
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        updatePlayerWidgets()

        errorRecoveryController.onPlaybackStateChanged(playbackState)

        scope.launch {
            val shouldSave = withContext(Dispatchers.IO) { dataStore.get(PersistentQueueKey, true) }
            if (shouldSave) {
                saveQueueToDisk()
            }
        }

        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            crossfadeAudio?.stop(resetMainFade = true)
            scrobbleManager?.onSongStop()
        }

        automixRadioController.onPlaybackStateChanged(
            playbackState,
        )

        ensurePresenceManager()
        scope.launch {
            try {
                val token = withContext(Dispatchers.IO) { dataStore.get(DiscordTokenKey, "") }
                if (token.isNotBlank() && DiscordPresenceManager.isRunning()) {
                    // Obtain the freshest Song from DB using current media item id to avoid stale currentSong.value
                    val mediaId = player.currentMediaItem?.mediaId
                    val song = if (mediaId != null) withContext(Dispatchers.IO) { database.song(mediaId).first() } else null
                    val finalSong = song ?: player.currentMetadata?.let { createTransientSongFromMedia(it) }

                    if (canUpdatePresence()) {
                        val success = withContext(Dispatchers.IO) {
                            DiscordPresenceManager.updateNow(
                                context = this@MusicService,
                                token = token,
                                song = finalSong,
                                positionMs = player.currentPosition,
                                isPaused = !player.playWhenReady,
                            )
                        }
                        if (!success) {
                            Timber.tag("MusicService").w("immediate presence update returned false — attempting restart")
                            if (DiscordPresenceManager.isRunning()) {
                                try {
                                    if (DiscordPresenceManager.restart()) {
                                        Timber.tag("MusicService").d("presence manager restarted after failed update")
                                    }
                                } catch (ex: Exception) {
                                    Timber.tag("MusicService").e(ex, "restart after failed presence update threw")
                                }
                            }
                        }

                        try {
                            val lbEnabled = withContext(Dispatchers.IO) { dataStore.get(ListenBrainzEnabledKey, false) }
                            val lbToken = withContext(Dispatchers.IO) { dataStore.get(ListenBrainzTokenKey, "") }
                            if (lbEnabled && !lbToken.isNullOrBlank()) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        ListenBrainzManager.submitPlayingNow(this@MusicService, lbToken, finalSong, player.currentPosition)
                                    } catch (ie: Exception) {
                                        Timber.tag("MusicService").v(ie, "ListenBrainz playing_now submit failed")
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                Timber.tag("MusicService").v(e, "immediate presence update failed")
            }
        }
    }


    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_MEDIA_METADATA_CHANGED)) {
            if (crossfadeAudio?.isCrossfading() != true) {
                currentMediaMetadata.value = player.currentMetadata
            }
        }
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest &&
            events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
        ) {
            if (!joined.roomState.settings.allowGuestsToControlPlayback) {
                scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
            } else {
                val now = android.os.SystemClock.elapsedRealtime()
                val playWhenReady = this.player.playWhenReady
                val isEcho =
                    isTogetherApplyingRemote() ||
                            (now < togetherSuppressEchoUntilElapsedMs &&
                                    togetherLastRemoteAppliedPlayWhenReady != null &&
                                    togetherLastRemoteAppliedPlayWhenReady == playWhenReady)
                if (!isEcho) {
                    val action =
                        if (playWhenReady) {
                            com.arturo254.opentune.together.ControlAction.Play
                        } else {
                            com.arturo254.opentune.together.ControlAction.Pause
                        }
                    requestTogetherControl(action)
                }
            }
        }
        if (events.contains(Player.EVENT_DEVICE_VOLUME_CHANGED)) {
            handleDeviceMuteStateChanged()
        }
        if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) && isDeviceMutedNow() && this.player.playWhenReady) {
            handleDeviceMuteStateChanged()
        }
        if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) &&
            (this.player.playbackState == Player.STATE_IDLE || this.player.playbackState == Player.STATE_ENDED)
        ) {
            wasAutoPausedByDeviceMute = false
        }
        if (events.contains(Player.EVENT_AUDIO_SESSION_ID)) {
            audioEffectsController.onAudioSessionIdChanged(
                this.player.audioSessionId,
            )
        }
        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            val playbackState = player.playbackState
            val keepAudioEffectSessionOpen =
                playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY
            if (player.playWhenReady && keepAudioEffectSessionOpen) {
                requestAudioFocus()
            }
            if (keepAudioEffectSessionOpen) {
                audioEffectsController.openSession()
            } else {
                audioEffectsController.closeSession()
            }
            updateWakeLock()
            if (player.playWhenReady && keepAudioEffectSessionOpen) {
                cancelIdleStop()
                promoteToStartedService()
                ensureStartedAsForeground()
            } else {
                scheduleStopIfIdle()
            }
        }

        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            if (crossfadeAudio?.isCrossfading() != true) {
                currentMediaMetadata.value = player.currentMetadata
            }
            // immediate update when media item transitions to avoid stale presence
            scope.launch {
                try {
                    val token = dataStore.get(DiscordTokenKey, "")
                    if (token.isNotBlank() && DiscordPresenceManager.isRunning()) {
                        val mediaId = player.currentMediaItem?.mediaId
                        val song = if (mediaId != null) withContext(Dispatchers.IO) { database.song(mediaId).first() } else null
                        val finalSong = song ?: player.currentMetadata?.let { createTransientSongFromMedia(it) }

                        if (canUpdatePresence()) {
                            val success = DiscordPresenceManager.updateNow(
                                context = this@MusicService,
                                token = token,
                                song = finalSong,
                                positionMs = player.currentPosition,
                                isPaused = !player.isPlaying,
                            )
                            if (!success) {
                                Timber.tag("MusicService").w("transition immediate presence update failed — attempting restart")
                                try { DiscordPresenceManager.stop(); DiscordPresenceManager.start(this@MusicService, dataStore.get(DiscordTokenKey, ""), { song }, { player.currentPosition }, { !player.isPlaying }, { getPresenceIntervalMillis(this@MusicService) }) } catch (_: Exception) {}
                            }
                            try {
                                val lbEnabled = dataStore.get(ListenBrainzEnabledKey, false)
                                val lbToken = dataStore.get(ListenBrainzTokenKey, "")
                                if (lbEnabled && !lbToken.isNullOrBlank()) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            ListenBrainzManager.submitPlayingNow(this@MusicService, lbToken, finalSong, player.currentPosition)
                                        } catch (ie: Exception) {
                                            Timber.tag("MusicService").v(ie, "ListenBrainz playing_now submit failed on transition")
                                        }
                                    }
                                }

                                // Last.fm now playing - handled by ScrobbleManager
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("MusicService").v(e, "immediate presence update failed on transition")
                }
            }
        }

        // Also handle immediate update for play state and media item transition events explicitly
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                currentMediaMetadata.value = player.currentMetadata
            }
            // Capture player state on Main thread
            val currentMediaId = player.currentMediaItem?.mediaId
            val currentMetadata = player.currentMetadata
            val currentPosition = player.currentPosition
            val isPlaying = player.isPlaying

            scope.launch {
                try {
                    val token = withContext(Dispatchers.IO) { dataStore.get(DiscordTokenKey, "") }
                    if (token.isNotBlank() && DiscordPresenceManager.isRunning()) {
                        val song = if (currentMediaId != null) withContext(Dispatchers.IO) { database.song(currentMediaId).first() } else null
                        val finalSong = song ?: currentMetadata?.let { createTransientSongFromMedia(it) }

                        if (canUpdatePresence()) {
                            // Run update on IO if possible, assuming updateNow is thread-safe or handles its own threading correctly
                            // If updateNow touches Views, this might break. Assuming it's network/logic.
                            val success = withContext(Dispatchers.IO) {
                                DiscordPresenceManager.updateNow(
                                    context = this@MusicService,
                                    token = token,
                                    song = finalSong,
                                    positionMs = currentPosition,
                                    isPaused = !isPlaying,
                                )
                            }
                            if (!success) {
                                Timber.tag("MusicService").w("isPlaying/mediaTransition immediate presence update failed — restarting manager")
                                if (DiscordPresenceManager.isRunning()) {
                                    try { DiscordPresenceManager.stop(); DiscordPresenceManager.restart() } catch (_: Exception) {}
                                }
                            }
                            try {
                                val lbEnabled = withContext(Dispatchers.IO) { dataStore.get(ListenBrainzEnabledKey, false) }
                                val lbToken = withContext(Dispatchers.IO) { dataStore.get(ListenBrainzTokenKey, "") }
                                if (lbEnabled && !lbToken.isNullOrBlank()) {
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            ListenBrainzManager.submitPlayingNow(this@MusicService, lbToken, finalSong, currentPosition)
                                        } catch (ie: Exception) {
                                            Timber.tag("MusicService").v(ie, "ListenBrainz playing_now submit failed for isPlaying/mediaTransition")
                                        }
                                    }
                                }

                                // Last.fm now playing - handled by ScrobbleManager
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("MusicService").v(e, "immediate presence update failed for isPlaying/mediaTransition")
                }
            }
        }

        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            ensurePresenceManager()
            // Scrobble: Track play/pause state
            scrobbleManager?.onPlayerStateChanged(player.isPlaying, player.currentMetadata, duration = player.duration)
            updatePlayerWidgets()
        } else if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            ensurePresenceManager()
            updatePlayerWidgets()
        } else {
            ensurePresenceManager()
        }
    }


    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
            if (!isTogetherApplyingRemote()) {
                if (!joined.roomState.settings.allowGuestsToControlPlayback) {
                    scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                    return
                }
                requestTogetherControl(
                    com.arturo254.opentune.together.ControlAction.SetShuffleEnabled(
                        shuffleEnabled = shuffleModeEnabled,
                    ),
                )
            }
            return
        }
        if (shuffleModeEnabled) {
            applyCurrentFirstShuffleOrder()
        }

        // Save state when shuffle mode changes - must be on Main thread to access player
        scope.launch {
            if (dataStore.get(PersistentQueueKey, true)) {
                saveQueueToDisk()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        val joined = togetherSessionState.value as? com.arturo254.opentune.together.TogetherSessionState.Joined
        if (joined?.role is com.arturo254.opentune.together.TogetherRole.Guest) {
            if (!isTogetherApplyingRemote()) {
                if (!joined.roomState.settings.allowGuestsToControlPlayback) {
                    scope.launch(SilentHandler) { applyRemoteRoomState(joined.roomState) }
                    return
                }
                requestTogetherControl(
                    com.arturo254.opentune.together.ControlAction.SetRepeatMode(
                        repeatMode = repeatMode,
                    ),
                )
            }
            return
        }
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }

        // Save state when repeat mode changes - must be on Main thread to access player
        scope.launch {
            if (dataStore.get(PersistentQueueKey, true)) {
                saveQueueToDisk()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        errorRecoveryController.handlePlayerError(error)
    }

    fun retryCurrentFromFreshStream() {
        errorRecoveryController.retryCurrentFromFreshStream()
    }

    private fun updateWakeLock() {
        val wl = wakeLock ?: return
        val shouldHold = wakelockEnabled && player.isPlaying
        if (shouldHold && !wl.isHeld) {
            wl.acquire()
        } else if (!shouldHold && wl.isHeld) {
            wl.release()
        }
    }

    override fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem

        if (playbackStats.totalPlayTimeMs >= (
                    dataStore[HistoryDuration]?.times(1000f)
                        ?: 30000f
                    ) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        ),
                    )
                } catch (_: SQLException) {
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val song = database.song(mediaItem.mediaId).first()
                        ?: return@launch

                    val lbEnabled = dataStore.get(ListenBrainzEnabledKey, false)
                    val lbToken = dataStore.get(ListenBrainzTokenKey, "")
                    if (lbEnabled && !lbToken.isNullOrBlank()) {
                        val endMs = System.currentTimeMillis()
                        val startMs = endMs - playbackStats.totalPlayTimeMs
                        try {
                            ListenBrainzManager.submitFinished(this@MusicService, lbToken, song, startMs, endMs)
                        } catch (ie: Exception) {
                            Timber.tag("MusicService").v(ie, "ListenBrainz finished submit failed")
                        }
                    }
                } catch (_: Exception) {
                }
            }

            CoroutineScope(Dispatchers.IO).launch {
                runCatching { registerRemoteListeningHistory(mediaItem.mediaId) }
            }
        }
    }

    private suspend fun registerRemoteListeningHistory(mediaId: String) {
        if (!isRemoteHistorySyncAllowed()) return

        val attemptedUrls = LinkedHashSet<String>()

        suspend fun registerTrackingUrl(url: String): Boolean {
            attemptedUrls += url
            return YouTube.registerPlayback(playbackTracking = url)
                .onFailure {
                    reportException(it)
                }.isSuccess
        }

        val cachedPlaybackUrl = database.format(mediaId).first()?.playbackUrl
        val cachedSuccess = cachedPlaybackUrl?.let { registerTrackingUrl(it) } == true
        if (cachedSuccess) return

        val playbackTracking =
            YTPlayerUtils.playerResponseForMetadata(mediaId, null)
                .getOrNull()
                ?.playbackTracking
                ?: return

        for (
        trackingUrl in listOfNotNull(
            playbackTracking.videostatsPlaybackUrl?.baseUrl,
            playbackTracking.videostatsWatchtimeUrl?.baseUrl,
        )
        ) {
            if (trackingUrl.isBlank() || trackingUrl in attemptedUrls) continue
            registerTrackingUrl(trackingUrl)
        }
    }

    private suspend fun isRemoteHistorySyncAllowed(): Boolean {
        if (!dataStore.getAsync(YtmSyncKey, true)) return false
        val cookie = dataStore.getAsync(InnerTubeCookieKey, "")
        return cookie.isNotBlank() && cookie.contains("SAPISID")
    }

    // Create a transient Song object from current Player MediaMetadata when the DB doesn't have it.
    private fun createTransientSongFromMedia(media: com.arturo254.opentune.models.MediaMetadata): Song {
        val songEntity = SongEntity(
            id = media.id,
            title = media.title,
            duration = media.duration,
            thumbnailUrl = media.thumbnailUrl,
            albumId = media.album?.id,
            albumName = media.album?.title,
            explicit = media.explicit,
        )

        val artists = media.artists.map { artist ->
            ArtistEntity(
                id = artist.id ?: "LA_unknown_${artist.name}",
                name = artist.name,
                thumbnailUrl = if (!artist.thumbnailUrl.isNullOrBlank()) artist.thumbnailUrl else media.thumbnailUrl,
            )
        }

        val album = media.album?.let { alb ->
            AlbumEntity(
                id = alb.id,
                playlistId = null,
                title = alb.title,
                year = null,
                thumbnailUrl = media.thumbnailUrl,
                themeColor = null,
                songCount = 1,
                duration = media.duration,
            )
        }

        return Song(
            song = songEntity,
            artists = artists,
            album = album,
            format = null,
        )
    }

    private inline fun <reified T> readPersistentObject(fileName: String): T? {
        val persistentFile = filesDir.resolve(fileName)
        if (!persistentFile.exists() || !persistentFile.isFile) return null

        return synchronized(persistentStateLock) {
            runCatching {
                persistentFile.inputStream().use { fis ->
                    ObjectInputStream(fis).use { input ->
                        input.readObject() as? T
                    }
                }
            }.onFailure {
                Timber.tag("MusicService").w(it, "Failed to read persistent file: $fileName")
                runCatching { persistentFile.delete() }
            }.getOrNull()
        }
    }

    private fun writePersistentObject(fileName: String, payload: Serializable) {
        val persistentFile = filesDir.resolve(fileName)
        val tempFile = filesDir.resolve("$fileName.tmp")

        synchronized(persistentStateLock) {
            runCatching {
                FileOutputStream(tempFile).use { fos ->
                    ObjectOutputStream(fos).use { output ->
                        output.writeObject(payload)
                        output.flush()
                    }
                    fos.fd.sync()
                }

                if (persistentFile.exists() && !persistentFile.delete()) {
                    error("Could not replace $fileName")
                }
                if (!tempFile.renameTo(persistentFile)) {
                    error("Could not atomically move $fileName")
                }
            }.onFailure {
                runCatching { tempFile.delete() }
                reportException(it)
            }
        }
    }

    private fun MediaItem.toPersistableMetadata(): com.arturo254.opentune.models.MediaMetadata? {
        val tagged = metadata
        if (tagged != null) return tagged

        val id =
            mediaId.trim().ifBlank {
                localConfiguration?.uri?.toString()?.trim().orEmpty()
            }.takeIf { it.isNotBlank() } ?: return null

        val title =
            mediaMetadata.title?.toString()?.trim().takeIf { !it.isNullOrBlank() }
                ?: id

        val artistText =
            mediaMetadata.artist?.toString()?.trim().takeIf { !it.isNullOrBlank() }
                ?: mediaMetadata.subtitle?.toString()?.trim().takeIf { !it.isNullOrBlank() }

        val artists =
            artistText
                ?.split(",")
                ?.mapNotNull { it.trim().takeIf(String::isNotBlank) }
                ?.map { name -> com.arturo254.opentune.models.MediaMetadata.Artist(id = null, name = name) }
                .orEmpty()

        val thumbnailUrl = mediaMetadata.artworkUri?.toString()
        val albumTitle = mediaMetadata.albumTitle?.toString()?.trim().takeIf { !it.isNullOrBlank() }
        val album =
            albumTitle?.let { titleValue ->
                com.arturo254.opentune.models.MediaMetadata.Album(id = titleValue, title = titleValue)
            }

        return com.arturo254.opentune.models.MediaMetadata(
            id = id,
            title = title,
            artists = artists,
            duration = -1,
            thumbnailUrl = thumbnailUrl,
            album = album,
        )
    }

    private suspend fun saveQueueToDisk() {
        val mediaItemsSnapshot = player.mediaItems.mapNotNull { it.toPersistableMetadata() }
        if (mediaItemsSnapshot.isEmpty()) return

        val currentMediaItemIndex = player.currentMediaItemIndex
        val currentPosition = player.currentPosition
        val automixSnapshot = automixItems.value.mapNotNull { it.metadata }
        val playWhenReady = player.playWhenReady
        val repeatMode = player.repeatMode
        val shuffleModeEnabled = player.shuffleModeEnabled
        val volume = playerVolume.value
        val playbackState = player.playbackState

        withContext(Dispatchers.IO) {
            // Save current queue with proper type information
            val persistQueue =
                queueController.toPersistQueue(
                    items = mediaItemsSnapshot,
                    mediaItemIndex = currentMediaItemIndex,
                    position = currentPosition,
                )

            val persistAutomix =
                PersistQueue(
                    title = "automix",
                    items = automixSnapshot,
                    mediaItemIndex = 0,
                    position = 0,
                )

            // Save player state
            val persistPlayerState = PersistPlayerState(
                playWhenReady = playWhenReady,
                repeatMode = repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
                volume = volume,
                currentPosition = currentPosition,
                currentMediaItemIndex = currentMediaItemIndex, // Redundant but part of data class
                playbackState = playbackState
            )

            writePersistentObject(PERSISTENT_QUEUE_FILE, persistQueue)
            writePersistentObject(PERSISTENT_AUTOMIX_FILE, persistAutomix)
            writePersistentObject(PERSISTENT_PLAYER_STATE_FILE, persistPlayerState)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterBluetoothReceiver()
        try {
            scope.launch { stopTogetherInternal() }
        } catch (_: Exception) {}
        try {
            DiscordPresenceManager.stop()
        } catch (_: Exception) {}
        try {
            discordRpc?.closeRPC()
        } catch (_: Exception) {}
        discordRpc = null
        try {
            connectivityObserver.unregister()
        } catch (_: Exception) {}
        abandonAudioFocus()
        try {
            audioEffectsController.release()
        } catch (_: Exception) {}
        try {
            if (dataStore.get(PersistentQueueKey, true) && player.mediaItemCount > 0) {
                val mediaItemsSnapshot = player.mediaItems.mapNotNull { it.metadata }
                val currentMediaItemIndex = player.currentMediaItemIndex
                val currentPosition = player.currentPosition
                val automixSnapshot = automixItems.value.mapNotNull { it.metadata }
                val repeatMode = player.repeatMode
                val shuffleModeEnabled = player.shuffleModeEnabled
                val volume = playerVolume.value
                val playbackState = player.playbackState
                val playWhenReady = player.playWhenReady
                runBlocking(Dispatchers.IO) {
                    val persistQueue =
                        queueController.toPersistQueue(
                            items = mediaItemsSnapshot,
                            mediaItemIndex = currentMediaItemIndex,
                            position = currentPosition,
                        )
                    val persistAutomix = PersistQueue(
                        title = "automix",
                        items = automixSnapshot,
                        mediaItemIndex = 0,
                        position = 0,
                    )
                    val persistPlayerState = PersistPlayerState(
                        playWhenReady = playWhenReady,
                        repeatMode = repeatMode,
                        shuffleModeEnabled = shuffleModeEnabled,
                        volume = volume,
                        currentPosition = currentPosition,
                        currentMediaItemIndex = currentMediaItemIndex,
                        playbackState = playbackState
                    )

                    writePersistentObject(PERSISTENT_QUEUE_FILE, persistQueue)
                    writePersistentObject(PERSISTENT_AUTOMIX_FILE, persistAutomix)
                    writePersistentObject(PERSISTENT_PLAYER_STATE_FILE, persistPlayerState)
                }
            }
        } catch (_: Exception) {}
        try {
            mediaSession.release()
        } catch (_: Exception) {}
        try {
            crossfadeAudio?.release()
            crossfadeAudio = null
        } catch (_: Exception) {}
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {}
        try {
            player.removeListener(this)
            player.removeListener(sleepTimer)
            player.release()
        } catch (_: Exception) {}
        scopeJob.cancel()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? {
        hasBoundClients = true
        cancelIdleStop()
        val result = super.onBind(intent) ?: binder
        if (player.mediaItemCount > 0 && player.currentMediaItem != null) {
            currentMediaMetadata.value = player.currentMetadata
            scope.launch {
                delay(50)
                updateNotification()
            }
        }
        return result
    }

    override fun onUnbind(intent: Intent?): Boolean {
        hasBoundClients = false
        scheduleStopIfIdle()
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        hasBoundClients = true
        cancelIdleStop()
        super.onRebind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // When the user clears the app from Recents, ensure we clear Discord rich presence
        try {
            scope.launch {
                try { discordRpc?.stopActivity() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        try {
            if (discordRpc?.isRpcRunning() == true) {
                try { discordRpc?.closeRPC() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        discordRpc = null
        try { DiscordPresenceManager.stop() } catch (_: Exception) {}
        lastPresenceToken = null

        val stopMusicOnTaskClearEnabled = dataStore.get(StopMusicOnTaskClearKey, false)

        try {
            val state = togetherSessionState.value
            val isHostSessionActive =
                state is com.arturo254.opentune.together.TogetherSessionState.Hosting ||
                        state is com.arturo254.opentune.together.TogetherSessionState.HostingOnline ||
                        (state is com.arturo254.opentune.together.TogetherSessionState.Joined &&
                                state.role is com.arturo254.opentune.together.TogetherRole.Host)

            val isPlaybackInactive = player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0

            if (shouldStopServiceOnTaskRemoved(stopMusicOnTaskClearEnabled, isHostSessionActive, isPlaybackInactive)) {
                if (isHostSessionActive && isPlaybackInactive) {
                    runCatching { scope.launch { stopTogetherInternal() } }
                    runCatching { togetherSessionState.value = com.arturo254.opentune.together.TogetherSessionState.Idle }
                    stopSelf()
                    return
                }

                if (stopMusicOnTaskClearEnabled) {
                    if (dataStore.get(PersistentQueueKey, true) && player.mediaItemCount > 0) {
                        runBlocking { saveQueueToDisk() }
                    }
                    runCatching { stopAndClearPlayback() }
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        } else {
                            stopForeground(true)
                        }
                    }
                    stopSelf()
                    return
                }
            }
        } catch (_: Exception) {}
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handlePlayerWidgetAction(intent?.action)
        super.onStartCommand(intent, flags, startId)
        return START_NOT_STICKY
    }

    private fun handlePlayerWidgetAction(action: String?) {
        when (action) {
            PlayerWidgetActions.ACTION_PLAY_PAUSE -> {
                if (!player.playWhenReady) {
                    cancelIdleStop()
                    promoteToStartedService()
                    ensureStartedAsForeground()
                }
                player.togglePlayPause()
                updatePlayerWidgets()
            }

            PlayerWidgetActions.ACTION_NEXT -> {
                if (player.hasNextMediaItem()) {
                    cancelIdleStop()
                    promoteToStartedService()
                    ensureStartedAsForeground()
                    player.seekToNext()
                    player.playWhenReady = true
                }
                updatePlayerWidgets()
            }

            PlayerWidgetActions.ACTION_PREVIOUS -> {
                if (player.hasPreviousMediaItem()) {
                    cancelIdleStop()
                    promoteToStartedService()
                    ensureStartedAsForeground()
                    player.seekToPrevious()
                    player.playWhenReady = true
                }
                updatePlayerWidgets()
            }

            PlayerWidgetActions.ACTION_REFRESH -> updatePlayerWidgets()
        }
    }

    private fun updatePlayerWidgets() {
        if (!::player.isInitialized) return

        scope.launch(SilentHandler) {
            val state = PlayerWidgetState.fromPlayer(player, this@MusicService)
            PlayerWidgetUpdater.update(this@MusicService, state)
        }
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        if (startInForegroundRequired) ensureStartedAsForeground()
        runCatching { super.onUpdateNotification(session, startInForegroundRequired) }
            .onFailure { reportException(it) }
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
        ): Boolean = (isHostSessionActive && isPlaybackInactive) || stopMusicOnTaskClearEnabled

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