package com.arturo254.opentune

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.navigation.NavHostController
import com.arturo254.opentune.constants.TogetherDisplayNameKey
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.extensions.toMediaItem
import com.arturo254.opentune.playback.MusicService
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.ListQueue
import com.arturo254.opentune.ui.screens.LOGIN_URL_ARGUMENT
import com.arturo254.opentune.ui.screens.Screens
import com.arturo254.opentune.ui.screens.buildLoginRoute
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns external intents and deep-link routing for [MainActivity].
 *
 * It keeps pending intents until Compose navigation is ready and keeps pending
 * playback/Together actions until [MusicService] is connected.
 */
internal class MainActivityDeepLinkController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val playerConnectionProvider: () -> PlayerConnection?,
) {
    private var navController: NavHostController? = null
    private var pendingIntent: Intent? = null
    private var pendingDeepLinkSong: MediaItem? = null
    private var pendingTogetherJoinLink: String? = null

    fun attachNavController(
        navController: NavHostController,
        initialIntent: Intent? = null,
    ) {
        this.navController = navController

        val intentToHandle = pendingIntent ?: initialIntent
        pendingIntent = null
        intentToHandle?.let { dispatchIntent(it, navController) }
    }

    fun handleIntent(intent: Intent) {
        val controller = navController
        if (controller == null) {
            pendingIntent = intent
            return
        }

        dispatchIntent(intent, controller)
    }

    fun onPlayerConnectionAvailable() {
        playPendingDeepLinkSongIfReady()
        joinPendingTogetherIfReady()
    }

    private fun dispatchIntent(
        intent: Intent,
        navController: NavHostController,
    ) {
        if (intent.action == MainActivity.ACTION_DOWNLOAD_QUEUE) {
            navController.navigate(Screens.DownloadQueue.route)
            return
        }

        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        val authority = uri.authority?.lowercase()

        if (uri.scheme.equals("OpenTune", ignoreCase = true) && authority == "together") {
            pendingTogetherJoinLink = uri.toString()
            startMusicServiceSafely()
            joinPendingTogetherIfReady()
            return
        }

        if (uri.scheme.equals("OpenTune", ignoreCase = true) && authority == "login") {
            navController.navigate(buildLoginRoute(uri.getQueryParameter(LOGIN_URL_ARGUMENT)))
            return
        }

        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    scope.launch {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                navController.navigate("album/$browseId")
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            else -> {
                val videoId =
                    when {
                        path == "watch" -> uri.getQueryParameter("v")
                        uri.host == "youtu.be" -> uri.pathSegments.firstOrNull()
                        else -> null
                    }

                val playlistId = uri.getQueryParameter("list")

                videoId?.let { vid ->
                    scope.launch {
                        val result =
                            withContext(Dispatchers.IO) {
                                YouTube.queue(listOf(vid), playlistId)
                            }

                        result.onSuccess { queued ->
                            val mediaItem =
                                queued.firstOrNull { it.id == vid }?.toMediaItem()
                                    ?: queued.firstOrNull()?.toMediaItem()
                                    ?: MediaItem
                                        .Builder()
                                        .setMediaId(vid)
                                        .setUri(vid)
                                        .setCustomCacheKey(vid)
                                        .build()

                            pendingDeepLinkSong = mediaItem
                            startMusicServiceSafely()
                            playPendingDeepLinkSongIfReady()
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    private fun playPendingDeepLinkSongIfReady() {
        val pending = pendingDeepLinkSong ?: return
        val connection = playerConnectionProvider() ?: return

        pendingDeepLinkSong = null
        connection.playQueue(ListQueue(items = listOf(pending)))
    }

    private fun joinPendingTogetherIfReady() {
        val pending = pendingTogetherJoinLink ?: return
        val connection = playerConnectionProvider() ?: return

        pendingTogetherJoinLink = null
        scope.launch(Dispatchers.IO) {
            val displayName =
                runCatching {
                    context.dataStore.data.first()[TogetherDisplayNameKey]
                }.getOrNull()
                    ?.trim()
                    .orEmpty()
                    .ifBlank {
                        Build.MODEL ?: context.getString(R.string.app_name)
                    }

            withContext(Dispatchers.Main) {
                connection.service.joinTogether(pending, displayName)
            }
        }
    }

    private fun startMusicServiceSafely() {
        runCatching {
            context.startService(Intent(context, MusicService::class.java))
        }.onFailure {
            reportException(it)
        }
    }
}
