# NextVibe

**A private, source-powered music player with a native listening experience.**

This review tree contains a native Android application: Kotlin/Jetpack Compose owns the interface, Android integration, Room storage and Media3 playback; a dependency-free Rust queue core is connected through JNI. The former browser client is not part of this build.

The application ID (`com.aurora.musicplayer`) and `.aurora.json` format identifier remain stable for compatibility. The product name and app icon are NextVibe. This is a local review only; nothing has been pushed or released.

## App behavior

- Show Online or Offline player mode in the top switch. With validated internet, Online is the default; without it, the Offline portal is selected automatically. The user can switch modes at the top.
- Browse trending and search catalogues from an **optional, user-installed music source**. The extension supplies data only; NextVibe renders the feed, search, track rows, thumbnails, details and player UI.
- In Offline mode, tap **Scan device audio** to ask Android for `READ_MEDIA_AUDIO` (Android 13+) or legacy `READ_EXTERNAL_STORAGE` access. A user-triggered MediaStore scan indexes shared audio exposed by Android. It cannot read another app's private folders or protected storage. The system picker remains available as an alternative.
- Play indexed local audio offline or a selected source's HTTPS stream URLs online through Media3 and the Android media session.
- Download a remote song for offline playback only when the selected source opts in. The user-started copy is saved to the shared `Music/NextVibe` folder and added to the library; files are limited to 250 MiB. Android 10+ uses scoped MediaStore, and Android 9 or lower asks for write access only when needed.
- Build locally ranked suggestions from local and source listening history. Taste signals stay on-device; search terms are sent to the selected source when the user searches.
- Tap an artist name on a song row to open an artist profile. It lists matching local-library songs; a source-track artist can query the active source, while a local artist profile stays local until **Find songs in [source]** is tapped. Remote results depend on that source's search API and are capped at 60 tracks per response.
- Search local tracks, favorite tracks, and control queue/shuffle/repeat. Settings includes playback volume, flat/bass/vocal/treble EQ presets, and an opt-in volume boost limited to +3 dB.
- Fresh installs have **no bundled or preselected provider**. Add a source from Settings using JSON or a public GitHub repository.

## User-added source contract

A manifest is declarative JSON. It names a public HTTPS origin and same-origin `trendingPath` and `searchPath` endpoints. The provider API returns `{ "tracks": [...] }` with stable `id`, `title`, `artist`, `album`, `description`, `thumbnailUrl`, `streamUrl`, and optional `durationMs` fields. Artist profiles reuse `searchPath` and filter the returned tracks by artist name; the source may not expose a complete artist catalogue. An optional `downloadsAllowed` flag defaults to `false`; setting it to `true` lets users request eligible offline copies. NextVibe bounds response sizes and results, validates HTTPS URLs, and does not execute extension code. See [source setup](docs/EXTENSIONS.md), [provider contract](docs/PLUGIN_DEVELOPMENT.md), and [schema](aurora-extension-format/schema-v1.json).

The source service must implement this contract; the app does not scrape arbitrary sites or ship any music provider. Playback depends on a valid, reachable stream URL and Android Media3 compatibility. Downloads require the source's explicit opt-in, a direct public HTTPS audio response, and user rights to save it; authenticated/DRM streams, DRM bypass, lyrics, and social services are not supported.

## Source layout

- `android/` — Kotlin/Jetpack Compose app, Room history/library, Media3 playback, opt-in MediaStore scanner and offline downloads, and source manager.
- `rust/govibe-core/` — standard-library-only queue engine and JNI entry points.
- `aurora-extension-format/` — versioned declarative manifest schema.
- `examples/music-source.example.aurora.json` — a provider-neutral source manifest template (example host only).

## Checks and build

Rust checks require Rust/Cargo:

```bash
cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check
cargo test --manifest-path rust/govibe-core/Cargo.toml
cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings
```

An Android build also needs JDK 17+, the pinned Android SDK/Build Tools and NDK, Rust, and `cargo-ndk`. Review SDK/NDK terms before installing; no SDK license is accepted by this review. See [build instructions](docs/BUILDING.md). This review environment has no Android SDK/NDK or `cargo-ndk`, so device/APK verification remains pending.

## License

Project-authored code in this review tree is GPL-3.0-only; see [LICENSE](LICENSE). Third-party components keep their own licenses and notices. Earlier public revisions remain subject to their previously granted license. These changes are local for review and have not been pushed or published.
