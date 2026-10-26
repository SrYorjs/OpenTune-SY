package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.LibraryFilter
import com.arturo254.opentune.spotify.SpotifyLibraryViewModel
import com.arturo254.opentune.ui.screens.*
import com.arturo254.opentune.viewmodels.LibraryMixViewModel

@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    onTabSelected: (LibraryFilter) -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    spotifyLibraryViewModel: SpotifyLibraryViewModel = hiltViewModel(),
) {
    LibraryScreen(
        navController = navController,
        viewModel = viewModel,
        spotifyViewModel = spotifyLibraryViewModel,
        filterContent = filterContent,
        onCategorySelected = onTabSelected,
    )
}

internal fun collectionLabel(section: LibraryFilter): String = when (section) {
    LibraryFilter.LIBRARY -> "Todo"
    LibraryFilter.SONGS -> "Canciones"
    LibraryFilter.ALBUMS -> "Álbumes"
    LibraryFilter.ARTISTS -> "Artistas"
    LibraryFilter.PLAYLISTS -> "Listas"
    LibraryFilter.SPOTIFY -> "Spotify"
}

@Composable
internal fun VaultHeader(
    songCount: Int,
    collectionCount: Int,
    artworks: List<String>,
    onCreate: () -> Unit,
    onSettings: () -> Unit,
) {
    val palette = asterPalette()
    Column(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsterEyebrow("BIBLIOTECA  /  TU ESPACIO", Modifier.weight(1f))
            AsterIcon(R.drawable.settings, "Ajustes", onSettings)
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Muy tuyo.",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                )
                Text(
                    "$songCount canciones\n$collectionCount álbumes y listas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted,
                    lineHeight = 22.sp,
                )
            }
            Box(Modifier.width(112.dp).height(116.dp), contentAlignment = Alignment.Center) {
                repeat(3) { index ->
                    AsterArtwork(
                        artworks.getOrNull(2 - index),
                        Modifier.size(76.dp).graphicsLayer {
                            rotationZ = -16f + index * 14f
                            translationX = (index - 1) * 10.dp.toPx()
                            translationY = (1 - index) * 5.dp.toPx()
                        },
                    )
                }
            }
        }
        Surface(
            onClick = onCreate,
            shape = RoundedCornerShape(14.dp),
            color = palette.violet.copy(alpha = .12f),
            border = BorderStroke(1.dp, palette.violet.copy(alpha = .24f)),
        ) {
            Row(
                Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(painterResource(R.drawable.add), null, Modifier.size(20.dp), tint = palette.violet)
                Text("Crear una lista", style = MaterialTheme.typography.labelLarge, color = palette.violet)
            }
        }
    }
}

@Composable
internal fun VaultCategories(
    selected: LibraryFilter,
    spotify: Boolean,
    counts: Map<LibraryFilter, Int>,
    onSelect: (LibraryFilter) -> Unit,
) {
    val palette = asterPalette()
    val tabs = listOf(
        LibraryFilter.LIBRARY,
        LibraryFilter.PLAYLISTS,
        LibraryFilter.SONGS,
        LibraryFilter.ALBUMS,
        LibraryFilter.ARTISTS,
    ) + if (spotify) listOf(LibraryFilter.SPOTIFY) else emptyList()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs, key = { it.name }) { tab ->
            val active = tab == selected
            Column(
                Modifier.widthIn(min = 88.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (active) palette.violet.copy(alpha = .14f) else palette.panel.copy(alpha = .55f))
                    .border(1.dp, if (active) palette.violet.copy(alpha = .45f) else palette.line, RoundedCornerShape(17.dp))
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    counts[tab]?.toString() ?: "∞",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Light,
                    color = if (active) palette.violet else palette.muted,
                )
                Text(
                    collectionLabel(tab),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) palette.violet else palette.ink,
                )
            }
        }
    }
}

@Composable
internal fun VaultShortcut(
    title: String,
    subtitle: String,
    icon: Int,
    index: Int,
    onClick: () -> Unit,
) {
    val palette = asterPalette()
    val accent = if (index % 2 == 0) palette.rose else palette.ice
    Box(
        Modifier.width(184.dp).clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = .07f))
            .border(1.dp, accent.copy(alpha = .2f), RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) { drawAsterGlyph(index + 2, accent.copy(alpha = .45f)) }
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(painterResource(icon), null, Modifier.size(22.dp), tint = accent)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VaultFolder(
    title: String,
    subtitle: String,
    image: String?,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
    album: Boolean = false,
) {
    val palette = asterPalette()
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .combinedClickable(role = Role.Button, onClick = onOpen, onLongClick = onMenu),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (album) 1f else 1.18f)) {
            if (album) {
                AsterArtwork(image, Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxWidth(.54f).height(18.dp)
                        .background(palette.violet.copy(alpha = .25f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                )
                Box(
                    Modifier.fillMaxSize().padding(top = 9.dp)
                        .background(palette.panel, RoundedCornerShape(15.dp))
                        .border(1.dp, palette.line, RoundedCornerShape(15.dp)),
                ) {
                    AsterArtwork(image, Modifier.fillMaxSize().padding(14.dp).graphicsLayer { rotationZ = -5f })
                }
            }
            Surface(
                color = palette.base.copy(alpha = .94f),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
            ) {
                AsterIcon(R.drawable.more_vert, "Opciones de $title", onMenu)
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VaultArtist(
    title: String,
    count: Int,
    image: String?,
    onOpen: () -> Unit,
    onMenu: () -> Unit,
) {
    val palette = asterPalette()
    Column(
        Modifier.width(132.dp).clip(RoundedCornerShape(18.dp))
            .combinedClickable(role = Role.Button, onClick = onOpen, onLongClick = onMenu)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(110.dp).border(1.dp, palette.line, CircleShape).padding(6.dp)) {
            AsterArtwork(image, Modifier.fillMaxSize(), circular = true)
        }
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("$count canciones", style = MaterialTheme.typography.bodySmall, color = palette.muted)
    }
}

@Composable
internal fun VaultSearch(
    query: String,
    onQuery: (String) -> Unit,
) {
    val palette = asterPalette()
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Encuentra algo tuyo") },
        leadingIcon = { Icon(painterResource(R.drawable.search), null, tint = palette.muted) },
        trailingIcon = {
            if (query.isNotEmpty()) AsterIcon(R.drawable.close, "Borrar búsqueda", { onQuery("") })
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = palette.violet,
            unfocusedBorderColor = palette.line,
            focusedContainerColor = palette.panel.copy(alpha = .6f),
            unfocusedContainerColor = palette.panel.copy(alpha = .6f),
            cursorColor = palette.violet,
        ),
    )
}
