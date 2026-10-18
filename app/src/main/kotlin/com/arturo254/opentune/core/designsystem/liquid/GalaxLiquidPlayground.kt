package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

/**
 * Live preview over a genuine, separately captured Kyant backdrop.
 *
 * The captured source contains ONLY wallpaper and lettering. The lens and
 * four interactive samples are its siblings, so they can never capture their
 * own graphics layers. This preview is driven by the SAME variant and tuning
 * values as the global application — no local shadow draft and no fixed demo
 * recipes that ignore the sliders.
 */
/**
 * Glass Studio 2.0: isolated source layer with an optional SAF photo.
 * The photo, patterned typography and color field are behind the lens,
 * while all sample controls exist in sibling layers and never capture themselves.
 */
@Composable
fun GalaxLiquidPreview(
    variant: GalaxLiquidVariant,
    tuning: GalaxLiquidTuning,
    backgroundUri: String = "",
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val backdrop = rememberLayerBackdrop()
    var wallpaperIndex by remember { mutableIntStateOf(0) }
    var usePersonalPhoto by remember(backgroundUri) { mutableStateOf(backgroundUri.isNotBlank()) }
    var enabled by remember { mutableStateOf(true) }
    var sampleValue by remember { mutableFloatStateOf(.58f) }
    var sampleTab by remember { mutableIntStateOf(0) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val previewVariant = if (enabled) variant else GalaxLiquidVariant.DISABLED
    val backdropColors = when (wallpaperIndex % 3) {
        1 -> listOf(Color(0xFF14233D), Color(0xFF31ACC9), Color(0xFFA1F8F1))
        2 -> listOf(Color(0xFF1E123A), Color(0xFF8C56D6), Color(0xFFF7C2EA))
        else -> listOf(Color(0xFF082B58), Color(0xFF177CB7), Color(0xFFBAE1FF))
    }
    val previewShape = RoundedCornerShape(30.dp)
    Box(
        modifier.fillMaxWidth().height(430.dp)
            // HWUI must clip every drawBackdrop and AndroidView descendant
            // to this viewport; ordinary Modifier.clip alone did not stop
            // a detached black preview rectangle escaping at the top.
            .graphicsLayer {
                shape = previewShape
                clip = true
                compositingStrategy = CompositingStrategy.Offscreen
            },
    ) {
        // The only captured subtree. NEVER put drawBackdrop controls here.
        Box(
            Modifier.matchParentSize()
                // Source recording comes BEFORE all its painted contents,
                // otherwise the sampler sees an empty/black capture layer.
                .layerBackdrop(backdrop)
                .background(Brush.linearGradient(backdropColors)),
        ) {
            if (usePersonalPhoto && backgroundUri.isNotBlank()) {
                coil3.compose.AsyncImage(
                    model = android.net.Uri.parse(backgroundUri),
                    contentDescription = "Fondo elegido",
                    modifier = Modifier.matchParentSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
            Box(
                Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = .24f), Color.Transparent, Color.Black.copy(alpha = .43f)),
                        ),
                    ),
            )
            Column(Modifier.align(Alignment.TopStart).padding(18.dp)) {
                Text(
                    "GALAXOT    •    GLASS STUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = .9f),
                )
                Text(
                    "WATER / REFRACTION",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Text("TOCA, ARRASTRA Y COMPARA",
                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .85f))
            }
            // High-frequency background detail exposes real blur/refraction differences.
            Text(
                "GALAXOT  / / /  01  02  03  \u2726  LIQUID  / / /  04  05  06",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = .76f),
                maxLines = 2,
            )
            Text(
                "LENTE     AGUA     CRISTAL     LUZ",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = .76f),
            )
        }

        // Draggable optical sample; its pan never becomes part of the capture.
        Box(
            Modifier.align(Alignment.Center)
                .offset {
                    androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt() - 23)
                }
                .fillMaxWidth(.78f)
                .height(84.dp)
                .galaxLiquidEffect(variant = previewVariant, tuning = tuning, backdrop = backdrop)
                .pointerInput(variant) {
                    detectDragGesturesAfterLongPress { change, drag ->
                        offsetX = (offsetX + drag.x).coerceIn(-62f, 62f)
                        offsetY = (offsetY + drag.y).coerceIn(-54f, 54f)
                        change.consume()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (previewVariant == GalaxLiquidVariant.DISABLED) "GLASS OFF"
                    else variant.title.substringAfter(" · "),
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Mantén pulsado para mover", color = colors.onSurface, style = MaterialTheme.typography.labelSmall)
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GalaxLiquidButton(
                    text = if (usePersonalPhoto) "Fondo demo" else "Cambiar fondo",
                    onClick = {
                        if (usePersonalPhoto) usePersonalPhoto = false
                        else wallpaperIndex = (wallpaperIndex + 1) % 3
                        offsetX = 0f
                        offsetY = 0f
                    },
                    backdrop = backdrop, variant = variant, tuning = tuning, modifier = Modifier.weight(1f),
                )
                if (backgroundUri.isNotBlank()) {
                    GalaxLiquidButton(
                        text = "Mi foto", onClick = { usePersonalPhoto = true },
                        backdrop = backdrop, variant = variant, tuning = tuning, modifier = Modifier.weight(.65f),
                    )
                }
                GalaxLiquidToggle(
                    checked = enabled, onCheckedChange = { enabled = it },
                    backdrop = backdrop, variant = variant, tuning = tuning,
                )
            }
            GalaxLiquidSlider(
                value = sampleValue, onValueChange = { sampleValue = it },
                backdrop = backdrop, variant = variant, tuning = tuning,
                modifier = Modifier.fillMaxWidth(),
            )
            GalaxLiquidBottomTabs(
                selected = sampleTab,
                labels = listOf("Inicio", "Buscar", "Música"),
                onSelected = { sampleTab = it },
                backdrop = backdrop, variant = variant, tuning = tuning,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun GalaxLiquidOpticsControls(
    tuning: GalaxLiquidTuning,
    onChange: (GalaxLiquidTuning) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Glass Playground", style = MaterialTheme.typography.titleMedium)
        Text(
            "Los ajustes cambian la vista previa inmediatamente y se guardan automáticamente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LiquidTuningSlider("Corner radius", tuning.cornerRadiusDp, 0f..64f, "dp") {
            onChange(tuning.copy(cornerRadiusDp = it))
        }
        LiquidTuningSlider("Blur radius", tuning.blurRadiusDp, 0f..32f, "dp") {
            onChange(tuning.copy(blurRadiusDp = it))
        }
        LiquidTuningSlider("Refraction height", tuning.refractionHeight, 0f..1f, "") {
            onChange(tuning.copy(refractionHeight = it))
        }
        LiquidTuningSlider("Refraction amount", tuning.refractionAmount, 0f..1f, "") {
            onChange(tuning.copy(refractionAmount = it))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Chromatic aberration", style = MaterialTheme.typography.bodyMedium)
                Text("Reflejos de color en los bordes", style = MaterialTheme.typography.labelSmall)
            }
            Switch(
                checked = tuning.chromaticAberration > .5f,
                onCheckedChange = { onChange(tuning.copy(chromaticAberration = if (it) 1f else 0f)) },
            )
        }
        LiquidTuningSlider("Glass tint", tuning.tintAlpha, 0f..0.65f, "") {
            onChange(tuning.copy(tintAlpha = it))
        }
    }
}

@Composable
private fun LiquidTuningSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    units: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (units.isNotEmpty()) "${value.roundToInt()} $units"
                else "${(value * 100f).roundToInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
        )
    }
}
