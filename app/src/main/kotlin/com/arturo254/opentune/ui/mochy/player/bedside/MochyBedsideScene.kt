package com.arturo254.opentune.ui.mochy.player.bedside

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.player.state.PlayerState

@Composable
fun MochyBedsideScene(
    state: PlayerState,
    queuePeekHeight: Dp,
    onClose: () -> Unit,
    onStyles: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time = rememberBedsideTime(live = true)
    val ink = remember { BedsideInk(Color.White) }
    val art = rememberAsyncImagePainter(model = state.artworkUrl)
    var face by remember { mutableStateOf(BedsideClockFace.BIG) }
    val safe = WindowInsets.systemBars.asPaddingValues()
    Column(
        modifier = modifier.fillMaxSize().background(Color.Black)
            .padding(top = safe.calculateTopPadding() + 8.dp)
            .padding(horizontal = 17.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = "Cerrar reproductor",
                    tint = Color.White,
                )
            }
            Text(
                text = "BEDSIDE · MOCHY",
                color = Color.White.copy(alpha = .75f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onStyles) {
                Text("Estilos", color = Color.White)
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clickable {
            val faces = BedsideClockFace.entries
            face = faces[(faces.indexOf(face) + 1) % faces.size]
        }) {
            AnimatedContent(
                targetState = face,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "Bedside clock face",
            ) { selected ->
                BedsideClockFaceView(selected, time, ink, art, Modifier.fillMaxSize())
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = .065f))
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = state.artworkUrl,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(13.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    state.title.orEmpty(), color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.mediaMetadata?.artists?.joinToString { it.name }.orEmpty(),
                    color = Color.White.copy(alpha = .56f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onPrevious, enabled = state.canSkipPrevious) {
                Icon(painterResource(R.drawable.skip_previous), "Anterior", tint = Color.White)
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    painterResource(if (state.isPlaying) R.drawable.pause else R.drawable.play),
                    if (state.isPlaying) "Pausar" else "Reproducir", tint = Color.White,
                )
            }
            IconButton(onClick = onNext, enabled = state.canSkipNext) {
                Icon(painterResource(R.drawable.skip_next), "Siguiente", tint = Color.White)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            TextButton(onClick = onLyrics) { Text("Letras", color = Color.White.copy(alpha = .64f)) }
            Spacer(Modifier.width(14.dp))
            TextButton(onClick = onQueue) { Text("Cola", color = Color.White.copy(alpha = .64f)) }
            Spacer(Modifier.width(14.dp))
            TextButton(onClick = {
                val faces = BedsideClockFace.entries
                face = faces[(faces.indexOf(face) + 1) % faces.size]
            }) { Text("Reloj", color = Color.White.copy(alpha = .64f)) }
        }
        Spacer(Modifier.height(queuePeekHeight.coerceAtLeast(0.dp) + 5.dp))
    }
}
