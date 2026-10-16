# Provider integrations

GoVibe separates local audio playback, metadata, and home-screen presentation profiles. A declarative UI profile is not a streaming provider. **No online music provider or extension is bundled or enabled by default.**

| Source                         | Native Android                                                                   | Web PWA                                                   | Network/audio status                                              |
| ------------------------------ | -------------------------------------------------------------------------------- | --------------------------------------------------------- | ----------------------------------------------------------------- |
| Local library                  | Implemented: user-selected documents, best-effort metadata, Media3 playback      | Implemented: local browser files/tags and browser audio   | Local content only; codecs and output behavior vary by device     |
| Public GitHub manifests        | User-initiated scan, preview, and install of declarative `.aurora.json` profiles | Native importer is not available in the PWA               | Public GitHub Contents API only; not an audio source              |
| Spotify                        | Not implemented                                                                  | Planned only                                              | Requires authorized API access and an approved playback mechanism |
| Last.fm                        | Not implemented                                                                  | Planned only                                              | Discovery/scrobbling does not itself provide playable audio       |
| Executable community providers | Not supported                                                                    | SDK contract exists, but installation/runtime is disabled | Third-party code is not installed or executed                     |

## Integration rules

- Declare capabilities honestly; a presentation profile does not provide search, authentication, or audio.
- Use documented, authorized interfaces and permitted playback mechanisms.
- Do not spoof clients, extract protected streams, bypass subscriptions, or circumvent DRM.
- Keep API keys and user tokens out of source control and logs.
- Handle cancellation, offline states, expiry, and service rate limits.
- Do not mark search, authentication, audio quality, or playback as implemented until tested through a permitted service path.
