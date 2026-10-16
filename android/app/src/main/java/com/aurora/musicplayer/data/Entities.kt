package com.aurora.musicplayer.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "audio_tracks")
data class AudioTrackEntity(
    @PrimaryKey val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mimeType: String,
    val importedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackUri"],
    indices = [Index("trackUri")],
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackUri: String,
    val position: Int,
)

data class PlaylistWithTracks(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "uri",
        associateBy = Junction(
            value = PlaylistTrackCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "trackUri",
        ),
        entity = AudioTrackEntity::class,
    )
    val tracks: List<AudioTrackEntity>,
)

@Entity(tableName = "play_history", indices = [Index("playedAt")])
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackUri: String,
    val playedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "installed_extensions")
data class InstalledExtensionEntity(
    @PrimaryKey val id: String,
    val manifestJson: String,
    val sourceRepo: String,
    // Kept for database compatibility; startup removes all legacy bundled rows.
    val builtIn: Boolean,
    val enabled: Boolean = true,
    val installedAt: Long = System.currentTimeMillis(),
)
