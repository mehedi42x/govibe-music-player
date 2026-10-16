// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurora.musicplayer.core.NativePlaybackCore
import com.aurora.musicplayer.extensions.ExtensionManifest
import com.aurora.musicplayer.extensions.ExtensionManifestParser
import com.aurora.musicplayer.extensions.ExtensionRepository
import com.aurora.musicplayer.extensions.GitHubExtensionClient
import com.aurora.musicplayer.extensions.InstalledExtensionProfile
import com.aurora.musicplayer.extensions.RemoteExtensionPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    extensions: ExtensionRepository,
    installed: List<InstalledExtensionProfile>,
    activeProfile: InstalledExtensionProfile?,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    libraryCount: Int,
    onClearLibrary: () -> Unit,
    shuffleEnabled: Boolean,
    onShuffleEnabledChange: (Boolean) -> Unit,
    repeatMode: Int,
    onRepeatModeChange: (Int) -> Unit,
    playbackVolume: Float,
    onPlaybackVolumeChange: (Float) -> Unit,
    volumeBoostDb: Float,
    onVolumeBoostChange: (Float) -> Unit,
    equalizerPreset: Int,
    onEqualizerPresetChange: (Int) -> Unit,
) {
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        SettingsToggleCard(
            title = "Dark appearance",
            description = "A calm, deep-colour listening space.",
            checked = darkTheme,
            onCheckedChange = onDarkThemeChange,
        )
        AudioSettingsCard(
            volume = playbackVolume,
            onVolumeChange = onPlaybackVolumeChange,
            boostDb = volumeBoostDb,
            onBoostChange = onVolumeBoostChange,
            equalizerPreset = equalizerPreset,
            onEqualizerPresetChange = onEqualizerPresetChange,
        )
        ExtensionManagerSection(extensions, installed, activeProfile)
        SettingsToggleCard(
            title = "Shuffle",
            description = "Randomize playback navigation.",
            checked = shuffleEnabled,
            onCheckedChange = onShuffleEnabledChange,
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Repeat", fontWeight = FontWeight.SemiBold)
                listOf(
                    NativePlaybackCore.REPEAT_OFF to "Off",
                    NativePlaybackCore.REPEAT_ALL to "All tracks",
                    NativePlaybackCore.REPEAT_ONE to "Current track",
                ).forEach { (mode, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = repeatMode == mode, onClick = { onRepeatModeChange(mode) })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text("Queue navigation uses the app's local playback core.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Privacy and source access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("NextVibe sends catalogue requests only to the source you select. Your search terms go to that source; listening history and personal recommendations stay on this device. Audio-read permission is requested only for device scans. Downloads go to Music/NextVibe; Android 10+ uses scoped MediaStore, while Android 9 and below ask for write access only when you download.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("A download button appears only when the selected source manifest declares downloadsAllowed=true. That declaration is the source's claim, not a legal check. Sources cannot run code, request credentials, read your library, or replace NextVibe screens; no provider is bundled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$libraryCount indexed local audio entries, including device scans and offline copies. Clearing removes the app's index and listening history; original files and completed copies in Music/NextVibe are not deleted.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { showClearDialog = true }, enabled = libraryCount > 0) { Text("Clear library and history") }
            }
        }
        Spacer(Modifier.height(12.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear library and history?") },
            text = { Text("This removes local library index entries plus local and source listening history. Original audio files, completed downloads in Music/NextVibe and installed sources are not deleted.") },
            confirmButton = {
                Button(onClick = {
                    onClearLibrary()
                    showClearDialog = false
                }) { Text("Clear data") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AudioSettingsCard(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    boostDb: Float,
    onBoostChange: (Float) -> Unit,
    equalizerPreset: Int,
    onEqualizerPresetChange: (Int) -> Unit,
) {
    val presets = listOf(0 to "Flat", 1 to "Bass", 2 to "Vocal", 3 to "Treble")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Audio controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Player volume · ${(volume * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            Slider(value = volume, onValueChange = onVolumeChange, valueRange = 0f..1f, steps = 19)
            Text("Volume boost · $boostDb dB", style = MaterialTheme.typography.bodySmall)
            Slider(value = boostDb, onValueChange = onBoostChange, valueRange = 0f..3f, steps = 5)
            Text("Boost is off by default and capped at +3 dB. Higher gain can clip audio; start low and reduce device volume if needed.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("Equalizer", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            presets.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (value, label) ->
                        FilterChip(
                            selected = equalizerPreset == value,
                            onClick = { onEqualizerPresetChange(value) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Text("Equalizer effects depend on Android device support and apply to this player's audio session.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsToggleCard(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ExtensionManagerSection(
    extensions: ExtensionRepository,
    installed: List<InstalledExtensionProfile>,
    activeProfile: InstalledExtensionProfile?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importer = remember { GitHubExtensionClient() }
    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var remote by remember { mutableStateOf<List<RemoteExtensionPreview>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf(ExtensionManifestParser.template()) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var editorPreview by remember { mutableStateOf<ExtensionManifest?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val enabledSources = installed.filter { it.enabled && it.manifest.source != null }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Music sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Choose a catalogue; NextVibe owns the screens and playback controls.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                editorText = ExtensionManifestParser.template()
                editorError = null
                editorPreview = null
                editorOpen = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add a music source")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Add a source from GitHub", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("Scan a public repository's nextvibe-sources/ folder for JSON manifests. Each result is previewed before installation; nothing is added until you choose it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = repositoryUrl,
                    onValueChange = { repositoryUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("https://github.com/owner/repository") },
                    placeholder = { Text("Public repository URL") },
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            loading = true
                            scanError = null
                            remote = emptyList()
                            scope.launch {
                                try {
                                    remote = importer.scan(repositoryUrl)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (error: Exception) {
                                    scanError = error.message ?: "Could not scan this repository."
                                } finally {
                                    loading = false
                                }
                            }
                        },
                        enabled = !loading && repositoryUrl.isNotBlank(),
                    ) {
                        if (loading) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(if (loading) "Scanning" else "Scan repository", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text("Public repos only · no sign-in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                scanError?.let { InlineMessage(it, isError = true) }
                if (remote.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Found ${remote.size} manifest${if (remote.size == 1) "" else "s"}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    remote.forEach { preview ->
                        RemoteSourceCard(
                            preview = preview,
                            onAdd = {
                                val raw = preview.rawJson ?: return@RemoteSourceCard
                                scope.launch {
                                    try {
                                        extensions.installCommunity(raw, preview.sourceRepo)
                                        Toast.makeText(context, "${preview.manifest?.name} added", Toast.LENGTH_SHORT).show()
                                    } catch (error: Exception) {
                                        scanError = error.message ?: "Could not add this source."
                                    }
                                }
                            },
                        )
                    }
                }
                Text("GitHub public API limits apply. A scan checks up to 20 small JSON files and does not run downloaded code.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Active catalogue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = activeProfile == null || activeProfile.manifest.source == null, onClick = { extensions.clearActive() })
                    Column(Modifier.weight(1f)) {
                        Text("No remote source", fontWeight = FontWeight.Medium)
                        Text("Keep Home focused on your local library.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (enabledSources.isEmpty()) {
                    Text("No source installed. Add a JSON manifest above; no music provider comes preinstalled.", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                enabledSources.forEach { profile ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = profile.manifest.id == activeProfile?.manifest?.id,
                            onClick = { scope.launch { runCatching { extensions.setActive(profile.manifest.id) }.onFailure { actionError = it.message } } },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(profile.manifest.name, fontWeight = FontWeight.Medium)
                            Text("${profile.manifest.author} · v${profile.manifest.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (profile.manifest.id == activeProfile?.manifest?.id) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Selected source", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        if (installed.isNotEmpty()) {
            Text("Installed sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            installed.forEach { profile ->
                InstalledSourceCard(
                    profile = profile,
                    active = profile.manifest.id == activeProfile?.manifest?.id,
                    onEnabledChange = { enabled ->
                        scope.launch { runCatching { extensions.setEnabled(profile.manifest.id, enabled) }.onFailure { actionError = it.message } }
                    },
                    onRemove = {
                        scope.launch { runCatching { extensions.uninstall(profile.manifest.id) }.onFailure { actionError = it.message } }
                    },
                )
            }
        }
        actionError?.let { InlineMessage(it, isError = true) }
        SourceSafetyNotice()
    }

    if (editorOpen) {
        AlertDialog(
            onDismissRequest = { editorOpen = false },
            title = { Text("Add source manifest") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                    Text("This JSON describes HTTPS search and trending endpoints using the NextVibe song-data contract. It cannot provide screens or executable code.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = editorText,
                        onValueChange = { editorText = it; editorError = null; editorPreview = null },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 390.dp),
                        label = { Text(".aurora.json manifest") },
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                    editorPreview?.let { ManifestSummary(it) }
                    editorError?.let { InlineMessage(it, isError = true) }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        try {
                            editorPreview = ExtensionManifestParser.parse(editorText)
                            editorError = null
                        } catch (error: Exception) {
                            editorPreview = null
                            editorError = error.message ?: "Manifest validation failed."
                        }
                    }) { Text("Validate") }
                    Button(
                        enabled = editorPreview?.source != null,
                        onClick = {
                            val manifest = editorPreview ?: return@Button
                            scope.launch {
                                try {
                                    extensions.installCommunity(editorText, "Manual JSON")
                                    Toast.makeText(context, "${manifest.name} added", Toast.LENGTH_SHORT).show()
                                    editorOpen = false
                                } catch (error: Exception) {
                                    editorError = error.message ?: "Could not add this source."
                                }
                            }
                        },
                    ) { Text("Add source") }
                }
            },
            dismissButton = { TextButton(onClick = { editorOpen = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RemoteSourceCard(preview: RemoteExtensionPreview, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (preview.manifest == null) {
                Text(preview.fileName, fontWeight = FontWeight.SemiBold)
                InlineMessage(preview.error ?: "Invalid source manifest.", isError = true)
            } else {
                ManifestSummary(preview.manifest)
                Text("File: ${preview.fileName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onAdd, modifier = Modifier.align(Alignment.End), enabled = preview.manifest.source != null) { Text("Add source") }
            }
        }
    }
}

@Composable
private fun InstalledSourceCard(profile: InstalledExtensionProfile, active: Boolean, onEnabledChange: (Boolean) -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Extension, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(profile.manifest.name, fontWeight = FontWeight.SemiBold)
                    Text("${profile.manifest.author} · v${profile.manifest.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = profile.enabled, onCheckedChange = onEnabledChange)
            }
            Text(profile.manifest.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (profile.manifest.source == null) "Legacy display profile — not a music source."
                else "Source: ${profile.manifest.source.baseUrl}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profile.manifest.source != null) {
                Text(
                    "Offline downloads: ${if (profile.manifest.source.downloadsAllowed) "allowed by source manifest" else "not enabled"} · source declaration is not a rights verification",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onRemove) { Text("Remove") }
                if (active) {
                    FilledTonalButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.CheckCircle, null)
                        Text("Active", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ManifestSummary(manifest: ExtensionManifest) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(manifest.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text("${manifest.author} · v${manifest.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(manifest.description, style = MaterialTheme.typography.bodySmall)
        Text(
            manifest.source?.let { "HTTPS catalogue · search + trending" } ?: "Legacy display-only manifest",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        manifest.source?.let {
            Text(
                if (it.downloadsAllowed) "Offline download requests enabled by this source" else "Offline downloads disabled by this source",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceSafetyNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Source-only by design", fontWeight = FontWeight.SemiBold)
                Text("A source returns a tracks array with stable IDs, title, artist, album, description, HTTPS artwork and HTTPS playable URL. NextVibe renders the list, thumbnail, search and player; manifests cannot inject UI, code, credentials or local-file access.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InlineMessage(message: String, isError: Boolean) {
    Surface(color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
