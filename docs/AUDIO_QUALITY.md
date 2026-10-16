# Audio quality and honest labels

GoVibe is a local Android music player. It sends user-selected files to AndroidX Media3/ExoPlayer; the device's decoder, audio route, Bluetooth stack, system processing, and output hardware determine the audible path. GoVibe does not inspect decoded output or verify the device DAC.

A filename extension or MIME type is not proof of bitrate, sample rate, bit depth, channel count, losslessness, or bit-perfect output. The app makes no bit-perfect claim. It does not transcode or resample local files and does not alter source encoding.

The Rust component selects queue indexes for sequential, repeat, and shuffle navigation. It does not decode, modify, resample, or improve audio fidelity.

Any future provider or integration must be separately authorized and must report available formats and account/region constraints truthfully. Metadata access alone does not imply a playable stream.
