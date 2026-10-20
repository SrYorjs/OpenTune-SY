package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GalaxLibraryChapter(
    number: String,
    title: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette=MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().padding(horizontal=17.dp,vertical=11.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        palette.primary.copy(alpha=.15f),
                        palette.surfaceContainerLow,
                        palette.surfaceContainerLow,
                    ),
                ),
            )
            .clickable(role=Role.Button,onClick=onOpen)
            .padding(horizontal=17.dp,vertical=17.dp),
        verticalAlignment=Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(15.dp),
    ) {
        Box(
            Modifier.size(51.dp).clip(CircleShape)
                .background(palette.primary.copy(alpha=.19f)),
            contentAlignment=Alignment.Center,
        ) {
            Canvas(Modifier.matchParentSize()) {
                for(i in 0..3) {
                    drawCircle(
                        palette.primary.copy(alpha=.16f),
                        radius=size.minDimension*(.22f+i*.11f),
                        style=Stroke(width=.8.dp.toPx()),
                    )
                }
                drawArc(palette.tertiary.copy(alpha=.71f),
                    startAngle=-80f,sweepAngle=127f,useCenter=false,
                    topLeft=Offset(size.width*.1f,size.height*.1f),
                    size=androidx.compose.ui.geometry.Size(size.width*.8f,size.height*.8f),
                    style=Stroke(width=1.2.dp.toPx()))
            }
            Text(number,color=palette.primary,style=MaterialTheme.typography.labelMedium,
                fontWeight=FontWeight.Bold)
        }
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
            Text("BIBLIOTECA  /  $number",style=MaterialTheme.typography.labelSmall,
                color=palette.primary,letterSpacing=1.25.sp,fontWeight=FontWeight.Bold)
            Text(title,style=MaterialTheme.typography.titleLarge,
                color=palette.onSurface,fontWeight=FontWeight.Bold)
        }
        Box(
            Modifier.size(36.dp).clip(CircleShape)
                .background(palette.primary.copy(alpha=.15f)),
            contentAlignment=Alignment.Center,
        ) {
            Text("→",color=palette.primary,style=MaterialTheme.typography.titleLarge)
        }
    }
}
