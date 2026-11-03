package com.arturo254.opentune.sources.navidrome

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavidromeCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)

data class NavidromeConnectionInfo(
    val baseUrl: String,
    val username: String,
)

@Singleton
class NavidromeAccountStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences("galaxot_navidrome", Context.MODE_PRIVATE)
    private val _connection = MutableStateFlow(readAccount()?.publicInfo())
    val connection: StateFlow<NavidromeConnectionInfo?> = _connection.asStateFlow()

    fun credentials(): NavidromeCredentials? = readAccount()

    fun save(credentials: NavidromeCredentials) {
        val verified = credentials.copy(baseUrl = validateNavidromeUrl(credentials.baseUrl))
        require(verified.username.isNotBlank() && verified.password.isNotEmpty())
        val secret = encrypt(verified.password)
        check(preferences.edit()
            .putString("base_url", verified.baseUrl)
            .putString("username", verified.username)
            .putString("password", secret)
            .commit())
        _connection.value = verified.publicInfo()
    }

    fun disconnect() {
        preferences.edit().clear().apply()
        _connection.value = null
    }

    private fun readAccount(): NavidromeCredentials? {
        val url = preferences.getString("base_url", null) ?: return null
        val user = preferences.getString("username", null) ?: return null
        val encrypted = preferences.getString("password", null) ?: return null
        return runCatching {
            NavidromeCredentials(
                baseUrl = validateNavidromeUrl(url),
                username = user,
                password = decrypt(encrypted),
            )
        }.getOrNull()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val data = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + data, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val data = Base64.decode(value, Base64.NO_WRAP)
        require(data.size > 28)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, data, 0, 12))
        return String(cipher.doFinal(data, 12, data.size - 12), Charsets.UTF_8)
    }

    private fun encryptionKey(): SecretKey {
        val keystore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keystore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun NavidromeCredentials.publicInfo() = NavidromeConnectionInfo(baseUrl, username)

    private companion object {
        const val KEY_ALIAS = "galaxot_navidrome_password_v1"
    }
}

internal fun validateNavidromeUrl(url: String): String {
    val parsed = runCatching { URI(url.trim()) }
        .getOrElse { throw IllegalArgumentException("Introduce una dirección HTTPS válida") }
    require(parsed.scheme.equals("https", ignoreCase = true) &&
        !parsed.host.isNullOrBlank() && parsed.userInfo == null &&
        parsed.query == null && parsed.fragment == null) {
        "Utiliza una dirección HTTPS válida, sin usuario, contraseña ni parámetros"
    }
    val path = parsed.path.orEmpty().trimEnd('/')
    require(!path.endsWith("/rest")) { "Introduce la dirección del servidor sin /rest" }
    return URI(
        "https",
        null,
        parsed.host,
        parsed.port,
        path.ifBlank { null },
        null,
        null,
    ).toASCIIString().trimEnd('/')
}
