package com.arturo254.opentune.sources

import com.arturo254.opentune.core.model.ProviderId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central inventory of installed music sources.
 *
 * The registry does not resolve which source should win. It only exposes what is
 * installed and what each source declares it can do. SourceResolver will own
 * ranking/fallback policy in a later Phase 2 checkpoint.
 */
@Singleton
class SourceRegistry
@Inject
constructor(
    sources: Set<@JvmSuppressWildcards MusicSource>,
) {
    private val sourcesById: Map<ProviderId, MusicSource>

    init {
        val grouped = sources.groupBy(MusicSource::id)
        val duplicates = grouped.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate music source ids: " + duplicates.joinToString { it.value }
        }

        sourcesById =
            sources
                .sortedBy { it.id.value }
                .associateBy(MusicSource::id)
    }

    val all: List<MusicSource>
        get() = sourcesById.values.toList()

    operator fun get(id: ProviderId): MusicSource? = sourcesById[id]

    fun require(id: ProviderId): MusicSource =
        checkNotNull(sourcesById[id]) { "Music source is not registered: " + id.value }

    fun withCapability(capability: SourceCapability): List<MusicSource> =
        all.filter { it.supports(capability) }

    fun trackSearchSources(): List<TrackSearchSource> =
        withCapability(SourceCapability.TRACK_SEARCH)
            .filterIsInstance<TrackSearchSource>()
}
