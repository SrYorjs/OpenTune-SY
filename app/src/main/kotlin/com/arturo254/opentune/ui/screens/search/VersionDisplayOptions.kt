package com.arturo254.opentune.ui.screens.search

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.matching.TrackMatcher
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult

internal data class VersionDisplayRecording(
    val recording: UnifiedTrackSearchResult,
    val variants: List<SourceVariant>,
)

internal fun versionDisplayRecordings(
    selected: UnifiedTrackSearchResult,
    related: List<UnifiedTrackSearchResult>,
): List<VersionDisplayRecording> {
    val matcher = TrackMatcher()
    val grouped = mutableListOf<MutableVersionDisplayRecording>()
    for (recording in listOf(selected) + related) {
        val existing = grouped.firstOrNull { candidate ->
            com.arturo254.opentune.sources.search.sameRecording(
                candidate.recording,
                recording,
                matcher,
            )
        }
        if (existing == null) {
            grouped += MutableVersionDisplayRecording(recording, recording.variants.toMutableList())
        } else {
            existing.variants.addAll(recording.variants)
        }
    }

    return grouped.mapNotNull { group ->
        val variants = distinctQualityOptions(group.variants)
        if (variants.isEmpty()) null else VersionDisplayRecording(group.recording, variants)
    }
}

internal fun distinctQualityOptions(variants: List<SourceVariant>): List<SourceVariant> {
    val available = variants.distinctBy { it.provider to it.sourceId }
    val playable = available.filterNot(SourceVariant::isCatalogOnly)
    val candidates = playable.ifEmpty { available }
    return candidates
        .sortedWith(
            compareByDescending<SourceVariant> { it.quality?.lossless == true }
                .thenByDescending { it.quality?.bitDepth ?: 0 }
                .thenByDescending { it.quality?.sampleRateHz ?: 0 }
                .thenByDescending { it.quality?.bitrateBitsPerSecond ?: 0 }
                .thenBy(::providerPreference)
                .thenBy { it.sourceId },
        )
        .distinctBy(::qualityOptionKey)
}

private fun qualityOptionKey(variant: SourceVariant): String {
    if (variant.isCatalogOnly()) return "metadata-only"
    val quality = variant.quality
    if (quality == null || listOf(
            quality.mimeType,
            quality.codec,
            quality.bitrateBitsPerSecond,
            quality.sampleRateHz,
            quality.bitDepth,
            quality.channelCount,
            quality.lossless,
        ).all { it == null }
    ) {
        return "unknown:" + variant.provider.value
    }
    return listOf(
        quality.mimeType?.lowercase().orEmpty(),
        quality.codec?.lowercase().orEmpty(),
        quality.bitrateBitsPerSecond?.toString().orEmpty(),
        quality.sampleRateHz?.toString().orEmpty(),
        quality.bitDepth?.toString().orEmpty(),
        quality.channelCount?.toString().orEmpty(),
        quality.lossless?.toString().orEmpty(),
    ).joinToString("|")
}

private fun providerPreference(variant: SourceVariant): Int =
    when (variant.provider) {
        ProviderId.OPEN_TUNE_OFFLINE -> 0
        ProviderId.LOCAL -> 1
        ProviderId.NAVIDROME -> 2
        ProviderId.AUDIUS -> 3
        ProviderId.YOUTUBE_MUSIC -> 4
        else -> 4
    }

private data class MutableVersionDisplayRecording(
    val recording: UnifiedTrackSearchResult,
    val variants: MutableList<SourceVariant>,
)
