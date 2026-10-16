package com.aurora.musicplayer.extensions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val MAX_EXTENSION_FILES = 20
private const val MAX_EXTENSION_BYTES = 64 * 1024
private const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024

class RemoteExtensionPreview(
    val fileName: String,
    val manifest: ExtensionManifest?,
    val rawJson: String?,
    val sourceRepo: String,
    val error: String? = null,
)

class GitHubExtensionClient {
    suspend fun scan(repositoryUrl: String): List<RemoteExtensionPreview> = withContext(Dispatchers.IO) {
        val repository = parseRepositoryUrl(repositoryUrl)
        val sourceUrl = "https://github.com/${repository.owner}/${repository.name}"
        val directoryUrl = "https://api.github.com/repos/${repository.owner}/${repository.name}/contents/aurora-extensions"
        val listing = get(directoryUrl, raw = false)
        val entries = try {
            JSONArray(listing)
        } catch (_: Exception) {
            throw IOException("GitHub returned an unexpected extension directory response.")
        }

        val files = (0 until entries.length()).mapNotNull { index ->
            val item = entries.optJSONObject(index) ?: return@mapNotNull null
            val name = item.optString("name")
            val path = item.optString("path")
            val size = item.optLong("size", -1)
            if (item.optString("type") != "file" || !name.endsWith(".aurora.json", ignoreCase = true)) return@mapNotNull null
            require(path.startsWith("aurora-extensions/") && !path.split('/').any { it == ".." }) {
                "GitHub returned an invalid extension path."
            }
            require(size in 1..MAX_EXTENSION_BYTES.toLong()) {
                "${name.take(80)} exceeds the 64 KiB extension file limit."
            }
            name to path
        }.sortedBy { it.first.lowercase() }

        if (files.isEmpty()) {
            throw IOException("No .aurora.json files found. Add extension manifests to the repository's aurora-extensions/ folder.")
        }
        require(files.size <= MAX_EXTENSION_FILES) {
            "This repository contains more than $MAX_EXTENSION_FILES extension files. Keep catalogs small to respect GitHub's public API limits."
        }

        files.map { (name, path) ->
            try {
                val raw = get(
                    "https://api.github.com/repos/${repository.owner}/${repository.name}/contents/${encodePath(path)}",
                    raw = true,
                )
                val manifest = ExtensionManifestParser.parse(raw)
                RemoteExtensionPreview(name, manifest, raw, sourceUrl)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                RemoteExtensionPreview(
                    fileName = name,
                    manifest = null,
                    rawJson = null,
                    sourceRepo = sourceUrl,
                    error = error.message?.take(220) ?: "Could not load this extension file.",
                )
            }
        }
    }

    private fun parseRepositoryUrl(input: String): Repository {
        val uri = try {
            URI(input.trim())
        } catch (_: Exception) {
            throw IllegalArgumentException("Enter a public GitHub repository URL, such as https://github.com/owner/repository.")
        }
        require(uri.scheme.equals("https", ignoreCase = true) && uri.host.equals("github.com", ignoreCase = true)) {
            "Only public https://github.com/owner/repository links are supported."
        }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Use a plain public repository URL without credentials, query parameters, or fragments."
        }
        val segments = uri.path.trim('/').split('/').filter { it.isNotBlank() }
        require(segments.size == 2) { "Paste the repository home link, not a file, branch, or issue link." }
        val owner = segments[0]
        val name = segments[1].removeSuffix(".git")
        require(owner.matches(Regex("^[A-Za-z0-9_.-]{1,100}$")) && name.matches(Regex("^[A-Za-z0-9_.-]{1,100}$"))) {
            "The GitHub repository link contains unsupported characters."
        }
        return Repository(owner, name)
    }

    private fun get(url: String, raw: Boolean): String {
        require(url.startsWith("https://api.github.com/")) { "Extension downloads must use GitHub's HTTPS API." }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 12_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", if (raw) "application/vnd.github.raw+json" else "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "GoVibe-Android/0.2")
        }
        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException(
                    when {
                        status == 404 -> "Repository or aurora-extensions/ folder not found. Public repositories only."
                        status == 403 || status == 429 -> "GitHub public API rate limit reached. Try again after the reset time."
                        else -> "GitHub request failed with HTTP $status."
                    },
                )
            }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_RESPONSE_BYTES || (raw && total > MAX_EXTENSION_BYTES)) {
                        throw IOException("GitHub response is larger than the supported limit.")
                    }
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            return String(bytes, StandardCharsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }

    private fun encodePath(path: String): String = path.split('/').joinToString("/") { segment ->
        URLEncoder.encode(segment, StandardCharsets.UTF_8.name()).replace("+", "%20")
    }

    private data class Repository(val owner: String, val name: String)
}
