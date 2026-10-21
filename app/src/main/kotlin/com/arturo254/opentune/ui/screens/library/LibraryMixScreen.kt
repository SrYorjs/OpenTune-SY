package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.arturo254.opentune.ui.screens.NovaArtwork
import com.arturo254.opentune.ui.screens.NovaIcon
import com.arturo254.opentune.viewmodels.LibraryMixViewModel

/** The legacy entry now opens the same new collection, with its caller's filters. */
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    onTabSelected: (LibraryFilter) -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
    spotifyLibraryViewModel: SpotifyLibraryViewModel = hiltViewModel(),
) {
    LibraryScreen(navController = navController, viewModel = viewModel,
        spotifyViewModel = spotifyLibraryViewModel, filterContent = filterContent,
        onCategorySelected = onTabSelected)
}

internal fun collectionLabel(section: LibraryFilter): String = when (section) {
    LibraryFilter.LIBRARY -> "Todo"
    LibraryFilter.SONGS -> "Canciones"
    LibraryFilter.ALBUMS -> "Álbumes"
    LibraryFilter.ARTISTS -> "Artistas"
    LibraryFilter.PLAYLISTS -> "Listas"
    LibraryFilter.SPOTIFY -> "Spotify"
}

/** Collection typography and navigation are intentionally independent of Home. */
@Composable
internal fun CollectionMasthead(songCount: Int, collectionCount: Int, onCreate: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BIBLIOTECA", style = MaterialTheme.typography.labelMedium, letterSpacing = 2.5.sp,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            NovaIcon(R.drawable.add, "Crear playlist", onCreate)
            NovaIcon(R.drawable.settings, "Ajustes", onSettings)
        }
        Text("Tu colección.", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium, letterSpacing = (-1.5).sp)
        Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(songCount.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light)
                Text("canciones", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.weight(1f)) {
                Text(collectionCount.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light)
                Text("álbumes y listas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val ink = MaterialTheme.colorScheme.primary
            Canvas(Modifier.size(66.dp)) {
                // An engraved collection seal, not another hero illustration.
                repeat(4) { i ->
                    drawCircle(ink.copy(alpha = .12f + i * .05f), size.minDimension * (.18f + i * .09f), style = Stroke(1.dp.toPx()))
                }
                drawLine(ink.copy(alpha = .5f), Offset(size.width * .15f, size.height * .85f),
                    Offset(size.width * .85f, size.height * .15f), 1.dp.toPx())
            }
        }
    }
}

@Composable
internal fun CollectionTabs(selected: LibraryFilter, spotify: Boolean, counts: Map<LibraryFilter, Int>, onSelect: (LibraryFilter) -> Unit) {
    val tabs = listOf(LibraryFilter.LIBRARY, LibraryFilter.SONGS, LibraryFilter.PLAYLISTS,
        LibraryFilter.ALBUMS, LibraryFilter.ARTISTS) + if (spotify) listOf(LibraryFilter.SPOTIFY) else emptyList()
    LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        items(tabs, key = { it.name }) { tab ->
            Column(Modifier.clip(RoundedCornerShape(8.dp)).selectable(selected = tab == selected, role = Role.Tab, onClick = { onSelect(tab) })
                .padding(top = 14.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(collectionLabel(tab), fontWeight = if (tab == selected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (tab == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    counts[tab]?.let { Text(it.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Box(Modifier.width(36.dp).height(2.dp).background(if (tab == selected) MaterialTheme.colorScheme.primary else Color.Transparent))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CollectionTile(title: String, subtitle: String, image: String?, onOpen: () -> Unit,
    onMenu: () -> Unit, modifier: Modifier = Modifier, circular: Boolean = false) {
    Column(modifier.combinedClickable(role = Role.Button, onClick = onOpen, onLongClick = onMenu),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            NovaArtwork(image, Modifier.fillMaxWidth().aspectRatio(1f), circular)
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = .92f), shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)) {
                NovaIcon(R.drawable.more_vert, "Opciones de $title", onMenu)
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CollectionShortcut(title: String, icon: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge)
        }
    }
}
