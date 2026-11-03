package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arturo254.opentune.viewmodels.NavidromeSettingsViewModel

@Composable
fun NavidromeConnectionPanel(
    viewModel: NavidromeSettingsViewModel = hiltViewModel(),
) {
    val connected by viewModel.connection.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var server by remember(connected?.baseUrl) {
        mutableStateOf(connected?.baseUrl.orEmpty())
    }
    var username by remember(connected?.username) {
        mutableStateOf(connected?.username.orEmpty())
    }
    var password by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Navidrome · Biblioteca personal", style = MaterialTheme.typography.titleMedium)
            Text(
                if (connected == null) {
                    "Conecta tu propio servidor de música para buscar, reproducir FLAC y descargar tus archivos."
                } else {
                    "Conectado a ${connected!!.baseUrl} como ${connected!!.username}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ocultar configuración" else if (connected == null) {
                    "Configurar servidor"
                } else "Cambiar servidor o contraseña")
            }
            if (expanded) {
                HorizontalDivider()
                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Dirección HTTPS") },
                    placeholder = { Text("https://musica.ejemplo.com") },
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Usuario Navidrome") },
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                )
                Text(
                    "La contraseña se cifra en este dispositivo. Utiliza HTTPS con un certificado válido.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    enabled = !state.isChecking && server.isNotBlank() &&
                        username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        viewModel.connect(server, username, password)
                        password = ""
                    },
                ) {
                    Text(if (state.isChecking) "Verificando conexión…" else "Conectar")
                }
                if (state.isChecking) CircularProgressIndicator()
                if (connected != null) {
                    OutlinedButton(onClick = {
                        viewModel.disconnect()
                        password = ""
                        expanded = false
                    }) { Text("Desconectar Navidrome") }
                }
            }
            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
