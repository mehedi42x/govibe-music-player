// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.core

/** Primitive-only bridge to the Rust queue engine shipped in the Android app. */
internal object NativePlaybackCore {
    const val REPEAT_OFF = 0
    const val REPEAT_ALL = 1
    const val REPEAT_ONE = 2

    init {
        System.loadLibrary("govibe_core")
    }

    external fun safeStartIndex(trackCount: Int, requestedIndex: Int): Int

    external fun nextIndex(
        trackCount: Int,
        currentIndex: Int,
        repeatMode: Int,
        shuffle: Boolean,
        seed: Long,
    ): Int

    external fun previousIndex(
        trackCount: Int,
        currentIndex: Int,
        repeatMode: Int,
        shuffle: Boolean,
        seed: Long,
    ): Int
}
