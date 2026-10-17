# Installation

Preview APKs are experimental and debug-signed, not production or Google Play builds. Android may identify the publisher as unknown or show a Play Protect sideload warning because the APK has no Play signing/reputation. See [Troubleshooting](TROUBLESHOOTING.md#play-protect-or-unknown-publisher-warning) before installing. Build requirements and the local debug APK task are in [`BUILDING.md`](BUILDING.md).

Fresh installs have an empty library and **no preinstalled or preselected music provider**. The top Online/Offline switch defaults to Online when Android reports validated internet, otherwise to Offline. In Online mode, add a declarative JSON source or scan a public GitHub repository from **Settings → Music sources**, then enable and select it. Sources provide catalogue data only; NextVibe renders all screens.

If Android audio-read permission has not been granted, **Allow and scan** is shown; tapping it requests permission. Once granted, NextVibe automatically indexes shared audio exposed through MediaStore on launch/resume and hides the scan action. Android does not expose other apps' private folders; if permission is declined, use **Pick files** to import individual audio through the system picker. A source may optionally permit explicit **Download for offline playback** actions; completed copies are saved to shared `Music/NextVibe`, indexed in the library, and never uploaded. Android 10+ uses scoped MediaStore; Android 9 or lower requests write permission only at download time.

Open **Settings → Audio controls** for player volume, the device-session Equalizer presets and optional volume boost (off by default; capped at +3 dB). Effect support varies by device. Preserve original audio files independently of the app.

Project-authored code is GPL-3.0-only; third-party dependencies retain their own license notices. Earlier public versions remain subject to their prior license.
