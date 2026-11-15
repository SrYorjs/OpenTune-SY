package com.arturo254.opentune.ui.original.adapters
/** Explicit unavailable features, not simulated playback or provider state. */
object C1Capabilities {
 const val parallelTranslationAvailable = false
 val unavailable = mapOf(
  "outputRouting" to "Device output and casting are unavailable",
  "providerAttribution" to "LyricsEntity stores only id and lyrics",
  "autoplayProvenance" to "Queue windows do not expose the service's private auto-added set",
  "wordRuby" to "Only whole-line Japanese/Korean romanization exists",
  "parallelTranslation" to "Deferred to C6; existing translated lyric bodies remain readable",
  "imageVideoShare" to "Deferred to operation 12; native text sharing remains available"
 )
}
