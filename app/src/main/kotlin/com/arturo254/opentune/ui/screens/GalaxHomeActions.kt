package com.arturo254.opentune.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.arturo254.opentune.core.designsystem.theme.GalaxVisualKit

@Composable
fun GalaxHomeActions(
    canShuffle: Boolean,
    isShuffling: Boolean = false,
    onShuffle: () -> Unit,
    onRecognition: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(horizontal = GalaxScreenStyle.pagePadding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GalaxHomeAction(
            text = stringResource(R.string.shuffle),
            icon = R.drawable.shuffle,
            primary = true,
            enabled = canShuffle,
            loading = isShuffling,
            onClick = onShuffle,
            modifier = Modifier.weight(1.15f),
        )
        GalaxHomeAction(
            text = stringResource(R.string.music_recognition),
            icon = R.drawable.music_note,
            primary = false,
            enabled = true,
            onClick = onRecognition,
            modifier = Modifier.weight(.85f),
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
    val scheme = MaterialTheme.colorScheme
    val shape = GalaxVisualKit.tileShape
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = OpenTuneDesignSystem.galaxMotion
    val scale by animateFloatAsState(
        targetValue = if (pressed) motion.pressScale else 1f,
        animationSpec = motion.pressSpec,
        label = "galaxHomeActionPress",
    )
    val background = Brush.linearGradient(
        listOf(scheme.primary, scheme.primary.copy(alpha = .72f)),
    )
    val foreground = if (primary) scheme.onPrimary else scheme.onSurface
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else .42f
        }
            .height(94.dp)
            .clip(shape)
            .then(
                if (primary) Modifier.background(background)
                else Modifier.galaxLiquidChrome(shape, emphasis = .68f),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Box(
                modifier = Modifier.size(29.dp)
                    .clip(CircleShape)
                    .background(
                        if (primary) Color.White.copy(alpha = .17f)
                        else scheme.primary.copy(alpha = .17f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(17.dp),
                        color = foreground,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                        tint = foreground,
                    )
                }
            }
            Text(
                text = text,
                color = foreground,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
