package com.arturo254.opentune.playback

import android.content.Context
import android.database.SQLException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import com.arturo254.opentune.constants.DiscordTokenKey
import com.arturo254.opentune.constants.EnableDiscordRPCKey
import com.arturo254.opentune.constants.EnableLastFMScrobblingKey
import com.arturo254.opentune.constants.HistoryDuration
import com.arturo254.opentune.constants.InnerTubeCookieKey
import com.arturo254.opentune.constants.LastFMUseNowPlaying
import com.arturo254.opentune.constants.ListenBrainzEnabledKey
import com.arturo254.opentune.constants.ListenBrainzTokenKey
import com.arturo254.opentune.constants.PauseListenHistoryKey
import com.arturo254.opentune.constants.ScrobbleDelayPercentKey
import com.arturo254.opentune.constants.ScrobbleDelaySecondsKey
import com.arturo254.opentune.constants.ScrobbleMinSongDurationKey
import com.arturo254.opentune.constants.YtmSyncKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.AlbumEntity
import com.arturo254.opentune.db.entities.ArtistEntity
import com.arturo254.opentune.db.entities.Event
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.extensions.collect
import com.arturo254.opentune.extensions.collectLatest
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.lastfm.LastFM
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.ui.screens.settings.DiscordPresenceManager
import com.arturo254.opentune.ui.screens.settings.ListenBrainzManager
import com.arturo254.opentune.utils.DiscordRPC
import com.arturo254.opentune.utils.ScrobbleManager
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.utils.getAsync
import com.arturo254.opentune.utils.getPresenceIntervalMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime

