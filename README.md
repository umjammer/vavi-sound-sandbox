[![](https://jitpack.io/v/umjammer/vavi-sound-sandbox.svg)](https://jitpack.io/#umjammer/vavi-sound-sandbox)

# vavi-sound-sandbox

| **SPI** |  **Codec** |  **Description** | **IN Status** | **OUT Status** | **SPI Status** | ** Comment ** |
|:--------|:-----------|:-----------------|:--------------|:---------------|:---------------|:--------------|
| midi | unknown | MFi by [unknown]() | x | x | - | |
| midi | ittake | MFi by [ittake]() | x | x | - | |
| sampled | ilbc | [c](http://www.ilbcfreeware.org/) | x | x | - | |
| sampled | ldcelp | [c]() | x | x | - | |
| sampled | mp3 | [mp3]() | x | - | - | need to deal tags |
| sampled | sse  | [equalizing]() | x | - | x | |
| sampled | laoe | [resampling]() | ? | - | - | |
| sampled | rohm | resampling | ? | - | - | |
| sampled | polyphase | [sox](http://sox.sourceforge.net/) resampling | o | - | - | |
| sampled | resampler | [sox](http://sox.sourceforge.net/) resampling | ? | - | - | |
| sampled | perfect | [sox](http://sox.sourceforge.net/) resampling | x | - | - | |
| sampled | ssrc2 | resampling | x | - | - | |
| sampled | trironus | resampling| o | - | o | |
| sampled | alac | [Apple Lossless Audio Decoder](https://github.com/soiaf/Java-Apple-Lossless-decoder) | o | - | o | |
| sampled | QTKit | [rocococa]() | o | - | - | you must lock jna version |
| sampled | AVFoundation | [rocococa]() | - | - | - | you must lock jna version |
| sampled | twinvq |  | x | x | - | |
| - | vsq | YAMAHA Vocaloid | o | - | - | |
| sampled | opus | [concentus](https://github.com/lostromb/concentus) | o | x | o | |
| midi | midi | [osxmidi4j](https://github.com/locurasoft/osxmidi4j) | x | - | x | |
| sampled | speex | [jspeex](http://jspeex.sourceforge.net/) | o | - | o | sample rate is limited to convert |
| sampled | flac | [jFLAC](http://jflac.sourceforge.net/) | o | - | o | see also JustFLAC |
| sampled | aac | [JAADec](https://github.com/DV8FromTheWorld/JAADec) | - | - | x | mark/reset error? (not for all files) |
| sampled | vorbis | [vorbisspi](http://www.javazoom.net/vorbisspi/vorbisspi.html) | - | - | o | AudioSystem version conflict? |

### Legend ###

|Mark|Meaning|
|:--|:---|
| o | ok |
| ? | not tested |
| c | under construction |
| x | ng |
| - | n/a |
