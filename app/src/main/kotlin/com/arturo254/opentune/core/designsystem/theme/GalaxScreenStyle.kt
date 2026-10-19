package com.arturo254.opentune.core.designsystem.theme

import com.arturo254.opentune.core.designsystem.motion.galaxMotion

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object GalaxScreenStyle {
    val pagePadding = GalaxVisualKit.pageInset
    val artworkShape = GalaxVisualKit.imageShape
    val panelShape = GalaxVisualKit.panelShape
    val pillShape = GalaxVisualKit.pillShape
    val deepSpace = GalaxVisualKit.night
    val midnight = GalaxVisualKit.abyss
    val elevated = GalaxVisualKit.layer
    val violet = GalaxVisualKit.ultraviolet
    val blue = GalaxVisualKit.azurite
    val ice = GalaxVisualKit.frost
}

@Composable
fun Modifier.galaxScreenBackground(pureBlack: Boolean = false): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < .45f
    val ground = if (pureBlack) Color.Black else if (dark) GalaxScreenStyle.deepSpace else scheme.background
    val blue = if (dark) GalaxScreenStyle.blue else scheme.primary
    val violet = if (dark) GalaxScreenStyle.violet else scheme.tertiary
    return this.background(ground).drawWithCache {
        val span = size.width.coerceAtLeast(1f)
        val upper = Brush.radialGradient(
            0f to blue.copy(alpha = if (dark) .19f else .08f),
            .58f to blue.copy(alpha = if (dark) .035f else .015f),
            1f to Color.Transparent,
            center = Offset(size.width * .86f, size.height * .04f),
            radius = span * 1.24f,
        )
        val lower = Brush.radialGradient(
            0f to violet.copy(alpha = if (dark) .115f else .045f),
            .7f to violet.copy(alpha = .014f),
            1f to Color.Transparent,
            center = Offset(size.width * .12f, size.height * .86f),
            radius = span * 1.1f,
        )
        val stars = listOf(
            Offset(.12f, .08f), Offset(.79f, .16f), Offset(.39f, .35f),
            Offset(.91f, .48f), Offset(.24f, .66f), Offset(.63f, .87f),
        )
        onDrawBehind {
            drawRect(upper)
            drawRect(lower)
            if (dark && !pureBlack) {
                stars.forEachIndexed { index, position ->
                    drawCircle(
                        color = Color.White.copy(alpha = if (index % 2 == 0) .17f else .11f),
                        radius = if (index % 3 == 0) 1.3.dp.toPx() else .75.dp.toPx(),
                        center = Offset(size.width * position.x, size.height * position.y),
                    )
                }
            }
        }
    }
}

@Composable
fun GalaxScreenHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GalaxScreenStyle.pagePadding)
            .padding(top = 23.dp, bottom = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = if (subtitle.isBlank()) 63.dp else 86.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.primary,
                            colors.tertiary.copy(alpha = .66f),
                            Color.Transparent,
                        ),
                    ),
                    GalaxVisualKit.pillShape,
                ),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            GalaxEyebrow("GALAXOT  /  MUSIC UNIVERSE")
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = colors.onSurface,
                maxLines = 2,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
fun GalaxDestinationTheme(immersive: Boolean = false, content: @Composable () -> Unit) {
    val original = if (immersive) galaxColorScheme(dark = true) else MaterialTheme.colorScheme
    val reducedMotion = com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem.galaxMotion.reducedMotion
    val isDark = original.background.luminance() < .45f
    val scheme = if (isDark) original.copy(
        background = if (original.background == Color.Black) Color.Black else GalaxScreenStyle.deepSpace,
        surface = if (original.background == Color.Black) Color.Black else GalaxScreenStyle.midnight,
        surfaceDim = Color(0xFF060912),
        surfaceContainerLowest = Color(0xFF060912),
        surfaceContainerLow = Color(0xFF0C1220),
        surfaceContainer = Color(0xFF121A2A),
        surfaceContainerHigh = Color(0xFF1B2537),
        surfaceContainerHighest = Color(0xFF26334A),
        outlineVariant = Color(0xFF354159),
    ) else original
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-.85).sp,
            ),
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-.4).sp,
            ),
        ),
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(9.dp),
            small = RoundedCornerShape(14.dp),
            medium = GalaxScreenStyle.artworkShape,
            large = GalaxScreenStyle.panelShape,
            extraLarge = RoundedCornerShape(36.dp),
        ),
    ) {
        ProvideOpenTuneDesignSystem(
            artworkRadiusDp = 20f,
            reduceMotion = reducedMotion,
        ) {
            Box(Modifier.fillMaxSize().galaxScreenBackground()) {
                content()
            }
        }
    }
}
