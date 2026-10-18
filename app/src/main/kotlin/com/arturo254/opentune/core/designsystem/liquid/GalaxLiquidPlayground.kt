package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
@Composable
fun GalaxLiquidPreview(
    variant: GalaxLiquidVariant,
    tuning: GalaxLiquidTuning,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val backdrop = rememberLayerBackdrop()
    var alternateWallpaper by remember { mutableStateOf(false) }
    var sampleEnabled by remember { mutableStateOf(true) }
    var sampleValue by remember { mutableFloatStateOf(.58f) }
    var sampleTab by remember { mutableIntStateOf(0) }
    val wallpaper = if (alternateWallpaper) {
        listOf(colors.tertiary, colors.primaryContainer, colors.secondary)
    } else {
        listOf(colors.primary, colors.tertiaryContainer, colors.secondaryContainer)
    }

    Box(
        modifier.fillMaxWidth().height(318.dp).clip(RoundedCornerShape(26.dp))
    ) {
        // SOURCE: no drawBackdrop(), no glass consumers below layerBackdrop.
        Box(
            Modifier.matchParentSize()
                .layerBackdrop(backdrop)
                .background(Brush.linearGradient(wallpaper))
        ) {
            Column(
                Modifier.align(Alignment.TopStart).padding(18.dp)
            ) {
                Text(
                    "GALAXOT   /   GLASS STUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimaryContainer.copy(alpha = .88f),
                )
                Text(
                    "AQUA    /    LIQUID",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = colors.onPrimaryContainer,
                )
                Text(
                    "✦   CRYSTAL    ✦    WATER    ✦",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer,
                )
            }
            Text(
                "GALAXOT      14 STYLES      2026",
                modifier = Modifier.align(Alignment.Center).padding(top = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer.copy(alpha = .73f),
            )
        }

        Box(
            Modifier.align(Alignment.Center)
                .padding(bottom = 35.dp)
                .fillMaxWidth(.79f)
                .height(81.dp)
                .galaxLiquidEffect(
                    variant = variant,
                    tuning = tuning,
                    backdrop = backdrop,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (variant == GalaxLiquidVariant.DISABLED) "GLASS OFF" else "GALAX LIQUID",
                color = colors.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                GalaxLiquidButton(
                    text = "Cambiar fondo",
                    onClick = { alternateWallpaper = !alternateWallpaper },
                    backdrop = backdrop,
                    variant = variant,
                    tuning = tuning,
                    modifier = Modifier.weight(1f),
                )
                GalaxLiquidToggle(
                    checked = sampleEnabled,
                    onCheckedChange = { sampleEnabled = it },
                    backdrop = backdrop,
                    variant = variant,
                    tuning = tuning,
                    modifier = Modifier.weight(.48f),
                )
            }
            GalaxLiquidSlider(
                value = sampleValue,
                onValueChange = { sampleValue = it },
                backdrop = backdrop,
                variant = variant,
                tuning = tuning,
                modifier = Modifier.fillMaxWidth().height(34.dp),
            )
            GalaxLiquidBottomTabs(
                selected = sampleTab,
                labels = listOf("Home", "Buscar", "Biblioteca"),
                onSelected = { sampleTab = it },
                backdrop = backdrop,
                variant = variant,
                tuning = tuning,
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
