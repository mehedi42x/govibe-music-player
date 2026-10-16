# GoVibe

**A local-first music player for the music you already own.**

GoVibe has two separate clients: a React/TypeScript progressive web app (PWA) for browsers and a native Kotlin/Jetpack Compose Android app. The Android app uses the system document picker, Room for local metadata, and Media3 for playback; it does not embed the web app.

The displayed app name is **GoVibe**. The Android application ID (`com.aurora.musicplayer`) and `.aurora.json` format identifiers remain unchanged for compatibility.

## Native Android

- Import audio through Android's system document picker; no broad media-library permission is requested.
- Store selected-file URI references and best-effort metadata locally in Room; audio is not uploaded or copied to a server.
- Play selected local audio through Android Media3 with a `MediaSessionService` and system media controls.
- Search the library, favorite tracks, play a queue, and control playback in Compose.
- Use a dark-first interface with a light-appearance option.
- The app includes **no preinstalled extensions**. GoVibe's default home is part of the app itself, not an extension.
- Add optional declarative `.aurora.json` home profiles through the plus-button JSON editor or a user-initiated public GitHub scan. Profiles can change presentation text and accent color; they cannot run code, access music, request credentials, or stream audio.

## Web PWA

The independent browser app supports local audio import/playback, library search, favorites, queue, and IndexedDB persistence. It includes no bundled third-party extension or remote provider runtime. Browser codec, storage, and background-playback support varies by device.

On Android, the PWA can be installed from a trusted HTTPS origin using Chrome's **Install app** or **Add to Home screen** menu. This is a browser-installed PWA, not an APK; it does not use the native Media3 service.

```bash
npm ci
npm run dev
```

Run the web checks:

```bash
npm run format:check
npm run typecheck
npm run lint
npm test
npm run build
npm audit --audit-level=moderate
```

## Build a local Android APK

See [Building](docs/BUILDING.md) for prerequisites. The source review bundle does not include a prebuilt APK; native Gradle and device verification remain pending.

```bash
cd android
./gradlew assembleDebug
```

Expected output: `app/build/outputs/apk/debug/app-debug.apk`. This is a debug-signed local test build, not a Play Store release. See [Installation](docs/INSTALLATION.md).

## Extensions and integrations

See [Extension setup](docs/EXTENSIONS.md), the [v1 JSON schema](aurora-extension-format/schema-v1.json), and [Provider integrations](docs/PROVIDER_INTEGRATIONS.md). A fresh installation has no extensions; GoVibe's app-native home remains available until the user installs and selects a profile.

## Audio quality and privacy

GoVibe does not claim bit-perfect or verified lossless output. Android passes selected local audio to the device media decoder; the device, decoder, and output route determine the actual result. See [Audio quality](docs/AUDIO_QUALITY.md) and [Privacy](docs/PRIVACY.md).

GoVibe is licensed under [0BSD](LICENSE); see [third-party notices](THIRD_PARTY_NOTICES.md). The source bundle contains no APK, signing credentials, local SDK state, or bundled extension manifests.
