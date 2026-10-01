/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



@file:Suppress("DEPRECATION")

package com.arturo254.opentune.playback

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.MediaCodecList
import android.media.audiofx.Virtualizer
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
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AudioQualityKey
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
import com.arturo254.opentune.constants.PersistentQueueKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.constants.PlayerStreamClientKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.FormatEntity
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

    private lateinit var components:
        MusicServiceComposition.Components

    private val audioFocusController
        get() = components.audioFocusController

    private val playbackLifecycleController
        get() = components.playbackLifecycleController

    private val mediaSessionNotificationController
        get() = components.mediaSessionNotificationController

    private val libraryActionsController
        get() = components.libraryActionsController

    private val presenceHistoryController
        get() = components.presenceHistoryController

    private val playbackEventCoordinator
        get() = components.playbackEventCoordinator

    private val playbackRuntimeController
        get() = components.playbackRuntimeController

    private val errorRecoveryController
        get() = components.errorRecoveryController

    private val streamResolver
        get() = components.streamResolver

    private val playbackPersistenceController
        get() = components.playbackPersistenceController

    private val queueController
        get() = components.queueController

    private val audioEffectsController
        get() = components.audioEffectsController

    private val automixRadioController
        get() = components.automixRadioController

    private val togetherController
        get() = components.togetherController

    private var scopeJob = Job()
    private var scope = CoroutineScope(Dispatchers.Main + scopeJob)
    private var ioScope = CoroutineScope(Dispatchers.IO + scopeJob)
    private val binder = MusicBinder()

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

    val currentMediaMetadata =
        MutableStateFlow<com.arturo254.opentune.models.MediaMetadata?>(null)

    val queueRestoreCompleted
        get() =
            playbackPersistenceController.queueRestoreCompleted
    var playerVolume = MutableStateFlow(1f)
    private val audioFocusVolumeFactor = MutableStateFlow(1f)
    @Inject
    @PlayerCache
    lateinit var playerCache: Cache

    @Inject
    @DownloadCache
    lateinit var downloadCache: Cache

    val player: ExoPlayer
        get() = components.player

    val sleepTimer: SleepTimer
        get() = components.sleepTimer

    val eqCapabilities
        get() = audioEffectsController.eqCapabilities

    val automixItems
        get() = automixRadioController.automixItems

    val automixLoading
        get() = automixRadioController.automixLoading

    val automixError
        get() = automixRadioController.automixError

    val autoAddedMediaIds
        get() = automixRadioController.autoAddedMediaIds


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

        components =
            MusicServiceComposition.create(
                service = this,
                scopeProvider = { scope },
                ioScopeProvider = { ioScope },
                audioFocusVolumeFactor =
                    audioFocusVolumeFactor,
                audioQualityProvider = {
                    audioQuality
                },
                preferredStreamClientProvider = {
                    preferredStreamClient
                },
                callbacks =
                    MusicServiceComposition.Callbacks(
                        ensureScopesActive =
                            ::ensureScopesActive,
                        recoverSong = { mediaId, playbackData ->
                            recoverSong(
                                mediaId,
                                playbackData,
                            )
                        },
                        installNotificationProvider = { provider ->
                            setMediaNotificationProvider(
                                provider,
                            )
                        },
                    ),
            )

        playbackRuntimeController.start()
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
        abandonAudioFocus()
        try {
            audioEffectsController.release()
        } catch (_: Exception) {}
        try {
            playbackPersistenceController
                .saveBlockingIfEnabled()
        } catch (_: Exception) {}

        try {
            playbackRuntimeController.release()
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