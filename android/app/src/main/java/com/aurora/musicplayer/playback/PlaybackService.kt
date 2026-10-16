// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.playback

import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var requestedVolume = 1f
    private var requestedBoostDb = 0f
    private var requestedEqualizerPreset = EQ_FLAT

    private val audioSessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            attachEffects(audioSessionId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("nextvibe-ui-settings", MODE_PRIVATE)
        requestedVolume = prefs.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f)
        requestedBoostDb = prefs.getFloat(KEY_BOOST_DB, 0f).coerceIn(0f, MAX_BOOST_DB)
        requestedEqualizerPreset = prefs.getInt(KEY_EQ_PRESET, EQ_FLAT).coerceIn(EQ_FLAT, EQ_TREBLE)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player = ExoPlayer.Builder(this).build().also { exoPlayer ->
            exoPlayer.setAudioAttributes(audioAttributes, true)
            exoPlayer.setHandleAudioBecomingNoisy(true)
            exoPlayer.volume = requestedVolume
            exoPlayer.addListener(audioSessionListener)
            if (exoPlayer.audioSessionId > 0) attachEffects(exoPlayer.audioSessionId)
        }
        mediaSession = MediaSession.Builder(this, requireNotNull(player)).build()
        activeService = this
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun applyAudioSettings(volume: Float, boostDb: Float, equalizerPreset: Int) {
        requestedVolume = volume.coerceIn(0f, 1f)
        requestedBoostDb = boostDb.coerceIn(0f, MAX_BOOST_DB)
        requestedEqualizerPreset = equalizerPreset.coerceIn(EQ_FLAT, EQ_TREBLE)
        player?.volume = requestedVolume
        runCatching {
            loudnessEnhancer?.setTargetGain((requestedBoostDb * MILLIBELS_PER_DB).toInt())
            loudnessEnhancer?.enabled = requestedBoostDb > 0f
        }
        applyEqualizerPreset()
    }

    private fun attachEffects(audioSessionId: Int) {
        releaseEffects()
        if (audioSessionId <= 0) return
        runCatching {
            equalizer = Equalizer(0, audioSessionId)
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            applyEqualizerPreset()
            loudnessEnhancer?.setTargetGain((requestedBoostDb * MILLIBELS_PER_DB).toInt())
            loudnessEnhancer?.enabled = requestedBoostDb > 0f
        }.onFailure {
            releaseEffects()
        }
    }

    private fun applyEqualizerPreset() {
        val effect = equalizer ?: return
        runCatching {
            val range = effect.bandLevelRange
            val lower = range[0].toInt()
            val upper = range[1].toInt()
            val boost = (upper * 0.32f).toInt().coerceAtLeast(0)
            for (index in 0 until effect.numberOfBands.toInt()) {
                val band = index.toShort()
                val frequencyHz = effect.getCenterFreq(band) / 1_000
                val level = when (requestedEqualizerPreset) {
                    EQ_BASS -> if (frequencyHz < 250) boost else 0
                    EQ_VOCAL -> if (frequencyHz in 500..4_000) boost / 2 else 0
                    EQ_TREBLE -> if (frequencyHz >= 4_000) boost else 0
                    else -> 0
                }
                effect.setBandLevel(band, level.coerceIn(lower, upper).toShort())
            }
            effect.enabled = requestedEqualizerPreset != EQ_FLAT
        }
    }

    private fun releaseEffects() {
        runCatching { equalizer?.enabled = false }
        runCatching { equalizer?.release() }
        equalizer = null
        runCatching { loudnessEnhancer?.enabled = false }
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
    }

    override fun onDestroy() {
        if (activeService === this) activeService = null
        player?.removeListener(audioSessionListener)
        releaseEffects()
        mediaSession?.runCatching { release() }
        mediaSession = null
        player?.runCatching { release() }
        player = null
        super.onDestroy()
    }

    companion object {
        const val EQ_FLAT = 0
        const val EQ_BASS = 1
        const val EQ_VOCAL = 2
        const val EQ_TREBLE = 3
        const val MAX_BOOST_DB = 3f
        private const val MILLIBELS_PER_DB = 100f
        private const val KEY_VOLUME = "playback-volume"
        private const val KEY_BOOST_DB = "volume-boost-db"
        private const val KEY_EQ_PRESET = "equalizer-preset"

        @Volatile
        private var activeService: PlaybackService? = null

        fun updateAudioSettings(volume: Float, boostDb: Float, equalizerPreset: Int) {
            activeService?.applyAudioSettings(volume, boostDb, equalizerPreset)
        }
    }
}
