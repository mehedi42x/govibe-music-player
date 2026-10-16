// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class AudioLibraryRepository(
    private val context: Context,
    private val tracks: AudioTrackDao,
    private val history: PlaybackHistoryDao,
    private val remoteHistory: RemotePlaybackHistoryDao,
) {
    fun observeTracks(): Flow<List<AudioTrackEntity>> = tracks.observeAll()
    fun observeFavorites(): Flow<List<AudioTrackEntity>> = tracks.observeFavorites()
    fun observeRecommendations(): Flow<List<AudioTrackEntity>> = tracks.observeRecommendations()
    fun search(query: String): Flow<List<AudioTrackEntity>> = tracks.search(query.trim())
    suspend fun scanDeviceAudio(): Int = DeviceAudioScanner(context.contentResolver, tracks).scan()

    fun observeTasteArtists(): Flow<List<String>> = combine(
        history.observeTopArtists(),
        remoteHistory.observeTopArtists(),
    ) { local, remote ->
        (remote + local).distinctBy { it.trim().lowercase() }.filter { it.isNotBlank() }.take(12)
    }

    suspend fun import(uri: Uri): AudioTrackEntity = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val fileName = displayName(resolver, uri).orEmpty().ifBlank { "Untitled audio" }
        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            // Metadata may be absent or the document provider may not expose it.
        } finally {
            runCatching { retriever.release() }
        }

        val extension = fileName.substringAfterLast('.', "")
        val resolvedTitle = title?.trim().takeUnless { it.isNullOrEmpty() }
            ?: fileName.removeSuffix(if (extension.isEmpty()) "" else ".$extension")
        val existing = tracks.find(uri.toString())
        AudioTrackEntity(
            uri = uri.toString(),
            title = resolvedTitle,
            artist = artist?.trim().takeUnless { it.isNullOrEmpty() } ?: "Unknown artist",
            album = album?.trim().takeUnless { it.isNullOrEmpty() } ?: "Unknown album",
            durationMs = duration,
            mimeType = resolver.getType(uri) ?: "application/octet-stream",
            importedAt = existing?.importedAt ?: System.currentTimeMillis(),
            isFavorite = existing?.isFavorite ?: false,
        ).also { tracks.insert(it) }
    }

    suspend fun downloadForOffline(
        track: CatalogTrack,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): AudioTrackEntity = withContext(Dispatchers.IO) {
        require(track.downloadsAllowed) { "This music source has not enabled offline downloads." }
        val downloaded = MusicTrackDownloader(context).download(track, onProgress)
        val uri = downloaded.uri.toString()
        val existing = tracks.find(uri)
        AudioTrackEntity(
            uri = uri,
            title = track.title,
            artist = track.artist,
            album = track.album.ifBlank { "Unknown album" },
            durationMs = track.durationMs,
            mimeType = downloaded.mimeType,
            importedAt = existing?.importedAt ?: System.currentTimeMillis(),
            isFavorite = existing?.isFavorite ?: false,
        ).also { tracks.insert(it) }
    }

    suspend fun setFavorite(uri: String, favorite: Boolean) = tracks.setFavorite(uri, favorite)
    suspend fun remove(uri: String) = tracks.delete(uri)
    suspend fun recordPlayed(uri: String) = history.add(PlaybackHistoryEntity(trackUri = uri))
    suspend fun recordRemotePlayed(track: CatalogTrack) {
        remoteHistory.add(
            RemotePlaybackHistoryEntity(
                sourceId = track.sourceId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                durationMs = track.durationMs,
            ),
        )
    }

    suspend fun clearLibrary() {
        tracks.getAll().forEach { track ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    Uri.parse(track.uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        tracks.clear()
        history.clear()
        remoteHistory.clear()
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
