package com.arturo254.opentune.sources.offline

import com.arturo254.opentune.core.model.ArtistIdentity
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackIdentity
import com.arturo254.opentune.sources.ManagedDownloadMusicSource
import com.arturo254.opentune.sources.ManagedDownloadResource
import com.arturo254.opentune.sources.MusicSource
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.SourceRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadPlannerTest {
    @Test
    fun youtubeVariantBecomesBackingAudioWhileAllDiscoverySourcesArePreserved() =
        runBlocking {
            val planner = OfflineDownloadPlanner(SourceRegistry(emptySet()))
            val identity =
                TrackIdentity(
                    title = "Song",
                    artists = listOf(ArtistIdentity("Artist")),
                    durationMs = 180_000L,
                    isrc = "USAAA2600200",
                )
            val variants =
                listOf(
                    SourceVariant(
                        provider = ProviderId.TIDAL,
                        sourceId = "tidal-1",
                        title = "Song",
                        artists = listOf("Artist"),
                        durationMs = 180_000L,
                        isrc = "USAAA2600200",
                        artworkUrl = "https://example.test/cover.jpg",
                        animatedArtworkUrl = "https://example.test/canvas.m3u8",
                    ),
                    SourceVariant(
                        provider = ProviderId.YOUTUBE_MUSIC,
                        sourceId = "yt-1",
                        title = "Song",
                        artists = listOf("Artist"),
                        durationMs = 180_000L,
                        isrc = "USAAA2600200",
                    ),
                )

            val plan = planner.plan(identity, variants).getOrThrow()

            assertEquals("yt-1", plan.asset.mediaId)
            assertEquals(ProviderId.YOUTUBE_MUSIC, plan.asset.backingProvider)
            assertEquals("yt-1", plan.asset.backingSourceId)
            assertEquals("USAAA2600200", plan.asset.isrc)
            assertEquals("https://example.test/cover.jpg", plan.asset.artworkUrl)
            assertEquals("https://example.test/canvas.m3u8", plan.asset.animatedArtworkUrl)
            assertEquals(
                setOf(
                    ProviderId.TIDAL,
                    ProviderId.YOUTUBE_MUSIC,
                ),
                plan.asset.discoverySources.map { it.provider }.toSet(),
            )
            assertEquals("yt-1", plan.metadata.id)
            assertEquals("Song", plan.metadata.title)
        }

    @Test
    fun authorizedDirectSourceCreatesOfflinePlanWithoutYoutube() =
        runBlocking {
            val provider = ProviderId("authorized_direct")
            val source = object : ManagedDownloadMusicSource {
                override val id = provider
                override val displayName = "Authorized Direct"
                override val capabilities = SourceCapabilities.of(
                    SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
                    SourceCapability.STREAM,
                )
                override suspend fun resolveManagedDownload(
                    variant: SourceVariant,
                ): ManagedDownloadResource = ManagedDownloadResource(
                    url = "https://cdn.example.test/audio.mp3",
                )
            }
            val planner = OfflineDownloadPlanner(SourceRegistry(setOf<MusicSource>(source)))
            val variant = SourceVariant(
                provider = provider,
                sourceId = "recording-1",
                title = "Song",
                artists = listOf("Artist"),
                durationMs = 180_000L,
            )
            val identity = TrackIdentity(
                title = "Song",
                artists = listOf(ArtistIdentity("Artist")),
                durationMs = 180_000L,
            )
            val plan = planner.plan(identity, listOf(variant)).getOrThrow()

            assertEquals("offline:authorized_direct:recording-1", plan.asset.mediaId)
            assertEquals(provider, plan.asset.backingProvider)
            assertEquals("recording-1", plan.asset.backingSourceId)
            assertEquals("https://cdn.example.test/audio.mp3", plan.downloadUri)
            assertEquals(plan.asset.mediaId, plan.metadata.id)
            assertEquals(listOf(provider), plan.asset.discoverySources.map { it.provider })
        }

    @Test
    fun streamingWithoutDownloadPermissionUsesLegacyAudioResolver() =
        runBlocking {
            val provider = ProviderId.SOUNDCLOUD
            val source = object : ManagedDownloadMusicSource {
                override val id = provider
                override val displayName = "SoundCloud"
                override val capabilities = SourceCapabilities.of(SourceCapability.STREAM)
                override suspend fun resolveManagedDownload(
                    variant: SourceVariant,
                ): ManagedDownloadResource = error("Must not request a forbidden download")
            }
            val planner = OfflineDownloadPlanner(SourceRegistry(setOf<MusicSource>(source)))
            val variant = SourceVariant(
                provider = provider,
                sourceId = "track-123",
                title = "Song",
                artists = listOf("Artist"),
            )
            val youtube = variant.copy(
                provider = ProviderId.YOUTUBE_MUSIC,
                sourceId = "yt-valid",
            )
            val plan = planner.plan(
                TrackIdentity(title = "Song", artists = listOf(ArtistIdentity("Artist"))),
                listOf(variant, youtube),
            ).getOrThrow()
            assertEquals(ProviderId.YOUTUBE_MUSIC, plan.asset.backingProvider)
            assertEquals(null, plan.downloadUri)
        }

    @Test
    fun audiusTrackWithoutDownloadPermissionCannotBeDownloadedAsYoutube() =
        runBlocking {
            val variant = SourceVariant(
                provider = ProviderId.AUDIUS,
                sourceId = "audius-only",
                title = "Not Downloadable",
                artists = listOf("Artist"),
                downloadable = false,
            )
            val result = OfflineDownloadPlanner(SourceRegistry(emptySet())).plan(
                TrackIdentity(
                    title = "Not Downloadable",
                    artists = listOf(ArtistIdentity("Artist")),
                ),
                listOf(variant),
            )
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Audius") == true)
        }

    @Test
    fun localOnlyTrackDoesNotPretendToNeedAnOfflineDownload() =
        runBlocking {
            val planner = OfflineDownloadPlanner(SourceRegistry(emptySet()))
            val identity =
                TrackIdentity(
                    title = "Local Song",
                    artists = listOf(ArtistIdentity("Artist")),
                )
            val variants =
                listOf(
                    SourceVariant(
                        provider = ProviderId.LOCAL,
                        sourceId = "content://media/1",
                        title = "Local Song",
                        artists = listOf("Artist"),
                    ),
                )

            val result = planner.plan(identity, variants)

            assertTrue(result.isFailure)
        }
}
