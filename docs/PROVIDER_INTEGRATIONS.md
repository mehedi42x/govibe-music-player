# Music-source integrations

NextVibe has an optional, user-selected music-source contract. **No provider is bundled or enabled by default**, and the app does not scrape arbitrary sites or install executable provider code. See [Extensions](EXTENSIONS.md) and the [provider contract](PLUGIN_DEVELOPMENT.md).

| Capability | Status | Boundary |
|---|---|---|
| Local library | System picker plus shared-audio MediaStore scan; Room metadata, favorites, local-history ranking and Media3 playback | Android exposes shared media only. A permission action appears only while audio access is missing; after grant the app scans automatically. Other apps' private folders are not accessible. |
| User-added music source | Declarative HTTPS manifest with trending/search endpoints; normalized track metadata, artwork and direct stream URLs render in NextVibe UI | No arbitrary headers, credentials, code, scraping, protected-stream extraction, DRM bypass or provider UI. Source compatibility and permissions depend on each service. |
| Public GitHub manifests | User-initiated repository scan, preview and install | Public GitHub API discovery only; it does not itself provide audio. |
| YouTube Music | Opens the official app/site; search can hand a query to the official service | No in-app audio extraction, hidden/background playback, or offline copies under the [YouTube API policies](https://developers.google.com/youtube/terms/developer-policies); the official [YouTube Data API](https://developers.google.com/youtube/v3) does not provide YouTube Music stream playback. |
| Last.fm | Official website link | In-app account sync/scrobbling is not enabled. Authenticated writes require a registered API key, shared secret, and user session ([auth](https://www.last.fm/api/authspec), [scrobbling](https://www.last.fm/api/scrobbling)); none is configured for this build. |
| Offline playback | Indexed or individually selected local audio | No remote downloads or cached streaming catalogue. |
| Audio controls | Player volume, device-session EQ presets and optional +3 dB max boost | Android AudioEffect support varies by device; no bit-perfect claim. |

## Integration rules

- Declare capabilities honestly; a valid catalogue response does not guarantee that its stream is reachable or playable.
- Use documented, authorized interfaces and permitted playback mechanisms.
- YouTube Music links/search hand off to the official service; do not recreate its player, extract audio, or bypass service controls.
- Last.fm API work must use a registered app key/secret and user-authorized session; never invent credentials or collect a user's Last.fm password.
- Do not spoof clients, extract protected streams, bypass subscriptions, or circumvent DRM.
- Keep API keys and user tokens out of source control and logs. The current declarative source schema does not support private credentials.
- Handle cancellation, offline states, expiry, source rate limits and invalid data.
- Do not mark codec support, output quality, authentication, or device effects as verified until tested on supported Android devices.
