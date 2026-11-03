package com.arturo254.opentune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.sources.navidrome.NavidromeAccountStore
import com.arturo254.opentune.sources.navidrome.NavidromeCredentials
import com.arturo254.opentune.sources.navidrome.NavidromeGateway
import com.arturo254.opentune.sources.navidrome.validateNavidromeUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NavidromeSettingsState(
    val isChecking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

@HiltViewModel
class NavidromeSettingsViewModel @Inject constructor(
    private val accounts: NavidromeAccountStore,
    private val gateway: NavidromeGateway,
) : ViewModel() {
    val connection = accounts.connection
    private val _state = MutableStateFlow(NavidromeSettingsState())
    val state: StateFlow<NavidromeSettingsState> = _state.asStateFlow()

    fun connect(url: String, username: String, password: String) {
        if (_state.value.isChecking) return
        val credentials = try {
            NavidromeCredentials(
                baseUrl = validateNavidromeUrl(url),
                username = username.trim().also { require(it.isNotBlank()) },
                password = password.also { require(it.isNotEmpty()) },
            )
        } catch (_: IllegalArgumentException) {
            _state.value = NavidromeSettingsState(
                error = "Introduce una URL HTTPS, tu usuario y la contraseña de Navidrome",
            )
            return
        }

        viewModelScope.launch {
            _state.value = NavidromeSettingsState(isChecking = true)
            try {
                val test = gateway.ping(credentials)
                test.fold(
                    onSuccess = {
                        accounts.save(credentials)
                        _state.value = NavidromeSettingsState(
                            message = "Navidrome conectado. Tu biblioteca ya puede aparecer en Search.",
                        )
                    },
                    onFailure = {
                        _state.value = NavidromeSettingsState(
                            error = "No se pudo conectar: " + (it.message ?: "Comprueba el servidor"),
                        )
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.value = NavidromeSettingsState(
                    error = "No se pudo guardar la conexión de forma segura",
                )
            }
        }
    }

    fun disconnect() {
        if (_state.value.isChecking) return
        accounts.disconnect()
        _state.value = NavidromeSettingsState(message = "Navidrome desconectado")
    }
}
