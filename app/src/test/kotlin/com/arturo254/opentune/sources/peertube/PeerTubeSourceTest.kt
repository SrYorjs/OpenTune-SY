package com.arturo254.opentune.sources.peertube

import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.sources.playback.SourcePlaybackRoute
import com.arturo254.opentune.sources.playback.playbackRoute
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class PeerTubeSourceTest {
    private val item = Json.parseToJsonElement("""{"uuid":"8f65c679-68f2-4ba8-9368-ca73699955a7","name":"The New 2018 Blender Institute!","duration":305,"thumbnailPath":"/lazy-static/thumbnails/example.jpg","channel":{"displayName":"Blender"}}""").jsonObject
    @Test fun actualPublicResponseShapeEntersUnifiedPlaybackWithNamespacedIdentity() {
        val variant = parsePeerTubeVariant(item,"https://video.blender.org/")!!
        assertEquals(ProviderId.PEERTUBE,variant.provider)
        assertEquals("video.blender.org:443:8f65c679-68f2-4ba8-9368-ca73699955a7",variant.sourceId)
        assertEquals(305000L,variant.durationMs)
        assertTrue(TrackVersionTrait.MUSIC_VIDEO in variant.version.traits)
        assertEquals(SourcePlaybackRoute.PROVIDER_DIRECT,variant.playbackRoute())
        assertEquals(false,variant.downloadable)
    }
    @Test fun missingCreatorAndUnencryptedInstanceAreNotInvented() {
        assertNull(parsePeerTubeVariant(item,"http://video.blender.org/"))
        assertNull(parsePeerTubeVariant(Json.parseToJsonElement("""{"uuid":"id","name":"Video"}""").jsonObject,"https://video.blender.org/"))
    }
}
