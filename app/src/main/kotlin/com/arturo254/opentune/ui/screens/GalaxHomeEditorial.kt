package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.models.MediaMetadata

@Composable
fun GalaxHomeEditorialTopBar(
    accountName: String?,
    accountImage: String?,
    onProfile: () -> Unit,
    onRecognition: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(start = 23.dp, end = 23.dp, top = 24.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = "GALAXOT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.1.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Inicio",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
            )
        }
        GalaxEditorialIcon(
            icon = R.drawable.search,
            contentDescription = "Buscar",
            onClick = onSearch,
        )
        Spacer(Modifier.width(9.dp))
        GalaxEditorialIcon(
            icon = R.drawable.graphic_eq,
            contentDescription = "Reconocer música",
            onClick = onRecognition,
        )
        Spacer(Modifier.width(9.dp))
        GalaxEditorialSurface(
            modifier = Modifier.size(46.dp),
            radius = 17.dp,
            onClick = onProfile,
        ) {
            if (!accountImage.isNullOrBlank()) {
                AsyncImage(
                    model = accountImage,
                    contentDescription = "Cuenta",
                    modifier = Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(17.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = "Cuenta",
                    modifier = Modifier.align(Alignment.Center).size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalaxHomeImmersiveFeature(
    song: Song,
    active: Boolean,
    isPlaying: Boolean,
    animationsActive: Boolean,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
    onLongPress: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val deep = Color(0xFF070E1A)
    val shape = RoundedCornerShape(34.dp)
    val ink = Color.White
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 2.dp)
            .height(386.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        lerp(Color(0xFF0D1C33), scheme.primary, .17f),
                        Color(0xFF101D36),
                        deep,
                    ),
                )
            )
            .combinedClickable(
                onClick = onPlay,
                onLongClick = onLongPress,
            ),
    ) {
        val compact = maxWidth < 340.dp
        val artwork = if (compact) 188.dp else 232.dp
        GalaxEditorialBackdrop(
            modifier = Modifier.matchParentSize(),
            active = animationsActive,
            intensity = .80f,
            density = 32,
            accent = Color(0xFF8CBDFF),
        )
        GalaxEditorialOrb(
            modifier = Modifier.matchParentSize(),
            active = animationsActive,
            accent = Color(0xFF8DB8FF),
            secondary = Color(0xFFC2A3FF),
            rings = 4,
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 26.dp, end = 18.dp)
                .size(artwork)
                .clip(RoundedCornerShape(27.dp))
                .background(Color(0xFF14223C)),
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = .15f)),
                        ),
                    ),
            )
        }
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .45f to deep.copy(alpha = .10f),
                    .69f to deep.copy(alpha = .72f),
                    1f to deep.copy(alpha = .97f),
                ),
            ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(25.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(5.dp).clip(CircleShape)
                        .background(Color(0xFF9FC8FF)),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "EN DESTAQUE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = ink.copy(alpha = .77f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.9).sp,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 30.sp,
            )
            Text(
                text = song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = .76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GalaxEditorialSurface(
                    modifier = Modifier.size(49.dp),
                    radius = 17.dp,
                    accent = Color(0xFFB2CFFF),
                    onClick = onPlay,
                ) {
                    Icon(
                        painter = painterResource(
                            if (active && isPlaying) R.drawable.pause else R.drawable.play,
                        ),
                        contentDescription = if (isPlaying && active) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(27.dp),
                    )
                }
                Spacer(Modifier.width(13.dp))
                Text(
                    text = if (active && isPlaying) "Reproduciendo" else "Escuchar",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink.copy(alpha = .92f),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalaxHomePoster(
    song: Song,
    isActive: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(160.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GalaxEditorialArtworkFrame(
            modifier = Modifier.size(160.dp),
            accent = MaterialTheme.colorScheme.primary,
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            if (isActive) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(9.dp).size(35.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = .64f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalaxHomeCompactTrack(
    song: Song,
    active: Boolean,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) scheme.primary.copy(alpha = .10f)
                else Color.Transparent,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 7.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(61.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(scheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) scheme.primary else scheme.onSurface,
            )
            Text(
                text = song.artists.joinToString { it.name },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
        GalaxEditorialSurface(
            modifier = Modifier.size(39.dp),
            radius = 14.dp,
            accent = scheme.primary,
            onClick = onClick,
        ) {
            Icon(
                painter = painterResource(
                    if (active && isPlaying) R.drawable.pause else R.drawable.play,
                ),
                contentDescription = if (active && isPlaying) "Pausar" else "Escuchar",
                tint = scheme.onSurface,
                modifier = Modifier.align(Alignment.Center).size(19.dp),
            )
        }
    }
}

@Composable
fun GalaxHomeCurrentStrip(
    current: MediaMetadata?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (current == null) return
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).height(82.dp),
        radius = 22.dp,
        elevated = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            AsyncImage(
                model = current.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "EN REPRODUCCIÓN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(5.dp))
        }
    }
}

@Composable
fun GalaxHomeNavigationRow(
    onShuffle: () -> Unit,
    onLibrary: () -> Unit,
    shuffling: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GalaxEditorialAction(
            title = "Aleatorio",
            icon = R.drawable.shuffle,
            processing = shuffling,
            modifier = Modifier.weight(1f),
            onClick = onShuffle,
        )
        GalaxEditorialAction(
            title = "Biblioteca",
            icon = R.drawable.library_outlined,
            modifier = Modifier.weight(1f),
            onClick = onLibrary,
        )
    }
}

@Composable
fun GalaxHomeDiscoveryBridge(
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 20.dp).height(126.dp),
        radius = 26.dp,
        accent = MaterialTheme.colorScheme.tertiary,
        onClick = onOpenSearch,
    ) {
        GalaxEditorialOrb(
            modifier = Modifier.matchParentSize(),
            active = false,
            rings = 2,
        )
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "Explorar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Encuentra tu próxima canción",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 24.dp).size(23.dp),
        )
    }
}
