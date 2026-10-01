package com.arturo254.opentune.playback

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.opentune.constants.AutoDownloadOnLikeKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.utils.SyncUtils
import com.arturo254.opentune.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
internal class LibraryActionsController(
    private val context: Context,
    private val database: MusicDatabase,
    private val dataStore: DataStore<Preferences>,
    private val syncUtils: SyncUtils,
    private val currentMediaMetadata:
        MutableStateFlow<MediaMetadata?>,
    scope: CoroutineScope,
) {
    val currentSong: StateFlow<Song?> =
        currentMediaMetadata
            .flatMapLatest { mediaMetadata ->
                database.song(mediaMetadata?.id)
            }.flowOn(Dispatchers.IO)
            .stateIn(
                scope,
                SharingStarted.Lazily,
                null,
            )

    fun toggleLibrary() {
        database.query {
            currentSong.value?.let { song ->
                update(song.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let { current ->
                val song = current.song.toggleLike()
                update(song)
                syncUtils.likeSong(song)

                if (
                    dataStore.get(
                        AutoDownloadOnLikeKey,
                        false,
                    ) &&
                    song.liked
                ) {
                    val request =
                        DownloadRequest
                            .Builder(
                                song.id,
                                song.id.toUri(),
                            ).setCustomCacheKey(
                                song.id,
                            ).setData(
                                song.title.toByteArray(),
                            ).build()

                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        request,
                        false,
                    )
                }
            }
        }
    }
}
