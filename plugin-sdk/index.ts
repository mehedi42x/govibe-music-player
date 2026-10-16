export { CORE_VERSION, PLUGIN_SDK_VERSION } from '../src/domain/models';
export { ProviderError } from '../src/domain/models';
export type {
  AudioFormat,
  IntegrationStatus,
  PluginAuthentication,
  PluginManifest,
  PluginPermission,
  PlayerStatus,
  ProviderAlbum,
  ProviderArtist,
  ProviderCapability,
  ProviderErrorCode,
  ProviderLyrics,
  ProviderPlugin,
  ProviderPlaylist,
  ProviderTrack,
  QualityPreset,
  Track,
} from '../src/domain/models';
export { missingPermissions, validateManifest } from '../src/core/plugins';
export type { ManifestValidation } from '../src/core/plugins';
