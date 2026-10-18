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

/** Current listening picks, with their own safe artwork source for the selected glass. */
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
            Box(Modifier.width(248.dp).height(292.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(32.dp))
                .combinedClickable(interactionSource = interactions, indication = null,
                    onClick = {
                        if (active) playerConnection.player.togglePlayPause()
                        else playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show { SongMenu(originalSong = song, navController = navController,
                            metadata = metadataMap[song.id], onDismiss = menuState::dismiss) }
                    })) {
                Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
                    AsyncImage(model = song.song.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize())
                    Box(Modifier.matchParentSize().background(Brush.verticalGradient(
                        0f to Color.Transparent, .45f to Color.Transparent, 1f to Color.Black.copy(alpha = .85f))))
                }
                GalaxLiquidSurface(backdrop = backdrop, shapeOverride = RoundedCornerShape(24.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(song.song.title, color = foreground, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, color = foreground.copy(alpha = .82f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(painterResource(if (active && isPlaying) R.drawable.pause else R.drawable.play), null, Modifier.size(24.dp), tint = foreground)
                    }
                }
            }
        }
    }
}
