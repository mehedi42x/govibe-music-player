# Contributing to NextVibe

Thanks for helping make NextVibe useful, honest and maintainable. The app is Kotlin/Jetpack Compose for Android with a small Rust queue core. Keep local audio, credentials, API tokens, signing material and build output out of Git.

## Development checks

Run Rust checks from the repository root:

```bash
cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check
cargo test --manifest-path rust/govibe-core/Cargo.toml
cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings
```

An Android build also needs JDK 17+, the pinned SDK/NDK, Android Rust targets and `cargo-ndk`. Review SDK/NDK terms first; do not accept licenses unattended. See [Building](docs/BUILDING.md).

## Design expectations

Keep local files and listening history on-device. Index audio only after a user selects files or explicitly requests a MediaStore scan with runtime permission; the scan must not claim access to other apps' private directories. Keep UI, Android APIs, Room, permissions and Media3 lifecycle in Kotlin. Keep Rust side-effect-free with primitive JNI arguments and test queue boundaries.

Community sources remain declarative JSON only. A source may declare a public HTTPS origin plus same-host search/trending paths and returns a bounded standard tracks array with HTTPS artwork and stream URLs. NextVibe owns the UI; sources cannot execute code, provide screens, access the local library, ask for credentials, or add arbitrary headers. Do not bundle a provider, upload listening history, or claim unverified audio/device capabilities. Preserve application ID/JNI compatibility and GPL-3.0-only notices unless a reviewed change requires otherwise.

## Licensing and publication

NextVibe-authored code in this local review revision is licensed under GNU GPL version 3, SPDX `GPL-3.0-only`. Third-party components and Gradle wrapper scripts retain their own licenses and notices. Earlier public source copies were released under 0BSD; this revision cannot revoke that prior grant. Review changes before a public push or release.
