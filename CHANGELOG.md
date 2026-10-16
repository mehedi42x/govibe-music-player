# Changelog

All notable changes to NextVibe are recorded here. This is a local review tree; no workflow has been dispatched and no GitHub Release has been created.

## Unreleased — NextVibe source-powered redesign

### Added and changed

- Rename the product and launcher identity to **NextVibe**, with a new adaptive Android icon and refined dark/light color system.
- Add a top Online/Offline mode selector. Validated connectivity defaults to Online; loss of internet automatically exposes the offline portal.
- Add opt-in MediaStore indexing of shared device audio after a runtime permission request; preserve the system picker as a selective alternative. Other apps' private storage remains inaccessible.
- Replace extension-facing Home profiles with optional user-added declarative music sources. NextVibe owns Home, Search, Library, track rows, artwork, metadata and player UI.
- Add source-backed trending and search, display track title/artist/album/description/artwork, and play direct HTTPS stream URLs through Media3.
- Store local and source listening history, migrate Room schema v1 to v2, and locally rank artist-aware suggestions without transmitting taste history.
- Move source installation and selection into Settings; retain explicit public GitHub JSON discovery, plus-editor, preview-before-install, no bundled providers, and old folder scan fallback.
- Add Settings controls for player volume, device-session Flat/Bass/Vocal/Treble Equalizer presets and an opt-in LoudnessEnhancer boost capped at +3 dB.
- Open the expanded Now Playing view when a song row is selected; resolve local embedded album art and bounded remote HTTPS thumbnails, with a smooth seek bar and pulsing gradient progress indicator.
- Make artist names on local and source track rows open profiles. Local profiles list all matching local tracks; opening an artist from a source row queries the selected source, and local profiles offer a separate opt-in source search.
- Add a source-declared, default-off offline-download capability. An explicit card action streams eligible HTTPS audio (four redirects, 250 MiB cap) through MediaStore to shared `Music/NextVibe` and adds it to the local library; Android 9 and below request write permission only at download time.
- Keep GPL-3.0-only notices and third-party license disclosures accurate; add no runtime dependency.

### Verification status

- Rust `fmt`, all 8 unit tests, `clippy -D warnings`, JSON/schema and Android XML parsing, static feature checks, and `git diff --check` pass.
- `./gradlew testDebugUnitTest --no-daemon` was attempted but stopped at SDK discovery (`SDK location not found`). Android compilation, APK creation, Room migration, runtime permissions, audio effects and device verification remain pending because this computer has no Android SDK/NDK or `cargo-ndk`; no SDK license was accepted.
- Nothing was pushed, a workflow was not dispatched, and no public release was created.

## Historical note

Earlier public copies were licensed under 0BSD; this local GPL-3.0-only revision does not retroactively revoke permissions already granted for them. The former browser prototype is not part of the current Android app.
