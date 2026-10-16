# Music sources

NextVibe accepts user-added `.aurora.json` **data-source manifests**. A source configures two HTTPS JSON endpoints—trending and search—and may explicitly declare `downloadsAllowed: true`. It cannot contribute Compose screens, branding, executable code, credentials, or local-file access. NextVibe's own UI displays its track list, thumbnails, titles, descriptions, artist profiles, search results, download controls, and player UI.

No source is bundled or selected by default. Open **Settings → Music sources** to choose one of:

1. Tap **+** and edit/validate a JSON manifest, or
2. Paste a public GitHub repository URL and scan it. The scanner checks `nextvibe-sources/` for `.aurora.json` files and also recognizes the former `aurora-extensions/` folder for compatibility. Review each parsed preview, then add only the source you choose.

After installation, enable the source and select it under **Active catalogue**. Switch back to **No remote source** at any time. Removing a source does not remove local audio or listening history. Old presentation-only profiles can still be read and removed, but cannot be installed as a music source or change the NextVibe UI.

## Source manifest and API

See the [schema](../aurora-extension-format/schema-v1.json) and [example](../examples/music-source.example.aurora.json). A source has a public HTTPS origin plus same-host paths:

- `trendingPath`, for example `/v1/trending`
- `searchPath`, for example `/v1/search?q={query}` (the app URL-encodes the search term)
- optional `downloadsAllowed` boolean, which defaults to `false` for new and existing manifests. Only set it to `true` if the source permits user-requested offline copies of its direct audio streams.

Each endpoint returns an object with a `tracks` array. A track should contain stable `id`, `title`, `artist`, `album`, `description`, `thumbnailUrl`, `streamUrl`, and optional `durationMs`. `title`, `id`, and a playable direct `streamUrl` are required. Artwork and stream URLs must use public HTTPS hosts. Search, trending responses and artwork are bounded. Catalogue endpoint redirects and nonstandard ports are not supported; artwork follows at most four URL-validated HTTPS redirects.

Artist profiles reuse `searchPath` with the artist name as the query and filter returned tracks by the `artist` field. There is no dedicated artist endpoint, and the source's 60-track response cap means a profile may not represent its full catalogue.

## Offline downloads

When the selected source declares `downloadsAllowed: true`, its remote track cards show **Download for offline playback**. Downloads happen only after the user taps the button. NextVibe accepts only public HTTPS audio responses, follows at most four HTTPS redirects, and limits each file to 250 MiB. A response should declare an `audio/*` Content-Type or use a recognized audio extension in its URL; opaque generic streams cannot be reliably identified and are rejected. It does not download DRM-protected or authenticated streams and does not bypass provider controls. Completed audio is saved through Android MediaStore under the shared `Music/NextVibe` folder and added to the local library. Android 10+ uses scoped MediaStore; Android 9 and below request `WRITE_EXTERNAL_STORAGE` only when the user starts a download. Clearing the app's library index does not delete completed files in that folder.

`downloadsAllowed` is the source author's declaration, not an independent license or terms-of-service check. The source maintainer and user are responsible for having permission to save the audio. No provider or media service is bundled.

## Data and privacy

A source request is made only when the user selects that source, searches, opens an artist from a source track, or explicitly taps **Find songs in [source]** on a local artist profile. The query or artist name is sent to the selected source. Opening a local artist profile alone remains on-device. Listening history is stored locally and is not transmitted for personalization. Trending and artwork requests also contact the selected source's declared HTTPS host/CDN. A download fetches the tapped audio from its stream host and writes it to the shared Music folder; it does not upload personal library or history data. GitHub scanning is a separate explicit action and uses the unauthenticated public API.

## Compatibility note

The format identifier remains `aurora-extension`, version 1, so existing stored manifests remain readable. Their former presentation fields are ignored by the app. Only manifests declaring `music.source` and a valid `source` object can be newly installed; `downloadsAllowed` remains optional and defaults to off.
