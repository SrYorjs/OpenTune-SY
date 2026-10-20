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
import androidx.compose.foundation.shape.CircleShape
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
fun StudioThemeV15(
    state: StudioSceneState,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(state.primary,Color(0xFF020612),.58f),
                        Color(0xFF0F062A),
                        Color(0xFF020612),
                    ),
                ),
            )
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal+WindowInsetsSides.Top),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c=Offset(size.width*.51f,size.height*.41f)
            for(i in 1..12) {
                val radius=size.width*(.10f+i*.068f)
                drawCircle(
                    if(i%2==0)state.primary.copy(alpha=.14f)
                    else state.secondary.copy(alpha=.18f),
                    radius=radius,center=c,
                    style=Stroke(width=(1+i%3).dp.toPx()),
                )
            }
            for(i in 0..29) {
                val x=size.width*(i+1)/31f
                val h=size.height*(.012f+(i*11%15)/270f)
                drawLine(state.primary.copy(alpha=.3f),
                    Offset(x,size.height*.8f-h),
                    Offset(x,size.height*.8f+h),2.dp.toPx())
            }
            val r=size.width*.38f
            drawArc(
                state.secondary.copy(alpha=.6f),
                startAngle=195f,
                sweepAngle=state.progress.coerceIn(0f,1f)*309f,
                useCenter=false,
                topLeft=Offset(c.x-r,c.y-r),
                size=Size(r*2,r*2),
                style=Stroke(width=2.dp.toPx()),
            )
        }
        if(state.isLandscape) {
            Row(
                Modifier.fillMaxSize().padding(
                    start=14.dp,end=14.dp,bottom=state.queuePeekHeight+14.dp,
                ),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp),
            ) {
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment=Alignment.Center,
                ) {
                    val diameter=min(maxWidth.value*.75f,maxHeight.value*.88f).dp
                    Box(
                        Modifier.size(diameter).clip(RoundedCornerShape(30.dp))
                            .background(state.primary.copy(alpha=.13f)),
                        contentAlignment=Alignment.Center,
                        content=artwork,
                    )
                }
                Box(
                    Modifier.weight(1f).galaxLiquidChrome(RoundedCornerShape(25.dp),.75f)
                        .padding(vertical=12.dp),
                ) { controls() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(bottom=state.queuePeekHeight),
                verticalArrangement=Arrangement.spacedBy(13.dp),
                horizontalAlignment=Alignment.CenterHorizontally,
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment=Alignment.Center,
                ) {
                    val diameter=min(maxWidth.value*.71f,maxHeight.value*.9f).dp
                    Box(
                        Modifier.size(diameter).clip(RoundedCornerShape(30.dp))
                            .background(state.secondary.copy(alpha=.15f)),
                        contentAlignment=Alignment.Center,
                        content=artwork,
                    )
                }
                Box(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp)
                        .galaxLiquidChrome(RoundedCornerShape(25.dp),.75f).padding(vertical=10.dp),
                ) { controls() }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}
