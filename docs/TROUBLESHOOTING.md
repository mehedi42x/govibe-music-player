# Troubleshooting

## Android APK is missing / Gradle says SDK components are unavailable

This source review does not include a new APK. An Android build needs the SDK Platform/Build Tools listed in [Building](BUILDING.md), the pinned NDK, Rust and `cargo-ndk`. Review SDK/NDK license terms before installation or use. Set `ANDROID_HOME`/`ANDROID_SDK_ROOT` and `ANDROID_NDK_HOME`, then run `cd android && ./gradlew testDebugUnitTest assembleDebug` in a fully provisioned environment.

## Device scan shows no tracks or permission was declined

The scan reads only shared audio entries exposed by Android MediaStore. It cannot read another app's private folders or protected storage. Tap **Scan device audio** and grant Android audio permission, or use **Pick files** to select individual tracks through the system picker. Re-run the scan after adding new shared audio.

## Online mode is unavailable

The switch follows Android's validated-internet state. A captive portal or network without verified internet selects Offline mode; try reconnecting or use Offline until Android validates the network. When online, add and select a music source from Settings before browsing its trending/search catalogue.

## Source feed/search is empty or fails

Check that a source is selected and enabled, its public HTTPS origin is reachable, and its trending/search paths return JSON with a `tracks` array. Each playable result needs `id`, `title` and a direct HTTPS `streamUrl`. Search paths must contain exactly one `{query}` placeholder. Oversized responses, over 60 tracks, HTTP endpoints, redirects and nonstandard ports are rejected.

## Artwork or source playback does not appear

Artwork loads from a public HTTPS URL, follows at most four validated HTTPS redirects, and is limited to 5 MiB; invalid redirects, unsupported images and failed requests show built-in fallback artwork. Playback relies on Android Media3 and the source's direct stream URL, so unsupported codecs, DRM/authentication requirements or network errors may prevent it.

## Download button is missing or an offline copy fails

The selected source must declare `downloadsAllowed: true`; the button is intentionally hidden for all other sources. Downloads accept only a public HTTPS response and are limited to 250 MiB and four redirects. DRM/authenticated streams, responses declared as text/HTML or unrelated media, empty responses, and files without an identifiable audio MIME type or extension are rejected. Android 10+ saves through scoped MediaStore to `Music/NextVibe`; on Android 9 and below, grant write permission when prompted. Check free storage and the provider's terms if a download fails.

## Volume boost or EQ has no effect

Volume boost is off by default and capped at +3 dB. Equalizer and LoudnessEnhancer effects attach to the current Android audio session; some devices/audio routes do not support them. Try another route, restart playback, check system media volume, and begin with boost at zero. Excess gain can clip audio.

## Report a bug safely

Include Android version, source/API response shape without secrets, steps, and expected/actual behavior. Do not attach private audio, account credentials or tokens. Security-sensitive issues should use a private reporting route once available.
