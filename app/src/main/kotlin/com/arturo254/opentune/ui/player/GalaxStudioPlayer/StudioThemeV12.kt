package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import kotlin.math.min

@Composable
fun StudioThemeV12(
    state: StudioSceneState,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    val cloud=Brush.verticalGradient(
        listOf(
            androidx.compose.ui.graphics.lerp(state.primary,Color(0xFF28160A),.76f),
            androidx.compose.ui.graphics.lerp(state.secondary,Color(0xFF130C08),.82f),
            Color(0xFF04060D),
        ),
    )
    Box(
        Modifier.fillMaxSize().background(cloud)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal+WindowInsetsSides.Top)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val warm = Color(0xFFDBAD79)
            for (i in 0..8) {
                val y=size.height*(.08f+i*.115f)
                drawLine(warm.copy(alpha=.045f),
                    Offset(0f,y),Offset(size.width,y),2.dp.toPx())
            }
            for (i in 0..1) {
                val c=Offset(size.width*(if(i==0).27f else .73f),size.height*.32f)
                drawCircle(warm.copy(alpha=.14f),radius=size.width*.24f,
                    center=c,style=Stroke(width=5.dp.toPx()))
                for (ring in 0..4) {
                    drawCircle(warm.copy(alpha=.11f),
                        radius=size.width*(.07f+ring*.028f),center=c,
                        style=Stroke(width=1.dp.toPx()))
                }
            }
            drawRect(Color.Black.copy(alpha=.34f),
                topLeft=Offset(size.width*.15f,size.height*.73f),
                size=Size(size.width*.70f,size.height*.08f))
            drawLine(warm.copy(alpha=.42f),
                Offset(size.width*.18f,size.height*.77f),
                Offset(size.width*.82f,size.height*.77f),1.dp.toPx())
        }
        if(state.isLandscape) {
            Row(
                Modifier.fillMaxSize().padding(start=16.dp,end=16.dp,bottom=state.queuePeekHeight+9.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp),
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.Center) {
                    val side=min(maxWidth.value*.84f,maxHeight.value*.89f).dp
                    Box(Modifier.size(side).clip(RoundedCornerShape(18.dp))
                        .background(state.primary.copy(alpha=.18f)),content=artwork)
                }
                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp))
                        .galaxLiquidChrome(RoundedCornerShape(19.dp),.66f).padding(vertical=8.dp)) { controls() }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(bottom=state.queuePeekHeight),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(10.dp),
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                    val side=min(maxWidth.value*.85f,maxHeight.value*.9f).dp
                    Box(Modifier.size(side).clip(RoundedCornerShape(18.dp))
                        .background(state.secondary.copy(alpha=.23f)),content=artwork)
                }
                Box(Modifier.fillMaxWidth().padding(horizontal=10.dp)
                    .galaxLiquidChrome(RoundedCornerShape(19.dp),.68f)
                    .padding(vertical=9.dp)) { controls() }
                Spacer(Modifier.size(12.dp))
            }
        }
    }
}
