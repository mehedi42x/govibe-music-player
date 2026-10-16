# Troubleshooting

## Android APK is missing / Gradle says SDK components are unavailable

The source review bundle does not contain a prebuilt APK. Install the Android SDK Platform and Build-Tools versions listed in [Building](BUILDING.md), set `ANDROID_HOME` or `ANDROID_SDK_ROOT`, then run `cd android && ./gradlew assembleDebug`. Review any SDK license terms presented by the official tools before accepting them; do not bypass a toolchain requirement.

## A selected audio file will not import or later disappears (Android)

Use Android's system document picker and select an audio file whose document provider grants read access. Some cloud providers may require a connection or may revoke access; moving/deleting the source can break its URI. GoVibe stores metadata and a URI reference rather than copying audio. Re-select the original file if access is lost.

## A selected audio file will not play (Android)

Codec/container support varies by Android version and device decoder. Try another local file/format. Confirm the original document is still available and that system media volume is audible. Audio focus or a noisy-route event can pause playback. GoVibe does not transcode or promise lossless/bit-perfect output.

## The library appears empty (Android)

Android library data is local to the app installation. The web PWA and Android app use separate stores and do not synchronize. Clearing app data or reinstalling removes local metadata/settings; import files again from their original locations.

## No extensions are listed

That is the normal fresh-install state. GoVibe's default home is part of the app and is not an extension. Add a profile with the plus-button JSON editor or scan a public GitHub repository containing `.aurora.json` files in its root `aurora-extensions/` folder.

## GitHub extension scan finds no results or fails

Use a public repository home URL in the form `https://github.com/owner/repository`, not a file, branch, pull request, or issue URL. A scan starts only after **Scan repository** is tapped and is capped at 20 manifest files; GitHub's unauthenticated API rate limit may apply. Review the preview and correct invalid JSON/unsupported fields before adding.

## Web PWA file or install problems

Browser codec support and IndexedDB storage quotas differ. Keep original files; clearing browser/site data or eviction can remove the web library. PWA installation and service workers require a supported browser and a secure origin such as `localhost` or HTTPS.

## Report a bug safely

Include the client (Android or web), OS/browser version, steps, and expected/actual behavior. Do not attach private audio or account credentials. Security-sensitive issues should use a private reporting route once available.
