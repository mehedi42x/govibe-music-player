// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.extensions

import com.aurora.musicplayer.data.CatalogTrack
import com.aurora.musicplayer.security.PublicHostValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val MAX_CATALOG_BYTES = 2 * 1024 * 1024
private const val MAX_TRACKS_PER_RESPONSE = 60

/** Reads the NextVibe source JSON contract; manifests never execute code or provide UI. */
class MusicSourceClient {
    suspend fun trending(manifest: ExtensionManifest): List<CatalogTrack> = withContext(Dispatchers.IO) {
        val source = requireNotNull(manifest.source) { "This extension does not define a music source." }
        parseTracks(get(source, source.trendingPath), manifest.id, source.downloadsAllowed)
    }

    suspend fun search(manifest: ExtensionManifest, query: String): List<CatalogTrack> = withContext(Dispatchers.IO) {
        val source = requireNotNull(manifest.source) { "This extension does not define a music source." }
        val encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.name()).replace("+", "%20")
        parseTracks(get(source, source.searchPath.replace("{query}", encoded)), manifest.id, source.downloadsAllowed)
    }

    private fun get(source: MusicSourceDefinition, path: String): String {
        val url = "${source.baseUrl.trimEnd('/')}$path"
        val uri = runCatching { URI(url) }.getOrElse { throw IOException("The music source URL is invalid.") }
        val baseHost = URI(source.baseUrl).host ?: throw IOException("The music source host is invalid.")
        if (!uri.scheme.equals("https", true) || !uri.host.equals(baseHost, true) || uri.userInfo != null) {
            throw IOException("Source requests must stay on the configured HTTPS host.")
        }
        if (!PublicHostValidator.resolvesOnlyToPublicAddresses(baseHost)) {
            throw IOException("The music source host resolved to a local, reserved, or unavailable network address.")
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NextVibe-Android/0.3")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException(when (status) {
                    301, 302, 303, 307, 308 -> "Source endpoint redirected. Use a direct HTTPS endpoint on the configured host."
                    404 -> "The source endpoint was not found. Check the extension settings."
                    else -> "Music source request failed with HTTP $status."
                })
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_CATALOG_BYTES) throw IOException("Source response exceeds the 2 MiB limit.")
                    output.write(buffer, 0, count)
                }
            }
            return String(output.toByteArray(), StandardCharsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTracks(rawJson: String, sourceId: String, downloadsAllowed: Boolean): List<CatalogTrack> {
        val root = try {
            JSONObject(rawJson)
        } catch (_: Exception) {
            throw IOException("Source returned invalid JSON. Expected an object with a tracks array.")
        }
        val array: JSONArray = root.optJSONArray("tracks")
            ?: throw IOException("Source response is missing the tracks array.")
        if (array.length() > MAX_TRACKS_PER_RESPONSE) throw IOException("Source returned more than $MAX_TRACKS_PER_RESPONSE tracks.")
        val publicHostCache = mutableMapOf<String, Boolean>()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("id", "").trim().take(160)
            val title = item.optString("title", "").trim().take(160)
            val streamUrl = item.optString("streamUrl", "").trim()
            if (id.isBlank() || title.isBlank() || !isPublicHttpsUrl(streamUrl, publicHostCache)) return@mapNotNull null
            val artworkUrl = item.optString("thumbnailUrl", "").trim()
            val artwork = artworkUrl.takeIf { isPublicHttpsUrl(it, publicHostCache) }.orEmpty()
            CatalogTrack(
                sourceId = sourceId,
                id = id,
                title = title,
                artist = item.optString("artist", "Unknown artist").trim().ifBlank { "Unknown artist" }.take(120),
                album = item.optString("album", "").trim().take(120),
                description = item.optString("description", "").trim().take(500),
                artworkUrl = artwork,
                streamUrl = streamUrl,
                durationMs = item.optLong("durationMs", 0L).coerceIn(0L, 86_400_000L),
                downloadsAllowed = downloadsAllowed,
            )
        }
    }

    private fun isPublicHttpsUrl(value: String, hostCache: MutableMap<String, Boolean>): Boolean {
        if (value.isBlank() || value.length > 2_048) return false
        val uri = runCatching { URI(value) }.getOrNull() ?: return false
        val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null || uri.fragment != null) return false
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return false
        if (host.matches(Regex("^[0-9.]+$")) || host.contains(':')) return false
        if (uri.port != -1 && uri.port != 443) return false
        val resolvesPublicly = hostCache[host] ?: PublicHostValidator.resolvesOnlyToPublicAddresses(host).also { hostCache[host] = it }
        return resolvesPublicly
    }
}
