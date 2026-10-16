# Privacy

GoVibe is local-first and provides no first-party account service.

## Android app

- No sign-in, analytics, advertising, scrobbling, or provider API key is active.
- No extension is installed by default. User-installed declarative profile JSON, library metadata, selected document URI references, favorites, and appearance settings are stored locally in Room or app preferences.
- Audio files are not copied to GoVibe's server or uploaded. Playback reads the user-selected document URI through Android's media stack. A URI grant does not guarantee the original file will remain available if moved or deleted.
- The app requests `INTERNET` only to support a user-initiated scan of public GitHub repositories for `.aurora.json` profiles. The unauthenticated GitHub Contents API is contacted only when the user explicitly scans; the app does not transmit the audio library, credentials, or listening history. GitHub's own privacy policy applies to that request.
- No broad media-library permission is requested. Android's system document picker grants access only to files the user selects.
- Standard Android app-data backup is disabled. GoVibe does not encrypt Room data or app preferences; protect the device and its user account.
- Removing app data or uninstalling may erase library metadata and settings. Keep original audio files separately; export/restore is not implemented.

## Data handling summary

| Data | Android handling |
| --- | --- |
| Audio bytes | Read via selected document URI; not uploaded or copied to a service |
| Track metadata/favorites | Local Room database |
| Extension profiles | None preinstalled; user-added JSON is local; public GitHub scan is user-initiated |
| Accounts/analytics | None |

The Rust queue core receives only primitive navigation values through JNI. It does not read files, network data, personal data, or extension manifests.
