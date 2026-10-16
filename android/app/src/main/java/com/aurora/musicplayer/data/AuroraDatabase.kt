package com.aurora.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AudioTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        InstalledExtensionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun extensionDao(): ExtensionDao

    companion object {
        @Volatile
        private var instance: AuroraDatabase? = null

        fun get(context: Context): AuroraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuroraDatabase::class.java,
                    "aurora-library.db",
                ).build().also { instance = it }
            }
    }
}
