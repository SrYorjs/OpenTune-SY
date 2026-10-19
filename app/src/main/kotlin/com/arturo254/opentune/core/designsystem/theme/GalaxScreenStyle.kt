package com.arturo254.opentune.core.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    val pagePadding = 20.dp
    val artworkShape = RoundedCornerShape(18.dp)
    val panelShape = RoundedCornerShape(30.dp)
    val pillShape = RoundedCornerShape(50)
    val deepSpace = Color(0xFF05070D)
    val midnight = Color(0xFF0B1120)
    val elevated = Color(0xFF151B2B)
    val violet = Color(0xFFA38AFF)
    val blue = Color(0xFF75B9FF)
    val ice = Color(0xFFE9F5FF)
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
    Column(
        modifier.fillMaxWidth().padding(horizontal = GalaxScreenStyle.pagePadding)
            .padding(top = 18.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier.size(6.dp).background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(50),
                ),
            )
            Text(
                text = "GALAXOS  /  MUSIC UNIVERSE",
                color = MaterialTheme.colorScheme.primary.copy(alpha = .84f),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                maxLines = 1,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.1).sp,
                lineHeight = 38.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(
            Modifier.padding(top = 5.dp).fillMaxWidth(.24f).height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to MaterialTheme.colorScheme.primary.copy(alpha = .68f),
                        1f to Color.Transparent,
                    ),
                ),
        )
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
        shapes = MaterialTheme.shapes.copy(
            extraSmall = RoundedCornerShape(9.dp),
            small = RoundedCornerShape(14.dp),
            medium = GalaxScreenStyle.artworkShape,
            large = GalaxScreenStyle.panelShape,
            extraLarge = RoundedCornerShape(36.dp),
        ),
    ) {
        ProvideOpenTuneDesignSystem(
            artworkRadiusDp = 18f,
            reduceMotion = reducedMotion,
            content = content,
        )
    }
}
