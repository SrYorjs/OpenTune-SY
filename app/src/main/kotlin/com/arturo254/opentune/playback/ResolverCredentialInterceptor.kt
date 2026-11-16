package com.arturo254.opentune.playback

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

class ResolverCredentialInterceptor(
    private val origin: () -> HttpUrl?,
    private val key: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val root = origin()
        val secret = key()
        val trusted = root != null && request.url.scheme == "https" &&
            request.url.host == root.host && request.url.port == root.port &&
            request.url.encodedPath.startsWith(root.encodedPath.trimEnd('/') + "/")
        val builder = request.newBuilder().removeHeader("X-API-Key")
        val cleanUrl = request.url.newBuilder()
        if (secret.isNotEmpty()) request.url.queryParameterNames.filter {
            secret in request.url.queryParameterValues(it)
        }.forEach(cleanUrl::removeAllQueryParameters)
        builder.url(cleanUrl.build())
        if (trusted) {
            builder.url(cleanUrl.removeAllQueryParameters("key").build())
            if (secret.isNotBlank()) builder.header("X-API-Key", secret)
        }
        return chain.proceed(builder.build())
    }
}
