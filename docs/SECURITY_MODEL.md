# Security model

## Android trust boundary

Local audio can be selected through Android's system document picker. In response to an explicit **Scan device audio** action, NextVibe may request `READ_MEDIA_AUDIO` (Android 13+) or `READ_EXTERNAL_STORAGE` (older supported versions) to index audio exposed by MediaStore. It does not request `MANAGE_EXTERNAL_STORAGE` and cannot see another app's private folders through this feature. The permission is runtime-controlled; denying it leaves the system picker available. User-requested eligible source downloads are the exception to local-file non-copying: they are written to `Music/NextVibe` via MediaStore; `WRITE_EXTERNAL_STORAGE` is requested only on Android 9 and below. Standard app-data backup and cleartext HTTP are disabled.

The `INTERNET` permission supports validated connectivity detection, public GitHub JSON source discovery, and requests to a user-selected source. GitHub scans use the public unauthenticated API, redirects are disabled, timeouts are bounded, and each scan is limited to 20 manifest files (64 KiB each).

A new music source is untrusted declarative data. The parser accepts only known fields, HTTPS origins and same-origin relative endpoint paths; it rejects credentials, local/IP-literal hosts, nonstandard ports, path traversal, unknown fields and excessive lengths. Before source, stream and artwork requests, NextVibe also preflights DNS answers to reject local/reserved address ranges while keeping HTTPS host verification. This is defense in depth, not connection-level DNS pinning; the selected source and its declared CDN remain user-trusted input. Catalogue replies are JSON only, capped at 2 MiB and 60 tracks. Track/artwork URLs require HTTPS; image bytes are capped at 5 MiB. Manifests cannot execute code, define UI, add headers/tokens or read the local library. Search terms are sent to the selected service; local listening history is not. Offline downloads are hidden unless the selected source explicitly declares `downloadsAllowed: true`, and each user-started response is restricted to public HTTPS, four redirects and 250 MiB; responses declared as HTML/text or unrelated media types are rejected. A provider that deliberately mislabels its response cannot be independently verified. The declaration is not a license check, and other music apps may see completed files in the shared Music folder.

A user-provided service may still return malicious, unavailable or rights-infringing content and can observe requests made to it. Direct playback is delegated to Android Media3. A source's download opt-in does not establish that the provider or user owns the necessary rights; user responsibility still applies. Optional volume boost uses Android LoudnessEnhancer and is off by default, limited to +3 dB; it can cause clipping or uncomfortable output, so the UI warns the user. Equalizer/effect availability depends on the device. No provider is bundled by default.

## Rust/JNI boundary

The Rust core is side-effect-free queue logic. JNI methods accept primitive queue counts, indexes, repeat modes, flags and seeds, then return a safe index or `-1`. Rust receives no Android object, file URI, manifest or network payload. Kotlin owns bounds-safe navigation, storage and Media3 lifecycle.

## Build status and licensing

The current review computer lacks Android SDK/NDK and `cargo-ndk`, so no Android Gradle build or device test has been performed. Rust host tests do not verify Android/JNI packaging or platform audio effects. No SDK license has been accepted. NextVibe-authored code is GPL-3.0-only; third-party dependencies and wrapper scripts retain their own licenses and notices. Earlier public 0BSD copies retain permissions already granted.
