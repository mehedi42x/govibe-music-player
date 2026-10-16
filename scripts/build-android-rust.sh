#!/usr/bin/env bash
# Copyright (C) 2026 Mehedi Hasan
# SPDX-License-Identifier: GPL-3.0-only
#
set -euo pipefail

OUTPUT_DIR="${1:?Usage: build-android-rust.sh <jniLibs-output-directory> [debug|release]}"
PROFILE="${2:-debug}"
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST_PATH="$PROJECT_ROOT/rust/govibe-core/Cargo.toml"

if [[ "$PROFILE" != "debug" && "$PROFILE" != "release" ]]; then
  echo "Unsupported Rust build profile: $PROFILE (expected debug or release)." >&2
  exit 1
fi

if ! command -v cargo >/dev/null 2>&1; then
  echo "Rust Cargo is required to build GoVibe's Android native core." >&2
  exit 1
fi
if ! command -v cargo-ndk >/dev/null 2>&1; then
  echo "cargo-ndk is required. Install it with: cargo install cargo-ndk" >&2
  exit 1
fi

NDK_HOME="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$NDK_HOME" && -n "$SDK_ROOT" && -d "$SDK_ROOT/ndk" ]]; then
  NDK_HOME="$(find "$SDK_ROOT/ndk" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)"
fi
if [[ -z "$NDK_HOME" || ! -d "$NDK_HOME" ]]; then
  echo "Android NDK not found. Install it with Android Studio after reviewing its license, then set ANDROID_NDK_HOME." >&2
  exit 1
fi
export ANDROID_NDK_HOME="$NDK_HOME"
mkdir -p "$OUTPUT_DIR"

BUILD_ARGS=()
if [[ "$PROFILE" == "release" ]]; then
  BUILD_ARGS+=(--release)
fi

cargo ndk \
  -t arm64-v8a \
  -t armeabi-v7a \
  -t x86_64 \
  -o "$OUTPUT_DIR" \
  build --manifest-path "$MANIFEST_PATH" --lib "${BUILD_ARGS[@]}"
