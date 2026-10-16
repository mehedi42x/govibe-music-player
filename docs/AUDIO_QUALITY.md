# Audio quality and honest labels

GoVibe distinguishes what a provider reports from properties independently measured by the application. The web core quality resolver can select among formats a provider explicitly offers. Its presets are `auto`, `low`, `medium`, `high`, `lossless`, and `maximum`; a `lossless` request returns no format if no lossless candidate exists. A source-reported lossless format is labeled as reported, not verified.

## Native Android local files

Android passes selected local media to AndroidX Media3/ExoPlayer. The device's decoder, audio route, Bluetooth stack, system processing, and output hardware determine the audible path. GoVibe does not inspect the decoded output or verify the device DAC.

A filename extension or MIME type is not proof of bitrate, sample rate, bit depth, channel count, losslessness, or bit-perfect output. The app makes no bit-perfect claim. Quality presets do not transcode or resample local files and do not alter the source encoding.

## Web PWA local files

The web client passes imported IndexedDB Blobs to the browser's native audio decoder. Browser-supported codecs vary by OS and browser. Unsupported formats can fail to play; GoVibe does not show a false quality badge. Upsampling is not implemented and is not described as improving source fidelity.

A future authorized streaming provider must report its available formats and account/region constraints. The UI must not call lossy output lossless or imply that metadata access provides a playable stream.
