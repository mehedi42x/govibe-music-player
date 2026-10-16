# Changelog

All notable changes to GoVibe are recorded here. The project is pre-release; no public release artifact has been published.

## Unreleased — GoVibe native Android source

### Added

- Separate Kotlin/Jetpack Compose Android client using Room for on-device library/profile data and Media3 for local playback.
- Android system document-picker import using user-selected URI access; no broad media-library permission is requested.
- MediaSessionService source for background playback and system media controls.
- Declarative `.aurora.json` profile parser, direct JSON editor/template, public GitHub repository scan/preview, enable/disable controls, and active-home selection.

### Changed

- GoVibe now starts without any bundled extension manifests or provider demos; its default home is part of the app UI.
- Android is a native client rather than a Capacitor WebView wrapper. The independent React/TypeScript PWA remains available; the clients have separate storage and playback paths.
- Android profile manifests are data-only UI profiles and do not implement the web TypeScript provider SDK.
- Removed the obsolete browser plugin-toggle store and bundled profile assets; browser-local library features remain core app behavior.

### Verification

- Web formatting, typecheck, lint, tests, production build, and dependency audit are run for the reviewed source bundle.
- Native Gradle/device verification remains pending; the review bundle contains no APK.

## 0.1.0 — web prototype (unreleased)

- Local-first responsive PWA, browser audio import/tag reading, local playback, queue, favorites, and IndexedDB persistence.
- TypeScript provider contract, app-internal local-library adapter, manifest validation, and deterministic core/storage tests.
- CI, security/dependency checks, community policies, and implementation documentation.
