# ADR 0001: Browser prototype and initial 0BSD grant

- **Status:** Superseded for the current review-tree revision. The initial public code release used 0BSD; this local migration changes GoVibe-authored code to GPL-3.0-only going forward.
- **Context:** The initial prototype began as a browser-first React/TypeScript PWA. A native Kotlin/Jetpack Compose Android application was later added for local audio playback and device media integration. The current migration removes the browser client and adds a small Rust queue core connected through JNI.
- **Historical decision:** The earlier revision used [0BSD](https://opensource.org/license/0bsd), SPDX `0BSD`. That license grant was published with earlier copies of the source.
- **Current review decision:** At the user's explicit request, the current local revision (now branded NextVibe; formerly GoVibe) uses GNU GPL version 3, SPDX `GPL-3.0-only`, for project-authored code. The complete license text is in [`LICENSE`](../../LICENSE). Third-party dependencies retain their own terms and notices.
- **Important scope:** A later license change cannot retroactively revoke permissions already granted under 0BSD for copies distributed with that license. The GPL migration is local and will not be pushed to the public repository unless the user reviews the exact changed files and approves publication.
