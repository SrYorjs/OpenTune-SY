package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.component.Artwork
import com.arturo254.opentune.core.designsystem.component.OpenTuneEmptyPresentation
import com.arturo254.opentune.core.designsystem.component.OpenTuneEmptyState
import com.arturo254.opentune.core.designsystem.component.OpenTuneLoadingState
import com.arturo254.opentune.core.designsystem.theme.colors
import com.arturo254.opentune.core.designsystem.theme.typography
import com.arturo254.opentune.core.designsystem.theme.spacing
import com.arturo254.opentune.core.designsystem.theme.sizes
import com.arturo254.opentune.core.designsystem.theme.shapes
import com.arturo254.opentune.core.designsystem.theme.surfaces
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.constants.UniversalAudioSelectionModeKey
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.sources.offline.OfflineTrackState
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.ui.component.liquidGlassScreenBackground
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import com.arturo254.opentune.utils.rememberEnumPreference
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalSearchScreen(
    query: String,
    pureBlack: Boolean,
    viewModel: UniversalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val designSpacing = OpenTuneDesignSystem.spacing
    val designSizes = OpenTuneDesignSystem.sizes
    val playerConnection = LocalPlayerConnection.current ?: return
    val state by viewModel.state.collectAsState()
    val offlineAssets by viewModel.offlineAssets.collectAsState()
    val lazyListState = rememberLazyListState()
    val activeMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val scope = rememberCoroutineScope()
    var resolvingPlaybackKey by remember { mutableStateOf<String?>(null) }
    var preparingOfflineKey by remember { mutableStateOf<String?>(null) }
    var selectionDialogKey by remember { mutableStateOf<String?>(null) }
    val manualChoices = remember { mutableStateMapOf<String, SourceVariantKey>() }
    var qualityMode by rememberEnumPreference(
        UniversalAudioSelectionModeKey,
        AudioSelectionMode.AUTOMATIC,
    )

    val playResult:
        (UnifiedTrackSearchResult, SourceVariant?, List<SourceVariant>) -> Unit =
        playback@{ recording, chosen, fallbackList ->
            if (resolvingPlaybackKey != null) return@playback
            val candidates = listOfNotNull(chosen) + fallbackList
            if (candidates.isEmpty()) return@playback

            scope.launch {
                resolvingPlaybackKey = recording.stableResultKey()
                try {
                    val played =
                        viewModel.playFirstAvailable(
                            connection = playerConnection,
                            variants = candidates,
                            queueTitle = context.getString(R.string.queue_searched_songs),
                        )
                    if (!played) {
                        Toast.makeText(
                            context,
                            R.string.universal_versions_play_error,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                } finally {
                    resolvingPlaybackKey = null
                }
            }
        }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    LaunchedEffect(lazyListState, state.result.tracks.size, state.result.hasMore) {
        if (!state.result.hasMore) return@LaunchedEffect

        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val lastVisibleIndex =
                layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index
                    ?: -1

            layoutInfo.totalItemsCount > 0 &&
                lastVisibleIndex >= layoutInfo.totalItemsCount - 5
        }.distinctUntilChanged()
            .filter { it }
            .collect {
                viewModel.loadMore()
            }
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(
                if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
            ),
    ) {
    LazyColumn(
        state = lazyListState,
        contentPadding =
            PaddingValues(
                start = designSpacing.md,
                top = designSpacing.md,
                end = designSpacing.md,
                bottom = designSpacing.searchBottomInset,
            ),
        verticalArrangement = Arrangement.spacedBy(designSpacing.sm),
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    if (pureBlack) Color.Black else MaterialTheme.colorScheme.background,
                )
                .liquidGlassScreenBackground(pureBlack = pureBlack),
    ) {
        item(key = "galax_search_heading") {
            GalaxSearchEditorialHero(
                query = query,
                count = state.result.tracks.size,
                isLoading = state.isLoading,
            )
        }
        item(key = "galax_quality_controls") {
            GalaxSearchQualityBar(
                mode = qualityMode,
                onModeChange = { qualityMode = it },
            )
        }
        if (state.result.tracks.isNotEmpty()) {
            item(key = "galax_search_result_heading") {
                AtlasResultsHeader(
                    title = "Canciones",
                    count = state.result.tracks.size,
                )
            }
        }

        if (state.result.failedSourceCount > 0) {
            item(key = "source_warning") {
                AtlasResultsWarning(
                    title = stringResource(R.string.some_sources_unavailable),
                )
            }
        }

        if (state.isLoading && state.result.tracks.isEmpty()) {
            item(key = "loading") {
                AtlasResultsSkeleton()
            }
        }

        if (
            state.query.isNotBlank() &&
            !state.isLoading &&
            state.result.tracks.isEmpty()
        ) {
            item(key = "empty") {
                GalaxUniversalNoResults(
                    message = stringResource(R.string.no_results_found),
                )
            }
        }

        itemsIndexed(
            items = state.result.tracks,
            key = { index, result -> universalResultRowKey(index, result.stableResultKey()) },
            contentType = { _, _ -> "universal_track" },
        ) { _, result ->
            val variantKeys =
                result.variants
                    .map { variant -> variant.provider to variant.sourceId }
                    .toSet()
            val trackedOfflineAsset =
                offlineAssets.firstOrNull { asset ->
                    asset.discoverySources.any { ref ->
                        (ref.provider to ref.sourceId) in variantKeys
                    }
                }
            val offlineVariant =
                trackedOfflineAsset
                    ?.takeIf { asset -> asset.state == OfflineTrackState.AVAILABLE }
                    ?.toSourceVariant()
                    ?: result.variants.firstOrNull {
                        it.provider == ProviderId.OPEN_TUNE_OFFLINE
                    }
            val resultKey = result.stableResultKey()
            val playable =
                viewModel.selectVariant(
                    result = result,
                    variants = result.variants + listOfNotNull(offlineVariant),
                    mode = qualityMode,
                    manualVariant = manualChoices[resultKey],
                )
            val catalogFallbacks =
                if (playable == null) {
                    result.variants.filter { variant ->
                        variant.provider == ProviderId.DEEZER ||
                            variant.provider == ProviderId.SPOTIFY ||
                            variant.provider == ProviderId.TIDAL ||
                            variant.provider == ProviderId.APPLE_MUSIC
                    }
                } else {
                    emptyList()
                }
            val isActive =
                (playable != null && playable.sourceId == activeMetadata?.id) ||
                    (
                        activeMetadata != null &&
                            (playable == null || playable.isCatalogOnly()) &&
                            activeMetadata?.title.equals(result.identity.title, ignoreCase = true) &&
                            result.identity.artists.any { identityArtist ->
                                activeMetadata?.artists
                                    ?.any {
                                        it.name.equals(
                                            identityArtist.name,
                                            ignoreCase = true,
                                        )
                                    } == true
                            }
                    )

            val hasOfflineCopy =
                offlineVariant != null
            val isOfflinePending =
                trackedOfflineAsset?.state == OfflineTrackState.PENDING
            val canDownload =
                !hasOfflineCopy &&
                    !isOfflinePending &&
                    result.variants.any { variant ->
                        variant.provider != ProviderId.LOCAL &&
                            variant.provider != ProviderId.OPEN_TUNE_OFFLINE
                    }

            UnifiedTrackRow(
                modifier = Modifier.animateItem(),
                result = result,
                preferredDisplayVariant = playable,
                onVersionsClick = { selectionDialogKey = resultKey },
                isActive = isActive,
                isPlaying = isPlaying,
                isResolving = resolvingPlaybackKey == resultKey,
                isOfflineAvailable = hasOfflineCopy,
                isPreparingOffline =
                    preparingOfflineKey == resultKey ||
                        isOfflinePending,
                onDownload =
                    if (canDownload) {
                        {
                            if (preparingOfflineKey == null) {
                                scope.launch {
                                    preparingOfflineKey = resultKey
                                    viewModel
                                        .enqueueOffline(result)
                                        .onSuccess {
                                            Toast
                                                .makeText(
                                                    context,
                                                    R.string.catalog_offline_download_started,
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }.onFailure {
                                            Toast
                                                .makeText(
                                                    context,
                                                    it.message
                                                        ?: context.getString(
                                                            R.string.catalog_source_unplayable,
                                                        ),
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        }
                                    preparingOfflineKey = null
                                }
                            }
                        }
                    } else {
                        null
                    },
                onClick =
                    if (isActive || playable != null || catalogFallbacks.isNotEmpty()) {
                        {
                            if (isActive) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playResult(result, playable, catalogFallbacks)
                            }
                        }
                    } else {
                        null
                    },
            )
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                AtlasResultsLoadIndicator(loading = true)
            }
        }
    }
    }

    val selectedRecording =
        state.result.tracks.firstOrNull { it.stableResultKey() == selectionDialogKey }
    if (selectedRecording != null) {
        UniversalVersionQualityDialog(
            selected = selectedRecording,
            relatedVersions = viewModel.relatedVersions(selectedRecording),
            qualityMode = qualityMode,
            manualVariant = manualChoices[selectedRecording.stableResultKey()],
            onQualityModeChange = { qualityMode = it },
            onSelectVariant = { recording, variant ->
                manualChoices[recording.stableResultKey()] =
                    SourceVariantKey(
                        provider = variant.provider,
                        sourceId = variant.sourceId,
                    )
                selectionDialogKey = null
                playResult(recording, variant, emptyList())
            },
            onClearManual = {
                manualChoices.remove(selectedRecording.stableResultKey())
                selectionDialogKey = null
            },
            onDismiss = { selectionDialogKey = null },
        )
    }
}

