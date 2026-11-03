package com.arturo254.opentune.playback

import java.util.Locale

internal object PlaybackGateClassifier {
    fun requiresAgeVerification(reason: String?): Boolean {
        val normalized = reason?.lowercase(Locale.ROOT) ?: return false
        return listOf(
            "confirm your age",
            "verify your age",
            "age-restricted",
            "age restricted",
            "inappropriate for some users",
            "mature audiences",
            "confirma tu edad",
            "confirmar tu edad",
            "verificar tu edad",
            "restricción de edad",
            "restringido por edad",
            "inadecuado para algunos usuarios",
            "mayor de edad",
        ).any { it in normalized }
    }
}
