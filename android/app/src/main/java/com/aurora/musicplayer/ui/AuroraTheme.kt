// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DeepSpace = Color(0xFF080D19)
private val NightSurface = Color(0xFF111A2A)
private val NightElevated = Color(0xFF19243A)
private val Aqua = Color(0xFF69E7D5)
private val Sky = Color(0xFF7D9BFF)
private val Lime = Color(0xFFD3F879)
private val LightInk = Color(0xFF182237)
private val LightSurface = Color(0xFFF0F4FA)

@Composable
fun AuroraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = Aqua,
            onPrimary = DeepSpace,
            secondary = Sky,
            onSecondary = DeepSpace,
            tertiary = Lime,
            background = DeepSpace,
            onBackground = Color(0xFFF4F7FF),
            surface = NightSurface,
            onSurface = Color(0xFFF4F7FF),
            surfaceVariant = NightElevated,
            onSurfaceVariant = Color(0xFFB4C0D4),
            outline = Color(0xFF37445B),
            error = Color(0xFFFF8291),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF087E79),
            onPrimary = Color.White,
            secondary = Color(0xFF435CC4),
            onSecondary = Color.White,
            tertiary = Color(0xFF4A6B08),
            background = Color(0xFFF6F8FC),
            onBackground = LightInk,
            surface = Color.White,
            onSurface = LightInk,
            surfaceVariant = LightSurface,
            onSurfaceVariant = Color(0xFF59667C),
            outline = Color(0xFFD5DCE8),
            error = Color(0xFFBA1A1A),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
