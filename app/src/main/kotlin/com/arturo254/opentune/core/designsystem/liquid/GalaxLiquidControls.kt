package com.arturo254.opentune.core.designsystem.liquid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop

/**
 * Four original GalaxOT controls modeled on Kyant's public catalog examples:
 * their OWN Compose interactions, built on our new optical renderer.
 * No catalog source is copied and no extra Backdrop capture is allocated.
 */
@Composable
fun GalaxLiquidButton(
    text: String,
    onClick: () -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_BUTTON,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    Box(
        modifier.galaxLiquidEffect(
            variant = variant,
            tuning = tuning,
            backdrop = backdrop,
            // Shape follows Playground corner radius for immediate visual feedback.
        ).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GalaxLiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_TOGGLE,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    Box(
        modifier
            .galaxLiquidEffect(
                variant = variant,
            tuning = tuning,
                backdrop = backdrop,
                // Shape follows Playground corner radius for immediate visual feedback.
            )
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (checked) "ON" else "OFF", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GalaxLiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_SLIDER,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.galaxLiquidEffect(
            variant = variant,
            tuning = tuning,
            backdrop = backdrop,
            // Shape follows Playground corner radius for immediate visual feedback.
        ).padding(horizontal = 12.dp),
        valueRange = 0f..1f,
    )
}

@Composable
fun GalaxLiquidBottomTabs(
    selected: Int,
    labels: List<String>,
    onSelected: (Int) -> Unit,
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    variant: GalaxLiquidVariant = GalaxLiquidVariant.KYANT_TABS,
    tuning: GalaxLiquidTuning = galvanicRecipe(variant),
) {
    Row(
        modifier.galaxLiquidEffect(
            variant = variant,
            tuning = tuning,
            backdrop = backdrop,
            // Shape follows Playground corner radius for immediate visual feedback.
        ).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { index, label ->
            Box(
                Modifier.weight(1f).height(35.dp).clickable(role = Role.Tab) { onSelected(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
