package com.arturo254.opentune.playback

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.constants.PersistentQueueKey
import com.arturo254.opentune.extensions.currentMetadata
import com.arturo254.opentune.extensions.mediaItems
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.models.PersistPlayerState
import com.arturo254.opentune.models.PersistQueue
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.time.Duration.Companion.seconds
import timber.log.Timber

internal class PlaybackPersistenceController(
    private val context: Context,
    private val player: ExoPlayer,
    private val queueController: QueueController,
    private val automixRadioController: AutomixRadioController,
    private val dataStore: DataStore<Preferences>,
    private val scopeProvider: () -> CoroutineScope,
    private val playerVolume: MutableStateFlow<Float>,
    private val currentMediaMetadata: MutableStateFlow<MediaMetadata?>,
    private val onRestored: () -> Unit,
    private val onError: (Throwable) -> Unit,
) {
    val queueRestoreCompleted = MutableStateFlow(false)

    private val persistentStateLock = Any()

    fun start() {
        scopeProvider().launch(Dispatchers.IO) {
            restoreIfEnabled()
        }

        scopeProvider().launch {
            while (isActive) {
                val interval =
                    if (player.isPlaying) {
                        10.seconds
                    } else {
                        30.seconds
                    }

                delay(interval)

                if (dataStore.get(PersistentQueueKey, true)) {
                    save()
                }
            }
        }
    }

    suspend fun saveIfEnabled() {
        if (!dataStore.get(PersistentQueueKey, true)) return
        save()
    }

    fun saveBlockingIfEnabled() {
        if (!dataStore.get(PersistentQueueKey, true)) return
        if (player.mediaItemCount <= 0) return

        val snapshot = captureSnapshot() ?: return

        runBlocking(Dispatchers.IO) {
            writeSnapshot(snapshot)
        }
    }

    private suspend fun restoreIfEnabled() {
        try {
            if (dataStore.get(PersistentQueueKey, true)) {
                readPersistentObject<PersistQueue>(
                    PERSISTENT_QUEUE_FILE,
                )?.let { persistedQueue ->
                    queueController.restorePersistentQueue(
                        persistedQueue,
                    )
                }

                readPersistentObject<PersistQueue>(
                    PERSISTENT_AUTOMIX_FILE,
                )?.let { persistedAutomix ->
                    val items =
                        persistedAutomix.items.map {
                            it.toMediaItem()
                        }

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

                readPersistentObject<PersistPlayerState>(
                    PERSISTENT_PLAYER_STATE_FILE,
                )?.let { playerState ->
                    delay(1_000)

                    withContext(Dispatchers.Main) {
                        player.repeatMode =
                            playerState.repeatMode
                        player.shuffleModeEnabled =
                            playerState.shuffleModeEnabled
                        playerVolume.value =
                            playerState.volume

                        if (player.mediaItemCount > 0) {
                            val index =
                                if (
                                    playerState
                                        .currentMediaItemIndex in
                                        0 until
                                            player.mediaItemCount
                                ) {
                                    playerState
                                        .currentMediaItemIndex
                                } else {
                                    player
                                        .currentMediaItemIndex
                                        .coerceIn(
                                            0,
                                            player.mediaItemCount - 1,
                                        )
                                }

                            player.seekTo(
                                index,
                                playerState.currentPosition,
                            )
                        }

                        currentMediaMetadata.value =
                            player.currentMetadata
                        onRestored()
                    }
                }
            }
        } finally {
            withContext(Dispatchers.Main) {
                queueRestoreCompleted.value = true
            }
        }
    }

    suspend fun save() {
        val snapshot =
            withContext(Dispatchers.Main) {
                captureSnapshot()
            } ?: return

        withContext(Dispatchers.IO) {
            writeSnapshot(snapshot)
        }
    }

    private fun captureSnapshot(): Snapshot? {
        val mediaItemsSnapshot =
            player.mediaItems
                .mapNotNull {
                    it.toPersistableMetadata()
                }

        if (mediaItemsSnapshot.isEmpty()) {
            return null
        }

        return Snapshot(
            queueItems = mediaItemsSnapshot,
            currentMediaItemIndex =
                player.currentMediaItemIndex,
            currentPosition =
                player.currentPosition,
            automixItems =
                automixRadioController
                    .automixItems
                    .value
                    .mapNotNull {
                        it.metadata
                    },
            playWhenReady =
                player.playWhenReady,
            repeatMode =
                player.repeatMode,
            shuffleModeEnabled =
                player.shuffleModeEnabled,
            volume =
                playerVolume.value,
            playbackState =
                player.playbackState,
        )
    }

    private fun writeSnapshot(snapshot: Snapshot) {
        val persistQueue =
            queueController.toPersistQueue(
                items = snapshot.queueItems,
                mediaItemIndex =
                    snapshot.currentMediaItemIndex,
                position = snapshot.currentPosition,
            )

        val persistAutomix =
            PersistQueue(
                title = "automix",
                items = snapshot.automixItems,
                mediaItemIndex = 0,
                position = 0,
            )

        val persistPlayerState =
            PersistPlayerState(
                playWhenReady = snapshot.playWhenReady,
                repeatMode = snapshot.repeatMode,
                shuffleModeEnabled =
                    snapshot.shuffleModeEnabled,
                volume = snapshot.volume,
                currentPosition =
                    snapshot.currentPosition,
                currentMediaItemIndex =
                    snapshot.currentMediaItemIndex,
                playbackState =
                    snapshot.playbackState,
            )

        writePersistentObject(
            PERSISTENT_QUEUE_FILE,
            persistQueue,
        )
        writePersistentObject(
            PERSISTENT_AUTOMIX_FILE,
            persistAutomix,
        )
        writePersistentObject(
            PERSISTENT_PLAYER_STATE_FILE,
            persistPlayerState,
        )
    }

    private inline fun <reified T> readPersistentObject(
        fileName: String,
    ): T? {
        val persistentFile =
            context.filesDir.resolve(fileName)

        if (
            !persistentFile.exists() ||
            !persistentFile.isFile
        ) {
            return null
        }

        return synchronized(persistentStateLock) {
            runCatching {
                persistentFile
                    .inputStream()
                    .use { inputStream ->
                        ObjectInputStream(
                            inputStream,
                        ).use { input ->
                            input.readObject() as? T
                        }
                    }
            }.onFailure { error ->
                Timber
                    .tag(TAG)
                    .w(
                        error,
                        "Failed to read persistent file: $fileName",
                    )
                runCatching {
                    persistentFile.delete()
                }
            }.getOrNull()
        }
    }

    private fun writePersistentObject(
        fileName: String,
        payload: Serializable,
    ) {
        val persistentFile =
            context.filesDir.resolve(fileName)
        val tempFile =
            context.filesDir.resolve(
                "$fileName.tmp",
            )

        synchronized(persistentStateLock) {
            runCatching {
                FileOutputStream(tempFile)
                    .use { outputStream ->
                        ObjectOutputStream(
                            outputStream,
                        ).use { output ->
                            output.writeObject(payload)
                            output.flush()
                        }
                        outputStream.fd.sync()
                    }

                if (
                    persistentFile.exists() &&
                    !persistentFile.delete()
                ) {
                    error(
                        "Could not replace $fileName",
                    )
                }

                if (
                    !tempFile.renameTo(
                        persistentFile,
                    )
                ) {
                    error(
                        "Could not atomically move $fileName",
                    )
                }
            }.onFailure { error ->
                runCatching {
                    tempFile.delete()
                }
                onError(error)
            }
        }
    }

    private fun MediaItem.toPersistableMetadata():
        MediaMetadata? {
        val tagged = metadata
        if (tagged != null) return tagged

        val id =
            mediaId
                .trim()
                .ifBlank {
                    localConfiguration
                        ?.uri
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                }.takeIf {
                    it.isNotBlank()
                } ?: return null

        val title =
            mediaMetadata
                .title
                ?.toString()
                ?.trim()
                .takeIf {
                    !it.isNullOrBlank()
                }
                ?: id

        val artistText =
            mediaMetadata
                .artist
                ?.toString()
                ?.trim()
                .takeIf {
                    !it.isNullOrBlank()
                }
                ?: mediaMetadata
                    .subtitle
                    ?.toString()
                    ?.trim()
                    .takeIf {
                        !it.isNullOrBlank()
                    }

        val artists =
            artistText
                ?.split(",")
                ?.mapNotNull {
                    it
                        .trim()
                        .takeIf(
                            String::isNotBlank,
                        )
                }?.map { name ->
                    MediaMetadata.Artist(
                        id = null,
                        name = name,
                    )
                }.orEmpty()

        val thumbnailUrl =
            mediaMetadata
                .artworkUri
                ?.toString()

        val albumTitle =
            mediaMetadata
                .albumTitle
                ?.toString()
                ?.trim()
                .takeIf {
                    !it.isNullOrBlank()
                }

        val album =
            albumTitle?.let { value ->
                MediaMetadata.Album(
                    id = value,
                    title = value,
                )
            }

        return MediaMetadata(
            id = id,
            title = title,
            artists = artists,
            duration = -1,
            thumbnailUrl = thumbnailUrl,
            album = album,
        )
    }

    private data class Snapshot(
        val queueItems: List<MediaMetadata>,
        val currentMediaItemIndex: Int,
        val currentPosition: Long,
        val automixItems: List<MediaMetadata>,
        val playWhenReady: Boolean,
        val repeatMode: Int,
        val shuffleModeEnabled: Boolean,
        val volume: Float,
        val playbackState: Int,
    )

    private companion object {
        const val TAG =
            "PlaybackPersistence"

        const val PERSISTENT_QUEUE_FILE =
            "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE =
            "persistent_automix.data"
        const val PERSISTENT_PLAYER_STATE_FILE =
            "persistent_player_state.data"
    }
}
