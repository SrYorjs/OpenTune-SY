package com.arturo254.opentune.ui.original.component

import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.playback.DownloadUtil
import java.util.WeakHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class OriginalDownloadItem(val key: String, val artworkUrl: String?)
data class OriginalDownloadBatch(
    val isRunning: Boolean,
    val fraction: Float,
    val items: List<OriginalDownloadItem>,
)

private val originalDownloadStateLock = Any()
private val originalDownloadStates = WeakHashMap<DownloadUtil, StateFlow<OriginalDownloadBatch>>()

val DownloadUtil.batch: StateFlow<OriginalDownloadBatch>
    get() = synchronized(originalDownloadStateLock) {
        originalDownloadStates.getOrPut(this) {
            downloads.map { current ->
                val active = current.values.filter { download ->
                    download.state == Download.STATE_QUEUED ||
                        download.state == Download.STATE_DOWNLOADING ||
                        download.state == Download.STATE_RESTARTING
                }
                val measured = active.mapNotNull { download ->
                    when {
                        download.percentDownloaded in 0f..100f ->
                            download.percentDownloaded / 100f
                        download.contentLength > 0 ->
                            (download.bytesDownloaded.toFloat() / download.contentLength.toFloat()).coerceIn(0f, 1f)
                        else -> null
                    }
                }
                OriginalDownloadBatch(
                    isRunning = active.isNotEmpty(),
                    fraction = when {
                        active.isEmpty() -> 1f
                        measured.isEmpty() -> -1f
                        else -> measured.average().toFloat()
                    },
                    items = active.map { download ->
                        OriginalDownloadItem(
                            key = download.request.id,
                            artworkUrl = database.getSongByIdBlocking(download.request.id)?.song?.thumbnailUrl,
                        )
                    },
                )
            }.flowOn(Dispatchers.IO).stateIn(
                CoroutineScope(SupervisorJob() + Dispatchers.IO),
                SharingStarted.Eagerly,
                OriginalDownloadBatch(isRunning = false, fraction = 1f, items = emptyList()),
            )
        }
    }
