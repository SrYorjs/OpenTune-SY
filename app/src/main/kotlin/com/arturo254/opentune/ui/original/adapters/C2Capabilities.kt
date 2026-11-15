package com.arturo254.opentune.ui.original.adapters
object C2Capabilities {
 const val audioSpectrum = false
 fun actionAvailable(action: com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction): Boolean =
  action != com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.OUTPUT &&
  action != com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.TRANSLATE_LYRICS
 val unavailable = mapOf(
  "lookModules" to "GalaxOT has no active Mochy look-module runtime",
  "audioSpectrum" to "GalaxOT does not expose a PCM spectrum tap",
  "parallelTranslation" to "Pending C6; existing translated lyric bodies remain real",
  "galaxMochyEditor" to "Mochy layout editor cannot edit V1?V17; selection remains available"
 )
}
