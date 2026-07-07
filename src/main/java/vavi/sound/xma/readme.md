# package vavi.sound.xma

WMA Pro / XMA audio decoder.

A Java port of [Echo](https://github.com/IsaacMarovitz/Echo) — an experimental
standalone decoder for XMA / WMA Pro audio, itself derived from ffmpeg's WMA Pro
decoder (`libavcodec/wmaprodec.c`).

## Layout

| Java class                                                                  | ported from (`Echo/`)                              |
|-----------------------------------------------------------------------------|----------------------------------------------------|
| `BitReader`                                                                 | `Bitstream/BitReader.cs`                           |
| `XmaVersion`, `XmaStreamInfo`, `XmaFrame`, `XmaContainer`, `XmaFrameReader` | `Container/*.cs`                                   |
| `Fft`, `Imdct`, `SineWindow`, `FloatDsp`                                    | `WmaPro/{Fft,Imdct,SineWindow,FloatDsp}.cs`        |
| `Vlc`, `WmaProVlc`                                                          | `WmaPro/Vlc.cs`                                    |
| `WmaCommon`, `WmaProTables`, `WmaProDecoder`                                | `WmaPro/{WmaCommon,WmaProTables,WmaProDecoder}.cs` |

The SPI lives in [`vavi.sound.sampled.xma`](../../sampled/wma).

## Scope / caveats

- Decodes the **XMA container** (RIFF/WAVE with an XMA1 `0x0165` or XMA2 `0x0166`
  `fmt ` chunk) or a raw 2 KiB packet stream. It does **not** parse the ASF
  container used by ordinary `.wma` files — those start with an ASF GUID, not
  `RIFF`, and are rejected by the file reader.
- Port is line-by-line faithful to Echo; C# `Span<T>` slices become
  `array + offset` pairs, and the `System.Numerics` SIMD path in `FloatDsp` is
  reduced to the scalar reference loop.
- Like the upstream project, this targets XMA content and has only been
  exercised against synthetic fixtures here plus the upstream's own test corpus.

## Usage

```java
byte[] file = Files.readAllBytes(path);              // an .xma (RIFF) file
XmaContainer container = XmaContainer.open(file);
XmaStreamInfo info = container.streamInfo();
XmaFrameReader reader = new XmaFrameReader(container);
WmaProDecoder[] decoders = ...;                       // one per substream
// see vavi.sound.sampled.xma.Wma2PcmAudioInputStream for the full loop
```
