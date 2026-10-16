# ADR 0001: Browser-first foundation and 0BSD

- **Status:** Superseded for the primary mobile target; the web PWA remains a supported independent client.
- **Context:** The initial prototype began as a browser-first React/TypeScript PWA with local audio and IndexedDB. Native background playback and device-specific audio behavior could not be established by the browser client alone.
- **Decision:** Retain the PWA as a separate client and add a native Kotlin/Jetpack Compose Android app using Android's media stack and local persistence. The Android app does not wrap or embed the PWA. Each client owns its UI, playback path, and data store.
- **License:** Use OSI-approved [0BSD](https://opensource.org/license/0bsd), SPDX [`0BSD`](https://spdx.org/licenses/0BSD.html). This permissive license avoids inventing a copyright holder or legal identity and supports varied licenses for separately maintained extensions. Trade-off: 0BSD does not provide an express patent grant; contributors should seek review before adopting patent-sensitive code.
- **Revisit when:** A further native platform, extension isolation design, or patent/licensing concern is established.
