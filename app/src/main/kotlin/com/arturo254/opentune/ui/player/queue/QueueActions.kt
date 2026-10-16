package com.arturo254.opentune.ui.player.queue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.together.ControlAction
import com.arturo254.opentune.together.TogetherRole
import com.arturo254.opentune.together.TogetherSessionState
import com.arturo254.opentune.ui.player.actions.PlayerActions

/**
 * All queue-screen mutations flow through this adapter.
 * MusicService / QueueController / Media3 still own the actual playback queue;
 * this layer never persists a second queue or overrides Together permissions.
 */
class QueueActions internal constructor(private val backend: QueueActionBackend) {
    fun toggleShuffle() = backend.toggleShuffle()
    fun toggleRepeat() = backend.toggleRepeat()

    /** The displayed order is already shuffled by Player.getQueueWindows(). */
    fun reorder(from: Int, to: Int, displayedPhysicalIndices: List<Int>, shuffled: Boolean): Boolean {
        val reordered = movedQueueOrder(displayedPhysicalIndices, from, to) ?: return false
        if (from == to) return false
        if (shuffled) backend.setShuffleOrder(reordered)
        else backend.moveMediaItem(from, to)
        return true
    }

    fun removeAt(physicalIndex: Int): Boolean {
        if (physicalIndex !in 0 until backend.mediaItemCount) return false
        backend.removeMediaItem(physicalIndex)
        return true
    }

    fun restore(item: MediaItem, formerPhysicalIndex: Int) {
        backend.restoreMediaItem(item, formerPhysicalIndex)
    }

    /**
     * Selecting the active song retains play/pause; otherwise Together guests
     * send a track request rather than seeking the local player.
     * The UI owns toasts and scrolling, according to the returned outcome.
     */
    fun select(
        isCurrent: Boolean,
        physicalIndex: Int,
        trackId: String?,
    ): QueueSelectionResult {
        if (isCurrent) {
            backend.togglePlayPause()
            return QueueSelectionResult.TOGGLED_CURRENT
        }
        return when (backend.guestPermission) {
            QueueGuestPermission.DENIED -> QueueSelectionResult.DENIED
            QueueGuestPermission.ALLOWED -> {
                val id = trackId?.trim().orEmpty()
                if (id.isBlank()) QueueSelectionResult.INVALID
                else {
                    backend.requestTogetherTrack(id)
                    QueueSelectionResult.REQUESTED
                }
            }
            QueueGuestPermission.NOT_GUEST -> {
                if (physicalIndex !in 0 until backend.mediaItemCount) {
                    QueueSelectionResult.INVALID
                } else {
                    backend.playAt(physicalIndex)
                    QueueSelectionResult.PLAYED
                }
            }
        }
    }
}

enum class QueueSelectionResult { TOGGLED_CURRENT, DENIED, INVALID, REQUESTED, PLAYED }
internal enum class QueueGuestPermission { NOT_GUEST, DENIED, ALLOWED }

internal interface QueueActionBackend {
    val mediaItemCount: Int
    val guestPermission: QueueGuestPermission
    fun togglePlayPause()
    fun toggleShuffle()
    fun toggleRepeat()
    fun playAt(physicalIndex: Int)
    fun requestTogetherTrack(trackId: String)
    fun removeMediaItem(physicalIndex: Int)
    fun restoreMediaItem(item: MediaItem, formerPhysicalIndex: Int)
    fun moveMediaItem(from: Int, to: Int)
    fun setShuffleOrder(indices: IntArray)
}

private class PlayerConnectionQueueBackend(
    private val connection: PlayerConnection,
    private val playerActions: PlayerActions,
) : QueueActionBackend {
    private val player get() = connection.player
    override val mediaItemCount: Int get() = player.mediaItemCount
    override val guestPermission: QueueGuestPermission
        get() {
            val joined = connection.service.togetherSessionState.value as? TogetherSessionState.Joined
            if (joined?.role !is TogetherRole.Guest) return QueueGuestPermission.NOT_GUEST
            return if (joined.roomState.settings.allowGuestsToControlPlayback) {
                QueueGuestPermission.ALLOWED
            } else QueueGuestPermission.DENIED
        }

    override fun togglePlayPause() = playerActions.playPause()
    override fun toggleShuffle() = playerActions.toggleShuffle()
    override fun toggleRepeat() = playerActions.toggleRepeat()
    override fun playAt(physicalIndex: Int) {
        player.seekToDefaultPosition(physicalIndex)
        player.playWhenReady = true
    }
    override fun requestTogetherTrack(trackId: String) {
        connection.service.requestTogetherControl(
            ControlAction.SeekToTrack(trackId = trackId, positionMs = 0L),
        )
    }
    override fun removeMediaItem(physicalIndex: Int) = player.removeMediaItem(physicalIndex)
    override fun restoreMediaItem(item: MediaItem, formerPhysicalIndex: Int) {
        player.addMediaItem(item)
        val last = player.mediaItemCount - 1
        if (last >= 0) player.moveMediaItem(last, formerPhysicalIndex.coerceIn(0, last))
    }
    override fun moveMediaItem(from: Int, to: Int) = player.moveMediaItem(from, to)
    override fun setShuffleOrder(indices: IntArray) {
        player.setShuffleOrder(DefaultShuffleOrder(indices, System.currentTimeMillis()))
    }
}

@Composable
fun rememberQueueActions(connection: PlayerConnection, playerActions: PlayerActions): QueueActions =
    remember(connection, playerActions) {
        QueueActions(PlayerConnectionQueueBackend(connection, playerActions))
    }
