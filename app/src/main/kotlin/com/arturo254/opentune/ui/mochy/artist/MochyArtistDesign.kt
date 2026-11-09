package com.arturo254.opentune.ui.mochy.artist

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import kotlin.math.min

internal const val ArtistAvatarAt = .45f
internal val ArtistAvatarSize = 96.dp
internal val ArtistNameStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 40.sp,
    letterSpacing = (-.05).em,
)
private val BoneColor = Color(0xFF262627)

@Composable
private fun BoxScope.BannerScrims(background: State<Color>) {
    Box(
        modifier = Modifier
            .matchParentSize()
            .drawWithCache {
                val h = size.height
                val top = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                    endY = h * 0.3f,
                )
                onDrawBehind {
                    val bg = background.value
                    drawRect(Color.Black.copy(alpha = 0.14f))
                    drawRect(top, size = Size(size.width, h * 0.3f))
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.5f to bg.copy(alpha = 0.55f),
                            0.82f to bg.copy(alpha = 0.92f),
                            1f to bg,
                            startY = h * 0.42f,
                            endY = h,
                        ),
                        topLeft = Offset(0f, h * 0.42f),
                        size = Size(size.width, h * 0.58f),
                    )
                }
            }
    )
}



@Composable
internal fun ArtistAvatar(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ArtistAvatarSize)
            .clip(CircleShape)
            .background(BoneColor),
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}



@Composable
internal fun ArtistName(
    name: String,
    logoUrl: String?,
    logoAspect: Float?,
    modifier: Modifier = Modifier,
) {
    var logoLoaded by remember(logoUrl) { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(if (logoLoaded) 1f else 0f, tween(350), label = "artistLogo")
    Box(modifier = modifier.animateContentSize(spring(dampingRatio = 0.9f, stiffness = 380f))) {
        if (logoUrl != null) {
            val aspect = (logoAspect ?: 3f).coerceIn(0.6f, 12f)
            val width = minOf(260.dp, 72.dp * aspect)
            AsyncImage(
                model = logoUrl,
                contentDescription = name,
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomStart,
                onSuccess = { logoLoaded = true },
                modifier = Modifier
                    .size(width = width, height = width / aspect)
                    .graphicsLayer { alpha = logoAlpha },
            )
        }
        if (!logoLoaded) {
            Text(
                text = name,
                style = ArtistNameStyle,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}




@Composable
fun MochyArtistHeader(
    name: String,
    photoUrl: String?,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val background = rememberUpdatedState(Color.Black.copy(alpha = .93f))
    val height = (LocalConfiguration.current.screenHeightDp * .56f).dp
    Box(modifier.fillMaxWidth().height(height)) {
        AsyncImage(
            model = photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        BannerScrims(background)
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 23.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            ArtistAvatar(photoUrl)
            ArtistName(name, logoUrl = null, logoAspect = null)
        }
    }
}
