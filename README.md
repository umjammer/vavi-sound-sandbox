[![](https://jitpack.io/v/umjammer/vavi-sound-sandbox.svg)](https://jitpack.io/#umjammer/vavi-sound-sandbox)

# vavi-sound-sandbox

Sandbox for sound libraries.

## Status

| **SPI** |  **Codec** | **IN Status** | **OUT Status** | **SPI Status** | **project** | **Description** | **Comment** |
|:--------|:-----------|:--------------|:---------------|:---------------|:------------|:----------------|:------------|
| midi    | unknown    | 🚫 | 🚫 | - | this | MFi by [unknown]() | |
| midi    | ittake     | 🚫 | 🚫 | - | this | MFi by [ittake]() | |
| sampled | ilbc       | 🚫 | 🚫 | - | this | [c](http://www.ilbcfreeware.org/) | |
| sampled | ldcelp     | 🚫 | 🚫 | - | this | [c](ftp://svr-ftp.eng.cam.ac.uk/pub/comp.speech/coding/ldcelp-2.0.tar.gz) | |
| sampled | mp3        | 🚫 | -  | -  | this | [mp3](https://github.com/umjammer/vavi-sound-sandbox/tree/master/src/main/java/vavi/sound/mp3) | need to deal tags |
| sampled | sse        | 🚫 | -  | 🚫 | this | [sse](http://shibatch.sourceforge.net/download/) | |
| sampled | laoe       | ?  | -  | -  | this | [resampling](http://www.oli4.ch/laoe/home.html) | |
| sampled | rohm       | ?  | -  | -  | this | resampling | |
| sampled | polyphase  | ✅ | -  | 🚧 | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | resampler  | ✅ | -  | - | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | perfect    | 🚧 | -  | - | this | [sox](http://sox.sourceforge.net/) resampling | |
| sampled | tritonus   | ✅ | -  | ✅ | [tritonus](https://github.com/umjammer/tritonus) | | monauralize |
| sampled | alac       | ✅ | -  | ✅ | [Apple Lossless Audio Decoder](https://github.com/umjammer/Java-Apple-Lossless-decoder) | | |
| sampled | QTKit      | ✅ | -  | - | [rococoa](https://github.com/umjammer/rococoa) | | you must lock jna version |
| sampled | AVFoundation | - | -  | - | [rococoa](https://github.com/umjammer/rococoa) | | you must lock jna version |
| sampled | twinvq     | 🚫 | 🚫 | - | this | | TODO use ffmpeg |
| -       | vsq        | ✅ | -  | - | this | | YAMAHA Vocaloid |
| sampled | opus       | ✅ | 🚫 | ✅ | this | [concentus](https://github.com/lostromb/concentus) | |
| midi    | midi       | 🚫 | -  | 🚫 | [osxmidi4j](https://github.com/locurasoft/osxmidi4j) | | for hardware midi only? |
| sampled | speex      | ✅ | -  | ✅ | [jspeex](http://jspeex.sourceforge.net/) | | sample rate is limited to convert |
| sampled | flac       | ✅ | -  | ✅ | [jFLAC](http://jflac.sourceforge.net/) | | |
| sampled | aac        | -  | -  | ✅ | [JAADec](https://github.com/umjammer/JAADec) | | |
| sampled | vorbis     | -  | -  | ✅ | [vorbisspi](http://www.javazoom.net/vorbisspi/vorbisspi.html) | | AudioSystem version conflict? |

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

## License

  * [Java Sound Example](http://www.jsresources.org/)

> ```
> /*
>  * Copyright (c) 1999 - 2003 by Matthias Pfisterer
>  * All rights reserved.
>  *
>  * Redistribution and use in source and binary forms, with or without
>  * modification, are permitted provided that the following conditions
>  * are met:
>  *
>  * - Redistributions of source code must retain the above copyright notice,
>  *   this list of conditions and the following disclaimer.
>  * - Redistributions in binary form must reproduce the above copyright
>  *   notice, this list of conditions and the following disclaimer in the
>  *   documentation and/or other materials provided with the distribution.
>  *
>  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
>  * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
>  * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
>  * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
>  * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
>  * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
>  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
>  * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
>  * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
>  * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
>  * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
>  * OF THE POSSIBILITY OF SUCH DAMAGE.
>  */
> ```
 