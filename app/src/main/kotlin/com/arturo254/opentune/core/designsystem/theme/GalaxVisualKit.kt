package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Immutable
object GalaxVisualKit {
    val night = Color(0xFF050811)
    val abyss = Color(0xFF0A1020)
    val layer = Color(0xFF151D32)
    val azurite = Color(0xFF70B5FF)
    val ultraviolet = Color(0xFFAD8BFF)
    val frost = Color(0xFFEAF2FF)
    val muted = Color(0xFF99A9C4)
    val pageInset = 20.dp
    val smallGap = 8.dp
    val normalGap = 16.dp
    val largeGap = 24.dp
    val artworkRadius = 20.dp
    val tileRadius = 24.dp
    val panelRadius = 30.dp
    val chipRadius = 100.dp
    val imageShape = RoundedCornerShape(artworkRadius)
    val tileShape = RoundedCornerShape(tileRadius)
    val panelShape = RoundedCornerShape(panelRadius)
    val pillShape = RoundedCornerShape(chipRadius)
}

@Composable
fun Modifier.galaxNebulaSurface(
    shape: Shape = GalaxVisualKit.panelShape,
    accent: Color = MaterialTheme.colorScheme.primary,
    intensity: Float = 1f,
): Modifier {
    val theme = MaterialTheme.colorScheme
    val level = intensity.coerceIn(0f, 1f)
    val base = theme.surfaceContainerLow
    val violet = theme.tertiary
    return this
        .clip(shape)
        .background(base)
        .drawWithCache {
            val radial = Brush.radialGradient(
                0f to accent.copy(alpha = .24f * level),
                .55f to accent.copy(alpha = .045f * level),
                1f to Color.Transparent,
                center = Offset(size.width * .88f, size.height * .04f),
                radius = size.width.coerceAtLeast(1f) * 1.08f,
            )
            val lower = Brush.radialGradient(
                0f to violet.copy(alpha = .12f * level),
                1f to Color.Transparent,
                center = Offset(size.width * .08f, size.height * 1.02f),
                radius = size.width.coerceAtLeast(1f) * 1.18f,
            )
            val sheen = Brush.verticalGradient(
                0f to Color.White.copy(alpha = .055f * level),
                .14f to Color.Transparent,
                1f to Color.Black.copy(alpha = .13f),
            )
            onDrawBehind {
                drawRect(radial)
                drawRect(lower)
                drawRect(sheen)
            }
        }
}

@Composable
fun GalaxEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(Modifier.size(5.dp).background(color, CircleShape))
        Text(
            text = text.uppercase(),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GalaxGlyphFrame(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * .30f))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = .26f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = .13f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun GalaxArtworkTile(
    artworkUrl: String?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    artist: Boolean = false,
    width: Dp = 154.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    overlay: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        val shape = if (artist) CircleShape else GalaxVisualKit.imageShape
        Box(
            modifier = Modifier
                .size(width)
                .aspectRatio(1f)
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = .3f),
                            GalaxVisualKit.abyss,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = .24f),
                        ),
                    ),
                ),
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            overlay?.let {
                Box(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) { it() }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
