package com.arturo254.opentune.ui.player.mochy

enum class MochyPlayerStyle(val title: String, val subtitle: String) {
    NONE("GalaxOT", "Tus reproductores originales"),
    FLAGSHIP("Flagship", "Cristal, portada y profundidad"),
    CLASSIC("Classic", "Escenario limpio y atemporal"),
    TURNTABLE("Turntable", "Vinilo animado y controles analógicos"),
    VIDEO("Full video", "Escenario panorámico"),
    IPOD("iPod", "Rueda interactiva y pantalla retro"),
    POSTER("Poster", "Portada editorial protagonista"),
    TIMELINE("Timeline", "El tiempo como elemento visual"),
    LYRICS("Lyrics stage", "Tipografía y letras protagonistas"),
    BEDSIDE("Bedside clock", "Reloj nocturno minimalista"),
    BEDSIDE_TILES("Bedside tiles", "Panel musical en dos tarjetas");

    companion object {
        val selectable: List<MochyPlayerStyle> = entries.filterNot { it == NONE }
    }
}
