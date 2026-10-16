# Audio quality and honest labels

NextVibe is a local Android music player. It hands local files or selected-source URLs to AndroidX Media3/ExoPlayer; the device decoder, audio route, Bluetooth stack, system processing and output hardware determine the audible path. NextVibe does not inspect decoded output or verify the device DAC.

A filename extension or MIME type does not prove bitrate, sample rate, bit depth, channel count, losslessness, or bit-perfect output. The app makes no bit-perfect claim. It does not transcode source files; however, the user may choose app-level volume, device-session Equalizer presets or an optional LoudnessEnhancer gain capped at +3 dB. Those effects change the playback signal, depend on Android/device support and can cause clipping; boost is off by default.

The Rust component selects queue indexes for sequential, repeat and shuffle navigation. It does not decode, modify, resample or improve audio fidelity.

Any future provider or integration must be separately authorized and must report available formats and account/region constraints truthfully. Metadata access alone does not imply a playable stream. Android device testing is still needed to verify playback, codecs and platform audio effects.
