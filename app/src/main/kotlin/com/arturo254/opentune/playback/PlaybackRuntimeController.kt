package com.arturo254.opentune.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.constants.AudioCrossfadeDurationKey
import com.arturo254.opentune.constants.AudioNormalizationKey
import com.arturo254.opentune.constants.AudioOffload
import com.arturo254.opentune.constants.MaxSongCacheSizeKey
import com.arturo254.opentune.constants.PlayerVolumeKey
import com.arturo254.opentune.constants.RepeatModeKey
import com.arturo254.opentune.constants.ShowLyricsKey
import com.arturo254.opentune.constants.SkipSilenceKey
import com.arturo254.opentune.constants.SmartTrimmerKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.extensions.collect
import com.arturo254.opentune.extensions.collectLatest
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.lyrics.LyricsHelper
import com.arturo254.opentune.lyrics.LyricsPreloadManager
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.utils.NetworkConnectivityObserver
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow

@OptIn(FlowPreview::class)
internal class PlaybackRuntimeController(
    private val context: Context,
    private val player: ExoPlayer,
    private val database: MusicDatabase,
    private val dataStore: DataStore<Preferences>,
    private val lyricsHelper: LyricsHelper,
    private val streamResolver: StreamResolver,
    private val playbackEngine: PlaybackEngine,
    private val audioEffectsController: AudioEffectsController,
    private val errorRecoveryController: ErrorRecoveryController,
    private val presenceHistoryController: PresenceHistoryController,
    private val persistenceController: PlaybackPersistenceController,
    private val mediaSessionNotificationController:
        MediaSessionNotificationController,
    private val lifecycleController: PlaybackLifecycleController,
    private val currentMediaMetadata:
        MutableStateFlow<MediaMetadata?>,
    val playerVolume: MutableStateFlow<Float>,
    private val audioFocusVolumeFactor:
        MutableStateFlow<Float>,
    private val scopeProvider: () -> CoroutineScope,
    private val ioScopeProvider: () -> CoroutineScope,
) {
    private val normalizeFactor =
        MutableStateFlow(1f)
    private val playbackFadeFactor =
        MutableStateFlow(1f)
    private val crossfadeDurationMs =
        MutableStateFlow(0)
    private val audioNormalizationEnabled =
        MutableStateFlow(true)

    private val currentFormat =
        currentMediaMetadata
            .flatMapLatest { metadata ->
                database.format(metadata?.id)
            }.flowOn(Dispatchers.IO)

    private val connectivityObserver =
        NetworkConnectivityObserver(context)

    val crossfadeAudio =
        CrossfadeAudio(
            player = player,
            database = database,
            crossfadeDurationMs =
                crossfadeDurationMs,
            playbackFadeFactor =
                playbackFadeFactor,
            playerVolume = playerVolume,
            audioFocusVolumeFactor =
                audioFocusVolumeFactor,
            audioNormalizationEnabled =
                audioNormalizationEnabled,
            maxSafeGainFactor =
                MAX_SAFE_GAIN_FACTOR,
            overlapPlayerFactory = {
                playbackEngine
                    .createOverlapPlayer()
            },
            onCrossfadeStart = { mediaItem ->
                currentMediaMetadata.value =
                    mediaItem.metadata

                presenceHistoryController
                    .onCrossfadeStart(
                        mediaItem,
                    )
            },
        )

    val lyricsPreloadManager =
        LyricsPreloadManager(
            context = context,
            database = database,
            networkConnectivity =
                connectivityObserver,
        )

    fun start() {
        mediaSessionNotificationController.start()
        lifecycleController.start()

        scopeProvider().launch(
            Dispatchers.IO,
        ) {
            val prefs =
                dataStore.data.first()
            val repeatMode =
                prefs[RepeatModeKey]
                    ?: Player.REPEAT_MODE_OFF
            val volume =
                (
                    prefs[PlayerVolumeKey]
                        ?: 1f
                ).coerceIn(0f, 1f)
            val offload =
                prefs[AudioOffload]
                    ?: false

            withContext(Dispatchers.Main) {
                player.repeatMode =
                    repeatMode
                playerVolume.value =
                    volume
                playbackEngine
                    .updateAudioOffload(
                        player,
                        offload,
                    )
            }
        }

        scopeProvider().launch {
            connectivityObserver
                .networkStatus
                .collect { isConnected ->
                    errorRecoveryController
                        .onNetworkConnectivityChanged(
                            isConnected,
                        )
                }
        }

        combine(
            playerVolume,
            normalizeFactor,
            audioFocusVolumeFactor,
            playbackFadeFactor,
        ) {
            playerVolume,
            normalizeFactor,
            audioFocusVolumeFactor,
            playbackFadeFactor,
            ->
            playerVolume *
                normalizeFactor *
                audioFocusVolumeFactor *
                playbackFadeFactor
        }.collectLatest(
            scopeProvider(),
        ) { finalVolume ->
            player.volume = finalVolume
        }

        playerVolume
            .debounce(1_000)
            .collect(
                ioScopeProvider(),
            ) { volume ->
                dataStore.edit {
                        settings ->
                    settings[PlayerVolumeKey] =
                        volume
                }
            }

        combine(
            currentMediaMetadata
                .distinctUntilChangedBy {
                    it?.id
                },
            dataStore.data
                .map {
                    it[ShowLyricsKey]
                        ?: false
                }.distinctUntilChanged(),
        ) { metadata, showLyrics ->
            metadata to showLyrics
        }.collectLatest(
            ioScopeProvider(),
        ) {
                (
                    metadata,
                    showLyrics,
                ),
            ->
            if (
                showLyrics &&
                metadata != null &&
                database
                    .lyrics(metadata.id)
                    .first() == null
            ) {
                val lyrics =
                    lyricsHelper
                        .getLyrics(metadata)

                database.query {
                    upsert(
                        LyricsEntity(
                            id = metadata.id,
                            lyrics = lyrics,
                        ),
                    )
                }
            }
        }

        dataStore.data
            .map {
                it[SkipSilenceKey]
                    ?: false
            }.distinctUntilChanged()
            .collectLatest(
                scopeProvider(),
            ) {
                player.skipSilenceEnabled =
                    it
            }

        combine(
            dataStore.data
                .map {
                    it[AudioOffload]
                        ?: false
                }.distinctUntilChanged(),
            audioEffectsController
                .internalDspActive,
        ) {
            requestedOffload,
            dspActive,
            ->
            requestedOffload to
                dspActive
        }.collectLatest(
            scopeProvider(),
        ) {
                (
                    requestedOffload,
                    dspActive,
                ),
            ->
            val effectiveOffload =
                requestedOffload &&
                    !dspActive

            playbackEngine
                .updateAudioOffload(
                    player,
                    effectiveOffload,
                )

            if (effectiveOffload) {
                val skipSilenceEnabled =
                    dataStore.get(
                        SkipSilenceKey,
                        false,
                    )

                if (skipSilenceEnabled) {
                    dataStore.edit {
                        it[SkipSilenceKey] =
                            false
                    }
                    player.skipSilenceEnabled =
                        false
                }

                val crossfadeSeconds =
                    dataStore.get(
                        AudioCrossfadeDurationKey,
                        0,
                    )

                if (crossfadeSeconds != 0) {
                    dataStore.edit {
                        it[
                            AudioCrossfadeDurationKey
                        ] = 0
                    }
                }
            }
        }

        dataStore.data
            .map {
                (
                    it[
                        AudioCrossfadeDurationKey
                    ] ?: 0
                ) * 1_000
            }.distinctUntilChanged()
            .collectLatest(
                scopeProvider(),
            ) {
                crossfadeDurationMs.value =
                    it
            }

        crossfadeAudio.start(
            scopeProvider(),
        )

        audioEffectsController.start()

        combine(
            currentFormat,
            dataStore.data
                .map {
                    it[AudioNormalizationKey]
                        ?: true
                }.distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(
            scopeProvider(),
        ) {
                (
                    format,
                    normalizeAudio,
                ),
            ->
            audioNormalizationEnabled.value =
                normalizeAudio

            Timber
                .tag("AudioNormalization")
                .d(
                    "Audio normalization enabled: $normalizeAudio",
                )

            val loudness =
                format?.loudnessDb
                    ?: format
                        ?.perceptualLoudnessDb

            normalizeFactor.value =
                if (
                    normalizeAudio &&
                    loudness != null
                ) {
                    val loudnessDb =
                        loudness.toFloat()
                    var factor =
                        10f.pow(
                            -loudnessDb / 20,
                        )

                    if (factor > 1f) {
                        factor =
                            min(
                                factor,
                                MAX_SAFE_GAIN_FACTOR,
                            )
                    }

                    factor
                } else {
                    1f
                }
        }

        dataStore.data
            .map { prefs ->
                (
                    prefs[SmartTrimmerKey]
                        ?: false
                ) to
                    (
                        prefs[
                            MaxSongCacheSizeKey
                        ] ?: 1_024
                    )
            }.debounce(300)
            .distinctUntilChanged()
            .collectLatest(
                ioScopeProvider(),
            ) {
                    (
                        enabled,
                        maxSongCacheSizeMb,
                    ),
                ->
                if (!enabled) {
                    return@collectLatest
                }

                if (
                    maxSongCacheSizeMb <= 0 ||
                    maxSongCacheSizeMb == -1
                ) {
                    return@collectLatest
                }

                val bytesPerMb =
                    1_024L * 1_024L
                val safeSizeMb =
                    maxSongCacheSizeMb
                        .toLong()
                        .coerceAtMost(
                            Long.MAX_VALUE /
                                bytesPerMb,
                        )
                val limitBytes =
                    safeSizeMb * bytesPerMb

                streamResolver
                    .trimPlayerCacheToBytes(
                        limitBytes,
                    )
            }

        presenceHistoryController.start()
        persistenceController.start()
    }

    fun release() {
        runCatching {
            connectivityObserver.unregister()
        }

        runCatching {
            crossfadeAudio.release()
        }
    }

    private companion object {
        const val MAX_SAFE_GAIN_FACTOR =
            1.414f
    }
}
