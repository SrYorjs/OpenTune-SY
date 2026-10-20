package com.arturo254.opentune.ui.player.preferences

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.v9.originalPlayerStyleConfig

@Composable
fun GalaxPlayerStyleGallery(
    selected: PlayerDesignStyle,
    onSelected: (PlayerDesignStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "ESTUDIO DE REPRODUCTORES",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.3.sp,
            )
            Text(
                text = "17 universos musicales",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Selecciona una vista para cambiar al instante. La elección se conserva.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(PlayerDesignStyle.entries, key = { it.name }) { style ->
                GalaxStylePreview(style, selected == style) { onSelected(style) }
            }
        }
    }
}

@Composable
private fun GalaxStylePreview(
    style: PlayerDesignStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.tertiary
    val shape = RoundedCornerShape(23.dp)
    Column(
        modifier = Modifier.width(154.dp)
            .clip(shape)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant,
                shape,
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(172.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = .20f), Color(0xFF050811)),
                    ),
                ),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * .5f, size.height * .43f)
                val r = size.width * .31f
                if (style == PlayerDesignStyle.V10 || style == PlayerDesignStyle.V12) {
                    for (i in 0..8) {
                        drawCircle(
                            color = if (i % 2 == 0) primary.copy(alpha = .43f) else secondary.copy(alpha = .35f),
                            radius = r * (.28f + i * .12f),
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                } else if (style == PlayerDesignStyle.V11) {
                    val spacing = 16.dp.toPx()
                    for (i in 0..13) {
                        drawLine(primary.copy(alpha = .25f), Offset(i * spacing, 0f), Offset(i * spacing, size.height), 1.dp.toPx())
                        drawLine(secondary.copy(alpha = .22f), Offset(0f, i * spacing), Offset(size.width, i * spacing), 1.dp.toPx())
                    }
                } else if (style == PlayerDesignStyle.V13) {
                    for (i in 0..7) {
                        drawLine(
                            if (i % 2 == 0) primary.copy(alpha = .28f) else secondary.copy(alpha = .18f),
                            Offset(0f, size.height * (.12f + i * .08f)),
                            Offset(size.width, size.height * (.30f + i * .06f)),
                            (4 + i).dp.toPx(),
                        )
                    }
                } else if (style == PlayerDesignStyle.V14) {
                    drawRect(primary.copy(alpha = .80f), Offset(size.width * .17f, size.height * .13f), Size(3.dp.toPx(), size.height * .53f))
                    drawRect(Color.White.copy(alpha = .17f), Offset(size.width * .35f, size.height * .22f), Size(size.width * .47f, size.height * .41f))
                } else if (style == PlayerDesignStyle.V15) {
                    for (i in 0..11) {
                        drawCircle(primary.copy(alpha = .13f), r * (.45f + i * .15f), center, style = Stroke(width = 1.dp.toPx()))
                    }
                } else {
                    drawRoundRect(
                        Brush.linearGradient(listOf(primary.copy(alpha = .76f), secondary.copy(alpha = .24f))),
                        Offset(center.x - r, center.y - r),
                        Size(r * 2f, r * 2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            if (style == PlayerDesignStyle.V9) 29.dp.toPx() else 12.dp.toPx(),
                        ),
                    )
                }
                drawCircle(Color.White.copy(alpha = .74f), 3.dp.toPx(), Offset(size.width * .5f, size.height * .85f))
                drawLine(Color.White.copy(alpha = .44f), Offset(size.width * .28f, size.height * .85f), Offset(size.width * .43f, size.height * .85f), 2.dp.toPx())
                drawLine(Color.White.copy(alpha = .44f), Offset(size.width * .57f, size.height * .85f), Offset(size.width * .72f, size.height * .85f), 2.dp.toPx())
            }
            Text(
                if (isSelected) "✓" else style.name,
                Modifier.align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = .6f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                color = if (isSelected) primary else Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = if (style.ordinal in 8..14 || style == PlayerDesignStyle.V17) originalPlayerStyleConfig(style).name
                else playerLayoutLabel(style),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (style.ordinal >= 8) "ORIGINAL GALAXOT" else if (style == PlayerDesignStyle.V16) "GALAXTHEME" else if (style == PlayerDesignStyle.V17) "VINJAX" else "ESTILO CLÁSICO",
            color = if (isSelected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = .6.sp,
        )
    }
}
