package com.arturo254.opentune.ui.original.adapters
object C2Capabilities {
 const val audioSpectrum = false
 fun actionAvailable(action: com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction): Boolean =
  action != com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.OUTPUT &&
  action != com.arturo254.opentune.ui.original.player.studio.PlayerMenuAction.TRANSLATE_LYRICS
 val unavailable = mapOf(
  "lookModules" to "Look modules are unavailable",
  "audioSpectrum" to "Audio spectrum is unavailable",
  "parallelTranslation" to "Pending C6; existing translated lyric bodies remain real",
  "galaxMochyEditor" to "Layout editing is unavailable for V1-V17; selection remains available"
 )
}
