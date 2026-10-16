// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.aurora.musicplayer.data.AudioLibraryRepository
import com.aurora.musicplayer.data.AudioTrackEntity
import com.aurora.musicplayer.data.CatalogTrack
import com.aurora.musicplayer.data.MusicRecommendations
import com.aurora.musicplayer.extensions.ExtensionRepository
import com.aurora.musicplayer.extensions.InstalledExtensionProfile
import com.aurora.musicplayer.extensions.MusicSourceClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppTab(val label: String) {
    HOME("Home"), SEARCH("Search"), LIBRARY("Library"), SETTINGS("Settings"),
}

private enum class PlayerMode { ONLINE, OFFLINE }

private val LocalOpenArtist = staticCompositionLocalOf<(String, Boolean) -> Unit> { _, _ -> }
private val LocalDownloadTrack = staticCompositionLocalOf<(CatalogTrack) -> Unit> { _ -> }
private val LocalDownloadStates = staticCompositionLocalOf<Map<String, DownloadUiState>> { emptyMap() }

private data class DownloadUiState(
    val isDownloading: Boolean = false,
    val progress: Float? = null,
    val isComplete: Boolean = false,
    val error: String? = null,
)

private fun CatalogTrack.downloadKey(): String = "$sourceId:$id"

private data class PlaybackSnapshot(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraApp(
    library: AudioLibraryRepository,
    extensions: ExtensionRepository,
    player: MediaController?,
    onImportAudio: () -> Unit,
    onScanDeviceAudio: () -> Unit,
    onAudioSettingsChange: (Float, Float, Int) -> Unit,
    onPlayQueue: (List<AudioTrackEntity>, Int) -> Unit,
    onPlayRemoteQueue: (List<CatalogTrack>, Int) -> Unit,
    onSkipNext: (Int, Boolean) -> Unit,
    onSkipPrevious: (Int, Boolean) -> Unit,
) {
    val tracks by library.observeTracks().collectAsState(initial = emptyList())
    val favorites by library.observeFavorites().collectAsState(initial = emptyList())
    val localSuggestions by library.observeRecommendations().collectAsState(initial = emptyList())
    val tasteArtists by library.observeTasteArtists().collectAsState(initial = emptyList())
    val installed by extensions.observeInstalled().collectAsState(initial = emptyList())
    val activeProfile by extensions.observeActive().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val playback = rememberPlaybackSnapshot(player)
    val networkAvailable = rememberInternetAvailable()
    var offlineModeSelected by rememberSaveable { mutableStateOf(false) }
    val playerMode = when {
        !networkAvailable -> PlayerMode.OFFLINE
        offlineModeSelected -> PlayerMode.OFFLINE
        else -> PlayerMode.ONLINE
    }
    val sourceClient = remember { MusicSourceClient() }
    val downloadStates = remember { mutableStateMapOf<String, DownloadUiState>() }
    var activeDownloadKey by remember { mutableStateOf<String?>(null) }
    var pendingLegacyDownload by remember { mutableStateOf<CatalogTrack?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    var openedArtist by rememberSaveable { mutableStateOf<String?>(null) }
    var requestArtistSourceSearch by rememberSaveable { mutableStateOf(false) }
    var remoteArtistTracks by remember { mutableStateOf<List<CatalogTrack>>(emptyList()) }
    var artistLoading by remember { mutableStateOf(false) }
    var artistError by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var remoteSearchResults by remember { mutableStateOf<List<CatalogTrack>>(emptyList()) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var trendingTracks by remember { mutableStateOf<List<CatalogTrack>>(emptyList()) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    val prefs = remember(context) { context.getSharedPreferences("nextvibe-ui-settings", 0) }
    var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark-theme", true)) }
    var shuffleEnabled by remember { mutableStateOf(prefs.getBoolean("shuffle-enabled", false)) }
    var repeatMode by remember { mutableStateOf(prefs.getInt("repeat-mode", 0).coerceIn(0, 2)) }
    var playbackVolume by remember { mutableStateOf(prefs.getFloat("playback-volume", 1f).coerceIn(0f, 1f)) }
    var volumeBoostDb by remember { mutableStateOf(prefs.getFloat("volume-boost-db", 0f).coerceIn(0f, 3f)) }
    var equalizerPreset by remember { mutableStateOf(prefs.getInt("equalizer-preset", 0).coerceIn(0, 3)) }
    val activeSourceProfile = activeProfile?.takeIf { it.enabled && it.manifest.source != null }
    val startDownload: (CatalogTrack) -> Unit = { track ->
        val key = track.downloadKey()
        if (activeDownloadKey == null) {
            activeDownloadKey = key
            downloadStates[key] = DownloadUiState(isDownloading = true)
            scope.launch {
                try {
                    library.downloadForOffline(track) { downloaded, total ->
                        val progress = if (total > 0L) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
                        launch { downloadStates[key] = DownloadUiState(isDownloading = true, progress = progress) }
                    }
                    downloadStates[key] = DownloadUiState(isComplete = true)
                } catch (cancelled: CancellationException) {
                    downloadStates.remove(key)
                    throw cancelled
                } catch (error: Exception) {
                    downloadStates[key] = DownloadUiState(error = error.message ?: "Could not save this song for offline playback.")
                } finally {
                    activeDownloadKey = null
                }
            }
        }
    }
    val legacyStoragePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingLegacyDownload
        pendingLegacyDownload = null
        if (pending != null) {
            if (granted) startDownload(pending)
            else downloadStates[pending.downloadKey()] = DownloadUiState(error = "Storage permission is needed to save into Music/NextVibe on this Android version.")
        }
    }
    val onDownloadTrack: (CatalogTrack) -> Unit = { track ->
        val key = track.downloadKey()
        val current = downloadStates[key]
        if (current?.isDownloading != true && current?.isComplete != true && activeDownloadKey == null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingLegacyDownload = track
                legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                startDownload(track)
            }
        }
    }
    val playLocalAndOpen: (List<AudioTrackEntity>, Int) -> Unit = { queue, index ->
        onPlayQueue(queue, index)
        showNowPlaying = true
    }
    val playRemoteAndOpen: (List<CatalogTrack>, Int) -> Unit = { queue, index ->
        onPlayRemoteQueue(queue, index)
        showNowPlaying = true
    }

    LaunchedEffect(activeSourceProfile?.manifest?.id, playerMode) {
        trendingTracks = emptyList()
        feedError = null
        val profile = activeSourceProfile
        if (playerMode == PlayerMode.ONLINE && profile?.manifest?.source != null) {
            feedLoading = true
            try {
                trendingTracks = sourceClient.trending(profile.manifest)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                feedError = error.message ?: "Could not load this source's trending list."
            } finally {
                feedLoading = false
            }
        } else {
            feedLoading = false
        }
    }

    LaunchedEffect(selectedTab, searchQuery, activeSourceProfile?.manifest?.id, playerMode) {
        if (selectedTab != AppTab.SEARCH || searchQuery.isBlank() || playerMode != PlayerMode.ONLINE) {
            remoteSearchResults = emptyList()
            searchError = null
            searchLoading = false
        } else {
            val profile = activeSourceProfile
            if (profile?.manifest?.source == null) {
                remoteSearchResults = emptyList()
                searchError = null
                searchLoading = false
            } else {
                delay(350)
                searchLoading = true
                searchError = null
                try {
                    remoteSearchResults = sourceClient.search(profile.manifest, searchQuery)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    searchError = error.message ?: "Search could not reach this source."
                    remoteSearchResults = emptyList()
                } finally {
                    searchLoading = false
                }
            }
        }
    }

    LaunchedEffect(openedArtist, requestArtistSourceSearch, activeSourceProfile?.manifest?.id, playerMode) {
        remoteArtistTracks = emptyList()
        artistError = null
        val artist = openedArtist
        val profile = activeSourceProfile
        if (artist == null || !requestArtistSourceSearch || playerMode != PlayerMode.ONLINE || profile?.manifest?.source == null) {
            artistLoading = false
        } else {
            artistLoading = true
            try {
                remoteArtistTracks = sourceClient.search(profile.manifest, artist).filter { matchesArtistName(it.artist, artist) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                artistError = error.message ?: "Could not load songs for this artist."
            } finally {
                artistLoading = false
            }
        }
    }

    BackHandler(enabled = openedArtist != null) {
        openedArtist = null
        requestArtistSourceSearch = false
    }

    LaunchedEffect(player, shuffleEnabled, repeatMode) {
        player?.shuffleModeEnabled = shuffleEnabled
        player?.repeatMode = when (repeatMode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    AuroraTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalOpenArtist provides { artist, fromRemoteTrack ->
                openedArtist = artist.trim()
                requestArtistSourceSearch = fromRemoteTrack
            },
            LocalDownloadTrack provides onDownloadTrack,
            LocalDownloadStates provides downloadStates,
        ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NextVibe", fontWeight = FontWeight.Black, letterSpacing = 1.6.sp)
                            Text(
                                if (openedArtist != null) "ARTIST PROFILE"
                                else if (playerMode == PlayerMode.ONLINE) activeSourceProfile?.manifest?.name ?: "ONLINE MUSIC"
                                else if (networkAvailable) "OFFLINE PLAYER" else "OFFLINE · NO CONNECTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            PlayerModeSwitch(
                                mode = playerMode,
                                onlineAvailable = networkAvailable,
                                onSelect = { selected -> offlineModeSelected = selected == PlayerMode.OFFLINE },
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
            },
            bottomBar = {
                Column {
                    if (playback.mediaId != null) {
                        MiniPlayer(
                            playback = playback,
                            onOpen = { showNowPlaying = true },
                            onToggle = { if (player?.isPlaying == true) player.pause() else player?.play() },
                            onNext = { onSkipNext(repeatMode, shuffleEnabled) },
                        )
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        AppTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                AppTab.HOME -> Icons.Default.Home
                                AppTab.SEARCH -> Icons.Default.Search
                                AppTab.LIBRARY -> Icons.Default.LibraryMusic
                                AppTab.SETTINGS -> Icons.Default.Settings
                            }
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { openedArtist = null; requestArtistSourceSearch = false; selectedTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                if (openedArtist != null) ArtistProfileScreen(
                    artist = openedArtist.orEmpty(),
                    localTracks = tracks.filter { matchesArtistName(it.artist, openedArtist.orEmpty()) },
                    remoteTracks = remoteArtistTracks,
                    sourceName = activeSourceProfile?.manifest?.name,
                    sourceSearchAvailable = playerMode == PlayerMode.ONLINE && activeSourceProfile != null,
                    sourceSearchRequested = requestArtistSourceSearch && playerMode == PlayerMode.ONLINE && activeSourceProfile != null,
                    loading = artistLoading,
                    error = artistError,
                    currentId = playback.mediaId,
                    playing = playback.playing,
                    onBack = { openedArtist = null; requestArtistSourceSearch = false },
                    onSearchSource = { requestArtistSourceSearch = true },
                    onPlayLocal = playLocalAndOpen,
                    onPlayRemote = playRemoteAndOpen,
                    onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                ) else when (selectedTab) {
                    AppTab.HOME -> if (playerMode == PlayerMode.OFFLINE) OfflineHomeScreen(
                        tracks = tracks,
                        currentId = playback.mediaId,
                        playing = playback.playing,
                        onScanDeviceAudio = onScanDeviceAudio,
                        onImportAudio = onImportAudio,
                        onPlay = { index -> playLocalAndOpen(tracks, index) },
                        onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                    ) else HomeScreen(
                        tracks = tracks,
                        localSuggestions = localSuggestions,
                        favoritesCount = favorites.size,
                        sourceProfile = activeSourceProfile,
                        trendingTracks = trendingTracks,
                        tasteArtists = tasteArtists,
                        feedLoading = feedLoading,
                        feedError = feedError,
                        onImport = onImportAudio,
                        onSearch = { selectedTab = AppTab.SEARCH },
                        onOpenSettings = { selectedTab = AppTab.SETTINGS },
                        onPlayLocal = { index -> playLocalAndOpen(tracks, index) },
                        onPlayRemote = playRemoteAndOpen,
                        onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                        currentId = playback.mediaId,
                        playing = playback.playing,
                    )
                    AppTab.SEARCH -> SearchScreen(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        localTracks = tracks,
                        remoteTracks = if (playerMode == PlayerMode.ONLINE) remoteSearchResults else emptyList(),
                        activeProfile = activeSourceProfile.takeIf { playerMode == PlayerMode.ONLINE },
                        offlineMode = playerMode == PlayerMode.OFFLINE,
                        loading = searchLoading,
                        error = searchError,
                        currentId = playback.mediaId,
                        playing = playback.playing,
                        onPlayLocal = { selected -> playLocalAndOpen(selected.first, selected.second) },
                        onPlayRemote = playRemoteAndOpen,
                        onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                        onOpenSettings = { selectedTab = AppTab.SETTINGS },
                    )
                    AppTab.LIBRARY -> LibraryScreen(
                        tracks = tracks,
                        currentId = playback.mediaId,
                        playing = playback.playing,
                        onImport = onImportAudio,
                        onScanDeviceAudio = onScanDeviceAudio,
                        onPlay = { index -> playLocalAndOpen(tracks, index) },
                        onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        extensions = extensions,
                        installed = installed,
                        activeProfile = activeProfile,
                        darkTheme = darkTheme,
                        onDarkThemeChange = {
                            darkTheme = it
                            prefs.edit().putBoolean("dark-theme", it).apply()
                        },
                        libraryCount = tracks.size,
                        onClearLibrary = { scope.launch { library.clearLibrary() } },
                        shuffleEnabled = shuffleEnabled,
                        onShuffleEnabledChange = {
                            shuffleEnabled = it
                            prefs.edit().putBoolean("shuffle-enabled", it).apply()
                        },
                        repeatMode = repeatMode,
                        onRepeatModeChange = {
                            repeatMode = it
                            prefs.edit().putInt("repeat-mode", it).apply()
                        },
                        playbackVolume = playbackVolume,
                        onPlaybackVolumeChange = {
                            playbackVolume = it
                            prefs.edit().putFloat("playback-volume", it).apply()
                            onAudioSettingsChange(it, volumeBoostDb, equalizerPreset)
                        },
                        volumeBoostDb = volumeBoostDb,
                        onVolumeBoostChange = {
                            volumeBoostDb = it
                            prefs.edit().putFloat("volume-boost-db", it).apply()
                            onAudioSettingsChange(playbackVolume, it, equalizerPreset)
                        },
                        equalizerPreset = equalizerPreset,
                        onEqualizerPresetChange = {
                            equalizerPreset = it
                            prefs.edit().putInt("equalizer-preset", it).apply()
                            onAudioSettingsChange(playbackVolume, volumeBoostDb, it)
                        },
                    )
                }
            }
        }

        if (showNowPlaying && playback.mediaId != null) {
            ModalBottomSheet(onDismissRequest = { showNowPlaying = false }) {
                NowPlayingPanel(
                    playback = playback,
                    player = player,
                    onClose = { showNowPlaying = false },
                    onSkipNext = { onSkipNext(repeatMode, shuffleEnabled) },
                    onSkipPrevious = { onSkipPrevious(repeatMode, shuffleEnabled) },
                )
            }
        }
        }
    }
}

@Composable
private fun HomeScreen(
    tracks: List<AudioTrackEntity>,
    localSuggestions: List<AudioTrackEntity>,
    favoritesCount: Int,
    sourceProfile: InstalledExtensionProfile?,
    trendingTracks: List<CatalogTrack>,
    tasteArtists: List<String>,
    feedLoading: Boolean,
    feedError: String?,
    onImport: () -> Unit,
    onSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayLocal: (Int) -> Unit,
    onPlayRemote: (List<CatalogTrack>, Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
    currentId: String?,
    playing: Boolean,
) {
    val sortedForYou = remember(trendingTracks, tasteArtists) { MusicRecommendations.personalize(trendingTracks, tasteArtists) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        ),
                    ),
                ).padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                        Text(
                            if (sourceProfile != null) "SOURCE CONNECTED" else "YOUR PERSONAL MUSIC SPACE",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                    }
                    Text("Find your next\nfavourite.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
                    Text(
                        if (sourceProfile != null) "${sourceProfile.manifest.name} brings the music. NextVibe brings the experience."
                        else "Connect a music source to discover trending tracks and search its catalogue.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onSearch) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Search music")
                        }
                        Text("${tracks.size} local · $favoritesCount liked", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        if (localSuggestions.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionHeading("Your listening, your way", "Suggestions ranked on this device")
                localSuggestions.take(4).forEach { track ->
                    TrackRow(
                        track = track,
                        isCurrent = currentId == track.uri,
                        isPlaying = playing && currentId == track.uri,
                        onClick = { onPlayLocal(tracks.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)) },
                        onFavorite = { onFavorite(track) },
                    )
                }
            }
        }

        val source = sourceProfile?.manifest?.source
        if (source == null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No music source yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Add a source in Settings. Sources provide catalogue data only; song cards, artwork, search and playback stay in NextVibe.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onOpenSettings) { Text("Set up a source") }
                }
            }
        } else {
            val artistTaste = tasteArtists.map { it.trim().lowercase() }.toSet()
            val hasTasteMatch = sortedForYou.any { it.artist.trim().lowercase() in artistTaste }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                SectionHeading(
                    if (hasTasteMatch) "Picked for your taste" else "A little of what’s trending",
                    if (tasteArtists.isNotEmpty()) "Ranked from your listening history on this device" else "Your play history will tune these picks over time",
                )
                when {
                    feedLoading -> LoadingCard("Finding tracks from ${sourceProfile.manifest.name}…")
                    feedError != null -> MessageCard(feedError)
                    sortedForYou.isEmpty() -> MessageCard("This source has no tracks to show yet. Check it again later or choose another source in Settings.")
                    else -> sortedForYou.take(5).forEachIndexed { index, track ->
                        RemoteTrackRow(track, onClick = { onPlayRemote(sortedForYou, index) })
                    }
                }
            }
            if (trendingTracks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    SectionHeading("Trending now", "Latest list from ${sourceProfile.manifest.name}")
                    trendingTracks.take(8).forEachIndexed { index, track ->
                        RemoteTrackRow(track, onClick = { onPlayRemote(trendingTracks, index) })
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bring your own music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Choose audio files from your device. NextVibe keeps the library index local.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onImport) { Text("Add local audio") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    localTracks: List<AudioTrackEntity>,
    remoteTracks: List<CatalogTrack>,
    activeProfile: InstalledExtensionProfile?,
    offlineMode: Boolean,
    loading: Boolean,
    error: String?,
    currentId: String?,
    playing: Boolean,
    onPlayLocal: (Pair<List<AudioTrackEntity>, Int>) -> Unit,
    onPlayRemote: (List<CatalogTrack>, Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val localResults = remember(query, localTracks) {
        if (query.isBlank()) emptyList() else localTracks.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) || it.album.contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text("Search songs, artists or albums") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        if (activeProfile?.manifest?.source == null && !offlineMode) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Add a source in Settings to search its catalogue.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onOpenSettings) { Text("Settings") }
                }
            }
        } else if (offlineMode) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                Text("Offline search · this device only", modifier = Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (error != null) MessageCard(error)
        if (loading) LoadingCard("Searching ${activeProfile?.manifest?.name ?: "music source"}…")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp),
        ) {
            if (remoteTracks.isNotEmpty()) {
                item { SectionHeading("From ${activeProfile?.manifest?.name ?: "source"}", "Search results") }
                items(remoteTracks, key = { "remote:${it.sourceId}:${it.id}" }) { track ->
                    RemoteTrackRow(track, onClick = { onPlayRemote(remoteTracks, remoteTracks.indexOf(track)) })
                }
            }
            if (localResults.isNotEmpty()) {
                item { SectionHeading("On this device", "Local library") }
                items(localResults, key = { "local:${it.uri}" }) { track ->
                    val index = localTracks.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
                    TrackRow(
                        track = track,
                        isCurrent = currentId == track.uri,
                        isPlaying = playing && currentId == track.uri,
                        onClick = { onPlayLocal(localTracks to index) },
                        onFavorite = { onFavorite(track) },
                    )
                }
            }
            if (query.isNotBlank() && remoteTracks.isEmpty() && localResults.isEmpty() && !loading && error == null) {
                item { MessageCard("No matches yet. Try another search or check the source settings.") }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<AudioTrackEntity>,
    currentId: String?,
    playing: Boolean,
    onImport: () -> Unit,
    onScanDeviceAudio: () -> Unit,
    onPlay: (Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(tracks, query) {
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) || it.album.contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            label = { Text("Search your library") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row {
                TextButton(onClick = onScanDeviceAudio) { Text("Scan device") }
                TextButton(onClick = onImport) { Text("Pick audio") }
            }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (tracks.isEmpty()) "Your local library is empty" else "No matching tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(filtered, key = { it.uri }) { track ->
                    val index = tracks.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)
                    TrackRow(
                        track = track,
                        isCurrent = currentId == track.uri,
                        isPlaying = playing && currentId == track.uri,
                        onClick = { onPlay(index) },
                        onFavorite = { onFavorite(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteTrackRow(track: CatalogTrack, onClick: () -> Unit) {
    val openArtist = LocalOpenArtist.current
    val downloadTrack = LocalDownloadTrack.current
    val downloadStates = LocalDownloadStates.current
    val downloadState = downloadStates[track.downloadKey()]
    val anotherDownloadInProgress = downloadStates.values.any { it.isDownloading } && downloadState?.isDownloading != true
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkArtwork(track.artworkUrl, Modifier.size(62.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                listOf(track.artist, track.album).filter(String::isNotBlank).joinToString(" · ").ifBlank { "From music source" },
                modifier = Modifier.clickable(enabled = track.artist.isNotBlank()) { openArtist(track.artist, true) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (track.artist.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (track.description.isNotBlank()) {
                Text(track.description, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            when {
                downloadState?.isDownloading == true -> Text(
                    downloadState.progress?.let { "Saving offline · ${(it * 100).toInt()}%" } ?: "Saving offline…",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall,
                )
                downloadState?.isComplete == true -> Text("Saved to Music/NextVibe", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
                downloadState?.error != null -> Text(downloadState.error, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (track.downloadsAllowed) {
            IconButton(
                onClick = { downloadTrack(track) },
                enabled = downloadState?.isDownloading != true && downloadState?.isComplete != true && !anotherDownloadInProgress,
            ) {
                when {
                    downloadState?.isDownloading == true -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    downloadState?.isComplete == true -> Icon(Icons.Default.CheckCircle, contentDescription = "Saved for offline playback", tint = MaterialTheme.colorScheme.secondary)
                    else -> Icon(Icons.Default.Download, contentDescription = "Download ${track.title} for offline playback", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun TrackRow(
    track: AudioTrackEntity,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    val openArtist = LocalOpenArtist.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f))),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    track.artist.ifBlank { "Unknown artist" },
                    modifier = Modifier.weight(1f).clickable(enabled = track.artist.isNotBlank()) { openArtist(track.artist, false) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (track.artist.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(" · ${formatDuration(track.durationMs)}", maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        IconButton(onClick = onFavorite) {
            Icon(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (track.isFavorite) "Remove favorite" else "Add favorite", tint = if (track.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LoadingCard(message: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MessageCard(message: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MiniPlayer(playback: PlaybackSnapshot, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaybackArtwork(playback.artworkUrl, playback.mediaId, Modifier.size(44.dp))
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(playback.title.ifBlank { "Now playing" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(playback.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggle) { Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playback.playing) "Pause" else "Play") }
                IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next track") }
            }
            AnimatedTrackProgress(playbackFraction(playback), playback.playing, Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingPanel(
    playback: PlaybackSnapshot,
    player: MediaController?,
    onClose: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
) {
    val safeDuration = playback.durationMs.coerceAtLeast(1L)
    val animatedPosition by animateFloatAsState(
        targetValue = playback.positionMs.coerceAtMost(safeDuration).toFloat(),
        animationSpec = tween(durationMillis = 360, easing = LinearEasing),
        label = "now-playing-seek",
    )
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PlaybackArtwork(playback.artworkUrl, playback.mediaId, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(playback.title.ifBlank { "Nothing selected" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(playback.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AnimatedTrackProgress(playbackFraction(playback), playback.playing, Modifier.fillMaxWidth())
        Slider(
            value = animatedPosition,
            onValueChange = { player?.seekTo(it.toLong()) },
            valueRange = 0f..safeDuration.toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playback.positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(34.dp)) }
            FloatingActionButton(onClick = { if (playback.playing) player?.pause() else player?.play() }) {
                Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playback.playing) "Pause" else "Play", modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = onSkipNext, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(34.dp)) }
        }
        Text("Playback uses the source URL supplied by the selected source and Android Media3. Device and source compatibility may vary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun rememberPlaybackSnapshot(player: Player?): PlaybackSnapshot {
    var snapshot by remember(player) { mutableStateOf(readPlaybackSnapshot(player)) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = readPlaybackSnapshot(player)
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (player != null) {
            snapshot = readPlaybackSnapshot(player)
            delay(400)
        }
    }
    return snapshot
}

private fun readPlaybackSnapshot(player: Player?): PlaybackSnapshot {
    val media = player?.currentMediaItem
    val metadata = media?.mediaMetadata
    val duration = player?.duration?.takeUnless { it == C.TIME_UNSET || it < 0L } ?: 0L
    return PlaybackSnapshot(
        mediaId = media?.mediaId,
        title = metadata?.title?.toString().orEmpty(),
        artist = metadata?.artist?.toString().orEmpty(),
        artworkUrl = metadata?.artworkUri?.toString().orEmpty(),
        playing = player?.isPlaying == true,
        positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
        durationMs = duration,
    )
}

private fun playbackFraction(playback: PlaybackSnapshot): Float =
    if (playback.durationMs <= 0L) 0f else (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
@Composable
private fun PlayerModeSwitch(mode: PlayerMode, onlineAvailable: Boolean, onSelect: (PlayerMode) -> Unit) {
    Row(
        modifier = Modifier.padding(top = 7.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(PlayerMode.ONLINE to "Online", PlayerMode.OFFLINE to "Offline").forEach { (item, label) ->
            val selected = mode == item
            val enabled = item != PlayerMode.ONLINE || onlineAvailable
            Surface(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable(enabled = enabled) { onSelect(item) },
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    if (item == PlayerMode.ONLINE && !onlineAvailable) "Online · offline" else label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OfflineHomeScreen(
    tracks: List<AudioTrackEntity>,
    currentId: String?,
    playing: Boolean,
    onScanDeviceAudio: () -> Unit,
    onImportAudio: () -> Unit,
    onPlay: (Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)))).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Text("OFFLINE PLAYER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Your music,\nready offline.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, lineHeight = 38.sp)
                    Text("Scan audio indexed by Android on this device, or pick individual files. Your library stays here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onScanDeviceAudio) { Text("Scan device audio") }
                        TextButton(onClick = onImportAudio) { Text("Pick files") }
                    }
                    Text("${tracks.size} local tracks · no network needed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (tracks.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Start with your device library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Android will ask for audio access only when you tap Scan. You can still pick individual files if you decline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item { SectionHeading("On this device", "Recently indexed audio · ${tracks.size} tracks") }
            items(tracks, key = { it.uri }) { track ->
                TrackRow(
                    track = track,
                    isCurrent = currentId == track.uri,
                    isPlaying = playing && currentId == track.uri,
                    onClick = { onPlay(tracks.indexOfFirst { it.uri == track.uri }.coerceAtLeast(0)) },
                    onFavorite = { onFavorite(track) },
                )
            }
        }
    }
}

@Composable
private fun rememberInternetAvailable(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember(context) { context.getSystemService(ConnectivityManager::class.java) }
    var available by remember(manager) { mutableStateOf(hasValidatedInternet(manager)) }
    DisposableEffect(manager) {
        var registered = false
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                available = manager.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                available = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }

            override fun onLost(network: Network) {
                available = hasValidatedInternet(manager)
            }
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }.onSuccess { registered = true }
        onDispose { if (registered) runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return available
}

private fun hasValidatedInternet(manager: ConnectivityManager): Boolean {
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}


@Composable
private fun AnimatedTrackProgress(fraction: Float, isPlaying: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420, easing = LinearEasing),
        label = "mini-player-progress",
    )
    val transition = rememberInfiniteTransition(label = "progress-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(animation = tween(760), repeatMode = RepeatMode.Reverse),
        label = "progress-head-pulse",
    )
    BoxWithConstraints(
        modifier = modifier.height(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
    ) {
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(progress).background(
                Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
                CircleShape,
            ),
        )
        if (progress > 0f) {
            val dotSize = 9.dp
            val dotX = (maxWidth * progress - dotSize * 0.5f).coerceIn(0.dp, (maxWidth - dotSize).coerceAtLeast(0.dp))
            val scale = if (isPlaying) pulse else 1f
            Box(
                Modifier.align(Alignment.CenterStart).offset(x = dotX).size(dotSize)
                    .graphicsLayer { alpha = if (isPlaying) 0.92f else 0.72f; scaleX = scale; scaleY = scale }
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}


@Composable
private fun ArtistProfileScreen(
    artist: String,
    localTracks: List<AudioTrackEntity>,
    remoteTracks: List<CatalogTrack>,
    sourceName: String?,
    sourceSearchAvailable: Boolean,
    sourceSearchRequested: Boolean,
    loading: Boolean,
    error: String?,
    currentId: String?,
    playing: Boolean,
    onBack: () -> Unit,
    onSearchSource: () -> Unit,
    onPlayLocal: (List<AudioTrackEntity>, Int) -> Unit,
    onPlayRemote: (List<CatalogTrack>, Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Back to music") }
            Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Row(
                    Modifier.fillMaxWidth().background(
                        Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))),
                    ).padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(artist.firstOrNull()?.uppercase() ?: "♪", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ARTIST PROFILE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(artist, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${localTracks.size} local songs${if (sourceSearchRequested) " · ${remoteTracks.size} source matches" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (sourceSearchAvailable && !sourceSearchRequested && sourceName != null) {
            item {
                Button(onClick = onSearchSource, modifier = Modifier.fillMaxWidth()) {
                    Text("Find songs in $sourceName")
                }
            }
        }
        if (sourceSearchRequested && sourceName != null) {
            item { SectionHeading("From $sourceName", "Artist matches returned by the selected source (up to 60 per search)") }
            if (loading) item { LoadingCard("Finding songs by $artist…") }
            if (error != null) item { MessageCard(error) }
            itemsIndexed(remoteTracks, key = { _, track -> "remote:${track.sourceId}:${track.id}" }) { index, track ->
                RemoteTrackRow(track) { onPlayRemote(remoteTracks, index) }
            }
        }
        if (localTracks.isNotEmpty()) {
            item { SectionHeading("On this device", "Songs by $artist in your local library") }
            itemsIndexed(localTracks, key = { _, track -> "local:${track.uri}" }) { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = currentId == track.uri,
                    isPlaying = playing && currentId == track.uri,
                    onClick = { onPlayLocal(localTracks, index) },
                    onFavorite = { onFavorite(track) },
                )
            }
        }
        if (localTracks.isEmpty() && remoteTracks.isEmpty() && !loading && error == null && !sourceSearchRequested) {
            item {
                MessageCard(
                    if (sourceSearchAvailable && !sourceSearchRequested) "No local songs for this artist yet. You can also search the selected source."
                    else "No songs for this artist were found in the available library or selected source.",
                )
            }
        }
        if (sourceSearchRequested && !loading && remoteTracks.isEmpty() && error == null) {
            item { MessageCard("The selected source returned no matching artist tracks. Its search API may not expose a complete artist catalogue.") }
        }
    }
}

private fun matchesArtistName(candidate: String, requested: String): Boolean {
    val target = requested.trim()
    if (target.isEmpty()) return false
    if (candidate.trim().equals(target, ignoreCase = true)) return true
    val separators = Regex("""\s*(?:[,;&]|\bfeat(?:uring)?\.?|\bft\.?)\s*""", RegexOption.IGNORE_CASE)
    return candidate.split(separators).any { it.trim().equals(target, ignoreCase = true) }
}
