package com.arturo254.opentune.playback

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.di.DownloadCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Copia audio COMPLETADO del caché Media3 a Music/GalaxOT mediante MediaStore.
 *
 * El audio ya reside en el teléfono: la VPS únicamente transmite los bytes.
 * El origen de la exportación nunca usa la red ni pide resolver la canción.
 * La copia exportada sigue siendo accesible desde Archivos aun si se borra
 * el caché de la aplicación.
 */
@Singleton
class UserMusicExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    @DownloadCache private val cache: Cache,
) {
    private val mutexes = ConcurrentHashMap<String, Mutex>()
    private val preferences by lazy {
        context.getSharedPreferences("galaxot_exported_audio", Context.MODE_PRIVATE)
    }

    suspend fun exportCompleted(download: Download) = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            download.state != Download.STATE_COMPLETED
        ) return@withContext

        val id = download.request.id
        mutexes.getOrPut(id) { Mutex() }.withLock {
            if (preferences.getBoolean(id, false)) return@withLock

            val source = CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(null)
                .setCacheWriteDataSinkFactory(null)
                .createDataSource()
            var destination: Uri? = null
            try {
                val cacheKey = download.request.customCacheKey ?: id
                source.open(
                    DataSpec.Builder()
                        .setUri(download.request.uri)
                        .setKey(cacheKey)
                        .build()
                )
                val initial = ByteArray(16)
                val initialRead = source.read(initial, 0, initial.size)
                if (initialRead <= 0) throw IOException("Audio ausente en el caché")

                val (extension, mimeType) = detectAudioFormat(initial, initialRead)
                val displayTitle = download.request.data
                    .toString(Charsets.UTF_8)
                    .ifBlank { id }
                    .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
                    .trim()
                    .take(90)
                    .ifBlank { "Canción" }
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$displayTitle - $id.$extension")
                    put(MediaStore.Audio.Media.TITLE, displayTitle)
                    put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                    put(
                        MediaStore.Audio.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_MUSIC + "/GalaxOT/"
                    )
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                destination = resolver.insert(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    values,
                ) ?: throw IOException("MediaStore rechazó la creación de audio")

                resolver.openOutputStream(destination!!, "w").use { target ->
                    val stream = target ?: throw IOException("No se pudo abrir el archivo de Música")
                    stream.write(initial, 0, initialRead)
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = source.read(buffer, 0, buffer.size)
                        if (read == -1) break
                        if (read > 0) stream.write(buffer, 0, read)
                    }
                    stream.flush()
                }

                resolver.update(destination!!, ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }, null, null)
                preferences.edit().putBoolean(id, true).apply()
                Timber.tag("GalaxMusicExport").i("Audio disponible en Music/GalaxOT: %s", id)
            } catch (error: Exception) {
                destination?.let { runCatching { context.contentResolver.delete(it, null, null) } }
                Timber.tag("GalaxMusicExport").w(error, "No se pudo exportar %s", id)
                // El archivo Media3 original sigue íntegro y disponible sin conexión.
            } finally {
                runCatching { source.close() }
            }
        }
    }

    private fun detectAudioFormat(header: ByteArray, count: Int): Pair<String, String> {
        val startsWebm = count >= 4 &&
            header[0] == 0x1A.toByte() && header[1] == 0x45.toByte() &&
            header[2] == 0xDF.toByte() && header[3] == 0xA3.toByte()
        if (startsWebm) return "webm" to "audio/webm"
        if (count >= 8 && String(header, 4, 4, Charsets.US_ASCII) == "ftyp")
            return "m4a" to "audio/mp4"
        if (count >= 4 && String(header, 0, 4, Charsets.US_ASCII) == "OggS")
            return "ogg" to "audio/ogg"
        if (count >= 4 && String(header, 0, 4, Charsets.US_ASCII) == "fLaC")
            return "flac" to "audio/flac"
        if (count >= 3 && String(header, 0, 3, Charsets.US_ASCII) == "ID3")
            return "mp3" to "audio/mpeg"
        return "bin" to "application/octet-stream"
    }
}
