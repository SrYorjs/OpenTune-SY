package com.arturo254.opentune.sources.resolution

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Search/UI selection policy. Playback uses the same SourceResolver as the rest
 * of OpenTune; a metadata-only provider is never treated as a direct stream.
 */
@Singleton
class UniversalVersionQualityPolicy
@Inject
constructor(
    private val resolver: SourceResolver,
) {
    fun choose(
        identity: TrackIdentity,
        variants: List<SourceVariant>,
        mode: AudioSelectionMode,
        manualVariant: SourceVariantKey? = null,
    ): SourceVariant? {
        // A manual selection may be a catalog reference. Its actual audio is
        // resolved separately and its catalog quality must not be promised.
        val forced =
            variants.firstOrNull { variant ->
                variant.provider == manualVariant?.provider &&
                    variant.sourceId == manualVariant.sourceId
            }
        if (forced != null) return forced

        val resolution =
            resolver.resolve(
                reference = identity,
                candidates =
                    variants.map { variant ->
                        SourcePlaybackCandidate(
                            variant = variant,
                            availability =
                                when (variant.provider) {
                                    ProviderId.LOCAL -> PlaybackAvailability.LOCAL_FILE
                                    ProviderId.OPEN_TUNE_OFFLINE ->
                                        PlaybackAvailability.OPEN_TUNE_OFFLINE
                                    else -> PlaybackAvailability.ONLINE
                                },
                        )
                    },
                preferences =
                    SourceResolutionPreferences(
                        preferOffline = true,
                        preferLocal = true,
                        preferredProviders =
                            listOf(
                                ProviderId.YOUTUBE_MUSIC,
                                ProviderId.LOCAL,
                                ProviderId.SOUNDCLOUD,
                            ),
                        qualityMode = mode,
                    ),
            )

        return resolution.selected?.candidate?.variant
    }

    /** Separate recordings stay separate; the picker only offers nearby versions. */
    fun relatedVersions(
        selected: UnifiedTrackSearchResult,
        results: List<UnifiedTrackSearchResult>,
    ): List<UnifiedTrackSearchResult> {
        val base = normalizedBaseTitle(selected.identity.title)
        if (base.isBlank()) return emptyList()
        val artists =
            selected.identity.artists.map { normalize(it.name) }.filter(String::isNotBlank).toSet()
        if (artists.isEmpty()) return emptyList()

        return results
            .asSequence()
            .filterNot { it === selected }
            .filter { candidate ->
                normalizedBaseTitle(candidate.identity.title) == base &&
                    candidate.identity.artists
                        .map { normalize(it.name) }
                        .any { it in artists }
            }
            .take(MAX_RELATED_VERSIONS)
            .toList()
    }

    private fun normalizedBaseTitle(title: String): String {
        val stripped =
            title
                .replace(
                    Regex(
                        """(?i)[\[(][^\])]*(?:live|acoustic|remaster|remix|sped[ -]?up|slowed|instrumental|cover|radio edit|music video)[^\])]*[\])]""",
                    ),
                    " ",
                )
                .replace(
                    Regex(
                        """(?i)\s*[-–—]\s*(?:live|acoustic|remaster|remix|sped[ -]?up|slowed|instrumental|cover|radio edit|music video).*""",
                    ),
                    " ",
                )
        return normalize(stripped)
    }

    private fun normalize(value: String): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    private companion object {
        const val MAX_RELATED_VERSIONS = 12
    }
}
