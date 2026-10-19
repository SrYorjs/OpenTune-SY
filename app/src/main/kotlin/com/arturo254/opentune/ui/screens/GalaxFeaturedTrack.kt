package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val upper = lerp(accent, Color.Black, .54f)
    val base = lerp(accent, Color.Black, .90f)
    Box(
        Modifier.padding(horizontal = GalaxScreenStyle.pagePadding, vertical = 9.dp)
            .fillMaxWidth()
            .height(306.dp)
            .clip(GalaxScreenStyle.panelShape),
    ) {
        Box(
            Modifier.matchParentSize().background(Brush.linearGradient(listOf(upper, base))),
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = .08f),
                        .27f to upper.copy(alpha = .16f),
                        .60f to base.copy(alpha = .61f),
                        1f to Color.Black.copy(alpha = .88f),
                    ),
                ),
            )
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        0f to base.copy(alpha = .32f),
                        .78f to Color.Transparent,
                    ),
                ),
            )
        }
        Column(
            Modifier.fillMaxSize().clickable(onClick = onClick).padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Box(
                    Modifier.size(7.dp).background(
                        GalaxScreenStyle.blue,
                        RoundedCornerShape(50),
                    ),
                )
                Text(
                    text = stringResource(R.string.galax_discover_for_you).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = .90f),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-.9).sp,
                        lineHeight = 34.sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artists.joinToString { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.clip(GalaxScreenStyle.pillShape)
                        .background(Color.White.copy(alpha = .11f))
                        .galaxLiquidChrome(GalaxScreenStyle.pillShape, emphasis = .78f)
                        .clickable(onClick = onClick),
                ) {
                    Row(
                        Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Icon(
                            painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = Color.White,
                        )
                        Text(
                            text = stringResource(
                                if (isPlaying) R.string.player_a11y_pause else R.string.play,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                }
            }
        }
    }
}
