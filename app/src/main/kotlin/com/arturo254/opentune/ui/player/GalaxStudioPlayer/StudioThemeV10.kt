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
fun StudioThemeV10(
    state: StudioSceneState,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        androidx.compose.ui.graphics.lerp(state.primary,Color.Black,.58f),
                        Color(0xFF050712),
                        Color.Black,
                    ),
                ),
            )
            .windowInsetsPadding(
                WindowInsets.systemBars.only(WindowInsetsSides.Horizontal+WindowInsetsSides.Top),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c=Offset(size.width*.49f,size.height*.40f)
            for(i in 0..12) {
                val r=size.width*(.17f+i*.059f)
                drawArc(
                    color=if(i%2==0)state.primary.copy(alpha=.33f)else state.secondary.copy(alpha=.27f),
                    startAngle=-80f+i*32f,
                    sweepAngle=70f+i*5f,
                    useCenter=false,
                    topLeft=Offset(c.x-r,c.y-r*.85f),
                    size=Size(r*2,r*1.7f),
                    style=Stroke(width=(1f+i*.12f).dp.toPx()),
                )
            }
            for(i in 0..9) {
                val r=size.width*(.24f+i*.049f)
                drawCircle(
                    Color.White.copy(alpha=.025f),
                    radius=r,center=c,style=Stroke(width=.8.dp.toPx()),
                )
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.Black,Color.Black,Color.Transparent),
                    center=c,radius=size.width*.24f,
                ),
                radius=size.width*.24f,center=c,
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
                        Modifier.size(diameter).clip(CircleShape)
                            .background(state.primary.copy(alpha=.13f)),
                        contentAlignment=Alignment.Center,
                        content=artwork,
                    )
                }
                Box(
                    Modifier.weight(1f).galaxLiquidChrome(CircleShape,.4f)
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
                        Modifier.size(diameter).clip(CircleShape)
                            .background(state.secondary.copy(alpha=.15f)),
                        contentAlignment=Alignment.Center,
                        content=artwork,
                    )
                }
                Box(
                    Modifier.fillMaxWidth().padding(horizontal=16.dp)
                        .galaxLiquidChrome(CircleShape,.35f).padding(vertical=10.dp),
                ) { controls() }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}
