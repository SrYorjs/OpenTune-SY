package com.arturo254.opentune.ui.player.GalaxStudioPlayer

import androidx.compose.runtime.Immutable
import com.arturo254.opentune.constants.PlayerDesignStyle

@Immutable
data class GalaxOriginalStyleConfig(
    val name: String,
    val subtitle: String,
    val description: String,
    val cornerRadius: Float,
    val roundPlay: Boolean,
    val artScale: Float,
    val ornament: String,
)

fun originalPlayerStyleConfig(style: PlayerDesignStyle): GalaxOriginalStyleConfig = when (style) {
    PlayerDesignStyle.V9 -> GalaxOriginalStyleConfig(
        "NEBULA GARDEN", "DEEP SPACE", "Nebulosas en capas y portada flotante",
        32f, true, .72f, "NOVA  /  09",
    )
    PlayerDesignStyle.V10 -> GalaxOriginalStyleConfig(
        "EVENT HORIZON", "CELESTIAL", "Órbitas concéntricas y movimiento orbital",
        90f, true, .68f, "ORBIT  /  10",
    )
    PlayerDesignStyle.V11 -> GalaxOriginalStyleConfig(
        "CIRCUIT BREAK", "SYNTH CIRCUIT", "Rejilla digital, esquinas rectas y luces de neón",
        9f, false, .77f, "GRID  /  11",
    )
    PlayerDesignStyle.V12 -> GalaxOriginalStyleConfig(
        "MAGNETIC TAPE", "ANALOG SOUL", "Disco circular y surcos de vinilo",
        55f, true, .70f, "VINYL  /  12",
    )
    PlayerDesignStyle.V13 -> GalaxOriginalStyleConfig(
        "POLAR SILK", "NORTHERN LIGHTS", "Auroras suaves y composición atmosférica",
        37f, false, .82f, "AURORA  /  13",
    )
    PlayerDesignStyle.V14 -> GalaxOriginalStyleConfig(
        "WHITE NOISE", "MONO EDITION", "Tipografía monumental y geometría esencial",
        4f, false, .56f, "MONO  /  14",
    )
    PlayerDesignStyle.V15 -> GalaxOriginalStyleConfig(
        "ION REACTOR", "ENERGY CORE", "Ondas de energía sincronizadas con la reproducción",
        23f, true, .75f, "PULSE  /  15",
    )
    PlayerDesignStyle.V17 -> GalaxOriginalStyleConfig(
        "VINJAX", "33⅓ RPM", "Tocadiscos analógico con aguja móvil",
        40f, true, .73f, "VINJAX  /  17",
    )
    else -> GalaxOriginalStyleConfig(
        "GALAXOT", "MUSIC UNIVERSE", "Tu música sin límites", 24f, true, .72f, "GALAXOT",
    )
}

