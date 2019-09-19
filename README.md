[![](https://jitpack.io/v/umjammer/vavi-sound-sandbox.svg)](https://jitpack.io/#umjammer/vavi-sound-sandbox)

# vavi-sound-sandbox

| **SPI** |  **Codec** |  **Description** | **IN Status** | **OUT Status** | **SPI Status** | ** Comment ** |
|:--------|:-----------|:-----------------|:--------------|:---------------|:---------------|:--------------|
| midi | unknown | MFi by [unknown]() | 🚫 | 🚫 | - | |
| midi | ittake | MFi by [ittake]() | 🚫 | 🚫 | - | |
| sampled | ilbc | [c](http://www.ilbcfreeware.org/) | 🚫 | 🚫 | - | |
| sampled | ldcelp | [c]() | 🚫 | 🚫 | - | |
| sampled | mp3 | [mp3]() | 🚫 | - | - | need to deal tags |
| sampled | sse  | [equalizing]() | 🚫 | - | 🚫 | |
| sampled | laoe | [resampling]() | ? | - | - | |
| sampled | rohm | resampling | ? | - | - | |
| sampled | polyphase | [sox](http://sox.sourceforge.net/) resampling | ✅ | - | - | |
| sampled | resampler | [sox](http://sox.sourceforge.net/) resampling | ? | - | - | |
| sampled | perfect | [sox](http://sox.sourceforge.net/) resampling | 🚫 | - | - | |
| sampled | ssrc2 | resampling | 🚫 | - | - | |
| sampled | trironus | resampling| ✅ | - | ✅ | |
| sampled | alac | [Apple Lossless Audio Decoder](https://github.com/soiaf/Java-Apple-Lossless-decoder) | ✅ | - | ✅ | |
| sampled | QTKit | [rocococa]() | ✅ | - | - | you must lock jna version |
| sampled | AVFoundation | [rocococa]() | - | - | - | you must lock jna version |
| sampled | twinvq |  | x | x | - | |
| - | vsq | YAMAHA Vocaloid | ✅ | - | - | |
| sampled | opus | [concentus](https://github.com/lostromb/concentus) | ✅ | 🚫 | ✅ | |
| midi | midi | [osxmidi4j](https://github.com/locurasoft/osxmidi4j) | 🚫 | - | 🚫 | |
| sampled | speex | [jspeex](http://jspeex.sourceforge.net/) | ✅ | - | ✅ | sample rate is limited to convert |
| sampled | flac | [jFLAC](http://jflac.sourceforge.net/) | ✅ | - | ✅ | see also [JustFLAC](https://github.com/drogatkin/JustFLAC) |
| sampled | aac | [JAADec](https://github.com/DV8FromTheWorld/JAADec) | - | - | 🚫 | mark/reset error? (not for all files) |
| sampled | vorbis | [vorbisspi](http://www.javazoom.net/vorbisspi/vorbisspi.html) | - | - | ✅ | AudioSystem version conflict? |
