# Plugin development (SDK v1.0.0)

The public contract is exported from `plugin-sdk/index.ts`. Implement `ProviderPlugin` and provide a manifest that passes `validateManifest`. The app-internal `src/plugins/local-library.ts` adapter powers core local-library search; it is not a separately installed extension. No third-party provider package is bundled with or executed by the app.

**Scope:** This guide describes the separate web TypeScript SDK only. The native Android client uses the data-only `.aurora.json` home-profile format documented in [`EXTENSIONS.md`](EXTENSIONS.md); Android does not install or execute these TypeScript packages.

## Manifest and compatibility

A manifest contains a unique lowercase dotted/hyphenated `id`, display `name`, semantic `version`, `description`, exact `sdkVersion`, explicit `authentication` mode (`none`, `oauth2`, `api-key`, or `account`), declared `capabilities`, least-privilege `permissions`, and a relative `entry` path. SDK v1.0.0 accepts `search`, `metadata`, `playback`, `playlists`, `lyrics`, `scrobbling`, and `recommendations`. Permission names are `library.read`, `playback.resolve`, `network.request`, and `account.read`. Authenticated manifests must declare both `account.read` and `network.request`; no OAuth/API-key broker or token vault exists in this release.

## Data and operations

The SDK publishes provider track, album, artist, playlist, lyrics, and audio-format metadata types. `search(query, signal?)` returns provider track metadata; `resolve(trackId, signal?)` returns a playable Blob or `null`. Optional methods expose track/album/artist/playlist details, lyrics, recommendations, and scrobbling. Implement only capabilities declared in the manifest. A future runtime must broker playback and credentials instead of trusting plugin declarations.

Operations are asynchronous and accept `AbortSignal` where applicable. Abort stale requests promptly and dispose plugin-owned resources. Failures may use `ProviderError` with stable codes (`ABORTED`, `AUTH_REQUIRED`, `AUTH_EXPIRED`, `RATE_LIMITED`, `NETWORK`, `UNSUPPORTED`, `NOT_FOUND`, `PERMISSION_DENIED`, `INVALID_RESPONSE`, `INTERNAL`) plus `retryable` and optional `retryAfterMs`. Do not return fabricated metadata after an error.

## Test expectations

Test empty queries, cancellation, unsupported tracks, auth expiry for providers that need it, rate limits, invalid responses, declared permissions, repeated initialization/disposal, offline behavior, and truthful codec/quality claims. Use deterministic mocks in CI; run real-provider tests only with authorized test credentials and service permission.

## Packaging and installation status

The example uses a relative import into this repository because the SDK is not yet published as an npm package. A future distributable format should specify signed packages, dependency locks, manifest/content validation, integrity checks, permission review, installation, update, diagnostics, isolation, and removal. **GoVibe 0.1.0 does not discover, install, activate, update, or execute external packages.** Passing manifest validation is not a security approval or sandbox.

Never bundle secrets or copyrighted audio. Follow API terms and licensing rules. Do not implement protected stream extraction, DRM circumvention, or subscription bypass.
