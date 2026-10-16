# Integrations

GoVibe currently provides local audio playback and a public GitHub JSON-manifest scanner. A declarative home-screen profile is not a streaming provider. **No online music provider or executable extension is bundled or enabled by default.**

| Source | Status | Network/audio behavior |
| --- | --- | --- |
| Local library | Implemented: user-selected documents, best-effort metadata, Media3 playback | Local content only; codec and output behavior vary by device |
| Public GitHub manifests | User-initiated scan, preview, and install of declarative `.aurora.json` profiles | Public GitHub Contents API only; never an audio source |
| Streaming music services | Not implemented | No streaming, extraction, or provider credentials |
| Executable community providers | Not supported | Community data is JSON only; no code is installed or executed |

## Integration rules

- Declare capabilities honestly; a presentation profile does not provide search, authentication, or audio.
- Use documented, authorized interfaces and permitted playback mechanisms for any future integration.
- Do not spoof clients, extract protected streams, bypass subscriptions, or circumvent DRM.
- Keep API keys and user tokens out of source control and logs.
- Handle cancellation, offline states, expiry, and service rate limits.
- Do not mark search, authentication, audio quality, or playback as implemented until tested through a permitted service path.
