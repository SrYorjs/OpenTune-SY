package com.arturo254.opentune.ui.mochy.player.bedside

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.ui.player.preferences.GalaxPlayerStyleGallery
import com.arturo254.opentune.ui.player.state.PlayerState
import com.arturo254.opentune.ui.mochy.theme.MochyTheme
import com.arturo254.opentune.ui.mochy.player.MochyPlayerMode
import com.arturo254.opentune.ui.mochy.player.turntable.drawVinyl
import com.arturo254.opentune.ui.mochy.player.turntable.drawArmBase
import com.arturo254.opentune.ui.mochy.player.turntable.drawTonearm
import com.arturo254.opentune.ui.mochy.player.ipod.IpodBodyPaint
import com.arturo254.opentune.ui.mochy.player.ipod.deviceGeometry
import com.arturo254.opentune.ui.mochy.player.ipod.rememberIpodLook
import com.arturo254.opentune.ui.mochy.player.ipod.IpodTheme
import com.arturo254.opentune.ui.mochy.player.ipod.IpodWheelColor
import com.arturo254.opentune.ui.mochy.player.ipod.IpodThemeKey
import com.arturo254.opentune.ui.mochy.player.ipod.IpodWheelColorKey
import com.arturo254.opentune.utils.rememberEnumPreference
import androidx.compose.ui.graphics.drawscope.Stroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochyPlayerStyleSelector(
    state: PlayerState,
    native: PlayerDesignStyle,
    selectedMochy: MochyPlayerMode,
    onNative: (PlayerDesignStyle) -> Unit,
    onMochy: (MochyPlayerMode) -> Unit,
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
                    Text("Los diseños originales de Mochy y los 17 reproductores de GalaxOT.",
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
                        .clickable { onMochy(MochyPlayerMode.BEDSIDE) }
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
                    Text(if (selectedMochy == MochyPlayerMode.BEDSIDE) "Seleccionado" else "Toca para usar el reloj original de Mochy",
                        color = Color.White.copy(alpha = .65f),
                        style = MaterialTheme.typography.labelMedium)
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable { onMochy(MochyPlayerMode.TURNTABLE) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF09090A)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                            val u = size.minDimension
                            val x = (size.width - u) / 2f
                            val y = (size.height - u) / 2f
                            val origin = Offset(x + u * .162f, y + u * .703f)
                            drawArmBase(u)
                            drawVinyl(origin, u * .703f, .526f)
                            drawTonearm(Offset(x + u * .895f, y + u * .125f),
                                115f, u, silhouette = false)
                        }
                    }
                    Text("Mochy · Turntable",
                        color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text(if (selectedMochy == MochyPlayerMode.TURNTABLE)
                        "Seleccionado" else "Vinilo y brazo vectoriales originales de Mochy",
                        color = Color.White.copy(alpha = .65f),
                        style = MaterialTheme.typography.labelMedium)
                }
                val ipodLook = rememberIpodLook()
                val (ipodTheme, setIpodTheme) = rememberEnumPreference(
                    IpodThemeKey, IpodTheme.SILVER
                )
                val (ipodWheelColor, setIpodWheelColor) = rememberEnumPreference(
                    IpodWheelColorKey, IpodWheelColor.THEME
                )
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C1E))
                        .clickable { onMochy(MochyPlayerMode.IPOD) }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black),
                    ) {
                        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                            val geometry = deviceGeometry(size.width, size.height, 0f, 0f, false)
                            with(IpodBodyPaint(geometry, ipodLook)) {
                                draw(onBlack = false)
                            }
                            drawRoundRect(
                                color = ipodLook.screen.background,
                                topLeft = geometry.screen.topLeft,
                                size = geometry.screen.size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(geometry.screenCorner),
                            )
                            drawCircle(
                                color = ipodLook.wheel.face,
                                center = geometry.wheel.center,
                                radius = geometry.wheel.width * .5f,
                            )
                            drawCircle(
                                color = ipodLook.wheel.center,
                                center = geometry.wheel.center,
                                radius = geometry.wheel.width * .235f,
                            )
                            drawCircle(
                                color = ipodLook.wheel.edge,
                                center = geometry.wheel.center,
                                radius = geometry.wheel.width * .5f,
                                style = Stroke(1f),
                            )
                        }
                    }
                    Text(
                        "Mochy · iPod",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (selectedMochy == MochyPlayerMode.IPOD)
                            "Seleccionado"
                        else "Rueda táctil, carátula, paletas y geometría originales de Mochy",
                        color = Color.White.copy(alpha = .65f),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        "ACABADO",
                        color = Color.White.copy(alpha = .6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(IpodTheme.entries) { option ->
                            FilterChip(
                                selected = ipodTheme == option,
                                onClick = { setIpodTheme(option) },
                                label = {
                                    Text(
                                        when (option) {
                                            IpodTheme.SILVER -> "Plata"
                                            IpodTheme.BLACK -> "Negro"
                                            IpodTheme.U2 -> "U2"
                                            IpodTheme.TRUE_DARK -> "Oscuro"
                                        }
                                    )
                                },
                            )
                        }
                    }
                    Text(
                        "RUEDA",
                        color = Color.White.copy(alpha = .6f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(IpodWheelColor.entries) { option ->
                            FilterChip(
                                selected = ipodWheelColor == option,
                                onClick = { setIpodWheelColor(option) },
                                label = {
                                    Text(
                                        when (option) {
                                            IpodWheelColor.THEME -> "Automática"
                                            IpodWheelColor.WHITE -> "Blanca"
                                            IpodWheelColor.BLACK -> "Negra"
                                            IpodWheelColor.RED -> "Roja"
                                        }
                                    )
                                },
                            )
                        }
                    }
                }
                GalaxPlayerStyleGallery(
                    selected = native,
                    onSelected = onNative,
                )
            }
        }
    }
}
