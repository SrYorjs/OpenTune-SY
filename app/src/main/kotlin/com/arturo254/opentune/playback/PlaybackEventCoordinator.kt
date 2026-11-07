package com.arturo254.opentune.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.constants.AutoLoadMoreKey
import com.arturo254.opentune.constants.RepeatModeKey
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.mediaItems
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.lyrics.LyricsPreloadManager
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class PlaybackEventCoordinator(
    private val player: ExoPlayer,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val currentMediaMetadata:
        MutableStateFlow<MediaMetadata?>,
    private val errorRecoveryController:
        ErrorRecoveryController,
    private val crossfadeAudioProvider:
        () -> CrossfadeAudio?,
    private val lyricsPreloadManagerProvider:
        () -> LyricsPreloadManager?,
    private val streamResolver:
        StreamResolver,
    private val togetherController:
        TogetherController,
    private val automixRadioController:
        AutomixRadioController,
    private val queueController:
        QueueController,
    private val persistenceController:
        PlaybackPersistenceController,
    private val presenceHistoryController:
        PresenceHistoryController,
    private val lifecycleController:
        PlaybackLifecycleController,
    private val audioEffectsController:
        AudioEffectsController,
    private val requestAudioFocus: () -> Boolean,
    private val updateNotification: () -> Unit,
    private val refreshNotification: () -> Unit,
    private val updateWidgets: () -> Unit,
) {
    private var nextPageLoadJob: Job? = null
    private var paginationQueue: com.arturo254.opentune.playback.queues.Queue? = null

    fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        errorRecoveryController
            .onMediaItemTransition(
                mediaItem
                    ?.mediaId
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: player
                        .currentMediaItem
                        ?.mediaId,
            )

        crossfadeAudioProvider()
            ?.onMediaItemTransition(
                mediaItem,
                reason,
            )

        val currentIndex =
            player.currentMediaItemIndex
        val queue =
            player.mediaItems
                .mapNotNull {
                    it.metadata
                }

        if (queue.isNotEmpty()) {
            lyricsPreloadManagerProvider()
                ?.onSongChanged(
                    currentIndex,
                    queue,
                )
        }

        streamResolver
            .scheduleNextStreamWarmup()

        if (
            togetherController
                .onMediaItemTransition(
                    mediaItem,
                    reason,
                )
        ) {
            return
        }

        val timelineEmpty =
            player.currentTimeline.isEmpty ||
                player.mediaItemCount == 0 ||
                player.currentMediaItem == null

        currentMediaMetadata.value =
            if (timelineEmpty) {
                null
            } else {
                mediaItem?.metadata
                    ?: player.currentMetadata
            }

        queueController.updatePagingSeed(
            player.currentMediaItem?.mediaId
                ?: mediaItem?.mediaId,
        )

        automixRadioController
            .onMediaItemTransition(
                mediaItem = mediaItem,
                reason = reason,
                timelineEmpty =
                    timelineEmpty,
                queueHasNextPage =
                    queueController
                        .hasNextPage(),
            )

        requestNextQueuePageIfNeeded()

        presenceHistoryController
            .onMediaItemTransition()

        scopeProvider().launch {
            persistenceController
                .saveIfEnabled()
        }

        refreshNotification()
        updateWidgets()
    }

    /**
     * La primera canción puede comenzar con un único elemento; las recomendaciones
     * se obtienen después de preparar el reproductor, sin bloquear el audio.
     * Al llegar a los últimos diez temas se solicitan nuevas páginas.
     */
    internal fun requestNextQueuePageIfNeeded() {
        val requestedQueue = queueController.currentQueue
        if (paginationQueue !== requestedQueue) {
            nextPageLoadJob?.cancel()
            nextPageLoadJob = null
            paginationQueue = requestedQueue
        }

        if (!queueController.initialQueueReady ||
            player.playbackState == Player.STATE_IDLE ||
            player.currentTimeline.isEmpty ||
            nextPageLoadJob?.isActive == true
        ) return

        fun shouldLoad(): Boolean = QueuePrefetchPolicy.shouldLoad(
            currentIndex = player.currentMediaItemIndex,
            totalItems = player.mediaItemCount,
            hasNextPage = queueController.hasNextPage(),
            autoLoadEnabled = dataStore.get(AutoLoadMoreKey, true),
            repeatEnabled = player.repeatMode != Player.REPEAT_MODE_OFF,
            suppressed = queueController.suppressAutoPlayback,
        )

        if (!shouldLoad()) return
        nextPageLoadJob = scopeProvider().launch(SilentHandler) {
            repeat(MAX_PAGE_FETCHES_PER_EVENT) {
                if (paginationQueue !== requestedQueue ||
                    queueController.currentQueue !== requestedQueue ||
                    !queueController.initialQueueReady ||
                    player.playbackState == Player.STATE_IDLE ||
                    !shouldLoad()
                ) return@launch

                val page = queueController.nextPage()
                if (queueController.currentQueue !== requestedQueue) return@launch

                val ids = (0 until player.mediaItemCount)
                    .map { player.getMediaItemAt(it).mediaId }
                    .toMutableSet()
                val newItems = page.filter { ids.add(it.mediaId) }
                if (newItems.isEmpty()) return@launch
                player.addMediaItems(newItems)
            }
        }
    }

    fun onPlaybackStateChanged(
        playbackState: Int,
    ) {
        updateWidgets()
        if (playbackState == Player.STATE_READY) requestNextQueuePageIfNeeded()

        errorRecoveryController
            .onPlaybackStateChanged(
                playbackState,
            )

        scopeProvider().launch {
            persistenceController
                .saveIfEnabled()
        }

        if (
            playbackState ==
                Player.STATE_IDLE ||
            playbackState ==
                Player.STATE_ENDED
        ) {
            crossfadeAudioProvider()
                ?.stop(
                    resetMainFade = true,
                )
        }

        automixRadioController
            .onPlaybackStateChanged(
                playbackState,
            )

        presenceHistoryController
            .onPlaybackStateChanged(
                playbackState,
            )
    }

    fun onEvents(
        events: Player.Events,
    ) {
        if (events.contains(Player.EVENT_TIMELINE_CHANGED)) requestNextQueuePageIfNeeded()
        if (
            events.contains(
                Player.EVENT_MEDIA_METADATA_CHANGED,
            ) &&
            crossfadeAudioProvider()
                ?.isCrossfading() != true
        ) {
            currentMediaMetadata.value =
                player.currentMetadata
        }

        if (
            events.contains(
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
            )
        ) {
            togetherController
                .onPlayWhenReadyChanged()
        }

        lifecycleController
            .onPlayerEvents(events)

        if (
            events.contains(
                Player.EVENT_AUDIO_SESSION_ID,
            )
        ) {
            audioEffectsController
                .onAudioSessionIdChanged(
                    player.audioSessionId,
                )
        }

        if (
            events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
            )
        ) {
            val playbackState =
                player.playbackState
            val keepAudioEffectSessionOpen =
                playbackState ==
                    Player.STATE_BUFFERING ||
                    playbackState ==
                        Player.STATE_READY

            if (
                player.playWhenReady &&
                keepAudioEffectSessionOpen
            ) {
                requestAudioFocus()
            }

            if (
                keepAudioEffectSessionOpen
            ) {
                audioEffectsController
                    .openSession()
            } else {
                audioEffectsController
                    .closeSession()
            }
        }

        if (
            events.containsAny(
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_POSITION_DISCONTINUITY,
            ) &&
            crossfadeAudioProvider()
                ?.isCrossfading() != true
        ) {
            currentMediaMetadata.value =
                player.currentMetadata
        }

        if (
            events.contains(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            )
        ) {
            currentMediaMetadata.value =
                player.currentMetadata
        }

        presenceHistoryController
            .onEvents(events)

        if (
            events.containsAny(
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            )
        ) {
            refreshNotification()
        }

        if (
            events.containsAny(
                Player.EVENT_IS_PLAYING_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
            )
        ) {
            updateWidgets()
        }
    }

    fun onShuffleModeEnabledChanged(
        shuffleModeEnabled: Boolean,
    ) {
        updateNotification()

        if (
            togetherController
                .onShuffleModeChanged(
                    shuffleModeEnabled,
                )
        ) {
            return
        }

        if (shuffleModeEnabled) {
            queueController
                .applyCurrentFirstShuffleOrder()
        }

        scopeProvider().launch {
            persistenceController
                .saveIfEnabled()
        }
    }

    fun onRepeatModeChanged(
        repeatMode: Int,
    ) {
        updateNotification()

        if (
            togetherController
                .onRepeatModeChanged(
                    repeatMode,
                )
        ) {
            return
        }

        scopeProvider().launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] =
                    repeatMode
            }
        }

        scopeProvider().launch {
            persistenceController
                .saveIfEnabled()
        }
    }

    fun onPlayerError(
        error: PlaybackException,
    ) {
        errorRecoveryController
            .handlePlayerError(error)
    }

    private companion object {
        const val MAX_PAGE_FETCHES_PER_EVENT = 4
    }
}
