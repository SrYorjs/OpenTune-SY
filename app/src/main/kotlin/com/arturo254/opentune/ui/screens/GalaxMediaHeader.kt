package com.arturo254.opentune.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawWithCache
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
    val scheme = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth()) {
        Box(
            Modifier.matchParentSize()
                .layerBackdrop(backdrop)
                .drawWithCache {
                    val radius = size.width.coerceAtLeast(1f) * 1.13f
                    val accentCenter = Offset(size.width * .78f, size.height * .22f)
                    val blue = Brush.radialGradient(
                        0f to accent.copy(alpha = .20f),
                        .65f to accent.copy(alpha = .035f),
                        1f to Color.Transparent,
                        center = accentCenter,
                        radius = radius,
                    )
                    val purpleCenter = Offset(0f, size.height)
                    val purpleRadius = size.width * .9f
                    val purple = Brush.radialGradient(
                        0f to scheme.tertiary.copy(alpha = .13f),
                        1f to Color.Transparent,
                        center = purpleCenter,
                        radius = purpleRadius,
                    )
                    onDrawBehind {
                        drawCircle(blue, radius = radius, center = accentCenter)
                        drawCircle(purple, radius = purpleRadius, center = purpleCenter)
                    }
                },
        )
        CompositionLocalProvider(LocalGalaxLiquidBackdrop provides backdrop) {
            Column(
                Modifier.fillMaxWidth().padding(
                    horizontal = GalaxScreenStyle.pagePadding,
                    vertical = 30.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(23.dp),
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 560.dp
                    val cover = if (wide) 220.dp else (maxWidth * .65f).coerceIn(178.dp, 292.dp)
                    if (wide) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(26.dp),
                        ) {
                            GalaxMediaCoverStage(artworkUrls, artist, Modifier.size(cover))
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
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                        ) {
                            GalaxMediaTitle(label, title, centered = false)
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                GalaxMediaCoverStage(
                                    artworkUrls,
                                    artist,
                                    Modifier.size((maxWidth * .76f).coerceIn(210.dp, 326.dp)),
                                )
                            }
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
private fun GalaxMediaCoverStage(
    artworkUrls: List<String?>,
    artist: Boolean,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.matchParentSize()
                .drawWithCache {
                    val center = Offset(size.width * .51f, size.height * .48f)
                    val radial = Brush.radialGradient(
                        0f to scheme.primary.copy(alpha = .28f),
                        .64f to scheme.tertiary.copy(alpha = .14f),
                        1f to Color.Transparent,
                        center = center,
                        radius = size.width * .85f,
                    )
                    onDrawBehind {
                        drawCircle(radial, radius = size.width * .75f, center = center)
                        for (i in 1..4) {
                            drawCircle(
                                scheme.primary.copy(alpha = .085f),
                                radius = size.width * (.30f + i * .085f),
                                center = center,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                            )
                        }
                    }
                },
        )
        GalaxCoverMosaic(
            artworkUrls, artist,
            Modifier.fillMaxSize(.89f).clip(
                if (artist) CircleShape else GalaxScreenStyle.artworkShape,
            ),
        )
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
