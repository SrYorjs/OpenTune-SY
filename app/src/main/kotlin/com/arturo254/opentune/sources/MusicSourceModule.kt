package com.arturo254.opentune.sources

import com.arturo254.opentune.sources.deezer.DeezerCatalogGateway
import com.arturo254.opentune.sources.deezer.DeezerMusicSource
import com.arturo254.opentune.sources.deezer.DefaultDeezerCatalogGateway
import com.arturo254.opentune.sources.local.LocalMusicSource
import com.arturo254.opentune.sources.offline.OfflineMusicSource
import com.arturo254.opentune.sources.spotify.DefaultSpotifyCatalogGateway
import com.arturo254.opentune.sources.spotify.SpotifyCatalogGateway
import com.arturo254.opentune.sources.spotify.SpotifyMusicSource
import com.arturo254.opentune.sources.soundcloud.SoundCloudCatalogGateway
import com.arturo254.opentune.sources.soundcloud.SoundCloudMusicSource
import com.arturo254.opentune.sources.soundcloud.SoundCloudStreamGateway
import com.arturo254.opentune.sources.soundcloud.SoundCloudWebClient
import com.arturo254.opentune.sources.tidal.DefaultTidalCatalogGateway
import com.arturo254.opentune.sources.tidal.TidalCatalogGateway
import com.arturo254.opentune.sources.tidal.TidalMusicSource
import com.arturo254.opentune.sources.apple.AppleMusicCatalogGateway
import com.arturo254.opentune.sources.apple.AppleMusicSource
import com.arturo254.opentune.sources.apple.DefaultAppleMusicCatalogGateway
import com.arturo254.opentune.sources.youtube.YouTubeMusicSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers source adapters without making UI or playback depend on them yet.
 *
 * SourceRegistry receives this set when Phase 2 consumers start opting in.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MusicSourceModule {
    @Binds
    @IntoSet
    abstract fun bindYouTubeMusicSource(source: YouTubeMusicSource): MusicSource

    @Binds
    @IntoSet
    abstract fun bindLocalMusicSource(source: LocalMusicSource): MusicSource

    @Binds
    @IntoSet
    abstract fun bindOfflineMusicSource(source: OfflineMusicSource): MusicSource

    @Binds
    @IntoSet
    abstract fun bindDeezerMusicSource(source: DeezerMusicSource): MusicSource

    @Binds
    abstract fun bindDeezerCatalogGateway(
        gateway: DefaultDeezerCatalogGateway,
    ): DeezerCatalogGateway

    @Binds
    @IntoSet
    abstract fun bindSpotifyMusicSource(source: SpotifyMusicSource): MusicSource

    @Binds
    abstract fun bindSpotifyCatalogGateway(
        gateway: DefaultSpotifyCatalogGateway,
    ): SpotifyCatalogGateway

    @Binds
    @IntoSet
    abstract fun bindTidalMusicSource(source: TidalMusicSource): MusicSource

    @Binds
    abstract fun bindTidalCatalogGateway(
        gateway: DefaultTidalCatalogGateway,
    ): TidalCatalogGateway

    @Binds
    @IntoSet
    abstract fun bindAppleMusicSource(source: AppleMusicSource): MusicSource

    @Binds
    abstract fun bindAppleMusicCatalogGateway(
        gateway: DefaultAppleMusicCatalogGateway,
    ): AppleMusicCatalogGateway

    @Binds
    @IntoSet
    abstract fun bindSoundCloudMusicSource(source: SoundCloudMusicSource): MusicSource

    @Binds
    abstract fun bindSoundCloudCatalogGateway(
        client: SoundCloudWebClient,
    ): SoundCloudCatalogGateway

    @Binds
    abstract fun bindSoundCloudStreamGateway(
        client: SoundCloudWebClient,
    ): SoundCloudStreamGateway
}
