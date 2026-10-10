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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.colors
import com.arturo254.opentune.core.designsystem.theme.typography
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
                start = 12.dp,
                top = 12.dp,
                end = 12.dp,
                bottom = 128.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxSize()
                .liquidGlassScreenBackground(pureBlack = pureBlack),
    ) {
        item(key = "universal_quality_mode") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.universal_quality_title),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 3.dp),
                )
                listOf(
                    AudioSelectionMode.AUTOMATIC to R.string.universal_quality_auto,
                    AudioSelectionMode.BEST_QUALITY to R.string.universal_quality_best,
                    AudioSelectionMode.DATA_SAVER to R.string.universal_quality_saver,
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = qualityMode == mode,
                        onClick = { qualityMode = mode },
                        label = {
                            Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        },
                    )
                }
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }

        if (
            state.query.isNotBlank() &&
            !state.isLoading &&
            state.result.tracks.isEmpty()
        ) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.no_results_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp),
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
) {
    val designColors = OpenTuneDesignSystem.colors
    val designTypography = OpenTuneDesignSystem.typography
    val recommended = preferredDisplayVariant ?: result.recommendedVariant
    val artwork =
        recommended?.artworkUrl
            ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onClick != null,
                    onClick = { onClick?.invoke() },
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                if (artwork != null) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.library_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp),
                    )
                }

                if (isActive) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.28f)),
                    )
                    Icon(
                        painter =
                            painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play,
                            ),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                } else if (isResolving) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.24f)),
                    )
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = Color.White,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = result.identity.title,
                    style = designTypography.songTitle,
                    color =
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = result.identity.artists.joinToString { it.name },
                    style = designTypography.songSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text =
                        (
                            result.variants +
                                listOfNotNull(preferredDisplayVariant)
                        ).distinctBy { it.provider }
                            .joinToString(" • ") { variant -> providerName(variant.provider) },
                    style = designTypography.metadata,
                    color = designColors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                qualitySummary(recommended)?.let { quality ->
                    Text(
                        text =
                            if (recommended?.isCatalogOnly() == true) {
                                quality + " (catálogo)"
                            } else {
                                quality
                            },
                        style = designTypography.metadata,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val displayProvider =
                recommended?.provider
                    ?: result.variants
                        .firstOrNull { variant ->
                            variant.provider == ProviderId.DEEZER ||
                                variant.provider == ProviderId.SPOTIFY ||
                                variant.provider == ProviderId.TIDAL ||
                                variant.provider == ProviderId.APPLE_MUSIC
                        }
                        ?.provider

            if (displayProvider != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = providerShortName(displayProvider),
                    style = designTypography.badge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(designColors.badgeAccent)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }

            IconButton(
                onClick = onVersionsClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.tune),
                    contentDescription = stringResource(R.string.universal_versions_action),
                    modifier = Modifier.size(21.dp),
                )
            }

            when {
                isOfflineAvailable -> {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                isPreparingOffline -> {
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                }

                onDownload != null -> {
                    Spacer(Modifier.width(2.dp))
                    IconButton(
                        onClick = onDownload,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.download),
                            contentDescription = null,
                        )
                    }
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
