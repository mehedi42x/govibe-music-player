# Native Android extension profiles

GoVibe's Android extension system accepts `.aurora.json` manifests containing **declarative home-screen presentation data only**. Profiles may set a title, subtitle, badge, accent color, and plain-text detail rows. They cannot execute code, access the music library, make network requests, or supply audio.

The format is defined by [`aurora-extension-format/schema-v1.json`](../aurora-extension-format/schema-v1.json).

## No extensions are installed by default

A fresh installation contains no extension manifests. GoVibe's default home screen is part of the app itself, not an extension. If no user-installed profile is active, the app uses that default home.

## Add a manifest with JSON

1. Open **Extensions** and tap the **plus** icon.
2. Edit or paste a manifest into the JSON editor.
3. Tap **Validate** and review the parsed profile summary.
4. Tap **Add** to install the profile locally.
5. Use the profile card to enable/disable it or set it as the home profile.
6. To return to GoVibe's own home, open **Settings → Home appearance** and select **GoVibe default home**.

A minimal manifest template is available directly in the app. The schema requires the `format`, `formatVersion`, `id`, `name`, `version`, `author`, `description`, `capabilities`, and `presentation` fields.

## Install manifests from GitHub

1. Put one manifest per profile in a public repository's root `aurora-extensions/` directory; filenames must end in `.aurora.json`.
2. In **Extensions → Browse a public GitHub repository**, paste the repository home URL, for example `https://github.com/owner/repository`.
3. Tap **Scan repository**. Pasting a URL alone does not make a request.
4. Review each manifest preview and add only the profiles you choose.

Only public repositories are supported; no GitHub sign-in or token is used. Each scan is user-initiated and capped at 20 manifests. Invalid files cannot be installed. GitHub's unauthenticated API rate limit may apply, and repository contents can change after review.

## Format limits and trust boundary

- `format` must be `aurora-extension`; `formatVersion` must be `1`.
- IDs must use lowercase segments separated by dots or hyphens, must be unique, and cannot start with the reserved `org.aurora.` prefix.
- Declare one to three supported capabilities from `ui.home`, `theme.accent`, and `metadata.display`.
- `presentation` supports `accentColor` (`#RRGGBB`), `homeTitle` (60 characters), `homeSubtitle` (160), `homeBadge` (24), and up to 12 detail rows.
- Each detail row has a `label` (40 characters) and `value` (240 characters).
- Manifests are limited to 64 KiB; unknown fields are rejected.

Downloaded manifests are untrusted input. GoVibe parses and renders bounded text/color values only; it never downloads or executes extension code.

## Compatibility

This JSON profile importer is implemented for the native Android client. The separate web PWA's TypeScript provider SDK is a different contract and is not installed or executed by Android.
