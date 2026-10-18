package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidSurface
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.models.ItemMetadata
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GalaxListeningDeck(
    quickPicks: List<Song>, mediaMetadata: MediaMetadata?, isPlaying: Boolean,
    navController: NavController, playerConnection: PlayerConnection,
    menuState: MenuState, haptic: HapticFeedback,
    metadataMap: Map<String, ItemMetadata> = emptyMap(),
) {
    val songs = remember(quickPicks) { quickPicks.distinctBy { it.id } }
    val motion = OpenTuneDesignSystem.galaxMotion
    val foreground = if (com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidVariant.current ==
        com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidVariant.DISABLED)
        MaterialTheme.colorScheme.onSurface else Color.White
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(songs, key = { it.id }) { song ->
            val backdrop = rememberLayerBackdrop()
            val interactions = remember { MutableInteractionSource() }
            val pressed by interactions.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) motion.pressScale else 1f, motion.pressSpec, label = "orbitCardPress")
            val active = song.id == mediaMetadata?.id
            Column(Modifier.width(176.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .combinedClickable(interactionSource = interactions, indication = null,
                    onClick = {
                        if (active) playerConnection.player.togglePlayPause()
                        else playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show { SongMenu(originalSong = song, navController = navController,
                            metadata = metadataMap[song.id], onDismiss = menuState::dismiss) }
                    }), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle.artworkShape)) {
                    AsyncImage(model = song.song.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().layerBackdrop(backdrop))
                    if (active) GalaxLiquidSurface(backdrop = backdrop, shapeOverride = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp)) {
                        Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            androidx.compose.ui.res.stringResource(if (isPlaying) R.string.player_a11y_pause else R.string.play),
                            Modifier.align(Alignment.Center).size(22.dp), tint = foreground)
                    }
                }
                Text(song.song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1,
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis)
                Text(song.artists.joinToString { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
