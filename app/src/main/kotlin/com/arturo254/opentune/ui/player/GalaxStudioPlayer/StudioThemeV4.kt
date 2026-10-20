package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StudioThemeV4(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Color.Black.copy(alpha=.19f),
                Offset(0f,0f),
                Size(size.width,size.height*.052f),
            )
            drawRect(
                Color.Black.copy(alpha=.19f),
                Offset(0f,size.height*.95f),
                Size(size.width,size.height*.05f),
            )
            for(i in 0..6) {
                val x=size.width*(.13f+i*.12f)
                drawLine(
                    accent.copy(alpha=.14f),
                    Offset(x,size.height*.05f),
                    Offset(x,size.height*.13f),
                    1.dp.toPx(),
                )
                drawLine(
                    accent.copy(alpha=.12f),
                    Offset(x,size.height*.88f),
                    Offset(x,size.height*.95f),
                    1.dp.toPx(),
                )
            }
            for(i in 0..3) {
                val y=size.height*(.23f+i*.18f)
                drawLine(
                    accent.copy(alpha=.075f),
                    Offset(0f,y),
                    Offset(size.width,y),
                    1.dp.toPx(),
                )
            }
        }
        content()
    }
}
