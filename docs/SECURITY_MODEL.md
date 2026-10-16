# Security model

## Native Android trust boundary

The native app reads audio only after the user selects it through Android's system document picker. It stores the selected URI and best-effort metadata locally; it does not upload audio. The Android manifest disables standard app-data backup and cleartext HTTP.

The Android `INTERNET` permission supports user-initiated public GitHub profile scans. Repository input must be a plain `https://github.com/owner/repository` URL. Manifest listing/content requests are restricted to `https://api.github.com/`, redirects are disabled, and connection/read timeouts are bounded. Each scan accepts at most 20 `.aurora.json` files; each manifest is limited to 64 KiB and each response to 2 MiB. The app uses no GitHub token.

No profiles are bundled or installed by default. Downloaded manifests are untrusted data. The parser rejects unknown keys and unsupported capabilities, bounds fields and detail rows, and stores plain JSON. The Android app renders only bounded strings/color metadata and never executes downloaded Kotlin, JavaScript, HTML, or scripts. Manifests cannot access the local library, make network requests, or resolve audio. Review each preview before installing.

## Web PWA and web provider SDK

The PWA stores user-selected audio in browser IndexedDB and plays it through browser media APIs. Its provider SDK and manifest validator describe a provider contract; third-party provider packages are not installed or executed by the current web release. Manifest validation is not a sandbox or a security boundary for executable code.

The Android JSON profile system is not the web SDK runtime. Neither system executes untrusted community code.

## Build status and reporting

Native source is not yet verified by Gradle or on a device, and the source review bundle does not contain a prebuilt APK. Web checks do not verify native behavior. Before public contributions are invited, maintainers should enable private vulnerability reporting/Security Advisories. Do not include credentials, private listening data, or security-sensitive exploit details in public issues. CI uses least-privilege permissions and should not expose secrets to untrusted pull requests.
