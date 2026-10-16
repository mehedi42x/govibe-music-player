# Troubleshooting

## Android APK is missing / Gradle says SDK components are unavailable

The source review tree does not include an APK. Install the Android SDK Platform and Build-Tools versions listed in [Building](BUILDING.md), plus the pinned Android NDK, Rust, and `cargo-ndk`. Review SDK/NDK license terms before installing or using the components; the project does not accept them unattended. Set `ANDROID_HOME` or `ANDROID_SDK_ROOT`, and `ANDROID_NDK_HOME`, then run `cd android && ./gradlew assembleDebug`.

## Rust build says cargo-ndk or Android NDK is missing

Install Rust/Cargo and `cargo-ndk`, then install the NDK version pinned in `android/app/build.gradle` after reviewing its terms. Set `ANDROID_NDK_HOME` or `ANDROID_NDK_ROOT`. The build script does not download an NDK or accept any SDK license.

## A selected audio file will not import or later disappears

Use Android's system document picker and select an audio file whose document provider grants read access. Some cloud providers may require a connection or may revoke access; moving/deleting the source can break its URI. GoVibe stores metadata and a URI reference rather than copying audio. Re-select the original file if access is lost.

## A selected audio file will not play

Codec/container support varies by Android version and device decoder. Try another local file/format. Confirm the original document is still available and that system media volume is audible. Audio focus or a noisy-route event can pause playback. GoVibe does not transcode or promise lossless/bit-perfect output.

## The library appears empty

Android library data is local to the app installation. Clearing app data or reinstalling removes local metadata/settings; import files again from their original locations.

## No extensions are listed

That is the normal fresh-install state. GoVibe's default home is part of the app and is not an extension. Add a profile with the plus-button JSON editor or scan a public GitHub repository containing `.aurora.json` files in its root `aurora-extensions/` folder.

## GitHub extension scan finds no results or fails

Use a public repository home URL in the form `https://github.com/owner/repository`, not a file, branch, pull request, or issue URL. A scan starts only after **Scan repository** is tapped and is capped at 20 manifest files; GitHub's unauthenticated API rate limit may apply. Review the preview and correct invalid JSON/unsupported fields before adding.

## Report a bug safely

Include the Android OS version, steps, and expected/actual behavior. Do not attach private audio or account credentials. Security-sensitive issues should use a private reporting route once available.
