# Changelog

All notable changes to GoVibe are recorded here. The current tree is a local migration review; no workflow has been dispatched and no GitHub Release has been created.

## Unreleased — Kotlin/Rust Android migration review

### Changed

- Retain the native Kotlin/Jetpack Compose Android app, local Room library, system document picker, Media3 playback, and generic declarative JSON/GitHub extension manager; remove the browser/TypeScript client and its build system.
- Add a dependency-free Rust queue core connected through JNI, with shuffle/repeat navigation and host tests.
- Change the current local GoVibe-authored source license to GPL-3.0-only at the user's request; preserve third-party components' own license notices.
- Add a manual APK release workflow that requires signing-key secrets and defaults to a draft release. It has not been run.

### Verification status

- Rust host formatting, tests, and clippy are pending in this migration pass.
- Android Gradle build, APK creation, and device/JNI verification are pending because this review computer has no Android SDK/NDK or `cargo-ndk`; no SDK license has been accepted.
- No push, workflow dispatch, or public release has occurred. Review the exact local changes before approving publication.

## Historical — earlier public source revision

- Earlier copies were published under 0BSD. Changing this working tree does not retroactively revoke permissions already granted for those copies.
- The earlier prototype included a browser PWA and TypeScript provider contract; these are not part of the current migration target.
