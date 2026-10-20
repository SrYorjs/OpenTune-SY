package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun StudioThemeV6(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            for(i in 0..7) {
                val c=Offset(
                    size.width*(.10f+i*.11f),
                    size.height*(.13f+(i%4)*.12f),
                )
                val r=size.width*(.10f+i*.022f)
                drawCircle(
                    Brush.radialGradient(
                        listOf(
                            accent.copy(alpha=.14f+(i%3)*.04f),
                            Color.Transparent,
                        ),
                        center=c,
                        radius=r,
                    ),
                    radius=r,
                    center=c,
                )
                drawCircle(
                    accent.copy(alpha=.18f),
                    radius=r*.66f,
                    center=c,
                    style=Stroke(width=1.dp.toPx()),
                )
            }
            for(i in 0..8) {
                val y=size.height*(.10f+i*.092f)
                drawLine(
                    accent.copy(alpha=.075f),
                    Offset(size.width*.06f,y),
                    Offset(size.width*.91f,y+size.height*.09f),
                    1.dp.toPx(),
                )
            }
        }
        content()
    }
}
