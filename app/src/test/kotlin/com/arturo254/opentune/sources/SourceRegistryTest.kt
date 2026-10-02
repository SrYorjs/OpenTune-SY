package com.arturo254.opentune.sources

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SourceRegistryTest {
    @Test
    fun registryFindsSourcesByProviderIdAndCapability() {
        val youtube = FakeSearchSource(ProviderId.YOUTUBE_MUSIC)
        val metadataOnly = FakeMetadataSource(ProviderId("metadata_only"))
        val registry = SourceRegistry(setOf(metadataOnly, youtube))

        assertEquals(youtube, registry[ProviderId.YOUTUBE_MUSIC])
        assertNull(registry[ProviderId.SPOTIFY])
        assertEquals(listOf(youtube), registry.trackSearchSources())
        assertEquals(
            listOf(metadataOnly),
            registry.withCapability(SourceCapability.TRACK_METADATA),
        )
    }

    @Test
    fun registryOrderIsStableByProviderId() {
        val z = FakeMetadataSource(ProviderId("z_source"))
        val a = FakeMetadataSource(ProviderId("a_source"))
        val registry = SourceRegistry(setOf(z, a))

        assertEquals(listOf("a_source", "z_source"), registry.all.map { it.id.value })
    }

    @Test(expected = IllegalArgumentException::class)
    fun registryRejectsDuplicateProviderIds() {
        SourceRegistry(
            setOf(
                FakeMetadataSource(ProviderId("same")),
                FakeSearchSource(ProviderId("same")),
            ),
        )
    }

    private class FakeMetadataSource(
        override val id: ProviderId,
    ) : MusicSource {
        override val displayName = id.value
        override val capabilities =
            SourceCapabilities.of(SourceCapability.TRACK_METADATA)
    }

    private class FakeSearchSource(
        override val id: ProviderId,
    ) : TrackSearchSource {
        override val displayName = id.value
        override val capabilities =
            SourceCapabilities.of(SourceCapability.TRACK_SEARCH)

        override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> =
            Result.success(
                TrackSearchPage(
                    items =
                        listOf(
                            SourceVariant(
                                provider = id,
                                sourceId = "1",
                                title = request.query,
                                artists = emptyList(),
                            ),
                        ),
                ),
            )
    }
}
