package com.arturo254.opentune.ui.player

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.arturo254.opentune.R
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YouTubeClient
import okhttp3.OkHttpClient
import java.util.Locale

internal val LocalCanvasFrameReceiver = androidx.compose.runtime.staticCompositionLocalOf<((androidx.compose.ui.graphics.ImageBitmap?) -> Unit)?> { null }

@Composable
internal fun CanvasArtworkPlayer(
    primaryUrl: String?,
    fallbackUrl: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
) {
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val frameReceiver by androidx.compose.runtime.rememberUpdatedState(LocalCanvasFrameReceiver.current)
    var texture by remember { mutableStateOf<android.view.TextureView?>(null) }
    val primary = primaryUrl?.takeIf { it.isNotBlank() }
    val fallback = fallbackUrl?.takeIf { it.isNotBlank() }
    val initial = primary ?: fallback ?: return
    var currentUrl by remember(initial) { mutableStateOf(initial) }
    var isVideoReady by remember(initial) { mutableStateOf(false) }

    val okHttpClient =
        remember {
            OkHttpClient
                .Builder()
                .proxy(YouTube.proxy)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val host = request.url.host
                    val isYouTubeMediaHost =
                        host.endsWith("googlevideo.com") ||
                                host.endsWith("googleusercontent.com") ||
                                host.endsWith("youtube.com") ||
                                host.endsWith("youtube-nocookie.com") ||
                                host.endsWith("ytimg.com")

                    if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                    val clientParam = request.url.queryParameter("c")?.trim().orEmpty()
                    val isWeb =
                        clientParam.startsWith("WEB", ignoreCase = true) ||
                                clientParam.startsWith("WEB_REMIX", ignoreCase = true) ||
                                request.url.toString().contains("c=WEB", ignoreCase = true)

                    val userAgent =
                        when {
                            clientParam.startsWith("WEB", ignoreCase = true) ||
                                    clientParam.startsWith("WEB_REMIX", ignoreCase = true) -> YouTubeClient.USER_AGENT_WEB

                            clientParam.startsWith("IOS", ignoreCase = true) -> YouTubeClient.IOS.userAgent

                            clientParam.startsWith("ANDROID_VR", ignoreCase = true) -> YouTubeClient.ANDROID_VR_NO_AUTH.userAgent

                            clientParam.startsWith("ANDROID", ignoreCase = true) -> YouTubeClient.MOBILE.userAgent

                            else -> YouTubeClient.USER_AGENT_WEB
                        }

                    val builder = request.newBuilder().header("User-Agent", userAgent)
                    if (isWeb) {
                        builder.header("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                        builder.header("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
                    }

                    chain.proceed(builder.build())
                }
                .build()
        }
    val mediaSourceFactory =
        remember(okHttpClient) {
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(
                    context,
                    OkHttpDataSource.Factory(okHttpClient),
                ),
            )
        }
    val exoPlayer =
        remember(initial, mediaSourceFactory) {
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes
                            .Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                            .build(),
                        false,
                    )
                    volume = 0f
                    repeatMode = Player.REPEAT_MODE_ONE
                    playWhenReady = isPlaying
                }
        }

    LaunchedEffect(isPlaying) {
        if (exoPlayer.playWhenReady != isPlaying) {
            exoPlayer.playWhenReady = isPlaying
        }
    }

    DisposableEffect(exoPlayer, primary, fallback) {
        val listener =
            object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val next =
                        when (currentUrl) {
                            primary -> fallback
                            else -> null
                        }
                    if (!next.isNullOrBlank()) {
                        currentUrl = next
                        isVideoReady = false
                    }
                }

                override fun onRenderedFirstFrame() {
                    isVideoReady = true
                }
            }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(currentUrl, exoPlayer) {
        val normalized = currentUrl.trim()
        val lowercaseUrl = normalized.lowercase(Locale.ROOT)
        val mimeType =
            when {
                lowercaseUrl.contains("m3u8") -> MimeTypes.APPLICATION_M3U8
                lowercaseUrl.contains("mp4") -> MimeTypes.VIDEO_MP4
                primary != null && currentUrl == primary -> MimeTypes.APPLICATION_M3U8
                fallback != null && currentUrl == fallback -> MimeTypes.VIDEO_MP4
                else -> MimeTypes.APPLICATION_M3U8
            }

        val mediaItem =
            MediaItem.Builder()
                .setUri(normalized)
                .setMimeType(mimeType)
                .build()

        exoPlayer.stop()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "canvasAlpha",
    )

    // A bounded CPU image is the source, never the AndroidView RenderNode.
    // This keeps the optical pipeline identical and prevents capture cycles.
    LaunchedEffect(texture, currentUrl, isVideoReady, isPlaying, frameReceiver != null, lifecycle) {
        frameReceiver?.invoke(null)
        val view = texture
        if (view != null && isVideoReady && frameReceiver != null) {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            do {
                if (view.isAvailable && view.width > 0 && view.height > 0) {
                    val width = minOf(256, view.width)
                    val height = (width.toFloat() * view.height / view.width).toInt().coerceIn(1, 512)
                    val bitmap = runCatching { view.getBitmap(width, height) }.getOrNull()
                    frameReceiver?.invoke(bitmap?.asImageBitmap())
                }
                kotlinx.coroutines.delay(67)
            } while (isPlaying)
            }
        }
    }
    DisposableEffect(initial) {
        onDispose { frameReceiver?.invoke(null) }
    }

    AndroidView(
        factory = { viewContext ->
            // Media3 PlayerView defaults to SurfaceView, whose separate window
            // composition leaked a black box into Glass Studio on some Samsung
            // builds. TextureView is strictly clipped/composited with Compose.
            (android.view.LayoutInflater.from(viewContext)
                .inflate(R.layout.galax_canvas_texture_player, null, false) as PlayerView).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        update = { view ->
            texture = view.videoSurfaceView as? android.view.TextureView
            if (view.player !== exoPlayer) view.player = exoPlayer
            if (view.resizeMode != resizeMode) view.resizeMode = resizeMode
        },
        modifier = modifier.alpha(alpha),
    )
}