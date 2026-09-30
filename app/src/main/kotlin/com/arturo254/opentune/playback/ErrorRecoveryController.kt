package com.arturo254.opentune.playback

import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.ExoPlayer
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.extensions.SilentHandler
import com.arturo254.opentune.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Owns playback-error recovery state and decisions.
 *
 * Stream resolution itself remains outside this class for now; this controller
 * owns the recovery lifecycle around those streams: retry limits, invalidation,
 * network wait/resume, cache eviction, silence-skip recovery and fresh-stream
 * validation.
 */
internal class ErrorRecoveryController(
    private val player: ExoPlayer,
    private val scopeProvider: () -> CoroutineScope,
    private val downloadCache: Cache,
    private val playerCache: Cache,
    private val preferredStreamClientProvider: () -> PlayerStreamClient,
    private val autoSkipNextOnErrorProvider: () -> Boolean,
    private val skipSilenceEnabledProvider: () -> Boolean,
    private val disableSkipSilence: suspend () -> Unit,
) {
    val waitingForNetworkConnection = MutableStateFlow(false)

    private var isNetworkConnected = false
    private var consecutivePlaybackErrors = 0

    private val playbackUrlCache =
        ConcurrentHashMap<String, Pair<String, Long>>()
    private val playbackWarmupInFlight =
        ConcurrentHashMap.newKeySet<String>()
    private val streamRecoveryState =
        ConcurrentHashMap<String, Pair<Int, Long>>()

    @Volatile
    private var pendingStreamRefreshValidationMediaId: String? = null

    @Volatile
    private var refreshValidatedPlayingMediaId: String? = null

    fun onNetworkConnectivityChanged(isConnected: Boolean) {
        isNetworkConnected = isConnected

        if (isConnected && waitingForNetworkConnection.value) {
            waitingForNetworkConnection.value = false
            if (
                player.currentMediaItem != null &&
                player.playWhenReady &&
                player.playbackState == Player.STATE_IDLE
            ) {
                player.prepare()
                player.play()
            }
        }
    }

    fun handlePlayerError(error: PlaybackException) {
        val nestedCause = error.cause?.cause
        val isConnectionError =
            nestedCause is PlaybackException &&
                nestedCause.errorCode ==
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED

        if (!isNetworkConnected || isConnectionError) {
            waitingForNetworkConnection.value = true
            return
        }

        val currentMediaId = player.currentMediaItem?.mediaId
        val httpStatusCode = error.httpStatusCodeOrNull()

        if (
            currentMediaId != null &&
            YTPlayerUtils.isBotDetectionException(error) &&
            markAndCheckRecoveryAllowance(currentMediaId)
        ) {
            Timber
                .tag(TAG)
                .w(
                    "Bot detection error for $currentMediaId — clearing caches and retrying with fresh stream",
                )
            YTPlayerUtils.invalidateCachedStreamUrls(currentMediaId)
            removeCachedStream(currentMediaId)
            pendingStreamRefreshValidationMediaId = currentMediaId
            player.prepare()
            player.playWhenReady = true
            return
        }

        val shouldAttemptStreamRefresh =
            currentMediaId != null &&
                ErrorRecoveryPolicy.shouldAttemptStreamRefresh(
                    StreamRefreshSignals(
                        badHttpStatusError =
                            error.errorCode ==
                                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                        fileNotFoundError =
                            error.errorCode ==
                                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                        readPositionOutOfRangeError =
                            error.errorCode ==
                                PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        httpStatusCode = httpStatusCode,
                    ),
                )

        if (
            currentMediaId != null &&
            error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND
        ) {
            scopeProvider().launch(Dispatchers.IO + SilentHandler) {
                runCatching { downloadCache.removeResource(currentMediaId) }
                runCatching { playerCache.removeResource(currentMediaId) }
            }
        }

        if (
            shouldAttemptStreamRefresh &&
            currentMediaId != null &&
            shouldSkipRedundantStreamRefresh(currentMediaId)
        ) {
            Timber
                .tag(TAG)
                .w(
                    "Skipping redundant stream refresh for $currentMediaId after validated recovery; resuming playback without URL refresh",
                )
            refreshValidatedPlayingMediaId = null
            player.prepare()
            player.playWhenReady = true
            return
        }

        if (
            shouldAttemptStreamRefresh &&
            currentMediaId != null &&
            markAndCheckRecoveryAllowance(currentMediaId)
        ) {
            val failingStreamClientKey =
                cachedStream(currentMediaId)
                    ?.first
                    ?.toHttpUrlOrNull()
                    ?.queryParameter("c")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }

            Timber
                .tag(TAG)
                .w(
                    "Attempting stream refresh for " +
                        currentMediaId +
                        " (http=" +
                        httpStatusCode +
                        ", code=" +
                        error.errorCode +
                        ", client=" +
                        (failingStreamClientKey ?: "unknown") +
                        ")",
                )

            YTPlayerUtils.markStreamClientFailed(
                currentMediaId,
                failingStreamClientKey,
                httpStatusCode,
            )
            YTPlayerUtils.markPreferredClientFailed(
                currentMediaId,
                preferredStreamClientProvider(),
                httpStatusCode,
            )
            YTPlayerUtils.invalidateCachedStreamUrls(currentMediaId)
            removeCachedStream(currentMediaId)
            pendingStreamRefreshValidationMediaId = currentMediaId
            player.prepare()
            player.playWhenReady = true
            return
        }

        val causeText =
            (error.cause?.stackTraceToString() ?: error.stackTraceToString())
                .lowercase()

        if (
            ErrorRecoveryPolicy.looksLikeSilenceProcessorFailure(
                skipSilenceEnabled = skipSilenceEnabledProvider(),
                causeText = causeText,
            )
        ) {
            scopeProvider().launch {
                try {
                    disableSkipSilence()
                    player.skipSilenceEnabled = false

                    val currentPosition = player.currentPosition
                    val targetPosition =
                        min(
                            currentPosition + 1_500L,
                            if (player.duration > 0) {
                                player.duration - 1_000L
                            } else {
                                currentPosition + 1_500L
                            },
                        )

                    player.seekTo(targetPosition)
                    player.prepare()
                    player.play()
                    return@launch
                } catch (error: Throwable) {
                    Timber
                        .tag(TAG)
                        .e(error, "Failed to recover from silence-skipper error")
                }

                applyTerminalErrorPolicy()
            }
            return
        }

        applyTerminalErrorPolicy()
    }

    fun onMediaItemTransition(activeMediaId: String?) {
        clearStreamRefreshGuards(activeMediaId)
    }

    fun onPlaybackStateChanged(playbackState: Int) {
        val activeMediaId = player.currentMediaItem?.mediaId
        clearStreamRefreshGuards(activeMediaId)

        if (
            playbackState == Player.STATE_READY &&
            player.playWhenReady &&
            player.isPlaying &&
            activeMediaId != null &&
            pendingStreamRefreshValidationMediaId == activeMediaId
        ) {
            refreshValidatedPlayingMediaId = activeMediaId
            pendingStreamRefreshValidationMediaId = null
            streamRecoveryState.remove(activeMediaId)
            Timber
                .tag(TAG)
                .i(
                    "Stream refresh validated and playback resumed for $activeMediaId",
                )
        }
    }

    fun retryCurrentFromFreshStream() {
        val mediaId = player.currentMediaItem?.mediaId ?: return
        clearStreamRefreshGuards(mediaId)
        YTPlayerUtils.invalidateCachedStreamUrls(mediaId)
        removeCachedStream(mediaId)
        pendingStreamRefreshValidationMediaId = mediaId
        player.prepare()
        player.playWhenReady = true
    }

    fun resetForPlaybackStop() {
        clearStreamRefreshGuards()
        waitingForNetworkConnection.value = false
        consecutivePlaybackErrors = 0
    }

    fun cachedStream(mediaId: String): Pair<String, Long>? =
        playbackUrlCache[mediaId]

    fun validCachedStream(
        mediaId: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Pair<String, Long>? =
        playbackUrlCache[mediaId]
            ?.takeIf { (_, expiresAt) -> expiresAt > nowMs }

    fun hasValidCachedStream(
        mediaId: String,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean =
        validCachedStream(mediaId, nowMs) != null

    fun cacheStream(
        mediaId: String,
        url: String,
        expiresAtMs: Long,
    ) {
        playbackUrlCache[mediaId] = url to expiresAtMs
    }

    fun removeCachedStream(mediaId: String) {
        playbackUrlCache.remove(mediaId)
    }

    fun tryBeginWarmup(mediaId: String): Boolean =
        playbackWarmupInFlight.add(mediaId)

    fun finishWarmup(mediaId: String) {
        playbackWarmupInFlight.remove(mediaId)
    }

    private fun applyTerminalErrorPolicy() {
        if (autoSkipNextOnErrorProvider()) {
            skipOnError()
        } else {
            player.pause()
        }
    }

    private fun skipOnError() {
        consecutivePlaybackErrors =
            ErrorRecoveryPolicy.nextConsecutiveErrorCount(
                consecutivePlaybackErrors,
            )

        val nextWindowIndex = player.nextMediaItemIndex
        val hasNext = nextWindowIndex != C.INDEX_UNSET

        if (
            ErrorRecoveryPolicy.shouldSkipToNext(
                nextConsecutiveErrorCount = consecutivePlaybackErrors,
                hasNextItem = hasNext,
            )
        ) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }

        player.pause()
        consecutivePlaybackErrors = 0
    }

    private fun PlaybackException.httpStatusCodeOrNull(): Int? {
        var throwable: Throwable? = cause
        while (throwable != null) {
            if (throwable is HttpDataSource.InvalidResponseCodeException) {
                return throwable.responseCode
            }
            throwable = throwable.cause
        }
        return null
    }

    private fun markAndCheckRecoveryAllowance(mediaId: String): Boolean {
        val now = System.currentTimeMillis()
        val (count, lastAt) =
            streamRecoveryState[mediaId] ?: (0 to 0L)
        val nextCount =
            if (now - lastAt > RECOVERY_WINDOW_MS) 1 else count + 1

        if (nextCount > MAX_STREAM_RECOVERY_ATTEMPTS) return false

        streamRecoveryState[mediaId] = nextCount to now
        return true
    }

    private fun shouldSkipRedundantStreamRefresh(mediaId: String): Boolean {
        if (refreshValidatedPlayingMediaId != mediaId) return false

        val expiresAt = cachedStream(mediaId)?.second ?: return false
        if (expiresAt <= System.currentTimeMillis()) {
            refreshValidatedPlayingMediaId = null
            return false
        }

        return true
    }

    private fun clearStreamRefreshGuards(activeMediaId: String? = null) {
        val normalizedActiveMediaId =
            activeMediaId
                ?.trim()
                ?.takeIf { it.isNotBlank() }

        if (
            normalizedActiveMediaId == null ||
            refreshValidatedPlayingMediaId != normalizedActiveMediaId
        ) {
            refreshValidatedPlayingMediaId = null
        }

        if (
            normalizedActiveMediaId == null ||
            pendingStreamRefreshValidationMediaId != normalizedActiveMediaId
        ) {
            pendingStreamRefreshValidationMediaId = null
        }
    }

    private companion object {
        const val TAG = "ErrorRecoveryController"
        const val RECOVERY_WINDOW_MS = 45_000L
        const val MAX_STREAM_RECOVERY_ATTEMPTS = 2
    }
}
