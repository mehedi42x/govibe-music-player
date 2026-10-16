// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicRecommendationsTest {
    @Test
    fun listeningArtistsAreRankedFirstWithoutReorderingTies() {
        val tracks = listOf(
            track("1", "Northstar"),
            track("2", "Echo Harbor"),
            track("3", "Northstar"),
            track("4", "Juniper Vale"),
        )

        val ranked = MusicRecommendations.personalize(tracks, listOf("northstar"))

        assertEquals(listOf("1", "3", "2", "4"), ranked.map { it.id })
    }

    @Test
    fun noListeningHistoryLeavesTrendingOrderUntouched() {
        val tracks = listOf(track("1", "Echo Harbor"), track("2", "Northstar"))

        assertEquals(tracks, MusicRecommendations.personalize(tracks, emptyList()))
    }

    private fun track(id: String, artist: String) = CatalogTrack(
        sourceId = "source.test",
        id = id,
        title = "Track $id",
        artist = artist,
        album = "",
        description = "",
        artworkUrl = "",
        streamUrl = "https://audio.example.org/$id.mp3",
        durationMs = 60_000L,
    )
}
