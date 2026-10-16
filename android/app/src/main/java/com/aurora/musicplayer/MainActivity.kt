// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aurora.musicplayer.core.NativePlaybackCore
import com.aurora.musicplayer.data.AudioTrackEntity
import com.aurora.musicplayer.playback.PlaybackService
import com.aurora.musicplayer.ui.AuroraApp
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val controllerState = androidx.compose.runtime.mutableStateOf<MediaController?>(null)
    private lateinit var app: AuroraApplication
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlayback: (() -> Unit)? = null

    private val audioPicker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNullOrEmpty()) return@registerForActivityResult
        lifecycleScope.launch {
            var imported = 0
            uris.forEach { uri ->
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: SecurityException) {
                    // Some document providers do not offer a persistable grant; import metadata anyway.
                }
                runCatching { app.library.import(uri) }
                    .onSuccess { imported++ }
            }
            Toast.makeText(this@MainActivity, "Added $imported audio file${if (imported == 1) "" else "s"}", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingPlayback?.invoke()
        pendingPlayback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as AuroraApplication
        window.statusBarColor = android.graphics.Color.rgb(11, 16, 32)
        window.navigationBarColor = android.graphics.Color.rgb(11, 16, 32)

        lifecycleScope.launch { app.extensions.cleanupLegacyBundledProfiles() }
        connectPlaybackController()

        setContent {
            AuroraApp(
                library = app.library,
                extensions = app.extensions,
                player = controllerState.value,
                onImportAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onPlayQueue = ::playQueue,
                onSkipNext = ::skipNext,
                onSkipPrevious = ::skipPrevious,
            )
        }
    }

    private fun connectPlaybackController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            { runCatching { future.get() }.onSuccess { controllerState.value = it } },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun playQueue(tracks: List<AudioTrackEntity>, startIndex: Int) {
        val controller = controllerState.value
        if (controller == null || tracks.isEmpty()) {
            Toast.makeText(this, "Player is still connecting. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }
        val safeIndex = NativePlaybackCore.safeStartIndex(tracks.size, startIndex)
        if (safeIndex < 0) return
        val action = {
            val items = tracks.map { track ->
                MediaItem.Builder()
                    .setMediaId(track.uri)
                    .setUri(Uri.parse(track.uri))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .build(),
                    )
                    .build()
            }
            controller.setMediaItems(items, safeIndex, 0L)
            controller.prepare()
            controller.play()
            lifecycleScope.launch { app.library.recordPlayed(tracks[safeIndex].uri) }
        }
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPlayback = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    private fun skipNext(repeatMode: Int, shuffle: Boolean) {
        val controller = controllerState.value ?: return
        val index = NativePlaybackCore.nextIndex(
            trackCount = controller.mediaItemCount,
            currentIndex = controller.currentMediaItemIndex,
            repeatMode = repeatMode,
            shuffle = shuffle,
            seed = System.nanoTime(),
        )
        if (index >= 0) controller.seekTo(index, 0L)
    }

    private fun skipPrevious(repeatMode: Int, shuffle: Boolean) {
        val controller = controllerState.value ?: return
        if (controller.currentPosition > 3_000L) {
            controller.seekTo(0L)
            return
        }
        val index = NativePlaybackCore.previousIndex(
            trackCount = controller.mediaItemCount,
            currentIndex = controller.currentMediaItemIndex,
            repeatMode = repeatMode,
            shuffle = shuffle,
            seed = System.nanoTime(),
        )
        if (index >= 0) controller.seekTo(index, 0L)
    }

    override fun onDestroy() {
        controllerState.value = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        super.onDestroy()
    }
}
