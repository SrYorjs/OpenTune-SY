package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R

enum class GalaxLibraryDestination {
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
}

@Composable
fun GalaxLibraryDestinationHero(
    destination: GalaxLibraryDestination,
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val palette = MaterialTheme.colorScheme
    val mainColor = when(destination) {
        GalaxLibraryDestination.ALBUMS -> palette.primary
        GalaxLibraryDestination.ARTISTS -> palette.tertiary
        GalaxLibraryDestination.PLAYLISTS -> palette.secondary
    }
    val icon = when(destination) {
        GalaxLibraryDestination.ALBUMS -> R.drawable.album
        GalaxLibraryDestination.ARTISTS -> R.drawable.person
        GalaxLibraryDestination.PLAYLISTS -> R.drawable.queue_music
    }
    val number = when(destination) {
        GalaxLibraryDestination.ALBUMS -> "01"
        GalaxLibraryDestination.ARTISTS -> "02"
        GalaxLibraryDestination.PLAYLISTS -> "03"
    }
    Box(
        modifier = modifier.fillMaxWidth()
            .padding(horizontal=14.dp,vertical=13.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        mainColor.copy(alpha=.20f),
                        palette.surfaceContainerLow,
                        palette.surfaceContainerLow,
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val right=Offset(size.width*.86f,size.height*.22f)
            when(destination) {
                GalaxLibraryDestination.ALBUMS -> {
                    for (i in 0..10) {
                        val r=size.width*(.12f+i*.043f)
                        drawCircle(
                            if(i%2==0)mainColor.copy(alpha=.18f)
                            else palette.tertiary.copy(alpha=.12f),
                            radius=r,center=right,
                            style=Stroke(width=1.dp.toPx()),
                        )
                    }
                    val r=size.width*.28f
                    drawArc(
                        mainColor.copy(alpha=.63f),
                        startAngle=-78f,sweepAngle=97f,useCenter=false,
                        topLeft=Offset(right.x-r,right.y-r),
                        size=Size(r*2,r*2),
                        style=Stroke(width=2.dp.toPx()),
                    )
                    drawCircle(
                        Brush.radialGradient(
                            listOf(mainColor.copy(alpha=.19f),Color.Transparent),
                            center=right,radius=size.width*.47f,
                        ),
                        radius=size.width*.47f,center=right,
                    )
                }
                GalaxLibraryDestination.ARTISTS -> {
                    val nodes=listOf(
                        Offset(size.width*.60f,size.height*.17f),
                        Offset(size.width*.87f,size.height*.10f),
                        Offset(size.width*.94f,size.height*.39f),
                        Offset(size.width*.72f,size.height*.59f),
                        Offset(size.width*.55f,size.height*.43f),
                        Offset(size.width*.87f,size.height*.76f),
                    )
                    for(i in nodes.indices) {
                        val next=nodes[(i+1)%nodes.size]
                        drawLine(mainColor.copy(alpha=.28f),
                            nodes[i],next,1.dp.toPx())
                        drawCircle(
                            Color.White.copy(alpha=.51f),
                            radius=(2f+(i%3)).dp.toPx(),
                            center=nodes[i],
                        )
                        drawCircle(
                            mainColor.copy(alpha=.11f),
                            radius=(9+i%2*5).dp.toPx(),
                            center=nodes[i],
                        )
                    }
                    for(i in 0..28) {
                        val c=Offset(
                            (i*37%41)/41f*size.width,
                            (i*17%31)/31f*size.height,
                        )
                        drawCircle(
                            Color.White.copy(alpha=.13f),
                            radius=.65.dp.toPx(),center=c,
                        )
                    }
                }
                GalaxLibraryDestination.PLAYLISTS -> {
                    for(i in 0..4) {
                        val x=size.width*(.55f+i*.047f)
                        val y=size.height*(.12f+i*.085f)
                        drawRoundRect(
                            mainColor.copy(alpha=.09f+i*.035f),
                            topLeft=Offset(x,y),
                            size=Size(size.width*.29f,size.height*.52f),
                            cornerRadius=CornerRadius(16.dp.toPx()),
                            style=Stroke(width=1.dp.toPx()),
                        )
                    }
                    for(i in 0..6) {
                        val x=size.width*(.69f+i*.048f)
                        val y=size.height*(.14f+(i%4)*.14f)
                        drawLine(palette.tertiary.copy(alpha=.21f),
                            Offset(x,y),Offset(x,y+size.height*.12f),
                            2.dp.toPx())
                    }
                    drawArc(
                        mainColor.copy(alpha=.54f),
                        startAngle=190f,sweepAngle=103f,useCenter=false,
                        topLeft=Offset(right.x-size.width*.25f,right.y-size.width*.25f),
                        size=Size(size.width*.50f,size.width*.50f),
                        style=Stroke(width=2.dp.toPx()),
                    )
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal=23.dp,vertical=23.dp),
            verticalArrangement=Arrangement.spacedBy(15.dp),
        ) {
            Row(
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(43.dp)
                        .clip(CircleShape)
                        .background(mainColor.copy(alpha=.19f)),
                    contentAlignment=Alignment.Center,
                ) {
                    Icon(painterResource(icon),contentDescription=null,
                        tint=mainColor,modifier=Modifier.size(23.dp))
                }
                Text(
                    "GALAXOT  /  $number",
                    style=MaterialTheme.typography.labelSmall,
                    color=mainColor,
                    fontWeight=FontWeight.Bold,
                    letterSpacing=1.4.sp,
                )
            }
            Text(
                text=title,
                style=MaterialTheme.typography.headlineLarge,
                color=palette.onSurface,
                fontWeight=FontWeight.Black,
                maxLines=2,
                letterSpacing=(-.95).sp,
                overflow=TextOverflow.Ellipsis,
            )
            Box(
                Modifier.fillMaxWidth(.23f)
                    .height(2.dp)
                    .background(mainColor.copy(alpha=.73f),CircleShape),
            )
            Row(
                modifier=Modifier.fillMaxWidth(),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(19.dp))
                        .background(mainColor.copy(alpha=.15f))
                        .padding(horizontal=14.dp,vertical=9.dp),
                ) {
                    Text(
                        count.toString(),
                        style=MaterialTheme.typography.titleMedium,
                        fontWeight=FontWeight.Bold,
                        color=mainColor,
                    )
                }
                Row(
                    horizontalArrangement=Arrangement.spacedBy(7.dp),
                    verticalAlignment=Alignment.CenterVertically,
                ) {
                    for(i in 0..3) {
                        Box(
                            Modifier.size(if(i==0)7.dp else 4.dp)
                                .background(
                                    mainColor.copy(alpha=if(i==0).77f else .32f),
                                    CircleShape,
                                ),
                        )
                    }
                }
            }
        }
    }
}
