package com.arturo254.opentune.ui.player.panels

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import java.util.Locale
import kotlin.math.abs

fun equalizerGainLabel(millibels: Int): String {
    val db = millibels / 100f
    return String.format(Locale.US, "%+.1f dB", db)
}

@Composable
fun GalaxEqStudioHeader(
    enabled: Boolean,
    selectedProfile: String,
    levels: List<Int>,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    val average = if (levels.isEmpty()) 0 else levels.average().toInt()
    val gain = equalizerGainLabel(average)

    Box(
        modifier = modifier.fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        palette.primary.copy(alpha = .20f),
                        palette.surfaceContainer,
                        palette.tertiary.copy(alpha = .13f),
                    ),
                ),
                RoundedCornerShape(28.dp),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .88f, size.height * .19f)
            for (index in 1..7) {
                drawCircle(
                    color = palette.primary.copy(alpha = .06f + index * .009f),
                    radius = size.width * (.09f + index * .052f),
                    center = center,
                    style = Stroke(width = .9.dp.toPx()),
                )
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier.size(7.dp).background(
                        if (enabled) palette.primary else palette.outline,
                        CircleShape,
                    ),
                )
                Text(
                    text = stringResource(R.string.galax_eq_response_title),
                    color = palette.primary,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = selectedProfile,
                color = palette.onSurface,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.galax_eq_profile_status),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(if (enabled) R.string.enabled else R.string.disabled),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (enabled) palette.primary else palette.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = gain,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary,
                    )
                    Text(
                        text = stringResource(R.string.galax_eq_response_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
