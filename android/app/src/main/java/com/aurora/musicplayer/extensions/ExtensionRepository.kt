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
    private val preferences = context.getSharedPreferences("aurora-extension-settings", Context.MODE_PRIVATE)
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
        profiles.firstOrNull { it.manifest.id == selectedId && it.enabled }
    }

    /** Removes profiles bundled by older development builds; new installs never seed extensions. */
    suspend fun cleanupLegacyBundledProfiles() {
        dao.removeBuiltIns()
        val selected = activeId.value ?: return
        if (dao.find(selected)?.enabled != true) clearActive()
    }

    suspend fun installCommunity(rawJson: String, sourceRepo: String): ExtensionManifest {
        val manifest = ExtensionManifestParser.parse(rawJson)
        require(!manifest.id.startsWith(RESERVED_ID_PREFIX)) {
            "Community extensions may not use GoVibe's reserved org.aurora.* IDs."
        }
        require(dao.find(manifest.id) == null) {
            "This extension ID is already installed. Uninstall it before adding another copy."
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
        require(dao.find(id) != null) { "Extension is not installed." }
        dao.setEnabled(id, enabled)
        if (!enabled && activeId.value == id) clearActive()
    }

    suspend fun setActive(id: String) {
        val profile = dao.find(id)?.let(::toProfile)
            ?: throw IllegalArgumentException("Extension is not installed.")
        require(profile.enabled) { "Enable this extension before selecting it as the active home profile." }
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
        private const val KEY_ACTIVE_EXTENSION = "active-extension-id"
        private const val RESERVED_ID_PREFIX = "org.aurora."
    }
}