@OptIn(FlowPreview::class)
internal class PresenceHistoryController(
    private val context: Context,
    private val player: ExoPlayer,
    private val database: MusicDatabase,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val ioScopeProvider: () -> CoroutineScope,
    private val currentSong: StateFlow<Song?>,
    private val onCurrentSongChanged: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    private var discordRpc: DiscordRPC? = null
    private var lastPresenceToken: String? = null
    private var lastPresenceUpdateTime = 0L
    private var scrobbleManager: ScrobbleManager? = null

    fun start() {
        currentSong
            .debounce(300)
            .collect(scopeProvider()) { song ->
                onCurrentSongChanged()

                if (
                    song != null &&
                    player.playWhenReady &&
                    player.playbackState ==
                        Player.STATE_READY
                ) {
                    ensurePresenceManager()
                } else {
                    runCatching {
                        discordRpc?.closeRPC()
                    }
                }
            }

        dataStore.data
            .map {
                it[DiscordTokenKey] to
                    (it[EnableDiscordRPCKey] ?: true)
            }.debounce(300)
            .distinctUntilChanged()
            .collectLatest(scopeProvider()) {
                    (token, enabled) ->
                val newRpc =
                    withContext(Dispatchers.IO) {
                        if (
                            !token.isNullOrBlank() &&
                            enabled
                        ) {
                            runCatching {
                                DiscordRPC(
                                    context,
                                    token,
                                )
                            }.onFailure {
                                Timber
                                    .tag(TAG)
                                    .e(
                                        it,
                                        "failed to create DiscordRPC client",
                                    )
                            }.getOrNull()
                        } else {
                            null
                        }
                    }

                runCatching {
                    if (
                        discordRpc?.isRpcRunning() ==
                        true
                    ) {
                        withContext(Dispatchers.IO) {
                            discordRpc?.closeRPC()
                        }
                    }
                }

                discordRpc = newRpc

                if (
                    discordRpc != null &&
                    player.playbackState ==
                        Player.STATE_READY &&
                    player.playWhenReady &&
                    currentSong.value != null
                ) {
                    ensurePresenceManager()
                } else if (discordRpc == null) {
                    runCatching {
                        DiscordPresenceManager.stop()
                    }
                }
            }

        dataStore.data
            .map {
                it[EnableLastFMScrobblingKey]
                    ?: false
            }.debounce(300)
            .distinctUntilChanged()
            .collect(scopeProvider()) { enabled ->
                if (
                    enabled &&
                    scrobbleManager == null
                ) {
                    val delayPercent =
                        dataStore.get(
                            ScrobbleDelayPercentKey,
                            LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                        )
                    val minSongDuration =
                        dataStore.get(
                            ScrobbleMinSongDurationKey,
                            LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                        )
                    val delaySeconds =
                        dataStore.get(
                            ScrobbleDelaySecondsKey,
                            LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS,
                        )

                    scrobbleManager =
                        ScrobbleManager(
                            ioScopeProvider(),
                            minSongDuration =
                                minSongDuration,
                            scrobbleDelayPercent =
                                delayPercent,
                            scrobbleDelaySeconds =
                                delaySeconds,
                        ).also {
                            it.useNowPlaying =
                                dataStore.get(
                                    LastFMUseNowPlaying,
                                    false,
                                )
                        }
                } else if (
                    !enabled &&
                    scrobbleManager != null
                ) {
                    scrobbleManager?.destroy()
                    scrobbleManager = null
                }
            }

        dataStore.data
            .map {
                it[LastFMUseNowPlaying] ?: false
            }.distinctUntilChanged()
            .collectLatest(scopeProvider()) {
                scrobbleManager
                    ?.useNowPlaying = it
            }

        dataStore.data
            .map { prefs ->
                Triple(
                    prefs[ScrobbleDelayPercentKey]
                        ?: LastFM.DEFAULT_SCROBBLE_DELAY_PERCENT,
                    prefs[ScrobbleMinSongDurationKey]
                        ?: LastFM.DEFAULT_SCROBBLE_MIN_SONG_DURATION,
                    prefs[ScrobbleDelaySecondsKey]
                        ?: LastFM.DEFAULT_SCROBBLE_DELAY_SECONDS,
                )
            }.distinctUntilChanged()
            .collect(scopeProvider()) { values ->
                val (
                    delayPercent,
                    minSongDuration,
                    delaySeconds,
                ) = values

                scrobbleManager?.let {
                    it.scrobbleDelayPercent =
                        delayPercent
                    it.minSongDuration =
                        minSongDuration
                    it.scrobbleDelaySeconds =
                        delaySeconds
                }
            }
    }

    fun onCrossfadeStart(
        mediaItem: MediaItem,
    ) {
        scopeProvider().launch {
            updatePresenceNow(
                mediaId = mediaItem.mediaId,
                metadata = mediaItem.metadata,
                positionMs = 0L,
                isPaused = false,
                reason = "crossfade",
            )
        }
    }

    fun onMediaItemTransition() {
        scrobbleManager?.onSongStop()

        if (
            player.playWhenReady &&
            player.playbackState ==
                Player.STATE_READY
        ) {
            scrobbleManager?.onSongStart(
                player.currentMetadata,
                duration = player.duration,
            )
        }

        ensurePresenceManager()
    }

    fun onPlaybackStateChanged(
        playbackState: Int,
    ) {
        if (
            playbackState ==
                Player.STATE_IDLE ||
            playbackState ==
                Player.STATE_ENDED
        ) {
            scrobbleManager?.onSongStop()
        }

        ensurePresenceManager()

        scopeProvider().launch {
            updatePresenceNow(
                mediaId =
                    player.currentMediaItem?.mediaId,
                metadata =
                    player.currentMetadata,
                positionMs =
                    player.currentPosition,
                isPaused =
                    !player.playWhenReady,
                reason = "playback-state",
            )
        }
    }

    fun onEvents(
        events: Player.Events,
    ) {
        if (
            events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            )
        ) {
            val mediaId =
                player.currentMediaItem?.mediaId
            val metadata =
                player.currentMetadata
            val positionMs =
                player.currentPosition
            val isPaused =
                !player.isPlaying

            scopeProvider().launch {
                updatePresenceNow(
                    mediaId = mediaId,
                    metadata = metadata,
                    positionMs = positionMs,
                    isPaused = isPaused,
                    reason = "player-events",
                )
            }
        }

        if (
            events.contains(
                Player.EVENT_IS_PLAYING_CHANGED,
            )
        ) {
            scrobbleManager
                ?.onPlayerStateChanged(
                    player.isPlaying,
                    player.currentMetadata,
                    duration = player.duration,
                )
        }

        ensurePresenceManager()
    }

    fun stopActivity() {
        scopeProvider().launch {
            runCatching {
                discordRpc?.stopActivity()
            }
        }
    }

    fun onPlaybackStatsReady(
        eventTime: AnalyticsListener.EventTime,
        playbackStats: PlaybackStats,
    ) {
        val mediaItem =
            eventTime.timeline
                .getWindow(
                    eventTime.windowIndex,
                    Timeline.Window(),
                ).mediaItem

        val thresholdMs =
            dataStore.get(
                HistoryDuration,
                30f,
            ) * 1000f

        if (
            playbackStats.totalPlayTimeMs <
                thresholdMs ||
            dataStore.get(
                PauseListenHistoryKey,
                false,
            )
        ) {
            return
        }

        database.query {
            incrementTotalPlayTime(
                mediaItem.mediaId,
                playbackStats.totalPlayTimeMs,
            )

            try {
                insert(
                    Event(
                        songId = mediaItem.mediaId,
                        timestamp =
                            LocalDateTime.now(),
                        playTime =
                            playbackStats
                                .totalPlayTimeMs,
                    ),
                )
            } catch (_: SQLException) {
            }
        }

        ioScopeProvider().launch {
            try {
                val song =
                    database
                        .song(mediaItem.mediaId)
                        .first()
                        ?: return@launch

                val enabled =
                    dataStore.get(
                        ListenBrainzEnabledKey,
                        false,
                    )
                val token =
                    dataStore.get(
                        ListenBrainzTokenKey,
                        "",
                    )

                if (
                    enabled &&
                    token.isNotBlank()
                ) {
                    val endMs =
                        System.currentTimeMillis()
                    val startMs =
                        endMs -
                            playbackStats
                                .totalPlayTimeMs

                    runCatching {
                        ListenBrainzManager
                            .submitFinished(
                                context,
                                token,
                                song,
                                startMs,
                                endMs,
                            )
                    }.onFailure {
                        Timber
                            .tag(TAG)
                            .v(
                                it,
                                "ListenBrainz finished submit failed",
                            )
                    }
                }
            } catch (_: Exception) {
            }
        }

        ioScopeProvider().launch {
            runCatching {
                registerRemoteListeningHistory(
                    mediaItem.mediaId,
                )
            }
        }
    }

    fun clearOnTaskRemoved() {
        stopActivity()

        runCatching {
            if (
                discordRpc?.isRpcRunning() ==
                true
            ) {
                discordRpc?.closeRPC()
            }
        }

        discordRpc = null

        runCatching {
            DiscordPresenceManager.stop()
        }

        lastPresenceToken = null
    }

    fun release() {
        runCatching {
            DiscordPresenceManager.stop()
        }

        runCatching {
            discordRpc?.closeRPC()
        }

        discordRpc = null
        scrobbleManager?.destroy()
        scrobbleManager = null
        lastPresenceToken = null
    }

    private fun ensurePresenceManager() {
        if (
            DiscordPresenceManager.isRunning() &&
            lastPresenceToken != null
        ) {
            return
        }

        scopeProvider().launch {
            if (
                !dataStore.get(
                    EnableDiscordRPCKey,
                    true,
                )
            ) {
                if (
                    DiscordPresenceManager
                        .isRunning()
                ) {
                    runCatching {
                        DiscordPresenceManager.stop()
                    }
                    lastPresenceToken = null
                }
                return@launch
            }

            val token =
                dataStore.get(
                    DiscordTokenKey,
                    "",
                )

            if (token.isBlank()) {
                if (
                    DiscordPresenceManager
                        .isRunning()
                ) {
                    runCatching {
                        DiscordPresenceManager.stop()
                    }
                    lastPresenceToken = null
                }
                return@launch
            }

            if (
                DiscordPresenceManager.isRunning() &&
                lastPresenceToken == token
            ) {
                return@launch
            }

            try {
                DiscordPresenceManager.stop()
                DiscordPresenceManager.start(
                    context = context,
                    token = token,
                    songProvider = {
                        player.currentMetadata
                            ?.let(
                                ::createTransientSongFromMedia,
                            )
                            ?: currentSong.value
                    },
                    positionProvider = {
                        player.currentPosition
                    },
                    isPausedProvider = {
                        !player.isPlaying
                    },
                    intervalProvider = {
                        getPresenceIntervalMillis(
                            context,
                        )
                    },
                )

                lastPresenceToken = token
            } catch (error: Exception) {
                Timber
                    .tag(TAG)
                    .e(
                        error,
                        "Failed to start presence manager",
                    )
            }
        }
    }

    private suspend fun updatePresenceNow(
        mediaId: String?,
        metadata: MediaMetadata?,
        positionMs: Long,
        isPaused: Boolean,
        reason: String,
    ) {
        try {
            val token =
                withContext(Dispatchers.IO) {
                    dataStore.get(
                        DiscordTokenKey,
                        "",
                    )
                }

            if (
                token.isBlank() ||
                !DiscordPresenceManager
                    .isRunning()
            ) {
                return
            }

            val song =
                if (!mediaId.isNullOrBlank()) {
                    withContext(Dispatchers.IO) {
                        database
                            .song(mediaId)
                            .first()
                    }
                } else {
                    null
                }

            val finalSong =
                song
                    ?: metadata?.let(
                        ::createTransientSongFromMedia,
                    )

            if (!canUpdatePresence()) {
                return
            }

            val success =
                withContext(Dispatchers.IO) {
                    DiscordPresenceManager
                        .updateNow(
                            context = context,
                            token = token,
                            song = finalSong,
                            positionMs =
                                positionMs,
                            isPaused = isPaused,
                        )
                }

            if (!success) {
                Timber
                    .tag(TAG)
                    .w(
                        "$reason presence update failed; attempting restart",
                    )

                if (
                    DiscordPresenceManager
                        .isRunning()
                ) {
                    runCatching {
                        DiscordPresenceManager
                            .restart()
                    }
                }
            }

            submitListenBrainzPlayingNow(
                finalSong,
                positionMs,
            )
        } catch (error: Exception) {
            Timber
                .tag(TAG)
                .v(
                    error,
                    "$reason presence update failed",
                )
        }
    }

    private suspend fun submitListenBrainzPlayingNow(
        song: Song?,
        positionMs: Long,
    ) {
        if (song == null) return

        try {
            val enabled =
                withContext(Dispatchers.IO) {
                    dataStore.get(
                        ListenBrainzEnabledKey,
                        false,
                    )
                }
            val token =
                withContext(Dispatchers.IO) {
                    dataStore.get(
                        ListenBrainzTokenKey,
                        "",
                    )
                }

            if (
                enabled &&
                token.isNotBlank()
            ) {
                ioScopeProvider().launch {
                    runCatching {
                        ListenBrainzManager
                            .submitPlayingNow(
                                context,
                                token,
                                song,
                                positionMs,
                            )
                    }.onFailure {
                        Timber
                            .tag(TAG)
                            .v(
                                it,
                                "ListenBrainz playing_now submit failed",
                            )
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun canUpdatePresence(): Boolean {
        val now =
            System.currentTimeMillis()

        synchronized(this) {
            return if (
                now -
                    lastPresenceUpdateTime >
                MIN_PRESENCE_UPDATE_INTERVAL
            ) {
                lastPresenceUpdateTime = now
                true
            } else {
                false
            }
        }
    }

    private suspend fun registerRemoteListeningHistory(
        mediaId: String,
    ) {
        if (!isRemoteHistorySyncAllowed()) {
            return
        }

        val attemptedUrls =
            LinkedHashSet<String>()

        suspend fun registerTrackingUrl(
            url: String,
        ): Boolean {
            attemptedUrls += url

            return YouTube
                .registerPlayback(
                    playbackTracking = url,
                ).onFailure(onError)
                .isSuccess
        }

        val cachedPlaybackUrl =
            database
                .format(mediaId)
                .first()
                ?.playbackUrl

        if (
            cachedPlaybackUrl
                ?.let {
                    registerTrackingUrl(it)
                } == true
        ) {
            return
        }

        val playbackTracking =
            YTPlayerUtils
                .playerResponseForMetadata(
                    mediaId,
                    null,
                ).getOrNull()
                ?.playbackTracking
                ?: return

        for (
            trackingUrl in
            listOfNotNull(
                playbackTracking
                    .videostatsPlaybackUrl
                    ?.baseUrl,
                playbackTracking
                    .videostatsWatchtimeUrl
                    ?.baseUrl,
            )
        ) {
            if (
                trackingUrl.isBlank() ||
                trackingUrl in attemptedUrls
            ) {
                continue
            }

            registerTrackingUrl(
                trackingUrl,
            )
        }
    }

    private suspend fun isRemoteHistorySyncAllowed():
        Boolean {
        if (
            !dataStore.getAsync(
                YtmSyncKey,
                true,
            )
        ) {
            return false
        }

        val cookie =
            dataStore.getAsync(
                InnerTubeCookieKey,
                "",
            )

        return cookie.isNotBlank() &&
            cookie.contains("SAPISID")
    }

    private fun createTransientSongFromMedia(
        media: MediaMetadata,
    ): Song {
        val songEntity =
            SongEntity(
                id = media.id,
                title = media.title,
                duration = media.duration,
                thumbnailUrl =
                    media.thumbnailUrl,
                albumId = media.album?.id,
                albumName =
                    media.album?.title,
                explicit = media.explicit,
            )

        val artists =
            media.artists.map { artist ->
                ArtistEntity(
                    id =
                        artist.id
                            ?: "LA_unknown_${artist.name}",
                    name = artist.name,
                    thumbnailUrl =
                        if (
                            !artist.thumbnailUrl
                                .isNullOrBlank()
                        ) {
                            artist.thumbnailUrl
                        } else {
                            media.thumbnailUrl
                        },
                )
            }

        val album =
            media.album?.let { value ->
                AlbumEntity(
                    id = value.id,
                    playlistId = null,
                    title = value.title,
                    year = null,
                    thumbnailUrl =
                        media.thumbnailUrl,
                    themeColor = null,
                    songCount = 1,
                    duration =
                        media.duration,
                )
            }

        return Song(
            song = songEntity,
            artists = artists,
            album = album,
            format = null,
        )
    }

    private companion object {
        const val TAG =
            "PresenceHistoryController"
        const val MIN_PRESENCE_UPDATE_INTERVAL =
            1_000L
    }
}
