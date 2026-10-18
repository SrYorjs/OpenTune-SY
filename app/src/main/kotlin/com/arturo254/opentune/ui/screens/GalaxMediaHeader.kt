package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.core.designsystem.component.Artwork
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Composable
fun GalaxMediaHeader(
    title: String,
    label: String,
    artworkUrls: List<String?>,
    modifier: Modifier = Modifier,
    artist: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    facts: List<String> = emptyList(),
    details: @Composable ColumnScope.() -> Unit = {},
    supporting: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val backdrop = rememberLayerBackdrop()
    Box(modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .18f), Color.Transparent),
                Offset(size.width * .9f, size.height * .08f), size.width * .8f),
                size.width * .8f, Offset(size.width * .9f, size.height * .08f))
            drawCircle(Brush.radialGradient(listOf(GalaxScreenStyle.violet.copy(alpha = .08f), Color.Transparent),
                Offset(0f, size.height), size.width * .7f), size.width * .7f, Offset(0f, size.height))
        }
        CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val coverSize = (maxWidth * .43f).coerceIn(112.dp, 224.dp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        GalaxCoverMosaic(artworkUrls, artist, Modifier.size(coverSize))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                                color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold,
                                letterSpacing = (-.7).sp), maxLines = 4, overflow = TextOverflow.Ellipsis)
                            details()
                        }
                    }
                }
                GalaxMediaFacts(facts)
                supporting()
                actions()
            }
        }
    }
}

@Composable
fun GalaxCoverMosaic(urls: List<String?>, artist: Boolean = false, modifier: Modifier = Modifier) {
    val shape = if (artist) CircleShape else GalaxScreenStyle.artworkShape
    val images = urls.filterNotNull().filter(String::isNotBlank).distinct().take(4)
    if (images.size <= 1 || artist) {
        Artwork(images.firstOrNull(), modifier, shape = shape)
    } else BoxWithConstraints(modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        val tile = maxWidth / 2
        listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd).forEachIndexed { index, alignment ->
            AsyncImage(model = images[index % images.size], contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.align(alignment).size(tile))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalaxMediaFacts(facts: List<String>, modifier: Modifier = Modifier) {
    if (facts.isEmpty()) return
    FlowRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        facts.filter(String::isNotBlank).forEach { fact ->
            Text(fact, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .055f))
                    .padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@Composable
fun GalaxSectionHeading(title: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f))
        if (onClick != null) androidx.compose.material3.TextButton(onClick = onClick) {
            Text(androidx.compose.ui.res.stringResource(com.arturo254.opentune.R.string.see_all))
        }
    }
}
