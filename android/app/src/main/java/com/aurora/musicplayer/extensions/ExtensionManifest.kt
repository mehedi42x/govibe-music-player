// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.extensions

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.nio.charset.StandardCharsets

private const val MAX_MANIFEST_BYTES = 64 * 1024
private val ID_PATTERN = Regex("^[a-z0-9]+(?:[.-][a-z0-9]+)+$")
private val VERSION_PATTERN = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?$")
private val COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
private val ALLOWED_CAPABILITIES = setOf("ui.home", "theme.accent", "metadata.display", "music.source")

/** Declarative manifest only. A source describes bounded HTTPS JSON endpoints; it cannot supply app UI or code. */
data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val capabilities: Set<String>,
    val presentation: ExtensionPresentation,
    val source: MusicSourceDefinition? = null,
)

data class MusicSourceDefinition(
    val baseUrl: String,
    val trendingPath: String,
    val searchPath: String,
    val downloadsAllowed: Boolean = false,
)

data class ExtensionPresentation(
    val accentColor: String,
    val homeTitle: String,
    val homeSubtitle: String,
    val homeBadge: String,
    val details: List<ExtensionDetail>,
)

data class ExtensionDetail(val label: String, val value: String)

data class InstalledExtensionProfile(
    val manifest: ExtensionManifest,
    val sourceRepo: String,
    val enabled: Boolean,
    val installedAt: Long,
)

object ExtensionManifestParser {
    private val rootKeys = setOf(
        "format", "formatVersion", "id", "name", "version", "author", "description",
        "capabilities", "presentation", "source",
    )
    private val presentationKeys = setOf("accentColor", "homeTitle", "homeSubtitle", "homeBadge", "details")
    private val detailKeys = setOf("label", "value")
    private val sourceKeys = setOf("baseUrl", "trendingPath", "searchPath", "downloadsAllowed")

