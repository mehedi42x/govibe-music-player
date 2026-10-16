# Contributing to GoVibe

Thanks for helping make GoVibe useful, honest, and maintainable. The app is Kotlin/Jetpack Compose for Android with a small Rust native queue core. Keep local audio, credentials, API tokens, signing material, and build output out of Git.

## Development checks

Run Rust checks from the repository root:

```bash
cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check
cargo test --manifest-path rust/govibe-core/Cargo.toml
cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings
```

To compile the Android app, install JDK 17+, Android SDK API 36/Build-Tools 36.0.0, the NDK version specified in Gradle, Rust/Cargo with Android targets, and `cargo-ndk`; review applicable SDK/NDK terms before installation or use. Then run `cd android && ./gradlew assembleDebug`. Do not use unattended SDK license acceptance. See [Building](docs/BUILDING.md).

## Design expectations

Keep GoVibe local-first: only user-selected audio is indexed; never upload audio or add broad storage access without a separately reviewed requirement. Keep UI, Android APIs, Room, permissions, and Media3 lifecycle in Kotlin. Keep Rust side-effect-free with primitive JNI arguments, and test every boundary/index case.

Keep community extensions declarative JSON only. No extension is bundled by default; do not add executable code, arbitrary network access, credentials, or audio-stream resolution to the extension format. Preserve GoVibe branding, the current application ID, and compatible extension schema unless a reviewed change says otherwise. Do not claim bit-perfect output or capabilities not verified on a device.

## Licensing and publication

GoVibe-authored code in this local review revision is licensed under GNU GPL version 3, SPDX `GPL-3.0-only`. Third-party components and generated Gradle wrapper scripts retain their own licenses and notices; do not relabel them. Earlier public source copies were released with a 0BSD grant, which this change cannot retroactively revoke. Review all changed files before any public push or release; the current migration has not been pushed or published.
