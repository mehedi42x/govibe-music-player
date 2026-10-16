# Installation

## Run the web PWA from source

1. Install Node.js 22 or newer and npm.
2. Clone or download the source.
3. In the project root, run `npm ci`.
4. Run `npm run dev` for development, or `npm run build && npm run preview` for a production-style web preview.
5. Open Vite's local URL and import audio files in the browser.

## Android APK

The source review bundle contains no APK. To make a local debug APK, install the Android SDK Platform and Build-Tools versions listed in [Building](BUILDING.md), review and accept the applicable SDK terms, then run `cd android && ./gradlew assembleDebug`. The expected APK is `android/app/build/outputs/apk/debug/app-debug.apk`. This is a local debug build, not a Play Store release.

## Install the browser PWA on Android

Open a trusted HTTPS-hosted copy of the PWA in Android Chrome, then choose **Install app** or **Add to Home screen** from the browser menu. This installs a browser-based PWA, not an APK; background audio and other behavior depend on Chrome and the device and do not use native Media3. A secure HTTPS origin is required for installable/offline PWA features; opening local files with `file://` is not equivalent.

## Native local audio and extensions

The Android app asks the user to choose files through Android's system document picker. It stores metadata and selected URI references locally, not uploaded audio copies. A fresh install has no extensions; the default home is part of the app. Users can add declarative profiles through JSON or public GitHub manifests. The selected audio remains at its original document URI, so keep original files separately.