@Composable
private fun UnifiedTrackRow(
    result: UnifiedTrackSearchResult,
    preferredDisplayVariant: SourceVariant?,
    isActive: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isOfflineAvailable: Boolean,
    isPreparingOffline: Boolean,
    onDownload: (() -> Unit)?,
    onVersionsClick: () -> Unit,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val selected = preferredDisplayVariant ?: result.recommendedVariant
    val cover = selected?.artworkUrl
        ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val providers = remember(result, preferredDisplayVariant) {
        (result.variants + listOfNotNull(preferredDisplayVariant))
            .distinctBy { it.provider }
            .joinToString(" · ") { providerName(it.provider) }
    }
    GalaxUniversalResultCard(
        modifier = modifier,
        title = result.identity.title,
        artist = result.identity.artists.joinToString { it.name },
        artwork = cover,
        providers = providers,
        quality = qualitySummary(selected),
        catalogOnly = selected?.isCatalogOnly() == true,
        active = isActive,
        playing = isPlaying,
        resolving = isResolving,
        downloaded = isOfflineAvailable,
        downloading = isPreparingOffline,
        onClick = onClick,
        onVersions = onVersionsClick,
        onDownload = onDownload,
    )
}

private fun UnifiedTrackSearchResult.stableResultKey(): String =
    identity.isrc
        ?.takeIf(String::isNotBlank)
        ?.let { "isrc:" + it.uppercase() }
        ?: buildString {
            append(identity.title.trim().lowercase())
            append('|')
            append(
                identity.artists
                    .joinToString(",") { artist -> artist.name.trim().lowercase() },
            )
            append('|')
            append(identity.durationMs ?: -1L)
            append('|')
            append(identity.album?.canonicalId ?: identity.album?.title.orEmpty())
            append('|')
            append(
                identity.version.traits
                    .map { trait -> trait.name }
                    .sorted()
                    .joinToString(","),
            )
            append('|')
            append(identity.version.label.orEmpty().trim().lowercase())
        }

