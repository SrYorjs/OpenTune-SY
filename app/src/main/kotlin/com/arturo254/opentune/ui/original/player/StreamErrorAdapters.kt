package com.arturo254.opentune.ui.original.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.arturo254.opentune.ui.original.adapters.LocalOriginalPlayerConnection

val StreamFailureRed = Color(0xFFFF4D4F)

@Composable
fun rememberStreamFailureProgress(): State<Float> {
    val connection = LocalOriginalPlayerConnection.current
    val error = connection?.error?.collectAsState()
    return remember(error) { derivedStateOf { if (error?.value == null) -1f else 0f } }
}

@Composable
fun rememberStreamFailureLabel(progress: State<Float>): State<String?> {
    val error = LocalOriginalPlayerConnection.current?.error?.collectAsState()
    return remember(error) { derivedStateOf { error?.value?.errorCodeName } }
}

fun androidx.compose.ui.Modifier.streamFailureOverlay(progress: androidx.compose.runtime.State<Float>): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val p = progress.value
        if (p > 0f) {
            val w = size.width * p
            drawRect(
                color = StreamFailureRed,
                topLeft = androidx.compose.ui.geometry.Offset(size.width - w, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
                blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop,
            )
        }
    }
