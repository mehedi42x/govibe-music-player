# Troubleshooting

## Android APK is missing / Gradle says SDK components are unavailable

An Android build needs the SDK Platform/Build Tools listed in [Building](BUILDING.md), the pinned NDK, Rust and `cargo-ndk`. Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME`, then run `cd android && ./gradlew testDebugUnitTest assembleDebug` in a fully provisioned environment.

## Play Protect or unknown-publisher warning

The experimental GitHub draft APK is signed with Android's debug certificate, not Google Play App Signing or a production publisher key. Sideloaded debug builds have little/no publisher reputation, so Android can display an unknown-publisher or Play Protect warning; this is not fixed by changing the app title, permissions, or package metadata. The legitimate release path is a stable, privately stored production signing key and distribution through Play Console/Play App Signing. Every update to an already installed package must be signed by the same key; a different debug key can make Android reject an update. Do not bypass Play Protect. Verify the APK's SHA-256 from the release before installing, and only install if you trust the source.

## Device scan shows no tracks or permission was declined

The scan reads only shared audio entries exposed by Android MediaStore. It cannot read another app's private folders or protected storage. If audio permission is missing, tap **Allow and scan** and grant Android audio access; after that the app auto-scans when opened/resumed and hides the scan action. Use **Pick files** to select individual tracks through the system picker, or return to the app after adding shared audio to refresh the library.

## Online mode is unavailable

The switch follows Android's validated-internet state. A captive portal or network without verified internet selects Offline mode; try reconnecting or use Offline until Android validates the network. When online, add and select a music source from Settings before browsing its trending/search catalogue.

## Source feed/search is empty or fails

Check that a source is selected and enabled, its public HTTPS origin is reachable, and its trending/search paths return JSON with a `tracks` array. Each playable result needs `id`, `title` and a direct HTTPS `streamUrl`. Search paths must contain exactly one `{query}` placeholder. Oversized responses, over 60 tracks, HTTP endpoints, redirects and nonstandard ports are rejected.

## Artwork or source playback does not appear

Artwork loads from a public HTTPS URL, follows at most four validated HTTPS redirects, and is limited to 5 MiB; invalid redirects, unsupported images and failed requests show built-in fallback artwork. Playback relies on Android Media3 and the source's direct stream URL, so unsupported codecs, DRM/authentication requirements or network errors may prevent it.

## Download button is missing or an offline copy fails

The selected source must declare `downloadsAllowed: true`; the button is intentionally hidden for all other sources. Downloads accept only a public HTTPS response and are limited to 250 MiB and four redirects. DRM/authenticated streams, responses declared as text/HTML or unrelated media, empty responses, and files without an identifiable audio MIME type or extension are rejected. Android 10+ saves through scoped MediaStore to `Music/NextVibe`; on Android 9 and below, grant write permission when prompted. Check free storage and the provider's terms if a download fails.

## YouTube Music or Last.fm integration is unavailable

YouTube Music search opens the official YouTube Music app/site; the official YouTube Data API does not grant an app the right to extract audio, play it in a hidden/background player, or save it offline. Last.fm account sync/scrobbling needs a registered Last.fm API key, shared secret, and an authenticated user session. These credentials are not configured for this preview, so only the official Last.fm website link is available.

## Volume boost or EQ has no effect

Volume boost is off by default and capped at +3 dB. Equalizer and LoudnessEnhancer effects attach to the current Android audio session; some devices/audio routes do not support them. Try another route, restart playback, check system media volume, and begin with boost at zero. Excess gain can clip audio.

## Report a bug safely

Include Android version, source/API response shape without secrets, steps, and expected/actual behavior. Do not attach private audio, account credentials or tokens. Security-sensitive issues should use a private reporting route once available.
