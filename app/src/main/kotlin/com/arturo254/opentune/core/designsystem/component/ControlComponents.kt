package com.arturo254.opentune.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.theme.spacing
import com.arturo254.opentune.ui.component.LiquidGlassIconButton
import com.arturo254.opentune.ui.component.LiquidGlassPillButton

/**
 * Stateless controls: host screens own input state, preference persistence,
 * actions, haptic feedback and playback. Standard controls remain Material 3.
 */
@Composable
fun OpenTuneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(OpenTuneDesignSystem.spacing.sm))
        }
        Text(text)
    }
}

/** Retains the LiquidGlassPillButton layout and touch behavior of ChipsRow. */
@Composable
fun OpenTunePillChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    horizontalPadding: Dp = 14.dp,
    content: @Composable RowScope.() -> Unit,
) {
    LiquidGlassPillButton(
        onClick = onClick,
        modifier = modifier,
        selected = selected,
        enabled = enabled,
        horizontalPadding = horizontalPadding,
        content = content,
    )
}

@Composable
fun OpenTuneSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
    )
}

@Composable
fun OpenTuneSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun OpenTuneIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun OpenTuneIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pureBlack: Boolean = false,
    size: Dp = 46.dp,
) {
    LiquidGlassIconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        pureBlack = pureBlack,
        size = size,
    ) {
        OpenTuneIcon(
            icon = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
