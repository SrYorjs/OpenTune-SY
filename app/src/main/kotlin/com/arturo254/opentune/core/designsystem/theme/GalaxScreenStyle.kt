package com.arturo254.opentune.core.designsystem.theme

import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val pagePadding = 24.dp
    val artworkShape = RoundedCornerShape(20.dp)
    val panelShape = RoundedCornerShape(28.dp)
    val deepSpace = Color(0xFF060912)
    val violet = Color(0xFF8D7BF5)
    val blue = Color(0xFF6CABFF)
}

@Composable
fun Modifier.galaxScreenBackground(pureBlack: Boolean = false): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.background.luminance() < .45f
    val base = if (pureBlack) Color.Black else if (dark) GalaxScreenStyle.deepSpace else scheme.background
    val blue = if (dark) GalaxScreenStyle.blue else scheme.primary
    val violet = if (dark) GalaxScreenStyle.violet else scheme.tertiary
    return background(base).drawWithCache {
        val upper = Brush.radialGradient(
            listOf(blue.copy(alpha = if (dark) .13f else .07f), Color.Transparent),
            center = Offset(size.width, 0f), radius = size.width * .95f,
        )
        val lower = Brush.radialGradient(
            listOf(violet.copy(alpha = if (dark) .09f else .04f), Color.Transparent),
            center = Offset(0f, size.height * .68f), radius = size.width * .8f,
        )
        onDrawBehind { drawRect(upper); drawRect(lower) }
    }
}

@Composable
fun GalaxScreenHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = GalaxScreenStyle.pagePadding, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.Bold, letterSpacing = (-.8).sp,
        ))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun GalaxDestinationTheme(content: @Composable () -> Unit) {
    val original = MaterialTheme.colorScheme
    val reducedMotion = com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem.galaxMotion.reducedMotion
    val scheme = if (original.background.luminance() < .45f) original.copy(
        background = if (original.background == Color.Black) Color.Black else GalaxScreenStyle.deepSpace,
        surface = if (original.background == Color.Black) Color.Black else GalaxScreenStyle.deepSpace,
        surfaceContainerLowest = Color(0xFF04060D),
        surfaceContainerLow = Color(0xFF0D1220),
        surfaceContainer = Color(0xFF141B2C),
        surfaceContainerHigh = Color(0xFF1B253A),
        surfaceContainerHighest = Color(0xFF26334B),
    ) else original
    MaterialTheme(colorScheme = scheme, shapes = MaterialTheme.shapes.copy(
        small = RoundedCornerShape(12.dp), medium = GalaxScreenStyle.artworkShape,
        large = GalaxScreenStyle.panelShape, extraLarge = RoundedCornerShape(32.dp),
    )) {
        ProvideOpenTuneDesignSystem(artworkRadiusDp = 20f, reduceMotion = reducedMotion, content = content)
    }
}
