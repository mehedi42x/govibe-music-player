// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

/** UI and lifecycle policy for indexing the shared device music library. */
internal object AudioScanPolicy {
    const val RESCAN_DEBOUNCE_MS = 5_000L

    fun shouldShowPermissionAction(hasPermission: Boolean): Boolean = !hasPermission

    fun shouldAutoScan(
        hasPermission: Boolean,
        scanInProgress: Boolean,
        lastScanStartedAtMs: Long,
        nowMs: Long,
    ): Boolean {
        if (!hasPermission || scanInProgress) return false
        if (lastScanStartedAtMs <= 0L) return true
        return nowMs - lastScanStartedAtMs >= RESCAN_DEBOUNCE_MS
    }
}
