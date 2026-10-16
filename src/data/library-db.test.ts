import 'fake-indexeddb/auto';
import { afterEach, describe, expect, it } from 'vitest';
import { openDB } from 'idb';
import { DEFAULT_SETTINGS, type Track } from '../domain/models';
import { libraryDb } from './library-db';

const sampleTrack: Track = {
  id: 'test-track-1',
  title: 'Test Song',
  artist: 'Test Artist',
  album: 'Test Album',
  duration: 123,
  mimeType: 'audio/mpeg',
  size: 4,
  addedAt: 1,
  favorite: false,
  audio: new Blob(['test'], { type: 'audio/mpeg' }),
};

afterEach(async () => {
  await libraryDb.clearLibrary();
  await libraryDb.clearHistory();
});

describe('IndexedDB storage', () => {
  it('migrates a v1 library to v3 and removes the legacy plugin-toggle store', async () => {
    const oldDatabase = await openDB('aurora-library', 1, {
      upgrade(database) {
        const tracks = database.createObjectStore('tracks', { keyPath: 'id' });
        tracks.createIndex('by-addedAt', 'addedAt');
        tracks.createIndex('by-artist', 'artist');
        tracks.createIndex('by-album', 'album');
        const playlists = database.createObjectStore('playlists', { keyPath: 'id' });
        playlists.createIndex('by-updatedAt', 'updatedAt');
        database.createObjectStore('settings');
        const history = database.createObjectStore('history', { keyPath: 'id', autoIncrement: true });
        history.createIndex('by-playedAt', 'playedAt');
        database.createObjectStore('plugins', { keyPath: 'id' });
      },
    });
    oldDatabase.close();
    await libraryDb.getTracks();
    const database = await openDB('aurora-library');
    expect(database.version).toBe(3);
    for (const store of ['tracks', 'playlists', 'settings', 'history', 'queue']) {
      expect(database.objectStoreNames.contains(store)).toBe(true);
    }
    expect(database.objectStoreNames.contains('plugins')).toBe(false);
    database.close();
  });

  it('persists settings and returns privacy-safe defaults', async () => {
    expect(await libraryDb.getSettings()).toEqual(DEFAULT_SETTINGS);
    const settings = { ...DEFAULT_SETTINGS, volume: 0.42, locale: 'bn' as const };
    await libraryDb.putSettings(settings);
    expect(await libraryDb.getSettings()).toEqual(settings);
  });

  it('persists audio blobs and queue snapshots across reads', async () => {
    await libraryDb.putTrack(sampleTrack);
    const saved = await libraryDb.getTracks();
    expect(saved[0]?.title).toBe('Test Song');
    expect(await saved[0]?.audio.text()).toBe('test');

    const snapshot = { trackIds: ['test-track-1'], currentIndex: 0, shuffle: true, repeat: 'all' as const };
    await libraryDb.putQueue(snapshot);
    expect(await libraryDb.getQueue()).toEqual(snapshot);
  });

  it('clears listening history without deleting the library', async () => {
    await libraryDb.putTrack(sampleTrack);
    await libraryDb.addHistory({ trackId: sampleTrack.id, playedAt: 10 });
    await libraryDb.clearHistory();
    expect(await libraryDb.getHistory()).toEqual([]);
    expect(await libraryDb.getTracks()).toHaveLength(1);
  });
});
