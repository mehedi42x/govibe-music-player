// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NightInk = Color(0xFF0B1020)
private val NightSurface = Color(0xFF121A2B)
private val NightVariant = Color(0xFF1B263B)
private val Mint = Color(0xFFB7E36B)
private val LightInk = Color(0xFF182033)
private val LightSurface = Color(0xFFF4F6FC)

@Composable
fun AuroraTheme(
    accent: Color = Mint,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = NightInk,
            secondary = Color(0xFF78DCE3),
            background = NightInk,
            onBackground = Color(0xFFF4F6FF),
            surface = NightSurface,
            onSurface = Color(0xFFF4F6FF),
            surfaceVariant = NightVariant,
            onSurfaceVariant = Color(0xFFB6C0D3),
            outline = Color(0xFF39465F),
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = LightInk,
            secondary = Color(0xFF287D83),
            background = Color(0xFFF8F9FC),
            onBackground = LightInk,
            surface = Color.White,
            onSurface = LightInk,
            surfaceVariant = LightSurface,
            onSurfaceVariant = Color(0xFF566176),
            outline = Color(0xFFD4D9E4),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
