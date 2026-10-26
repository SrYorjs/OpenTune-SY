package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.UniversalAudioSelectionModeKey
import com.arturo254.opentune.core.model.ProviderId
import com.arturo254.opentune.core.model.SourceVariant
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.sources.offline.OfflineTrackState
import com.arturo254.opentune.sources.resolution.AudioSelectionMode
import com.arturo254.opentune.sources.resolution.SourceVariantKey
import com.arturo254.opentune.sources.search.UnifiedTrackSearchResult
import com.arturo254.opentune.ui.screens.*
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.viewmodels.UniversalSearchViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UniversalSearchScreen(query: String, pureBlack: Boolean, viewModel: UniversalSearchViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val player = LocalPlayerConnection.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val offlineAssets by viewModel.offlineAssets.collectAsStateWithLifecycle()
    val current = player?.mediaMetadata?.collectAsStateWithLifecycle()?.value
    val playing = player?.isPlaying?.collectAsStateWithLifecycle()?.value == true
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var resolving by remember { mutableStateOf<String?>(null) }
    var downloading by remember { mutableStateOf<String?>(null) }
    var dialog by remember(query) { mutableStateOf<String?>(null) }
    val manual = remember(query) { mutableStateMapOf<String, SourceVariantKey>() }
    var quality by rememberEnumPreference(UniversalAudioSelectionModeKey, AudioSelectionMode.AUTOMATIC)
    val sameQuery = state.query == query.trim()
    val entries = if (sameQuery) state.result.tracks else emptyList()
    val loading = state.isLoading || (!sameQuery && query.isNotBlank())
    val bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(query) {
        viewModel.query.value = query
        list.scrollToItem(0)
    }
    LaunchedEffect(list, query, entries.size, state.result.hasMore) {
        if (!sameQuery || !state.result.hasMore) return@LaunchedEffect
        snapshotFlow {
            val info = list.layoutInfo
            info.totalItemsCount > 0 && (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 4
        }.distinctUntilChanged().filter { it }.collect { viewModel.loadMore() }
    }
    val play: (UnifiedTrackSearchResult, List<SourceVariant>) -> Unit = { result, variants ->
        if (player != null && resolving == null && variants.isNotEmpty()) {
            scope.launch {
                resolving = result.signalKey()
                try {
                    if (!viewModel.playFirstAvailable(player, variants, context.getString(R.string.queue_searched_songs))) {
                        Toast.makeText(context, R.string.universal_versions_play_error, Toast.LENGTH_SHORT).show()
                    }
                } finally { resolving = null }
            }
        }
    }

    val phase = rememberAsterPhase(false)
    val palette = asterPalette()
    Box(Modifier.fillMaxSize().background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)) {
        AsterBackdrop(AsterScene.SEARCH, phase, Modifier.fillMaxSize(), pureBlack = pureBlack)
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 18.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("transmission/header") {
                TransmissionHeader(query, entries.size, loading)
            }
            item("transmission/quality") {
                TransmissionQuality(quality) { quality = it }
            }
            if (sameQuery && state.result.failedSourceCount > 0) {
                item("transmission/partial") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                            .background(palette.rose.copy(alpha = .1f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(painterResource(R.drawable.info), null, Modifier.size(20.dp), tint = palette.rose)
                        Text(
                            "Algunas fuentes no respondieron. Puedes escuchar los resultados disponibles.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink,
                        )
                    }
                }
            }
            if (loading && entries.isEmpty()) {
                item("transmission/loading") { AsterLoading("Buscando en todos los catálogos…") }
            }
            if (!loading && entries.isEmpty()) {
                item("transmission/empty") {
                    AsterMessage(
                        title = if (sameQuery && state.error != null) "La señal no llegó" else if (query.isBlank()) "Tu próxima canción empieza aquí" else "No encontramos esa frecuencia",
                        message = if (sameQuery && state.error != null) "Comprueba tu conexión y vuelve a enviar la búsqueda."
                            else "Prueba con un título, un artista o una parte del nombre.",
                    )
                }
            }
            itemsIndexed(
                entries,
                key = { index, result -> universalResultRowKey(index, result.signalKey()) },
                contentType = { _, _ -> "transmission/track" },
            ) { index, result ->
                val key = result.signalKey()
                val sources = result.variants.map { it.provider to it.sourceId }.toSet()
                val asset = offlineAssets.firstOrNull { candidate ->
                    candidate.discoverySources.any { source ->
                        (source.provider to source.sourceId) in sources
                    }
                }
                val offline = asset?.takeIf { it.state == OfflineTrackState.AVAILABLE }?.toSourceVariant()
                    ?: result.variants.firstOrNull { it.provider == ProviderId.OPEN_TUNE_OFFLINE }
                val preferred = viewModel.selectVariant(
                    result = result,
                    variants = result.variants + listOfNotNull(offline),
                    mode = quality,
                    manualVariant = manual[key],
                )
                val fallbacks = if (preferred == null) result.variants.filter {
                    it.provider in setOf(ProviderId.DEEZER, ProviderId.SPOTIFY, ProviderId.TIDAL, ProviderId.APPLE_MUSIC)
                } else emptyList()
                val active = (current != null && (result.variants + listOfNotNull(offline)).any { it.sourceId == current.id }) ||
                    (current != null && (preferred == null || preferred.isCatalogOnly()) &&
                        current.title.equals(result.identity.title, true) && result.identity.artists.any { artist ->
                            current.artists.any { it.name.equals(artist.name, true) }
                        })
                val pending = downloading == key || asset?.state == OfflineTrackState.PENDING
                val canDownload = offline == null && !pending && result.variants.any {
                    it.provider != ProviderId.LOCAL && it.provider != ProviderId.OPEN_TUNE_OFFLINE
                }
                TransmissionResult(
                    index = index,
                    result = result,
                    preferred = preferred,
                    active = active,
                    playing = playing,
                    resolving = resolving == key,
                    downloaded = offline != null,
                    downloading = pending,
                    modifier = asterItemMotion(),
                    onVersions = { dialog = key },
                    onPlay = if (player != null && (active || preferred != null || fallbacks.isNotEmpty())) {
                        { if (active) player.player.togglePlayPause() else play(result, listOfNotNull(preferred) + fallbacks) }
                    } else null,
                    onDownload = if (canDownload) { {
                        if (downloading == null) scope.launch {
                            downloading = key
                            try {
                                viewModel.enqueueOffline(result).onSuccess {
                                    Toast.makeText(context, R.string.catalog_offline_download_started, Toast.LENGTH_SHORT).show()
                                }.onFailure {
                                    Toast.makeText(context, it.message ?: context.getString(R.string.catalog_source_unplayable), Toast.LENGTH_SHORT).show()
                                }
                            } finally { downloading = null }
                        }
                    } } else null,
                )
            }

            if (sameQuery && state.isLoadingMore) {
                item("transmission/loading-more") { AsterLoading("Encontrando más música…") }
            } else if (sameQuery && state.result.hasMore && entries.isNotEmpty()) {
                item("transmission/more") {
                    OutlinedButton(
                        onClick = viewModel::loadMore,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    ) { Text("Seguir explorando resultados") }
                }
            }
        }
    }
    val selected = entries.firstOrNull { it.signalKey() == dialog }
    if (selected != null) {
        UniversalVersionQualityDialog(
            selected = selected,
            relatedVersions = viewModel.relatedVersions(selected),
            qualityMode = quality,
            manualVariant = manual[selected.signalKey()],
            onQualityModeChange = { quality = it },
            onSelectVariant = { result, variant ->
                manual[result.signalKey()] = SourceVariantKey(variant.provider, variant.sourceId)
                dialog = null
                play(result, listOf(variant))
            },
            onClearManual = { manual.remove(selected.signalKey()); dialog = null },
            onDismiss = { dialog = null },
        )
    }
}


