package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidEffect
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import java.util.Locale

/** One scrolling chooser keeps every source and quality action reachable on short windows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UniversalVersionQualityDialog(
    selected: UnifiedTrackSearchResult,
    relatedVersions: List<UnifiedTrackSearchResult>,
    qualityMode: AudioSelectionMode,
    manualVariant: SourceVariantKey?,
    onQualityModeChange: (AudioSelectionMode) -> Unit,
    onSelectVariant: (UnifiedTrackSearchResult, SourceVariant) -> Unit,
    onClearManual: () -> Unit,
    onDismiss: () -> Unit,
) {
    val recordings = versionDisplayRecordings(selected, relatedVersions)
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .height(maxHeight * .94f)
                    .clip(RoundedCornerShape(30.dp))
            ) {
                // This dialog is a separate window and does not contain the app's mini player.
                CompositionLocalProvider(
                    LocalPlayerAwareWindowInsets provides WindowInsets(0, 0, 0, 0),
                    LocalPrismBackdrop provides null,
                ) {
                    PrismScreenFrame(reserveAppBar = false, insetTop = false) { _, insets ->
                        LazyColumn(
                            contentPadding = insets,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            item("versions/header") {
                                Row(
                                    Modifier.fillMaxWidth()
                                        .padding(start = 24.dp, top = 12.dp, end = 12.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(
                                        Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.prism_results_audio_title),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            selected.identity.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = onDismiss) {
                                        Icon(
                                            painterResource(R.drawable.close),
                                            stringResource(R.string.universal_versions_close),
                                        )
                                    }
                                }
                            }
                            item("versions/quality") {
                                Column(
                                    Modifier.padding(horizontal = 24.dp)
                                        .fillMaxWidth()
                                        .galaxLiquidEffect(
                                            shapeOverride = RoundedCornerShape(22.dp)
                                        )
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.prism_results_quality_summary,
                                            recordings.size,
                                            recordings
                                                .flatMap { it.variants }
                                                .map { it.provider }
                                                .distinct()
                                                .size,
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        stringResource(R.string.universal_quality_title),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        listOf(
                                                AudioSelectionMode.AUTOMATIC to
                                                    R.string.universal_quality_auto,
                                                AudioSelectionMode.BEST_QUALITY to
                                                    R.string.universal_quality_best,
                                                AudioSelectionMode.DATA_SAVER to
                                                    R.string.universal_quality_saver,
                                            )
                                            .forEach { (mode, label) ->
                                                FilterChip(
                                                    selected = mode == qualityMode,
                                                    onClick = { onQualityModeChange(mode) },
                                                    label = { Text(stringResource(label)) },
                                                    shape = RoundedCornerShape(50),
                                                )
                                            }
                                    }
                                    Text(
                                        stringResource(R.string.universal_versions_quality_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            if (manualVariant != null)
                                item("versions/reset") {
                                    TextButton(
                                        onClick = onClearManual,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    ) {
                                        Text(stringResource(R.string.universal_versions_reset))
                                    }
                                }
                            recordings.forEachIndexed { index, recording ->
                                item("versions/title/$index") {
                                    PrismSectionTitle(
                                        recording.recording.identity.title,
                                        recording.recording.identity.version.label
                                            ?: recording.recording.identity.version.traits
                                                .joinToString(" · ") { it.name.replace('_', ' ') }
                                                .takeIf(String::isNotBlank)
                                            ?: stringResource(R.string.universal_versions_original),
                                    )
                                }
                                items(
                                    recording.variants.size,
                                    key = { offset ->
                                        "version/$index/${recording.variants[offset].provider.value}/${recording.variants[offset].sourceId}"
                                    },
                                ) { offset ->
                                    val variant = recording.variants[offset]
                                    val chosen =
                                        manualVariant?.let {
                                            it.provider == variant.provider &&
                                                it.sourceId == variant.sourceId
                                        } == true
                                    Row(
                                        Modifier.padding(horizontal = 24.dp)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (chosen)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else
                                                    MaterialTheme.colorScheme.surfaceContainer.copy(
                                                        alpha = .6f
                                                    )
                                            )
                                            .clickable(role = Role.Button) {
                                                onSelectVariant(recording.recording, variant)
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Column(
                                            Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(7.dp),
                                        ) {
                                            Text(
                                                variantProviderName(variant.provider),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                variantQualityDescription(variant)
                                                    ?: stringResource(
                                                        R.string.universal_versions_unknown_quality
                                                    ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (variant.isCatalogOnly())
                                                Text(
                                                    stringResource(
                                                        R.string.universal_versions_catalog
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color =
                                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                        }
                                        Icon(
                                            painterResource(
                                                if (chosen) R.drawable.check else R.drawable.play
                                            ),
                                            null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            item("versions/done") {
                                FilledTonalButton(
                                    onClick = onDismiss,
                                    modifier =
                                        Modifier.padding(horizontal = 24.dp)
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp),
                                ) {
                                    Text(stringResource(R.string.universal_versions_close))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun SourceVariant.isCatalogOnly(): Boolean =
    provider == ProviderId.SPOTIFY ||
        provider == ProviderId.DEEZER ||
        provider == ProviderId.TIDAL ||
        provider == ProviderId.APPLE_MUSIC

internal fun variantProviderName(provider: ProviderId): String =
    when (provider) {
        ProviderId.YOUTUBE_MUSIC -> "YouTube Music"
        ProviderId.OPEN_TUNE_OFFLINE -> "Offline"
        ProviderId.LOCAL -> "Local"
        ProviderId.SPOTIFY -> "Spotify"
        ProviderId.DEEZER -> "Deezer"
        ProviderId.TIDAL -> "TIDAL"
        ProviderId.APPLE_MUSIC -> "Apple Music"
        ProviderId.AUDIUS -> "Audius"
        else -> provider.value
    }

internal fun variantQualityDescription(variant: SourceVariant): String? {
    if (variant.isCatalogOnly()) return null
    val quality = variant.quality ?: return null
    val parts = mutableListOf<String>()
    if (quality.lossless == true) parts += "Lossless"
    val format =
        quality.codec?.takeIf(String::isNotBlank)
            ?: when (quality.mimeType?.lowercase(Locale.ROOT)) {
                "audio/mpeg" -> "MP3"
                "audio/flac",
                "audio/x-flac" -> "FLAC"
                "audio/aac",
                "audio/aacp" -> "AAC"
                "audio/ogg" -> "OGG"
                "audio/opus" -> "Opus"
                "audio/mp4" -> "MP4"
                else -> null
            }
    format?.let(parts::add)
    quality.bitDepth?.takeIf { it > 0 }?.let { parts += "$it-bit" }
    quality.sampleRateHz
        ?.takeIf { it > 0 }
        ?.let { parts += String.format(Locale.US, "%.1f kHz", it / 1000.0) }
    quality.bitrateBitsPerSecond
        ?.takeIf { it > 0 }
        ?.let { parts += (it / 1_000).toString() + " kbps" }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}
