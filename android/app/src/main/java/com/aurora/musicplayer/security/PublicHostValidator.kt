// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.security

import java.net.InetAddress

/** Best-effort DNS preflight for public media hosts; this is not connection-level DNS pinning. */
internal object PublicHostValidator {
    fun resolvesOnlyToPublicAddresses(host: String): Boolean {
        if (host.isBlank()) return false
        val addresses = runCatching { InetAddress.getAllByName(host) }.getOrNull() ?: return false
        return addresses.isNotEmpty() && addresses.none(::isNonPublicAddress)
    }

    private fun isNonPublicAddress(address: InetAddress): Boolean {
        val bytes = address.address
        val first = bytes.firstOrNull()?.toInt()?.and(0xFF) ?: return true
        val second = bytes.getOrNull(1)?.toInt()?.and(0xFF) ?: 0
        val carrierGradeNat = bytes.size == 4 && first == 100 && second in 64..127
        val ipv6UniqueLocal = bytes.size == 16 && (first and 0xFE) == 0xFC
        val reservedIpv4 = bytes.size == 4 && (first == 0 || first >= 224)
        return address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress || carrierGradeNat || ipv6UniqueLocal || reservedIpv4
    }
}
