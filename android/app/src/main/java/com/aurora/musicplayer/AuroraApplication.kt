package com.aurora.musicplayer

import android.app.Application
import com.aurora.musicplayer.data.AuroraDatabase
import com.aurora.musicplayer.data.AudioLibraryRepository
import com.aurora.musicplayer.extensions.ExtensionRepository

class AuroraApplication : Application() {
    val database by lazy { AuroraDatabase.get(this) }
    val library by lazy {
        AudioLibraryRepository(this, database.audioTrackDao(), database.playbackHistoryDao())
    }
    val extensions by lazy {
        ExtensionRepository(this, database.extensionDao())
    }
}
