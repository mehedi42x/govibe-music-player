import { describe, expect, it } from 'vitest';
import { createQueue, enqueue, moveInQueue, nextIndex, removeFromQueue } from './queue';
import { qualityLabel, resolveAudioQuality } from './quality';
import { validateManifest } from './plugins';
import { ProviderError, type AudioFormat } from '../domain/models';

const formats: AudioFormat[] = [
  { codec: 'mp3', container: 'mpeg', bitrateKbps: 320, lossless: false, verified: true, source: 'browser' },
  {
    codec: 'flac',
    container: 'flac',
    sampleRateHz: 96000,
    bitDepth: 24,
    lossless: true,
    verified: false,
    source: 'provider',
  },
];

describe('queue transitions', () => {
  it('enqueues without duplicating existing tracks', () =>
    expect(enqueue(createQueue(['a']), ['a', 'b']).trackIds).toEqual(['a', 'b']));
  it('moves the current item with its index', () => {
    const result = moveInQueue({ ...createQueue(['a', 'b', 'c'], 1), currentIndex: 1 }, 1, 2);
    expect(result.trackIds).toEqual(['a', 'c', 'b']);
    expect(result.currentIndex).toBe(2);
  });
  it('adjusts current selection when an earlier item is removed', () => {
    const result = removeFromQueue({ ...createQueue(['a', 'b', 'c'], 2), currentIndex: 2 }, 0);
    expect(result.trackIds).toEqual(['b', 'c']);
    expect(result.currentIndex).toBe(1);
  });
  it('respects end-of-queue repeat behavior', () => {
    const queue = { ...createQueue(['a', 'b'], 1), currentIndex: 1 };
    expect(nextIndex(queue)).toBe(-1);
    expect(nextIndex({ ...queue, repeat: 'all' })).toBe(0);
    expect(nextIndex({ ...queue, repeat: 'one' })).toBe(1);
  });
  it('chooses a different track for deterministic shuffle', () => {
    expect(nextIndex({ ...createQueue(['a', 'b', 'c'], 1), currentIndex: 1, shuffle: true }, () => 0)).toBe(
      0,
    );
  });
});

describe('audio quality claims', () => {
  it('selects lossless only when the source actually provides it', () => {
    expect(resolveAudioQuality(formats, 'lossless')?.codec).toBe('flac');
    expect(resolveAudioQuality([formats[0]!], 'lossless')).toBeUndefined();
  });
  it('does not call unverified lossless metadata verified', () => {
    expect(qualityLabel(formats[1])).toBe('Lossless · source reported');
    expect(qualityLabel({ ...formats[1]!, verified: true })).toBe('Lossless · verified');
  });
});

describe('plugin manifest validation', () => {
  const valid = {
    id: 'community.example',
    name: 'Example',
    version: '1.0.0',
    description: 'A test provider',
    sdkVersion: '1.0.0',
    authentication: 'oauth2',
    capabilities: ['search'],
    permissions: ['network.request', 'account.read'],
    entry: 'dist/index.js',
  };
  it('accepts a compatible and constrained manifest', () => expect(validateManifest(valid).ok).toBe(true));
  it('rejects an incompatible SDK and external entrypoint', () => {
    const result = validateManifest({
      ...valid,
      sdkVersion: '2.0.0',
      entry: 'https://bad.example/plugin.js',
    });
    expect(result.ok).toBe(false);
  });
  it('rejects undeclared privileges', () =>
    expect(validateManifest({ ...valid, permissions: ['filesystem.root'] }).ok).toBe(false));
  it('requires an explicit supported authentication mode', () =>
    expect(validateManifest({ ...valid, authentication: 'magic' }).ok).toBe(false));
  it('requires account and network permissions for authenticated providers', () => {
    expect(validateManifest({ ...valid, permissions: ['network.request'] }).ok).toBe(false);
    expect(validateManifest({ ...valid, authentication: 'none', permissions: [] }).ok).toBe(true);
  });
  it('exposes structured provider error codes and retry hints', () => {
    const error = new ProviderError('RATE_LIMITED', 'Try later', true, 5000);
    expect(error.code).toBe('RATE_LIMITED');
    expect(error.retryable).toBe(true);
    expect(error.retryAfterMs).toBe(5000);
  });
});
