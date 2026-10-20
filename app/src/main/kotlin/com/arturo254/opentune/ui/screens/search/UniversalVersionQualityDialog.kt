package com.arturo254.opentune.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import java.util.Locale

/**
 * The chooser never calls provider APIs. It presents already searched recordings,
 * and selection is handed back to the same universal playback engine.
 */
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
    val recordings = listOf(selected) + relatedVersions

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.universal_versions_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GalaxQualitySelectionHeader(
                    versionCount = recordings.size,
                    providerCount = recordings.flatMap { it.variants }.map { it.provider }.distinct().size,
                    manualSelected = manualVariant != null,
                )
                Text(
                    text = stringResource(R.string.universal_quality_title),
                    fontWeight = FontWeight.SemiBold,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                ) {
                    listOf(
                        AudioSelectionMode.AUTOMATIC to R.string.universal_quality_auto,
                        AudioSelectionMode.BEST_QUALITY to R.string.universal_quality_best,
                        AudioSelectionMode.DATA_SAVER to R.string.universal_quality_saver,
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = mode == qualityMode,
                            onClick = { onQualityModeChange(mode) },
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

                Text(
                    text = stringResource(R.string.universal_versions_quality_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.heightIn(max = 420.dp),
                ) {
                    recordings.forEachIndexed { index, recording ->
                        item(
                            key =
                                "version-title-" +
                                    index.toString(),
                        ) {
                            Column(modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) {
                                Text(
                                    text =
                                        stringResource(
                                            if (index == 0) {
                                                R.string.universal_versions_current
                                            } else {
                                                R.string.universal_versions_related
                                            },
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = recording.identity.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val versionDescription =
                                    recording.identity.version.label
                                        ?: recording.identity.version.traits
                                            .joinToString(" • ") { it.name.replace('_', ' ') }
                                            .takeIf(String::isNotBlank)
                                Text(
                                    text =
                                        versionDescription
                                            ?: stringResource(R.string.universal_versions_original),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        items(
                            count = recording.variants.size,
                            key = { offset ->
                                "variant-" +
                                    index.toString() +
                                    ":" +
                                    recording.variants[offset].provider.value +
                                    ":" +
                                    recording.variants[offset].sourceId
                            },
                        ) { offset ->
                            val variant = recording.variants[offset]
                            val chosen =
                                manualVariant?.let { key ->
                                    key.provider == variant.provider &&
                                        key.sourceId == variant.sourceId
                                } == true
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color =
                                    if (chosen) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectVariant(recording, variant) },
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = variantProviderName(variant.provider),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text =
                                            variantQualityDescription(variant)
                                                ?: stringResource(
                                                    R.string.universal_versions_unknown_quality,
                                                ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (variant.isCatalogOnly()) {
                                        Text(
                                            text =
                                                stringResource(
                                                    R.string.universal_versions_catalog,
                                                ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.universal_versions_close))
            }
        },
        dismissButton = {
            if (manualVariant != null) {
                TextButton(onClick = onClearManual) {
                    Text(stringResource(R.string.universal_versions_reset))
                }
            }
        },
    )
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
        ProviderId.SOUNDCLOUD -> "SoundCloud"
        else -> provider.value
    }

internal fun variantQualityDescription(variant: SourceVariant): String? {
    val quality = variant.quality ?: return null
    val parts = mutableListOf<String>()
    if (quality.lossless == true) parts += "Lossless"
    quality.codec?.takeIf(String::isNotBlank)?.let(parts::add)
    quality.bitDepth?.takeIf { it > 0 }?.let { parts += "$it-bit" }
    quality.sampleRateHz?.takeIf { it > 0 }?.let {
        parts += String.format(Locale.US, "%.1f kHz", it / 1000.0)
    }
    quality.bitrateBitsPerSecond?.takeIf { it > 0 }?.let {
        parts += (it / 1_000).toString() + " kbps"
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}
