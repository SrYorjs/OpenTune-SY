package com.arturo254.opentune.ui.mochy.player.bedside

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.preferences.GalaxPlayerStyleGallery
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.mochy.theme.MochyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochyPlayerStyleSelector(
    state: PlayerState,
    native: PlayerDesignStyle,
    bedsideEnabled: Boolean,
    onNative: (PlayerDesignStyle) -> Unit,
    onBedside: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Black,
        contentColor = Color.White,
    ) {
        MochyTheme {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(bottom = 35.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(
                    Modifier.padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("REPRODUCTORES", color = Color.White.copy(alpha = .65f),
                        style = MaterialTheme.typography.labelSmall)
                    Text("Elige tu universo", color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Text("El estilo original de Mochy y los 17 reproductores de GalaxOT.",
                        color = Color.White.copy(alpha = .53f),
                        style = MaterialTheme.typography.bodySmall)
                }
                val face = BedsideClockFace.BIG
                val time = rememberBedsideTime(live = false)
                val ink = remember { BedsideInk(Color.White) }
                val painter = rememberAsyncImagePainter(model = state.artworkUrl)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable(onClick = onBedside)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black),
                    ) {
                        BedsideClockFaceView(face, time, ink, painter)
                    }
                    Text("Mochy · Bedside Clock",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (bedsideEnabled) "Seleccionado" else "Toca para usar el reloj original de Mochy",
                        color = Color.White.copy(alpha = .65f),
                        style = MaterialTheme.typography.labelMedium)
                }
                GalaxPlayerStyleGallery(
                    selected = native,
                    onSelected = onNative,
                )
            }
        }
    }
}
