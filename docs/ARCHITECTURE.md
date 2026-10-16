# Architecture

## Two separate clients

GoVibe maintains a React/TypeScript browser PWA and a native Kotlin/Jetpack Compose Android client. The Android app does not embed the web app in a WebView. They share product concepts and the declarative extension format but use separate UI, playback, and persistence implementations.

The web client uses Vite, HTML audio, Web Audio, and IndexedDB. Android uses Jetpack Compose, Room, Android's system document picker, and AndroidX Media3. Native Gradle/device verification remains pending; this source package does not include an APK.

## Repository boundaries

### Web PWA

- `src/App.tsx` — navigation, browser library views, forms, and playback interaction.
- `src/core/queue.ts` — pure queue transitions.
- `src/core/quality.ts` — provider-format selection and truthful quality labels.
- `src/core/plugins.ts` and `plugin-sdk/` — web provider SDK contract and manifest validation.
- `src/domain/models.ts` — web domain types and SDK/core version constants.
- `src/plugins/local-library.ts` — app-internal local-library adapter; not a separately installed extension.
- `src/data/library-db.ts` — IndexedDB persistence and migrations.
- Vite and `vite-plugin-pwa` — web asset bundling and offline shell.

### Native Android

- `android/app/src/main/java/com/aurora/musicplayer/MainActivity.kt` — Compose entry point and MediaController connection.
- `android/.../data/` — Room entities/DAOs and local library repository.
- `android/.../playback/PlaybackService.kt` — Media3 ExoPlayer hosted by a MediaSessionService.
- `android/.../extensions/` — strict declarative parser, user-installed profile management, and public GitHub Contents API scanner.

No extension manifests are packaged in Android assets. GoVibe's default home is implemented by the app UI; a fresh install starts with no extensions. Startup removes legacy bundled rows from development databases while preserving user-installed community profiles.

## Data and playback

The PWA stores selected audio Blobs, tags, playlists, settings, history, and queue data in browser IndexedDB. Each browser profile has its own library and quotas.

Android stores selected document URI strings and best-effort audio metadata in Room. Audio files are not copied to a server. Android's Storage Access Framework grants access only to user-selected documents; persistence depends on the document provider's URI permissions and file availability. Media3 reads the selected source and exposes a system MediaSession. Device decoder/output behavior determines the actual audio path; GoVibe does not claim verified bit-perfect output.

## Extension boundary

The Android `.aurora.json` format is presentation metadata, not executable plugin code. A manifest may declare supported UI/theme/metadata capabilities and provide bounded plain-text labels, colors, and detail rows. Unknown fields, unsupported capabilities, excessive size, and invalid values are rejected. Imported manifests cannot search or play audio, access local files, invoke JavaScript/Kotlin, fetch arbitrary URLs, or request permissions.

The Android app can scan a public GitHub repository's root `aurora-extensions/` directory using the unauthenticated GitHub Contents API after the user requests a scan. It previews valid manifests before installation and caps each scan at 20 files. GitHub source is untrusted input; parsing is bounded and no downloaded code is executed. The web provider SDK is separate and is not loaded by the native app.

## Build status

The native code has not yet been verified by Gradle or on an Android device. No APK is included in the source review bundle. The browser PWA can run on Android from HTTPS but is not a native substitute for Media3 playback.
