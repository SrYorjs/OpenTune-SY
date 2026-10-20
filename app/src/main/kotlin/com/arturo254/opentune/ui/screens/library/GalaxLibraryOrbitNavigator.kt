package com.arturo254.opentune.ui.screens.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.LibraryFilter

@Composable
fun GalaxLibraryOrbitNavigator(
    playlists: Int,
    albums: Int,
    artists: Int,
    onOpen: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal=17.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.spacedBy(12.dp),
        ) {
            GalaxLibraryOrbitTile(
                title=stringResource(R.string.filter_playlists),
                index="01",
                count=playlists,
                kind=0,
                icon=R.drawable.queue_music,
                onClick={onOpen(LibraryFilter.PLAYLISTS)},
                modifier=Modifier.weight(1f),
            )
            GalaxLibraryOrbitTile(
                title=stringResource(R.string.filter_albums),
                index="02",
                count=albums,
                kind=1,
                icon=R.drawable.album,
                onClick={onOpen(LibraryFilter.ALBUMS)},
                modifier=Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement=Arrangement.spacedBy(12.dp),
        ) {
            GalaxLibraryOrbitTile(
                title=stringResource(R.string.filter_artists),
                index="03",
                count=artists,
                kind=2,
                icon=R.drawable.person,
                onClick={onOpen(LibraryFilter.ARTISTS)},
                modifier=Modifier.weight(1f),
            )
            GalaxLibraryOrbitTile(
                title=stringResource(R.string.filter_songs),
                index="04",
                count=null,
                kind=3,
                icon=R.drawable.music_note,
                onClick={onOpen(LibraryFilter.SONGS)},
                modifier=Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GalaxLibraryOrbitTile(
    title: String,
    index: String,
    count: Int?,
    kind: Int,
    icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme=MaterialTheme.colorScheme
    val primary=when(kind) {
        0->scheme.primary
        1->scheme.tertiary
        2->scheme.secondary
        else->scheme.primary
    }
    Box(
        modifier.height(162.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha=.22f),
                        scheme.surfaceContainerLow,
                    ),
                ),
            )
            .clickable(role=Role.Button,onClick=onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val c=Offset(size.width*.83f,size.height*.23f)
            for(i in 0..6) {
                val radius=size.width*(.12f+i*.085f)
                val color=if(kind==2)scheme.tertiary else primary
                if(kind==1 || kind==3) {
                    drawCircle(color.copy(alpha=.13f),radius,c,
                        style=Stroke(width=1.dp.toPx()))
                } else if(kind==0) {
                    drawRoundRect(
                        color.copy(alpha=.10f),
                        topLeft=Offset(c.x-radius,c.y-radius*.48f),
                        size=Size(radius*1.55f,radius*.95f),
                        cornerRadius=androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                        style=Stroke(width=1.dp.toPx()),
                    )
                } else {
                    val x=size.width*(.45f+i*.072f)
                    drawLine(color.copy(alpha=.2f),
                        Offset(x,size.height*.12f),
                        Offset(x,size.height*(.27f+(i%3)*.09f)),
                        1.3.dp.toPx())
                }
            }
            drawArc(
                primary.copy(alpha=.53f),
                startAngle=-80f,sweepAngle=102f,useCenter=false,
                topLeft=Offset(c.x-size.width*.24f,c.y-size.width*.24f),
                size=Size(size.width*.48f,size.width*.48f),
                style=Stroke(width=1.5.dp.toPx()),
            )
        }
        Column(
            Modifier.align(Alignment.BottomStart)
                .padding(horizontal=16.dp,vertical=17.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp),
            ) {
                Icon(painterResource(icon),contentDescription=null,
                    tint=primary,modifier=Modifier.size(17.dp))
                Text(index,color=scheme.onSurfaceVariant,
                    style=MaterialTheme.typography.labelSmall,
                    fontWeight=FontWeight.Bold)
            }
            Text(title,color=scheme.onSurface,
                fontWeight=FontWeight.Bold,
                style=MaterialTheme.typography.titleMedium,
                maxLines=1,overflow=TextOverflow.Ellipsis)
            if(count!=null) {
                Text("$count",color=primary,
                    style=MaterialTheme.typography.labelLarge,
                    fontWeight=FontWeight.Bold)
            }
        }
    }
}
