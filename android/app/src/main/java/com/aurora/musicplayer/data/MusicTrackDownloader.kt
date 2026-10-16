// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.aurora.musicplayer.security.PublicHostValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest

private const val MAX_DOWNLOAD_BYTES = 250L * 1024L * 1024L
private const val MAX_DOWNLOAD_REDIRECTS = 4
private const val PROGRESS_INTERVAL_BYTES = 256L * 1024L

internal data class DownloadedAudioFile(val uri: Uri, val mimeType: String, val displayName: String)

/** Saves a permitted user-requested direct source stream to the shared Music/NextVibe folder. */
internal class MusicTrackDownloader(private val context: Context) {
    suspend fun download(
        track: CatalogTrack,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): DownloadedAudioFile = withContext(Dispatchers.IO) {
        require(track.downloadsAllowed) { "This music source has not enabled offline downloads." }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            throw IOException("Allow storage access to save music on this Android version.")
        }

        var current = validateStreamUrl(track.streamUrl)
        val visited = mutableSetOf<String>()
        for (redirect in 0..MAX_DOWNLOAD_REDIRECTS) {
            currentCoroutineContext().ensureActive()
            val urlString = current.toASCIIString()
            if (!visited.add(urlString)) throw IOException("The music source returned a redirect loop.")
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "audio/*,application/octet-stream")
                setRequestProperty("User-Agent", "NextVibe-Android/0.3")
            }
            try {
                when (val status = connection.responseCode) {
                    301, 302, 303, 307, 308 -> {
                        if (redirect == MAX_DOWNLOAD_REDIRECTS) throw IOException("The audio stream redirected too many times.")
                        val location = connection.getHeaderField("Location") ?: throw IOException("The audio stream returned an invalid redirect.")
                        current = validateStreamUrl(current.resolve(location).toASCIIString())
                    }
                    200 -> {
                        val expectedBytes = connection.getHeaderFieldLong("Content-Length", -1L)
                        if (expectedBytes > MAX_DOWNLOAD_BYTES) throw IOException("This audio file exceeds the 250 MiB download limit.")
                        val headerMime = connection.contentType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
                        if (headerMime.isNotEmpty() && !isAllowedAudioMimeType(headerMime)) {
                            throw IOException("The selected source did not return an audio file.")
                        }
                        val mimeType = resolveMimeType(headerMime, track.streamUrl)
                        val displayName = fileNameFor(track, mimeType)
                        val destination = createMediaStoreItem(displayName, mimeType)
                        var published = false
                        try {
                            val output = context.contentResolver.openOutputStream(destination, "w")
                                ?: throw IOException("Could not open the Music/NextVibe destination.")
                            output.use { stream ->
                                connection.inputStream.use { input ->
                                    val buffer = ByteArray(32 * 1024)
                                    var total = 0L
                                    var nextProgress = PROGRESS_INTERVAL_BYTES
                                    while (true) {
                                        currentCoroutineContext().ensureActive()
                                        val count = input.read(buffer)
                                        if (count < 0) break
                                        total += count
                                        if (total > MAX_DOWNLOAD_BYTES) throw IOException("This audio file exceeds the 250 MiB download limit.")
                                        stream.write(buffer, 0, count)
                                        if (total >= nextProgress || (expectedBytes > 0L && total == expectedBytes)) {
                                            onProgress(total, expectedBytes)
                                            nextProgress = total + PROGRESS_INTERVAL_BYTES
                                        }
                                    }
                                    if (total == 0L) throw IOException("The source returned an empty audio file.")
                                    if (expectedBytes > 0L && total != expectedBytes) throw IOException("The source connection ended before the audio file finished downloading.")
                                    onProgress(total, if (expectedBytes > 0L) expectedBytes else total)
                                }
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val ready = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                                if (context.contentResolver.update(destination, ready, null, null) != 1) {
                                    throw IOException("The downloaded song could not be published to Music/NextVibe.")
                                }
                            }
                            published = true
                            return@withContext DownloadedAudioFile(destination, mimeType, displayName)
                        } finally {
                            if (!published) context.contentResolver.delete(destination, null, null)
                        }
                    }
                    else -> throw IOException("Audio download failed with HTTP $status.")
                }
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("The audio source could not be reached.")
    }

    private fun createMediaStoreItem(displayName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/NextVibe")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            } else {
                val musicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val nextVibeDirectory = java.io.File(musicDirectory, "NextVibe")
                if (!nextVibeDirectory.exists() && !nextVibeDirectory.mkdirs()) {
                    throw IOException("Could not create the Music/NextVibe folder.")
                }
                put(MediaStore.MediaColumns.DATA, java.io.File(nextVibeDirectory, displayName).absolutePath)
            }
        }
        return context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Android could not create a file in Music/NextVibe.")
    }

    private fun validateStreamUrl(value: String): URI {
        if (value.isBlank() || value.length > 2_048) throw IOException("The audio URL is invalid.")
        val uri = runCatching { URI(value).normalize() }.getOrNull() ?: throw IOException("The audio URL is invalid.")
        val host = uri.host?.lowercase()?.trimEnd('.') ?: throw IOException("The audio URL has no valid host.")
        if (!uri.scheme.equals("https", true) || uri.userInfo != null || uri.fragment != null) {
            throw IOException("Offline audio must use a public HTTPS stream URL.")
        }
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal") || host.matches(Regex("^[0-9.]+$")) || host.contains(':')) {
            throw IOException("Local and IP-literal audio hosts are not supported.")
        }
        if (uri.port != -1 && uri.port != 443) throw IOException("Nonstandard audio ports are not supported.")
        if (!PublicHostValidator.resolvesOnlyToPublicAddresses(host)) {
            throw IOException("Audio hosts resolving to local or reserved IP addresses are not supported.")
        }
        return uri
    }

    private fun fileNameFor(track: CatalogTrack, mimeType: String): String {
        val identity = "${track.sourceId}:${track.id}:${track.streamUrl}"
        val digest = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { byte -> "%02x".format(byte) }
        val artist = safeFilePart(track.artist).ifBlank { "Artist" }
        val title = safeFilePart(track.title).ifBlank { "Track" }
        val mimeExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?.lowercase()?.takeIf { it in AUDIO_EXTENSIONS }
        val pathExtension = runCatching { URI(track.streamUrl).path.orEmpty().substringAfterLast('.', "") }
            .getOrNull()?.lowercase()?.takeIf { it in AUDIO_EXTENSIONS }
        val extension = mimeExtension ?: pathExtension
        return "$artist - $title-$digest${extension?.let { ".$it" }.orEmpty()}"
    }

    private fun safeFilePart(value: String): String = value.trim()
        .map { char -> if (char.isLetterOrDigit() || char in " -_" ) char else '_' }
        .joinToString("")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '_', '.')
        .take(20)

    private fun resolveMimeType(headerMime: String, streamUrl: String): String {
        if (headerMime.startsWith("audio/")) return headerMime
        val extension = runCatching { URI(streamUrl).path.orEmpty().substringAfterLast('.', "") }.getOrNull().orEmpty()
        val inferred = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
            ?.takeIf { it.startsWith("audio/") }
        if (inferred != null) return inferred
        return when (headerMime) {
            "application/ogg" -> "audio/ogg"
            "application/x-flac" -> "audio/flac"
            "application/mp4" -> "audio/mp4"
            else -> throw IOException("The source must identify the response as an audio file or use a recognized audio-file URL extension.")
        }
    }

    private fun isAllowedAudioMimeType(value: String): Boolean =
        value.startsWith("audio/") || value in setOf("application/octet-stream", "application/ogg", "application/x-flac", "application/mp4")

    private companion object {
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "mp4", "webm")
    }
}
