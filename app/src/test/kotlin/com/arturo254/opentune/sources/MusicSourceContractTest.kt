package com.arturo254.opentune.sources

import com.arturo254.opentune.core.model.ProviderId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicSourceContractTest {
    @Test
    fun capabilities_reportOnlyDeclaredFeatures() {
        val capabilities =
            SourceCapabilities.of(
                SourceCapability.TRACK_SEARCH,
                SourceCapability.STREAM,
            )

        assertTrue(capabilities.supports(SourceCapability.TRACK_SEARCH))
        assertTrue(SourceCapability.STREAM in capabilities)
        assertFalse(capabilities.supports(SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD))
    }

    @Test
    fun sourceCanExposeCapabilitiesWithoutPlaybackPolicy() {
        val source =
            object : MusicSource {
                override val id = ProviderId("test")
                override val displayName = "Test"
                override val capabilities =
                    SourceCapabilities.of(SourceCapability.TRACK_METADATA)
            }

        assertTrue(source.supports(SourceCapability.TRACK_METADATA))
        assertFalse(source.supports(SourceCapability.STREAM))
    }

    @Test(expected = IllegalArgumentException::class)
    fun trackSearchRejectsNonPositiveLimit() {
        TrackSearchRequest(query = "song", limit = 0)
    }
}
