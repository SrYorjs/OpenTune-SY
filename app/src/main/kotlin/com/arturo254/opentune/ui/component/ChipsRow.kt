/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.component.OpenTunePillChip
import com.arturo254.opentune.ui.screens.OptionStats

@Composable
fun <E> ChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    icons: Map<E, Int> = emptyMap(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(14.dp))

        chips.forEach { (value, label) ->
            val selected = currentValue == value
            val iconRes = icons[value]

            OpenTunePillChip(
                onClick = { onValueUpdate(value) },
                selected = selected,
                horizontalPadding = 14.dp,
            ) {
                AnimatedContent(
                    targetState = selected,
                    transitionSpec = {
                        fadeIn(tween(90)) + slideInHorizontally { -it / 3 } togetherWith
                            fadeOut(tween(70)) + slideOutHorizontally { it / 3 }
                    },
                    label = "chipIcon",
                ) { isSelected ->
                    when {
                        isSelected ->
                            Icon(
                                painter = painterResource(R.drawable.done),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )

                        iconRes != null ->
                            Icon(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                    }
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(8.dp))
        }

        Spacer(Modifier.width(6.dp))
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun <Int> ChoiceChipsRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    var expandIconDegree by remember { mutableFloatStateOf(0f) }
    val rotationAnimation by animateFloatAsState(
        targetValue = expandIconDegree,
        animationSpec = tween(durationMillis = 400),
        label = "",
    )

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
    ) {
        var expanded by remember { mutableStateOf(false) }

        Column {
            val optionShape = RoundedCornerShape(18.dp)
            AssistChip(
                modifier = Modifier.then(
                    if (LocalEnableLiquidGlass.current) {
                        Modifier.liquidGlassSurface(
                            shape = optionShape,
                            pureBlack = false,
                            blurRadius = 3,
                            refraction = 0.035f,
                            curve = 0.075f,
                            dispersion = 0.001f,
                            edge = 0f,
                        )
                    } else Modifier
                ),
                onClick = {
                    expanded = !expanded
                    expandIconDegree -= 180
                },
                label = {
                    Text(
                        text =
                        when (selectedOption) {
                            OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                            OptionStats.MONTHS -> stringResource(id = R.string.months)
                            OptionStats.YEARS -> stringResource(id = R.string.years)
                            OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                        },
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer(rotationZ = rotationAnimation),
                    )
                },
                shape = optionShape,
                border = null,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (LocalEnableLiquidGlass.current) Color.Transparent else containerColor,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )

            AnimatedVisibility(
                visible = expanded,
                enter = expandIn() + fadeIn(),
                exit = shrinkOut() + fadeOut(),
            ) {
                DropdownMenu(
                    modifier = Modifier.padding(start = 12.dp),
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                        expandIconDegree -= 180
                    },
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(text = option.second) },
                            onClick = {
                                onSelectionChange(option.first)
                                expandIconDegree -= 180
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = selectedOption,
            transitionSpec = { slideInHorizontally() + fadeIn() togetherWith slideOutHorizontally() + fadeOut() },
            label = "",
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                chips.forEach { (value, label) ->
                    Spacer(Modifier.width(8.dp))

                    val chipShape = RoundedCornerShape(18.dp)
                    FilterChip(
                        label = { Text(label) },
                        selected = currentValue == value,
                        modifier = Modifier
                            .animateContentSize()
                            .then(
                                if (LocalEnableLiquidGlass.current) {
                                    Modifier.liquidGlassSurface(
                                        shape = chipShape,
                                        pureBlack = false,
                                        blurRadius = 3,
                                        refraction = 0.035f,
                                        curve = 0.075f,
                                        dispersion = 0.001f,
                                        edge = 0f,
                                    )
                                } else Modifier
                            ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (LocalEnableLiquidGlass.current) Color.Transparent else containerColor,
                        ),
                        onClick = { onValueUpdate(value) },
                        shape = chipShape,
                        border = null
                    )
                }
            }
        }
    }
}
