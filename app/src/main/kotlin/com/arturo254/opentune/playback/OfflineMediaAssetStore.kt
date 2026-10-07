package com.arturo254.opentune.playback

import android.content.Context
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.models.MediaMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class OfflineMediaAssetManifest(
    val mediaId: String,
    val title: String,
    val artists: List<String>,
    val albumId: String? = null,
    val albumName: String? = null,
    val durationSeconds: Int = MediaMetadata.UNKNOWN_DURATION,
    val explicit: Boolean = false,
    val remoteArtworkUrl: String? = null,
    val localArtworkUri: String? = null,
    val localLyricsUri: String? = null,
    val localAnimationUri: String? = null,
    val backingProvider: String? = null,
    val discoveryProviders: List<String> = emptyList(),
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    fun toMediaMetadata(): MediaMetadata =
        MediaMetadata(
            id = mediaId,
            title = title,
            artists =
                artists.map { artist ->
                    MediaMetadata.Artist(
                        id = null,
                        name = artist,
                    )
                },
            duration = durationSeconds,
            thumbnailUrl = localArtworkUri ?: remoteArtworkUrl,
            album =
                albumName
                    ?.takeIf(String::isNotBlank)
                    ?.let { albumTitle ->
                        MediaMetadata.Album(
                            id = albumId.orEmpty(),
                            title = albumTitle,
                        )
                    },
            explicit = explicit,
        )
}

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
    private val json = Json { ignoreUnknownKeys = true }

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

    fun getManifestUri(mediaId: String): String? =
        manifestFile(mediaId)
            .takeIf { it.isFile && it.length() > 0L }
            ?.toURI()
            ?.toString()

    fun readLyrics(mediaId: String): String? =
        getLyricsFile(mediaId)
            .takeIf { it.isFile && it.length() > 0L }
            ?.let { file ->
                runCatching { file.readText() }.getOrNull()
            }

    fun readManifest(mediaId: String): OfflineMediaAssetManifest? {
        val file = manifestFile(mediaId)
        if (!file.isFile || file.length() == 0L) return null

        return runCatching {
            decodeManifest(
                json.parseToJsonElement(file.readText()).jsonObject,
            )
        }.onFailure {
            Timber.tag("OfflineAssets").w(it, "Unable to read asset manifest for %s", mediaId)
        }.getOrNull()
    }

    suspend fun saveManifest(
        manifest: OfflineMediaAssetManifest,
    ): String =
        withContext(Dispatchers.IO) {
            val target = manifestFile(manifest.mediaId)
            atomicWrite(
                target,
                encodeManifest(
                    manifest.copy(updatedAtMs = System.currentTimeMillis()),
                ).toString().toByteArray(Charsets.UTF_8),
            )
            target.toURI().toString()
        }

    suspend fun removeBundle(mediaId: String) =
        withContext(Dispatchers.IO) {
            val dir = rawAssetDir(mediaId)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }

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

    private fun manifestFile(mediaId: String): File =
        File(assetDir(mediaId), "metadata.json")

    private fun encodeManifest(manifest: OfflineMediaAssetManifest): JsonObject =
        buildJsonObject {
            put("mediaId", manifest.mediaId)
            put("title", manifest.title)
            put(
                "artists",
                buildJsonArray {
                    manifest.artists.forEach { artist ->
                        add(JsonPrimitive(artist))
                    }
                },
            )
            manifest.albumId?.let { put("albumId", it) }
            manifest.albumName?.let { put("albumName", it) }
            put("durationSeconds", manifest.durationSeconds)
            put("explicit", manifest.explicit)
            manifest.remoteArtworkUrl?.let { put("remoteArtworkUrl", it) }
            manifest.localArtworkUri?.let { put("localArtworkUri", it) }
            manifest.localLyricsUri?.let { put("localLyricsUri", it) }
            manifest.localAnimationUri?.let { put("localAnimationUri", it) }
            manifest.backingProvider?.let { put("backingProvider", it) }
            put(
                "discoveryProviders",
                buildJsonArray {
                    manifest.discoveryProviders.forEach { provider ->
                        add(JsonPrimitive(provider))
                    }
                },
            )
            put("updatedAtMs", manifest.updatedAtMs)
        }

    private fun decodeManifest(obj: JsonObject): OfflineMediaAssetManifest =
        OfflineMediaAssetManifest(
            mediaId = obj.string("mediaId") ?: error("Offline metadata manifest missing mediaId"),
            title = obj.string("title").orEmpty(),
            artists =
                obj["artists"]
                    ?.let { runCatching { it.jsonArray }.getOrNull() }
                    ?.mapNotNull { value -> value.jsonPrimitive.contentOrNull }
                    .orEmpty(),
            albumId = obj.string("albumId"),
            albumName = obj.string("albumName"),
            durationSeconds =
                obj["durationSeconds"]
                    ?.jsonPrimitive
                    ?.intOrNull
                    ?: MediaMetadata.UNKNOWN_DURATION,
            explicit =
                obj["explicit"]
                    ?.jsonPrimitive
                    ?.booleanOrNull
                    ?: false,
            remoteArtworkUrl = obj.string("remoteArtworkUrl"),
            localArtworkUri = obj.string("localArtworkUri"),
            localLyricsUri = obj.string("localLyricsUri"),
            localAnimationUri = obj.string("localAnimationUri"),
            backingProvider = obj.string("backingProvider"),
            discoveryProviders =
                obj["discoveryProviders"]
                    ?.let { runCatching { it.jsonArray }.getOrNull() }
                    ?.mapNotNull { value -> value.jsonPrimitive.contentOrNull }
                    .orEmpty(),
            updatedAtMs =
                obj["updatedAtMs"]
                    ?.jsonPrimitive
                    ?.longOrNull
                    ?: 0L,
        )

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

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

    private fun rawAssetDir(mediaId: String): File =
        File(
            root,
            mediaId.replace(Regex("""[^A-Za-z0-9._-]"""), "_"),
        )

    private fun assetDir(mediaId: String): File =
        rawAssetDir(mediaId).apply { mkdirs() }
}
