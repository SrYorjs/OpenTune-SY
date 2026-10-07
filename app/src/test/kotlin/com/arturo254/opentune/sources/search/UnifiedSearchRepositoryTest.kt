package com.arturo254.opentune.sources.search

import com.arturo254.opentune.core.model.AudioQualityInfo
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.core.model.TrackVersion
import com.arturo254.opentune.core.model.TrackVersionTrait
import com.arturo254.opentune.sources.MusicSource
import com.arturo254.opentune.sources.SourceCapabilities
import com.arturo254.opentune.sources.SourceCapability
import com.arturo254.opentune.sources.SourceRegistry
import com.arturo254.opentune.sources.TrackSearchPage
import com.arturo254.opentune.sources.TrackSearchRequest
import com.arturo254.opentune.sources.TrackSearchSource
import com.arturo254.opentune.sources.matching.TrackMatcher
import com.arturo254.opentune.sources.resolution.SourceResolver
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedSearchRepositoryTest {
    @Test
    fun searchesAllSourcesConcurrently() =
        runBlocking {
            val started = AtomicInteger(0)
            val gate = CompletableDeferred<Unit>()
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        if (started.incrementAndGet() == 2) gate.complete(Unit)
                        gate.await()
                        page(variant(ProviderId.YOUTUBE_MUSIC, "yt"))
                    },
                )
            val local =
                FakeSearchSource(
                    id = ProviderId.LOCAL,
                    onSearch = {
                        if (started.incrementAndGet() == 2) gate.complete(Unit)
                        gate.await()
                        page(variant(ProviderId.LOCAL, "local", title = "Different"))
                    },
                )

            val result =
                withTimeout(1_000) {
                    repository(youtube, local).search(UnifiedSearchRequest("song"))
                }

            assertEquals(2, started.get())
            assertEquals(2, result.successfulSourceCount)
        }

    @Test
    fun providerFailureDoesNotDiscardSuccessfulResults() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = { page(variant(ProviderId.YOUTUBE_MUSIC, "yt")) },
                )
            val broken =
                FakeSearchSource(
                    id = ProviderId("broken"),
                    onSearch = { Result.failure(IllegalStateException("provider down")) },
                )

            val result = repository(youtube, broken).search(UnifiedSearchRequest("song"))

            assertEquals(1, result.tracks.size)
            assertEquals(1, result.successfulSourceCount)
            assertEquals(1, result.failedSourceCount)
            assertEquals("IllegalStateException", result.sources[ProviderId("broken")]?.error?.type)
        }

    @Test
    fun sameRecordingAcrossProvidersIsDeduplicatedIntoOneConceptualTrack() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        page(
                            variant(
                                provider = ProviderId.YOUTUBE_MUSIC,
                                sourceId = "yt",
                                isrc = "USAAA1111111",
                            ),
                        )
                    },
                )
            val catalogProvider = ProviderId("catalog")
            val catalog =
                FakeSearchSource(
                    id = catalogProvider,
                    onSearch = {
                        page(
                            variant(
                                provider = catalogProvider,
                                sourceId = "catalog",
                                isrc = "US-AAA-11-11111",
                            ),
                        )
                    },
                )

            val result = repository(youtube, catalog).search(UnifiedSearchRequest("song"))

            assertEquals(1, result.tracks.size)
            assertEquals(2, result.tracks.single().variants.size)
            assertEquals("USAAA1111111", result.tracks.single().identity.isrc)
        }

    @Test
    fun sameIsrcAcrossAllMajorProvidersMergesIntoOneTrack() =
        runBlocking {
            val providers =
                listOf(
                    ProviderId.YOUTUBE_MUSIC,
                    ProviderId.SPOTIFY,
                    ProviderId.DEEZER,
                    ProviderId.TIDAL,
                    ProviderId.APPLE_MUSIC,
                    ProviderId.SOUNDCLOUD,
                )
            val sources =
                providers.mapIndexed { index, provider ->
                    FakeSearchSource(
                        id = provider,
                        onSearch = {
                            page(
                                variant(
                                    provider = provider,
                                    sourceId = provider.value + "-" + index,
                                    isrc = "USAAA2600020",
                                ),
                            )
                        },
                    )
                }

            val result =
                repository(*sources.toTypedArray())
                    .search(UnifiedSearchRequest("song"))

            assertEquals(1, result.tracks.size)
            assertEquals(6, result.tracks.single().variants.size)
            assertEquals(
                providers.toSet(),
                result.tracks.single().variants.map { it.provider }.toSet(),
            )
            assertEquals("USAAA2600020", result.tracks.single().identity.isrc)
        }

    @Test
    fun multipleProviderFailuresStillReturnHealthyProviders() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = { page(variant(ProviderId.YOUTUBE_MUSIC, "yt")) },
                )
            val soundCloud =
                FakeSearchSource(
                    id = ProviderId.SOUNDCLOUD,
                    onSearch = { page(variant(ProviderId.SOUNDCLOUD, "sc", title = "SC Song")) },
                )
            val brokenTidal =
                FakeSearchSource(
                    id = ProviderId.TIDAL,
                    onSearch = { Result.failure(IllegalStateException("tidal down")) },
                )
            val brokenApple =
                FakeSearchSource(
                    id = ProviderId.APPLE_MUSIC,
                    onSearch = { Result.failure(IllegalStateException("apple down")) },
                )

            val result =
                repository(youtube, soundCloud, brokenTidal, brokenApple)
                    .search(UnifiedSearchRequest("song"))

            assertEquals(2, result.successfulSourceCount)
            assertEquals(2, result.failedSourceCount)
            assertEquals(2, result.tracks.size)
            assertTrue(result.sources[ProviderId.TIDAL]?.error != null)
            assertTrue(result.sources[ProviderId.APPLE_MUSIC]?.error != null)
        }

    @Test
    fun liveVersionIsNotMergedWithOriginal() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = { page(variant(ProviderId.YOUTUBE_MUSIC, "original")) },
                )
            val liveProvider = ProviderId("live_provider")
            val live =
                FakeSearchSource(
                    id = liveProvider,
                    onSearch = {
                        page(
                            variant(
                                provider = liveProvider,
                                sourceId = "live",
                                title = "Song - Live",
                                version = TrackVersion(setOf(TrackVersionTrait.LIVE)),
                            ),
                        )
                    },
                )

            val result = repository(youtube, live).search(UnifiedSearchRequest("song"))

            assertEquals(2, result.tracks.size)
        }

    @Test
    fun localVariantBecomesRecommendedPlaybackWhenItMatchesOnlineTrack() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        page(
                            variant(
                                ProviderId.YOUTUBE_MUSIC,
                                "yt",
                                quality = AudioQualityInfo(bitrateBitsPerSecond = 160_000),
                            ),
                        )
                    },
                )
            val local =
                FakeSearchSource(
                    id = ProviderId.LOCAL,
                    onSearch = {
                        page(
                            variant(
                                ProviderId.LOCAL,
                                "content://media/1",
                                quality = AudioQualityInfo(bitrateBitsPerSecond = 320_000),
                            ),
                        )
                    },
                )

            val result = repository(youtube, local).search(UnifiedSearchRequest("song"))

            assertEquals(1, result.tracks.size)
            assertEquals(
                ProviderId.LOCAL,
                result.tracks.single().recommendedVariant?.provider,
            )
        }

    @Test
    fun downloadedOfflineVariantWinsOverMatchingOnlineProviders() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        page(
                            variant(
                                provider = ProviderId.YOUTUBE_MUSIC,
                                sourceId = "yt-online",
                                isrc = "USAAA2600300",
                            ),
                        )
                    },
                )
            val offline =
                FakeSearchSource(
                    id = ProviderId.OPEN_TUNE_OFFLINE,
                    capabilities =
                        SourceCapabilities.of(
                            SourceCapability.TRACK_SEARCH,
                            SourceCapability.TRACK_METADATA,
                            SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
                        ),
                    onSearch = {
                        page(
                            variant(
                                provider = ProviderId.OPEN_TUNE_OFFLINE,
                                sourceId = "yt-downloaded",
                                isrc = "USAAA2600300",
                            ),
                        )
                    },
                )

            val result =
                repository(youtube, offline)
                    .search(UnifiedSearchRequest("song"))

            assertEquals(1, result.tracks.size)
            assertEquals(
                ProviderId.OPEN_TUNE_OFFLINE,
                result.tracks.single().recommendedVariant?.provider,
            )
            assertEquals("yt-downloaded", result.tracks.single().recommendedVariant?.sourceId)
        }

    @Test
    fun metadataOnlyProviderCanContributeVariantButIsNotRecommendedForPlayback() =
        runBlocking {
            val metadataProvider = ProviderId("metadata")
            val metadata =
                FakeSearchSource(
                    id = metadataProvider,
                    capabilities =
                        SourceCapabilities.of(
                            SourceCapability.TRACK_SEARCH,
                            SourceCapability.TRACK_METADATA,
                        ),
                    onSearch = { page(variant(metadataProvider, "metadata")) },
                )

            val result = repository(metadata).search(UnifiedSearchRequest("song"))

            assertEquals(1, result.tracks.size)
            assertNull(result.tracks.single().recommendedVariant)
        }

    @Test
    fun offlineResultsRemainPlayableWhenOnlineProvidersFail() =
        runBlocking {
            val offline =
                FakeSearchSource(
                    id = ProviderId.OPEN_TUNE_OFFLINE,
                    capabilities =
                        SourceCapabilities.of(
                            SourceCapability.TRACK_SEARCH,
                            SourceCapability.TRACK_METADATA,
                            SourceCapability.OPEN_TUNE_MANAGED_DOWNLOAD,
                        ),
                    onSearch = {
                        page(
                            variant(
                                provider = ProviderId.OPEN_TUNE_OFFLINE,
                                sourceId = "yt-cached",
                                title = "Offline Song",
                                isrc = "USAAA2600400",
                            ),
                        )
                    },
                )
            val brokenProviders =
                listOf(
                    ProviderId.YOUTUBE_MUSIC,
                    ProviderId.SPOTIFY,
                    ProviderId.DEEZER,
                    ProviderId.TIDAL,
                    ProviderId.APPLE_MUSIC,
                    ProviderId.SOUNDCLOUD,
                ).map { provider ->
                    FakeSearchSource(
                        id = provider,
                        onSearch = {
                            Result.failure(IllegalStateException(provider.value + " offline"))
                        },
                    )
                }

            val result =
                repository(
                    offline,
                    *brokenProviders.toTypedArray(),
                ).search(UnifiedSearchRequest("offline song"))

            assertEquals(1, result.tracks.size)
            assertEquals(1, result.successfulSourceCount)
            assertEquals(6, result.failedSourceCount)
            assertEquals(
                ProviderId.OPEN_TUNE_OFFLINE,
                result.tracks.single().recommendedVariant?.provider,
            )
            assertEquals("yt-cached", result.tracks.single().recommendedVariant?.sourceId)
        }

    @Test
    fun providerFilterQueriesOnlyRequestedSources() =
        runBlocking {
            var youtubeCalls = 0
            var localCalls = 0
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        youtubeCalls++
                        page(variant(ProviderId.YOUTUBE_MUSIC, "yt"))
                    },
                )
            val local =
                FakeSearchSource(
                    id = ProviderId.LOCAL,
                    onSearch = {
                        localCalls++
                        page(variant(ProviderId.LOCAL, "local"))
                    },
                )

            val result =
                repository(youtube, local)
                    .search(
                        UnifiedSearchRequest(
                            query = "song",
                            providers = setOf(ProviderId.LOCAL),
                        ),
                    )

            assertEquals(0, youtubeCalls)
            assertEquals(1, localCalls)
            assertEquals(setOf(ProviderId.LOCAL), result.sources.keys)
        }

    @Test
    fun continuationTokensRemainAssociatedWithTheirProvider() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        Result.success(
                            TrackSearchPage(
                                items = listOf(variant(ProviderId.YOUTUBE_MUSIC, "yt")),
                                continuationToken = "yt-next",
                            ),
                        )
                    },
                )

            val result = repository(youtube).search(UnifiedSearchRequest("song"))

            assertEquals(
                "yt-next",
                result.sources[ProviderId.YOUTUBE_MUSIC]?.continuationToken,
            )
        }

    @Test
    fun loadMoreUsesProviderContinuationAndMergesResults() =
        runBlocking {
            var continuationSeen: String? = null
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = { request ->
                        if (request.continuationToken == null) {
                            Result.success(
                                TrackSearchPage(
                                    items =
                                        listOf(
                                            variant(
                                                ProviderId.YOUTUBE_MUSIC,
                                                "first",
                                                title = "First Song",
                                            ),
                                        ),
                                    continuationToken = "next-page",
                                ),
                            )
                        } else {
                            continuationSeen = request.continuationToken
                            Result.success(
                                TrackSearchPage(
                                    items =
                                        listOf(
                                            variant(
                                                ProviderId.YOUTUBE_MUSIC,
                                                "second",
                                                title = "Second Song",
                                            ),
                                        ),
                                    continuationToken = null,
                                ),
                            )
                        }
                    },
                )
            val repository = repository(youtube)
            val request = UnifiedSearchRequest("song")
            val first = repository.search(request)

            assertTrue(first.hasMore)

            val second = repository.loadMore(request, first)

            assertEquals("next-page", continuationSeen)
            assertEquals(2, second.tracks.size)
            assertEquals(2, second.sources[ProviderId.YOUTUBE_MUSIC]?.itemCount)
            assertTrue(!second.hasMore)
        }

    @Test
    fun loadMoreDoesNotDuplicateProviderTrackIds() =
        runBlocking {
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = { request ->
                        Result.success(
                            TrackSearchPage(
                                items =
                                    listOf(
                                        variant(
                                            ProviderId.YOUTUBE_MUSIC,
                                            "same-id",
                                        ),
                                    ),
                                continuationToken =
                                    if (request.continuationToken == null) "again" else null,
                            ),
                        )
                    },
                )
            val repository = repository(youtube)
            val request = UnifiedSearchRequest("song")
            val first = repository.search(request)
            val second = repository.loadMore(request, first)

            assertEquals(1, second.tracks.size)
            assertEquals(1, second.tracks.single().variants.size)
        }

    @Test
    fun blankQueryDoesNotCallProviders() =
        runBlocking {
            var calls = 0
            val youtube =
                FakeSearchSource(
                    id = ProviderId.YOUTUBE_MUSIC,
                    onSearch = {
                        calls++
                        page(variant(ProviderId.YOUTUBE_MUSIC, "yt"))
                    },
                )

            val result = repository(youtube).search(UnifiedSearchRequest("   "))

            assertEquals(0, calls)
            assertTrue(result.tracks.isEmpty())
            assertTrue(result.sources.isEmpty())
        }

    private fun repository(vararg sources: TrackSearchSource): UnifiedSearchRepository {
        val registry = SourceRegistry(sources.toSet<MusicSource>())
        val matcher = TrackMatcher()
        return UnifiedSearchRepository(
            registry = registry,
            trackMatcher = matcher,
            sourceResolver =
                SourceResolver(
                    registry = registry,
                    trackMatcher = matcher,
                ),
        )
    }

    private fun page(variant: SourceVariant): Result<TrackSearchPage> =
        Result.success(TrackSearchPage(items = listOf(variant)))

    private fun variant(
        provider: ProviderId,
        sourceId: String,
        title: String = "Song",
        artist: String = "Artist",
        isrc: String? = null,
        version: TrackVersion = TrackVersion(),
        quality: AudioQualityInfo? = null,
    ): SourceVariant =
        SourceVariant(
            provider = provider,
            sourceId = sourceId,
            title = title,
            artists = listOf(artist),
            durationMs = 180_000,
            isrc = isrc,
            version = version,
            quality = quality,
        )

    private class FakeSearchSource(
        override val id: ProviderId,
        override val capabilities: SourceCapabilities =
            SourceCapabilities.of(
                SourceCapability.TRACK_SEARCH,
                SourceCapability.TRACK_METADATA,
                SourceCapability.STREAM,
            ),
        private val onSearch: suspend (TrackSearchRequest) -> Result<TrackSearchPage>,
    ) : TrackSearchSource {
        override val displayName: String = id.value

        override suspend fun searchTracks(request: TrackSearchRequest): Result<TrackSearchPage> =
            onSearch(request)
    }
}
