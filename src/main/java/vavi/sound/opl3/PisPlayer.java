/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2023 Simon Peter, <dn.tlp@gmx.net>, et al.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package vavi.sound.opl3;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Beni Tracker PIS Player.
 * Ported from adplug's pis.cpp / pis.h by Jonas Santoso,
 * adapted from 'pisplay' by Dmitry Smagin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class PisPlayer extends Opl3Player {

    private static final int PIS_NONE = -1;

    private static final int OPL_NOTE_FREQUENCY_LO_B = 0x143;
    private static final int OPL_NOTE_FREQUENCY_LO_C = 0x157;
    private static final int OPL_NOTE_FREQUENCY_HI_B = 0x287;
    private static final int OPL_NOTE_FREQUENCY_HI_C = 0x2ae;

    private static final int PIS_DEFAULT_SPEED = 6;

    private static final int[] opl_voice_offset_into_registers = {
        0, 1, 2, 8, 9, 10, 16, 17, 18
    };

    private static final int[] frequency_table = {
        0x157, 0x16B, 0x181, 0x198, 0x1B0, 0x1CA,
        0x1E5, 0x202, 0x220, 0x241, 0x263, 0x287
    };

    private static class PisInstrument {
        int mul1, mul2; // multiplier
        int lev1, lev2; // level
        int atd1, atd2; // attack / decay
        int sur1, sur2; // sustain / release
        int wav1, wav2; // waveform
        int fbcon;      // feedback / connection_type
    }

    private static class PisVoiceState {
        int note;
        int octave;
        int instrument = PIS_NONE;
        int volume;
        int frequency;
        int previousEffect;
        int slideIncrement;
        int portaIncrement;
        int portaSrcFreq;
        int portaSrcOctave;
        int portaDestFreq;
        int portaDestOctave;
        int portaSign;
        int arpeggioFlag;
        final int[] arpeggioFreq = new int[3];
        final int[] arpeggioOctave = new int[3];
    }

    private static class PisRowUnpacked {
        int note;
        int octave;
        int instrument;
        int effect;
    }

    // module data
    /** length of order list */
    private int length;
    private int numberOfPatterns;
    private int numberOfInstruments;
    /** order list for each channel */
    private final int[][] order = new int[256][9];
    /** pattern data */
    private final int[][] pattern = new int[128][64];
    /** instrument data */
    private final PisInstrument[] instrument = new PisInstrument[64];

    // replay state
    private int speed;
    private int count;
    private int position;
    private int row;
    private int positionJump;
    private int patternBreak;
    private int arpeggioIndex;
    private int loopFlag;
    private int loopStartRow;
    private int loopCount;
    private final PisVoiceState[] voiceState = new PisVoiceState[9];
    private final PisRowUnpacked[] rowBuffer = new PisRowUnpacked[9];

    private boolean isPlaying;

    public PisPlayer() {
        for (int i = 0; i < 64; i++) {
            instrument[i] = new PisInstrument();
        }
        for (int i = 0; i < 9; i++) {
            rowBuffer[i] = new PisRowUnpacked();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Beni Tracker PIS module", "pis");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("PIS");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            // PIS has no signature; adplug validates by file extension only.
            // When the source URI is known, gate on the extension; the
            // structural check below (exact size equation) does the rest.
            URI uri = SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && !path.toLowerCase().endsWith(".pis")) {
                    return false;
                }
            }
            bitStream.mark(Integer.MAX_VALUE);
            return matchFormatImpl(bitStream);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    private static boolean matchFormatImpl(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 3) return false;

        int length = buf[0] & 0xff;
        int npat = buf[1] & 0xff;
        int ninst = buf[2] & 0xff;
        if (length == 0 || npat == 0 || npat > 128 || ninst == 0 || ninst > 32) {
            return false;
        }

        // the file layout size must match (some files carry a small
        // trailing signature tag, e.g. "B.J." in ACTION.PIS)
        int expected = 3 + npat + ninst + 9 * length + npat * 64 * 3 + ninst * 11;
        if (buf.length < expected || buf.length - expected > 16) {
            return false;
        }

        // map entries must be in range
        for (int i = 0; i < npat; i++) {
            if ((buf[3 + i] & 0xff) >= 128) return false;
        }
        for (int i = 0; i < ninst; i++) {
            if ((buf[3 + npat + i] & 0xff) >= 64) return false;
        }
        return true;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 3) {
            throw new IllegalArgumentException("file too short");
        }

        int offset = 0;
        length = buf[offset++] & 0xff;
        numberOfPatterns = buf[offset++] & 0xff;
        numberOfInstruments = buf[offset++] & 0xff;

        int[] patternMap = new int[128];
        int[] instrumentMap = new int[32];

        for (int i = 0; i < numberOfPatterns; i++) {
            patternMap[i] = buf[offset++] & 0xff;
        }
        for (int i = 0; i < numberOfInstruments; i++) {
            instrumentMap[i] = buf[offset++] & 0xff;
        }

        for (int i = 0; i < length; i++) {
            for (int v = 0; v < 9; v++) {
                order[i][v] = buf[offset++] & 0xff;
            }
        }

        for (int i = 0; i < numberOfPatterns; i++) {
            int j = patternMap[i];
            for (int r = 0; r < 64; r++) {
                int packed = (buf[offset] & 0xff) << 16;
                packed |= (buf[offset + 1] & 0xff) << 8;
                packed |= buf[offset + 2] & 0xff;
                offset += 3;
                pattern[j][r] = packed;
            }
        }

        for (int i = 0; i < numberOfInstruments; i++) {
            PisInstrument pi = instrument[instrumentMap[i]];
            pi.mul1 = buf[offset++] & 0xff;
            pi.mul2 = buf[offset++] & 0xff;
            pi.lev1 = buf[offset++] & 0xff;
            pi.lev2 = buf[offset++] & 0xff;
            pi.atd1 = buf[offset++] & 0xff;
            pi.atd2 = buf[offset++] & 0xff;
            pi.sur1 = buf[offset++] & 0xff;
            pi.sur2 = buf[offset++] & 0xff;
            pi.wav1 = buf[offset++] & 0xff;
            pi.wav2 = buf[offset++] & 0xff;
            pi.fbcon = buf[offset++] & 0xff;
        }

        rewind(0);
    }

    @Override
    public boolean update() {
        if (isPlaying) {
            count++;
            if (count >= speed) {

                unpackRow();

                for (int v = 0; v < 9; v++) {
                    replayVoice(v);
                }

                advanceRow();
            } else {
                doPerFrameEffects();
            }
        }
        return isPlaying;
    }

    @Override
    public void rewind(int subSong) {
        // init replay state
        speed = PIS_DEFAULT_SPEED;
        count = PIS_DEFAULT_SPEED - 1;
        position = 0;
        row = 0;
        positionJump = PIS_NONE;
        patternBreak = PIS_NONE;
        arpeggioIndex = 0;
        loopFlag = 0;
        loopStartRow = 0;
        loopCount = 0;
        for (int i = 0; i < 9; i++) {
            voiceState[i] = new PisVoiceState();
        }

        // init the OPL chip
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 0x20); // enable waveform control

        isPlaying = true;
    }

    @Override
    public float getRefresh() {
        return 50.0f;
    }

    // ---- replay routine

    private static int effectHi(PisRowUnpacked r) {
        return r.effect >> 8;
    }

    private static int effectLo(PisRowUnpacked r) {
        return r.effect & 0xff;
    }

    private static int effectMidnib(PisRowUnpacked r) {
        return (r.effect >> 4) & 15;
    }

    private static int effectLonib(PisRowUnpacked r) {
        return r.effect & 15;
    }

    private static boolean hasNote(PisRowUnpacked r) {
        return r.note < 12;
    }

    private static boolean hasInstrument(PisRowUnpacked r) {
        return r.instrument > 0;
    }

    private void replayVoice(int v) {
        PisVoiceState vs = voiceState[v];
        PisRowUnpacked r = rowBuffer[v];

        if (effectHi(r) == 0x03) {
            // with portamento
            enterRowWithPortamento(v, vs, r);
        } else {
            if (hasInstrument(r)) {
                if (hasNote(r)) {
                    enterRowWithInstrumentAndNote(v, vs, r);
                } else {
                    enterRowWithInstrumentOnly(v, vs, r);
                }
            } else {
                if (hasNote(r)) {
                    enterRowWithNoteOnly(v, vs, r);
                } else {
                    enterRowWithPossiblyEffectOnly(v, vs, r);
                }
            }
        }

        handleEffect(v, vs, r);

        if (r.effect != 0) {
            vs.previousEffect = r.effect;
        } else {
            vs.previousEffect = PIS_NONE;
            setVoiceVolatiles(v, 0, 0, 0);
        }
    }

    private void enterRowWithPortamento(int v, PisVoiceState vs, PisRowUnpacked r) {
        if (hasInstrument(r)) {
            setInstrument(v, r.instrument);
            if (vs.volume < 63) {
                setLevel(v, r.instrument, PIS_NONE, 0);
            }
        }
        if (hasNote(r)) {
            vs.portaSrcFreq = vs.frequency;
            vs.portaSrcOctave = vs.octave;
            vs.portaDestFreq = frequency_table[r.note];
            vs.portaDestOctave = r.octave;

            if (vs.portaDestOctave < vs.octave) {
                vs.portaSign = -1;
            } else if (vs.portaDestOctave > vs.octave) {
                vs.portaSign = 1;
            } else {
                vs.portaSign = (vs.portaDestFreq < vs.frequency) ? -1 : 1;
            }
        }
    }

    private void enterRowWithInstrumentAndNote(int v, PisVoiceState vs, PisRowUnpacked r) {
        vs.previousEffect = PIS_NONE;

        oplNoteOff(v);
        if (effectHi(r) != 0x0c) {
            // volume is not set
            if (r.instrument != vs.instrument) {
                // is new instrument
                setInstrument(v, r.instrument);
            } else if (vs.volume < 63) {
                setLevel(v, r.instrument, PIS_NONE, 0);
            }
        } else {
            // volume is set
            if (r.instrument != vs.instrument) {
                // is new instrument
                setInstrument(v, r.instrument);
            }
            setLevel(v, r.instrument, effectLo(r), 1);
        }
        // trigger new note
        setNote(v, vs, r);
    }

    private void enterRowWithInstrumentOnly(int v, PisVoiceState vs, PisRowUnpacked r) {
        if (r.instrument != vs.instrument) {
            // is new instrument
            setInstrument(v, r.instrument);

            // set operator level according to instrument and possibly Cxx effect
            if (effectHi(r) == 0x0c) {
                setLevel(v, r.instrument, effectLo(r), 1);
            } else if (vs.volume < 63) {
                setLevel(v, r.instrument, PIS_NONE, 0);
            }

            if ((vs.previousEffect != PIS_NONE) && ((vs.previousEffect & 0xF00) == 0)) {
                // reset to base tone after arpeggio
                oplSetPitch(v, vs.frequency, vs.octave);
            }
        }
    }

    private void enterRowWithNoteOnly(int v, PisVoiceState vs, PisRowUnpacked r) {
        vs.previousEffect = PIS_NONE;

        if (vs.instrument != PIS_NONE) {
            // set operator level according to instrument and possibly Cxx effect
            if (effectHi(r) == 0x0c) {
                setLevel(v, vs.instrument, effectLo(r), 1);
            } else if (vs.volume < 63) {
                setLevel(v, vs.instrument, PIS_NONE, 0);
            }
        }
        // trigger new note
        setNote(v, vs, r);
    }

    private void enterRowWithPossiblyEffectOnly(int v, PisVoiceState vs, PisRowUnpacked r) {
        // set operator level according to instrument and Cxx effect
        if (vs.instrument != PIS_NONE && effectHi(r) == 0x0c) {
            setLevel(v, vs.instrument, effectLo(r), 1);
        }

        if ((vs.previousEffect != PIS_NONE) && ((vs.previousEffect & 0xF00) == 0)) {
            // reset to base tone after arpeggio
            oplSetPitch(v, vs.frequency, vs.octave);
        }
    }

    private void handleEffect(int v, PisVoiceState vs, PisRowUnpacked r) {
        switch (effectHi(r)) {
        case 0x00: // arpeggio
            if (effectLo(r) != 0) {
                handleArpeggio(v, vs, r);
            } else {
                vs.arpeggioFlag = 0;
            }
            break;
        case 0x01: // slide up
            vs.slideIncrement = effectLo(r);
            break;
        case 0x02: // slide down
            vs.slideIncrement = -effectLo(r);
            break;
        case 0x03: // tone portamento
            setVoiceVolatiles(v, 0, 0, effectLo(r));
            break;
        case 0x0b: // position jump
            setVoiceVolatiles(v, 0, 0, 0);
            positionJump = effectLo(r);
            break;
        case 0x0d: // pattern break
            setVoiceVolatiles(v, 0, 0, 0);
            patternBreak = effectLo(r);
            break;
        case 0x0e: // Exx commands
            handleExxCommand(v, vs, r);
            break;
        case 0x0f: // set speed
            setVoiceVolatiles(v, 0, 0, 0);
            if (effectLo(r) != 0) {
                speed = effectLo(r);
            } else {
                isPlaying = false;
            }
            break;
        }
    }

    private void handleExxCommand(int v, PisVoiceState vs, PisRowUnpacked r) {
        switch (effectMidnib(r)) {
        case 0x06: // loop
            handleLoop(v, r);
            break;
        case 0x0a: // volume slide up
        case 0x0b: // volume slide down
            handleVolumeSlide(v, vs, r);
            break;
        }
    }

    private void handleLoop(int v, PisRowUnpacked r) {
        if (loopFlag == 0) {
            // playing for the first time
            if (effectLonib(r) == 0) {
                // set loop start row
                loopStartRow = row;
            } else {
                // initialize loop counter
                loopCount = effectLonib(r);
                loopFlag = 1;
            }
        }

        if (loopFlag != 0 && effectLonib(r) != 0) {
            // repeating
            loopCount--;

            if (loopCount >= 0) {
                row = loopStartRow - 1;
            } else {
                loopFlag = 0;
            }
        }
    }

    private void handleVolumeSlide(int v, PisVoiceState vs, PisRowUnpacked r) {
        if (vs.instrument != PIS_NONE) {
            int level = (effectMidnib(r) == 0x0a)
                    ? (vs.volume + effectLonib(r))
                    : (vs.volume - effectLonib(r));

            if (level < 2) {
                level = 2;
            } else if (level > 63) {
                level = 63;
            }

            setLevel(v, vs.instrument, level, 0);
        }
    }

    private void doPerFrameEffects() {
        arpeggioIndex++;
        if (arpeggioIndex == 3) {
            arpeggioIndex = 0;
        }

        for (int v = 0; v < 8; v++) {
            PisVoiceState vs = voiceState[v];
            if (vs.slideIncrement != 0) {
                vs.frequency += vs.slideIncrement;
                oplSetPitch(v, vs.frequency, vs.octave);
            } else if (vs.portaIncrement != 0) {
                doPerFramePortamento(v, vs);
            } else if (vs.arpeggioFlag != 0) {
                int freq = vs.arpeggioFreq[arpeggioIndex];
                oplSetPitch(v, freq, vs.arpeggioOctave[arpeggioIndex]);
            }
        }
    }

    private void doPerFramePortamento(int v, PisVoiceState vs) {
        if (vs.portaSign == 1) {
            vs.frequency += vs.portaIncrement;
            if ((vs.octave == vs.portaDestOctave) && (vs.frequency > vs.portaDestFreq)) {
                vs.frequency = vs.portaDestFreq;
                vs.portaIncrement = 0;
            }
            if (vs.frequency > OPL_NOTE_FREQUENCY_HI_B) {
                vs.frequency = OPL_NOTE_FREQUENCY_LO_B + (vs.frequency - OPL_NOTE_FREQUENCY_HI_B);
                vs.octave++;
            }
        } else {
            vs.frequency -= vs.portaIncrement;
            if ((vs.octave == vs.portaDestOctave) && (vs.frequency < vs.portaDestFreq)) {
                vs.frequency = vs.portaDestFreq;
                vs.portaIncrement = 0;
            }
            if (vs.frequency < OPL_NOTE_FREQUENCY_LO_C) {
                vs.frequency = OPL_NOTE_FREQUENCY_HI_C - (OPL_NOTE_FREQUENCY_LO_C - vs.frequency);
                vs.octave--;
            }
        }
        oplSetPitch(v, vs.frequency, vs.octave);
    }

    private void handleArpeggio(int v, PisVoiceState vs, PisRowUnpacked r) {
        if (effectLo(r) != (vs.previousEffect & 0xff)) {
            vs.arpeggioFreq[0] = frequency_table[vs.note];
            vs.arpeggioOctave[0] = vs.octave;
            int an1 = vs.note + effectMidnib(r);
            int an2 = vs.note + effectLonib(r);
            if (an1 < 12) {
                vs.arpeggioFreq[1] = frequency_table[an1];
                vs.arpeggioOctave[1] = vs.octave;
            } else {
                vs.arpeggioFreq[1] = frequency_table[an1 - 12];
                vs.arpeggioOctave[1] = vs.octave + 1;
            }
            if (an2 < 12) {
                vs.arpeggioFreq[2] = frequency_table[an2];
                vs.arpeggioOctave[2] = vs.octave;
            } else {
                vs.arpeggioFreq[2] = frequency_table[an2 - 12];
                vs.arpeggioOctave[2] = vs.octave + 1;
            }
            vs.arpeggioFlag = 1;
        }

        vs.slideIncrement = 0;
        vs.portaIncrement = 0;
    }

    private void setNote(int v, PisVoiceState vs, PisRowUnpacked r) {
        int frequency = frequency_table[r.note];
        oplSetPitch(v, frequency, r.octave);
        vs.note = r.note;
        vs.octave = r.octave;
        vs.frequency = frequency;
    }

    private void setInstrument(int v, int instrIndex) {
        oplSetInstrument(v, instrument[instrIndex]);
        voiceState[v].instrument = instrIndex;
    }

    private void setLevel(int v, int instrIndex, int gain, int doApplyCorrection) {
        PisInstrument instr = instrument[instrIndex];

        int base = doApplyCorrection != 0 ? 62 : 64;

        if (gain == PIS_NONE) {
            gain = 64;
            voiceState[v].volume = 63;
        } else {
            voiceState[v].volume = gain;
        }

        int l1 = base - (gain * (64 - instr.lev1) >> 6);
        int l2 = base - (gain * (64 - instr.lev2) >> 6);

        write(0, 0x40 + opl_voice_offset_into_registers[v], l1 & 0xff);
        write(0, 0x43 + opl_voice_offset_into_registers[v], l2 & 0xff);
    }

    private void setVoiceVolatiles(int v, int arpeggioFlag, int slideIncrement, int portaIncrement) {
        PisVoiceState vs = voiceState[v];
        vs.arpeggioFlag = arpeggioFlag;
        vs.slideIncrement = slideIncrement;
        vs.portaIncrement = portaIncrement;
    }

    private void unpackRow() {
        for (int v = 0; v < 9; v++) {
            int patternIndex = order[position][v];
            int packed = pattern[patternIndex][row];

            int el = packed & 0xff;
            packed >>= 8;
            int b2 = packed & 0xff;
            packed >>= 8;
            int b1 = packed & 0xff;

            rowBuffer[v].note = b1 >> 4;
            rowBuffer[v].octave = (b1 >> 1) & 7;
            rowBuffer[v].instrument = ((b1 & 1) << 4) | (b2 >> 4);
            rowBuffer[v].effect = ((b2 & 15) << 8) | el;
        }
    }

    private void advanceRow() {
        if (positionJump >= 0) {
            position = positionJump;
            isPlaying = false; // treat pattern jump as stop

            if (patternBreak == PIS_NONE) {
                // position jump without pattern break
                row = 0;
            } else {
                // position jump with pattern break
                row = patternBreak;
                patternBreak = PIS_NONE;
            }
            positionJump = PIS_NONE;
        } else if (patternBreak >= 0) {
            // pattern break
            position++;
            if (position == length) {
                position = 0;
                isPlaying = false;
            }
            row = patternBreak;
            patternBreak = PIS_NONE;
        } else {
            // simple row advance
            row++;
            if (row == 64) {
                row = 0;
                position++;
                if (position == length) {
                    position = 0;
                    isPlaying = false;
                }
            }
        }

        count = 0;
    }

    // ---- OPL output

    private void oplSetPitch(int v, int freq, int octave) {
        write(0, 0xa0 + v, freq & 0xff);
        write(0, 0xb0 + v, (0x20 | (octave << 2) | (freq >> 8)) & 0xff);
    }

    private void oplNoteOff(int v) {
        write(0, 0xb0 + v, 0);
    }

    private void oplSetInstrument(int v, PisInstrument instr) {
        int reg = 0x20 + opl_voice_offset_into_registers[v];
        write(0, reg, instr.mul1);
        reg += 3;
        write(0, reg, instr.mul2);
        reg += 0x1d;
        write(0, reg, instr.lev1);
        reg += 3;
        write(0, reg, instr.lev2);
        reg += 0x1d;
        write(0, reg, instr.atd1);
        reg += 3;
        write(0, reg, instr.atd2);
        reg += 0x1d;
        write(0, reg, instr.sur1);
        reg += 3;
        write(0, reg, instr.sur2);
        reg += 0x5d;
        write(0, reg, instr.wav1);
        reg += 3;
        write(0, reg, instr.wav2);
        write(0, 0xc0 + v, instr.fbcon);
    }
}
