# Extension profile authoring

GoVibe extensions are **declarative JSON home profiles**, not executable plugins. There is no JavaScript/TypeScript provider SDK or third-party code runtime in the app.

## Create a profile

Start with the in-app template or a copy of [`examples/extension-profile.example.aurora.json`](../examples/extension-profile.example.aurora.json). Validate the JSON against [`aurora-extension-format/schema-v1.json`](../aurora-extension-format/schema-v1.json) and the constraints in [Extension setup](EXTENSIONS.md).

Each profile may specify a display name, semantic version, author, description, supported UI/theme/metadata capabilities, an accent color, and short plain-text labels/details. Keep values concise and do not include secrets, account data, file paths, executable content, or remote URLs.

## Publish a profile catalog

For community discovery, place one `.aurora.json` file per profile in the `aurora-extensions/` directory at the root of a public GitHub repository. Users initiate a scan in GoVibe, inspect each parsed preview, and choose whether to install it. A URL alone does not trigger a scan. Invalid or unsupported manifests are rejected.

The app downloads JSON data only. It never executes code from the repository and profiles cannot access audio, make network requests, or provide streams. No profile is included with GoVibe by default.
