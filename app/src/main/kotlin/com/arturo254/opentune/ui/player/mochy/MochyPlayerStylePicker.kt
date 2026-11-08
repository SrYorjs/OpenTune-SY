package com.arturo254.opentune.ui.player.mochy

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.component.mochyPress3D
import com.arturo254.opentune.ui.player.preferences.GalaxPlayerStyleGallery

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MochyPlayerStylePicker(
    native: PlayerDesignStyle,
    mochy: MochyPlayerStyle,
    onNative: (PlayerDesignStyle) -> Unit,
    onMochy: (MochyPlayerStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        contentColor = colors.onSurface,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item("heading") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("GALAXOT × MOCHY", color = colors.primary,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                        style = MaterialTheme.typography.labelSmall)
                    Text("Tu estudio de reproductores",
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineMedium)
                    Text("17 estilos originales y 10 experiencias Mochy en un solo lugar.",
                        color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
            item("originals") {
                GalaxPlayerStyleGallery(
                    selected = native,
                    onSelected = {
                        onMochy(MochyPlayerStyle.NONE)
                        onNative(it)
                        onDismiss()
                    },
                )
            }
            item("mochy-heading") {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text("MOCHY MUSIC", color = colors.primary,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                    Text("Nuevos universos", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold)
                    Text("Inspirados en los estilos de Mochy Music, adaptados a la reproducción de GalaxOT.",
                        color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            items(MochyPlayerStyle.selectable.chunked(2), key = { chunk -> chunk.joinToString { it.name } }) { chunk ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    chunk.forEach { style ->
                        MochyStylePreview(
                            style = style,
                            selected = mochy == style,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                onMochy(style)
                                onDismiss()
                            },
                        )
                    }
                    if (chunk.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item("license") {
                Text(
                    "Mochy Music — realidkroo · GNU GPL-3.0",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun MochyStylePreview(
    style: MochyPlayerStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val shape = RoundedCornerShape(25.dp)
    val surface by animateColorAsState(
        if (selected) colors.primaryContainer else colors.surfaceContainerLow,
        label = "Mochy picker color",
    )
    val scale by animateFloatAsState(
        if (selected) 1f else .985f,
        animationSpec = spring(dampingRatio = .85f, stiffness = 380f),
        label = "Mochy picker scale",
    )
    Column(
        modifier.mochyPress3D(maxTilt = 5f, pressedScale = .96f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(surface)
            .border(1.dp, if (selected) accent else colors.outlineVariant.copy(alpha = .34f), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().height(135.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = .30f), Color(0xFF0B1019)))),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val c = Offset(size.width * .5f, size.height * .44f)
                when (style) {
                    MochyPlayerStyle.TURNTABLE, MochyPlayerStyle.TIMELINE -> {
                        repeat(9) {
                            drawCircle(
                                accent.copy(alpha = .25f - it * .018f),
                                radius = size.minDimension * (.14f + it * .036f),
                                center = c, style = Stroke(1.dp.toPx()),
                            )
                        }
                    }
                    MochyPlayerStyle.IPOD -> {
                        drawRoundRect(Color.White.copy(alpha = .86f),
                            topLeft = Offset(size.width * .24f, size.height * .08f),
                            size = androidx.compose.ui.geometry.Size(size.width * .52f, size.height * .82f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
                        drawCircle(Color.DarkGray, size.width * .13f, c.copy(y = size.height * .62f))
                        drawCircle(Color.White, size.width * .062f, c.copy(y = size.height * .62f))
                    }
                    MochyPlayerStyle.BEDSIDE, MochyPlayerStyle.BEDSIDE_TILES -> {
                        drawRoundRect(accent.copy(alpha = .20f),
                            topLeft = Offset(size.width * .14f, size.height * .20f),
                            size = androidx.compose.ui.geometry.Size(size.width * .72f, size.height * .56f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()))
                    }
                    MochyPlayerStyle.LYRICS -> {
                        repeat(4) { index ->
                            drawLine(
                                Color.White.copy(alpha = .68f - index * .12f),
                                Offset(size.width * .20f, size.height * (.25f + index * .13f)),
                                Offset(size.width * (if (index == 3) .57f else .83f),
                                    size.height * (.25f + index * .13f)), 5.dp.toPx(),
                            )
                        }
                    }
                    MochyPlayerStyle.VIDEO -> {
                        drawRoundRect(Color.Black.copy(alpha = .90f),
                            topLeft = Offset(size.width * .12f, size.height * .29f),
                            size = androidx.compose.ui.geometry.Size(size.width * .76f, size.height * .43f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
                    }
                    else -> {
                        drawRoundRect(accent.copy(alpha = .68f),
                            topLeft = Offset(size.width * .22f, size.height * .15f),
                            size = androidx.compose.ui.geometry.Size(size.width * .56f, size.height * .55f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(15.dp.toPx()))
                        drawLine(Color.White.copy(alpha = .68f),
                            Offset(size.width * .23f, size.height * .81f),
                            Offset(size.width * .68f, size.height * .81f), 3.dp.toPx())
                    }
                }
            }
            if (style == MochyPlayerStyle.BEDSIDE || style == MochyPlayerStyle.BEDSIDE_TILES) {
                Text("23:47", Modifier.align(Alignment.Center),
                    color = Color.White, fontWeight = FontWeight.Light, fontSize = 25.sp)
            }
            if (selected) {
                Text("✓", Modifier.align(Alignment.TopEnd).padding(10.dp),
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Text(style.title, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall)
        Text(style.subtitle, color = colors.onSurfaceVariant,
            maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall)
    }
}
