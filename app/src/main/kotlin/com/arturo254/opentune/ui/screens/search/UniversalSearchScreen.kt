package com.arturo254.opentune.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
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

/** Signal: one universal result stream, explicit versions, quality and offline state. */
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
    val manual = remember { mutableStateMapOf<String, SourceVariantKey>() }
    var quality by rememberEnumPreference(UniversalAudioSelectionModeKey, AudioSelectionMode.AUTOMATIC)
    // During the debounce interval do not relabel the previous query's results.
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
    Box(Modifier.fillMaxSize().background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)) {
        NovaBackdrop(NovaSpace.RESULTS, active = false, modifier = Modifier.fillMaxSize(), pureBlack = pureBlack)
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item("signal/header") { SignalHeader(query, entries.size, loading) }
            item("signal/quality") { SignalQuality(quality) { quality = it } }
            if (sameQuery && state.result.failedSourceCount > 0) item("signal/partial") {
                Surface(Modifier.padding(horizontal = 20.dp), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Algunas fuentes no respondieron. Estos resultados siguen disponibles.",
                        Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            if (loading && entries.isEmpty()) item("signal/loading") { NovaLoading("Buscando en todos los catálogos…") }
            if (!loading && entries.isEmpty()) item("signal/empty") {
                NovaMessage(
                    title = if (state.error != null) "No pudimos completar la búsqueda" else if (query.isBlank()) "Encuentra tu próxima canción" else "No hay coincidencias",
                    message = if (state.error != null) "Comprueba tu conexión y vuelve a enviar la búsqueda." else "Prueba con el título, un artista o una parte del nombre.",
                )
            }
            itemsIndexed(entries, key = { index, result -> universalResultRowKey(index, result.signalKey()) },
                contentType = { _, _ -> "signal/recording" }) { index, result ->
                val key = result.signalKey()
                val sources = result.variants.map { it.provider to it.sourceId }.toSet()
                val asset = offlineAssets.firstOrNull { asset -> asset.discoverySources.any { (it.provider to it.sourceId) in sources } }
                val offline = asset?.takeIf { it.state == OfflineTrackState.AVAILABLE }?.toSourceVariant()
                    ?: result.variants.firstOrNull { it.provider == ProviderId.OPEN_TUNE_OFFLINE }
                val preferred = viewModel.selectVariant(result, result.variants + listOfNotNull(offline), quality, manual[key])
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
                SignalResult(
                    index = index, result = result, preferred = preferred, active = active, playing = playing,
                    resolving = resolving == key, downloaded = offline != null, downloading = pending,
                    modifier = Modifier.animateItem(),
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
            if (sameQuery && state.isLoadingMore) item("signal/more-loading") { NovaLoading("Cargando más canciones…") }
            else if (sameQuery && state.result.hasMore && entries.isNotEmpty()) item("signal/more") {
                TextButton(onClick = viewModel::loadMore, modifier = Modifier.padding(horizontal = 20.dp)) { Text("Cargar más resultados") }
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
private fun SignalHeader(query: String, count: Int, loading: Boolean) {
    val ink = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Canvas(Modifier.size(28.dp)) {
                repeat(5) { bar ->
                    val height = size.height * listOf(.3f, .6f, 1f, .65f, .35f)[bar]
                    val x = size.width * (bar + .5f) / 5
                    drawLine(ink, Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), 2.dp.toPx())
                }
            }
            Text("TODOS LOS CATÁLOGOS", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.8.sp, color = ink)
        }
        Text(query.ifBlank { "Tu música, aquí." }, style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Medium, letterSpacing = (-1).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(if (loading) "Explorando…" else "$count canciones encontradas", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SignalQuality(selected: AudioSelectionMode, onSelect: (AudioSelectionMode) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(AudioSelectionMode.entries, key = { it.name }) { mode ->
            FilterChip(selected = selected == mode, onClick = { onSelect(mode) }, label = { Text(when (mode) {
                AudioSelectionMode.AUTOMATIC -> "Automática"
                AudioSelectionMode.BEST_QUALITY -> "Mejor calidad"
                AudioSelectionMode.DATA_SAVER -> "Ahorrar datos"
            }) })
        }
    }
}

@Composable
private fun SignalResult(
    index: Int, result: UnifiedTrackSearchResult, preferred: SourceVariant?, active: Boolean, playing: Boolean,
    resolving: Boolean, downloaded: Boolean, downloading: Boolean, onPlay: (() -> Unit)?,
    onVersions: () -> Unit, onDownload: (() -> Unit)?, modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val display = preferred ?: result.recommendedVariant
    val artwork = display?.artworkUrl ?: result.variants.firstNotNullOfOrNull { it.artworkUrl }
    val providers = (result.variants + listOfNotNull(preferred)).map { it.provider }.distinct().joinToString(" · ") { signalProvider(it) }
    val quality = signalQuality(display)
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp))
        .background(if (active) colors.primary.copy(alpha = .1f) else colors.surfaceContainerLow.copy(alpha = .8f))) {
        Row(Modifier.fillMaxWidth().clickable(enabled = onPlay != null, role = Role.Button) { onPlay?.invoke() }
            .padding(14.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                NovaArtwork(artwork, Modifier.size(68.dp))
                if (resolving) CircularProgressIndicator(Modifier.align(Alignment.Center).size(25.dp), strokeWidth = 2.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(result.identity.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = if (active) colors.primary else colors.onSurface)
                Text(result.identity.artists.joinToString { it.name }, style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (quality != null) Text(quality, style = MaterialTheme.typography.labelSmall, color = colors.primary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!resolving && onPlay != null) Icon(painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
                contentDescription = if (active && playing) "Pausar" else "Reproducir", tint = colors.primary, modifier = Modifier.size(22.dp))
        }
        HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = colors.outlineVariant.copy(alpha = .35f))
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(providers, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (downloaded || downloading || display?.isCatalogOnly() == true) Text(
                    when { downloaded -> "Disponible sin conexión"; downloading -> "Preparando descarga…"; else -> "Se buscará una versión reproducible" },
                    style = MaterialTheme.typography.labelSmall, color = colors.primary)
            }
            TextButton(onClick = onVersions) { Text("Versiones") }
            if (downloading) CircularProgressIndicator(Modifier.padding(12.dp).size(20.dp), strokeWidth = 2.dp)
            else if (onDownload != null) NovaIcon(R.drawable.download, "Descargar ${result.identity.title}", onDownload)
        }
    }
}

/** Includes version traits and album so alternate recordings cannot share choices. */
private fun UnifiedTrackSearchResult.signalKey(): String = identity.isrc?.takeIf { it.isNotBlank() }?.let { "isrc:${it.uppercase(Locale.ROOT)}" }
    ?: listOf(identity.title.trim().lowercase(Locale.ROOT), identity.artists.joinToString(",") { it.name.trim().lowercase(Locale.ROOT) },
        identity.durationMs ?: -1L, identity.album?.canonicalId ?: identity.album?.title.orEmpty(),
        identity.version.traits.map { it.name }.sorted().joinToString(","), identity.version.label.orEmpty().trim().lowercase(Locale.ROOT)).joinToString("|")

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
