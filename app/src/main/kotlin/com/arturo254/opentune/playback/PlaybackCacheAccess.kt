package com.arturo254.opentune.playback

import android.content.Context
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import com.arturo254.opentune.di.DownloadCache
import com.arturo254.opentune.di.PlayerCache
import com.arturo254.opentune.ui.original.utils.VideoTrack
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlaybackCacheAccess {
    @PlayerCache fun playerCache(): Cache
    @DownloadCache fun downloadCache(): Cache
}

fun videoCacheKey(mediaId: String, track: VideoTrack): String =
    "video:$mediaId:${track.itag}:${track.width}x${track.height}:${track.codecs}:${track.bitrate}"

fun cachedVideoFactory(context: Context, upstream: DataSource.Factory, mediaId: String, tracks: List<VideoTrack>): DataSource.Factory {
    if (tracks.all { android.net.Uri.parse(it.url).scheme in setOf("file","content") }) return upstream
    val cache = EntryPointAccessors.fromApplication(context.applicationContext, PlaybackCacheAccess::class.java)
    val keys = tracks.associate { it.url to videoCacheKey(mediaId, it) }
    val player = CacheDataSource.Factory().setCache(cache.playerCache()).setUpstreamDataSourceFactory(upstream)
        .setCacheKeyFactory { spec -> keys[spec.uri.toString()] ?: spec.key ?: spec.uri.toString() }
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    return CacheDataSource.Factory().setCache(cache.downloadCache()).setUpstreamDataSourceFactory(player)
        .setCacheKeyFactory { spec -> keys[spec.uri.toString()] ?: spec.key ?: spec.uri.toString() }
        .setCacheWriteDataSinkFactory(null).setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
