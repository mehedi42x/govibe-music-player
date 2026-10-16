# Roadmap

## Current local review

- NextVibe is a native Android app: Kotlin/Jetpack Compose, Room, an Online/Offline mode selector, permission-gated MediaStore scanning plus Android's system document picker, Media3/MediaSessionService, and a declarative JSON/GitHub source manager.
- Home and Search use a user-selected source's trending/search API while NextVibe renders all interface elements. Local/source history powers on-device artist ranking; no provider is preinstalled.
- Rust provides a dependency-free queue-navigation core through primitive JNI calls. It is not an audio decoder or source runtime.
- Settings includes player volume, device-dependent EQ presets and an opt-in +3 dB maximum volume boost. Android platform effects still need device testing.
- User-requested offline downloads are available only from source manifests that opt in; completed files are stored in shared `Music/NextVibe` and indexed locally. The source declaration does not verify rights.
- The old web/PWA client and TypeScript provider SDK are not part of the current build.

## Verification still required

- Run Rust formatting, tests and clippy; run Kotlin/JVM tests and schema validation where the environment permits.
- Build the Android debug variant on a separately prepared environment after reviewing SDK/NDK terms; verify Room migration, JNI packaging and supported ABIs on Android devices.
- Test document-picker grants, source endpoint compatibility, artwork, stream playback, notifications, audio focus, background playback, seeking and queue behavior on physical devices.
- Evaluate additional features such as playlists, lyrics, backup/export, and provider-specific protocols only after their product/security requirements are designed. This is not feature-for-feature LastWave parity; capabilities that need a matching provider API or are not implemented remain separate work.

## Distribution and license

No workflow dispatch, push or public release has occurred in this local review. Project-authored changes are GPL-3.0-only; third-party components retain their notices. Earlier public 0BSD copies retain their existing license grants.
