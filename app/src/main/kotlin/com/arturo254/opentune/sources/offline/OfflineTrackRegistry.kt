package com.arturo254.opentune.sources.offline

import android.content.Context
import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

enum class OfflineTrackState {
    PENDING,
    AVAILABLE,
    FAILED,
}

data class OfflineSourceRef(
    val provider: ProviderId,
    val sourceId: String,
)

data class OfflineTrackAsset(
    val mediaId: String,
    val discoverySources: List<OfflineSourceRef>,
    val backingProvider: ProviderId,
    val backingSourceId: String,
    val title: String,
    val artists: List<String>,
    val albumId: String? = null,
    val albumName: String? = null,
    val durationMs: Long? = null,
    val isrc: String? = null,
    val explicit: Boolean? = null,
    val version: TrackVersion = TrackVersion(),
    val artworkUrl: String? = null,
    val offlineArtworkUri: String? = null,
    val offlineLyricsUri: String? = null,
    val offlineAnimationUri: String? = null,
    val assetManifestUri: String? = null,
    val quality: AudioQualityInfo? = null,
    val state: OfflineTrackState = OfflineTrackState.PENDING,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = createdAtMs,
) {
    fun toSourceVariant(): SourceVariant =
        SourceVariant(
            provider = ProviderId.OPEN_TUNE_OFFLINE,
            sourceId = mediaId,
            title = title,
            artists = artists,
            albumId = albumId,
            albumName = albumName,
            durationMs = durationMs,
            isrc = isrc,
            explicit = explicit,
            version = version,
            artworkUrl = offlineArtworkUri ?: artworkUrl,
            quality = quality,
        )
}

/**
 * Persistent provider-neutral index for OpenTune-owned offline audio.
 *
 * Audio bytes remain owned by Media3's DownloadManager. This registry records
 * which conceptual track they belong to, where the user discovered the track,
 * and which concrete provider/source OpenTune actually downloaded.
 */
