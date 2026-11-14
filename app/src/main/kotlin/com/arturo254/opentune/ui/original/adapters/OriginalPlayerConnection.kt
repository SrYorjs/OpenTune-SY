package com.arturo254.opentune.ui.original.adapters

import androidx.media3.common.Player
import com.arturo254.opentune.extensions.getCurrentQueueIndex
import com.arturo254.opentune.extensions.getQueueWindows
import com.arturo254.opentune.playback.PlayerConnection
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
    private val player = connection.player
    val mediaMetadata = connection.mediaMetadata
    val currentSong = connection.currentSong
    val shuffleModeEnabled = connection.shuffleModeEnabled
    val repeatMode = connection.repeatMode
    val playbackState = connection.playbackState

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

    val isEffectivelyPlaying = snapshots.map { it.first.isPlaying }
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
