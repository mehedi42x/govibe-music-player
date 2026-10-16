# Building GoVibe

## Build the web PWA

Requirements: Node.js 22+ and npm.

```bash
npm ci
npm run dev
```

Run the checks:

```bash
npm run format:check
npm run typecheck
npm run lint
npm test
npm run build
npm audit --audit-level=moderate
```

`npm run build` produces `dist/`; `npm run preview` serves that build. Do not commit `dist/`, local audio, credentials, signing keys, browser profile data, or Android build outputs.

## Build the native Android APK

### Requirements

- JDK 17 or newer.
- Android SDK Platform API 36 and Android Build-Tools 36.0.0.
- The checked-in Gradle wrapper and Android Gradle Plugin versions pinned in `android/`.

Install the required SDK components using Android Studio's SDK Manager or the official command-line tools. Review the applicable Android SDK license terms before accepting them. Do not use unattended license acceptance. Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`, then run:

```bash
cd android
./gradlew assembleDebug
```

The expected output is `android/app/build/outputs/apk/debug/app-debug.apk`. This is a debug-signed local test build, not a Play Store release; no release signing credentials are included. The source review bundle does not include a prebuilt APK.

## Reproducibility and dependency policy

`package-lock.json` pins the resolved npm dependency tree; use `npm ci` for web CI. Native dependency versions are pinned in Gradle build files. Review dependency updates and license changes before merging. Web CI does not build the Android target unless an Android SDK build job is explicitly configured.
