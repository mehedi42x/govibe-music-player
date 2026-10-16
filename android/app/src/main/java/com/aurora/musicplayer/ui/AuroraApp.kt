package com.aurora.musicplayer.ui

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.aurora.musicplayer.data.AudioLibraryRepository
import com.aurora.musicplayer.data.AudioTrackEntity
import com.aurora.musicplayer.extensions.ExtensionRepository
import com.aurora.musicplayer.extensions.InstalledExtensionProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppTab(val label: String) {
    HOME("Home"), LIBRARY("Library"), EXTENSIONS("Extensions"), SETTINGS("Settings")
}

private data class PlaybackSnapshot(
    val mediaId: String? = null,
    val title: String = "",
    val artist: String = "",
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
    onPlayQueue: (List<AudioTrackEntity>, Int) -> Unit,
) {
    val tracks by library.observeTracks().collectAsState(initial = emptyList())
    val favorites by library.observeFavorites().collectAsState(initial = emptyList())
    val installed by extensions.observeInstalled().collectAsState(initial = emptyList())
    val activeProfile by extensions.observeActive().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val playback = rememberPlaybackSnapshot(player)
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var showNowPlaying by rememberSaveable { mutableStateOf(false) }
    val prefs = remember(context) { context.getSharedPreferences("aurora-ui-settings", 0) }
    var darkTheme by remember { mutableStateOf(prefs.getBoolean("dark-theme", true)) }
    val accent = remember(activeProfile?.manifest?.presentation?.accentColor) {
        runCatching {
            Color(AndroidColor.parseColor(activeProfile?.manifest?.presentation?.accentColor ?: "#B7E36B"))
        }.getOrDefault(Color(0xFFB7E36B))
    }

    AuroraTheme(accent = accent, darkTheme = darkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("GoVibe", fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                            activeProfile?.let {
                                Text(it.manifest.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                            onNext = { player?.seekToNextMediaItem() },
                        )
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        AppTab.entries.forEach { tab ->
                            val icon = when (tab) {
                                AppTab.HOME -> Icons.Default.Home
                                AppTab.LIBRARY -> Icons.Default.LibraryMusic
                                AppTab.EXTENSIONS -> Icons.Default.Extension
                                AppTab.SETTINGS -> Icons.Default.Settings
                            }
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    AppTab.HOME -> HomeScreen(
                        tracks = tracks,
                        favoritesCount = favorites.size,
                        profile = activeProfile,
                        onImport = onImportAudio,
                        onPlay = { index -> onPlayQueue(tracks, index) },
                        onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                        currentId = playback.mediaId,
                        playing = playback.playing,
                    )
                    AppTab.LIBRARY -> LibraryScreen(
                        tracks = tracks,
                        currentId = playback.mediaId,
                        playing = playback.playing,
                        onImport = onImportAudio,
                        onPlay = { index -> onPlayQueue(tracks, index) },
                        onFavorite = { track -> scope.launch { library.setFavorite(track.uri, !track.isFavorite) } },
                    )
                    AppTab.EXTENSIONS -> ExtensionsScreen(extensions = extensions)
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
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    tracks: List<AudioTrackEntity>,
    favoritesCount: Int,
    profile: InstalledExtensionProfile?,
    onImport: () -> Unit,
    onPlay: (Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
    currentId: String?,
    playing: Boolean,
) {
    val presentation = profile?.manifest?.presentation
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
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
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                        ),
                    ),
                ).padding(22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) {
                        Text(presentation?.homeBadge ?: "LOCAL LIBRARY", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(presentation?.homeTitle ?: "Your music, in its own orbit.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(presentation?.homeSubtitle ?: "A private, local-first listening space.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onImport) {
                            Icon(Icons.Default.MusicNote, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Add audio")
                        }
                        Text("${tracks.size} tracks · $favoritesCount favorites", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        if (presentation?.details?.isNotEmpty() == true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading("Profile details", "From the active extension")
                presentation.details.forEach { detail ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Text(detail.label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(0.34f))
                            Text(detail.value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.66f))
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionHeading("Recently added", "Your device only")
            if (tracks.isNotEmpty()) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        if (tracks.isEmpty()) {
            EmptyLibraryCard(onImport)
        } else {
            tracks.take(6).forEachIndexed { index, track ->
                TrackRow(
                    track = track,
                    isCurrent = currentId == track.uri,
                    isPlaying = playing && currentId == track.uri,
                    onClick = { onPlay(index) },
                    onFavorite = { onFavorite(track) },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<AudioTrackEntity>,
    currentId: String?,
    playing: Boolean,
    onImport: () -> Unit,
    onPlay: (Int) -> Unit,
    onFavorite: (AudioTrackEntity) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(tracks, query) {
        if (query.isBlank()) tracks
        else tracks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            singleLine = true,
            label = { Text("Search your library") },
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} tracks", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onImport) { Text("Import audio") }
        }
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyLibraryCard(onImport, message = if (tracks.isEmpty()) "Your library is empty" else "No matching tracks")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
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
private fun EmptyLibraryCard(onImport: () -> Unit, message: String = "Your library is ready when you are") {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
            Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Choose audio files using Android's system picker. GoVibe does not scan all storage.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onImport) { Text("Choose audio files") }
        }
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
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f))),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium)
            Text("${track.artist} · ${formatDuration(track.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
private fun MiniPlayer(playback: PlaybackSnapshot, onOpen: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(playback.title.ifBlank { "Now playing" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(playback.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onToggle) { Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playback.playing) "Pause" else "Play") }
                IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next track") }
            }
            Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))) {
                Box(Modifier.fillMaxWidth(playbackFraction(playback)).height(3.dp).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingPanel(playback: PlaybackSnapshot, player: MediaController?, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f), MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f))),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(84.dp))
        }
        Text(playback.title.ifBlank { "Nothing selected" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(playback.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = playback.positionMs.coerceAtMost(playback.durationMs.coerceAtLeast(1L)).toFloat(),
            onValueChange = { player?.seekTo(it.toLong()) },
            valueRange = 0f..playback.durationMs.coerceAtLeast(1L).toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(playback.positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(playback.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            IconButton(onClick = { player?.seekToPreviousMediaItem() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(34.dp)) }
            FloatingActionButton(onClick = { if (playback.playing) player?.pause() else player?.play() }) {
                Icon(if (playback.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (playback.playing) "Pause" else "Play", modifier = Modifier.size(30.dp))
            }
            IconButton(onClick = { player?.seekToNextMediaItem() }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(34.dp)) }
        }
        Text("Source audio is passed to the device decoder; actual output depends on the Android device and selected route.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val item = player?.currentMediaItem
    return PlaybackSnapshot(
        mediaId = item?.mediaId,
        title = item?.mediaMetadata?.title?.toString().orEmpty(),
        artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
        playing = player?.isPlaying == true,
        positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
        durationMs = player?.duration?.takeIf { it > 0L } ?: 0L,
    )
}

private fun playbackFraction(playback: PlaybackSnapshot): Float =
    if (playback.durationMs <= 0L) 0f else (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f)

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
