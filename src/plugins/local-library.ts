import type { PluginManifest, ProviderPlugin, Track } from '../domain/models';

export const localLibraryManifest: PluginManifest = {
  id: 'aurora.local-library',
  name: 'Local Library',
  version: '1.0.0',
  description: 'Search and play audio files explicitly imported by the listener.',
  sdkVersion: '1.0.0',
  authentication: 'none',
  capabilities: ['search', 'metadata', 'playback'],
  permissions: ['library.read', 'playback.resolve'],
  entry: 'src/plugins/local-library.ts',
};

export function createLocalLibraryProvider(getTracks: () => Track[]): ProviderPlugin {
  let disposed = false;
  return {
    manifest: localLibraryManifest,
    async search(query, signal) {
      if (disposed || signal?.aborted) return [];
      const needle = query.trim().toLocaleLowerCase();
      if (!needle) return [];
      return getTracks()
        .filter((track) =>
          `${track.title} ${track.artist} ${track.album}`.toLocaleLowerCase().includes(needle),
        )
        .map((track) => ({
          id: track.id,
          title: track.title,
          artistName: track.artist,
          albumTitle: track.album,
          durationSeconds: track.duration,
          artworkUrl: track.artwork,
        }));
    },
    async resolve(trackId, signal) {
      if (disposed || signal?.aborted) return null;
      return getTracks().find((track) => track.id === trackId)?.audio ?? null;
    },
    async dispose() {
      disposed = true;
    },
  };
}
