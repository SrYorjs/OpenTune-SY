package com.arturo254.opentune.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AutoLoadMoreKey
import com.arturo254.opentune.constants.HideExplicitKey
import com.arturo254.opentune.constants.HideVideoKey
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.WatchEndpoint
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.playback.queues.filterExplicit
import com.arturo254.opentune.playback.queues.filterVideo
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

internal class AutomixRadioController(
    private val context: Context,
    private val player: ExoPlayer,
    private val queueController: QueueController,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val addToQueue: (List<MediaItem>) -> Unit,
    private val playNext: (List<MediaItem>) -> Unit,
) {
    val automixItems =
        MutableStateFlow<List<MediaItem>>(emptyList())
    val automixLoading = MutableStateFlow(false)
    val automixError =
        MutableStateFlow<String?>(null)

    val autoAddedMediaIds: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf())

    private var automixJob: Job? = null
    private var automixSeedMediaId: String? = null

    fun startRadioSeamlessly() {
        queueController.suppressAutoPlayback = false

        val currentMetadata =
            player.currentMetadata ?: return
        val currentIndex = player.currentMediaItemIndex
        val currentMediaId = currentMetadata.id

        scopeProvider().launch(SilentHandler) {
            val radioQueue =
                YouTubeQueue(
                    endpoint =
                        WatchEndpoint(
                            videoId = currentMediaId,
                        ),
                )

            val initialStatus =
                withContext(Dispatchers.IO) {
                    radioQueue
                        .getInitialStatus()
                        .filterExplicit(
                            dataStore.get(
                                HideExplicitKey,
                                false,
                            ),
                        ).filterVideo(
                            dataStore.get(
                                HideVideoKey,
                                false,
                            ),
                        )
                }

            initialStatus.title?.let {
                queueController.queueTitle = it
            }

            val radioItems =
                initialStatus.items.filter { item ->
                    item.mediaId != currentMediaId
                }

            if (radioItems.isNotEmpty()) {
                val itemCount = player.mediaItemCount

                if (itemCount > currentIndex + 1) {
                    player.removeMediaItems(
                        currentIndex + 1,
                        itemCount,
                    )
                }

                player.addMediaItems(
                    currentIndex + 1,
                    radioItems,
                )
            }

            queueController.currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scopeProvider().launch(
            Dispatchers.IO + SilentHandler,
        ) {
            YouTube
                .album(albumId)
                .onSuccess {
                    getAutomix(it.album.playlistId)
                }
        }
    }

    fun getAutomix(playlistId: String) {
        if (
            !dataStore.get(AutoLoadMoreKey, true) ||
            player.repeatMode != REPEAT_MODE_OFF
        ) {
            return
        }

        scopeProvider().launch(
            Dispatchers.IO + SilentHandler,
        ) {
            val seedAtRequest =
                withContext(Dispatchers.Main) {
                    player
                        .currentMetadata
                        ?.id
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }

            YouTube
                .next(
                    WatchEndpoint(
                        playlistId = playlistId,
                    ),
                ).onSuccess { first ->
                    YouTube
                        .next(
                            WatchEndpoint(
                                playlistId =
                                    first.endpoint.playlistId,
                            ),
                        ).onSuccess { second ->
                            val mediaItems =
                                second.items.map { song ->
                                    song.toMediaItem()
                                }

                            withContext(Dispatchers.Main) {
                                val currentSeed =
                                    player
                                        .currentMetadata
                                        ?.id
                                        ?.trim()
                                        ?.takeIf {
                                            it.isNotBlank()
                                        }

                                if (
                                    seedAtRequest != null &&
                                    currentSeed != seedAtRequest
                                ) {
                                    return@withContext
                                }

                                automixItems.value =
                                    mediaItems
                                automixSeedMediaId =
                                    currentSeed
                            }
                        }
                }
        }
    }

    fun addToQueueAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value
                .toMutableList()
                .apply {
                    if (position in indices) {
                        removeAt(position)
                    }
                }

        addToQueue(listOf(item))
    }

    fun playNextAutomix(
        item: MediaItem,
        position: Int,
    ) {
        automixItems.value =
            automixItems.value
                .toMutableList()
                .apply {
                    if (position in indices) {
                        removeAt(position)
                    }
                }

        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixJob?.cancel()
        automixJob = null
        automixItems.value = emptyList()
        automixLoading.value = false
        automixError.value = null
        automixSeedMediaId = null
    }

    fun resetForNewQueue() {
        clearAutomix()
        autoAddedMediaIds.clear()
    }

    fun restoreAutomix(
        items: List<MediaItem>,
        currentMediaId: String?,
    ) {
        automixItems.value = items
        automixSeedMediaId =
            currentMediaId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
    }

    fun refreshForCurrentMedia(force: Boolean) {
        if (
            !AutomixPolicy.shouldRun(
                autoLoadMoreEnabled =
                    dataStore.get(
                        AutoLoadMoreKey,
                        true,
                    ),
                repeatModeOff =
                    player.repeatMode ==
                        REPEAT_MODE_OFF,
                suppressAutoPlayback =
                    queueController.suppressAutoPlayback,
                mediaItemCount =
                    player.mediaItemCount,
            )
        ) {
            return
        }

        val currentMeta =
            player.currentMetadata ?: return
        val seedMediaId =
            currentMeta.id
                .trim()
                .ifBlank { return }

        if (
            !AutomixPolicy.shouldRefreshCurrentSeed(
                force = force,
                sameSeed =
                    automixSeedMediaId == seedMediaId,
                hasItems =
                    automixItems.value.isNotEmpty(),
                jobActive =
                    automixJob?.isActive == true,
            )
        ) {
            return
        }

        automixJob?.cancel()
        automixJob = null
        automixItems.value = emptyList()
        automixLoading.value = true
        automixError.value = null
        automixSeedMediaId = seedMediaId

        val hideExplicit =
            dataStore.get(HideExplicitKey, false)
        val hideVideo =
            dataStore.get(HideVideoKey, false)

        automixJob =
            scopeProvider().launch {
                try {
                    val nextResult =
                        withContext(Dispatchers.IO) {
                            YouTube.next(
                                WatchEndpoint(
                                    videoId =
                                        seedMediaId,
                                ),
                            )
                        }

                    nextResult
                        .onSuccess { result ->
                            if (
                                automixSeedMediaId !=
                                seedMediaId
                            ) {
                                automixLoading.value =
                                    false
                                return@onSuccess
                            }

                            val queueIds =
                                currentQueueIds()

                            val fromNext =
                                result.items
                                    .map {
                                        it.toMediaItem()
                                    }.filter {
                                        it.mediaId !in
                                            queueIds
                                    }.filterExplicit(
                                        hideExplicit,
                                    ).filterVideo(
                                        hideVideo,
                                    )

                            val relatedCandidates =
                                result.relatedEndpoint
                                    ?.let { endpoint ->
                                        withContext(
                                            Dispatchers.IO,
                                        ) {
                                            YouTube.related(
                                                endpoint,
                                            )
                                        }.getOrNull()
                                            ?.songs
                                            .orEmpty()
                                    }.orEmpty()

                            val related =
                                relatedCandidates
                                    .map {
                                        it.toMediaItem()
                                    }.filter {
                                        it.mediaId !in
                                            queueIds
                                    }.filterExplicit(
                                        hideExplicit,
                                    ).filterVideo(
                                        hideVideo,
                                    )

                            val poolBase =
                                (fromNext + related)
                                    .asSequence()
                                    .distinctBy {
                                        it.mediaId
                                    }.take(50)
                                    .toList()

                            val pool =
                                if (
                                    poolBase.size >= 25 ||
                                    result
                                        .endpoint
                                        .playlistId
                                        .isNullOrBlank()
                                ) {
                                    poolBase
                                } else {
                                    val playlistId =
                                        result
                                            .endpoint
                                            .playlistId

                                    val extra =
                                        withContext(
                                            Dispatchers.IO,
                                        ) {
                                            YouTube.next(
                                                WatchEndpoint(
                                                    playlistId =
                                                        playlistId,
                                                ),
                                            )
                                        }.getOrNull()
                                            ?.items
                                            .orEmpty()
                                            .map {
                                                it.toMediaItem()
                                            }.filter {
                                                it.mediaId !in
                                                    queueIds
                                            }.filterExplicit(
                                                hideExplicit,
                                            ).filterVideo(
                                                hideVideo,
                                            )

                                    (poolBase + extra)
                                        .asSequence()
                                        .distinctBy {
                                            it.mediaId
                                        }.take(75)
                                        .toList()
                                }

                            if (
                                automixSeedMediaId !=
                                seedMediaId
                            ) {
                                automixLoading.value =
                                    false
                                return@onSuccess
                            }

                            automixItems.value = pool
                            if (pool.isEmpty()) {
                                automixError.value =
                                    context.getString(
                                        R.string.error_no_similar_songs,
                                    )
                            }
                            automixLoading.value = false
                        }.onFailure { throwable ->
                            if (
                                automixSeedMediaId ==
                                seedMediaId
                            ) {
                                automixLoading.value =
                                    false
                                automixError.value =
                                    throwable
                                        .localizedMessage
                                        ?: context.getString(
                                            R.string.error_automix_failed,
                                        )
                            }
                        }
                } catch (
                    cancellation: CancellationException
                ) {
                    throw cancellation
                } catch (error: Exception) {
                    if (
                        automixSeedMediaId ==
                        seedMediaId
                    ) {
                        automixLoading.value = false
                        automixError.value =
                            error.localizedMessage
                                ?: context.getString(
                                    R.string.error_automix_failed,
                                )
                    }
                }
            }
    }

    fun onInfiniteQueueDisabled() {
        automixJob?.cancel()
        automixJob = null
        automixLoading.value = false
        automixError.value = null

        val currentIndex =
            player.currentMediaItemIndex
        val idsToRemove =
            synchronized(autoAddedMediaIds) {
                autoAddedMediaIds.toSet()
            }

        if (idsToRemove.isEmpty()) {
            clearAutomix()
            return
        }

        for (
            index in
            player.mediaItemCount - 1 downTo 0
        ) {
            if (index == currentIndex) continue

            if (
                player
                    .getMediaItemAt(index)
                    .mediaId in idsToRemove
            ) {
                player.removeMediaItem(index)
            }
        }

        autoAddedMediaIds.clear()
        clearAutomix()
    }

    fun onInfiniteQueueEnabled() {
        val currentMeta = player.currentMetadata
        if (currentMeta == null) {
            automixError.value =
                context.getString(
                    R.string.error_no_song_playing,
                )
            return
        }

        automixJob?.cancel()
        automixLoading.value = true
        automixError.value = null
        automixItems.value = emptyList()
        automixSeedMediaId =
            currentMeta.id
                .trim()
                .ifBlank { null }

        val hideExplicit =
            dataStore.get(HideExplicitKey, false)
        val hideVideo =
            dataStore.get(HideVideoKey, false)

        automixJob =
            scopeProvider().launch {
                try {
                    val nextResult =
                        withContext(Dispatchers.IO) {
                            YouTube.next(
                                WatchEndpoint(
                                    videoId =
                                        currentMeta.id,
                                ),
                            )
                        }

                    nextResult
                        .onSuccess { result ->
                            if (
                                queueController
                                    .suppressAutoPlayback ||
                                player.playbackState ==
                                    Player.STATE_IDLE ||
                                player.mediaItemCount == 0
                            ) {
                                automixLoading.value =
                                    false
                                return@onSuccess
                            }

                            val initialQueueIds =
                                currentQueueIds()
                            val filteredFromNext =
                                result.items
                                    .map {
                                        it.toMediaItem()
                                    }.filter {
                                        it.mediaId !in
                                            initialQueueIds
                                    }.filterExplicit(
                                        hideExplicit,
                                    ).filterVideo(
                                        hideVideo,
                                    )

                            val addedNow =
                                ArrayList<MediaItem>(
                                    32,
                                )

                            if (
                                filteredFromNext
                                    .isNotEmpty()
                            ) {
                                val toAdd =
                                    filteredFromNext
                                        .take(25)
                                player.addMediaItems(toAdd)
                                toAdd.forEach {
                                    autoAddedMediaIds
                                        .add(
                                            it.mediaId,
                                        )
                                }
                                addedNow.addAll(toAdd)
                            }

                            val queueIdsAfterNext =
                                currentQueueIds()

                            val relatedCandidates =
                                result.relatedEndpoint
                                    ?.let {
                                        relatedEndpoint ->
                                        withContext(
                                            Dispatchers.IO,
                                        ) {
                                            YouTube.related(
                                                relatedEndpoint,
                                            )
                                        }.getOrNull()
                                            ?.songs
                                            .orEmpty()
                                    }.orEmpty()

                            val filteredRelated =
                                relatedCandidates
                                    .map {
                                        it.toMediaItem()
                                    }.filter {
                                        it.mediaId !in
                                            queueIdsAfterNext
                                    }.filterExplicit(
                                        hideExplicit,
                                    ).filterVideo(
                                        hideVideo,
                                    )

                            if (
                                addedNow.isEmpty() &&
                                filteredRelated
                                    .isNotEmpty()
                            ) {
                                val toAdd =
                                    filteredRelated
                                        .take(25)
                                player.addMediaItems(toAdd)
                                toAdd.forEach {
                                    autoAddedMediaIds
                                        .add(
                                            it.mediaId,
                                        )
                                }
                                addedNow.addAll(toAdd)
                            }

                            val queueIdsAfterAdds =
                                currentQueueIds()
                            val playlistId =
                                result
                                    .endpoint
                                    .playlistId

                            val automixCandidates =
                                if (
                                    playlistId
                                        .isNullOrBlank()
                                ) {
                                    emptyList()
                                } else {
                                    withContext(
                                        Dispatchers.IO,
                                    ) {
                                        YouTube.next(
                                            WatchEndpoint(
                                                playlistId =
                                                    playlistId,
                                            ),
                                        )
                                    }.getOrNull()
                                        ?.items
                                        .orEmpty()
                                }

                            val filteredAutomix =
                                automixCandidates
                                    .map {
                                        it.toMediaItem()
                                    }.filter {
                                        it.mediaId !in
                                            queueIdsAfterAdds
                                    }.filterExplicit(
                                        hideExplicit,
                                    ).filterVideo(
                                        hideVideo,
                                    )

                            val addedIds =
                                addedNow
                                    .map {
                                        it.mediaId
                                    }.toSet()

                            val pool =
                                (
                                    filteredFromNext +
                                        filteredRelated +
                                        filteredAutomix
                                )
                                    .asSequence()
                                    .distinctBy {
                                        it.mediaId
                                    }.filter {
                                        it.mediaId !in
                                            addedIds
                                    }.take(75)
                                    .toList()

                            automixItems.value = pool

                            if (
                                addedNow.isEmpty() &&
                                pool.isEmpty()
                            ) {
                                automixError.value =
                                    context.getString(
                                        R.string.error_no_similar_songs,
                                    )
                            }

                            automixLoading.value =
                                false
                        }.onFailure { throwable ->
                            automixLoading.value = false
                            automixError.value =
                                throwable
                                    .localizedMessage
                                    ?: context.getString(
                                        R.string.error_automix_failed,
                                    )
                        }
                } catch (
                    cancellation: CancellationException
                ) {
                    throw cancellation
                } catch (error: Exception) {
                    automixLoading.value = false
                    automixError.value =
                        error.localizedMessage
                            ?: context.getString(
                                R.string.error_automix_failed,
                            )
                }
            }
    }

    fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
        timelineEmpty: Boolean,
        queueHasNextPage: Boolean,
    ) {
        if (
            !timelineEmpty &&
            dataStore.get(AutoLoadMoreKey, true) &&
            reason !=
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.repeatMode == REPEAT_MODE_OFF
        ) {
            val nearEndWithoutPaging =
                player.mediaItemCount -
                    player.currentMediaItemIndex <= 3 &&
                    !queueHasNextPage

            if (!nearEndWithoutPaging) {
                val force =
                    reason ==
                        Player.MEDIA_ITEM_TRANSITION_REASON_SEEK ||
                        reason ==
                        Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED

                val currentId =
                    (
                        mediaItem?.currentMetadataOrNull()
                            ?: player.currentMetadata
                    )
                        ?.id
                        ?.trim()
                        .orEmpty()

                if (
                    force ||
                    (
                        currentId.isNotBlank() &&
                            automixSeedMediaId !=
                            currentId
                    )
                ) {
                    refreshForCurrentMedia(force)
                }
            }
        }

        if (
            queueController.suppressAutoPlayback ||
            timelineEmpty ||
            !dataStore.get(AutoLoadMoreKey, true) ||
            reason ==
                Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT ||
            player.repeatMode != REPEAT_MODE_OFF ||
            player.mediaItemCount -
                player.currentMediaItemIndex > 3 ||
            queueHasNextPage
        ) {
            return
        }

        scopeProvider().launch(SilentHandler) {
            if (
                queueController.suppressAutoPlayback ||
                player.mediaItemCount == 0
            ) {
                return@launch
            }

            val queueIds = currentQueueIds()
            val currentMetadata =
                player.currentMetadata
            val currentMediaId =
                currentMetadata
                    ?.id
                    ?.trim()
                    .orEmpty()
            val existingSeed =
                automixSeedMediaId
                    ?.trim()
                    .orEmpty()

            val existingAutomix =
                if (
                    currentMediaId.isNotBlank() &&
                    existingSeed ==
                        currentMediaId
                ) {
                    automixItems.value
                } else {
                    if (automixItems.value.isNotEmpty()) {
                        clearAutomix()
                    }
                    emptyList()
                }

            if (existingAutomix.isNotEmpty()) {
                if (
                    player.playbackState ==
                    Player.STATE_IDLE
                ) {
                    return@launch
                }

                val filtered =
                    existingAutomix.filter {
                        it.mediaId !in queueIds
                    }

                if (filtered.isNotEmpty()) {
                    player.addMediaItems(filtered)
                    filtered.forEach {
                        autoAddedMediaIds.add(
                            it.mediaId,
                        )
                    }
                }

                clearAutomix()
            } else if (currentMetadata != null) {
                refreshForCurrentMedia(
                    force = true,
                )
            }
        }
    }

    fun onPlaybackStateChanged(
        playbackState: Int,
    ) {
        if (
            queueController.suppressAutoPlayback ||
            playbackState != Player.STATE_ENDED ||
            !dataStore.get(AutoLoadMoreKey, true) ||
            player.repeatMode != REPEAT_MODE_OFF ||
            player.currentMediaItem == null
        ) {
            return
        }

        scopeProvider().launch(SilentHandler) {
            if (
                queueController.suppressAutoPlayback ||
                player.playbackState ==
                    Player.STATE_IDLE ||
                player.mediaItemCount == 0
            ) {
                return@launch
            }

            val lastMetadata =
                player.currentMetadata
            val existingAutomix =
                automixItems.value

            if (existingAutomix.isNotEmpty()) {
                val filtered =
                    existingAutomix.filter {
                        it.mediaId !=
                            lastMetadata?.id
                    }

                if (filtered.isNotEmpty()) {
                    autoAddedMediaIds.clear()
                    player.setMediaItems(
                        filtered,
                        0,
                        0,
                    )
                    player.prepare()
                    player.play()
                    filtered.forEach {
                        autoAddedMediaIds.add(
                            it.mediaId,
                        )
                    }
                }

                clearAutomix()
                return@launch
            }

            if (lastMetadata == null) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                YouTube.next(
                    WatchEndpoint(
                        videoId =
                            lastMetadata.id,
                    ),
                )
            }.onSuccess { nextResult ->
                if (
                    queueController
                        .suppressAutoPlayback ||
                    player.playbackState ==
                        Player.STATE_IDLE ||
                    player.mediaItemCount == 0
                ) {
                    return@onSuccess
                }

                val hideExplicit =
                    dataStore.get(
                        HideExplicitKey,
                        false,
                    )
                val hideVideo =
                    dataStore.get(
                        HideVideoKey,
                        false,
                    )

                val radioItems =
                    nextResult.items
                        .map {
                            it.toMediaItem()
                        }.filter {
                            it.mediaId !=
                                lastMetadata.id
                        }.filterExplicit(
                            hideExplicit,
                        ).filterVideo(
                            hideVideo,
                        )

                if (radioItems.isEmpty()) {
                    return@onSuccess
                }

                autoAddedMediaIds.clear()
                player.setMediaItems(
                    radioItems,
                    0,
                    0,
                )
                player.prepare()
                player.play()
                radioItems.forEach {
                    autoAddedMediaIds.add(
                        it.mediaId,
                    )
                }

                withContext(Dispatchers.IO) {
                    YouTube.next(
                        WatchEndpoint(
                            playlistId =
                                nextResult
                                    .endpoint
                                    .playlistId,
                        ),
                    )
                }.onSuccess { result ->
                    if (
                        queueController
                            .suppressAutoPlayback ||
                        player.playbackState ==
                            Player.STATE_IDLE
                    ) {
                        return@onSuccess
                    }

                    automixItems.value =
                        result.items
                            .map {
                                it.toMediaItem()
                            }.filter {
                                it.mediaId !=
                                    lastMetadata.id
                            }.filterExplicit(
                                hideExplicit,
                            ).filterVideo(
                                hideVideo,
                            )
                }
            }
        }
    }

    private fun currentQueueIds(): Set<String> =
        (0 until player.mediaItemCount)
            .map {
                player
                    .getMediaItemAt(it)
                    .mediaId
            }.toSet()

    private fun MediaItem.currentMetadataOrNull() =
        metadata
}
