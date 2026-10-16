# Roadmap

## Current local migration review

- GoVibe's current app target is native Android: Kotlin/Jetpack Compose, Room, Android's system document picker, Media3/MediaSessionService, and a declarative JSON/GitHub extension manager.
- The browser PWA, TypeScript provider SDK, and generated web bundle are being removed from the current tree.
- Rust provides a dependency-free queue-navigation core through primitive JNI calls; it is not an audio decoder or extension runtime.
- Fresh installs include no extension profiles. GoVibe's default home belongs to the app itself.

## Verification still required

- Run Rust formatting, host unit tests, and clippy.
- Build the Android debug variant on a separately prepared environment after reviewing SDK/NDK terms; verify JNI packaging and supported ABIs on actual Android devices.
- Test document-picker grants, notifications, audio focus, background playback, seeking, queue behavior, and supported codecs on physical devices.
- Add parser/repository and app tests, plus finished playlist/history UI and library backup/export after product behavior is designed and tested.

## Distribution

- A manual GitHub Actions workflow is being prepared to build and attach a signed APK when the repository owner chooses to run it.
- Release signing requires user-configured GitHub Actions secrets; no signing credentials are present in this tree.
- Workflow execution and public release creation have not been run here. Review the exact workflow and code before pushing or dispatching it.

## License

- GoVibe-authored code in this local review revision is marked GPL-3.0-only.
- Third-party components retain their own licenses and notices.
- Earlier publicly distributed 0BSD copies remain covered by the permissions granted with those copies; this change does not retroactively revoke them.
