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
import com.aurora.musicplayer.data.CatalogTrack
import com.aurora.musicplayer.playback.PlaybackService
import com.aurora.musicplayer.ui.AuroraApp
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val controllerState = androidx.compose.runtime.mutableStateOf<MediaController?>(null)
    private lateinit var app: AuroraApplication
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var pendingPlayback: (() -> Unit)? = null
    private var lastRecordedMediaId: String? = null

    private val historyListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            recordMediaItem(mediaItem)
        }
    }

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

    private val audioReadPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanDeviceAudioNow()
        else Toast.makeText(this, "Allow audio access to scan music on this device.", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as AuroraApplication
        window.statusBarColor = android.graphics.Color.rgb(8, 13, 25)
        window.navigationBarColor = android.graphics.Color.rgb(8, 13, 25)

        lifecycleScope.launch { app.extensions.cleanupLegacyBundledProfiles() }
        connectPlaybackController()

        setContent {
            AuroraApp(
                library = app.library,
                extensions = app.extensions,
                player = controllerState.value,
                onImportAudio = { audioPicker.launch(arrayOf("audio/*")) },
                onScanDeviceAudio = ::requestDeviceAudioScan,
                onAudioSettingsChange = { volume, boostDb, preset -> PlaybackService.updateAudioSettings(volume, boostDb, preset) },
                onPlayQueue = ::playQueue,
                onPlayRemoteQueue = ::playRemoteQueue,
                onSkipNext = ::skipNext,
                onSkipPrevious = ::skipPrevious,
            )
        }
    }

    private fun requestDeviceAudioScan() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            scanDeviceAudioNow()
        } else {
            audioReadPermission.launch(permission)
        }
    }

    private fun scanDeviceAudioNow() {
        lifecycleScope.launch {
            runCatching { app.library.scanDeviceAudio() }
                .onSuccess { count ->
                    Toast.makeText(this@MainActivity, "Found $count audio file${if (count == 1) "" else "s"} on this device.", Toast.LENGTH_SHORT).show()
                }
                .onFailure { error ->
                    Toast.makeText(this@MainActivity, error.message ?: "Could not scan device audio.", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun connectPlaybackController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { controller ->
                    controllerState.value = controller
                    controller.addListener(historyListener)
                    recordMediaItem(controller.currentMediaItem)
                }
            },
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
        val action: () -> Unit = {
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
            recordMediaItem(controller.currentMediaItem)
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

    private fun playRemoteQueue(tracks: List<CatalogTrack>, startIndex: Int) {
        val controller = controllerState.value
        if (controller == null || tracks.isEmpty()) {
            Toast.makeText(this, "Player is still connecting. Try again in a moment.", Toast.LENGTH_SHORT).show()
            return
        }
        val safeIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId("$REMOTE_MEDIA_ID_PREFIX${track.sourceId}:${track.id}")
                .setUri(Uri.parse(track.streamUrl))
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setDescription(track.description)
                        .setArtworkUri(track.artworkUrl.takeIf(String::isNotBlank)?.let(Uri::parse))
                        .build(),
                )
                .build()
        }
        val action: () -> Unit = {
            controller.setMediaItems(items, safeIndex, 0L)
            controller.prepare()
            controller.play()
            recordMediaItem(controller.currentMediaItem)
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

    private fun recordMediaItem(item: MediaItem?) {
        item ?: return
        val mediaId = item.mediaId
        if (mediaId == lastRecordedMediaId) return
        lastRecordedMediaId = mediaId
        if (mediaId.startsWith(REMOTE_MEDIA_ID_PREFIX)) {
            val identifiers = mediaId.removePrefix(REMOTE_MEDIA_ID_PREFIX).split(':', limit = 2)
            if (identifiers.size != 2 || identifiers[1].isBlank()) return
            val metadata = item.mediaMetadata
            val track = CatalogTrack(
                sourceId = identifiers[0],
                id = identifiers[1],
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                album = metadata.albumTitle?.toString().orEmpty(),
                description = metadata.description?.toString().orEmpty(),
                artworkUrl = metadata.artworkUri?.toString().orEmpty(),
                streamUrl = item.localConfiguration?.uri?.toString().orEmpty(),
                durationMs = 0L,
            )
            lifecycleScope.launch { runCatching { app.library.recordRemotePlayed(track) } }
        } else {
            lifecycleScope.launch { runCatching { app.library.recordPlayed(mediaId) } }
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
        controllerState.value?.removeListener(historyListener)
        controllerState.value = null
        controllerFuture?.let(MediaController::releaseFuture)
        controllerFuture = null
        super.onDestroy()
    }

    private companion object {
        const val REMOTE_MEDIA_ID_PREFIX = "nextvibe:"
    }
}
