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
            for(i in 0..7) {
                val c=Offset(size.width*(.14f+i*.125f),size.height*(.16f+(i%3)*.21f))
                drawCircle(
                    Brush.radialGradient(
                        listOf(if(i%2==0)state.primary.copy(alpha=.26f)else state.secondary.copy(alpha=.24f),Color.Transparent),
                        center=c,radius=size.width*.55f,
                    ),
                    radius=size.width*.55f,center=c,
                )
            }
            for(i in 0..67) {
                val at=Offset((i*73%101)/101f*size.width,(i*37%97)/97f*size.height)
                drawCircle(Color.White.copy(alpha=.13f+(i%4)*.05f),
                    radius=(.5f+(i%3)*.3f).dp.toPx(),center=at)
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
