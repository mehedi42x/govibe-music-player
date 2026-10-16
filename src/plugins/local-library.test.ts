import { describe, expect, it } from 'vitest';
import type { Track } from '../domain/models';
import { createLocalLibraryProvider } from './local-library';

const audio = new Blob(['test-audio'], { type: 'audio/mpeg' });
const track: Track = {
  id: 'song-1',
  title: 'Blue Morning',
  artist: 'Aurora Demo',
  album: 'First Light',
  duration: 42,
  mimeType: 'audio/mpeg',
  size: 10,
  addedAt: 1,
  favorite: false,
  audio,
};

describe('local library provider', () => {
  it('searches only explicitly supplied local metadata', async () => {
    const provider = createLocalLibraryProvider(() => [track]);
    expect(await provider.search('blue')).toEqual([
      {
        id: 'song-1',
        title: 'Blue Morning',
        artistName: 'Aurora Demo',
        albumTitle: 'First Light',
        durationSeconds: 42,
      },
    ]);
    expect(await provider.search('missing')).toEqual([]);
    expect(await provider.search('blue', AbortSignal.abort())).toEqual([]);
    await provider.dispose();
  });

  it('resolves a local audio Blob and stops after disposal', async () => {
    const provider = createLocalLibraryProvider(() => [track]);
    expect(await provider.resolve('song-1')).toBe(audio);
    expect(await provider.resolve('missing')).toBeNull();
    await provider.dispose();
    expect(await provider.resolve('song-1')).toBeNull();
  });
});
