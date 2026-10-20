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
fun StudioThemeV13(
    state: StudioSceneState,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
) {
    val cloud=Brush.verticalGradient(
        listOf(
            androidx.compose.ui.graphics.lerp(state.primary,Color(0xFF041D21),.76f),
            androidx.compose.ui.graphics.lerp(state.secondary,Color(0xFF101044),.82f),
            Color(0xFF04060D),
        ),
    )
    Box(
        Modifier.fillMaxSize().background(cloud)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal+WindowInsetsSides.Top)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..9) {
                val y=size.height*(.04f+i*.088f)
                val curtain=androidx.compose.ui.graphics.Path().apply {
                    moveTo(-size.width*.2f,y)
                    cubicTo(
                        size.width*.2f,y-size.height*.19f,
                        size.width*.60f,y+size.height*.18f,
                        size.width*1.2f,y-size.height*.15f,
                    )
                }
                drawPath(curtain,
                    if(i%2==0)state.primary.copy(alpha=.13f)else state.secondary.copy(alpha=.16f),
                    style=Stroke(width=(7+i*3).dp.toPx()))
            }
            for (i in 0..37) {
                val c=Offset((i*31%43)/43f*size.width,(i*13%39)/39f*size.height)
                drawCircle(Color.White.copy(alpha=.09f+(i%5)*.035f),
                    radius=(.3f+(i%3)*.4f).dp.toPx(),center=c)
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
                    Box(Modifier.size(side).clip(RoundedCornerShape(topStart=74.dp,topEnd=12.dp,bottomStart=12.dp,bottomEnd=74.dp))
                        .background(state.primary.copy(alpha=.18f)),content=artwork)
                }
                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) {
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart=35.dp,topEnd=35.dp,bottomStart=9.dp,bottomEnd=9.dp))
                        .galaxLiquidChrome(RoundedCornerShape(topStart=35.dp,topEnd=35.dp,bottomStart=9.dp,bottomEnd=9.dp),.66f).padding(vertical=8.dp)) { controls() }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(bottom=state.queuePeekHeight),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(10.dp),
            ) {
                BoxWithConstraints(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center) {
                    val side=min(maxWidth.value*.80f,maxHeight.value*.9f).dp
                    Box(Modifier.size(side).clip(RoundedCornerShape(topStart=74.dp,topEnd=12.dp,bottomStart=12.dp,bottomEnd=74.dp))
                        .background(state.secondary.copy(alpha=.23f)),content=artwork)
                }
                Box(Modifier.fillMaxWidth().padding(horizontal=10.dp)
                    .galaxLiquidChrome(RoundedCornerShape(topStart=35.dp,topEnd=35.dp,bottomStart=9.dp,bottomEnd=9.dp),.68f)
                    .padding(vertical=9.dp)) { controls() }
                Spacer(Modifier.size(12.dp))
            }
        }
    }
}
