package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arturo254.opentune.innertube.diagnostics.BackendComparisonReport
import com.arturo254.opentune.innertube.diagnostics.MediaBackendDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Manual experiment: no streams played or downloaded and no credentials transmitted. */
@Composable
fun MediaBackendDiagnosticsPanel() {
    var instance by remember { mutableStateOf("https://pipedapi.kavin.rocks") }
    var videos by remember { mutableStateOf("jNQXAC9IVRw, aqz-KE-bpKQ") }
    var query by remember { mutableStateOf("lofi") }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf<BackendComparisonReport?>(null) }
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Comparación experimental de YouTube", style = MaterialTheme.typography.titleMedium)
            Text(
                "Compara Piped, InnerTube y NewPipeExtractor usando los mismos videos públicos. " +
                    "No cambia el reproductor, no usa tu sesión ni descarga música.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = instance,
                onValueChange = { instance = it },
                label = { Text("Instancia API de Piped (HTTPS)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !running,
            )
            OutlinedTextField(
                value = videos,
                onValueChange = { videos = it },
                label = { Text("IDs de YouTube (máximo 3, separados por coma)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !running,
                minLines = 1,
                maxLines = 3,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Búsqueda musical en Piped") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !running,
            )
            Button(
                enabled = !running,
                onClick = {
                    error = null
                    report = null
                    running = true
                    scope.launch {
                        try {
                            report = MediaBackendDiagnostics.compare(
                                videoIds = videos.split(',', '\n', ' ')
                                    .filter(String::isNotBlank),
                                pipedBaseUrl = instance,
                                searchQuery = query,
                            )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (invalid: IllegalArgumentException) {
                            error = invalid.message ?: "Comprueba la URL y los IDs"
                        } catch (_: Exception) {
                            error = "El diagnóstico no pudo completarse"
                        } finally {
                            running = false
                        }
                    }
                },
            ) {
                Text(if (running) "Comparando…" else "Ejecutar diagnóstico")
            }
            if (running) CircularProgressIndicator()
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            report?.let { result ->
                Text("Instancia: ${result.pipedHost}", style = MaterialTheme.typography.labelMedium)
                result.observations.groupBy { it.videoId }.forEach { (videoId, observations) ->
                    Text("Video: $videoId", style = MaterialTheme.typography.titleSmall)
                    observations.forEach { observation ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(observation.engine, style = MaterialTheme.typography.labelLarge)
                                Text(observation.status, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                "Metadatos: ${if (observation.hasMetadata) "sí" else "no"} · " +
                                    "Formatos anunciados: ${observation.audioFormats}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (observation.formats.isNotEmpty()) {
                                Text(
                                    observation.formats.joinToString(" / "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Text("Filtros de búsqueda musical Piped", style = MaterialTheme.typography.titleSmall)
                result.search.forEach { item ->
                    Text(
                        "${item.filter}: ${item.results} resultados (${item.status})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    "Esta comparación mide acceso a los metadatos y formatos declarados, " +
                        "no verifica el acceso al CDN ni la reproducción real de los streams. " +
                        "No imprime URLs firmadas, cookies, tokens ni mensajes de error del servidor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
