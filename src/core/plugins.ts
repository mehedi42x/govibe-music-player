import {
  PLUGIN_SDK_VERSION,
  type PluginAuthentication,
  type PluginManifest,
  type PluginPermission,
  type ProviderCapability,
} from '../domain/models';

const ID = /^[a-z0-9]+(?:[.-][a-z0-9]+)*$/;
const SEMVER = /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/;
const CAPABILITIES = new Set<ProviderCapability>([
  'search',
  'metadata',
  'playback',
  'playlists',
  'lyrics',
  'scrobbling',
  'recommendations',
]);
const PERMISSIONS = new Set<PluginPermission>([
  'library.read',
  'playback.resolve',
  'network.request',
  'account.read',
]);
const AUTHENTICATION = new Set<PluginAuthentication>(['none', 'oauth2', 'api-key', 'account']);

export type ManifestValidation = { ok: true; manifest: PluginManifest } | { ok: false; errors: string[] };

export function validateManifest(input: unknown): ManifestValidation {
  const errors: string[] = [];
  if (!input || typeof input !== 'object' || Array.isArray(input))
    return { ok: false, errors: ['Manifest must be a JSON object.'] };
  const value = input as Record<string, unknown>;
  for (const key of ['id', 'name', 'version', 'description', 'sdkVersion', 'authentication', 'entry']) {
    if (typeof value[key] !== 'string' || value[key].trim() === '')
      errors.push(`${key} must be a non-empty string.`);
  }
  if (typeof value.id === 'string' && !ID.test(value.id))
    errors.push('id must use lowercase letters, digits, dots, or hyphens.');
  if (typeof value.name === 'string' && value.name.length > 80)
    errors.push('name must be 80 characters or fewer.');
  if (typeof value.version === 'string' && !SEMVER.test(value.version))
    errors.push('version must be semantic versioning (major.minor.patch).');
  if (typeof value.sdkVersion === 'string' && value.sdkVersion !== PLUGIN_SDK_VERSION)
    errors.push(`sdkVersion must equal ${PLUGIN_SDK_VERSION} for this SDK.`);
  if (
    typeof value.authentication === 'string' &&
    !AUTHENTICATION.has(value.authentication as PluginAuthentication)
  )
    errors.push('authentication must be none, oauth2, api-key, or account.');
  if (
    typeof value.entry === 'string' &&
    (value.entry.startsWith('/') || value.entry.includes('..') || /^[a-z]+:/i.test(value.entry))
  )
    errors.push('entry must be a relative package path; external code is not accepted.');
  const validateList = <T extends string>(key: 'capabilities' | 'permissions', allowed: Set<T>) => {
    const list = value[key];
    if (!Array.isArray(list)) {
      errors.push(`${key} must be an array.`);
      return;
    }
    if (list.some((item) => typeof item !== 'string' || !allowed.has(item as T)))
      errors.push(`${key} contains an unsupported value.`);
    if (new Set(list).size !== list.length) errors.push(`${key} must not contain duplicates.`);
  };
  validateList('capabilities', CAPABILITIES);
  validateList('permissions', PERMISSIONS);
  if (
    typeof value.authentication === 'string' &&
    value.authentication !== 'none' &&
    Array.isArray(value.permissions)
  ) {
    if (!value.permissions.includes('account.read'))
      errors.push('Authenticated providers must declare account.read.');
    if (!value.permissions.includes('network.request'))
      errors.push('Authenticated providers must declare network.request.');
  }
  if (errors.length) return { ok: false, errors };
  return { ok: true, manifest: value as unknown as PluginManifest };
}

export function missingPermissions(
  requested: PluginPermission[],
  granted: PluginPermission[],
): PluginPermission[] {
  return [...new Set(requested)].filter((permission) => !granted.includes(permission));
}
