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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import kotlinx.coroutines.launch

@Composable
fun ExtensionsScreen(extensions: ExtensionRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed by extensions.observeInstalled().collectAsState(initial = emptyList())
    val active by extensions.observeActive().collectAsState(initial = null)
    val importer = remember { GitHubExtensionClient() }
    var repositoryUrl by rememberSaveable { mutableStateOf("") }
    var remote by remember { mutableStateOf<List<RemoteExtensionPreview>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var editorText by remember { mutableStateOf(ExtensionManifestParser.template()) }
    var editorError by remember { mutableStateOf<String?>(null) }
    var editorPreview by remember { mutableStateOf<ExtensionManifest?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Extension studio", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("One .aurora.json file adds one inspectable home profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                editorText = ExtensionManifestParser.template()
                editorError = null
                editorPreview = null
                editorOpen = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add extension from JSON")
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Browse a public GitHub repository", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("GoVibe scans aurora-extensions/ for separate .aurora.json files. Nothing is downloaded until you scan; each valid file is previewed before you add it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Public repositories only · no GitHub sign-in", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                scanError?.let { InlineMessage(it, isError = true) }
                if (remote.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Found ${remote.size} extension file${if (remote.size == 1) "" else "s"}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    remote.forEach { preview ->
                        RemoteExtensionCard(
                            preview = preview,
                            onAdd = {
                                val manifestJson = preview.rawJson ?: return@RemoteExtensionCard
                                scope.launch {
                                    try {
                                        extensions.installCommunity(manifestJson, preview.sourceRepo)
                                        Toast.makeText(context, "${preview.manifest?.name} added", Toast.LENGTH_SHORT).show()
                                    } catch (error: Exception) {
                                        scanError = error.message ?: "Could not add this extension."
                                    }
                                }
                            },
                        )
                    }
                }
                Text("GitHub's unauthenticated API is rate-limited. A scan is capped at 20 extension files and is only started when you tap Scan.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text("Installed extensions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (installed.isEmpty()) {
            Text("No extensions are installed. GoVibe uses its own default home; add a profile with + or scan a public GitHub repository.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            installed.forEach { profile ->
                InstalledExtensionCard(
                    profile = profile,
                    isActive = profile.manifest.id == active?.manifest?.id,
                    onEnabledChange = { enabled ->
                        scope.launch {
                            runCatching { extensions.setEnabled(profile.manifest.id, enabled) }
                                .onFailure { scanError = it.message }
                        }
                    },
                    onSetActive = {
                        scope.launch {
                            runCatching { extensions.setActive(profile.manifest.id) }
                                .onFailure { scanError = it.message }
                        }
                    },
                    onUninstall = {
                        scope.launch {
                            runCatching { extensions.uninstall(profile.manifest.id) }
                                .onFailure { scanError = it.message }
                        }
                    },
                )
            }
        }
        SafeFormatNotice()
        Spacer(Modifier.height(12.dp))
    }

    if (editorOpen) {
        AlertDialog(
            onDismissRequest = { editorOpen = false },
            title = { Text("Add from manifest") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState())) {
                    Text("Edit the JSON template below. This is a declarative UI profile, not executable Kotlin or JavaScript.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        enabled = editorPreview != null,
                        onClick = {
                            val manifest = editorPreview ?: return@Button
                            scope.launch {
                                try {
                                    extensions.installCommunity(editorText, "Manual JSON")
                                    Toast.makeText(context, "${manifest.name} added", Toast.LENGTH_SHORT).show()
                                    editorOpen = false
                                } catch (error: Exception) {
                                    editorError = error.message ?: "Could not add this extension."
                                }
                            }
                        },
                    ) { Text("Add") }
                }
            },
            dismissButton = { TextButton(onClick = { editorOpen = false }) { Text("Cancel") } },
        )
    }
}

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
) {
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Dark appearance", fontWeight = FontWeight.SemiBold)
                    Text("Use GoVibe's dark-first palette.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = darkTheme, onCheckedChange = onDarkThemeChange)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Shuffle", fontWeight = FontWeight.SemiBold)
                    Text("Randomize playback navigation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = shuffleEnabled, onCheckedChange = onShuffleEnabledChange)
            }
        }

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
                Text("Queue navigation is calculated by GoVibe's Rust playback core.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Home appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Choose an installed profile or return to GoVibe's own app home. No extension is installed by default.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = activeProfile == null,
                        onClick = { extensions.clearActive() },
                    )
                    Column(Modifier.weight(1f).padding(start = 8.dp)) {
                        Text("GoVibe default home", fontWeight = FontWeight.SemiBold)
                        Text("The app's own layout; not an extension.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            installed.forEach { profile ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = profile.manifest.id == activeProfile?.manifest?.id,
                            onClick = {
                                if (profile.enabled) scope.launch {
                                    runCatching { extensions.setActive(profile.manifest.id) }.onFailure { actionError = it.message }
                                }
                            },
                            enabled = profile.enabled,
                        )
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(profile.manifest.name, fontWeight = FontWeight.SemiBold)
                            Text("${profile.manifest.author} · v${profile.manifest.version}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(profile.manifest.presentation.homeSubtitle, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = profile.enabled, onCheckedChange = { enabled -> scope.launch {
                            runCatching { extensions.setEnabled(profile.manifest.id, enabled) }.onFailure { actionError = it.message }
                        } })
                    }
                }
            }
        }

        activeProfile?.let { profile ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Active profile information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ManifestSummary(profile.manifest)
                    Text("Installed from: ${profile.sourceRepo}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Audio and privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Text("GoVibe plays user-selected local audio through Android Media3. It does not re-encode the source in-app; actual output depends on the device decoder and audio route. Bit-perfect output is not measured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("The only network feature is an explicit public GitHub extension scan. No token, account cookie, or audio-file upload is used.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Local library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("$libraryCount imported audio entries. Removing the library clears GoVibe's index and history; the original files are not deleted.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { showClearDialog = true }, enabled = libraryCount > 0) { Text("Clear library index") }
            }
        }
        actionError?.let { InlineMessage(it, isError = true) }
        Spacer(Modifier.height(16.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear library index?") },
            text = { Text("This removes imported track entries and listening history from GoVibe. Your original audio files will not be deleted.") },
            confirmButton = {
                Button(onClick = {
                    onClearLibrary()
                    showClearDialog = false
                }) { Text("Clear index") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun RemoteExtensionCard(preview: RemoteExtensionPreview, onAdd: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (preview.manifest == null) {
                Text(preview.fileName, fontWeight = FontWeight.SemiBold)
                InlineMessage(preview.error ?: "Invalid extension file.", isError = true)
            } else {
                ManifestSummary(preview.manifest)
                Text("File: ${preview.fileName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onAdd, modifier = Modifier.align(Alignment.End)) { Text("Add extension") }
            }
        }
    }
}

@Composable
private fun InstalledExtensionCard(
    profile: InstalledExtensionProfile,
    isActive: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSetActive: () -> Unit,
    onUninstall: () -> Unit,
) {
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onUninstall) { Text("Remove") }
                if (isActive) {
                    FilledTonalButton(onClick = {}, enabled = false) {
                        Icon(Icons.Default.CheckCircle, null)
                        Text("Active", modifier = Modifier.padding(start = 6.dp))
                    }
                } else {
                    TextButton(onClick = onSetActive, enabled = profile.enabled) { Text("Set as home") }
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
        Text("Home title: ${manifest.presentation.homeTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Accent: ${manifest.presentation.accentColor} · ${manifest.capabilities.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        manifest.presentation.details.forEach { detail ->
            Text("${detail.label}: ${detail.value}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SafeFormatNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Constrained by design", fontWeight = FontWeight.SemiBold)
                Text("Extensions can set an accent color and plain-text home details only. They cannot run code, read your music, request credentials, call arbitrary APIs, or provide audio streams.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