    fun parse(rawJson: String): ExtensionManifest {
        require(rawJson.toByteArray(StandardCharsets.UTF_8).size <= MAX_MANIFEST_BYTES) {
            "Extension file is larger than 64 KiB."
        }
        val root = JSONObject(rawJson)
        rejectUnknownKeys(root, rootKeys, "manifest")
        require(root.optString("format") == "aurora-extension") {
            "Unsupported extension format. Expected aurora-extension."
        }
        require(root.optInt("formatVersion", -1) == 1) { "Unsupported extension format version." }

        val id = requiredText(root, "id", 100)
        require(ID_PATTERN.matches(id)) { "Extension ID must use lowercase names separated by dots or hyphens." }
        val name = requiredText(root, "name", 60)
        val version = requiredText(root, "version", 40)
        require(VERSION_PATTERN.matches(version)) { "Version must use semantic version form, such as 1.0.0." }
        val author = requiredText(root, "author", 80)
        val description = requiredText(root, "description", 500)

        val capabilityArray = root.optJSONArray("capabilities")
            ?: throw IllegalArgumentException("capabilities must be a JSON array.")
        require(capabilityArray.length() in 1..ALLOWED_CAPABILITIES.size) {
            "Declare between one and ${ALLOWED_CAPABILITIES.size} supported capabilities."
        }
        val capabilities = buildSet {
            for (index in 0 until capabilityArray.length()) {
                val capability = capabilityArray.optString(index)
                require(capability in ALLOWED_CAPABILITIES) { "Unsupported capability: $capability" }
                require(add(capability)) { "Duplicate capability: $capability" }
            }
        }

        val source = if (root.has("source")) parseSource(
            root.optJSONObject("source") ?: throw IllegalArgumentException("source must be a JSON object."),
        ) else null
        require(("music.source" in capabilities) == (source != null)) {
            "Declare music.source and provide its source settings together."
        }

        // Keep reading old profile manifests for stored-data compatibility. NextVibe renders its own UI
        // and ignores these legacy presentation values; newly created manifests use only music.source.
        val presentation = if (root.has("presentation")) {
            parseLegacyPresentation(
                root.optJSONObject("presentation")
                    ?: throw IllegalArgumentException("presentation must be a JSON object."),
                name,
                description,
            )
        } else {
            ExtensionPresentation("#69E7D5", name, description, "SOURCE", emptyList())
        }

        return ExtensionManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            capabilities = capabilities,
            presentation = presentation,
            source = source,
        )
    }

    fun template(): String = """
        {
          "format": "aurora-extension",
          "formatVersion": 1,
          "id": "org.example.my-source",
          "name": "My music source",
          "version": "1.0.0",
          "author": "Your name",
          "description": "A user-added source for song lists and search.",
          "capabilities": ["music.source"],
          "source": {
            "baseUrl": "https://api.example.org",
            "trendingPath": "/v1/trending",
            "searchPath": "/v1/search?q={query}",
            "downloadsAllowed": false
          }
        }
    """.trimIndent()

    private fun parseSource(json: JSONObject): MusicSourceDefinition {
        rejectUnknownKeys(json, sourceKeys, "source")
        val baseUrl = requiredText(json, "baseUrl", 300).trimEnd('/')
        validateBaseUrl(baseUrl)
        val trendingPath = requiredText(json, "trendingPath", 300)
        val searchPath = requiredText(json, "searchPath", 300)
        validateEndpointPath(trendingPath, allowQueryPlaceholder = false)
        validateEndpointPath(searchPath, allowQueryPlaceholder = true)
        require(searchPath.count { it == '{' } == 1 && searchPath.count { it == '}' } == 1 && searchPath.contains("{query}")) {
            "source.searchPath must contain exactly one {query} placeholder."
        }
        val downloadsAllowed = if (json.has("downloadsAllowed")) {
            require(json.get("downloadsAllowed") is Boolean) { "source.downloadsAllowed must be a boolean." }
            json.getBoolean("downloadsAllowed")
        } else {
            false
        }
        return MusicSourceDefinition(baseUrl, trendingPath, searchPath, downloadsAllowed)
    }

    private fun validateBaseUrl(value: String) {
        val uri = runCatching { URI(value) }.getOrElse {
            throw IllegalArgumentException("source.baseUrl must be a valid public HTTPS origin.")
        }
        val host = uri.host?.lowercase().orEmpty()
        require(uri.scheme.equals("https", ignoreCase = true) && host.isNotBlank()) {
            "Music sources must use HTTPS."
        }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null && uri.port in listOf(-1, 443)) {
            "source.baseUrl may not contain credentials, a query, a fragment, or a nonstandard port."
        }
        require(uri.path.isNullOrEmpty() || uri.path == "/") { "source.baseUrl must be an origin only; put paths in trendingPath and searchPath." }
        require(!isLocalOrIpHost(host)) { "Local, private, and IP-literal source hosts are not supported." }
    }

    private fun validateEndpointPath(value: String, allowQueryPlaceholder: Boolean) {
        require(value.startsWith('/') && !value.startsWith("//")) { "Source endpoint paths must start with one slash and stay on the declared host." }
        require(!value.contains('#') && !value.contains('\\') && !value.contains("%2e", ignoreCase = true)) {
            "Source endpoint paths may not contain fragments, backslashes, or encoded dot segments."
        }
        require(value.substringBefore('?').split('/').none { it == "." || it == ".." }) {
            "Source endpoint paths may not traverse parent directories."
        }
        if (!allowQueryPlaceholder) require(!value.contains('{') && !value.contains('}')) {
            "Only searchPath may contain a placeholder."
        }
        if (allowQueryPlaceholder) require(value.replace("{query}", "").none { it == '{' || it == '}' }) {
            "Unsupported source endpoint placeholder."
        }
    }

    private fun isLocalOrIpHost(host: String): Boolean {
        val lower = host.lowercase().trimEnd('.')
        if (lower == "localhost" || lower.endsWith(".localhost") || lower.endsWith(".local") || lower.endsWith(".internal")) return true
        if (lower.matches(Regex("^[0-9.]+$")) || lower.contains(':')) return true
        return false
    }

    private fun parseLegacyPresentation(json: JSONObject, name: String, description: String): ExtensionPresentation {
        rejectUnknownKeys(json, presentationKeys, "presentation")
        val accentColor = optionalText(json, "accentColor", 7, "#69E7D5")
        require(COLOR_PATTERN.matches(accentColor)) { "accentColor must be a six-digit hex color." }
        val detailsJson = if (json.has("details")) {
            json.optJSONArray("details") ?: throw IllegalArgumentException("presentation.details must be a JSON array.")
        } else JSONArray()
        require(detailsJson.length() <= 12) { "A manifest may contain at most 12 detail rows." }
        val details = (0 until detailsJson.length()).map { index ->
            val row = detailsJson.optJSONObject(index) ?: throw IllegalArgumentException("Each details entry must be an object.")
            rejectUnknownKeys(row, detailKeys, "details[$index]")
            ExtensionDetail(requiredText(row, "label", 40), requiredText(row, "value", 240))
        }
        return ExtensionPresentation(
            accentColor = accentColor,
            homeTitle = optionalText(json, "homeTitle", 60, name),
            homeSubtitle = optionalText(json, "homeSubtitle", 160, description),
            homeBadge = optionalText(json, "homeBadge", 24, "SOURCE"),
            details = details,
        )
    }

    private fun requiredText(json: JSONObject, key: String, maxLength: Int): String {
        val value = json.optString(key, "").trim()
        require(value.isNotEmpty()) { "$key is required." }
        require(value.length <= maxLength) { "$key must be at most $maxLength characters." }
        return value
    }

    private fun optionalText(json: JSONObject, key: String, maxLength: Int, fallback: String): String {
        val value = json.optString(key, fallback).trim().ifEmpty { fallback }
        require(value.length <= maxLength) { "$key must be at most $maxLength characters." }
        return value
    }

    private fun rejectUnknownKeys(json: JSONObject, allowed: Set<String>, scope: String) {
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            require(key in allowed) { "Unknown $scope field '$key'. Executable code and undeclared fields are not supported." }
        }
    }
}
