package com.arturo254.opentune.sources.offline

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineMusicSource
@Inject
constructor(
    private val registry: OfflineTrackRegistry,
) : TrackSearchSource {
    override val id: ProviderId = ProviderId.OPEN_TUNE_OFFLINE
    override val displayName: String = "Offline"

    override val capabilities: SourceCapabilities =
        SourceCapabilities.of(
            SourceCapability.TRACK_SEARCH,
            SourceCapability.TRACK_METADATA,
            SourceCapability.OFFLINE_LIBRARY,
            SourceCapability.STATIC_ARTWORK,
        )

    override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> =
        runCatching {
            val query = normalize(request.query)
            if (query.isBlank()) return@runCatching TrackSearchPage(items = emptyList())

            val offset = decodeContinuation(request.continuationToken)
            val limit = request.limit.coerceAtMost(MAX_PAGE_SIZE)
            val queryTokens = query.split(' ').filter(String::isNotBlank)

            val matches =
                registry
                    .availableSnapshot()
                    .map { asset ->
                        asset to searchScore(asset, query, queryTokens)
                    }.filter { (_, score) -> score > 0 }
                    .sortedWith(
                        compareByDescending<Pair<OfflineTrackAsset, Int>> { it.second }
                            .thenByDescending { it.first.updatedAtMs },
                    )

            val pageItems =
                matches
                    .drop(offset)
                    .take(limit)
                    .map { (asset, _) -> asset.toSourceVariant() }

            val nextOffset =
                (offset + pageItems.size)
                    .takeIf { pageItems.isNotEmpty() && it < matches.size }

            TrackSearchPage(
                items = pageItems,
                continuationToken = nextOffset?.let(::encodeContinuation),
            )
        }

    private fun searchScore(
        asset: OfflineTrackAsset,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): Int {
        val title = normalize(asset.title)
        val artists = normalize(asset.artists.joinToString(" "))
        val album = normalize(asset.albumName.orEmpty())
        val haystack = listOf(title, artists, album).joinToString(" ").trim()

        if (haystack.isBlank()) return 0
        if (title == normalizedQuery) return 100
        if (title.startsWith(normalizedQuery)) return 90
        if (title.contains(normalizedQuery)) return 80
        if (artists.contains(normalizedQuery)) return 70
        if (album.contains(normalizedQuery)) return 60
        if (queryTokens.all(haystack::contains)) return 50
        return 0
    }

    private fun normalize(value: String): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    private fun decodeContinuation(token: String?): Int {
        if (token == null) return 0
        require(token.startsWith(CONTINUATION_PREFIX)) {
            "Invalid offline continuation token"
        }
        return token
            .removePrefix(CONTINUATION_PREFIX)
            .toIntOrNull()
            ?.takeIf { it >= 0 }
            ?: throw IllegalArgumentException("Invalid offline continuation offset")
    }

    private fun encodeContinuation(offset: Int): String = CONTINUATION_PREFIX + offset

    companion object {
        private const val CONTINUATION_PREFIX = "offset:"
        private const val MAX_PAGE_SIZE = 100
    }
}
