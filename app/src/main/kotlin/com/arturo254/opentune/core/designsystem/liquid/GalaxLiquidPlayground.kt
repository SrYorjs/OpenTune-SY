package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlin.math.roundToInt

/**
 * Dedicated Kyant-style optical lab, backed by an isolated, static sample
 * layer. Unlike the old NavHost setup, the captured source is a SIBLING of the
 * liquid preview; it cannot draw the glass recursively into itself.
 */
@Composable
fun GalaxLiquidPlayground(
    variant: GalaxLiquidVariant,
    tuning: GalaxLiquidTuning,
    onApply: (GalaxLiquidTuning) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(variant, tuning) { mutableStateOf(tuning) }
    var sampleEnabled by remember { mutableStateOf(true) }
    var sampleValue by remember { mutableFloatStateOf(.5f) }
    var sampleTab by remember { mutableIntStateOf(0) }
    val backdrop = rememberLayerBackdrop()
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Glass Playground", style = MaterialTheme.typography.titleMedium)
        Text("Vista previa en vivo. Ajusta el vidrio y pulsa Aplicar.", style = MaterialTheme.typography.bodySmall)
        Box(Modifier.fillMaxWidth().height(386.dp)) {
            // SOURCE: captured gradient and typography, with no sampling nodes.
            Box(
                Modifier.matchParentSize()
                    .layerBackdrop(backdrop)
                    .background(
                        Brush.linearGradient(
                            listOf(colors.primaryContainer, colors.tertiaryContainer, colors.secondaryContainer)
                        ),
                        RoundedCornerShape(30.dp),
                    ),
            ) {
                Column(Modifier.align(Alignment.Center).padding(12.dp)) {
                    Text("GALAXOT", style = MaterialTheme.typography.headlineLarge, color = colors.onPrimaryContainer)
                    Text("CRYSTAL • WATER • REFRACTION", color = colors.onPrimaryContainer)
                }
            }
            // CONSUMER: its own sibling overlay, never part of the capture.
            Box(
                Modifier.align(Alignment.Center)
                    .fillMaxWidth(.82f)
                    .height(92.dp)
                    .galaxLiquidEffect(
                        variant = variant,
                        tuning = draft,
                        backdrop = backdrop,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("GALAX LIQUID", fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GalaxLiquidButton(
                        text = "Button", onClick = { sampleEnabled = !sampleEnabled },
                        backdrop = backdrop, modifier = Modifier.weight(1f),
                    )
                    GalaxLiquidToggle(
                        checked = sampleEnabled, onCheckedChange = { sampleEnabled = it },
                        backdrop = backdrop, modifier = Modifier.weight(1f),
                    )
                }
                GalaxLiquidSlider(
                    value = sampleValue, onValueChange = { sampleValue = it },
                    backdrop = backdrop, modifier = Modifier.fillMaxWidth(),
                )
                GalaxLiquidBottomTabs(
                    selected = sampleTab,
                    labels = listOf("Home", "Buscar", "Música"),
                    onSelected = { sampleTab = it },
                    backdrop = backdrop, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        LabSlider("Corner radius", draft.cornerRadiusDp, 0f..64f, "dp") {
            draft = draft.copy(cornerRadiusDp = it)
        }
        LabSlider("Blur radius", draft.blurRadiusDp, 0f..32f, "dp") {
            draft = draft.copy(blurRadiusDp = it)
        }
        LabSlider("Refraction height", draft.refractionHeight, 0f..1f, "") {
            draft = draft.copy(refractionHeight = it)
        }
        LabSlider("Refraction amount", draft.refractionAmount, 0f..1f, "") {
            draft = draft.copy(refractionAmount = it)
        }
        LabSlider("Chromatic aberration", draft.chromaticAberration, 0f..1f, "") {
            draft = draft.copy(chromaticAberration = it)
        }
        LabSlider("Glass tint", draft.tintAlpha, 0f..0.65f, "") {
            draft = draft.copy(tintAlpha = it)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(onClick = { onApply(draft.bounded()) }) { Text("Aplicar a GalaxOT") }
        }
    }
}

@Composable
private fun LabSlider(
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
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Slider(value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange, valueRange = range)
    }
}
