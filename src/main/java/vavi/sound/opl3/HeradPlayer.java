/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2005 Simon Peter <dn.tlp@gmx.net>, et al.
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
import java.util.Arrays;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Herbulot AdLib Player (HERAD).
 * Ported from adplug's herad.cpp / herad.h by Stas'M.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class HeradPlayer extends Opl3Player {

    private static final int HERAD_MIN_SIZE = 6;
    private static final int HERAD_MAX_SIZE = 75775;
    private static final int HERAD_HEAD_SIZE = 52;
    private static final int HERAD_COMP_NONE = 0;
    private static final int HERAD_COMP_HSQ = 1;
    private static final int HERAD_COMP_SQX = 2;
    private static final int HERAD_MAX_TRACKS = 21;
    private static final int HERAD_INST_SIZE = 40;
    private static final int HERAD_INSTMODE_KMAP = -1;
    private static final int HERAD_BEND_CENTER = 0x40;
    private static final int HERAD_NOTE_OFF = 0;
    private static final int HERAD_NOTE_ON = 1;
    private static final int HERAD_NOTE_UPDATE = 2;
    private static final int HERAD_NUM_VOICES = 9;
    private static final int HERAD_NUM_NOTES = 12;
    private static final int HERAD_MEASURE_TICKS = 96;

    private static final int[] slot_offset = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private static final int[] FNum = {
        343, 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647
    };

    private static final int[] fine_bend = {
        0, 20, 22, 23, 24, 26, 27, 29, 30, 33, 34, 36, 38
    };

    private static final int[] coarse_bend = {
        0, 4, 8, 12, 16, 20, 24, 28, 32, 36
    };

    private static class HeradTrk {
        int size;
        byte[] data;
        int pos;
        long counter;
        int ticks;
    }

    private static class HeradChn {
        int program;
        int playprog;
        int note;
        boolean keyon;
        int bend;
        int slide_dur;
    }

    private static class HeradInst {
        final byte[] data = new byte[40];

        int mode() { return data[0]; } // signed!
        int mod_ksl() { return data[2] & 0xff; }
        int mod_mul() { return data[3] & 0xff; }
        int feedback() { return data[4] & 0xff; }
        int mod_A() { return data[5] & 0xff; }
        int mod_S() { return data[6] & 0xff; }
        int mod_eg() { return data[7] & 0xff; }
        int mod_D() { return data[8] & 0xff; }
        int mod_R() { return data[9] & 0xff; }
        int mod_out() { return data[10] & 0xff; }
        int mod_am() { return data[11] & 0xff; }
        int mod_vib() { return data[12] & 0xff; }
        int mod_ksr() { return data[13] & 0xff; }
        int con() { return data[14] & 0xff; }
        int car_ksl() { return data[15] & 0xff; }
        int car_mul() { return data[16] & 0xff; }
        int pan() { return data[17] & 0xff; }
        int car_A() { return data[18] & 0xff; }
        int car_S() { return data[19] & 0xff; }
        int car_eg() { return data[20] & 0xff; }
        int car_D() { return data[21] & 0xff; }
        int car_R() { return data[22] & 0xff; }
        int car_out() { return data[23] & 0xff; }
        int car_am() { return data[24] & 0xff; }
        int car_vib() { return data[25] & 0xff; }
        int car_ksr() { return data[26] & 0xff; }
        int mc_fb_at() { return data[27]; } // signed!
        int mod_wave() { return data[28] & 0xff; }
        int car_wave() { return data[29] & 0xff; }
        int mc_mod_out_vel() { return data[30]; } // signed!
        int mc_car_out_vel() { return data[31]; } // signed!
        int mc_fb_vel() { return data[32]; } // signed!
        int mc_slide_coarse() { return data[33] & 0xff; }
        int mc_transpose() { return data[34] & 0xff; }
        int mc_slide_dur() { return data[35] & 0xff; }
        int mc_slide_range() { return data[36]; } // signed!
        int mc_mod_out_at() { return data[38]; } // signed!
        int mc_car_out_at() { return data[39]; } // signed!

        // Keymap properties
        int keymap_offset() { return data[2] & 0xff; }
        int keymap_index(int idx) { return data[4 + idx] & 0xff; }
    }

    private boolean songend;
    private int wTime;
    private int ticks_pos;
    private int total_ticks;

    private int comp;
    private boolean AGD;
    private boolean v2;
    private int nTracks;
    private int nInsts;

    private int wLoopStart;
    private int wLoopEnd;
    private int wLoopCount;
    private int wSpeed;

    private HeradTrk[] track;
    private HeradChn[] chn;
    private HeradInst[] inst;

    public HeradPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Herbulot AdLib Player (HERAD)", "hrad");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("HERAD");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(0x38);
            return matchFormatImpl(bitStream);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    protected boolean matchFormatImpl(InputStream is) throws IOException {
        byte[] head = new byte[0x36];
        int read = is.read(head);
        if (read < 6) return false;
        if (isHSQ(head, read)) return true;
        if (isSQX(head)) return true;

        // Uncompressed HERAD check: need enough bytes to validate header
        if (read < 0x34) return false;
        int totalOffset = (head[0] & 0xff) | ((head[1] & 0xff) << 8);
        int firstTrkOff = (head[2] & 0xff) | ((head[3] & 0xff) << 8);
        if (firstTrkOff != 0x32 && firstTrkOff != 0x52) return false;
        // totalOffset should be > header size (0x32 or 0x52) and within valid range
        if (totalOffset < firstTrkOff || totalOffset > HERAD_MAX_SIZE) return false;
        // wSpeed at offset 0x32 must be non-zero
        int wSpeed = (head[0x32] & 0xff) | ((head[0x33] & 0xff) << 8);
        return wSpeed != 0;
    }

    private static boolean isHSQ(byte[] data, int size) {
        if (data.length < 6) return false;
        if (data[2] != 0) return false;
        int checksum = 0;
        for (int i = 0; i < 6; i++) {
            checksum += data[i] & 0xff;
        }
        return (checksum & 0xff) == 0xAB;
    }

    private static boolean isSQX(byte[] data) {
        if (data.length < 6) return false;
        if ((data[2] & 0xff) > 2 || (data[3] & 0xff) > 2 || (data[4] & 0xff) > 2) {
            return false;
        }
        int data5 = data[5] & 0xff;
        return data5 != 0 && data5 <= 15;
    }

    private static int u16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] rawData = is.readAllBytes();
        int size = rawData.length;

        if (size < HERAD_MIN_SIZE || size > HERAD_MAX_SIZE) {
            throw new IllegalArgumentException("invalid file size");
        }

        byte[] data;
        if (isHSQ(rawData, size)) {
            comp = HERAD_COMP_HSQ;
            byte[] out = new byte[HERAD_MAX_SIZE];
            size = HSQ_decompress(rawData, size, out);
            data = new byte[size];
            System.arraycopy(out, 0, data, 0, size);
        } else if (isSQX(rawData)) {
            comp = HERAD_COMP_SQX;
            byte[] out = new byte[HERAD_MAX_SIZE];
            size = SQX_decompress(rawData, size, out);
            data = new byte[size];
            System.arraycopy(out, 0, data, 0, size);
        } else {
            comp = HERAD_COMP_NONE;
            data = rawData;
        }

        if (size < HERAD_HEAD_SIZE) {
            throw new IllegalArgumentException("file size too small after decompression");
        }

        int totalOffset = u16(data, 0);
        if (size < totalOffset) {
            throw new IllegalArgumentException("incorrect offset/file size");
        }

        nInsts = (size - totalOffset) / HERAD_INST_SIZE;
        if (nInsts == 0) {
            throw new IllegalArgumentException("M32 files not supported");
        }

        int firstTrkOff = u16(data, 2);
        if (firstTrkOff != 0x32 && firstTrkOff != 0x52) {
            throw new IllegalArgumentException("wrong first track offset");
        }

        AGD = (firstTrkOff == 0x52);
        wLoopStart = u16(data, 0x2C);
        wLoopEnd = u16(data, 0x2E);
        wLoopCount = u16(data, 0x30);
        wSpeed = u16(data, 0x32);

        if (wSpeed == 0) {
            throw new IllegalArgumentException("speed not defined");
        }

        nTracks = 0;
        for (int i = 0; i < HERAD_MAX_TRACKS; i++) {
            if (u16(data, 2 + i * 2) == 0) {
                break;
            }
            nTracks++;
        }

        track = new HeradTrk[nTracks];
        chn = new HeradChn[nTracks];
        for (int i = 0; i < nTracks; i++) {
            track[i] = new HeradTrk();
            chn[i] = new HeradChn();
            int offset = u16(data, 2 + i * 2) + 2;
            int next = (i < HERAD_MAX_TRACKS - 1) ? u16(data, 2 + (i + 1) * 2) + 2 : totalOffset;
            if (next <= 2) next = totalOffset;

            track[i].size = next - offset;
            track[i].data = new byte[track[i].size];
            System.arraycopy(data, offset, track[i].data, 0, track[i].size);
        }

        inst = new HeradInst[nInsts];
        v2 = false;
        for (int i = 0; i < nInsts; i++) {
            inst[i] = new HeradInst();
            System.arraycopy(data, totalOffset + i * HERAD_INST_SIZE, inst[i].data, 0, HERAD_INST_SIZE);
            if (!v2 && inst[i].mode() == HERAD_INSTMODE_KMAP) {
                v2 = true;
            }
        }

        if (!v2) {
            v2 = (validTracks() == 1);
        }

        rewind(0);
    }

    private boolean validEvent(int i, int[] offsetRef, boolean v2Mode) {
        int offset = offsetRef[0];
        while (offset < track[i].size && (track[i].data[offset++] & 0x80) > 0);
        if (offset >= track[i].size) {
            return false;
        }

        int status = track[i].data[offset++] & 0xff;
        if (status < 0x80) {
            return false;
        } else if (status < 0x90 && v2Mode) {
            int check = track[i].data[offset++] & 0xff;
            if (check > 0x7F) return false;
        } else if (status < 0xC0) {
            int check = track[i].data[offset++] & 0xff;
            if (check > 0x7F) return false;
            check = track[i].data[offset++] & 0xff;
            if (check > 0x7F) return false;
        } else if (status < 0xF0) {
            int check = track[i].data[offset++] & 0xff;
            if (check > 0x7F) return false;
        } else if (status == 0xFF) {
            offset = track[i].size;
        }

        offsetRef[0] = offset;
        return true;
    }

    private int validTracks() {
        for (int i = 0; i < nTracks; i++) {
            int[] of_v1 = {0};
            int[] of_v2 = {0};

            while (of_v1[0] < track[i].size || of_v2[0] < track[i].size) {
                if (of_v1[0] < track[i].size) {
                    if (!validEvent(i, of_v1, false)) {
                        return 1;
                    }
                }
                if (of_v2[0] < track[i].size) {
                    if (!validEvent(i, of_v2, true)) {
                        return 2;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public void rewind(int subsong) throws IOException {
        wTime = 0;
        songend = false;
        ticks_pos = -1;
        total_ticks = 0;

        for (int i = 0; i < nTracks; i++) {
            track[i].pos = 0;
            int j = 0;
            while (track[i].pos < track[i].size) {
                j += GetTicks(i);
                int cmd = track[i].data[track[i].pos++] & 0xF0;
                switch (cmd) {
                    case 0x80 -> track[i].pos += (v2 ? 1 : 2);
                    case 0x90, 0xA0, 0xB0 -> track[i].pos += 2;
                    case 0xC0, 0xD0, 0xE0 -> track[i].pos++;
                    default -> track[i].pos = track[i].size;
                }
            }
            if (j > total_ticks) {
                total_ticks = j;
            }
            track[i].pos = 0;
            track[i].counter = 0;
            track[i].ticks = 0;
            chn[i].program = 0;
            chn[i].playprog = 0;
            chn[i].note = 0;
            chn[i].keyon = false;
            chn[i].bend = HERAD_BEND_CENTER;
            chn[i].slide_dur = 0;
        }

        if (v2) {
            if (wLoopStart == 0 || wLoopCount != 0) wLoopStart = 1;
            if (wLoopEnd == 0 || wLoopCount != 0) wLoopEnd = getpatterns() + 1;
            if (wLoopCount != 0) wLoopCount = 0;
        }

        // reset OPL chip
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
            write(1, i, 0);
        }
        write(0, 1, 32);
        write(0, 0xBD, 0);
        write(0, 8, 64);
        if (AGD) {
            write(1, 5, 1);
            write(1, 4, 0);
        }
    }

    private int GetTicks(int t) {
        int result = 0;
        do {
            result <<= 7;
            result |= track[t].data[track[t].pos] & 0x7F;
        } while ((track[t].data[track[t].pos++] & 0x80) != 0 && track[t].pos < track[t].size);
        return result;
    }

    private void executeCommand(int t) {
        if (t >= nTracks) return;
        if (t >= (AGD ? HERAD_NUM_VOICES * 2 : HERAD_NUM_VOICES)) {
            track[t].pos = track[t].size;
            return;
        }

        int status = track[t].data[track[t].pos++] & 0xff;
        if (status == 0xFF) {
            track[t].pos = track[t].size;
        } else {
            int cmd = status & 0xF0;
            int note, par;
            switch (cmd) {
                case 0x80 -> {
                    note = track[t].data[track[t].pos++] & 0xff;
                    par = (v2 ? 0 : (track[t].data[track[t].pos++] & 0xff));
                    ev_noteOff(t, note, par);
                }
                case 0x90 -> {
                    note = track[t].data[track[t].pos++] & 0xff;
                    par = track[t].data[track[t].pos++] & 0xff;
                    ev_noteOn(t, note, par);
                }
                case 0xA0, 0xB0 -> track[t].pos += 2;
                case 0xC0 -> {
                    par = track[t].data[track[t].pos++] & 0xff;
                    ev_programChange(t, par);
                }
                case 0xD0 -> {
                    par = track[t].data[track[t].pos++] & 0xff;
                    ev_aftertouch(t, par);
                }
                case 0xE0 -> {
                    par = track[t].data[track[t].pos++] & 0xff;
                    ev_pitchBend(t, par);
                }
                default -> track[t].pos = track[t].size;
            }
        }
    }

    private void writeReg(int c, int reg, int val) {
        write(c >= HERAD_NUM_VOICES ? 1 : 0, reg, val);
    }

    private void ev_noteOn(int ch, int note, int vel) {
        if (chn[ch].keyon) {
            chn[ch].keyon = false;
            playNote(ch, chn[ch].note, HERAD_NOTE_OFF);
        }
        if (v2 && inst[chn[ch].program].mode() == HERAD_INSTMODE_KMAP) {
            int mp = note - (inst[chn[ch].program].keymap_offset() + 24);
            if (mp < 0 || mp >= HERAD_INST_SIZE - 4) return;
            chn[ch].playprog = inst[chn[ch].program].keymap_index(mp);
            changeProgram(ch, chn[ch].playprog);
        }
        chn[ch].note = note;
        chn[ch].keyon = true;
        chn[ch].bend = HERAD_BEND_CENTER;
        if (v2 && inst[chn[ch].playprog].mode() == HERAD_INSTMODE_KMAP) return;
        playNote(ch, note, HERAD_NOTE_ON);
        int macro = inst[chn[ch].playprog].mc_mod_out_vel();
        if (macro != 0) macroModOutput(ch, chn[ch].playprog, macro, vel);
        macro = inst[chn[ch].playprog].mc_car_out_vel();
        if (macro != 0) macroCarOutput(ch, chn[ch].playprog, macro, vel);
        macro = inst[chn[ch].playprog].mc_fb_vel();
        if (macro != 0) macroFeedback(ch, chn[ch].playprog, macro, vel);
    }

    private void ev_noteOff(int ch, int note, int vel) {
        if (note != chn[ch].note || !chn[ch].keyon) return;
        chn[ch].keyon = false;
        playNote(ch, note, HERAD_NOTE_OFF);
    }

    private void ev_programChange(int ch, int prog) {
        if (prog >= nInsts) return;
        chn[ch].program = prog;
        chn[ch].playprog = prog;
        changeProgram(ch, prog);
    }

    private void ev_aftertouch(int ch, int vel) {
        if (v2) return;
        int macro = inst[chn[ch].playprog].mc_mod_out_at();
        if (macro != 0) macroModOutput(ch, chn[ch].playprog, macro, vel);
        macro = inst[chn[ch].playprog].mc_car_out_at();
        if (macro != 0 && inst[chn[ch].playprog].mc_car_out_vel() != 0) {
            macroCarOutput(ch, chn[ch].playprog, macro, vel);
        }
        macro = inst[chn[ch].playprog].mc_fb_at();
        if (macro != 0) macroFeedback(ch, chn[ch].playprog, macro, vel);
    }

    private void ev_pitchBend(int ch, int bend) {
        chn[ch].bend = bend;
        if (chn[ch].keyon) {
            playNote(ch, chn[ch].note, HERAD_NOTE_UPDATE);
        }
    }

    private void playNote(int c, int note, int state) {
        if (inst[chn[c].playprog].mc_transpose() != 0) {
            note = macroTranspose(note, chn[c].playprog);
        }
        note = (note - 24) & 0xFF;
        if (state != HERAD_NOTE_UPDATE && note >= 0x60) {
            note = 0;
        }
        int oct = note / HERAD_NUM_NOTES;
        int key = note % HERAD_NUM_NOTES;
        if (state != HERAD_NOTE_UPDATE && inst[chn[c].playprog].mc_slide_dur() != 0) {
            chn[c].slide_dur = (state == HERAD_NOTE_ON ? inst[chn[c].playprog].mc_slide_dur() : 0);
        }
        int bend = chn[c].bend;
        int amount, detune = 0;
        int amount_lo, amount_hi;
        if ((inst[chn[c].playprog].mc_slide_coarse() & 1) == 0) {
            if (bend - HERAD_BEND_CENTER < 0) {
                amount = HERAD_BEND_CENTER - bend;
                amount_lo = (amount >> 5);
                amount_hi = (amount << 3) & 0xFF;
                key -= amount_lo;
                if (key < 0) {
                    key += HERAD_NUM_NOTES;
                    oct--;
                }
                if (oct < 0) {
                    key = 0;
                    oct = 0;
                }
                detune = -1 * ((fine_bend[key] * amount_hi) >> 8);
            } else {
                amount = bend - HERAD_BEND_CENTER;
                amount_lo = (amount >> 5);
                amount_hi = (amount << 3) & 0xFF;
                key += amount_lo;
                if (key >= HERAD_NUM_NOTES) {
                    key -= HERAD_NUM_NOTES;
                    oct++;
                }
                detune = (fine_bend[key + 1] * amount_hi) >> 8;
            }
        } else {
            int offset;
            if (bend - HERAD_BEND_CENTER < 0) {
                amount = HERAD_BEND_CENTER - bend;
                key -= amount / 5;
                if (key < 0) {
                    key += HERAD_NUM_NOTES;
                    oct--;
                }
                if (oct < 0) {
                    key = 0;
                    oct = 0;
                }
                offset = (amount % 5) + (key >= 6 ? 5 : 0);
                detune = -1 * coarse_bend[offset];
            } else {
                amount = bend - HERAD_BEND_CENTER;
                key += amount / 5;
                if (key >= HERAD_NUM_NOTES) {
                    key -= HERAD_NUM_NOTES;
                    oct++;
                }
                offset = (amount % 5) + (key >= 6 ? 5 : 0);
                detune = coarse_bend[offset];
            }
        }
        setFreq(c, oct, FNum[key] + detune, state != HERAD_NOTE_OFF);
    }

    private void setFreq(int c, int oct, int freq, boolean on) {
        int reg = 0xA0 + (c % HERAD_NUM_VOICES);
        int val = freq & 0xFF;
        writeReg(c, reg, val);

        reg = 0xB0 + (c % HERAD_NUM_VOICES);
        val = ((freq >> 8) & 3) | ((oct & 7) << 2) | ((on ? 1 : 0) << 5);
        writeReg(c, reg, val);
    }

    private void changeProgram(int c, int i) {
        if (v2 && inst[i].mode() == HERAD_INSTMODE_KMAP) return;

        int reg = 0x20 + slot_offset[c % HERAD_NUM_VOICES];
        int val = (inst[i].mod_mul() & 15) |
                  ((inst[i].mod_ksr() & 1) << 4) |
                  ((inst[i].mod_eg() > 0 ? 1 : 0) << 5) |
                  ((inst[i].mod_vib() & 1) << 6) |
                  ((inst[i].mod_am() & 1) << 7);
        writeReg(c, reg, val);

        reg += 3;
        val = (inst[i].car_mul() & 15) |
              ((inst[i].car_ksr() & 1) << 4) |
              ((inst[i].car_eg() > 0 ? 1 : 0) << 5) |
              ((inst[i].car_vib() & 1) << 6) |
              ((inst[i].car_am() & 1) << 7);
        writeReg(c, reg, val);

        reg = 0x40 + slot_offset[c % HERAD_NUM_VOICES];
        val = (inst[i].mod_out() & 63) | ((inst[i].mod_ksl() & 3) << 6);
        writeReg(c, reg, val);

        reg += 3;
        val = (inst[i].car_out() & 63) | ((inst[i].car_ksl() & 3) << 6);
        writeReg(c, reg, val);

        reg = 0x60 + slot_offset[c % HERAD_NUM_VOICES];
        val = (inst[i].mod_D() & 15) | ((inst[i].mod_A() & 15) << 4);
        writeReg(c, reg, val);

        reg += 3;
        val = (inst[i].car_D() & 15) | ((inst[i].car_A() & 15) << 4);
        writeReg(c, reg, val);

        reg = 0x80 + slot_offset[c % HERAD_NUM_VOICES];
        val = (inst[i].mod_R() & 15) | ((inst[i].mod_S() & 15) << 4);
        writeReg(c, reg, val);

        reg += 3;
        val = (inst[i].car_R() & 15) | ((inst[i].car_S() & 15) << 4);
        writeReg(c, reg, val);

        reg = 0xC0 + (c % HERAD_NUM_VOICES);
        val = (inst[i].con() > 0 ? 0 : 1) |
              ((inst[i].feedback() & 7) << 1) |
              ((AGD ? (inst[i].pan() == 0 || inst[i].pan() > 3 ? 3 : inst[i].pan()) : 0) << 4);
        writeReg(c, reg, val);

        reg = 0xE0 + slot_offset[c % HERAD_NUM_VOICES];
        val = inst[i].mod_wave() & (AGD ? 7 : 3);
        writeReg(c, reg, val);

        reg += 3;
        val = inst[i].car_wave() & (AGD ? 7 : 3);
        writeReg(c, reg, val);
    }

    private void macroModOutput(int c, int i, int sens, int level) {
        if (sens < -4 || sens > 4) return;

        int output;
        if (sens < 0) {
            output = (level >> (sens + 4) > 63 ? 63 : level >> (sens + 4));
        } else {
            output = ((0x80 - level) >> (4 - sens) > 63 ? 63 : (0x80 - level) >> (4 - sens));
        }
        output += inst[i].mod_out();
        if (output > 63) output = 63;

        int reg = 0x40 + slot_offset[c % HERAD_NUM_VOICES];
        int val = (output & 63) | ((inst[i].mod_ksl() & 3) << 6);
        writeReg(c, reg, val);
    }

    private void macroCarOutput(int c, int i, int sens, int level) {
        if (sens < -4 || sens > 4) return;

        int output;
        if (sens < 0) {
            output = (level >> (sens + 4) > 63 ? 63 : level >> (sens + 4));
        } else {
            output = ((0x80 - level) >> (4 - sens) > 63 ? 63 : (0x80 - level) >> (4 - sens));
        }
        output += inst[i].car_out();
        if (output > 63) output = 63;

        int reg = 0x43 + slot_offset[c % HERAD_NUM_VOICES];
        int val = (output & 63) | ((inst[i].car_ksl() & 3) << 6);
        writeReg(c, reg, val);
    }

    private void macroFeedback(int c, int i, int sens, int level) {
        if (sens < -6 || sens > 6) return;

        int feedback;
        if (sens < 0) {
            feedback = (level >> (sens + 7) > 7 ? 7 : level >> (sens + 7));
        } else {
            feedback = ((0x80 - level) >> (7 - sens) > 7 ? 7 : (0x80 - level) >> (7 - sens));
        }
        feedback += inst[i].feedback();
        if (feedback > 7) feedback = 7;

        int reg = 0xC0 + (c % HERAD_NUM_VOICES);
        int val = (inst[i].con() > 0 ? 0 : 1) |
                  ((feedback & 7) << 1) |
                  ((AGD ? (inst[i].pan() == 0 || inst[i].pan() > 3 ? 3 : inst[i].pan()) : 0) << 4);
        writeReg(c, reg, val);
    }

    private int macroTranspose(int note, int i) {
        int tran = inst[i].mc_transpose();
        int diff = (tran - 0x31) & 0xFF;
        if (v2 && diff < 0x60) {
            return (diff + 0x18) & 0xFF;
        } else {
            return (note + tran) & 0xFF;
        }
    }

    private void macroSlide(int c) {
        if (chn[c].slide_dur == 0) return;

        chn[c].slide_dur--;
        chn[c].bend += inst[chn[c].playprog].mc_slide_range();

        if ((chn[c].note & 0x7F) == 0) return;
        playNote(c, chn[c].note, HERAD_NOTE_UPDATE);
    }

    private void processEvents() {
        songend = true;

        for (int i = 0; i < nTracks; i++) {
            if (chn[i].slide_dur > 0 && chn[i].keyon) {
                macroSlide(i);
            }
            if (track[i].pos >= track[i].size) {
                continue;
            }
            songend = false;
            if (track[i].counter == 0) {
                boolean first = (track[i].pos == 0);
                track[i].ticks = GetTicks(i);
                if (first && track[i].ticks != 0) {
                    track[i].ticks++;
                }
            }
            track[i].counter++;
            if (track[i].counter >= track[i].ticks) {
                track[i].counter = 0;
                while (track[i].pos < track[i].size) {
                    executeCommand(i);
                    if (track[i].pos >= track[i].size) {
                        break;
                    } else if (track[i].data[track[i].pos] == 0) {
                        track[i].pos++;
                    } else {
                        break;
                    }
                }
            } else if (track[i].ticks >= 0x8000) {
                track[i].pos = track[i].size;
                track[i].counter = track[i].ticks;
            }
        }
        if (!songend) {
            ticks_pos++;
        }
    }

    @Override
    public boolean update() throws IOException {
        wTime = wTime - 256;
        if (wTime < 0) {
            wTime = wTime + wSpeed;
            processEvents();
        }
        return !songend;
    }

    private static int HSQ_decompress(byte[] data, int size, byte[] out) {
        long queue = 1;
        int out_size = u16(data, 0);
        int src = 6;
        int dst = 0;

        while (true) {
            if (queue == 1) {
                queue = u16(data, src) | 0x10000L;
                src += 2;
            }
            int bit = (int) (queue & 1);
            queue >>= 1;

            if (bit != 0) {
                out[dst++] = data[src++];
            } else {
                if (queue == 1) {
                    queue = u16(data, src) | 0x10000L;
                    src += 2;
                }
                bit = (int) (queue & 1);
                queue >>= 1;

                int count;
                int offset;
                if (bit != 0) {
                    count = u16(data, src);
                    offset = (count >> 3) - 8192;
                    count &= 7;
                    src += 2;
                    if (count == 0) {
                        count = data[src++] & 0xff;
                    }
                    if (count == 0) {
                        break;
                    }
                } else {
                    if (queue == 1) {
                        queue = u16(data, src) | 0x10000L;
                        src += 2;
                    }
                    bit = (int) (queue & 1);
                    queue >>= 1;
                    count = bit << 1;
                    if (queue == 1) {
                        queue = u16(data, src) | 0x10000L;
                        src += 2;
                    }
                    bit = (int) (queue & 1);
                    queue >>= 1;
                    count += bit;

                    offset = data[src++] & 0xff;
                    offset -= 256;
                }
                count += 2;
                while (count-- > 0) {
                    out[dst] = out[dst + offset];
                    dst++;
                }
            }
        }
        return out_size;
    }

    private static int SQX_decompress(byte[] data, int size, byte[] out) {
        int src = 0;
        int dst = 0;
        boolean done = false;

        // C++ memcpy(dst, src, sizeof(uint16_t)) does NOT advance dst
        out[0] = data[0];
        out[1] = data[1];
        src = 6; // src += 6 in C++

        int queue = 1;
        int bit, bit_p;
        while (true) {
            bit = queue & 1;
            queue >>= 1;
            if (queue == 0) {
                queue = u16(data, src);
                src += 2;
                bit_p = bit;
                bit = queue & 1;
                queue >>= 1;
                if (bit_p != 0) {
                    queue |= 0x8000;
                }
            }

            if (bit == 0) {
                int mode = data[2] & 0xff;
                int count;
                int offset;
                switch (mode) {
                    case 0 -> out[dst++] = data[src++];
                    case 1 -> {
                        count = 0;
                        bit = queue & 1;
                        queue >>= 1;
                        if (queue == 0) {
                            queue = u16(data, src);
                            src += 2;
                            bit_p = bit;
                            bit = queue & 1;
                            queue >>= 1;
                            if (bit_p != 0) {
                                queue |= 0x8000;
                            }
                            count = bit;
                            bit = queue & 1;
                            queue >>= 1;
                        } else {
                            count = bit;
                            bit = queue & 1;
                            queue >>= 1;
                            if (queue == 0) {
                                queue = u16(data, src);
                                src += 2;
                                bit_p = bit;
                                bit = queue & 1;
                                queue >>= 1;
                                if (bit_p != 0) {
                                    queue |= 0x8000;
                                }
                            }
                        }
                        count = (count << 1) | bit;
                        offset = data[src++] & 0xff;
                        offset -= 256;
                        count += 2;
                        while (count-- > 0) {
                            out[dst] = out[dst + offset];
                            dst++;
                        }
                    }
                    case 2 -> {
                        count = u16(data, src);
                        int shift = data[5] & 0xff;
                        offset = (count >> shift) - (1 << (16 - shift));
                        count &= (1 << shift) - 1;
                        src += 2;
                        if (count == 0) {
                            count = data[src++] & 0xff;
                        }
                        if (count == 0) {
                            done = true;
                            break;
                        }
                        count += 2;
                        while (count-- > 0) {
                            out[dst] = out[dst + offset];
                            dst++;
                        }
                    }
                }
                if (done) break;
                continue;
            } else {
                bit = queue & 1;
                queue >>= 1;
                if (queue == 0) {
                    queue = u16(data, src);
                    src += 2;
                    bit_p = bit;
                    bit = queue & 1;
                    queue >>= 1;
                    if (bit_p != 0) {
                        queue |= 0x8000;
                    }
                }

                if (bit == 0) {
                    int mode = data[3] & 0xff;
                    int count;
                    int offset;
                    switch (mode) {
                        case 0 -> out[dst++] = data[src++];
                        case 1 -> {
                            count = 0;
                            bit = queue & 1;
                            queue >>= 1;
                            if (queue == 0) {
                                queue = u16(data, src);
                                src += 2;
                                bit_p = bit;
                                bit = queue & 1;
                                queue >>= 1;
                                if (bit_p != 0) {
                                    queue |= 0x8000;
                                }
                                count = bit;
                                bit = queue & 1;
                                queue >>= 1;
                            } else {
                                count = bit;
                                bit = queue & 1;
                                queue >>= 1;
                                if (queue == 0) {
                                    queue = u16(data, src);
                                    src += 2;
                                    bit_p = bit;
                                    bit = queue & 1;
                                    queue >>= 1;
                                    if (bit_p != 0) {
                                        queue |= 0x8000;
                                    }
                                }
                            }
                            count = (count << 1) | bit;
                            offset = data[src++] & 0xff;
                            offset -= 256;
                            count += 2;
                            while (count-- > 0) {
                                out[dst] = out[dst + offset];
                                dst++;
                            }
                        }
                        case 2 -> {
                            count = u16(data, src);
                            int shift = data[5] & 0xff;
                            offset = (count >> shift) - (1 << (16 - shift));
                            count &= (1 << shift) - 1;
                            src += 2;
                            if (count == 0) {
                                count = data[src++] & 0xff;
                            }
                            if (count == 0) {
                                done = true;
                                break;
                            }
                            count += 2;
                            while (count-- > 0) {
                                out[dst] = out[dst + offset];
                                dst++;
                            }
                        }
                    }
                    if (done) break;
                    continue;
                } else {
                    int mode = data[4] & 0xff;
                    int count;
                    int offset;
                    switch (mode) {
                        case 0 -> out[dst++] = data[src++];
                        case 1 -> {
                            count = 0;
                            bit = queue & 1;
                            queue >>= 1;
                            if (queue == 0) {
                                queue = u16(data, src);
                                src += 2;
                                bit_p = bit;
                                bit = queue & 1;
                                queue >>= 1;
                                if (bit_p != 0) {
                                    queue |= 0x8000;
                                }
                                count = bit;
                                bit = queue & 1;
                                queue >>= 1;
                            } else {
                                count = bit;
                                bit = queue & 1;
                                queue >>= 1;
                                if (queue == 0) {
                                    queue = u16(data, src);
                                    src += 2;
                                    bit_p = bit;
                                    bit = queue & 1;
                                    queue >>= 1;
                                    if (bit_p != 0) {
                                        queue |= 0x8000;
                                    }
                                }
                            }
                            count = (count << 1) | bit;
                            offset = data[src++] & 0xff;
                            offset -= 256;
                            count += 2;
                            while (count-- > 0) {
                                out[dst] = out[dst + offset];
                                dst++;
                            }
                        }
                        case 2 -> {
                            count = u16(data, src);
                            int shift = data[5] & 0xff;
                            offset = (count >> shift) - (1 << (16 - shift));
                            count &= (1 << shift) - 1;
                            src += 2;
                            if (count == 0) {
                                count = data[src++] & 0xff;
                            }
                            if (count == 0) {
                                done = true;
                                break;
                            }
                            count += 2;
                            while (count-- > 0) {
                                out[dst] = out[dst + offset];
                                dst++;
                            }
                        }
                    }
                    if (done) break;
                    continue;
                }
            }
        }
        return dst;
    }

    private int getpatterns() {
        return total_ticks / HERAD_MEASURE_TICKS + (total_ticks % HERAD_MEASURE_TICKS != 0 ? 1 : 0);
    }

    @Override
    public float getRefresh() {
        return 200.299f;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }
}
