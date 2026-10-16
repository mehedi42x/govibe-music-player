# Third-party notices

This notice covers third-party components in the current Kotlin Android + Rust source tree. GoVibe-authored code is covered by the root [`GPL-3.0-only` license](LICENSE); that does not change the licenses of third-party software.

## Android runtime dependencies

The direct AndroidX and Media3 dependencies below are distributed under the Apache License, Version 2.0. Compose module versions are aligned by the Compose BOM.

| Direct AndroidX dependency | Declared version |
|---|---:|
| `androidx.compose:compose-bom` | `2026.09.00` (version alignment platform) |
| `androidx.core:core-ktx` | `1.19.1` |
| `androidx.activity:activity-compose` | `1.13.0` |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.11.0` |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.11.0` |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.11.0` |
| `androidx.compose.ui:ui` and `ui-tooling-preview` | Compose BOM `2026.09.00` |
| `androidx.compose.material3:material3` | Compose BOM `2026.09.00` |
| `androidx.compose.material:material-icons-extended` | Compose BOM `2026.09.00` |
| `androidx.navigation:navigation-compose` | `2.10.2` |
| `androidx.media3:media3-exoplayer` and `media3-session` | `1.11.1` |
| `androidx.room:room-runtime` and `room-ktx` | `2.8.5` |

The full Apache-2.0 license text and AndroidX/Media3 notices are preserved in `android/app/src/main/assets/licenses/Apache-2.0.txt` and `android/app/src/main/assets/licenses/THIRD_PARTY_NOTICES.txt`. Upstream sources: [AndroidX](https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt) and [Media3](https://github.com/androidx/media).

## Build tools

The checked-in Gradle wrapper scripts are generated Gradle files with their own Apache-2.0 license headers. Do not replace those notices with GoVibe's GPL header. GitHub Actions used by the workflows are external build services/actions and are not bundled into the APK.

## Rust dependencies and audit status

The Rust queue crate currently has no third-party Cargo dependencies. JUnit, AndroidX test libraries, Compose tooling, Kotlin/Gradle build plugins, and transitive Android dependencies are not exhaustively inventoried here. A complete resolved dependency/license report must be generated and reviewed after an Android SDK build environment is available, before distributing an APK. Verify the Apache-2.0 assets are packaged in the first release build; Gradle currently excludes dependency `META-INF/LICENSE*` and `META-INF/NOTICE*` resources.
