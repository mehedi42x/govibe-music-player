// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AudioTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        RemotePlaybackHistoryEntity::class,
        InstalledExtensionEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AuroraDatabase : RoomDatabase() {
    abstract fun audioTrackDao(): AudioTrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun remotePlaybackHistoryDao(): RemotePlaybackHistoryDao
    abstract fun extensionDao(): ExtensionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `remote_play_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `artworkUrl` TEXT NOT NULL,
                        `durationMs` INTEGER NOT NULL,
                        `playedAt` INTEGER NOT NULL
                    )""".trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_remote_play_history_sourceId` ON `remote_play_history` (`sourceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_remote_play_history_playedAt` ON `remote_play_history` (`playedAt`)")
            }
        }

        @Volatile
        private var instance: AuroraDatabase? = null

        fun get(context: Context): AuroraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuroraDatabase::class.java,
                    "aurora-library.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
