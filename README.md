[![Maven Package](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/maven-publish.yml)
[![Java CI](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/codeql-analysis.yml)

# vavi-sound-sandbox

🌏 play the world ♪ 

## Status

| **SPI** |  **Codec** | **IN Status** | **OUT Status** | **SPI Status** | **project** | **Description** | **Comment** |
|:--------|:-----------|:--------------|:---------------|:---------------|:------------|:----------------|:------------|
| midi    | unknown    | 🚫 | 🚫 | - | this | MFi by [unknown]() | |
| midi    | ittake     | 🚫 | 🚫 | - | this | MFi by [ittake](https://web.archive.org/web/20090515001654/http://tokyo.cool.ne.jp/ittake/java/MIDIToMLDv013/MIDIToMLD.html) | |
| sampled | ilbc       | 🚫 | 🚫 | - | this | [c](http://www.ilbcfreeware.org/) | |
| sampled | ldcelp     | 🚫 | 🚫 | - | this | [c](ftp://svr-ftp.eng.cam.ac.uk/pub/comp.speech/coding/ldcelp-2.0.tar.gz) | |
| sampled | mp3        | 🚫 | -  | -  | this | [mp3](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavi/sound/mp3) | need to deal tags |
| sampled | sse        | 🚫 | -  | 🚫 | this | [sse](http://shibatch.sourceforge.net/download/) | |
| sampled | resampling | ✅ | -  | -  | this | [laoe](http://www.oli4.ch/laoe/home.html) | |
| sampled | resampling | ✅ | -  | -  | this | [rohm](https://en.wikipedia.org/wiki/Rohm) | |
| sampled | polyphase  | ✅ | -  | 🚧 | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | resampler  | ✅ | -  | - | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | perfect    | 🚧 | -  | - | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | monauralize | ✅ | - | ✅ | [tritonus-remaining](https://github.com/umjammer/tritonus-remaining) | `PCM2PCMConversionProvider` | works but not suitable for resampling |
| sampled | alac       | ✅ | -  | ✅ | [Apple Lossless Audio Decoder](https://github.com/umjammer/Java-Apple-Lossless-decoder) | | |
| ~~sampled~~ | ~~QTKit~~  | ~~✅~~ | -  | ? | ~~this~~ | ~~[rococoa](https://github.com/umjammer/rococoa)~~ | deprecated |
| sampled | AVFoundation | 🚧 | - | 🚧 | this | [rococoa](https://github.com/umjammer/rococoa) | use `AVAudioConverter` how to return objc value in callback? |
| sampled | twinvq     | 🚫 | 🚫 | - | this | | TODO use ffmpeg |
| midi    | vsq        | 🚧 | -  | 🚧 | this | | YAMAHA Vocaloid |
| sampled | opus       | ✅ | 🚫 | ✅ | this | [concentus](https://github.com/lostromb/concentus) | |
| midi    | AudioUnit  | ✅ | - | ✅ | this | [rococoa](https://github.com/umjammer/rococoa) | use `AVAudioUnitMIDIInstrument/kAudioUnitSubType_DLSSynth` |
| midi    | AudioUnit  | ✅ | - | 🚫 | this | [rococoa](https://github.com/umjammer/rococoa) | use `AVAudioUnitSampler`, how to adjust sf2 patch? |
| midi    | JSyn       | ✅ | -  | ✅ | this | [JSyn](https://github.com/philburk/jsyn) | looking for good drums |
| midi    | OPL3       | ✅ | - | ✅ | this | [adplug](https://github.com/adplug/adplug) | [opl3-player](http://opl3.cozendey.com/) |
| midi    | ?          | -  | -  | -  | this |  | opl, ma |
| midi    | CoreMIDI   | ✅ | ?  | ✅ | [osxmidi4j](https://github.com/locurasoft/osxmidi4j) | rococoa | iac ✓, network ✓, bluetooth ? |
| midi    | CoreMIDI   | ✅ | ?  | ✅ | [CoreMidi4J](https://github.com/DerekCook/CoreMidi4J) | jni | iac ✓, network ✓, bluetooth ? |
| sampled | speex      | ✅ | -  | ✅ | [jspeex](http://jspeex.sourceforge.net/) | | sample rate is limited to convert |
| sampled | flac       | ✅ | -  | ✅ | [jFLAC](http://jflac.sourceforge.net/) | | |
| sampled | aac        | -  | -  | ✅ | [JAADec](https://github.com/umjammer/JAADec) | | |
| sampled | vorbis     | -  | -  | ✅ | [tritonus-jorbis](https://github.com/umjammer/tritonus-jorbis) | | |

## Others

 * [iTunes Library (rococoa)](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavix/rococoa/ituneslibrary)

## Install

  * [jitpack](https://jitpack.io/#umjammer/vavi-sound-sandbox)
  * plain JitPack cannot retrieve GitHub Packages, please choose green marked version made by JitCI like `f49ed0f0`

## Tech Know

  * `tritonus-mp3` only supports mp3 w/o tags
  * the reason we got "`javax.sound.midi.MidiUnavailableException: MIDI OUT transmitter not available`" is that `sound.jar` of `JMF` is in the class path.

## TODO

### Library

 * ~~midi is super heavy~~
 * Transcoder
 * ~~channels~~
 * jsyn pink noise
 * ~~separate alac (git subtree, split?)~~

### codec

 * https://github.com/hendriks73/ffsampledsp (twinvq)
 * https://github.com/Icenowy/jcadencii (twinvq?)
 * https://github.com/hendriks73/casampledsp/tree/master (coreaudio is base of avfoundation?)
 * https://github.com/drogatkin/JustFLAC

### midi

#### macos coremidi

 * ~~[osxmidi4j](https://github.com/locurasoft/osxmidi4j)~~ (done)
 * ~~https://github.com/DerekCook/CoreMidi4J~~ (done)

#### macos audiounit

 * open audiounit custom view
   * https://github.com/nativelibs4java/BridJ (is able to deal objective-c blocks)
 * volume

#### others

 * opl3
   * opl3 volume
   * opl3 midi reader
 * https://github.com/fedex81/emuSandbox
 * https://github.com/toyoshim/tss
 * ~~Apple DLS Sound device~~ (done)
 * https://github.com/comsonica/comsonica-studio
 * https://github.com/jtrfp/javamod (has graphical equalizer, opl2,3 emulator)
 * http://www.jfugue.org/download.html
 * https://sourceforge.net/projects/bristol/
 * https://github.com/Xycl/JSynthLib
 * https://github.com/jonasreese/soundsgood
 * https://www.kvraudio.com/plugins/macosx/audio-units/
 * https://github.com/ggrandes-clones
 * pc88 (mml)
   * https://github.com/onitama/mucom88
   * https://github.com/BouKiCHi/mucom88
   * https://github.com/kuma4649/MDPlayer

#### ebml

 * https://github.com/Matroska-Org/jebm
 * https://github.com/matthewn4444/EBMLReader

## References

 * [Gervill](https://github.com/bluenote10/gervill)


