// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Indexes audio entries exposed by MediaStore after the user grants Android's audio-read permission. */
class DeviceAudioScanner(
    private val resolver: ContentResolver,
    private val tracks: AudioTrackDao,
) {
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val existingByUri = tracks.getAll().associateBy { it.uri }
        val discovered = mutableListOf<AudioTrackEntity>()
        resolver.query(collection, projection, null, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val mediaUri = ContentUris.withAppendedId(collection, cursor.getLong(idColumn)).toString()
                val existing = existingByUri[mediaUri]
                val fileName = cursor.getString(nameColumn).orEmpty().ifBlank { "Untitled audio" }
                val title = cursor.getString(titleColumn).orEmpty().trim().ifBlank {
                    fileName.substringBeforeLast('.', fileName)
                }
                discovered += AudioTrackEntity(
                    uri = mediaUri,
                    title = title,
                    artist = cursor.getString(artistColumn).orEmpty().trim().ifBlank { "Unknown artist" },
                    album = cursor.getString(albumColumn).orEmpty().trim().ifBlank { "Unknown album" },
                    durationMs = cursor.getLong(durationColumn).coerceAtLeast(0L),
                    mimeType = cursor.getString(mimeColumn).orEmpty().ifBlank { "audio/*" },
                    importedAt = existing?.importedAt ?: System.currentTimeMillis(),
                    isFavorite = existing?.isFavorite ?: false,
                )
            }
        }
        if (discovered.isNotEmpty()) tracks.insertAll(discovered)
        discovered.size
    }
}
