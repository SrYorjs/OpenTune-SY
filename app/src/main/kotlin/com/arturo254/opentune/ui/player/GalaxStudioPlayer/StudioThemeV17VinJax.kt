package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.core.designsystem.OpenTuneDesignSystem
import com.arturo254.opentune.core.designsystem.motion.galaxMotion
import kotlin.math.min

@Composable
fun VinJaxPlayerScene(
    isLandscape: Boolean,
    isPlaying: Boolean,
    progress: Float,
    accent: Color,
    queuePeekHeight: Dp,
    artwork: @Composable BoxScope.() -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = OpenTuneDesignSystem.galaxMotion.reducedMotion
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isPlaying,reducedMotion) {
        while (isPlaying && !reducedMotion) {
            rotation.animateTo(rotation.value+360f,tween(1818,easing=LinearEasing))
            rotation.snapTo(rotation.value%360f)
        }
    }
    Box(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF121315),
                    androidx.compose.ui.graphics.lerp(accent,Color(0xFF101013),.88f),
                    Color(0xFF060607),
                ),
            ),
        ).windowInsetsPadding(
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal+WindowInsetsSides.Top),
        ),
    ) {
        if(isLandscape) {
            Row(
                Modifier.fillMaxSize().padding(
                    start=12.dp,end=12.dp,bottom=queuePeekHeight+12.dp,
                ),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(8.dp),
            ) {
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment=Alignment.Center,
                ) {
                    val size=min(maxWidth.value*.98f,maxHeight.value*.95f).dp
                    VinJaxTurntable(
                        accent,progress,isPlaying,rotation.value,
                        artwork,Modifier.size(size),
                    )
                }
                Box(Modifier.weight(1f),contentAlignment=Alignment.Center) { controls() }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(bottom=queuePeekHeight),
                horizontalAlignment=Alignment.CenterHorizontally,
                verticalArrangement=Arrangement.spacedBy(5.dp),
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment=Alignment.Center,
                ) {
                    val size=min(maxWidth.value*.98f,maxHeight.value*.94f).dp
                    VinJaxTurntable(
                        accent,progress,isPlaying,rotation.value,
                        artwork,Modifier.size(size),
                    )
                }
                Box(
                    Modifier.fillMaxWidth().padding(horizontal=9.dp,vertical=5.dp),
                    contentAlignment=Alignment.Center,
                ) { controls() }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

@Composable
fun VinJaxTurntable(
    accent: Color,
    progress: Float,
    isPlaying: Boolean,
    rotation: Float,
    artwork: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val needlePosition by animateFloatAsState(
        progress.coerceIn(0f,1f),
        animationSpec=spring(stiffness=400f),
        label="vinJaxNeedleProgress",
    )
    val lifted by animateFloatAsState(
        if(isPlaying) 0f else 1f,
        animationSpec=spring(stiffness=360f),
        label="vinJaxNeedleLift",
    )
    Box(modifier,contentAlignment=Alignment.Center) {
        Box(
            Modifier.fillMaxSize(.81f)
                .clip(CircleShape)
                .graphicsLayer { rotationZ=rotation },
            contentAlignment=Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF26272A),
                            Color(0xFF070708),
                            Color(0xFF151517),
                            Color(0xFF030304),
                        ),
                    ),
                )
                val r=size.minDimension*.49f
                for (i in 1..46) {
                    drawCircle(
                        if(i%4==0)Color.White.copy(alpha=.17f)
                        else Color.White.copy(alpha=.038f),
                        radius=r*(.39f+i*.013f),
                        style=Stroke(width=.62.dp.toPx()),
                    )
                }
                drawCircle(accent.copy(alpha=.46f),
                    radius=r*.985f,style=Stroke(width=1.dp.toPx()))
            }
            Box(
                Modifier.fillMaxSize(.43f).align(Alignment.Center)
                    .clip(CircleShape),
                contentAlignment=Alignment.Center,
                content=artwork,
            )
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha=.77f),
                    radius=2.dp.toPx(),center=center)
                drawCircle(Color.Black.copy(alpha=.42f),
                    radius=5.dp.toPx(),center=center,
                    style=Stroke(width=1.dp.toPx()))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val radius=size.minDimension*.405f
            val c=center
            val pivot=Offset(size.width*.92f,size.height*.075f)
            val needle=Offset(
                c.x+radius*(.88f-needlePosition*.43f)+lifted*3.dp.toPx(),
                c.y-radius*(.26f+needlePosition*.08f)-lifted*9.dp.toPx(),
            )
            val elbow=Offset(size.width*.91f,size.height*.265f-lifted*5.dp.toPx())
            val path=Path().apply {
                moveTo(pivot.x,pivot.y)
                lineTo(elbow.x,elbow.y)
                lineTo(needle.x,needle.y)
            }
            drawPath(path,Color.Black.copy(alpha=.92f),
                style=Stroke(width=9.dp.toPx()))
            drawPath(path,Color(0xFFCCD0D6),
                style=Stroke(width=5.dp.toPx()))
            drawCircle(Color(0xFF34363A),
                radius=18.dp.toPx(),center=pivot)
            drawCircle(Color(0xFFDCE0E5),
                radius=11.dp.toPx(),center=pivot,
                style=Stroke(width=3.dp.toPx()))
            drawCircle(accent,radius=3.5.dp.toPx(),center=needle)
            drawLine(Color(0xFFEFEDF0),needle,
                Offset(needle.x-2.dp.toPx(),needle.y+5.dp.toPx()),
                strokeWidth=1.dp.toPx())
        }
    }
}
