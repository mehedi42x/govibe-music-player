# Privacy

GoVibe is local-first and provides no first-party account service.

## Native Android app

- No sign-in, analytics, advertising, scrobbling, or provider API key is active.
- No extension is installed by default. User-installed declarative profile JSON, library metadata, selected document URI references, favorites, and appearance settings are stored locally in Room or app preferences.
- Audio files are not copied to GoVibe's server or uploaded. Playback reads the user-selected document URI through Android's media stack. A URI grant does not guarantee the original file will remain available if moved or deleted.
- The app requests `INTERNET` only to support a user-initiated scan of public GitHub repositories for `.aurora.json` profiles. The unauthenticated GitHub Contents API is contacted only when the user explicitly scans; the app does not transmit the audio library, credentials, or listening history. GitHub's own privacy policy applies to that request.
- No broad media-library permission is requested. Android's system document picker grants access only to files the user selects.
- Standard Android app-data backup is disabled. GoVibe does not encrypt Room data or app preferences; protect the device and its user account.
- Removing app data or uninstalling may erase library metadata and settings. Keep original audio files separately; export/restore is not implemented.

## Web PWA

The separate web app stores selected audio Blobs and metadata, playlists, settings, history, and queue state in IndexedDB for the current browser profile. Browser quotas and eviction policies apply. The browser app does not upload or synchronize the library. Clearing site data can remove it; retain original audio files.

## Data handling summary

| Data                     | Native Android                                                                    | Web PWA                           |
| ------------------------ | --------------------------------------------------------------------------------- | --------------------------------- |
| Audio bytes              | Read via selected document URI; not uploaded or copied to a service               | Stored as local IndexedDB Blobs   |
| Track metadata/favorites | Local Room database                                                               | Local IndexedDB                   |
| Extension profiles       | None preinstalled; user-added JSON is local; public GitHub scan is user-initiated | No extension installation/runtime |
| Accounts/analytics       | None                                                                              | None                              |
