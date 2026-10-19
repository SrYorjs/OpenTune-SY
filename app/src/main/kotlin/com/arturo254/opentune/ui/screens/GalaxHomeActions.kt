package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import com.arturo254.opentune.core.designsystem.theme.GalaxScreenStyle

@Composable
fun GalaxHomeActions(
    canShuffle: Boolean,
    isShuffling: Boolean = false,
    onShuffle: () -> Unit,
    onRecognition: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = GalaxScreenStyle.pagePadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GalaxHomeAction(
            text = stringResource(R.string.shuffle),
            icon = R.drawable.shuffle,
            primary = true,
            enabled = canShuffle,
            loading = isShuffling,
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
        )
        GalaxHomeAction(
            text = stringResource(R.string.music_recognition),
            icon = R.drawable.music_note,
            primary = false,
            enabled = true,
            onClick = onRecognition,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GalaxHomeAction(
    text: String,
    icon: Int,
    primary: Boolean,
    enabled: Boolean,
    loading: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = OpenTuneDesignSystem.galaxMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.pressSpec,
        label = "galaxHomeActionPress",
    )
    val pill = GalaxScreenStyle.pillShape
    val scheme = MaterialTheme.colorScheme
    val background = Brush.horizontalGradient(
        listOf(
            scheme.primary,
            if (primary) scheme.primary.copy(alpha = .76f) else scheme.surfaceContainerHigh,
        ),
    )
    val fg = if (primary) scheme.onPrimary else scheme.onSurface
    Box(
        modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else .48f
        }
            .height(56.dp)
            .clip(pill)
            .then(
                if (primary) Modifier.background(background)
                else Modifier.galaxLiquidChrome(pill, emphasis = .74f),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = fg,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = fg,
                )
            }
            Text(
                text = text,
                color = fg,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
