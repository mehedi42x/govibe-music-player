// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioTrackDao {
    @Query("SELECT * FROM audio_tracks ORDER BY title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
    fun observeFavorites(): Flow<List<AudioTrackEntity>>

    @Query("SELECT * FROM audio_tracks WHERE uri = :uri LIMIT 1")
    suspend fun find(uri: String): AudioTrackEntity?

    @Query("SELECT * FROM audio_tracks")
    suspend fun getAll(): List<AudioTrackEntity>

    @Query("SELECT * FROM audio_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title COLLATE NOCASE ASC")
    fun search(query: String): Flow<List<AudioTrackEntity>>

    @Query(
        """SELECT a.* FROM audio_tracks AS a
            LEFT JOIN play_history AS h ON a.uri = h.trackUri
            GROUP BY a.uri
            ORDER BY CASE WHEN LOWER(a.artist) IN (
                SELECT LOWER(t.artist) FROM audio_tracks AS t
                JOIN play_history AS ph ON ph.trackUri = t.uri
                GROUP BY LOWER(t.artist)
                ORDER BY COUNT(*) DESC, MAX(ph.playedAt) DESC LIMIT 4
            ) THEN 0 ELSE 1 END,
            MAX(h.playedAt) DESC, a.isFavorite DESC, a.importedAt DESC
            LIMIT 12""",
    )
    fun observeRecommendations(): Flow<List<AudioTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: AudioTrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<AudioTrackEntity>)

    @Query("UPDATE audio_tracks SET isFavorite = :favorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, favorite: Boolean)

    @Query("DELETE FROM audio_tracks WHERE uri = :uri")
    suspend fun delete(uri: String)

    @Query("DELETE FROM audio_tracks")
    suspend fun clear()
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun observePlaylist(id: Long): Flow<PlaylistWithTracks?>

    @Insert
    suspend fun create(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTrack(entry: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackUri = :trackUri")
    suspend fun removeTrack(playlistId: Long, trackUri: String)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlaybackHistoryDao {
    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT t.artist FROM audio_tracks AS t JOIN play_history AS h ON h.trackUri = t.uri GROUP BY LOWER(t.artist) ORDER BY COUNT(*) DESC, MAX(h.playedAt) DESC LIMIT 12")
    fun observeTopArtists(): Flow<List<String>>

    @Insert
    suspend fun add(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM play_history WHERE playedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)

    @Query("DELETE FROM play_history")
    suspend fun clear()
}

@Dao
interface RemotePlaybackHistoryDao {
    @Query("SELECT artist FROM remote_play_history GROUP BY LOWER(artist) ORDER BY COUNT(*) DESC, MAX(playedAt) DESC LIMIT 12")
    fun observeTopArtists(): Flow<List<String>>

    @Insert
    suspend fun add(entry: RemotePlaybackHistoryEntity)

    @Query("DELETE FROM remote_play_history")
    suspend fun clear()
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM installed_extensions ORDER BY builtIn DESC, installedAt ASC")
    fun observeAll(): Flow<List<InstalledExtensionEntity>>

    @Query("SELECT * FROM installed_extensions WHERE id = :id LIMIT 1")
    suspend fun find(id: String): InstalledExtensionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(extension: InstalledExtensionEntity)

    @Query("UPDATE installed_extensions SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM installed_extensions WHERE builtIn = 1")
    suspend fun removeBuiltIns()

    @Query("DELETE FROM installed_extensions WHERE id = :id")
    suspend fun uninstall(id: String)
}
