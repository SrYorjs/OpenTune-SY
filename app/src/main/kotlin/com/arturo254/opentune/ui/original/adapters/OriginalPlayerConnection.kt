package com.arturo254.opentune.ui.original.adapters

import androidx.datastore.preferences.core.edit
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.arturo254.opentune.ui.original.playback.StreamFacts
import kotlinx.coroutines.launch
import androidx.media3.common.Player
import com.arturo254.opentune.extensions.getCurrentQueueIndex
import com.arturo254.opentune.extensions.getQueueWindows
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.ui.original.player.models.ProgressState
import com.arturo254.opentune.ui.original.player.models.QueueItemData
import com.arturo254.opentune.ui.original.player.models.QueueState
import com.arturo254.opentune.ui.original.player.models.TransportState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class OriginalPlayerConnection(
    private val connection: PlayerConnection,
    val scope: CoroutineScope,
    isPageActiveAndResumed: StateFlow<Boolean>,
) {
    val player = connection.player
    val service = connection.service
    val currentLyrics = connection.currentLyrics
    val error = connection.error
    val isPlaying = connection.isPlaying
    val uiState by lazy { com.arturo254.opentune.ui.original.player.PlayerUiState(this) }
    val skipDelayMs: Long get() = 0L
    val lyricsOffsetMs = service.dataStore.data.map {
        it[com.arturo254.opentune.constants.LyricsSyncOffsetKey] ?: 0
    }.stateIn(scope, SharingStarted.WhileSubscribed(0), service.dataStore.get(com.arturo254.opentune.constants.LyricsSyncOffsetKey, 0))

    fun seekTo(positionMs: Long) {
        // Same crossfade realignment as GalaxOT PlayerActions: visible metadata can lead Media3.
        if (mediaMetadata.value?.id != null && mediaMetadata.value?.id != player.currentMediaItem?.mediaId) player.seekToNext()
        player.seekTo(positionMs)
    }
    fun playQueue(queue: com.arturo254.opentune.playback.queues.Queue) = connection.playQueue(queue)
    fun seekToNext() = connection.seekToNext()
    fun seekToPrevious() = connection.seekToPrevious()
    fun isCurrentMusicVideo(): Boolean = player.currentMediaItem?.mediaMetadata?.extras
        ?.getBoolean(com.arturo254.opentune.extensions.ExtraIsMusicVideo) == true
    val mediaMetadata = connection.mediaMetadata
    val currentSong = connection.currentSong
    val shuffleModeEnabled = connection.shuffleModeEnabled
    val repeatMode = connection.repeatMode
    val playbackState = connection.playbackState

    val database = connection.database
    fun toggleLike() = connection.toggleLike()
    val infiniteQueueEnabled = service.dataStore.data.map { it[com.arturo254.opentune.constants.AutoLoadMoreKey] ?: true }
        .stateIn(scope, SharingStarted.WhileSubscribed(0), service.dataStore.get(com.arturo254.opentune.constants.AutoLoadMoreKey, true))
    fun setInfiniteQueueEnabled(enabled: Boolean) { scope.launch {
        service.dataStore.edit { it[com.arturo254.opentune.constants.AutoLoadMoreKey] = enabled }
        if (enabled) service.onInfiniteQueueEnabled() else service.onInfiniteQueueDisabled()
    } }
    private fun mediaIndexOf(uid: Int): Int? {
        val timeline = player.currentTimeline
        val window = Timeline.Window()
        // Int UIDs are Mochy's contract. Reject hash collisions rather than edit the wrong item.
        return (0 until timeline.windowCount).filter { timeline.getWindow(it, window).uid.hashCode() == uid }.singleOrNull()
    }
    fun playQueueItem(uid: Int) { val index = mediaIndexOf(uid) ?: return
        player.seekToDefaultPosition(index)
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) player.prepare()
        player.playWhenReady = true
    }
    fun moveQueueItem(uid: Int, afterUid: Int) {
        val from = mediaIndexOf(uid) ?: return
        val after = mediaIndexOf(afterUid) ?: return
        if (from == after) return
        if (!player.shuffleModeEnabled) player.moveMediaItem(from, if (from < after) after else after + 1)
        else {
            val timeline = player.currentTimeline
            val order = mutableListOf<Int>()
            var index = timeline.getFirstWindowIndex(true)
            while (index != androidx.media3.common.C.INDEX_UNSET && order.size < timeline.windowCount) {
                order.add(index); index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
            }
            order.remove(from); order.add(order.indexOf(after) + 1, from)
            player.setShuffleOrder(DefaultShuffleOrder(order.toIntArray(), System.currentTimeMillis()))
        }
    }
    fun playQueueItemNext(uid: Int) {
        val index = mediaIndexOf(uid) ?: return
        if (index == player.currentMediaItemIndex) return
        val item = player.getMediaItemAt(index)
        player.removeMediaItem(index)
        service.playNext(listOf(item))
    }
    fun removeQueueItems(uids: Collection<Int>) {
        uids.mapNotNull(::mediaIndexOf).distinct().filter { it != player.currentMediaItemIndex }.sortedDescending().forEach(player::removeMediaItem)
    }
    fun streamFacts(mediaId: String): Flow<StreamFacts> = combine(database.format(mediaId), callbackFlow {
        fun publish() { trySend(if (player.currentMediaItem?.mediaId == mediaId) StreamFacts.selectedAudio(player.currentTracks) else null) }
        val listener = object : Player.Listener { override fun onEvents(player: Player, events: Player.Events) = publish() }
        player.addListener(listener); publish(); awaitClose { player.removeListener(listener) }
    }.flowOn(Dispatchers.Main.immediate)) { stored, decoded ->
        StreamFacts(
            codec = decoded?.let { StreamFacts.codecOf(it.sampleMimeType, it.codecs) } ?: stored?.let { StreamFacts.of(it).codec },
            sampleRate = decoded?.sampleRate?.takeIf { it > 0 } ?: stored?.sampleRate?.takeIf { it > 0 },
        )
    }

    private fun transport() = TransportState(
        isPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED,
        shuffleModeEnabled = player.shuffleModeEnabled,
        repeatMode = player.repeatMode,
        canSkipNext = player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT),
        canSkipPrevious = player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS),
        playbackState = player.playbackState,
    )

    private fun queue() = QueueState(
        items = player.getQueueWindows().map { window ->
            val item = window.mediaItem
            QueueItemData(
                uid = window.uid.hashCode(),
                mediaId = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                artworkUri = item.mediaMetadata.artworkUri,
                duration = window.durationMs,
                isAutoplay = null,
            )
        },
        currentIndex = player.getCurrentQueueIndex(),
        title = connection.service.queueTitle.orEmpty(),
    )

    val initialTransportState = transport()
    val initialQueueState = queue()

    private val snapshots = callbackFlow {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                trySend(transport() to queue())
            }
        }
        player.addListener(listener)
        trySend(transport() to queue())
        awaitClose { player.removeListener(listener) }
    }.flowOn(Dispatchers.Main.immediate).stateIn(
        scope, SharingStarted.WhileSubscribed(0), initialTransportState to initialQueueState,
    )
    val queueSnapshot = snapshots.map { it.second }.stateIn(scope, SharingStarted.WhileSubscribed(0), initialQueueState)

    val isEffectivelyPlaying = snapshots.map { it.first.isPlaying }.stateIn(scope, SharingStarted.WhileSubscribed(0), initialTransportState.isPlaying)
    val canSkipNext = snapshots.map { it.first.canSkipNext }
    val canSkipPrevious = snapshots.map { it.first.canSkipPrevious }
    val queueItems = snapshots.map { it.second.items }
    val currentWindowIndex = snapshots.map { it.second.currentIndex }
    val queueTitle = snapshots.map { it.second.title }

    private fun progress(): ProgressState {
        val position = player.currentPosition.coerceAtLeast(0L)
        val duration = player.duration.coerceAtLeast(0L)
        return ProgressState(position, duration, if (duration > 0L) position.toFloat() / duration else 0f)
    }

    val progressState: StateFlow<ProgressState> = isPageActiveAndResumed.flatMapLatest { active ->
        if (!active) emptyFlow() else flow {
            while (currentCoroutineContext().isActive) {
                emit(withContext(Dispatchers.Main.immediate) { progress() })
                delay(66L)
            }
        }
    }.distinctUntilChanged().stateIn(scope, SharingStarted.WhileSubscribed(0), progress())
}
