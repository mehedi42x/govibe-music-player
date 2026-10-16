# Music-source integrations

NextVibe has an optional, user-selected music-source contract. **No provider is bundled or enabled by default**, and the app does not scrape arbitrary sites or install executable provider code. See [Extensions](EXTENSIONS.md) and the [provider contract](PLUGIN_DEVELOPMENT.md).

| Capability | Status | Boundary |
|---|---|---|
| Local library | System picker plus opt-in shared-audio MediaStore scan; Room metadata, favorites, local-history ranking and Media3 playback | Android exposes shared media only. A scan needs runtime permission; other apps' private folders are not accessible. |
| User-added music source | Declarative HTTPS manifest with trending/search endpoints; normalized track metadata, artwork and direct stream URLs render in NextVibe UI | No arbitrary headers, credentials, code, scraping, protected-stream extraction, DRM bypass or provider UI. Source compatibility and permissions depend on each service. |
| Public GitHub manifests | User-initiated repository scan, preview and install | Public GitHub API discovery only; it does not itself provide audio. |
| Offline playback | Indexed or individually selected local audio | No remote downloads or cached streaming catalogue. |
| Audio controls | Player volume, device-session EQ presets and optional +3 dB max boost | Android AudioEffect support varies by device; no bit-perfect claim. |

## Integration rules

- Declare capabilities honestly; a valid catalogue response does not guarantee that its stream is reachable or playable.
- Use documented, authorized interfaces and permitted playback mechanisms.
- Do not spoof clients, extract protected streams, bypass subscriptions, or circumvent DRM.
- Keep API keys and user tokens out of source control and logs. The current declarative source schema does not support private credentials.
- Handle cancellation, offline states, expiry, source rate limits and invalid data.
- Do not mark codec support, output quality, authentication, or device effects as verified until tested on supported Android devices.
