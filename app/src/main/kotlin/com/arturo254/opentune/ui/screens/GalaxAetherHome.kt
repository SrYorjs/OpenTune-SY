package com.arturo254.opentune.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.models.MediaMetadata
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun AetherHomeMasthead(
    accountImage: String?,
    onAccount: () -> Unit,
    onSearch: () -> Unit,
    onRecognize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "GALAXOT  /  01",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Inicio",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
                letterSpacing = (-1.3).sp,
            )
        }
        AetherMastheadButton(R.drawable.graphic_eq, "Reconocer canción", onRecognize)
        AetherMastheadButton(R.drawable.search, "Buscar", onSearch)
        AetherGlassPlate(
            modifier = Modifier.size(47.dp),
            radius = 17.dp,
            emphasis = .66f,
            onClick = onAccount,
        ) {
            if (accountImage.isNullOrBlank()) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = "Cuenta",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center).size(22.dp),
                )
            } else {
                AsyncImage(
                    model = accountImage,
                    contentDescription = "Cuenta",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(17.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@Composable
internal fun AetherMastheadButton(
    icon: Int,
    description: String,
    onClick: () -> Unit,
) {
    AetherGlassPlate(
        modifier = Modifier.size(47.dp),
        radius = 17.dp,
        emphasis = .48f,
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center).size(21.dp),
        )
    }
}

@Composable
internal fun AetherOpeningStage(
    song: Song,
    selected: Boolean,
    playing: Boolean,
    active: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deep = AetherInk.midnight
    val accent = MaterialTheme.colorScheme.primary
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 5.dp)
            .height(423.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        AetherInk.indigo,
                        lerp(AetherInk.midnight, accent, .22f),
                        deep,
                    ),
                ),
            ),
    ) {
        val compact = maxWidth < 365.dp
        AetherSoundRibbon(
            active = active && playing,
            modifier = Modifier.align(Alignment.Center)
                .fillMaxWidth()
                .height(175.dp),
            color = AetherInk.cyan,
        )
        AetherStageRecord(
            song = song,
            active = active && playing,
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 19.dp, end = if (compact) 4.dp else 14.dp)
                .size(if (compact) 252.dp else 282.dp),
        )
        Box(
            modifier = Modifier.matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            deep.copy(alpha = .04f),
                            deep.copy(alpha = .73f),
                            deep.copy(alpha = .97f),
                        ),
                    ),
        )
        AetherConstellation(
            modifier = Modifier.align(Alignment.TopStart).size(139.dp),
            seed = 3,
            active = active,
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(horizontal = 25.dp, vertical = 23.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                AetherIdentityDot(active = playing && selected, tint = AetherInk.cyan)
                Text(
                    text = "DESTACADO",
                    style = MaterialTheme.typography.labelSmall,
                    color = AetherInk.silver.copy(alpha = .81f),
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                lineHeight = 35.sp,
                letterSpacing = (-1.15).sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artists.joinToString { it.name },
                style = MaterialTheme.typography.bodyMedium,
                color = AetherInk.silver.copy(alpha = .78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AetherGlassPlate(
                    modifier = Modifier.size(55.dp),
                    radius = 19.dp,
                    emphasis = .91f,
                    onClick = onClick,
                ) {
                    Icon(
                        painter = painterResource(
                            if (playing && selected) R.drawable.pause else R.drawable.play
                        ),
                        contentDescription = if (playing && selected) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                    )
                }
                Text(
                    text = if (selected && playing) "En reproducción" else "Escuchar ahora",
                    color = AetherInk.silver,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                AetherGlassPlate(
                    modifier = Modifier.size(46.dp),
                    radius = 18.dp,
                    emphasis = .65f,
                    onClick = onMenu,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "Opciones",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.Center).size(23.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun AetherStageRecord(
    song: Song,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    AetherMotionClock(active = active, durationMillis = 75000) { phase ->
        Box(modifier) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension * .47f
                val center = Offset(size.width * .5f, size.height * .5f)
                drawCircle(AetherInk.midnight.copy(alpha = .86f), radius, center)
                for (ring in 0..11) {
                    drawCircle(
                        color = AetherInk.silver.copy(alpha = .065f + ring * .003f),
                        radius = radius * (.39f + ring * .05f),
                        center = center,
                        style = Stroke(width = .65.dp.toPx()),
                    )
                }
                drawArc(
                    color = AetherInk.cyan.copy(alpha = .33f),
                    startAngle = -78f + phase * 360f,
                    sweepAngle = 105f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center)
                    .fillMaxSize(.74f)
                    .clip(CircleShape)
                    .graphicsLayer { rotationZ = phase * 360f },
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier.align(Alignment.Center)
                    .size(25.dp).clip(CircleShape)
                    .background(AetherInk.midnight),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(7.dp).clip(CircleShape).background(AetherInk.silver.copy(alpha = .8f))
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AetherCoverTile(
    song: Song,
    active: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(165.dp)
            .combinedClickable(onClick = onPlay, onLongClick = onMenu),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier.size(165.dp).clip(RoundedCornerShape(25.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AsyncImage(
                model = song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth().height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, AetherInk.midnight.copy(alpha = .72f)),
                        ),
                    )
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = if (active && playing) "Sonando" else "Escuchar",
                    color = Color.White.copy(alpha = .89f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            text = song.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = song.artists.joinToString { it.name },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AetherMusicLine(
    song: Song,
    index: Int,
    active: Boolean,
    playing: Boolean,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = .08f)
                else Color.Transparent,
            )
            .combinedClickable(onClick = onPlay, onLongClick = onMenu)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp),
        )
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.size(57.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
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
        AetherIdentityDot(active = active, tint = MaterialTheme.colorScheme.primary)
        Icon(
            painter = painterResource(if (playing && active) R.drawable.pause else R.drawable.play),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun AetherListeningRail(
    metadata: MediaMetadata,
    playing: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AetherGlassPlate(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).height(86.dp),
        radius = 22.dp,
        emphasis = .84f,
        onClick = onTap,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = metadata.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "CONTINUAR ESCUCHANDO",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(if (playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun AetherDuetActions(
    busy: Boolean,
    onShuffle: () -> Unit,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        AetherAction(
            title = "Aleatorio",
            icon = R.drawable.shuffle,
            busy = busy,
            modifier = Modifier.weight(1f),
            onClick = onShuffle,
        )
        AetherAction(
            title = "Tu biblioteca",
            icon = R.drawable.library_outlined,
            modifier = Modifier.weight(1f),
            onClick = onLibrary,
        )
    }
}

@Composable
internal fun AetherCrossfadeInvitation(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AetherGlassPlate(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 5.dp)
            .height(170.dp),
        radius = 30.dp,
        emphasis = .58f,
        onClick = onSearch,
    ) {
        AetherConstellation(
            modifier = Modifier.align(Alignment.CenterEnd).size(168.dp),
            seed = 19,
            active = false,
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "MÁS ALLÁ DE TU LISTA",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.6.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Encuentra algo nuevo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.5).sp,
            )
            Text(
                text = "Explorar música",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(23.dp).size(21.dp),
        )
    }
}
