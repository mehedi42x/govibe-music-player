# Roadmap

## Web PWA foundation

- Responsive local-first PWA shell, offline app-asset cache, browser audio import/playback, queue, playlists, favorites, and history.
- IndexedDB library/settings/queue persistence and versioned web provider contract.
- Continue browser/device playback, storage-quota, keyboard, accessibility, and English/Bangla localization testing.
- Design a user-controlled library export/backup and restore path.

## Native Android — source prepared, build/device verification pending

- Compose application shell, Room entities/DAOs, selected-file URI import, library search/favorites, Media3 ExoPlayer, and MediaSessionService source.
- Declarative extension profile system, JSON editor/template, public GitHub manifest scan/preview, enable/disable control, and active-profile settings. No profile manifest is bundled; the app's default home is part of the GoVibe UI.
- Build with the documented Android SDK and fix any compiler/API errors; test import/URI access, notifications, audio focus, background playback, seek/queue behavior, and supported codecs on physical Android devices.
- Add parser/repository and app tests, plus finished playlist/history UI and backup/export after product behavior is designed and tested.

## Extension ecosystem

- The Android `.aurora.json` format is declarative UI metadata only, not an executable provider package.
- The web TypeScript provider SDK is a separate contract; third-party package installation/runtime is not enabled.
- Before adding executable plugins, design and review a restricted runtime, explicit permission grants, integrity/signature checks, resource limits, failure containment, diagnostics, updates, and removal.

## Provider integrations

- Evaluate documented, authorized APIs individually; no online provider is enabled or bundled by default.
- Consider discovery/scrobbling only with clear consent; it does not supply playable audio.
- Do not spoof client identities, extract streams, bypass subscriptions, or circumvent DRM.

## Distribution

- Complete local build and device review before preparing a signed release artifact.
- No signing credential or automated release publication is configured.
