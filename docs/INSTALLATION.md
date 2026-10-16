# Installation

NextVibe is currently provided as Android source in this review tree, not as a new release APK. Build requirements and the local debug APK task are in [`BUILDING.md`](BUILDING.md). Do not install the review draft over important data without first backing up the device; the Room v1-to-v2 history migration and audio effects still need Android build/device verification.

Fresh installs have an empty library and **no preinstalled or preselected music provider**. The top Online/Offline switch defaults to Online when Android reports validated internet, otherwise to Offline. In Online mode, add a declarative JSON source or scan a public GitHub repository from **Settings → Music sources**, then enable and select it. Sources provide catalogue data only; NextVibe renders all screens.

In Offline mode, use **Scan device audio** to ask Android for the audio-library permission and index shared audio exposed through MediaStore. Android does not grant access to other apps' private folders; if permission is declined, use **Pick files** to import individual audio through the system picker. A source may optionally permit explicit **Download for offline playback** actions; completed copies are saved to shared `Music/NextVibe`, indexed in the library, and never uploaded. Android 10+ uses scoped MediaStore; Android 9 or lower requests write permission only at download time.

Open **Settings → Audio controls** for player volume, the device-session Equalizer presets and optional volume boost (off by default; capped at +3 dB). Effect support varies by device. Preserve original audio files independently of the app.

Project-authored code in this local review is GPL-3.0-only; third-party dependencies retain their own license notices. Earlier public versions remain subject to their prior license. Nothing in this review has been pushed or released.
