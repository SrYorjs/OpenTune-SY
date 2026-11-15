package com.arturo254.opentune.ui.original.adapters
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
/** Deliberate capability absence: no PCM spectrum is fabricated or sampled from a timer. */
internal interface OriginalAudioSpectrum { fun read(positionMs: Long, target: FloatArray): Boolean }
internal fun originalAudioSpectrum(connection: OriginalPlayerConnection): OriginalAudioSpectrum? = null
@Composable internal fun UnavailableOriginalFeature(reason: String, modifier: Modifier = Modifier) { Text(reason, modifier) }
