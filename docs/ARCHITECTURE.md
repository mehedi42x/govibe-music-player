# Architecture

## One native Android application

GoVibe is a native Android local-audio player. Kotlin and Jetpack Compose own the screens, Android lifecycle, system document picker, Room persistence, Media3 playback service, and the declarative JSON/GitHub extension manager. Rust provides a small deterministic queue-navigation core loaded into the app as `libgovibe_core.so` through JNI. There is no browser/PWA client, WebView wrapper, JavaScript runtime, or network music provider.

## Repository boundaries

### Kotlin / Android (`android/`)

- `app/src/main/java/com/aurora/musicplayer/MainActivity.kt` — Compose entry, system audio document picker, MediaController connection, and Rust-backed queue navigation.
- `.../ui/` — Compose Home, Library, Now Playing, Extensions, and Settings screens.
- `.../data/` — Room entities, DAOs, and local library metadata/history persistence.
- `.../playback/PlaybackService.kt` — AndroidX Media3 ExoPlayer hosted by a MediaSessionService; this is the audio playback/decoding path.
- `.../extensions/` — bounded `.aurora.json` parsing, installed profile management, and explicit public GitHub Contents API scanning.
- `app/src/main/assets/licenses/` — existing packaged third-party license materials.

### Rust (`rust/govibe-core/`)

- `src/lib.rs` — pure queue index selection for sequential, repeat, and shuffle navigation; primitive-only JNI entry points; host unit tests.
- The crate has no external Rust dependencies. Rust does **not** decode, transform, or access audio files, Android objects, network data, or the extension database.
- `android/app/src/main/java/com/aurora/musicplayer/core/NativePlaybackCore.kt` is the single Kotlin library-loading/FFI boundary.
- `scripts/build-android-rust.sh` uses `cargo-ndk` and an installed Android NDK to build the shared library for packaged Android ABIs. Gradle packages the generated libraries.

## Local data and playback

The Android system document picker grants access only to user-selected audio. GoVibe stores URI references and best-effort metadata in Room; it does not copy audio to a server. URI access depends on document-provider grants and file availability. Media3 reads the selected source and exposes a system MediaSession. Device decoder/output behavior determines the actual audio path; GoVibe does not claim verified bit-perfect output.

Rust receives only primitive queue sizes, indexes, repeat modes, shuffle flags, and seeds through JNI. It returns a valid index or `-1` when there is no valid successor/predecessor. Kotlin validates and uses those results to seek the MediaController. Media3 remains responsible for playback, automatic transitions, audio decoding, and session behavior.

## Extension boundary

The `.aurora.json` format is presentation metadata, not executable plugin code. A manifest may declare supported UI/theme/metadata capabilities and provide bounded plain-text labels, colors, and detail rows. Unknown fields, unsupported capabilities, excessive size, and invalid values are rejected. Imported manifests cannot search or play audio, access local files, invoke JavaScript/Kotlin/Rust, fetch arbitrary URLs, or request permissions.

The Android app can scan a public GitHub repository's root `aurora-extensions/` directory using the unauthenticated GitHub Contents API after the user explicitly requests a scan. It previews valid manifests before installation and caps each scan at 20 files. No manifests are packaged; a fresh install has no extensions. GitHub source is untrusted input, parsing is bounded, and downloaded code is never executed.

## Build and verification status

Rust host formatting, tests, and lint can run without Android SDK components. Building the Android app requires the Android SDK, NDK, Rust toolchain, and `cargo-ndk`; the Gradle task builds the JNI library before packaging. This review computer has no Android SDK/NDK and no `cargo-ndk`, so Android Gradle compilation, APK generation, and device verification are pending. No SDK license is accepted automatically, and no release or publication is part of this review.
