# GoVibe

**A private, local-first music player for the audio you already own.**

This review tree contains one native Android application: **Kotlin with Jetpack Compose** for the interface and Android integration, plus a dependency-free **Rust** playback-queue core connected through JNI. The former React/TypeScript browser client and its Node/Vite toolchain are not part of this migration.

The Android application ID (`com.aurora.musicplayer`) and declarative `.aurora.json` extension format remain unchanged for compatibility. GoVibe branding is preserved. This is a local source review; it is not a release and no rewritten bundle has been published.

## App behavior

- Import audio with Android's system document picker; GoVibe does not request broad storage access or scan the device.
- Keep selected document URI references and best-effort metadata in the local Room database. Audio is neither uploaded nor copied to a server.
- Play user-selected audio through AndroidX Media3 and a `MediaSessionService` with system playback controls.
- Search the local library, favorite tracks, and control queue navigation, shuffle, and repeat. Rust calculates safe queue indexes and navigation transitions; Kotlin owns the UI and Android lifecycle.
- Choose dark or light appearance.
- Fresh installs contain **no preinstalled extensions**. GoVibe's default home is built into the app.
- Add optional, declarative `.aurora.json` home profiles by pasting JSON or scanning a public GitHub repository after an explicit user action. Profiles can change presentation text and accent color; they cannot execute code, access audio, request credentials, or stream music.

## Source layout

- `android/` — Kotlin/Jetpack Compose application, Room storage, document picker, Media3 playback, and the JSON/GitHub extension manager.
- `rust/govibe-core/` — standard-library-only Rust queue engine and JNI entry points, with host unit tests.
- `aurora-extension-format/` — versioned JSON schema for non-executable presentation profiles.
- `scripts/build-android-rust.sh` — cross-compiles the Rust shared library for Android ABIs when invoked by Gradle.

## Verify the Rust core

Requires Rust and Cargo:

```bash
cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check
cargo test --manifest-path rust/govibe-core/Cargo.toml
cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings
```

## Build a local Android debug APK

An Android build requires JDK 17+, Android SDK Platform/API 36 and Build-Tools 36.0.0, Android NDK, Rust, and `cargo-ndk`. Review Google's SDK/NDK terms before installing or using them. The project does not accept SDK licenses automatically; no SDK license has been accepted as part of this review.

After installing those prerequisites yourself and configuring `ANDROID_HOME`/`ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME`, run:

```bash
cd android
./gradlew assembleDebug
```

Gradle invokes `scripts/build-android-rust.sh` and packages `libgovibe_core.so`; expected APK output is `android/app/build/outputs/apk/debug/app-debug.apk`. This is only a local debug build, not a release. The current review computer has no Android SDK/NDK or `cargo-ndk`, so an APK cannot be built or device-tested here.

## Extensions and privacy

See [Extension setup](docs/EXTENSIONS.md), the [v1 JSON schema](aurora-extension-format/schema-v1.json), [Architecture](docs/ARCHITECTURE.md), and [Privacy](docs/PRIVACY.md). A public GitHub scan only retrieves bounded JSON manifests from the repository's `aurora-extensions/` folder; it does not download or run extension code.

## License

This review-tree revision licenses GoVibe-authored code under [GNU GPL version 3](LICENSE), SPDX `GPL-3.0-only`. Third-party components retain their own licenses; their notices in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) are unchanged. Earlier copies already published with the 0BSD license remain available under the permissions granted with those copies; changing this working tree cannot retroactively revoke that grant. This change is local for review and has not been pushed to the public repository.
