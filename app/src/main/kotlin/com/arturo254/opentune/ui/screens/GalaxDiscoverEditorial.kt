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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.innertube.models.SongItem

@Composable
fun GalaxDiscoveryHero(
    onSearch: () -> Unit,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(34.dp)
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = 19.dp, vertical = 14.dp)
            .height(306.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F1830),
                        lerp(Color(0xFF0A172C), MaterialTheme.colorScheme.primary, .23f),
                        Color(0xFF0B1021),
                    ),
                ),
            ),
    ) {
        GalaxEditorialBackdrop(
            modifier = Modifier.matchParentSize(),
            density = 40,
            active = active,
            intensity = .87f,
            accent = Color(0xFFBDCBFF),
            secondary = Color(0xFF9D8CE7),
        )
        GalaxEditorialOrb(
            modifier = Modifier.matchParentSize(),
            active = active,
            rings = 5,
            accent = Color(0xFF7DBAFF),
            secondary = Color(0xFFB89CFF),
        )
        Box(
            Modifier.align(Alignment.TopStart)
                .padding(start = 23.dp, top = 25.dp),
        ) {
            Text(
                "GALAXOT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = .75f),
                letterSpacing = 2.6.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(19.dp),
        ) {
            Text(
                text = "Descubrir",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.6).sp,
                color = Color.White,
            )
            Row(
                Modifier.fillMaxWidth()
                    .height(58.dp).clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = .13f))
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.White,
                )
                Text(
                    text = "¿Qué quieres escuchar?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .85f),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = .7f),
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
fun GalaxDiscoveryShortcut(
    title: String,
    icon: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GalaxEditorialSurface(
        modifier = modifier.height(87.dp),
        radius = 22.dp,
        accent = scheme.primary,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 17.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(43.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(scheme.primary.copy(alpha = .10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(23.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun GalaxDiscoveryCategory(
    title: String,
    swatch: Long,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = Color(swatch)
    val shape = RoundedCornerShape(25.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(148.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        lerp(tint, Color(0xFF0B1124), .24f),
                        lerp(tint, Color(0xFF0A1225), .66f),
                        Color(0xFF0B142B),
                    ),
                ),
            )
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val origin = Offset(size.width * .92f, size.height * .13f)
            repeat(4) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = .07f + index * .018f),
                    radius = size.width * (.16f + index * .15f),
                    center = origin,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            repeat(9) { index ->
                drawCircle(
                    color = Color.White.copy(alpha = .22f),
                    radius = if (index % 3 == 0) 1.25.dp.toPx() else .65.dp.toPx(),
                    center = Offset(
                        size.width * (((index * 43 + 11) % 97) / 97f),
                        size.height * (((index * 31 + 17) % 91) / 91f),
                    ),
                )
            }
        }
        Box(
            modifier = Modifier.align(Alignment.TopStart).padding(17.dp).size(31.dp)
                .clip(CircleShape).background(Color.White.copy(alpha = .13f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = title,
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-.36).sp,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GalaxDiscoveryNowPlaying(
    title: String,
    artist: String,
    artwork: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).height(82.dp),
        radius = 23.dp,
        elevated = true,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
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
                painter = painterResource(if (active) R.drawable.pause else R.drawable.play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(23.dp),
            )
            Spacer(Modifier.width(7.dp))
        }
    }
}

@Composable
fun GalaxDiscoverySong(
    title: String,
    artist: String,
    artwork: String?,
    active: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 19.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) scheme.primary.copy(alpha = .10f)
                else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(15.dp))
                .background(scheme.surfaceContainerHigh),
            contentScale = ContentScale.Crop,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (active) scheme.primary else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(if (active && playing) R.drawable.pause else R.drawable.play),
            contentDescription = null,
            tint = if (active) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun GalaxDiscoveryCategoryMore(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).height(59.dp),
        radius = 18.dp,
        onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Todos los géneros",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun GalaxDiscoveryFeaturedTile(
    title: String,
    subtitle: String,
    artwork: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GalaxEditorialSurface(
        modifier = modifier.fillMaxWidth().height(132.dp),
        radius = 25.dp,
        elevated = true,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                modifier = Modifier.size(100.dp).clip(RoundedCornerShape(18.dp)),
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
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                modifier = Modifier.size(21.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
