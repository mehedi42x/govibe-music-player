## Summary

## User-visible behavior and limitations

## Verification

- [ ] `cargo fmt --manifest-path rust/govibe-core/Cargo.toml -- --check`
- [ ] `cargo test --manifest-path rust/govibe-core/Cargo.toml`
- [ ] `cargo clippy --manifest-path rust/govibe-core/Cargo.toml --all-targets -- -D warnings`
- [ ] Android debug build/device test completed where SDK/NDK licenses and hardware are available

## Review checklist

- [ ] Documentation and migration notes updated where needed.
- [ ] Privacy, permissions, authorization, accessibility, and license implications considered.
- [ ] Third-party license notices are preserved accurately.
- [ ] No credentials, personal data, copyrighted audio, or generated build output committed.
- [ ] Provider capabilities and audio-quality claims match verified behavior.
