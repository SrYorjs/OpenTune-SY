package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StudioThemeV2(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..5) {
                val y = size.height*(.17f+i*.12f)
                drawLine(accent.copy(alpha=.11f),
                    Offset(size.width*.12f,y+size.height*.06f),
                    Offset(size.width*.88f,y-size.height*.06f),
                    1.dp.toPx())
            }
        }
        content()
    }
}
