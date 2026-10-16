import type { AudioFormat, QualityPreset } from '../domain/models';

const bitrate = (format: AudioFormat) => format.bitrateKbps ?? 0;
const sampleRate = (format: AudioFormat) => format.sampleRateHz ?? 0;

export function resolveAudioQuality(formats: AudioFormat[], preset: QualityPreset): AudioFormat | undefined {
  if (!formats.length) return undefined;
  const ordered = [...formats].sort(
    (a, b) =>
      Number(b.lossless) - Number(a.lossless) || sampleRate(b) - sampleRate(a) || bitrate(b) - bitrate(a),
  );
  if (preset === 'lossless') return ordered.find((format) => format.lossless);
  if (preset === 'low') return [...formats].sort((a, b) => bitrate(a) - bitrate(b))[0];
  if (preset === 'medium')
    return (
      ordered.filter((format) => !format.lossless).sort((a, b) => bitrate(a) - bitrate(b))[0] ?? ordered[0]
    );
  if (preset === 'high') return ordered.filter((format) => !format.lossless).at(-1) ?? ordered[0];
  return ordered[0];
}

export function qualityLabel(format: AudioFormat | undefined): string {
  if (!format) return 'Quality unavailable';
  if (format.lossless && format.verified) return 'Lossless · verified';
  if (format.lossless) return 'Lossless · source reported';
  if (format.verified && format.bitrateKbps) return `${format.bitrateKbps} kbps · verified`;
  return 'Format · source reported';
}
