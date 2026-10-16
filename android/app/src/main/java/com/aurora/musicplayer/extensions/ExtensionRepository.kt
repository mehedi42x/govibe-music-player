// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.extensions

import android.content.Context
import com.aurora.musicplayer.data.ExtensionDao
import com.aurora.musicplayer.data.InstalledExtensionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ExtensionRepository(
    context: Context,
    private val dao: ExtensionDao,
) {
    private val preferences = context.getSharedPreferences("nextvibe-source-settings", Context.MODE_PRIVATE)
    private val storedActiveId = preferences.getString(KEY_ACTIVE_EXTENSION, null)
    private val activeId = MutableStateFlow(storedActiveId?.takeUnless { it.startsWith(RESERVED_ID_PREFIX) })

    init {
        if (storedActiveId?.startsWith(RESERVED_ID_PREFIX) == true) {
            preferences.edit().remove(KEY_ACTIVE_EXTENSION).apply()
        }
    }

    fun observeInstalled(): Flow<List<InstalledExtensionProfile>> = dao.observeAll().map { rows ->
        rows.mapNotNull(::toProfile)
    }

    fun observeActive(): Flow<InstalledExtensionProfile?> = combine(dao.observeAll(), activeId) { rows, selectedId ->
        val profiles = rows.mapNotNull(::toProfile)
        profiles.firstOrNull { it.manifest.id == selectedId && it.enabled && it.manifest.source != null }
    }

    /** Removes profiles bundled by older development builds; fresh installs never seed sources. */
    suspend fun cleanupLegacyBundledProfiles() {
        dao.removeBuiltIns()
        val selected = activeId.value ?: return
        val validSource = dao.find(selected)?.let(::toProfile)?.let {
            it.enabled && it.manifest.source != null
        } == true
        if (!validSource) clearActive()
    }

    suspend fun installCommunity(rawJson: String, sourceRepo: String): ExtensionManifest {
        val manifest = ExtensionManifestParser.parse(rawJson)
        require(manifest.source != null && "music.source" in manifest.capabilities) {
            "Only declarative music.source manifests can be installed. Legacy presentation profiles are read-only."
        }
        require(!manifest.id.startsWith(RESERVED_ID_PREFIX)) {
            "Community sources may not use NextVibe's reserved org.aurora.* IDs."
        }
        require(dao.find(manifest.id) == null) {
            "This source ID is already installed. Remove it before adding another copy."
        }
        dao.put(
            InstalledExtensionEntity(
                id = manifest.id,
                manifestJson = rawJson,
                sourceRepo = sourceRepo,
                builtIn = false,
                enabled = true,
            ),
        )
        return manifest
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        require(dao.find(id) != null) { "Source is not installed." }
        dao.setEnabled(id, enabled)
        if (!enabled && activeId.value == id) clearActive()
    }

    suspend fun setActive(id: String) {
        val profile = dao.find(id)?.let(::toProfile)
            ?: throw IllegalArgumentException("Source is not installed.")
        require(profile.enabled) { "Enable this source before selecting it." }
        require(profile.manifest.source != null) { "Only a music source can be selected for the Home feed." }
        activeId.value = id
        preferences.edit().putString(KEY_ACTIVE_EXTENSION, id).apply()
    }

    fun clearActive() {
        activeId.value = null
        preferences.edit().remove(KEY_ACTIVE_EXTENSION).apply()
    }

    suspend fun uninstall(id: String) {
        if (dao.find(id) == null) return
        dao.uninstall(id)
        if (activeId.value == id) clearActive()
    }

    private fun toProfile(row: InstalledExtensionEntity): InstalledExtensionProfile? = runCatching {
        InstalledExtensionProfile(
            manifest = ExtensionManifestParser.parse(row.manifestJson),
            sourceRepo = row.sourceRepo,
            enabled = row.enabled,
            installedAt = row.installedAt,
        )
    }.getOrNull()

    companion object {
        private const val KEY_ACTIVE_EXTENSION = "active-source-id"
        private const val RESERVED_ID_PREFIX = "org.aurora."
    }
}
