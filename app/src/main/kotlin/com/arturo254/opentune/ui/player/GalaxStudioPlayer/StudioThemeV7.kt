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
fun StudioThemeV7(content: @Composable () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val center=Offset(size.width*.51f,size.height*.33f)
            for(i in 0..9) {
                val r=size.width*(.19f+i*.071f)
                drawArc(
                    accent.copy(alpha=.085f+(i%3)*.019f),
                    startAngle=-86f+i*23f,
                    sweepAngle=113f+i*7f,
                    useCenter=false,
                    topLeft=Offset(center.x-r,center.y-r),
                    size=Size(r*2,r*2),
                    style=Stroke(width=1.dp.toPx()),
                )
            }
            for(i in 0..5) {
                val xx=size.width*(.11f+i*.14f)
                drawLine(
                    accent.copy(alpha=.11f),
                    Offset(xx,size.height*.08f),
                    Offset(xx+size.width*.06f,size.height*.22f),
                    1.dp.toPx(),
                )
            }
        }
        content()
    }
}
