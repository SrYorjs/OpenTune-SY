package com.arturo254.opentune.ui.visual.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.R

@Composable
fun OriginalPlayerVolumeBar(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val held = animateFloatAsState(
        targetValue = if (dragged || pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "volumeHeld",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(28.dp).padding(horizontal = 42.dp),
    ) {
        Icon(
            painter = painterResource(if (volume <= 0f) R.drawable.volume_off else R.drawable.volume_up),
            contentDescription = "Silencio",
            tint = Color.White,
            modifier = Modifier.size(18.dp).speakerLight(held),
        )
        Slider(
            value = volume.coerceIn(0f, 1f),
            onValueChange = { onVolumeChange(it.coerceIn(0f, 1f)) },
            interactionSource = interaction,
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = {
                SeekBarTrack(
                    style = SeekBarStyle.DEFAULT,
                    fraction = volume.coerceIn(0f, 1f),
                    activeColor = Color.White,
                    inactiveColor = Color.White.copy(alpha = 0.24f),
                    animateWave = false,
                    active = dragged || pressed,
                )
            },
            modifier = Modifier.weight(1f).height(28.dp).padding(horizontal = 10.dp),
        )
        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = "Volumen",
            tint = Color.White,
            modifier = Modifier.size(18.dp).speakerLight(held),
        )
    }
}

private fun Modifier.speakerLight(held: State<Float>): Modifier = graphicsLayer {
    val h = held.value
    alpha = 0.7f + 0.3f * h.coerceIn(0f, 1f)
    val scale = 1f + 0.14f * h
    scaleX = scale
    scaleY = scale
    compositingStrategy = CompositingStrategy.ModulateAlpha
}