@Composable
private fun TransmissionHeader(query: String, count: Int, loading: Boolean) {
    val palette = asterPalette()
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(7.dp).background(palette.ice, CircleShape))
            AsterEyebrow("BÚSQUEDA UNIVERSAL", Modifier.weight(1f))
            Text(if (loading) "…" else count.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light, color = palette.violet)
        }
        Text(
            query.ifBlank { "Sigue el sonido." },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1).sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                if (loading) "Explorando catálogos" else "$count canciones encontradas",
                style = MaterialTheme.typography.bodySmall,
                color = palette.muted,
            )
            HorizontalDivider(Modifier.weight(1f), color = palette.line)
        }
    }
}

@Composable
private fun TransmissionQuality(
    selected: AudioSelectionMode,
    onSelect: (AudioSelectionMode) -> Unit,
) {
    val palette = asterPalette()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AudioSelectionMode.entries, key = { it.name }) { mode ->
                AsterChoice(
                    text = when (mode) {
                        AudioSelectionMode.AUTOMATIC -> "Automática"
                        AudioSelectionMode.BEST_QUALITY -> "Mejor calidad"
                        AudioSelectionMode.DATA_SAVER -> "Ahorrar datos"
                    },
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                )
            }
        }
        Text(
            text = when (selected) {
                AudioSelectionMode.AUTOMATIC -> "Elegimos una versión disponible para reproducir."
                AudioSelectionMode.BEST_QUALITY -> "Prioriza la mejor calidad disponible."
                AudioSelectionMode.DATA_SAVER -> "Prioriza versiones que consumen menos datos."
            },
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodySmall,
            color = palette.muted,
        )
    }
}

