// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioScanPolicyTest {
    @Test
    fun permissionActionIsShownOnlyWhenAudioAccessIsMissing() {
        assertTrue(AudioScanPolicy.shouldShowPermissionAction(hasPermission = false))
        assertFalse(AudioScanPolicy.shouldShowPermissionAction(hasPermission = true))
    }

    @Test
    fun automaticScanRequiresPermissionAndNoScanAlreadyRunning() {
        assertFalse(AudioScanPolicy.shouldAutoScan(false, false, 0L, 10_000L))
        assertFalse(AudioScanPolicy.shouldAutoScan(true, true, 0L, 10_000L))
        assertTrue(AudioScanPolicy.shouldAutoScan(true, false, 0L, 10_000L))
    }

    @Test
    fun recentAutomaticScanIsDebouncedButLaterForegroundScanIsAllowed() {
        assertFalse(AudioScanPolicy.shouldAutoScan(true, false, 10_000L, 14_999L))
        assertTrue(AudioScanPolicy.shouldAutoScan(true, false, 10_000L, 15_000L))
    }
}
