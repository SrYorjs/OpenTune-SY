package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.arturo254.opentune.core.designsystem.component.GalaxOrbitField
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.GalaxEyebrow
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import com.arturo254.opentune.core.designsystem.theme.galaxNebulaSurface
import com.arturo254.opentune.db.entities.Song

@Composable
fun GalaxHomeIdentity(
    title: String,
    subtitle: String,
    accountName: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GalaxEyebrow("GALAXOT  /  TU UNIVERSO")
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.2).sp,
                    lineHeight = 39.sp,
                ),
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
            if (!accountName.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(6.dp).background(scheme.primary, CircleShape),
                    )
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.primary,
                        maxLines = 1,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            scheme.primary.copy(alpha = .38f),
                            scheme.tertiary.copy(alpha = .24f),
                        ),
                    ),
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(25.dp),
                )
            }
        }
    }
}

@Composable
fun GalaxHomeSectionIntro(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 21.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GalaxEyebrow("GALAXOT  /  EN TU ÓRBITA")
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.65).sp,
            ),
            color = colors.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalaxHomeMixBoard(
    songs: List<Song>,
    activeId: String?,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
    onLongPress: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tracks = remember(songs) { songs.distinctBy { it.id }.take(4) }
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surfaceContainerLow.copy(alpha = .96f),
                        GalaxVisualKit.abyss.copy(alpha = .98f),
                    ),
                ),
            ),
    ) {
        GalaxOrbitField(
            modifier = Modifier.fillMaxSize(),
            active = isPlaying,
            intensity = .86f,
        )
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "GALAXOT  /  PERSONAL ORBIT",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                )
                Text(
                    text = "●   ${tracks.size}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            tracks.chunked(2).forEach { group ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    group.forEach { song ->
                        GalaxHomeMixTile(
                            song = song,
                            accentIndex = tracks.indexOf(song),
                            active = song.id == activeId,
                            isPlaying = isPlaying,
                            onPlay = { onPlay(song) },
                            onLongPress = { onLongPress(song) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (group.size < 2) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "✦   UN UNIVERSO HECHO CON TUS CANCIONES",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = .65.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalaxHomeMixTile(
    song: Song,
    accentIndex: Int,
    active: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = OpenTuneDesignSystem.galaxMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.pressSpec,
        label = "homeMixTilePress",
    )
    val accent = if (accentIndex % 2 == 0) scheme.primary else scheme.tertiary
    Box(
        modifier = modifier
            .height(184.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .galaxNebulaSurface(accent = accent, intensity = if (active) 1f else .68f)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onPlay,
                onLongClick = onLongPress,
            ),
    ) {
        GalaxOrbitField(
            modifier = Modifier.fillMaxSize(),
            active = active && isPlaying,
            intensity = if (active) .42f else .22f,
            accent = accent,
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier.size(72.dp)
                        .clip(GalaxVisualKit.imageShape)
                        .background(accent.copy(alpha = .21f)),
                ) {
                    AsyncImage(
                        model = song.song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    modifier = Modifier.size(27.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) accent.copy(alpha = .3f)
                            else Color.White.copy(alpha = .075f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(
                            if (active && isPlaying) R.drawable.pause else R.drawable.play,
                        ),
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = if (active) "●  AHORA SUENA" else "◌  EN TU ÓRBITA",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
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
