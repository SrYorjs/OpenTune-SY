package com.arturo254.opentune.playback

import androidx.media3.common.MediaItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException

internal object StreamFallbacks {
    private val pending = ConcurrentHashMap<String, suspend () -> MediaItem?>()
    fun register(id: String, resolve: suspend () -> MediaItem?) {
        if (pending.size >= 64) pending.keys.firstOrNull()?.let(pending::remove)
        pending[id] = resolve
    }
    fun available(id: String) = pending.containsKey(id)
    suspend fun resolve(id: String): MediaItem? = try { pending.remove(id)?.invoke() }
        catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { null }
}
