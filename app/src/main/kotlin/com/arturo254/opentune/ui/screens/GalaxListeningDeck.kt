package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.liquid.GalaxLiquidSurface
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.models.ItemMetadata
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.models.toMediaMetadata
import com.arturo254.opentune.playback.PlayerConnection
import com.arturo254.opentune.playback.queues.YouTubeQueue
import com.arturo254.opentune.ui.component.MenuState
import com.arturo254.opentune.ui.menu.SongMenu
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GalaxListeningDeck(
    quickPicks: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    metadataMap: Map<String, ItemMetadata> = emptyMap(),
) {
    val tracks = remember(quickPicks) { quickPicks.distinctBy { it.id } }
    val motion = OpenTuneDesignSystem.galaxMotion
    val scheme = MaterialTheme.colorScheme
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        items(tracks, key = { it.id }) { song ->
            val active = song.id == mediaMetadata?.id
            val backdrop = rememberLayerBackdrop()
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) motion.pressScale else 1f,
                animationSpec = motion.pressSpec,
                label = "orbitCardPress",
            )
            Column(
                modifier = Modifier
                    .width(214.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {
                            if (active) playerConnection.player.togglePlayPause()
                            else playerConnection.playQueue(
                                YouTubeQueue.radio(song.toMediaMetadata()),
                            )
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    metadata = metadataMap[song.id],
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                        .clip(GalaxVisualKit.tileShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    scheme.primary.copy(alpha = .30f),
                                    scheme.tertiary.copy(alpha = .2f),
                                ),
                            ),
                        ),
                ) {
                    AsyncImage(
                        model = song.song.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().layerBackdrop(backdrop),
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = .32f),
                                .25f to Color.Transparent,
                                1f to Color.Black.copy(alpha = .48f),
                            ),
                        ),
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopStart)
                            .padding(12.dp)
                            .clip(GalaxVisualKit.pillShape)
                            .background(Color.Black.copy(alpha = .27f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(5.dp)
                                .background(if (active) scheme.primary else Color.White, CircleShape),
                        )
                        Text(
                            text = if (active) "NOW PLAYING" else "GALAXOT  /  ORBIT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                        )
                    }
                    if (active) {
                        GalaxLiquidSurface(
                            backdrop = backdrop,
                            shapeOverride = CircleShape,
                            modifier = Modifier.align(Alignment.BottomEnd)
                                .padding(12.dp).size(43.dp),
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isPlaying) R.drawable.pause else R.drawable.play,
                                ),
                                contentDescription = null,
                                modifier = Modifier.align(Alignment.Center).size(22.dp),
                                tint = Color.White,
                            )
                        }
                    }
                }
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) scheme.primary else scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
