[![](https://jitpack.io/v/umjammer/vavi-sound-sandbox.svg)](https://jitpack.io/#umjammer/vavi-sound-sandbox) [![Java CI with Maven](https://github.com/umjammer/vavi-sound-sandbox/workflows/Java%20CI%20with%20Maven/badge.svg)](https://github.com/umjammer/vavi-sound-sandbox/actions)

# vavi-sound-sandbox

Sandbox for sound libraries.

## Status

| **SPI** |  **Codec** | **IN Status** | **OUT Status** | **SPI Status** | **project** | **Description** | **Comment** |
|:--------|:-----------|:--------------|:---------------|:---------------|:------------|:----------------|:------------|
| midi    | unknown    | 🚫 | 🚫 | - | this | MFi by [unknown]() | |
| midi    | ittake     | 🚫 | 🚫 | - | this | MFi by [ittake](https://web.archive.org/web/20090515001654/http://tokyo.cool.ne.jp/ittake/java/MIDIToMLDv013/MIDIToMLD.html) | |
| sampled | ilbc       | 🚫 | 🚫 | - | this | [c](http://www.ilbcfreeware.org/) | |
| sampled | ldcelp     | 🚫 | 🚫 | - | this | [c](ftp://svr-ftp.eng.cam.ac.uk/pub/comp.speech/coding/ldcelp-2.0.tar.gz) | |
| sampled | mp3        | 🚫 | -  | -  | this | [mp3](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavi/sound/mp3) | need to deal tags |
| sampled | sse        | 🚫 | -  | 🚫 | this | [sse](http://shibatch.sourceforge.net/download/) | |
| sampled | resampling | ?  | -  | -  | this | [laoe](http://www.oli4.ch/laoe/home.html) | |
| sampled | resampling | ?  | -  | -  | this | [rohm]() | |
| sampled | polyphase  | ✅ | -  | 🚧 | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | resampler  | ✅ | -  | - | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | perfect    | 🚧 | -  | - | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | monauralize | ✅ | -  | ✅ | [tritonus-dsp](https://github.com/umjammer/tritonus-dsp) | | |
| sampled | alac       | ✅ | -  | ✅ | this | [Apple Lossless Audio Decoder](https://github.com/umjammer/Java-Apple-Lossless-decoder) | |
| sampled | QTKit      | ✅ | -  | ? | this | [rococoa](https://github.com/umjammer/rococoa) | you must lock jna version |
| sampled | AVFoundation | - | -  | - | this | [rococoa](https://github.com/umjammer/rococoa) | you must lock jna version |
| sampled | twinvq     | 🚫 | 🚫 | - | this | | TODO use ffmpeg |
| -       | vsq        | ✅ | -  | - | this | | YAMAHA Vocaloid |
| sampled | opus       | ✅ | 🚫 | ✅ | this | [concentus](https://github.com/lostromb/concentus) | |
| midi    | midi       | 🚫 | -  | 🚫 | [osxmidi4j](https://github.com/locurasoft/osxmidi4j) | | for hardware midi only? |
| sampled | speex      | ✅ | -  | ✅ | [jspeex](http://jspeex.sourceforge.net/) | | sample rate is limited to convert |
| sampled | flac       | ✅ | -  | ✅ | [jFLAC](http://jflac.sourceforge.net/) | | |
| sampled | aac        | -  | -  | ✅ | [JAADec](https://github.com/umjammer/JAADec) | | |
| sampled | vorbis     | -  | -  | ✅ | [tritonus-jorbis](https://github.com/umjammer/tritonus-jorbis) | | |

## Others

 * [iTunes Library (rococoa)](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavix/rococoa/ituneslibrary)

## Tech Know

  * `tritonus-mp3` only supports mp3 w/o tags
  * the reason we got "`javax.sound.midi.MidiUnavailableException: MIDI OUT transmitter not available`" is that `sound.jar` of `JMF` is in the class path.

## TODO

 * ~~midi is super heavy~~
 * Transcoder
 * ~~channels~~
 * https://github.com/hendriks73/ffsampledsp
 * https://github.com/Icenowy/jcadencii
 * https://github.com/drogatkin/JustFLAC
