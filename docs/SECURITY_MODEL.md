# Security model

## Android trust boundary

The Kotlin app reads audio only after the user selects it through Android's system document picker. It stores the selected URI and best-effort metadata locally; it does not upload audio. The Android manifest disables standard app-data backup and cleartext HTTP.

The `INTERNET` permission supports user-initiated public GitHub profile scans. Repository input must be a plain `https://github.com/owner/repository` URL. Manifest listing/content requests are restricted to `https://api.github.com/`, redirects are disabled, and connection/read timeouts are bounded. Each scan accepts at most 20 `.aurora.json` files; each manifest is limited to 64 KiB and each response to 2 MiB. The app uses no GitHub token.

No profiles are bundled or installed by default. Downloaded manifests are untrusted data. The parser rejects unknown keys and unsupported capabilities, bounds fields and detail rows, and stores plain JSON. Kotlin renders only bounded strings/color metadata. Manifests cannot execute Kotlin, Java, JavaScript, Rust, HTML, or scripts; access the local library; make network requests; or resolve audio. Review each preview before installing.

## Rust/JNI boundary

The Rust core is side-effect-free queue logic. JNI methods accept primitive queue counts, indexes, repeat modes, boolean flags, and random seeds, then return a safe index or `-1`. Rust does not receive Android object references, file URIs, manifest data, or network payloads. Kotlin remains responsible for bounds-safe use of returned indexes, permissions, storage, and Media3 lifecycle. Host Rust tests cover empty queues, invalid indexes, repeat behavior, and shuffle selection; Android ABI/runtime verification is pending.

## Build status and reporting

The source review tree does not include a prebuilt APK. The current computer lacks the Android SDK/NDK and `cargo-ndk`, so no Android Gradle build or device test has been performed. Rust host tests do not verify JNI packaging or Android behavior. No SDK license has been accepted. Do not include credentials, private listening data, or security-sensitive exploit details in public issues. CI uses least-privilege permissions and should not expose secrets to untrusted pull requests.

## Licensing boundary

GoVibe-authored code in this local review revision is marked GPL-3.0-only. Third-party libraries and generated wrapper scripts retain their own licenses; see `THIRD_PARTY_NOTICES.md` and in-file notices. Earlier publicly distributed 0BSD copies remain subject to the 0BSD grant that accompanied them.
