package com.arturo254.opentune.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.arturo254.opentune.utils.GlobalLog
import com.arturo254.opentune.constants.PreferredLyricsProvider
import com.arturo254.opentune.constants.PreferredLyricsProviderKey
import com.arturo254.opentune.constants.ProviderOrderKey
import com.arturo254.opentune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.arturo254.opentune.extensions.toEnum
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.reportException
import com.arturo254.opentune.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val baseProviders =
        listOf(
            SimpMusicLyricsProvider,
            BetterLyricsProvider,
            LrcLibLyricsProvider,
            NetEaseLyricsProvider,
            KuGouLyricsProvider,
            YouTubeSubtitleLyricsProvider,
            YouTubeLyricsProvider,
        )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(
        mediaMetadata: MediaMetadata,
        preferredProviderOnly: Boolean = false,
    ): String {
        currentLyricsJob?.cancel()

        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            GlobalLog.append(
                Log.DEBUG,
                "LyricsHelper",
                "Found lyrics in cache for " + mediaMetadata.title,
            )
            return cached.lyrics
        }

        val isNetworkAvailable =
            try {
                networkConnectivity.isCurrentlyConnected()
            } catch (_: Exception) {
                true
            }

        if (!isNetworkAvailable) {
            return LYRICS_NOT_FOUND
        }

        val ordered = orderedProviders()
        val providers =
            if (preferredProviderOnly) {
                ordered.take(1)
            } else {
                ordered
            }.filter { it.isEnabled(context) }

        if (providers.isEmpty()) return LYRICS_NOT_FOUND

        val results =
            coroutineScope {
                providers.mapIndexed { index, provider ->
                    async(Dispatchers.IO) {
                        val lyrics =
                            withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                                try {
                                    provider
                                        .getLyrics(
                                            mediaMetadata.id,
                                            mediaMetadata.title,
                                            mediaMetadata.artists.joinToString { it.name },
                                            mediaMetadata.album?.title,
                                            mediaMetadata.duration,
                                        )
                                        .getOrElse { throwable ->
                                            reportException(throwable)
                                            LYRICS_NOT_FOUND
                                        }
                                } catch (throwable: Throwable) {
                                    if (throwable is kotlinx.coroutines.CancellationException) {
                                        throw throwable
                                    }
                                    reportException(throwable)
                                    LYRICS_NOT_FOUND
                                }
                            } ?: LYRICS_NOT_FOUND

                        LyricsCandidate(
                            providerIndex = index,
                            providerName = provider.name,
                            lyrics = lyrics,
                            quality = lyricsQuality(lyrics),
                        )
                    }
                }.awaitAll()
            }

        val best =
            results
                .filter { it.quality > 0 }
                .maxWithOrNull(
                    compareBy<LyricsCandidate> { it.quality }
                        .thenBy { -it.providerIndex }
                )
                ?: return LYRICS_NOT_FOUND

        cache.put(
            mediaMetadata.id,
            listOf(
                LyricsResult(
                    providerName = best.providerName,
                    lyrics = best.lyrics,
                )
            ),
        )

        GlobalLog.append(
            Log.DEBUG,
            "LyricsHelper",
            "Selected " + best.providerName + " lyrics quality=" + best.quality,
        )

        return best.lyrics
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }

        if (!isNetworkAvailable) {
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        val providers = orderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) lyricsCallback@{ lyrics ->
                            if (!isMeaningfulLyrics(lyrics)) return@lyricsCallback
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    private fun PreferredLyricsProvider.toLyricsProvider(): LyricsProvider = when (this) {
        PreferredLyricsProvider.LRCLIB -> LrcLibLyricsProvider
        PreferredLyricsProvider.NETEASE -> NetEaseLyricsProvider
        PreferredLyricsProvider.KUGOU -> KuGouLyricsProvider
        PreferredLyricsProvider.BETTER_LYRICS -> BetterLyricsProvider
        PreferredLyricsProvider.SIMPMUSIC -> SimpMusicLyricsProvider
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        val savedOrder = context.dataStore.data
            .first()[ProviderOrderKey]
            ?.split(",")
            ?.mapNotNull { name -> runCatching { PreferredLyricsProvider.valueOf(name) }.getOrNull() }
            ?.map { it.toLyricsProvider() }

        if (!savedOrder.isNullOrEmpty()) {
            val allProviders = savedOrder.toMutableList()
            PreferredLyricsProvider.entries.forEach { enumProvider ->
                val provider = enumProvider.toLyricsProvider()
                if (provider !in allProviders) allProviders.add(provider)
            }
            return allProviders
        }

        val preferred = context.dataStore.data
            .first()[PreferredLyricsProviderKey]
            .toEnum(PreferredLyricsProvider.LRCLIB)

        val first = preferred.toLyricsProvider()
        return listOf(first) + baseProviders.filterNot { it == first }
    }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    private fun isEnhancedLyrics(lyrics: String): Boolean =
        LyricsUtils.WORD_TIME_REGEX.containsMatchIn(lyrics) || LyricsUtils.isTtml(lyrics)

    private fun lyricsQuality(lyrics: String): Int {
        if (!isMeaningfulLyrics(lyrics)) return 0
        if (isEnhancedLyrics(lyrics)) return 4

        val timedLines = TIMESTAMP_REGEX.findAll(lyrics).count()
        return when {
            timedLines >= 4 -> 3
            timedLines > 0 -> 2
            else -> 1
        }
    }

    private data class LyricsCandidate(
        val providerIndex: Int,
        val providerName: String,
        val lyrics: String,
        val quality: Int,
    )

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 8
        private const val PROVIDER_TIMEOUT_MS = 7_000L
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)