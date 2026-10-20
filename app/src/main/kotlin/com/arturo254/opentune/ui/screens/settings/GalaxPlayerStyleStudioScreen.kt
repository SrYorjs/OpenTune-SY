package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.PlayerDesignStyle
import com.arturo254.opentune.constants.PlayerDesignStyleKey
import com.arturo254.opentune.core.designsystem.theme.galaxScreenBackground
import com.arturo254.opentune.ui.player.preferences.GalaxPlayerStyleGallery
import com.arturo254.opentune.ui.player.preferences.playerLayoutLabel
import com.arturo254.opentune.ui.player.v9.originalPlayerStyleConfig
import com.arturo254.opentune.utils.rememberEnumPreference

private data class StudioIdentity(val category: String, val description: String, val emblem: String)

private fun studioIdentity(style: PlayerDesignStyle): StudioIdentity = when (style) {
    PlayerDesignStyle.V1 -> StudioIdentity("HERITAGE", "Controles esenciales, portada protagonista y jerarquía tradicional", "01")
    PlayerDesignStyle.V2 -> StudioIdentity("MODERN", "Capas minimalistas, ritmos geométricos y espacios abiertos", "02")
    PlayerDesignStyle.V3 -> StudioIdentity("MINIMAL", "Tipografía contenida, máxima claridad y casi ningún adorno", "03")
    PlayerDesignStyle.V4 -> StudioIdentity("CINEMA", "Escenario panorámico con luces que abrazan la carátula", "04")
    PlayerDesignStyle.V5 -> StudioIdentity("COMPACT", "Toda la música en una interfaz pequeña y directa", "05")
    PlayerDesignStyle.V6 -> StudioIdentity("EXPRESSIVE", "Formas vivas, tarjetas orgánicas y acentos intensos", "06")
    PlayerDesignStyle.V7 -> StudioIdentity("IMMERSIVE", "Fotografía dramática y atmósfera envolvente", "07")
    PlayerDesignStyle.V8 -> StudioIdentity("APPLE MUSIC", "Portada cuadrada, controles discretos y fondo cromático suave", "08")
    PlayerDesignStyle.V9 -> StudioIdentity("NEBULA", "Materia estelar y nubes de color de la portada", "09")
    PlayerDesignStyle.V10 -> StudioIdentity("STELLAR", "Trayectorias de satélites y anillos astrales", "10")
    PlayerDesignStyle.V11 -> StudioIdentity("CYBER", "Circuitos y cuadrículas de neón", "11")
    PlayerDesignStyle.V12 -> StudioIdentity("ANALOG", "Surcos y resonancias de un disco orbital", "12")
    PlayerDesignStyle.V13 -> StudioIdentity("AURORA", "Bandas luminosas como una aurora boreal", "13")
    PlayerDesignStyle.V14 -> StudioIdentity("MONOLITH", "Arquitectura brutalista, líneas precisas y tipografía fuerte", "14")
    PlayerDesignStyle.V15 -> StudioIdentity("QUANTUM", "Impulsos, ondas y un núcleo de energía", "15")
    PlayerDesignStyle.V16 -> StudioIdentity("GALAXTHEME", "El estilo galáctico animado que antes ocupaba Apple Music", "16")
    PlayerDesignStyle.V17 -> StudioIdentity("VINJAX", "Tocadiscos con disco giratorio, etiqueta circular y brazo de aguja móvil", "17")
}

@Composable
fun GalaxPlayerStyleStudioScreen(navController: NavController) {
    val (selected, onSelected) = rememberEnumPreference(
        PlayerDesignStyleKey,
        PlayerDesignStyle.V8,
    )
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().galaxScreenBackground(),
        contentPadding = PaddingValues(bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp),
    ) {
        item("studio_back") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 18.dp, end = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(
                    Modifier.size(46.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh)
                        .clickable(role = Role.Button, onClick = { navController.popBackStack() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Volver",
                        tint = scheme.onSurface,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("GALAXOT  /  APARIENCIA", color = scheme.primary,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 1.4.sp)
                    Text("Estudio de reproductores", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
        item("studio_featured") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp)
                    .clip(RoundedCornerShape(33.dp))
                    .background(
                        Brush.linearGradient(listOf(
                            scheme.primary.copy(alpha = .28f),
                            scheme.surfaceContainer,
                            scheme.tertiary.copy(alpha = .12f),
                        )),
                    ),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    for (n in 0..7) {
                        drawCircle(
                            scheme.primary.copy(alpha = .11f),
                            radius = size.width * (.13f + .12f * n),
                            center = Offset(size.width * .86f, size.height * .15f),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("17 FORMAS DE SENTIR TU MÚSICA",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.primary, letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold)
                    Text("Un universo. Diecisiete identidades.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black, color = scheme.onSurface)
                    Text("Cada diseño conserva tus canciones, descargas y controles. Cambia el estilo cuando quieras.",
                        style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    Box(Modifier.clip(RoundedCornerShape(17.dp))
                        .background(scheme.primary.copy(alpha = .17f))
                        .padding(horizontal = 13.dp, vertical = 9.dp)) {
                        Text("SELECCIONADO  •  " + selected.name + "  /  " + studioIdentity(selected).category,
                            color = scheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item("studio_carousel") {
            GalaxPlayerStyleGallery(selected = selected, onSelected = onSelected)
        }
        item("studio_detail_heading") {
            Column(Modifier.padding(horizontal = 23.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("CATÁLOGO COMPLETO", color = scheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
                Text("Explora cada identidad",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Selecciona una tarjeta para activarla al abrir el reproductor.",
                    color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        items(PlayerDesignStyle.entries.chunked(2).size, key = { "studio_row_$" + it }) { rowIndex ->
            val styles = PlayerDesignStyle.entries.chunked(2)[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                styles.forEach { style ->
                    StudioStyleCard(
                        style = style,
                        selected = style == selected,
                        onSelect = { onSelected(style) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (styles.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item("studio_footer") {
            Text(
                "La elección se guarda automáticamente. La portada actual seguirá definiendo los colores de la app si está habilitado el tema reactivo.",
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StudioStyleCard(
    style: PlayerDesignStyle,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val identity = studioIdentity(style)
    Column(
        modifier = modifier.clip(RoundedCornerShape(23.dp))
            .background(
                if (selected) colors.primaryContainer.copy(alpha = .78f)
                else colors.surfaceContainerLow,
            )
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(identity.emblem, color = colors.primary,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge)
            Text(if (selected) "✓ ACTIVO" else style.name,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall)
        }
        Text(
            text = if (style.ordinal in 8..14) originalPlayerStyleConfig(style).name
                else playerLayoutLabel(style),
            color = colors.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            identity.description,
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            minLines = 3,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
