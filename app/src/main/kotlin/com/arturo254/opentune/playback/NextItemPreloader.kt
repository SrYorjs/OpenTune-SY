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
        val network = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = network?.getNetworkCapabilities(network.activeNetwork) ?: return
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return
        if (context.getSystemService(PowerManager::class.java)?.isPowerSaveMode != false) return
        val battery = context.getSystemService(BatteryManager::class.java) ?: return
        if (!battery.isCharging && battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) < 25) return
        if (config.uri.scheme !in setOf("https", "http")) return
        key = item.mediaId
        job = scope.launch(Dispatchers.IO) {
            previous?.join()
            ensureActive()
            val source = factory().createDataSource()
            val deadline = launch { delay(5000); runCatching { source.close() } }
            try {
                active.set(source)
                ensureActive()
                source.open(DataSpec.Builder().setUri(config.uri).setKey(config.customCacheKey ?: item.mediaId)
                    .setLength(262144).build())
                val buffer = ByteArray(32768)
                var left = 262144
                while (left > 0) {
                    ensureActive()
                    val read = source.read(buffer, 0, minOf(left, buffer.size))
                    if (read < 0) break
                    left -= read
                }
            } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) {
            } finally {
                deadline.cancel()
                active.compareAndSet(source, null)
                runCatching { source.close() }
            }
        }
    }
}