private fun providerName(provider: ProviderId): String =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> "YouTube Music"
        ProviderId.LOCAL -> "Local"
        ProviderId.DEEZER -> "Deezer"
        ProviderId.SPOTIFY -> "Spotify"
        ProviderId.TIDAL -> "TIDAL"
        ProviderId.APPLE_MUSIC -> "Apple Music"
        ProviderId.SOUNDCLOUD -> "SoundCloud"
        ProviderId.OPEN_TUNE_OFFLINE -> "Offline"
        else -> provider.value
    }

private fun providerShortName(provider: ProviderId): String =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> "YT Music"
        ProviderId.LOCAL -> "Local"
        ProviderId.DEEZER -> "Deezer"
        ProviderId.SPOTIFY -> "Spotify"
        ProviderId.TIDAL -> "TIDAL"
        ProviderId.APPLE_MUSIC -> "Apple"
        ProviderId.SOUNDCLOUD -> "SC"
        ProviderId.OPEN_TUNE_OFFLINE -> "Offline"
        else -> provider.value
    }

private fun qualitySummary(variant: SourceVariant?): String? {
    val quality = variant?.quality ?: return null
    val parts = mutableListOf<String>()

    if (quality.lossless == true) {
        parts += "Lossless"
    }
    quality.bitDepth?.takeIf { it > 0 }?.let { parts += "$it-bit" }
    quality.sampleRateHz
        ?.takeIf { it > 0 }
        ?.let { sampleRate ->
            val khz = sampleRate / 1000.0
            parts +=
                if (sampleRate % 1000 == 0) {
                    (sampleRate / 1000).toString() + " kHz"
                } else {
                    String.format(java.util.Locale.US, "%.1f kHz", khz)
                }
        }
    quality.bitrateBitsPerSecond
        ?.takeIf { it > 0 }
        ?.let { bitrate -> parts += (bitrate / 1000).toString() + " kbps" }

    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

internal fun universalResultRowKey(index: Int, identity: String): String =
    "universal-result:" + index + ":" + identity