@Composable
private fun TransmissionResult(
    index: Int,
    result: UnifiedTrackSearchResult,
    preferred: SourceVariant?,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    downloaded: Boolean,
    downloading: Boolean,
    onPlay: (() -> Unit)?,
    onVersions: () -> Unit,
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val palette = asterPalette()
    val display = preferred ?: result.recommendedVariant
    val artwork = display?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val providers = (result.variants + listOfNotNull(preferred)).map { it.provider }.distinct()
    val quality = signalQuality(display)
    val reduced = OpenTuneDesignSystem.galaxMotion.reducedMotion
    var expanded by rememberSaveable(result.signalKey()) { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) palette.violet.copy(alpha = .1f) else palette.panel.copy(alpha = .65f))
            .border(1.dp, if (active) palette.violet.copy(alpha = .4f) else palette.line, RoundedCornerShape(22.dp)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = palette.muted,
            )
            Spacer(Modifier.width(12.dp))
            if (downloaded || downloading) {
                Text(
                    if (downloaded) "SIN CONEXIÓN" else "PREPARANDO DESCARGA",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.ice,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    if (active) "AHORA SUENA" else "CANCIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) palette.violet else palette.muted,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ocultar detalles" else "Detalles", style = MaterialTheme.typography.labelSmall, color = palette.violet)
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .clickable(enabled = onPlay != null && !resolving, role = Role.Button) { onPlay?.invoke() }
                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsterArtwork(artwork, Modifier.size(66.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    result.identity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (active) palette.violet else palette.ink,
                )
                Text(
                    result.identity.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (resolving) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = palette.violet)
            } else if (onPlay != null) {
                Box(Modifier.size(38.dp).background(palette.violet.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
                    AsterPlaybackMark(active && playing)
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = if (reduced) EnterTransition.None else fadeIn(),
            exit = if (reduced) ExitTransition.None else fadeOut(),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HorizontalDivider(color = palette.line)
                TransmissionDetail("Fuentes", providers.joinToString(" · ") { signalProvider(it) })
                if (quality != null) TransmissionDetail("Calidad", quality)
                result.identity.album?.title?.takeIf { it.isNotBlank() }?.let { album ->
                    TransmissionDetail("Álbum", album)
                }
                if (display?.isCatalogOnly() == true) {
                    Text("Se buscará una versión reproducible al escuchar.", style = MaterialTheme.typography.bodySmall, color = palette.muted)
                }
                if (onPlay == null) {
                    Text("No hay una versión reproducible disponible. Revisa las versiones.", style = MaterialTheme.typography.bodySmall, color = palette.rose)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth()
                .background(palette.base.copy(alpha = .3f))
                .padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${result.variants.size} ${if (result.variants.size == 1) "versión" else "versiones"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.ink,
                )
                Text(
                    quality ?: providers.take(2).joinToString(" · ") { signalProvider(it) },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onVersions) { Text("Versiones", color = palette.violet) }
            when {
                downloading -> CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp, color = palette.ice)
                downloaded -> Icon(painterResource(R.drawable.offline), "Disponible sin conexión", Modifier.padding(12.dp).size(20.dp), tint = palette.ice)
                onDownload != null -> AsterIcon(R.drawable.download, "Descargar ${result.identity.title}", onDownload)
            }
        }
    }
}

@Composable
private fun TransmissionDetail(label: String, value: String) {
    val palette = asterPalette()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = palette.violet)
        Text(value, style = MaterialTheme.typography.bodySmall, color = palette.muted)
    }
}

private fun UnifiedTrackSearchResult.signalKey(): String {
    val isrc = identity.isrc?.takeIf { it.isNotBlank() }
    if (isrc != null) return "isrc:${isrc.uppercase(Locale.ROOT)}"
    return listOf(
        identity.title.trim().lowercase(Locale.ROOT),
        identity.artists.joinToString(",") { artist ->
            artist.name.trim().lowercase(Locale.ROOT)
        },
        identity.durationMs ?: -1L,
        identity.album?.canonicalId ?: identity.album?.title.orEmpty(),
        identity.version.traits.map { it.name }.sorted().joinToString(","),
        identity.version.label.orEmpty().trim().lowercase(Locale.ROOT),
    ).joinToString("|")
}

private fun signalProvider(provider: ProviderId): String = when (provider) {
    ProviderId.YOUTUBE_MUSIC -> "YouTube Music"
    ProviderId.DEEZER -> "Deezer"
    ProviderId.SPOTIFY -> "Spotify"
    ProviderId.TIDAL -> "TIDAL"
    ProviderId.APPLE_MUSIC -> "Apple Music"
    ProviderId.SOUNDCLOUD -> "SoundCloud"
    ProviderId.LOCAL -> "Local"
    ProviderId.OPEN_TUNE_OFFLINE -> "Offline"
    else -> provider.value
}

private fun signalQuality(variant: SourceVariant?): String? {
    val quality = variant?.quality ?: return null
    return buildList {
        if (quality.lossless == true) add("Lossless")
        quality.bitDepth?.takeIf { it > 0 }?.let { add("$it-bit") }
        quality.sampleRateHz?.takeIf { it > 0 }?.let {
            add(if (it % 1000 == 0) "${it / 1000} kHz" else String.format(Locale.US, "%.1f kHz", it / 1000.0))
        }
        quality.bitrateBitsPerSecond?.takeIf { it > 0 }?.let { add("${it / 1000} kbps") }
    }.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

internal fun universalResultRowKey(index: Int, identity: String): String = "universal-result:$index:$identity"
