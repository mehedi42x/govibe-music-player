export const CORE_VERSION = '0.1.0';
export const PLUGIN_SDK_VERSION = '1.0.0';

export type PlayerStatus =
  'idle' | 'loading' | 'ready' | 'playing' | 'paused' | 'buffering' | 'ended' | 'failed';
export type RepeatMode = 'off' | 'all' | 'one';
export type QualityPreset = 'auto' | 'low' | 'medium' | 'high' | 'lossless' | 'maximum';
export type AppSection = 'home' | 'search' | 'library' | 'playlists' | 'queue' | 'extensions' | 'settings';

export interface Track {
  id: string;
  title: string;
  artist: string;
  album: string;
  duration: number;
  mimeType: string;
  size: number;
  addedAt: number;
  favorite: boolean;
  artwork?: string;
  audio: Blob;
}

export interface ProviderTrack {
  id: string;
  title: string;
  artistId?: string;
  artistName: string;
  albumId?: string;
  albumTitle?: string;
  durationSeconds?: number;
  artworkUrl?: string;
  formats?: AudioFormat[];
}

export interface ProviderArtist {
  id: string;
  name: string;
  artworkUrl?: string;
}
export interface ProviderAlbum {
  id: string;
  title: string;
  artistId?: string;
  artistName: string;
  trackIds: string[];
  artworkUrl?: string;
}
export interface ProviderPlaylist {
  id: string;
  name: string;
  ownerName?: string;
  trackIds: string[];
  artworkUrl?: string;
}
export interface ProviderLyrics {
  text: string;
  syncedLines?: Array<{ timeMs: number; text: string }>;
}

export interface Playlist {
  id: string;
  name: string;
  trackIds: string[];
  createdAt: number;
  updatedAt: number;
}

export interface PlaybackRecord {
  id?: number;
  trackId: string;
  playedAt: number;
}

export interface AudioFormat {
  codec: string;
  container: string;
  bitrateKbps?: number;
  sampleRateHz?: number;
  bitDepth?: number;
  channels?: number;
  lossless: boolean;
  verified: boolean;
  source: 'browser' | 'provider';
}

export interface AppSettings {
  quality: QualityPreset;
  volume: number;
  repeat: RepeatMode;
  shuffle: boolean;
  theme: 'dark' | 'light' | 'system';
  locale: 'en' | 'bn';
  telemetry: false;
}

export const DEFAULT_SETTINGS: AppSettings = {
  quality: 'auto',
  volume: 0.78,
  repeat: 'off',
  shuffle: false,
  theme: 'dark',
  locale: 'en',
  telemetry: false,
};

export type ProviderCapability =
  'search' | 'metadata' | 'playback' | 'playlists' | 'lyrics' | 'scrobbling' | 'recommendations';
export type PluginPermission = 'library.read' | 'playback.resolve' | 'network.request' | 'account.read';
export type PluginAuthentication = 'none' | 'oauth2' | 'api-key' | 'account';
export type IntegrationStatus = 'implemented' | 'partial' | 'planned' | 'unavailable';
export type ProviderErrorCode =
  | 'ABORTED'
  | 'AUTH_REQUIRED'
  | 'AUTH_EXPIRED'
  | 'RATE_LIMITED'
  | 'NETWORK'
  | 'UNSUPPORTED'
  | 'NOT_FOUND'
  | 'PERMISSION_DENIED'
  | 'INVALID_RESPONSE'
  | 'INTERNAL';

export class ProviderError extends Error {
  readonly name = 'ProviderError';
  constructor(
    readonly code: ProviderErrorCode,
    message: string,
    readonly retryable = false,
    readonly retryAfterMs?: number,
  ) {
    super(message);
  }
}

export interface PluginManifest {
  id: string;
  name: string;
  version: string;
  description: string;
  sdkVersion: string;
  authentication: PluginAuthentication;
  capabilities: ProviderCapability[];
  permissions: PluginPermission[];
  entry: string;
}

export interface ProviderPlugin {
  manifest: PluginManifest;
  search(query: string, signal?: AbortSignal): Promise<ProviderTrack[]>;
  resolve(trackId: string, signal?: AbortSignal): Promise<Blob | null>;
  getTrack?(trackId: string, signal?: AbortSignal): Promise<ProviderTrack | null>;
  getAlbum?(albumId: string, signal?: AbortSignal): Promise<ProviderAlbum | null>;
  getArtist?(artistId: string, signal?: AbortSignal): Promise<ProviderArtist | null>;
  getPlaylist?(playlistId: string, signal?: AbortSignal): Promise<ProviderPlaylist | null>;
  getLyrics?(trackId: string, signal?: AbortSignal): Promise<ProviderLyrics | null>;
  getRecommendations?(trackId: string, signal?: AbortSignal): Promise<ProviderTrack[]>;
  scrobble?(trackId: string, playedAt: number, signal?: AbortSignal): Promise<void>;
  dispose(): Promise<void>;
}
