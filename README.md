[![Release](https://jitpack.io/v/umjammer/vavi-sound-sandbox.svg)](https://jitpack.io/#umjammer/vavi-sound-sandbox)
[![Java CI](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-sound-sandbox/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-21-b07219)

# vavi-sound-sandbox

<img alt="logo" src="src/test/resources/duke_maracas.png" width="160" />

🎸 play in the sandbox ♪ 

### Status

| **SPI**     | **Codec**    | **IN Status** | **OUT Status** | **SPI Status** | **project**                                                                                | **Description**                                                                                                              | **Comment**                                                      |
|:------------|:-------------|:-------------:|:--------------:|:--------------:|:-------------------------------------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------|:-----------------------------------------------------------------|
| midi        | unknown      |      🚫       |       🚫       |       -        | this                                                                                       | MFi by [unknown]()                                                                                                           |                                                                  |
| midi        | ittake       |      🚫       |       🚫       |       -        | this                                                                                       | MFi by [ittake](https://web.archive.org/web/20090515001654/http://tokyo.cool.ne.jp/ittake/java/MIDIToMLDv013/MIDIToMLD.html) |                                                                  |
| sampled     | ilbc         |       ✅       |       ?        |       ✅        | this                                                                                       | [c](http://www.ilbcfreeware.org/)                                                                                            |                                                                  |
| sampled     | ldcelp       |       ✅       |       ?        |       ✅        | this                                                                                       | [c](https://archive.org/details/2014.12.svr-ftp.eng.cam.ac.uk#/pub/comp.speech/coding/ldcelp-2.0.tar.gz)                     |                                                                  |
| sampled     | mp3          |      🚫       |       -        |       -        | this                                                                                       | [mp3](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavi/sound/mp3)                               | need to deal tags                                                |
| sampled     | mp3          |       ✅       |       -        |       ✅        | [mp3spi](https://github.com/umjammer/mp3spi)                                               | [jlayer](https://github.com/umjammer/jlayer)                                                                                 |                                                                  |
| sampled     | sse          |      🚫       |       -        |       🚫       | this                                                                                       | [sse](http://shibatch.sourceforge.net/download/)                                                                             |                                                                  |
| sampled     | resampling   |       ✅       |       -        |       -        | this                                                                                       | [laoe](http://www.oli4.ch/laoe/home.html)                                                                                    |                                                                  |
| sampled     | resampling   |       ✅       |       -        |       -        | this                                                                                       | [rohm](https://en.wikipedia.org/wiki/Rohm)                                                                                   |                                                                  |
| sampled     | polyphase    |       ✅       |       -        |       🚧       | this                                                                                       | [sox](http://sox.sourceforge.net/) resampling                                                                                |                                                                  |
| sampled     | resampler    |       ✅       |       -        |       -        | this                                                                                       | [sox](http://sox.sourceforge.net/) resampling                                                                                |                                                                  |
| sampled     | perfect      |      🚧       |       -        |       -        | this                                                                                       | [sox](http://sox.sourceforge.net/) resampling                                                                                |                                                                  |
| sampled     | monauralize  |       ✅       |       -        |       ✅        | [tritonus-remaining](https://github.com/umjammer/tritonus/tree/develop/tritonus-remaining) | `PCM2PCMConversionProvider`                                                                                                  | works but not suitable for resampling                            |
| sampled     | alac         |       ✅       |       -        |       ✅        | [vavi-sound-alac](https://github.com/umjammer/vavi-sound-alac)                             |                                                                                                                              | 🎓 graduated to vavi-sound-alac                                  |
| ~~sampled~~ | ~~QTKit~~    |     ~~✅~~     |       -        |       ?        | ~~this~~                                                                                   | ~~[rococoa](https://github.com/umjammer/rococoa)~~                                                                           | deprecated                                                       |
| sampled     | AVFoundation |      🚧       |       -        |       🚧       | this                                                                                       | [rococoa](https://github.com/umjammer/rococoa)                                                                               | ~~use `AVAudioConverter` how to return objc value in callback?~~ |
| sampled     | twinvq       |       ✅       |       -        |       ✅        | this                                                                                       |                                                                                                                              | TODO use ffmpeg                                                  |
| midi        | vsq          |      🚧       |       -        |       🚧       | this                                                                                       |                                                                                                                              | YAMAHA Vocaloid                                                  |
| sampled     | opus         |       ✅       |       🚫       |       ✅        | this                                                                                       | [concentus](https://github.com/lostromb/concentus)                                                                           |                                                                  |
| midi        | AudioUnit    |       ✅       |       -        |       ✅        | this                                                                                       | [rococoa](https://github.com/umjammer/rococoa)                                                                               | use `AVAudioUnitMIDIInstrument/kAudioUnitSubType_DLSSynth`       |
| midi        | AudioUnit    |       ✅       |       -        |       🚫       | this                                                                                       | [rococoa](https://github.com/umjammer/rococoa)                                                                               | use `AVAudioUnitSampler`, how to adjust sf2 patch?               |
| midi        | JSyn         |       ✅       |       -        |       ✅        | this                                                                                       | [JSyn](https://github.com/philburk/jsyn)                                                                                     | looking for good drums                                           |
| midi        | OPL3         |       ✅       |       -        |       ✅        | this                                                                                       | [adplug](https://github.com/adplug/adplug)                                                                                   | [opl3-player](http://opl3.cozendey.com/), YmF262(cozendey)       |
| midi        | ?            |       -       |       -        |       -        | this                                                                                       |                                                                                                                              | opl, ma                                                          |
| midi        | CoreMIDI     |       ✅       |       ?        |       ✅        | [osxmidi4j](https://github.com/umjammer/osxmidi4j)                                         | rococoa                                                                                                                      | iac ✓, network ✓, bluetooth ?                                    |
| midi        | CoreMIDI     |       ✅       |       ?        |       ✅        | [CoreMidi4J](https://github.com/DerekCook/CoreMidi4J)                                      | jni                                                                                                                          | iac ✓, network ✓, bluetooth ?                                    |
| sampled     | speex        |       ✅       |       -        |       ✅        | [jspeex](http://jspeex.sourceforge.net/)                                                   |                                                                                                                              | sample rate is limited to convert                                |
| sampled     | flac         |       ✅       |       -        |       ✅        | [vavi-sound-flac](https://github.com/umjammer/vavi-sound-flac)                             |                                                                                                                              |                                                                  |
| sampled     | flac         |       ✅       |       -        |       ✅        | [vavi-sound-flac-nayuki](https://github.com/umjammer/vavi-sound-flac-nayuki)               |                                                                                                                              |                                                                  |
| sampled     | aac          |       -       |       -        |       ✅        | [JAADec](https://github.com/umjammer/vavi-sound-aac)                                       |                                                                                                                              |                                                                  |
| sampled     | vorbis       |       -       |       -        |       ✅        | [tritonus-jorbis](https://github.com/umjammer/tritonus/tree/develop/tritonus-jorbis)       |                                                                                                                              |                                                                  |
| sampled     | atrac3       |       ✅       |       -        |       ?        | [vavi-sound-atrack](https://github.com/umjammer/vavi-sound-atrack)                         | jpcsp                                                                                                                        | Sony MD                                                          |
| sampled     | atrac3+      |       ✅       |       -        |       ✅        | [vavi-sound-atrack](https://github.com/umjammer/vavi-sound-atrack)                         | jpcsp                                                                                                                        | Sony MD                                                          |
| sampled     | atrac9       |       ✅       |       -        |       ✅        | [vavi-sound-atrack](https://github.com/umjammer/vavi-sound-atrack)                         | libatrac9                                                                                                                    | Sony Playstation                                                 |
| sampled     | g728         |       ✅       |       -        |       ✅        | [vavi-sound-amr](https://bitbucket.org/umjammer/vavi-sound-amr)                            | libCodec                                                                                                                     |                                                                  |
| sampled     | g729         |       ✅       |       -        |       ✅        | [vavi-sound-amr](https://bitbucket.org/umjammer/vavi-sound-amr)                            | libCodec                                                                                                                     |                                                                  |
| sampled     | g729a        |       ✅       |       -        |       ✅        | [vavi-sound-amr](https://bitbucket.org/umjammer/vavi-sound-amr)                            | libCodec                                                                                                                     |                                                                  |
| sampled     | amrnb        |       ✅       |       -        |       ✅        | [vavi-sound-amr](https://bitbucket.org/umjammer/vavi-sound-amr)                            | amrnb                                                                                                                        |                                                                  |
| midi        | mml          |       ✅       |       -        |       ✅        | this                                                                                       | [mml](http://asamomiji.jp/contents/mml-player)                                                                               | pc-8801 `cmd sing` like                                          |

### Features

 * ~~ALAC Java sound SPI~~ ... ([graduated incubation](https://github.com/umjammer/vavi-sound-alac))
 * OPAS Java sound SPI ... (candidate to graduate)
 * sox polyphase resampler Java sound SPI ... (candidate to graduate)
 * sox perfect resampler Java sound SPI ... (wip)
 * Mac AudioUnit synthesizer Java MIDI SPI ... (candidate to graduate)
 * JSyn synthesizer Java MIDI SPI ... (wip)
 * OPL3(Adlib) synthesizer Java MIDI SPI ... (wip)
 * [iTunes Library (rococoa) ... Music.app Music Database](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavix/rococoa/ituneslibrary)
 * MML synthesizer Java MIDI SPI

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-sound-sandbox)

## Usage

```java
AudioInputStream ais = AudioSystem.getAudioInputStream(Paths.get(opus).toFile());
Clip clip = AudioSystem.getClip();
clip.open(AudioSystem.getAudioInputStream(new AudioFormat(44100, 16, 2, true, false), ais));
clip.loop(Clip.LOOP_CONTINUOUSLY);
```

## References

 * [Gervill](https://github.com/bluenote10/gervill)
   * https://github.com/HectorRicardo/final-gervill
 * https://github.com/philfrei/AudioCue-maven
 * https://github.com/jitsi/libjitsi (voice codecs)
 * https://amei.or.jp/midistandardcommittee/MIDI1.0.pdf 🇯🇵
 * dsp
   * https://github.com/Frieve-A/effetune
   * https://github.com/psambit9791/jdsp
 * https://github.com/eclab/edisyn
 * https://github.com/eclab/seq

### Lesson

 * [javaassist doesn't support *enhanced for*](https://github.com/jboss-javassist/javassist/issues/403#issuecomment-989827788)
 * `com.sun.media.sound.SoftMidiAudioFileReader` has a bug that consumes 4 bytes and not releases (resets) those after examination

### Tech Know

* `tritonus-mp3` only supports mp3 w/o tags
* the reason we got "`javax.sound.midi.MidiUnavailableException: MIDI OUT transmitter not available`" is that `sound.jar` of `JMF` is in the class path.

## TODO

 * ~~jni in maven~~
 * ~~resampling spi~~
 * ~~qt has been deprecated~~
 * volume enabled clip
   * [mmscomputing](https://github.com/alex73/mmscomputing/blob/c666f63ec0b7f678aa3d05c1b2f63d748b743257/src/uk/co/mmscomputing/sound/provider/Clip.java#L12)
   * 🎯 [playn](https://github.com/playn/playn/blob/3ad0d6bf22c3f7c0eb6d3497523d197f4c50a46b/java-base/src/playn/java/BigClip.java#L46)
 * caf
   * https://github.com/ruda/caf

### Library

 * ~~midi is super heavy~~
 * Transcoder (outdated)
 * ~~channels~~
 * jsyn pink noise
 * ~~jsyn synth volume~~
 * ~~separate alac (git subtree, split?)~~
 * rename vavi.sound.midi.opl3 to vavi.sound.midi.adlib ?
 * check midi reader
   * tritonus???
   * instrument is not needed???

### codec

 * ~~twinvq~~
   * https://github.com/hendriks73/ffsampledsp
   * https://github.com/Icenowy/jcadencii (?)
   * http://k-takata.o.oo7.jp/mysoft/tvqdec.html
   * clean up twinvq
 * https://github.com/hendriks73/casampledsp/tree/master (coreaudio is base of avfoundation?)
 * ~~https://github.com/drogatkin/JustFLAC~~ → [vavi-sound-flac](https://github.com/umjammer/vavi-sound-flac)
 * ~~lc3~~ → [vavi-sound-lc3](https://github.com/umjammer/vavi-sound-lc3)
   * https://github.com/ninjasource/lc3-codec
   * https://www.iis.fraunhofer.de/ja/ff/amm/communication/lc3.html
 * ~~https://jmac.sourceforge.net/ (monkey's audio)~~ → [jmac](https://github.com/umjammer/jmac)

### midi

#### macos coremidi

 * ~~[osxmidi4j](https://github.com/locurasoft/osxmidi4j)~~ → [osxmidi4j](https://github.com/umjammer/osxmidi4j)
 * ~~https://github.com/DerekCook/CoreMidi4J~~ (jni)

#### macos audiounit

 * open audiounit custom view
   * https://github.com/nativelibs4java/BridJ (~~is able to deal objective-c blocks~~ inactive)
 * ~~volume~~
 * soundfont spi

#### others

 * opl3
   * need to fix: dro(old), midi, etc? 
   * ~~opl3 volume~~
   * opl3 midi reader
   * https://github.com/Wohlstand/ADLMIDI-Player-Java (android)
 * https://github.com/fedex81/emuSandbox
 * https://github.com/toyoshim/tss
 * ~~Apple DLS Sound device~~ (done)
 * https://github.com/comsonica/comsonica-studio
 * ~~https://github.com/jtrfp/javamod (has graphical equalizer, opl2,3 emulator)~~ → [javamod](https://github.com/umjammer/javamod)
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
 * ~~mml (cmd sing)~~
    * [thanks](http://asamomiji.jp/contents/mml-player)
    * crackling at end https://stackoverflow.com/a/9630897
    * https://github.com/trrk/FlMML-for-Android
    * ~~spi~~
 * Karplus-Strong
   * soundfont
 * exs24 soundfont
   * https://github.com/git-moss/ConvertWithMoss
   * [`AVAudioUnitSampler` can read exs24 soundfont???](https://github.com/AudioKit/AudioKit/blob/main/Tests/AudioKitTests/Node%20Tests/Playback%20Tests/AppleSamplerTests.swift#L68)
 * Muse-Sounds
   * https://github.com/CarlGao4/Muse-Sounds
   * `~/Library/Containers/com.muse.hub/Data/InstallData/Instruments/`
 * spi
   * https://github.com/hendriks73/pcmsampledsp
   * https://github.com/hendriks73/casampledsp
 * sf3
   * https://github.com/cognitone/sf2convert
 * sfz
   * https://github.com/git-moss/ConvertWithMoss
 * sse

### ebml (Extensible Binary Meta Language: Matroska)

 * https://github.com/Matroska-Org/jebml
 * https://github.com/matthewn4444/EBMLReader

---

<sub>image designed by @umjammer, drawn by nano banana</sub>
