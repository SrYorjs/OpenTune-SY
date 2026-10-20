package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StudioThemeV5(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            for(i in 0..7) {
                val y=size.height*(.10f+i*.108f)
                drawLine(
                    accent.copy(alpha=.10f),
                    Offset(size.width*.74f,y),
                    Offset(size.width*.94f,y),
                    (1+i%2).dp.toPx(),
                )
            }
            drawRoundRect(
                accent.copy(alpha=.12f),
                topLeft=Offset(size.width*.68f,size.height*.21f),
                size=Size(size.width*.26f,size.height*.22f),
                cornerRadius=androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
                style=Stroke(width=1.dp.toPx()),
            )
            for(i in 0..4) {
                val x=size.width*(.73f+i*.043f)
                val high=size.height*(.05f+i*.014f)
                drawLine(accent.copy(alpha=.14f),
                    Offset(x,size.height*.29f-high),
                    Offset(x,size.height*.29f+high),
                    1.dp.toPx())
            }
        }
        content()
    }
}
