# vavi.sound.wma

Windows Media Audio **v1 (0x0160) / v2 (0x0161)** decoder for **ASF** (`.wma`)
containers, producing planar float PCM.

Ported for the mission in `AGENTS.md`: port the
[`@audio/wma-decode`](https://www.npmjs.com/package/@audio/wma-decode) npm
package to Java.

## What that package is

`@audio/wma-decode` is a **pure-JS ASF demuxer** plus the **RockBox** fixed-point
`wmadec` decoder compiled to WebAssembly. Only the demuxer is JavaScript; the
actual audio decoding is a WASM blob. RockBox's `wmadeci.c` is itself a
fixed-point fork of FFmpeg's float `libavcodec/wmadec.c`.

## How this port maps

| Java | Source |
|------|--------|
| `AsfDemuxer`, `AsfInfo` | direct port of the JS `demuxASF` / `parsePacket` / `parseStreamProps` / `parseFileProps` |
| `WmaDecoder` | FFmpeg `libavcodec/wmadec.c` + `wma.c` + `wma_common.c` (float) |
| `WmaData` | FFmpeg `wmadata.h`, `wma_freqs.c`, `aactab.c`, and the `pow_tab` in `wmadec.c` |
| `BitReader` | FFmpeg `get_bits` semantics (MSB-first) |
| `Fft`, `Imdct`, `SineWindow`, `FloatDsp` | FFmpeg `ff_mdct` / `ff_sine_window` / float-DSP equivalents |
| `Vlc` | FFmpeg `vlc_init` (explicit codes) and `vlc_init_from_lengths` (canonical) |

The decoder covers: super-frame + bit-reservoir handling, fixed/variable block
lengths, LSP and VLC exponent coding, run/level spectral coefficients,
perceptual noise coding + high-band gains, mid/side stereo, the full IMDCT and
overlap-add windowing.

## Verification

Decoding `src/test/resources/test.wma` (real WMA v2, stereo, 44.1 kHz) matches
FFmpeg 8.1's reference `-f f32le` decode to within ~3e-7 (float epsilon) across
all ~1.4M samples. See `WmaDecoderTest`.

## Not handled here

WMA **Pro** (0x0162) and **Lossless** (0x0163). WMA Pro / XMA is decoded by the
sibling package `vavi.sound.xma`.

## SPI

`vavi.sound.sampled.wma` exposes this via `javax.sound.sampled`: an
`AudioFileReader` (sniffs the ASF header) and a `FormatConversionProvider`
(WMA → PCM signed 16-bit little-endian). Registered in `META-INF/services`.

Like the npm package, the priming delay (`2 * frame_len` samples) is **not**
trimmed, so the output is that many samples longer than FFmpeg's CLI output.