@Singleton
class OfflineTrackRegistry internal constructor(
    private val file: File,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : this(File(context.filesDir, FILE_NAME))

    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    private val _assets =
        MutableStateFlow(
            runCatching(::readFile)
                .getOrDefault(emptyList())
                .sortedByDescending(OfflineTrackAsset::updatedAtMs),
        )
    val assets: StateFlow<List<OfflineTrackAsset>> = _assets.asStateFlow()

    fun snapshot(): List<OfflineTrackAsset> = _assets.value

    fun availableSnapshot(): List<OfflineTrackAsset> =
        _assets.value.filter { it.state == OfflineTrackState.AVAILABLE }

    fun findByMediaId(mediaId: String): OfflineTrackAsset? =
        _assets.value.firstOrNull { it.mediaId == mediaId }

    fun findByDiscovery(
        provider: ProviderId,
        sourceId: String,
    ): OfflineTrackAsset? =
        _assets.value.firstOrNull { asset ->
            asset.discoverySources.any { ref ->
                ref.provider == provider && ref.sourceId == sourceId
            }
        }

    suspend fun registerPending(asset: OfflineTrackAsset): OfflineTrackAsset =
        mutate { current ->
            val existing = current.firstOrNull { it.mediaId == asset.mediaId }
            val merged =
                if (existing == null) {
                    asset.copy(
                        state = OfflineTrackState.PENDING,
                        updatedAtMs = System.currentTimeMillis(),
                    )
                } else {
                    existing.copy(
                        discoverySources =
                            (existing.discoverySources + asset.discoverySources)
                                .distinctBy { it.provider to it.sourceId },
                        backingProvider = asset.backingProvider,
                        backingSourceId = asset.backingSourceId,
                        title = asset.title.ifBlank { existing.title },
                        artists = asset.artists.ifEmpty { existing.artists },
                        albumId = asset.albumId ?: existing.albumId,
                        albumName = asset.albumName ?: existing.albumName,
                        durationMs = asset.durationMs ?: existing.durationMs,
                        isrc = asset.isrc ?: existing.isrc,
                        explicit = asset.explicit ?: existing.explicit,
                        version =
                            if (
                                asset.version.traits.isNotEmpty() ||
                                !asset.version.label.isNullOrBlank()
                            ) {
                                asset.version
                            } else {
                                existing.version
                            },
                        artworkUrl = asset.artworkUrl ?: existing.artworkUrl,
                        offlineArtworkUri = asset.offlineArtworkUri ?: existing.offlineArtworkUri,
                        offlineLyricsUri = asset.offlineLyricsUri ?: existing.offlineLyricsUri,
                        offlineAnimationUri = asset.offlineAnimationUri ?: existing.offlineAnimationUri,
                        assetManifestUri = asset.assetManifestUri ?: existing.assetManifestUri,
                        quality = asset.quality ?: existing.quality,
                        state =
                            if (existing.state == OfflineTrackState.AVAILABLE) {
                                OfflineTrackState.AVAILABLE
                            } else {
                                OfflineTrackState.PENDING
                            },
                        updatedAtMs = System.currentTimeMillis(),
                    )
                }

            current
                .filterNot { it.mediaId == merged.mediaId } +
                merged
        }.first { it.mediaId == asset.mediaId }

    suspend fun markAvailable(mediaId: String) {
        updateState(mediaId, OfflineTrackState.AVAILABLE)
    }

    suspend fun markFailed(mediaId: String) {
        updateState(mediaId, OfflineTrackState.FAILED)
    }

    suspend fun remove(mediaId: String) {
        mutate { current ->
            current.filterNot { it.mediaId == mediaId }
        }
    }

    suspend fun updateOfflineAssets(
        mediaId: String,
        artworkUri: String?,
        lyricsUri: String?,
        animationUri: String?,
        manifestUri: String?,
    ) {
        mutate { current ->
            current.map { asset ->
                if (asset.mediaId == mediaId) {
                    asset.copy(
                        offlineArtworkUri = artworkUri ?: asset.offlineArtworkUri,
                        offlineLyricsUri = lyricsUri ?: asset.offlineLyricsUri,
                        offlineAnimationUri = animationUri ?: asset.offlineAnimationUri,
                        assetManifestUri = manifestUri ?: asset.assetManifestUri,
                        updatedAtMs = System.currentTimeMillis(),
                    )
                } else {
                    asset
                }
            }
        }
    }

    suspend fun adoptLegacy(
        mediaId: String,
        title: String,
        artists: List<String>,
        albumId: String? = null,
        albumName: String? = null,
        durationMs: Long? = null,
        artworkUrl: String? = null,
    ) {
        if (findByMediaId(mediaId) != null) return

        val provider = ProviderId.YOUTUBE_MUSIC
        mutate { current ->
            if (current.any { it.mediaId == mediaId }) {
                current
            } else {
                current +
                    OfflineTrackAsset(
                        mediaId = mediaId,
                        discoverySources =
                            listOf(
                                OfflineSourceRef(
                                    provider = provider,
                                    sourceId = mediaId,
                                ),
                            ),
                        backingProvider = provider,
                        backingSourceId = mediaId,
                        title = title,
                        artists = artists,
                        albumId = albumId,
                        albumName = albumName,
                        durationMs = durationMs,
                        artworkUrl = artworkUrl,
                        state = OfflineTrackState.AVAILABLE,
                    )
            }
        }
    }

    private suspend fun updateState(
        mediaId: String,
        state: OfflineTrackState,
    ) {
        mutate { current ->
            current.map { asset ->
                if (asset.mediaId == mediaId) {
                    asset.copy(
                        state = state,
                        updatedAtMs = System.currentTimeMillis(),
                    )
                } else {
                    asset
                }
            }
        }
    }

    private suspend fun mutate(
        transform: (List<OfflineTrackAsset>) -> List<OfflineTrackAsset>,
    ): List<OfflineTrackAsset> =
        mutex.withLock {
            val updated =
                transform(_assets.value)
                    .distinctBy(OfflineTrackAsset::mediaId)
                    .sortedByDescending(OfflineTrackAsset::updatedAtMs)
            _assets.value = updated
            withContext(Dispatchers.IO) {
                persist(updated)
            }
            updated
        }

    private fun readFile(): List<OfflineTrackAsset> {
        if (!file.isFile || file.length() == 0L) return emptyList()
        val root = json.parseToJsonElement(file.readText()).jsonArray
        return root.mapNotNull { element ->
            runCatching { decodeAsset(element.jsonObject) }.getOrNull()
        }
    }

    private fun persist(assets: List<OfflineTrackAsset>) {
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".part")
        val payload =
            buildJsonArray {
                assets.forEach { asset ->
                    add(encodeAsset(asset))
                }
            }.toString()
        temp.writeText(payload)
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }

    private fun encodeAsset(asset: OfflineTrackAsset): JsonObject =
        buildJsonObject {
            put("mediaId", asset.mediaId)
            put("backingProvider", asset.backingProvider.value)
            put("backingSourceId", asset.backingSourceId)
            put("title", asset.title)
            put(
                "artists",
                buildJsonArray {
                    asset.artists.forEach { artist ->
                        add(JsonPrimitive(artist))
                    }
                },
            )
            asset.albumId?.let { put("albumId", it) }
            asset.albumName?.let { put("albumName", it) }
            asset.durationMs?.let { put("durationMs", it) }
            asset.isrc?.let { put("isrc", it) }
            asset.explicit?.let { put("explicit", it) }
            asset.version.label?.let { put("versionLabel", it) }
            put(
                "versionTraits",
                buildJsonArray {
                    asset.version.traits.forEach { trait ->
                        add(JsonPrimitive(trait.name))
                    }
                },
            )
            asset.artworkUrl?.let { put("artworkUrl", it) }
            asset.offlineArtworkUri?.let { put("offlineArtworkUri", it) }
            asset.offlineLyricsUri?.let { put("offlineLyricsUri", it) }
            asset.offlineAnimationUri?.let { put("offlineAnimationUri", it) }
            asset.assetManifestUri?.let { put("assetManifestUri", it) }
            asset.quality?.let { quality ->
                put(
                    "quality",
                    buildJsonObject {
                        quality.mimeType?.let { put("mimeType", it) }
                        quality.codec?.let { put("codec", it) }
                        quality.bitrateBitsPerSecond?.let { put("bitrate", it) }
                        quality.sampleRateHz?.let { put("sampleRate", it) }
                        quality.bitDepth?.let { put("bitDepth", it) }
                        quality.channelCount?.let { put("channels", it) }
                        quality.lossless?.let { put("lossless", it) }
                    },
                )
            }
            put("state", asset.state.name)
            put("createdAtMs", asset.createdAtMs)
            put("updatedAtMs", asset.updatedAtMs)
            put(
                "discoverySources",
                buildJsonArray {
                    asset.discoverySources.forEach { ref ->
                        add(
                            buildJsonObject {
                                put("provider", ref.provider.value)
                                put("sourceId", ref.sourceId)
                            },
                        )
                    }
                },
            )
        }

    private fun decodeAsset(obj: JsonObject): OfflineTrackAsset {
        val mediaId = obj.string("mediaId") ?: error("Offline asset missing mediaId")
        val backingProvider =
            obj.string("backingProvider")
                ?.let(::ProviderId)
                ?: ProviderId.YOUTUBE_MUSIC
        val backingSourceId = obj.string("backingSourceId") ?: mediaId
        val title = obj.string("title").orEmpty()
        val artists =
            obj["artists"]
                ?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                .orEmpty()
        val versionTraits =
            obj["versionTraits"]
                ?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.mapNotNull { value ->
                    value.jsonPrimitive.contentOrNull
                        ?.let { name ->
                            runCatching { TrackVersionTrait.valueOf(name) }.getOrNull()
                        }
                }.orEmpty()
                .toSet()
        val quality =
            obj["quality"]
                ?.let { runCatching { it.jsonObject }.getOrNull() }
                ?.let { qualityObj ->
                    AudioQualityInfo(
                        mimeType = qualityObj.string("mimeType"),
                        codec = qualityObj.string("codec"),
                        bitrateBitsPerSecond = qualityObj.int("bitrate"),
                        sampleRateHz = qualityObj.int("sampleRate"),
                        bitDepth = qualityObj.int("bitDepth"),
                        channelCount = qualityObj.int("channels"),
                        lossless = qualityObj.boolean("lossless"),
                    )
                }
        val discoverySources =
            obj["discoverySources"]
                ?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.mapNotNull { value ->
                    val ref = runCatching { value.jsonObject }.getOrNull() ?: return@mapNotNull null
                    val provider = ref.string("provider")?.let(::ProviderId) ?: return@mapNotNull null
                    val sourceId = ref.string("sourceId") ?: return@mapNotNull null
                    OfflineSourceRef(
                        provider = provider,
                        sourceId = sourceId,
                    )
                }.orEmpty()

        return OfflineTrackAsset(
            mediaId = mediaId,
            discoverySources =
                discoverySources.ifEmpty {
                    listOf(
                        OfflineSourceRef(
                            provider = backingProvider,
                            sourceId = backingSourceId,
                        ),
                    )
                },
            backingProvider = backingProvider,
            backingSourceId = backingSourceId,
            title = title,
            artists = artists,
            albumId = obj.string("albumId"),
            albumName = obj.string("albumName"),
            durationMs = obj.long("durationMs"),
            isrc = obj.string("isrc"),
            explicit = obj.boolean("explicit"),
            version =
                TrackVersion(
                    traits = versionTraits,
                    label = obj.string("versionLabel"),
                ),
            artworkUrl = obj.string("artworkUrl"),
            offlineArtworkUri = obj.string("offlineArtworkUri"),
            offlineLyricsUri = obj.string("offlineLyricsUri"),
            offlineAnimationUri = obj.string("offlineAnimationUri"),
            assetManifestUri = obj.string("assetManifestUri"),
            quality = quality,
            state =
                obj.string("state")
                    ?.let { runCatching { OfflineTrackState.valueOf(it) }.getOrNull() }
                    ?: OfflineTrackState.AVAILABLE,
            createdAtMs = obj.long("createdAtMs") ?: 0L,
            updatedAtMs = obj.long("updatedAtMs") ?: 0L,
        )
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.long(name: String): Long? =
        this[name]?.jsonPrimitive?.longOrNull

    private fun JsonObject.int(name: String): Int? =
        this[name]?.jsonPrimitive?.intOrNull

    private fun JsonObject.boolean(name: String): Boolean? =
        this[name]?.jsonPrimitive?.booleanOrNull

    companion object {
        private const val FILE_NAME = "offline_tracks_v1.json"
    }
}
