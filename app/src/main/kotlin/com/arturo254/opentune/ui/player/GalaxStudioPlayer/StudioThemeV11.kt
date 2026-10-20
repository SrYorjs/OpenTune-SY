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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.liquid.galaxLiquidChrome
import kotlin.math.min

@Composable
fun StudioThemeV11(
    state: StudioSceneState,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    val cloud=Brush.verticalGradient(
        listOf(
            androidx.compose.ui.graphics.lerp(state.primary,Color(0xFF021A1C),.76f),
            androidx.compose.ui.graphics.lerp(state.secondary,Color(0xFF05101A),.82f),
            Color(0xFF04060D),
        ),
    )
    Box(
        Modifier.fillMaxSize().background(cloud)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal+WindowInsetsSides.Top)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val spacing=24.dp.toPx()
            var x=0f
            while(x<size.width) {
                drawLine(state.primary.copy(alpha=.18f),
                    Offset(x,0f),Offset(x,size.height),1.dp.toPx())
                x+=spacing
            }
            var y=0f
            while(y<size.height) {
                drawLine(state.secondary.copy(alpha=.14f),
                    Offset(0f,y),Offset(size.width,y),.8.dp.toPx())
                y+=spacing
            }
            for(i in 0..9) {
                val yy=size.height*(.13f+i*.066f)
                drawLine(state.primary.copy(alpha=.24f),
                    Offset(0f,yy),Offset(size.width*.18f,yy),1.dp.toPx())
                drawLine(state.primary.copy(alpha=.18f),
                    Offset(size.width*.18f,yy),
                    Offset(size.width*.25f,yy+size.height*.05f),
                    1.dp.toPx())
                drawCircle(state.primary.copy(alpha=.63f),
                    radius=2.dp.toPx(),
                    center=Offset(size.width*.25f,yy+size.height*.05f))
            }
        }
        if(state.isLandscape) {
            Row(
                Modifier.fillMaxSize().padding(start=16.dp,end=16.dp,bottom=state.queuePeekHeight+9.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(10.dp),
            ) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(),contentAlignment=Alignment.Center) {
                    val side=min(maxWidth.value*.84f,maxHeight.value*.89f).dp
                    Box(Modifier.size(side).clip(RoundedCornerShape(2.dp))
                        .background(state.primary.copy(alpha=.18f)),content=artwork)
                }
                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                        .galaxLiquidChrome(RoundedCornerShape(3.dp),.66f).padding(vertical=8.dp)) { controls() }
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
                    Box(Modifier.size(side).clip(RoundedCornerShape(2.dp))
                        .background(state.secondary.copy(alpha=.23f)),content=artwork)
                }
                Box(Modifier.fillMaxWidth().padding(horizontal=10.dp)
                    .galaxLiquidChrome(RoundedCornerShape(3.dp),.68f)
                    .padding(vertical=9.dp)) { controls() }
                Spacer(Modifier.size(12.dp))
            }
        }
    }
}
