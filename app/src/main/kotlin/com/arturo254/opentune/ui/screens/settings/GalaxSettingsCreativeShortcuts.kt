package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R

@Composable
fun GalaxSettingsCreativeShortcuts(
    onPlayerStudio: () -> Unit,
    onLiquidGlass: () -> Unit,
    onPalette: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.galax_settings_creative_tools),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
            )
            Text(
                text = "03",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.primary,
                letterSpacing = 1.1.sp,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GalaxSettingsCreativeTile(
                title = stringResource(R.string.galax_settings_studio_shortcut),
                ordinal = "01",
                tint = scheme.primary,
                onClick = onPlayerStudio,
            )
            GalaxSettingsCreativeTile(
                title = stringResource(R.string.galax_settings_glass_shortcut),
                ordinal = "02",
                tint = scheme.tertiary,
                onClick = onLiquidGlass,
            )
            GalaxSettingsCreativeTile(
                title = stringResource(R.string.galax_settings_palette_shortcut),
                ordinal = "03",
                tint = scheme.secondary,
                onClick = onPalette,
            )
        }
    }
}

@Composable
private fun GalaxSettingsCreativeTile(
    title: String,
    ordinal: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.width(194.dp).height(110.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.surfaceContainerLow,
    ) {
        Box(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = .20f), colors.surfaceContainerLow),
                ),
            ),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val pivot = Offset(size.width * .86f, size.height * .15f)
                for (i in 1..5) {
                    drawCircle(
                        color = tint.copy(alpha = .09f + (i % 2) * .055f),
                        radius = size.width * (.14f + i * .08f),
                        center = pivot,
                        style = Stroke(width = .9.dp.toPx()),
                    )
                }
            }
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier.size(24.dp).background(tint.copy(alpha = .22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ordinal,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
