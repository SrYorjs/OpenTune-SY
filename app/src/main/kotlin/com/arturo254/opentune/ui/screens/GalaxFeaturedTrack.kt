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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit
import com.arturo254.opentune.db.entities.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GalaxFeaturedTrack(song: Song, isPlaying: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.primary
    val artwork = song.song.thumbnailUrl
    val accent by produceState(fallback, artwork, context) {
        value = fallback
        if (!artwork.isNullOrBlank()) {
            value = withContext(Dispatchers.IO) {
                try {
                    val result = context.imageLoader.execute(
                        ImageRequest.Builder(context).data(artwork)
                            .size(96).allowHardware(false).build(),
                    )
                    val bitmap = result.image?.toBitmap()
                    if (bitmap == null) fallback else withContext(Dispatchers.Default) {
                        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                        palette.vibrantSwatch?.rgb?.let(::Color)
                            ?: palette.dominantSwatch?.rgb?.let(::Color)
                            ?: fallback
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    fallback
                }
            }
        }
    }
    val deep = lerp(accent, GalaxVisualKit.night, .88f)
    val middle = lerp(accent, GalaxVisualKit.abyss, .61f)
    val upper = lerp(accent, GalaxVisualKit.layer, .53f)
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = GalaxScreenStyle.pagePadding, vertical = 5.dp)
            .fillMaxWidth()
            .height(438.dp)
            .clip(GalaxScreenStyle.panelShape)
            .background(
                Brush.linearGradient(
                    listOf(upper, middle, deep, GalaxVisualKit.night),
                ),
            ),
    ) {
        val artSize = (maxWidth * .64f).coerceAtMost(255.dp)
        Canvas(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(artSize + 52.dp),
        ) {
            val orbit = size.minDimension * .47f
            drawCircle(
                color = Color.White.copy(alpha = .13f),
                radius = orbit,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = scheme.primary.copy(alpha = .19f),
                radius = orbit * .76f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = .56f),
                radius = 1.7.dp.toPx(),
                center = Offset(size.width * .12f, size.height * .28f),
            )
            drawCircle(
                color = scheme.tertiary.copy(alpha = .66f),
                radius = 1.6.dp.toPx(),
                center = Offset(size.width * .87f, size.height * .78f),
            )
        }
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .padding(top = 31.dp, end = 22.dp)
                .size(artSize)
                .clip(GalaxVisualKit.imageShape)
                .background(accent.copy(alpha = .4f)),
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().drawWithCache {
                val shadow = Brush.verticalGradient(
                    0f to Color.Transparent,
                    .31f to Color.Transparent,
                    .59f to deep.copy(alpha = .72f),
                    .73f to deep.copy(alpha = .97f),
                    1f to GalaxVisualKit.night,
                )
                val vignette = Brush.horizontalGradient(
                    0f to deep.copy(alpha = .24f),
                    .38f to Color.Transparent,
                    1f to Color.Transparent,
                )
                onDrawBehind {
                    drawRect(shadow)
                    drawRect(vignette)
                }
            },
        )
        Column(
            modifier = Modifier.fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 23.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    Modifier.size(6.dp).background(Color.White.copy(alpha = .95f), CircleShape),
                )
                Text(
                    text = stringResource(R.string.galax_discover_for_you).uppercase(),
                    color = Color.White.copy(alpha = .88f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "01",
                        color = scheme.primary.copy(alpha = .95f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = "—  GALAXOT  /  FEATURED",
                        color = Color.White.copy(alpha = .55f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.15).sp,
                        lineHeight = 37.sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.clip(GalaxScreenStyle.pillShape)
                        .background(Color.White.copy(alpha = .14f))
                        .galaxLiquidChrome(GalaxScreenStyle.pillShape, emphasis = .78f)
                        .clickable(onClick = onClick),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 17.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(
                                if (isPlaying) R.string.player_a11y_pause else R.string.play,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
