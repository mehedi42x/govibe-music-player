import { openDB, type DBSchema, type IDBPDatabase } from 'idb';
import type { QueueState } from '../core/queue';
import type { AppSettings, PlaybackRecord, Playlist, Track } from '../domain/models';
import { DEFAULT_SETTINGS } from '../domain/models';

export type QueueSnapshot = Pick<QueueState, 'trackIds' | 'currentIndex' | 'shuffle' | 'repeat'>;

interface AuroraDB extends DBSchema {
  tracks: {
    key: string;
    value: Track;
    indexes: { 'by-addedAt': number; 'by-artist': string; 'by-album': string };
  };
  playlists: { key: string; value: Playlist; indexes: { 'by-updatedAt': number } };
  settings: { key: string; value: AppSettings };
  history: { key: number; value: PlaybackRecord; indexes: { 'by-playedAt': number } };
  queue: { key: string; value: QueueSnapshot };
}

let database: Promise<IDBPDatabase<AuroraDB>> | undefined;
function db() {
  database ??= openDB<AuroraDB>('aurora-library', 3, {
    upgrade(upgradeDb, oldVersion) {
      if (oldVersion < 1) {
        const tracks = upgradeDb.createObjectStore('tracks', { keyPath: 'id' });
        tracks.createIndex('by-addedAt', 'addedAt');
        tracks.createIndex('by-artist', 'artist');
        tracks.createIndex('by-album', 'album');
        const playlists = upgradeDb.createObjectStore('playlists', { keyPath: 'id' });
        playlists.createIndex('by-updatedAt', 'updatedAt');
        upgradeDb.createObjectStore('settings');
        const history = upgradeDb.createObjectStore('history', { keyPath: 'id', autoIncrement: true });
        history.createIndex('by-playedAt', 'playedAt');
      }
      if (oldVersion < 2) upgradeDb.createObjectStore('queue');
      if (oldVersion < 3) {
        const legacyDatabase = upgradeDb as unknown as IDBDatabase;
        if (legacyDatabase.objectStoreNames.contains('plugins')) {
          legacyDatabase.deleteObjectStore('plugins');
        }
      }
    },
  });
  return database;
}

export const libraryDb = {
  async getTracks() {
    return (await db()).getAll('tracks');
  },
  async putTrack(track: Track) {
    await (await db()).put('tracks', track);
  },
  async putTracks(tracks: Track[]) {
    const database = await db();
    const transaction = database.transaction('tracks', 'readwrite');
    await Promise.all([...tracks.map((track) => transaction.store.put(track)), transaction.done]);
  },
  async deleteTrack(id: string) {
    await (await db()).delete('tracks', id);
  },
  async getPlaylists() {
    return (await db()).getAll('playlists');
  },
  async putPlaylist(playlist: Playlist) {
    await (await db()).put('playlists', playlist);
  },
  async deletePlaylist(id: string) {
    await (await db()).delete('playlists', id);
  },
  async getSettings() {
    return (await (await db()).get('settings', 'app')) ?? DEFAULT_SETTINGS;
  },
  async putSettings(settings: AppSettings) {
    await (await db()).put('settings', settings, 'app');
  },
  async addHistory(record: PlaybackRecord) {
    await (await db()).add('history', record);
  },
  async getHistory() {
    return (await db()).getAllFromIndex('history', 'by-playedAt');
  },
  async clearHistory() {
    await (await db()).clear('history');
  },
  async getQueue() {
    return (await db()).get('queue', 'current');
  },
  async putQueue(snapshot: QueueSnapshot) {
    await (await db()).put('queue', snapshot, 'current');
  },
  async clearLibrary() {
    const database = await db();
    const transaction = database.transaction(['tracks', 'playlists', 'history', 'queue'], 'readwrite');
    await Promise.all([
      transaction.objectStore('tracks').clear(),
      transaction.objectStore('playlists').clear(),
      transaction.objectStore('history').clear(),
      transaction.objectStore('queue').clear(),
      transaction.done,
    ]);
  },
};
