package com.arturo254.opentune.playback

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.*

internal class NextItemPreloader(private val context: Context, private val scope: CoroutineScope) {
    private var job: Job? = null
    private var key: String? = null
    private val active = AtomicReference<DataSource?>()

    fun cancel() {
        job?.cancel()
        key = null
        val old = active.getAndSet(null)
        if (old != null) Dispatchers.IO.dispatch(kotlin.coroutines.EmptyCoroutineContext, Runnable { runCatching { old.close() } })
    }

    fun prepare(item: MediaItem?, factory: () -> DataSource.Factory) {
        val config = item?.localConfiguration ?: return
        if (item.mediaId == key) return
        val previous = job
        cancel()
        if (!permitted()) return
        if (!canPreloadAudio(item.mediaId, config.uri.scheme)) return
        key = item.mediaId
        job = scope.launch(Dispatchers.IO) {
            delay(1_200L)
            previous?.join()
            ensureActive()
            if (!permitted()) return@launch
            val source = factory().createDataSource()
            try {
                active.set(source)
                ensureActive()
                preloadAudioPrefix(source, DataSpec.Builder().setUri(config.uri).setKey(config.customCacheKey ?: item.mediaId).build())
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) {
            } finally {
                active.compareAndSet(source, null)
                runCatching { source.close() }
            }
        }
    }

    private fun permitted(): Boolean {
        val network = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = network.getNetworkCapabilities(network.activeNetwork) ?: return false
        val battery = context.getSystemService(BatteryManager::class.java) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == false &&
            (battery.isCharging || battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) >= 25)
    }
}

internal fun canPreloadAudio(mediaId: String, scheme: String?): Boolean =
    scheme in setOf("https", "http") ||
        (scheme == null && mediaId.matches(Regex("[A-Za-z0-9_-]{11}")))

internal suspend fun preloadAudioPrefix(source: DataSource, spec: DataSpec) = withTimeout(5_000L) {
    runInterruptible(Dispatchers.IO) {
        source.open(spec.buildUpon().setLength(262144).build())
        val buffer = ByteArray(32768)
        var left = 262144
        while (left > 0 && !Thread.currentThread().isInterrupted) {
            val read = source.read(buffer, 0, minOf(left, buffer.size))
            if (read < 0) break
            left -= read
        }
    }
}
