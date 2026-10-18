package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.R
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
                    val result = context.imageLoader.execute(ImageRequest.Builder(context)
                        .data(artwork).size(96).allowHardware(false).build())
                    val bitmap = result.image?.toBitmap()
                    if (bitmap == null) fallback else withContext(Dispatchers.Default) {
                        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                        palette.vibrantSwatch?.rgb?.let(::Color)
                            ?: palette.dominantSwatch?.rgb?.let(::Color) ?: fallback
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    fallback
                }
            }
        }
    }
    Column(Modifier.padding(horizontal = GalaxScreenStyle.pagePadding, vertical = 8.dp)
        .fillMaxWidth().clip(GalaxScreenStyle.panelShape)
        .background(Brush.linearGradient(listOf(lerp(accent, Color.Black, .38f), lerp(accent, Color.Black, .78f))))
        .clickable(onClick = onClick).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            AsyncImage(model = artwork, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.widthIn(max = 176.dp).weight(1f).aspectRatio(1f).clip(GalaxScreenStyle.artworkShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.galax_discover_for_you), style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = .76f))
                Text(song.song.title, style = MaterialTheme.typography.titleLarge, color = Color.White,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(song.artists.joinToString { it.name }, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = .8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                FilledTonalIconButton(onClick = onClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White,
                        contentColor = Color(0xFF10121C)), modifier = Modifier.size(48.dp)) {
                    Icon(painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        stringResource(if (isPlaying) R.string.player_a11y_pause else R.string.play))
                }
            }
        }
    }
}
