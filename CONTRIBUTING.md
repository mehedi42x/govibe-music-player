# Contributing to GoVibe

Thanks for helping make GoVibe useful, honest, and maintainable. The project includes a browser PWA and a native Kotlin/Jetpack Compose Android client; native build/device verification remains blocked by the SDK license-text mismatch documented in the roadmap. Please open an issue for large changes before investing in implementation.

## Development setup

Use Node.js 22+ and npm. Run `npm ci`, `npm run dev`, then the full check set: `npm run format:check`, `npm run typecheck`, `npm run lint`, `npm test`, `npm run build`, and `npm audit --audit-level=moderate`. Keep local audio, browser data, credentials, API tokens, signing material, and build output out of Git.

## Changes and review

Create a focused branch from the default branch and keep pull requests scoped. Explain user-visible behavior, browser compatibility, migration impact, accessibility, privacy, provider/API permissions, and tests. Include documentation for new behavior and limitations. Add or update deterministic tests; do not claim an integration worked unless it was exercised through a permitted test path. CI must pass before merge.

## Provider proposals

Use the provider proposal issue template. Identify the official API/authorization basis, supported regions/accounts, which capabilities are real, quality data provenance, token handling, rate limits, failure behavior, and applicable licensing/service restrictions. Do not propose unauthorized stream extraction or DRM/subscription bypass.

## Plugin work

The current SDK contract is at `plugin-sdk/` and the starter is under `examples/sample-plugin/`. Third-party runtime execution is not implemented. A manifest passing validation is not approval or a security sandbox. Keep sample code reviewable, request only necessary permissions, and do not introduce a runtime loader until the documented security roadmap is addressed.

## Style and compatibility

Use Prettier (`npm run format` to format and `npm run format:check` to verify), strict TypeScript, small pure core functions where practical, semantic accessible controls, and reduced-motion-friendly transitions. Preserve keyboard and touch operation. Keep English/Bangla text easy to localize and do not add external telemetry or assets without an explicit privacy review.
