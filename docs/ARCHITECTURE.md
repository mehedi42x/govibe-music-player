# Architecture

NextVibe is a native Android music player. Kotlin/Jetpack Compose owns UI, Android lifecycle, local Room persistence, the source JSON client and Media3 playback service. Rust supplies a small deterministic queue-navigation core through JNI. The application ID and JNI package remain `com.aurora.musicplayer` for compatibility.

## Repository boundaries

- `android/app/src/main/java/com/aurora/musicplayer/ui/` — app-owned Home, Search, Library, artist profiles, Settings, artwork, mini-player and Now Playing UI. Local embedded cover art is decoded on-device; remote art uses bounded HTTPS fetches with validated redirects, and player progress is animated in Compose.
- `.../extensions/` — validated manifest parsing, explicit source selection, public GitHub JSON discovery, and bounded HTTPS catalogue client.
- `.../data/` — Room library, opt-in MediaStore scanning, local/remote play history, and user-requested source downloads. Permitted downloads are streamed into shared `Music/NextVibe` through MediaStore and indexed in Room for offline playback; recommendations rank artist-frequency history on-device.
- `.../playback/` — Android Media3 ExoPlayer and MediaSession service, player volume, device Equalizer and LoudnessEnhancer.
- `rust/govibe-core/` — dependency-free queue-navigation functions and JNI boundary.

## Online/offline mode

A top-level mode switch selects Online or Offline. Android ConnectivityManager's validated-network status chooses the initial mode: Online when available, Offline when not. Network loss switches to the Offline portal; the user may also select Offline while connected. Offline search and playback use the local Room library and do not request a source feed.

The user can request a MediaStore scan from the Offline portal or Library. Runtime `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (older supported Android) is requested only after that action. The scanner reads shared audio exposed by MediaStore, stores track URIs/metadata in Room and does not copy files. It cannot access other apps' private directories. The document picker remains an alternative.

## Source boundary

An optional user-installed manifest contains only a public HTTPS origin and trending/search paths, with optional `downloadsAllowed=false`. Its API returns normalized track metadata including title, artist, description, artwork URL, and playable stream URL. Artist profiles reuse the selected source's search endpoint only when opened from a source result or explicitly requested from a local profile; local profile filtering stays on-device. Downloads are user-clicked and source-gated; HTTPS responses are limited to four redirects and 250 MiB before publication to shared `Music/NextVibe`. NextVibe owns all visuals and controls. No bundled provider, source-code runtime, arbitrary headers, credentials, scraping, or provider-defined UI is included. Search terms go to the actively selected source; listening history does not.

## Playback and local data

Local/source histories and recommendations remain on-device. Room v2 adds source listening history through an explicit v1-to-v2 migration. Settings' volume slider controls Media3 volume; the optional +3 dB ceiling is applied with Android LoudnessEnhancer, and Equalizer presets attach to the active audio session when Android reports it. Audio-effect support is device-dependent and requires build/device testing. Media3 handles stream decoding, transitions and the system media session.

## Compatibility and verification

Old `.aurora.json` presentation-only manifests remain parseable, but their presentation is ignored and they cannot be installed as sources. Fresh installs include no provider or extension. Rust host formatting, tests and lint run without Android SDK components. Building the app needs the Android SDK/NDK, Rust toolchain and `cargo-ndk`; this review computer lacks them, so Android compilation, APK creation and device verification are pending. No SDK license is accepted automatically, and no release/publication is part of this review.
