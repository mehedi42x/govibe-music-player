# Building GoVibe

## Rust host checks

Requirements: Rust and Cargo. These checks do not require Android SDK/NDK components:

```bash
cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check
cargo test --manifest-path rust/govibe-core/Cargo.toml
cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings
```

The crate uses only the Rust standard library. `cargo test` writes a lockfile alongside the crate; commit it with source changes to keep builds reproducible.

## Build a local Android debug APK

### Requirements

- JDK 17 or newer.
- Android SDK Platform/API 36 and Android Build-Tools 36.0.0.
- Android NDK (the Gradle app module pins the expected NDK version).
- Rust/Cargo and `cargo-ndk` (`cargo install cargo-ndk`).
- The checked-in Gradle wrapper and Android Gradle Plugin versions pinned in `android/`.

Install SDK/NDK components through Android Studio's SDK Manager or official command-line tools only after reviewing the applicable license terms. Do not use unattended license acceptance. Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`, plus `ANDROID_NDK_HOME` (or `ANDROID_NDK_ROOT`), then run:

```bash
cd android
./gradlew assembleDebug
```

Gradle runs `buildRustCore`, which invokes `scripts/build-android-rust.sh` to compile `rust/govibe-core` for the Android ABIs configured in `android/app/build.gradle`. The generated `libgovibe_core.so` libraries are packaged in the local debug APK.

Expected output: `android/app/build/outputs/apk/debug/app-debug.apk`. This is a local debug-signed test build, not a release; no signing credentials or public artifact are included.

## Review environment limits

The review computer has Rust/Cargo but no Android SDK/NDK, Android Rust target, or `cargo-ndk`. SDK licenses have not been accepted. Therefore this environment can run Rust host checks but cannot compile the Android app, create an APK, or verify device/JNI ABI behavior. Android build/device verification remains explicitly pending.

## Dependency and license policy

GoVibe-authored code in this local review revision is licensed under GPL-3.0-only. Android dependency versions are pinned in Gradle build files; the Rust crate currently has no third-party dependencies. Third-party libraries and Gradle wrapper scripts retain their own license terms and notices. Earlier publicly distributed copies under 0BSD remain subject to that prior grant; see [License](../LICENSE) and the historical [license decision](decisions/0001-browser-pwa-and-license.md).
