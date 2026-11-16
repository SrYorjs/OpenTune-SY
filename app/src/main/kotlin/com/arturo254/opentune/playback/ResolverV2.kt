package com.arturo254.opentune.playback

import com.arturo254.opentune.core.model.*
import kotlinx.serialization.json.*

data class ResolverStream(
    val provider: String, val id: String, val kind: String, val mimeType: String,
    val bitrate: Int?, val length: Long?, val expires: Int, val url: String,
    val codecs: String?, val width: Int?, val height: Int?, val fps: Int?,
)

internal val resolverJson = Json { ignoreUnknownKeys = true }
internal fun JsonObject.value(name: String) = (get(name) as? JsonPrimitive)?.contentOrNull

internal fun parseResolverSearch(raw: String, requestedProvider: String): List<SourceVariant>? {
    val body = resolverJson.parseToJsonElement(raw).jsonObject
    val results = body["results"]?.jsonArray ?: return null
    if (results.isEmpty() && (body["partial"] as? JsonPrimitive)?.booleanOrNull == true) return null
    return results.mapNotNull { element ->
        val item = element.jsonObject
        val provider = item.value("provider") ?: return@mapNotNull null
        if (provider != requestedProvider && !(provider == "youtube_music" && requestedProvider == "youtube")) return@mapNotNull null
        val id = item.value("id")?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) } ?: return@mapNotNull null
        val title = item.value("title")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val artist = item.value("artist")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val source = if (provider == "audius") ProviderId.AUDIUS else ProviderId.YOUTUBE_MUSIC
        SourceVariant(source,id,title,listOf(artist),
            durationMs = (item["durationSeconds"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it >= 0 }?.times(1000)?.toLong(),
            artworkUrl = item.value("thumbnail"),
            version = TrackVersion(if (item.value("kind") == "video") setOf(TrackVersionTrait.MUSIC_VIDEO) else emptySet()))
    }.distinctBy { it.provider to it.sourceId }
}

internal fun parseResolverStream(raw: String, provider: String, id: String, kind: String, url: String): ResolverStream? {
    val item = resolverJson.parseToJsonElement(raw).jsonObject
    if (item.value("provider") != provider || item.value("id") != id || item.value("kind") != kind) return null
    val mime = item.value("mimeType")?.takeIf { it.startsWith("$kind/") } ?: return null
    val expires = (item["expiresInSeconds"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 } ?: return null
    return ResolverStream(provider,id,kind,mime,(item["bitrate"] as? JsonPrimitive)?.intOrNull,
        (item["contentLength"] as? JsonPrimitive)?.longOrNull,expires,url,item.value("codecs"),
        (item["width"] as? JsonPrimitive)?.intOrNull,(item["height"] as? JsonPrimitive)?.intOrNull,
        (item["fps"] as? JsonPrimitive)?.intOrNull)
}
