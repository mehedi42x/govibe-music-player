// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aurora.musicplayer.security.PublicHostValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

private const val MAX_ARTWORK_BYTES = 5 * 1024 * 1024
private const val MAX_ARTWORK_DIMENSION = 2048
private const val MAX_REDIRECTS = 4
private val artworkCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
}

@Composable
fun NetworkArtwork(url: String, modifier: Modifier = Modifier) {
    val bitmap = produceState<Bitmap?>(initialValue = artworkCache.get(url), key1 = url) {
        value = artworkCache.get(url) ?: withContext(Dispatchers.IO) { loadArtwork(url) }
    }.value
    ArtworkFrame(bitmap, modifier)
}

/** Uses remote artwork first; for local content URIs, decodes embedded album art without uploading audio. */
@Composable
fun PlaybackArtwork(artworkUrl: String, mediaId: String?, modifier: Modifier = Modifier) {
    if (artworkUrl.isNotBlank()) {
        NetworkArtwork(artworkUrl, modifier)
        return
    }
    val context = LocalContext.current
    val localUri = mediaId?.takeIf { value ->
        runCatching { Uri.parse(value).scheme in setOf("content", "file") }.getOrDefault(false)
    }
    val cacheKey = localUri?.let { "local:$it" }
    val bitmap = produceState<Bitmap?>(initialValue = cacheKey?.let(artworkCache::get), key1 = cacheKey) {
        value = cacheKey?.let(artworkCache::get)
            ?: localUri?.let { uri ->
                withContext(Dispatchers.IO) {
                    loadEmbeddedArtwork(context, uri)?.also { artworkCache.put("local:$uri", it) }
                }
            }
    }.value
    ArtworkFrame(bitmap, modifier)
}

@Composable
private fun ArtworkFrame(bitmap: Bitmap?, modifier: Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap.asImageBitmap(), contentDescription = "Track artwork", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun loadArtwork(value: String): Bitmap? {
    var current = validatedArtworkUri(value) ?: return null
    val visited = mutableSetOf<String>()
    for (redirect in 0..MAX_REDIRECTS) {
        val currentString = current.toASCIIString()
        if (!visited.add(currentString)) return null
        val connection = runCatching { URL(currentString).openConnection() as HttpURLConnection }.getOrNull() ?: return null
        connection.connectTimeout = 5_000
        connection.readTimeout = 7_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "image/*")
        try {
            when (connection.responseCode) {
                301, 302, 303, 307, 308 -> {
                    if (redirect == MAX_REDIRECTS) return null
                    val location = connection.getHeaderField("Location") ?: return null
                    val next = runCatching { current.resolve(location) }.getOrNull() ?: return null
                    current = validatedArtworkUri(next.toASCIIString()) ?: return null
                }
                in 200..299 -> {
                    val contentLength = connection.getHeaderFieldLong("Content-Length", -1L)
                    if (contentLength > MAX_ARTWORK_BYTES) return null
                    val bytes = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_ARTWORK_BYTES) return null
                            bytes.write(buffer, 0, count)
                        }
                    }
                    return decodeArtwork(bytes.toByteArray())?.also { artworkCache.put(value, it) }
                }
                else -> return null
            }
        } catch (_: Exception) {
            return null
        } finally {
            connection.disconnect()
        }
    }
    return null
}

private fun validatedArtworkUri(value: String): URI? {
    val uri = runCatching { URI(value).normalize() }.getOrNull() ?: return null
    val host = uri.host?.lowercase()?.trimEnd('.') ?: return null
    if (!uri.scheme.equals("https", true) || uri.userInfo != null || uri.fragment != null) return null
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return null
    if (host.matches(Regex("^[0-9.]+$")) || host.contains(':')) return null
    if (uri.port != -1 && uri.port != 443) return null
    if (!PublicHostValidator.resolvesOnlyToPublicAddresses(host)) return null
    return uri
}

private fun loadEmbeddedArtwork(context: Context, value: String): Bitmap? {
    val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("content", "file")) return null
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val bytes = retriever.embeddedPicture?.takeIf { it.isNotEmpty() && it.size <= MAX_ARTWORK_BYTES } ?: return null
        decodeArtwork(bytes)
    } catch (_: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun decodeArtwork(bytes: ByteArray): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_ARTWORK_DIMENSION) sampleSize *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
}
