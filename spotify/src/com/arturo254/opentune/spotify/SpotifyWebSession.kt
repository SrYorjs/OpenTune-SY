/*
 * OpenTune Spotify web catalog session.
 *
 * This client mirrors the anonymous Spotify Web Player bootstrap only for
 * catalog metadata access. It does not expose audio streams or downloads.
 */

package com.arturo254.opentune.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SpotifyWebSession {
    data class Credentials(
        val accessToken: String,
        val clientToken: String?,
        val clientVersion: String?,
    )

    private data class State(
        val accessToken: String,
        val accessTokenExpiresAtMs: Long,
        val clientToken: String?,
        val clientTokenExpiresAtMs: Long,
        val clientVersion: String?,
        val clientId: String?,
        val deviceId: String,
        val cookies: Map<String, String>,
    ) {
        fun isUsable(nowMs: Long): Boolean =
            accessToken.isNotBlank() &&
                accessTokenExpiresAtMs > nowMs + EXPIRY_SKEW_MS &&
                (
                    clientToken == null ||
                        clientTokenExpiresAtMs > nowMs + EXPIRY_SKEW_MS
                )
    }

    private val mutex = Mutex()
    private val random = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true }

    private val client by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
            expectSuccess = false
        }
    }

    @Volatile
    private var cachedState: State? = null

    suspend fun credentials(): Credentials =
        mutex.withLock {
            val now = System.currentTimeMillis()
            val existing = cachedState
            if (existing != null && existing.isUsable(now)) {
                return@withLock existing.toCredentials()
            }

            val fresh = bootstrap()
            cachedState = fresh
            fresh.toCredentials()
        }

    suspend fun invalidate() {
        mutex.withLock {
            cachedState = null
        }
    }

    private suspend fun bootstrap(): State {
        val cookies = linkedMapOf<String, String>()
        val landing =
            client.get(SPOTIFY_WEB_URL) {
                header("User-Agent", USER_AGENT)
                header("Accept", "text/html,application/xhtml+xml")
            }

        captureCookies(landing.headers.getAll(HttpHeaders.SetCookie), cookies)
        if (landing.status.value !in 200..399) {
            throw Spotify.SpotifyException(
                landing.status.value,
                "Spotify Web bootstrap failed",
            )
        }

        val landingBody = landing.bodyAsText()
        val clientVersion = extractClientVersion(landingBody)
        val deviceId = cookies["sp_t"].orEmpty().ifBlank(::generateDeviceId)

        val totp = generateTotp(System.currentTimeMillis())
        val tokenResponse =
            client.get(SPOTIFY_TOKEN_URL) {
                header("User-Agent", USER_AGENT)
                header("Accept", "application/json")
                cookieHeader(cookies)
                parameter("reason", "init")
                parameter("productType", "web-player")
                parameter("totp", totp.code)
                parameter("totpVer", totp.version)
                parameter("totpServer", totp.code)
            }

        captureCookies(tokenResponse.headers.getAll(HttpHeaders.SetCookie), cookies)
        if (tokenResponse.status.value !in 200..299) {
            throw Spotify.SpotifyException(
                tokenResponse.status.value,
                "Spotify Web access-token bootstrap failed",
            )
        }

        val tokenJson = json.parseToJsonElement(tokenResponse.bodyAsText()).jsonObject
        val accessToken =
            tokenJson["accessToken"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf(String::isNotBlank)
                ?: throw Spotify.SpotifyException(500, "Spotify Web token response had no access token")
        val accessExpiry =
            tokenJson["accessTokenExpirationTimestampMs"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toLongOrNull()
                ?: tokenJson["accessTokenExpirationTimestamp"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.toLongOrNull()
                    ?.let { if (it < 100_000_000_000L) it * 1_000L else it }
                ?: (System.currentTimeMillis() + DEFAULT_ACCESS_TOKEN_TTL_MS)
        val clientId =
            tokenJson["clientId"]
                ?.jsonPrimitive
                ?.contentOrNull

        val clientToken =
            runCatching {
                requestClientToken(
                    clientId = clientId,
                    clientVersion = clientVersion,
                    deviceId = deviceId,
                )
            }.getOrNull()

        return State(
            accessToken = accessToken,
            accessTokenExpiresAtMs = accessExpiry,
            clientToken = clientToken?.first,
            clientTokenExpiresAtMs = clientToken?.second ?: 0L,
            clientVersion = clientVersion,
            clientId = clientId,
            deviceId = deviceId,
            cookies = cookies.toMap(),
        )
    }

    private suspend fun requestClientToken(
        clientId: String?,
        clientVersion: String?,
        deviceId: String,
    ): Pair<String, Long>? {
        if (clientId.isNullOrBlank() || clientVersion.isNullOrBlank()) return null

        val payload =
            """{"client_data":{"client_version":"${escapeJson(clientVersion)}","client_id":"${escapeJson(clientId)}","js_sdk_data":{"device_brand":"unknown","device_model":"unknown","os":"android","os_version":"unknown","device_id":"${escapeJson(deviceId)}","device_type":"computer"}}}"""

        val response =
            client.post(SPOTIFY_CLIENT_TOKEN_URL) {
                header("User-Agent", USER_AGENT)
                header("Accept", "application/json")
                setBody(
                    TextContent(
                        payload,
                        ContentType.Application.Json,
                    ),
                )
            }

        if (response.status.value !in 200..299) return null
        val root = json.parseToJsonElement(response.bodyAsText()).jsonObject
        if (root["response_type"]?.jsonPrimitive?.contentOrNull != "RESPONSE_GRANTED_TOKEN_RESPONSE") {
            return null
        }

        val granted = root["granted_token"]?.jsonObject ?: return null
        val token =
            granted["token"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.takeIf(String::isNotBlank)
                ?: return null
        val ttlSeconds =
            granted["expires_after_seconds"]
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toLongOrNull()
                ?: DEFAULT_CLIENT_TOKEN_TTL_SECONDS

        return token to (System.currentTimeMillis() + ttlSeconds * 1_000L)
    }

    private fun extractClientVersion(html: String): String? {
        val encoded =
            APP_SERVER_CONFIG_REGEX
                .find(html)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?: return null

        val decoded =
            runCatching { Base64.getDecoder().decode(encoded) }
                .recoverCatching { Base64.getUrlDecoder().decode(encoded) }
                .getOrNull()
                ?: return null

        val root =
            runCatching {
                json.parseToJsonElement(
                    decoded.toString(StandardCharsets.UTF_8),
                ).jsonObject
            }.getOrNull() ?: return null

        return root["clientVersion"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.takeIf(String::isNotBlank)
    }

    private fun captureCookies(
        setCookieHeaders: List<String>?,
        destination: MutableMap<String, String>,
    ) {
        setCookieHeaders.orEmpty().forEach { header ->
            val pair = header.substringBefore(';')
            val separator = pair.indexOf('=')
            if (separator <= 0) return@forEach
            val name = pair.substring(0, separator).trim()
            val value = pair.substring(separator + 1).trim()
            if (name.isNotBlank()) destination[name] = value
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.cookieHeader(
        cookies: Map<String, String>,
    ) {
        if (cookies.isEmpty()) return
        header(
            HttpHeaders.Cookie,
            cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" },
        )
    }

    private fun State.toCredentials(): Credentials =
        Credentials(
            accessToken = accessToken,
            clientToken = clientToken,
            clientVersion = clientVersion,
        )

    internal data class Totp(
        val code: String,
        val version: Int,
    )

    internal fun generateTotp(nowMs: Long): Totp {
        val secret = TOTP_SECRETS[TOTP_VERSION]
            ?: error("Missing Spotify Web TOTP secret version $TOTP_VERSION")
        val transformed =
            secret
                .mapIndexed { index, value ->
                    value xor ((index % 33) + 9)
                }.joinToString(separator = "")
                .toByteArray(StandardCharsets.UTF_8)

        val counter = nowMs / 1_000L / 30L
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val mac =
            Mac.getInstance("HmacSHA1").apply {
                init(SecretKeySpec(transformed, "HmacSHA1"))
            }
        val digest = mac.doFinal(counterBytes)
        val offset = digest.last().toInt() and 0x0f
        val binary =
            ((digest[offset].toInt() and 0x7f) shl 24) or
                ((digest[offset + 1].toInt() and 0xff) shl 16) or
                ((digest[offset + 2].toInt() and 0xff) shl 8) or
                (digest[offset + 3].toInt() and 0xff)
        val code = (binary % 1_000_000).toString().padStart(6, '0')
        return Totp(code = code, version = TOTP_VERSION)
    }

    private fun generateDeviceId(): String {
        val chars = "0123456789abcdef"
        return buildString(32) {
            repeat(32) {
                append(chars[random.nextInt(chars.length)])
            }
        }
    }

    private fun escapeJson(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

    private const val SPOTIFY_WEB_URL = "https://open.spotify.com/"
    private const val SPOTIFY_TOKEN_URL = "https://open.spotify.com/api/token"
    private const val SPOTIFY_CLIENT_TOKEN_URL = "https://clienttoken.spotify.com/v1/clienttoken"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"
    private const val EXPIRY_SKEW_MS = 60_000L
    private const val DEFAULT_ACCESS_TOKEN_TTL_MS = 30L * 60L * 1_000L
    private const val DEFAULT_CLIENT_TOKEN_TTL_SECONDS = 60L * 60L
    private val APP_SERVER_CONFIG_REGEX =
        Regex(
            """<script\s+id=["']appServerConfig["']\s+type=["']text/plain["']>([^<]+)</script>""",
            RegexOption.IGNORE_CASE,
        )

    private const val TOTP_VERSION = 61

    private val TOTP_SECRETS =
        mapOf(
            61 to
                intArrayOf(
                    44, 55, 47, 42, 70, 40, 34, 114, 76, 74, 50, 111, 120,
                    97, 75, 76, 94, 102, 43, 69, 49, 120, 118, 80, 64, 78,
                ),
        )
}
