# Source development

NextVibe source manifests are declarative JSON—not plugins. There is no JavaScript runtime, source-defined UI, or extension code execution. The app owns catalogue cards, search, thumbnails, local recommendations, playback controls, and Settings.

Start from the in-app JSON editor or [`examples/music-source.example.aurora.json`](../examples/music-source.example.aurora.json), and validate it with [`aurora-extension-format/schema-v1.json`](../aurora-extension-format/schema-v1.json).

## Manifest fields

A manifest declares `format: "aurora-extension"`, `formatVersion: 1`, a unique dotted/hyphenated lowercase `id`, source name/version/author/description, `capabilities: ["music.source"]`, and a `source` object:

- `baseUrl`: public HTTPS origin only, with no credentials, query, fragment, path or custom port.
- `trendingPath`: origin-relative endpoint path on that host.
- `searchPath`: same-host path with exactly one `{query}` placeholder.
- `downloadsAllowed`: optional boolean, default `false`. Set `true` only if the provider permits users to save direct audio streams for offline playback; this is a provider declaration, not a rights verification.

Endpoints must return JSON shaped like this:

```json
{
  "tracks": [
    {
      "id": "track-123",
      "title": "Example track",
      "artist": "Example artist",
      "album": "Example album",
      "description": "A short source-provided description.",
      "thumbnailUrl": "https://cdn.example.org/art/track-123.jpg",
      "streamUrl": "https://cdn.example.org/audio/track-123.mp3",
      "durationMs": 204000
    }
  ]
}
```

`id`, `title`, and a direct `streamUrl` are required. Other metadata may be empty; `durationMs` is optional. Keep the response to 60 tracks or fewer and under 2 MiB. Use stable identifiers. Search terms are URL-encoded by the app. Artist profiles reuse `searchPath` with the artist name as the query and display results whose `artist` field matches; there is no separate artist endpoint, so a source should return as many matching tracks as its 60-item response cap permits. If downloads are enabled, each user-started download must resolve to a public HTTPS audio response, at most four HTTPS redirects, and no more than 250 MiB; completed files are written to the user's shared `Music/NextVibe` folder. Return an `audio/*` Content-Type or use a recognized audio-file URL extension; opaque URLs served only as `application/octet-stream` cannot be identified and are rejected. Do not declare downloads enabled unless provider terms and rights permit this. Authenticated/DRM streams and arbitrary headers/secrets are unsupported; the app does not bypass them.

## Hosting and discovery

For GitHub discovery, put one `.aurora.json` manifest in the public repository's `nextvibe-sources/` folder. The scanner previews JSON before install, limits a scan to 20 files, and uses GitHub's unauthenticated API. Do not embed secrets or user credentials in manifests.

The client makes requests only to the origin the user selected. It rejects non-HTTPS, localhost/private/IP-literal origins, cross-host manifest endpoints, endpoint redirects, and oversized responses. Provider reliability, content rights, and direct stream availability are responsibilities of the provider and user; NextVibe does not curate or bundle providers.
