// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.security

import org.junit.Assert.assertFalse
import org.junit.Test

class PublicHostValidatorTest {
    @Test
    fun rejectsLoopbackAddresses() {
        assertFalse(PublicHostValidator.resolvesOnlyToPublicAddresses("127.0.0.1"))
    }

    @Test
    fun rejectsPrivateNetworkAddresses() {
        assertFalse(PublicHostValidator.resolvesOnlyToPublicAddresses("10.0.0.8"))
    }
}
