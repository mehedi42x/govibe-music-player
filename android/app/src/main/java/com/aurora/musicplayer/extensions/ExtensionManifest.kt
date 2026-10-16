// Copyright (C) 2026 Mehedi Hasan
// SPDX-License-Identifier: GPL-3.0-only

package com.aurora.musicplayer.extensions

import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets

private const val MAX_MANIFEST_BYTES = 64 * 1024
private val ID_PATTERN = Regex("^[a-z0-9]+(?:[.-][a-z0-9]+)+$")
private val VERSION_PATTERN = Regex("^[0-9]+\\.[0-9]+\\.[0-9]+(?:[-+][A-Za-z0-9.-]+)?$")
private val COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
private val ALLOWED_CAPABILITIES = setOf("ui.home", "theme.accent", "metadata.display")

/** Declarative UI metadata only. This format contains no executable extension code. */
data class ExtensionManifest(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val capabilities: Set<String>,
    val presentation: ExtensionPresentation,
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
        "capabilities", "presentation",
    )
    private val presentationKeys = setOf(
        "accentColor", "homeTitle", "homeSubtitle", "homeBadge", "details",
    )
    private val detailKeys = setOf("label", "value")

    fun parse(rawJson: String): ExtensionManifest {
        require(rawJson.toByteArray(StandardCharsets.UTF_8).size <= MAX_MANIFEST_BYTES) {
            "Extension file is larger than 64 KiB."
        }
        val root = JSONObject(rawJson)
        rejectUnknownKeys(root, rootKeys, "manifest")
        require(root.optString("format") == "aurora-extension") {
            "Unsupported extension format. Expected aurora-extension."
        }
        require(root.optInt("formatVersion", -1) == 1) {
            "Unsupported extension format version."
        }

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
                require(capability in ALLOWED_CAPABILITIES) {
                    "Unsupported capability: $capability"
                }
                require(add(capability)) { "Duplicate capability: $capability" }
            }
        }

        val presentation = root.optJSONObject("presentation")
            ?: throw IllegalArgumentException("presentation must be a JSON object.")
        rejectUnknownKeys(presentation, presentationKeys, "presentation")
        val accentColor = optionalText(presentation, "accentColor", 7, "#B7E36B")
        require(COLOR_PATTERN.matches(accentColor)) { "accentColor must be a six-digit hex color, for example #B7E36B." }
        val homeTitle = optionalText(presentation, "homeTitle", 60, name)
        val homeSubtitle = optionalText(presentation, "homeSubtitle", 160, description)
        val homeBadge = optionalText(presentation, "homeBadge", 24, "EXTENSION")
        val detailsJson = if (presentation.has("details")) {
            presentation.optJSONArray("details")
                ?: throw IllegalArgumentException("presentation.details must be a JSON array.")
        } else {
            JSONArray()
        }
        require(detailsJson.length() <= 12) { "A manifest may contain at most 12 detail rows." }
        val details = (0 until detailsJson.length()).map { index ->
            val row = detailsJson.optJSONObject(index)
                ?: throw IllegalArgumentException("Each details entry must be an object.")
            rejectUnknownKeys(row, detailKeys, "details[$index]")
            ExtensionDetail(
                label = requiredText(row, "label", 40),
                value = requiredText(row, "value", 240),
            )
        }

        return ExtensionManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            capabilities = capabilities,
            presentation = ExtensionPresentation(
                accentColor = accentColor,
                homeTitle = homeTitle,
                homeSubtitle = homeSubtitle,
                homeBadge = homeBadge,
                details = details,
            ),
        )
    }

    fun template(): String = """
        {
          "format": "aurora-extension",
          "formatVersion": 1,
          "id": "org.example.my-extension",
          "name": "My GoVibe Extension",
          "version": "1.0.0",
          "author": "Your name",
          "description": "Describe the home profile this extension adds.",
          "capabilities": ["ui.home", "theme.accent", "metadata.display"],
          "presentation": {
            "accentColor": "#B7E36B",
            "homeTitle": "Your home title",
            "homeSubtitle": "A short description shown on the home screen.",
            "homeBadge": "MY PROFILE",
            "details": [
              { "label": "Mode", "value": "Local metadata profile" }
            ]
          }
        }
    """.trimIndent()

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
