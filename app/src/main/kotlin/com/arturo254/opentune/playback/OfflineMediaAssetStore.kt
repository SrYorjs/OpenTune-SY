package com.arturo254.opentune.playback

import android.content.Context
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sidecar assets that belong to an offline song.
 *
 * Audio remains managed by Media3's DownloadManager. This store adds the pieces
 * a premium offline library should keep with it: artwork, synchronized LRC and
 * animated artwork/canvas whenever the provider exposes one.
 */
@Singleton
class OfflineMediaAssetStore
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val client =
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

    private val root = File(context.filesDir, "offline_media").apply { mkdirs() }

    fun getCoverUri(mediaId: String): String? =
        assetDir(mediaId)
            .listFiles()
            ?.firstOrNull { it.isFile && it.name.startsWith("cover.") && it.length() > 0L }
            ?.toURI()
            ?.toString()

    fun getAnimationUri(mediaId: String): String? {
        val dir = assetDir(mediaId)
        val hls = File(dir, "canvas.m3u8")
        if (hls.isFile && hls.length() > 0L) return hls.toURI().toString()

        return dir
            .listFiles()
            ?.firstOrNull {
                it.isFile &&
                    it.name.startsWith("canvas.") &&
                    it.extension.lowercase() in setOf("mp4", "webm", "m4v", "mov")
            }
            ?.takeIf { it.length() > 0L }
            ?.toURI()
            ?.toString()
    }

    fun getLyricsFile(mediaId: String): File = File(assetDir(mediaId), "lyrics.lrc")

    suspend fun saveLyrics(
        mediaId: String,
        lyrics: String,
    ) = withContext(Dispatchers.IO) {
        if (lyrics.isBlank() || lyrics == LYRICS_NOT_FOUND) return@withContext
        val target = getLyricsFile(mediaId)
        atomicWrite(target, lyrics.toByteArray(Charsets.UTF_8))
    }

    suspend fun cacheCover(
        mediaId: String,
        url: String?,
    ): String? =
        withContext(Dispatchers.IO) {
            if (url.isNullOrBlank()) return@withContext null
            getCoverUri(mediaId)?.let { return@withContext it }

            runCatching {
                val extension =
                    when {
                        url.contains(".png", ignoreCase = true) -> "png"
                        url.contains(".webp", ignoreCase = true) -> "webp"
                        else -> "jpg"
                    }
                val target = File(assetDir(mediaId), "cover." + extension)
                downloadBinary(url, target)
                target.toURI().toString()
            }.onFailure {
                Timber.tag("OfflineAssets").w(it, "Unable to cache cover for %s", mediaId)
            }.getOrNull()
        }

    suspend fun cacheAnimatedArtwork(
        mediaId: String,
        url: String?,
    ): String? =
        withContext(Dispatchers.IO) {
            if (url.isNullOrBlank()) return@withContext null
            getAnimationUri(mediaId)?.let { return@withContext it }

            runCatching {
                if (url.contains(".m3u8", ignoreCase = true)) {
                    cacheHls(mediaId, url)
                } else {
                    val extension =
                        when {
                            url.contains(".webm", ignoreCase = true) -> "webm"
                            url.contains(".m4v", ignoreCase = true) -> "m4v"
                            url.contains(".mov", ignoreCase = true) -> "mov"
                            else -> "mp4"
                        }
                    val target = File(assetDir(mediaId), "canvas." + extension)
                    downloadBinary(url, target)
                    target.toURI().toString()
                }
            }.onFailure {
                Timber.tag("OfflineAssets").w(it, "Unable to cache animated artwork for %s", mediaId)
            }.getOrNull()
        }

    private suspend fun cacheHls(
        mediaId: String,
        masterUrl: String,
    ): String {
        val dir = assetDir(mediaId)
        val masterText = requestText(masterUrl)
        val mediaUrl =
            chooseBestVariant(masterUrl, masterText)
                ?: masterUrl
        val playlistText =
            if (mediaUrl == masterUrl) masterText else requestText(mediaUrl)

        val playlistBase = URI(mediaUrl)
        val lines = playlistText.lines()
        val semaphore = Semaphore(6)

        val rewritten =
            coroutineScope {
                lines.mapIndexed { index, line ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            rewriteHlsLine(
                                dir = dir,
                                baseUri = playlistBase,
                                index = index,
                                line = line,
                            )
                        }
                    }
                }.awaitAll()
            }.joinToString("\n")

        val target = File(dir, "canvas.m3u8")
        atomicWrite(target, rewritten.toByteArray(Charsets.UTF_8))
        return target.toURI().toString()
    }

    private fun chooseBestVariant(
        masterUrl: String,
        playlist: String,
    ): String? {
        val lines = playlist.lines()
        var bestBandwidth = -1L
        var bestUrl: String? = null

        lines.forEachIndexed { index, line ->
            if (!line.startsWith("#EXT-X-STREAM-INF")) return@forEachIndexed
            val bandwidth =
                Regex("""BANDWIDTH=(\d+)""")
                    .find(line)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.toLongOrNull()
                    ?: 0L
            val uriLine =
                lines
                    .drop(index + 1)
                    .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                    ?: return@forEachIndexed

            if (bandwidth >= bestBandwidth) {
                bestBandwidth = bandwidth
                bestUrl = URI(masterUrl).resolve(uriLine).toString()
            }
        }

        return bestUrl
    }

    private fun rewriteHlsLine(
        dir: File,
        baseUri: URI,
        index: Int,
        line: String,
    ): String {
        if (line.isBlank()) return line

        if (line.startsWith("#EXT-X-MAP") || line.startsWith("#EXT-X-KEY")) {
            val match = Regex("""URI="([^"]+)"""").find(line) ?: return line
            val remote = baseUri.resolve(match.groupValues[1]).toString()
            val extension = extensionFor(remote, "bin")
            val fileName = "hls_" + index + "." + extension
            downloadBinary(remote, File(dir, fileName))
            return line.replace(match.groupValues[1], fileName)
        }

        if (line.startsWith("#")) return line

        val remote = baseUri.resolve(line.trim()).toString()
        val extension = extensionFor(remote, "ts")
        val fileName = "hls_" + index + "." + extension
        downloadBinary(remote, File(dir, fileName))
        return fileName
    }

    private fun requestText(url: String): String {
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "OpenTune-SY/3")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Asset HTTP " + response.code)
            }
            return response.body.string()
        }
    }

    private fun downloadBinary(
        url: String,
        target: File,
    ) {
        if (target.isFile && target.length() > 0L) return

        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".part")
        val request =
            Request.Builder()
                .url(url)
                .header("User-Agent", "OpenTune-SY/3")
                .get()
                .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Asset HTTP " + response.code)
            }
            response.body.byteStream().use { input ->
                temp.outputStream().buffered().use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE * 4)
                }
            }
        }

        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    private fun atomicWrite(
        target: File,
        bytes: ByteArray,
    ) {
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".part")
        temp.writeBytes(bytes)
        if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    private fun extensionFor(
        url: String,
        fallback: String,
    ): String {
        val path =
            runCatching { URI(url).path }
                .getOrNull()
                .orEmpty()
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension
            .takeIf {
                it.length in 2..5 &&
                    it.all(Char::isLetterOrDigit)
            }
            ?: fallback
    }

    private fun assetDir(mediaId: String): File =
        File(
            root,
            mediaId.replace(Regex("""[^A-Za-z0-9._-]"""), "_"),
        ).apply { mkdirs() }
}
