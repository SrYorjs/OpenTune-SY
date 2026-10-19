package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
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
                GalaxSearchSectionHeading(
                    title = "Resultados de tu universo",
                    detail = "Toca para reproducir, elegir una versión o gestionar su copia offline.",
                )
            }
        }

        if (state.result.failedSourceCount > 0) {
            item(key = "source_warning") {
                Text(
                    text = stringResource(R.string.some_sources_unavailable),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        if (state.isLoading && state.result.tracks.isEmpty()) {
            item(key = "loading") {
                OpenTuneLoadingState(
                    modifier = Modifier.fillMaxWidth(),
                    indicatorSize = designSizes.searchLoadingIndicator,
                    verticalPadding = 28.dp,
                )
            }
        }

        if (
            state.query.isNotBlank() &&
            !state.isLoading &&
            state.result.tracks.isEmpty()
        ) {
            item(key = "empty") {
                OpenTuneEmptyState(
                    icon = null,
                    text = stringResource(R.string.no_results_found),
                    presentation = OpenTuneEmptyPresentation.COMPACT,
                )
            }
        }

        items(
            items = state.result.tracks,
            key = UnifiedTrackSearchResult::stableResultKey,
        ) { result ->
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
                OpenTuneLoadingState(
                    modifier = Modifier.fillMaxWidth(),
                    indicatorSize = 24.dp,
                    verticalPadding = 16.dp,
                )
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
) {
    val recommended = preferredDisplayVariant ?: result.recommendedVariant
    val artwork = recommended?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val providers = remember(result, preferredDisplayVariant) {
        (result.variants + listOfNotNull(preferredDisplayVariant)).distinctBy { it.provider }
            .joinToString(" · ") { providerName(it.provider) }
    }
    Column(Modifier.fillMaxWidth().clip(com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.panelShape)
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = .035f))
        .clickable(enabled = onClick != null && !isResolving) { onClick?.invoke() }
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Artwork(artwork, Modifier.size(64.dp),
                shape = com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.artworkShape,
                overlay = {
                    if (isActive || isResolving) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .3f)),
                        contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                        else Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play), null, tint = Color.White)
                    }
                })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(result.identity.title, style = MaterialTheme.typography.titleMedium,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(result.identity.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onVersionsClick) {
                Icon(painterResource(R.drawable.tune), stringResource(R.string.universal_versions_action))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(providers, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (recommended?.isCatalogOnly() == true) {
                    Text(stringResource(R.string.universal_versions_catalog), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                qualitySummary(recommended)?.let { quality ->
                    Text(quality, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            when {
                isOfflineAvailable -> Icon(painterResource(R.drawable.offline), stringResource(R.string.offline),
                    Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                isPreparingOffline -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                onDownload != null -> IconButton(onClick = onDownload) {
                    Icon(painterResource(R.drawable.download), stringResource(R.string.download))
                }
            }
        }
    }
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
