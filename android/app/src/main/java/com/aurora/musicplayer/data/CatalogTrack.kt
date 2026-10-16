// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

data class CatalogTrack(
    val sourceId: String,
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val description: String,
    val artworkUrl: String,
    val streamUrl: String,
    val durationMs: Long,
    val downloadsAllowed: Boolean = false,
)

/** Local ranking only: listening artists are never sent to a music source. */
object MusicRecommendations {
    fun personalize(tracks: List<CatalogTrack>, tasteArtists: List<String>): List<CatalogTrack> {
        if (tracks.isEmpty() || tasteArtists.isEmpty()) return tracks
        val preferred = tasteArtists.map { it.normalizedArtist() }.filter { it.isNotEmpty() }.toSet()
        return tracks.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<CatalogTrack>> { it.value.artist.normalizedArtist() in preferred }
                    .thenBy { it.index },
            )
            .map { it.value }
    }

    private fun String.normalizedArtist(): String = trim().lowercase().replace(Regex("\\s+"), " ")
}
