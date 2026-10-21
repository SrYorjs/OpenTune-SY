package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class AtlasGenre(
    val title: String,
    val color: Color,
    val onClick: () -> Unit,
)

@Composable
internal fun AtlasTitleBar(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(start = 24.dp, end = 21.dp, top = 25.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "GALAXOT  /  02",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 2.3.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Descubrir",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.3).sp,
            )
        }
        AetherGlassPlate(
            modifier = Modifier.size(47.dp),
            radius = 17.dp,
            emphasis = .79f,
            onClick = onSearch,
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = "Buscar",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center).size(22.dp),
            )
        }
    }
}

@Composable
internal fun AtlasLaunchStage(
    active: Boolean,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val midnight = AetherInk.midnight
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(354.dp)
            .clip(RoundedCornerShape(33.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF12152F),
                        Color(0xFF26305F),
                        AetherInk.midnight,
                    ),
                )
            )
    ) {
        AetherMotionClock(active = active, durationMillis = 62000) { phase ->
            AtlasMesh(
                modifier = Modifier.matchParentSize(),
                phase = phase,
            )
        }
        AetherConstellation(
            modifier = Modifier.align(Alignment.CenterEnd)
                .size(if (maxWidth < 340.dp) 230.dp else 280.dp),
            seed = 46,
            active = active,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        midnight.copy(alpha = .03f),
                        midnight.copy(alpha = .74f),
                        midnight.copy(alpha = .96f),
                    ),
                )
            )
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(23.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(6.dp).clip(CircleShape)
                        .background(AetherInk.cyan),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = "EL UNIVERSO MUSICAL",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.8.sp,
                    color = AetherInk.silver.copy(alpha = .8f),
                )
            }
            Text(
                text = "Todo empieza\ncon una canción.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                lineHeight = 37.sp,
                letterSpacing = (-1.2).sp,
                color = Color.White,
            )
            Spacer(Modifier.height(7.dp))
            AtlasSearchField(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
internal fun AtlasMesh(
    modifier: Modifier = Modifier,
    phase: Float,
) {
    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val center = Offset(width * .75f, height * .31f)
        val radius = width * .47f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AetherInk.lilac.copy(alpha = .15f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        for (ring in 0..7) {
            val radiusFactor = .31f + ring * .14f
            drawCircle(
                color = AetherInk.silver.copy(alpha = .06f),
                radius = radius * radiusFactor,
                center = center,
                style = Stroke(width = .75.dp.toPx()),
            )
        }
        for (beam in 0..7) {
            val angle = (beam / 8.0 * PI * 2).toFloat() + phase * (PI * 2).toFloat()
            val vector = Offset(
                center.x + cos(angle) * radius * 1.4f,
                center.y + sin(angle) * radius * 1.4f,
            )
            drawLine(
                color = AetherInk.lilac.copy(alpha = .095f),
                start = center,
                end = vector,
                strokeWidth = .7.dp.toPx(),
            )
        }
        val spark = Offset(
            center.x + cos(phase * PI * 2).toFloat() * radius * .88f,
            center.y + sin(phase * PI * 2).toFloat() * radius * .88f,
        )
        drawCircle(AetherInk.cyan.copy(alpha = .84f), 2.1.dp.toPx(), spark)
        drawCircle(AetherInk.cyan.copy(alpha = .15f), 10.dp.toPx(), spark)
    }
}

@Composable
internal fun AtlasSearchField(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(21.dp)
    AetherGlassPlate(
        modifier = modifier.height(61.dp),
        radius = 21.dp,
        emphasis = .93f,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "Artistas, álbumes, canciones…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun AtlasGateway(
    title: String,
    subtitle: String,
    icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    AetherGlassPlate(
        modifier = modifier.height(108.dp),
        radius = 26.dp,
        emphasis = .65f,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun AtlasGenreTile(
    title: String,
    tint: Color,
    large: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val height = if (large) 177.dp else 125.dp
    val shape = RoundedCornerShape(27.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(height)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        tint.copy(alpha = .87f),
                        Color(0xFF1A2443),
                        Color(0xFF0A1328),
                    ),
                ),
            )
            .clickable(onClick = onClick),
    ) {
        AtlasGenrePattern(
            modifier = Modifier.fillMaxSize(),
            tint = tint,
            large = large,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = .07f),
                        Color.Black.copy(alpha = .64f),
                    ),
                )
            )
        )
        Text(
            text = title,
            modifier = Modifier.align(Alignment.BottomStart).padding(17.dp),
            style = if (large) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.3).sp,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = Color.White.copy(alpha = .81f),
            modifier = Modifier.align(Alignment.TopEnd).padding(15.dp).size(18.dp),
        )
    }
}

@Composable
internal fun AtlasGenrePattern(
    modifier: Modifier,
    tint: Color,
    large: Boolean,
) {
    Canvas(modifier) {
        val center = Offset(size.width * .83f, size.height * .19f)
        val radius = size.width * .48f
        for (i in 0..6) {
            val progress = i / 6f
            drawCircle(
                color = Color.White.copy(alpha = .12f - i * .012f),
                radius = radius * (.34f + progress * 1.2f),
                center = center,
                style = Stroke(width = .8.dp.toPx()),
            )
        }
        for (n in 0..12) {
            val t = n / 12f
            val x = size.width * t
            val top = size.height * (.2f + .19f * sin(t * 13.4f).toFloat())
            drawLine(
                color = Color.White.copy(alpha = .10f),
                start = Offset(x, top),
                end = Offset(x, top + size.height * (if (large) .29f else .16f)),
                strokeWidth = (if (n % 3 == 0) 1.6f else .7f).dp.toPx(),
            )
        }
    }
}

@Composable
internal fun AtlasSongEntry(
    title: String,
    artist: String,
    artwork: String?,
    active: Boolean,
    playing: Boolean,
    index: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 19.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = .07f)
                else Color.Transparent
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = (index + 1).toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        AsyncImage(
            model = artwork,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(15.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
internal fun AtlasFeaturedSong(
    title: String,
    artist: String,
    artwork: String?,
    active: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AetherGlassPlate(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp).height(124.dp),
        radius = 27.dp,
        emphasis = .77f,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.size(98.dp).clip(RoundedCornerShape(21.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
internal fun AtlasAllGenres(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AetherGlassPlate(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp).height(65.dp),
        radius = 20.dp,
        emphasis = .51f,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(13.dp))
            Text(
                text = "Todos los géneros",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun AtlasEmptyState(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AetherGlassPlate(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp).height(170.dp),
        radius = 28.dp,
        emphasis = .40f,
        onClick = onSearch,
    ) {
        AetherConstellation(
            modifier = Modifier.align(Alignment.CenterEnd).size(168.dp),
            seed = 6,
            active = false,
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Hay más por descubrir",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Buscar música",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
