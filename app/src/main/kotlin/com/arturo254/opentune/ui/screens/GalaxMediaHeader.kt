package com.arturo254.opentune.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.arturo254.opentune.core.designsystem.component.Artwork
import com.arturo254.opentune.core.designsystem.liquid.LocalGalaxLiquidBackdrop
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
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
            val radius = size.width.coerceAtLeast(1f) * 1.13f
            drawCircle(
                Brush.radialGradient(
                    0f to accent.copy(alpha = .20f),
                    .65f to accent.copy(alpha = .035f),
                    1f to Color.Transparent,
                    center = Offset(size.width * .78f, size.height * .22f),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(size.width * .78f, size.height * .22f),
            )
            drawCircle(
                Brush.radialGradient(
                    0f to GalaxScreenStyle.violet.copy(alpha = .085f),
                    1f to Color.Transparent,
                    center = Offset(0f, size.height),
                    radius = size.width * .9f,
                ),
                radius = size.width * .9f,
                center = Offset(0f, size.height),
            )
        }
        CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop) {
            Column(
                Modifier.fillMaxWidth().padding(
                    horizontal = GalaxScreenStyle.pagePadding,
                    vertical = 22.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(17.dp),
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 560.dp
                    val cover = if (wide) 220.dp else (maxWidth * .65f).coerceIn(178.dp, 292.dp)
                    if (wide) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(26.dp),
                        ) {
                            GalaxCoverMosaic(artworkUrls, artist, Modifier.size(cover))
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                GalaxMediaTitle(label, title, centered = false)
                                details()
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            GalaxCoverMosaic(artworkUrls, artist, Modifier.size(cover))
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(9.dp),
                            ) {
                                GalaxMediaTitle(label, title, centered = true)
                                details()
                            }
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
private fun GalaxMediaTitle(label: String, title: String, centered: Boolean) {
    Column(
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                lineHeight = 35.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
    }
}

@Composable
fun GalaxCoverMosaic(urls: List<String?>, artist: Boolean = false, modifier: Modifier = Modifier) {
    val shape = if (artist) CircleShape else GalaxScreenStyle.artworkShape
    val images = urls.filterNotNull().filter(String::isNotBlank).distinct().take(4)
    if (images.size <= 1 || artist) {
        Artwork(images.firstOrNull(), modifier, shape = shape)
    } else {
        BoxWithConstraints(
            modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            val tile = maxWidth / 2
            listOf(
                Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd,
            ).forEachIndexed { index, alignment ->
                AsyncImage(
                    model = images[index % images.size],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.align(alignment).size(tile),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GalaxMediaFacts(facts: List<String>, modifier: Modifier = Modifier) {
    if (facts.isEmpty()) return
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        facts.filter(String::isNotBlank).forEach { fact ->
            Text(
                text = fact,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.galaxLiquidChrome(
                    GalaxScreenStyle.pillShape, emphasis = .35f,
                ).padding(horizontal = 13.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun GalaxSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(
            start = GalaxScreenStyle.pagePadding,
            end = GalaxScreenStyle.pagePadding,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold, letterSpacing = (-.35).sp,
                ),
            )
            Spacer(
                Modifier.fillMaxWidth(.17f).height(2.dp).background(
                    MaterialTheme.colorScheme.primary.copy(alpha = .53f),
                    GalaxScreenStyle.pillShape,
                ),
            )
        }
        if (onClick != null) {
            androidx.compose.material3.TextButton(onClick = onClick) {
                Text(androidx.compose.ui.res.stringResource(com.arturo254.opentune.R.string.see_all))
            }
        }
    }
}
