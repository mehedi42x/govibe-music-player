# Installation

## Build and install a local debug APK

GoVibe is currently distributed in this review tree as Android source, not as a prebuilt APK.

1. Install JDK 17+, Android SDK Platform/API 36, Android Build-Tools 36.0.0, the NDK version specified in `android/app/build.gradle`, Rust/Cargo, the Android Rust targets, and `cargo-ndk`.
2. Review the applicable Android SDK and NDK license terms before installing or using them. No unattended license acceptance is used by this project.
3. Configure `ANDROID_HOME` or `ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME`.
4. From the repository root, build the local debug APK:

   ```bash
   cd android
   ./gradlew assembleDebug
   ```

5. Install `app/build/outputs/apk/debug/app-debug.apk` on a compatible test device using Android Studio or `adb install`.

This is a debug build for local testing, not a Play Store release. Do not use the app with important data until Room/URI persistence and playback have been verified on your target device. Preserve original audio files: GoVibe indexes selected URIs but does not back up the files.

## First run

GoVibe starts with an empty local library and no preinstalled extensions. Use **Add audio** to pick files from Android's system document picker. Optionally create or scan for a declarative JSON home profile from **Extensions**; scanning a public GitHub repository is only started after you tap the scan button.

## Current build limitation

The current review computer has no Android SDK/NDK or `cargo-ndk`, so no APK has been produced or device-tested here. See [Building](BUILDING.md) for verification status and prerequisites.

## Source license

GoVibe-authored code in this local review revision is GPL-3.0-only. Third-party dependencies retain their own licenses; see the unchanged [`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md). Earlier publicly distributed 0BSD copies remain under the permissions previously granted for those copies.
