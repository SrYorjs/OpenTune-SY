package com.arturo254.opentune.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

data class SpotifyNativeTrackMetadata(
    val isrc: String? = null,
    val durationMs: Long? = null,
    val explicit: Boolean? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
)

object SpotifyWebMetadataClient {
    private val cache = ConcurrentHashMap<String, SpotifyNativeTrackMetadata>()

    private val client by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
            expectSuccess = false
        }
    }

    suspend fun trackMetadata(trackId: String): Result<SpotifyNativeTrackMetadata> =
        runCatching {
            require(trackId.isNotBlank()) { "Spotify track id cannot be blank" }
            cache[trackId]?.let { return@runCatching it }

            val metadata =
                fetchTrackMetadata(
                    trackId = trackId,
                    allowRetry = true,
                )
            cache[trackId] = metadata
            metadata
        }

    private suspend fun fetchTrackMetadata(
        trackId: String,
        allowRetry: Boolean,
    ): SpotifyNativeTrackMetadata {
        val credentials = SpotifyWebSession.credentials()
        val gid = spotifyIdToHexGid(trackId)
        val response =
            client.get(
                "https://spclient.wg.spotify.com/metadata/4/track/$gid?market=from_token",
            ) {
                header("Authorization", "Bearer ${credentials.accessToken}")
                credentials.clientToken
                    ?.takeIf(String::isNotBlank)
                    ?.let { header("Client-Token", it) }
                credentials.clientVersion
                    ?.takeIf(String::isNotBlank)
                    ?.let { header("Spotify-App-Version", it) }
                header("App-Platform", "WebPlayer")
                header("User-Agent", USER_AGENT)
            }

        if (response.status.value == 401 && allowRetry) {
            SpotifyWebSession.invalidate()
            return fetchTrackMetadata(
                trackId = trackId,
                allowRetry = false,
            )
        }

        if (response.status.value == 404) {
            return SpotifyNativeTrackMetadata()
        }

        if (response.status.value !in 200..299) {
            throw Spotify.SpotifyException(
                response.status.value,
                "Spotify native metadata request failed",
            )
        }

        return parseTrackMetadata(response.body<ByteArray>())
    }

    internal fun parseTrackMetadata(bytes: ByteArray): SpotifyNativeTrackMetadata {
        if (bytes.isEmpty()) return SpotifyNativeTrackMetadata()
        val fields = ProtoReader(bytes).readFields()

        var isrc: String? = null
        fields[10]
            .orEmpty()
            .filter { it.wireType == 2 }
            .forEach { externalEntry ->
                val externalFields = ProtoReader(externalEntry.bytesValue).readFields()
                val type =
                    externalFields[1]
                        .orEmpty()
                        .firstOrNull { it.wireType == 2 }
                        ?.bytesValue
                        ?.toString(StandardCharsets.UTF_8)
                        ?.lowercase()
                        .orEmpty()
                val id =
                    externalFields[2]
                        .orEmpty()
                        .firstOrNull { it.wireType == 2 }
                        ?.bytesValue
                        ?.toString(StandardCharsets.UTF_8)
                        ?.trim()
                        .orEmpty()
                if (type == "isrc" && id.isNotBlank()) {
                    isrc = id.uppercase()
                }
            }

        val fallbackIsrc =
            if (isrc == null) {
                ISRC_FALLBACK_REGEX
                    .find(bytes.toString(StandardCharsets.ISO_8859_1))
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.uppercase()
            } else {
                null
            }

        return SpotifyNativeTrackMetadata(
            isrc = isrc ?: fallbackIsrc,
            durationMs = fields.firstVarint(7)?.decodeZigZag()?.takeIf { it > 0L },
            explicit = fields.firstVarint(9)?.let { it == 1L },
            trackNumber =
                fields.firstVarint(5)
                    ?.decodeZigZag()
                    ?.toInt()
                    ?.takeIf { it > 0 },
            discNumber =
                fields.firstVarint(6)
                    ?.decodeZigZag()
                    ?.toInt()
                    ?.takeIf { it > 0 },
        )
    }

    internal fun spotifyIdToHexGid(spotifyId: String): String {
        require(spotifyId.isNotBlank()) { "Spotify id cannot be blank" }

        val littleEndianBytes = mutableListOf<Int>()
        spotifyId.forEach { character ->
            val value = BASE62_ALPHABET.indexOf(character)
            require(value >= 0) { "Invalid Spotify id character: $character" }

            var carry = value
            for (index in littleEndianBytes.indices) {
                val total = littleEndianBytes[index] * 62 + carry
                littleEndianBytes[index] = total and 0xff
                carry = total ushr 8
            }
            while (carry > 0) {
                littleEndianBytes += carry and 0xff
                carry = carry ushr 8
            }
        }

        require(littleEndianBytes.size <= 16) { "Spotify id exceeds 128-bit GID size" }
        while (littleEndianBytes.size < 16) {
            littleEndianBytes += 0
        }

        return buildString(32) {
            for (index in 15 downTo 0) {
                append(littleEndianBytes[index].toString(16).padStart(2, '0'))
            }
        }
    }

    private data class ProtoValue(
        val wireType: Int,
        val varintValue: Long = 0L,
        val bytesValue: ByteArray = byteArrayOf(),
    )

    private class ProtoReader(
        private val bytes: ByteArray,
    ) {
        private var position = 0

        fun readFields(): Map<Int, List<ProtoValue>> {
            val fields = linkedMapOf<Int, MutableList<ProtoValue>>()

            while (position < bytes.size) {
                val tag = readVarint() ?: break
                if (tag <= 0L) break

                val fieldNumber = (tag / 8L).toInt()
                val wireType = (tag % 8L).toInt()
                val value =
                    when (wireType) {
                        0 ->
                            ProtoValue(
                                wireType = wireType,
                                varintValue = readVarint() ?: break,
                            )

                        1 -> {
                            if (position + 8 > bytes.size) break
                            ProtoValue(
                                wireType = wireType,
                                bytesValue = bytes.copyOfRange(position, position + 8),
                            ).also { position += 8 }
                        }

                        2 -> {
                            val length = readVarint()?.toInt() ?: break
                            if (length < 0 || position + length > bytes.size) break
                            ProtoValue(
                                wireType = wireType,
                                bytesValue = bytes.copyOfRange(position, position + length),
                            ).also { position += length }
                        }

                        5 -> {
                            if (position + 4 > bytes.size) break
                            ProtoValue(
                                wireType = wireType,
                                bytesValue = bytes.copyOfRange(position, position + 4),
                            ).also { position += 4 }
                        }

                        else -> break
                    }

                fields.getOrPut(fieldNumber) { mutableListOf() } += value
            }

            return fields
        }

        private fun readVarint(): Long? {
            var result = 0L
            var shift = 0
            repeat(10) {
                if (position >= bytes.size) return null
                val current = bytes[position++].toInt() and 0xff
                result = result or ((current and 0x7f).toLong() shl shift)
                if (current and 0x80 == 0) return result
                shift += 7
            }
            return null
        }
    }

    private fun Map<Int, List<ProtoValue>>.firstVarint(field: Int): Long? =
        this[field]
            .orEmpty()
            .firstOrNull { it.wireType == 0 }
            ?.varintValue

    private fun Long.decodeZigZag(): Long = (this ushr 1) xor -(this and 1L)

    private const val BASE62_ALPHABET =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Mobile Safari/537.36"
    private val ISRC_FALLBACK_REGEX =
        Regex("isrc[\\x00-\\x1f]+([A-Za-z0-9]{12})", RegexOption.IGNORE_CASE)
}
