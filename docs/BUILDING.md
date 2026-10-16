# Building NextVibe

## Rust host checks

Requirements: Rust and Cargo. These checks do not require Android SDK/NDK components:

```bash
cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check
cargo test --manifest-path rust/govibe-core/Cargo.toml
cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings
```

The crate uses only the Rust standard library.

## Android debug APK

Requirements include JDK 17+, Android SDK Platform/API 37, Build-Tools 36.0.0, the pinned NDK, Rust/Cargo, and `cargo-ndk`. Review SDK/NDK license terms before installing. Do not use unattended license acceptance. After provisioning prerequisites, set `ANDROID_HOME` or `ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME` or `ANDROID_NDK_ROOT`, then run:

```bash
cd android
./gradlew testDebugUnitTest assembleDebug
```

Gradle invokes `scripts/build-android-rust.sh` for the configured Android ABIs. The local debug APK is expected at `android/app/build/outputs/apk/debug/app-debug.apk`; it is not a release.

## Review environment limits

This review computer has Rust/Cargo but no Android SDK/NDK, Android Rust target, or `cargo-ndk`. SDK licenses have not been accepted. Rust checks, schema validation, and source-level checks are available; Android Kotlin/Room/Compose compilation, APK creation, and device verification are pending. Do not install SDK components or accept licenses as part of this review.

## License and dependency policy

Project-authored code in this review revision is GPL-3.0-only. Third-party Android dependencies and the Gradle wrapper retain their own licenses and notices. No runtime dependency has been added for the source feature; it uses Android/Java networking and the existing Compose/Media3 stack. Earlier public copies retain the permissions of their earlier license grant.
