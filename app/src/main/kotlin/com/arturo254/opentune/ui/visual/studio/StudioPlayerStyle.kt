package com.arturo254.opentune.ui.visual.studio

import com.arturo254.opentune.constants.PlayerDesignStyle

enum class StudioPlayerStyle(
    val selectorName: String,
    val legacy: PlayerDesignStyle?,
) {
    FLAGSHIP("Flagship", null),
    CLASSIC("Classic", null),
    TURNTABLE("Turntable", null),
    VIDEO("Full video", null),
    IPOD("iPod", null),
    BEDSIDE_TILES("Bedside", null),
    BEDSIDE("Big clock", null),
    POSTER("Poster", null),
    TIMELINE("Timeline", null),
    LYRICS("Lyrics stage", null),
    V1("V1", PlayerDesignStyle.V1),
    V2("V2", PlayerDesignStyle.V2),
    V3("V3", PlayerDesignStyle.V3),
    V4("V4", PlayerDesignStyle.V4),
    V5("V5", PlayerDesignStyle.V5),
    V6("V6", PlayerDesignStyle.V6),
    V7("V7", PlayerDesignStyle.V7),
    V8("V8", PlayerDesignStyle.V8),
    V9("V9", PlayerDesignStyle.V9),
    V10("V10", PlayerDesignStyle.V10),
    V11("V11", PlayerDesignStyle.V11),
    V12("V12", PlayerDesignStyle.V12),
    V13("V13", PlayerDesignStyle.V13),
    V14("V14", PlayerDesignStyle.V14),
    V15("V15", PlayerDesignStyle.V15),
    V16("V16", PlayerDesignStyle.V16),
    V17("V17", PlayerDesignStyle.V17),
    ;

    val native: Boolean get() = legacy == null
    val editable: Boolean get() = false

    companion object {
        val selectorColumns: List<List<StudioPlayerStyle>> = listOf(
            listOf(FLAGSHIP, CLASSIC),
            listOf(TURNTABLE),
            listOf(VIDEO),
            listOf(IPOD),
            listOf(BEDSIDE_TILES, BEDSIDE),
            listOf(POSTER),
            listOf(TIMELINE, LYRICS),
        ) + PlayerDesignStyle.entries.map { style ->
            listOf(entries.first { it.legacy == style })
        }

        val selectorStyles: List<StudioPlayerStyle> =
            selectorColumns.flatten()

        val columns: List<List<Int>> = selectorColumns.map { column ->
            column.map { selectorStyles.indexOf(it) }
        }

        fun fromStored(value: String?): StudioPlayerStyle =
            entries.firstOrNull { it.name == value } ?: FLAGSHIP
    }
}
