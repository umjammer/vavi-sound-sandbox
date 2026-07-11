/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2003 Simon Peter, <dn.tlp@gmx.net>, et al.
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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * XAD shell player.
 * Ported from adplug's xad.cpp / xad.h by Riven the Mage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class XadPlayer extends Opl3Player {

    public static class XadHeader {
        public int id;
        public String title = "";
        public String author = "";
        public int fmt;
        public int speed;
        public int reservedA;
    }

    protected XadHeader xad = new XadHeader();
    protected byte[] tune;
    protected int tuneSize;

    // Player state
    protected int plrPlaying;
    protected int plrLooping;
    protected int plrSpeed;
    protected int plrSpeedCounter;

    protected final int[] adlib = new int[256];

    private XadSubPlayer delegate;

    public void oplWrite(int reg, int val) {
        adlib[reg & 0xff] = val;
        write(0, reg, val);
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("XAD Shell Format", "xad");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("XAD");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(80);
            byte[] header = new byte[80];
            int r = bitStream.read(header);
            if (r >= 4) {
                int id = (header[0] & 0xff) | ((header[1] & 0xff) << 8) | ((header[2] & 0xff) << 16) | ((header[3] & 0xff) << 24);
                if (id == 0x21444158) { // "XAD!"
                    return true;
                }
                if ((id & 0xffffff) == 0x464d42) { // "BMF"
                    return true;
                }
            }
        } catch (IOException ignored) {
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] allBytes = is.readAllBytes();
        if (allBytes.length < 4) {
            throw new IOException("File too short");
        }

        int id = (allBytes[0] & 0xff) | ((allBytes[1] & 0xff) << 8) | ((allBytes[2] & 0xff) << 16) | ((allBytes[3] & 0xff) << 24);
        boolean xadHeader = true;

        if (id != 0x21444158) {
            if ((id & 0xffffff) == 0x464d42) {
                xadHeader = false;
                xad.fmt = 4; // BMF
            } else {
                throw new IOException("Invalid XAD header signature");
            }
        }

        if (!xadHeader) {
            xad.id = id;
            xad.title = "";
            xad.author = "";
            xad.speed = 0;
            xad.reservedA = 0;
            tuneSize = allBytes.length;
            tune = allBytes;
        } else {
            if (allBytes.length < 80) {
                throw new IOException("File too short for XAD header");
            }
            xad.id = id;
            xad.title = readString(allBytes, 4, 36);
            xad.author = readString(allBytes, 40, 36);
            xad.fmt = (allBytes[76] & 0xff) | ((allBytes[77] & 0xff) << 8);
            xad.speed = allBytes[78] & 0xff;
            xad.reservedA = allBytes[79] & 0xff;

            tuneSize = allBytes.length - 80;
            tune = new byte[tuneSize];
            System.arraycopy(allBytes, 80, tune, 0, tuneSize);
        }

        switch (xad.fmt) {
            case 1 -> delegate = new XadhypPlayer(this);
            case 2 -> delegate = new XadpsiPlayer(this);
            case 3 -> delegate = new XadflashPlayer(this);
            case 4 -> delegate = new XadbmfPlayer(this);
            case 5 -> delegate = new XadratPlayer(this);
            case 6 -> delegate = new XadhybridPlayer(this);
            default -> throw new IOException("Unsupported XAD format type: " + xad.fmt);
        }

        boolean ret = delegate.load();
        if (!ret) {
            throw new IOException("Failed to load sub-player format data");
        }

        rewind(0);
    }

    @Override
    public boolean update() throws IOException {
        if (delegate == null) {
            return false;
        }

        if (--plrSpeedCounter > 0) {
            return plrPlaying != 0 && plrLooping == 0;
        }

        plrSpeedCounter = plrSpeed;

        delegate.update();

        return plrPlaying != 0 && plrLooping == 0;
    }

    @Override
    public void rewind(int subSong) throws IOException {
        for (int i = 0; i < 256; ++i) {
            write(0, i, 0);
            write(1, i, 0);
        }
        Arrays.fill(adlib, 0);

        plrSpeed = xad.speed;
        plrSpeedCounter = 1;
        plrPlaying = 1;
        plrLooping = 0;

        if (delegate != null) {
            delegate.rewind(subSong);
        }
    }

    @Override
    public float getRefresh() {
        return delegate != null ? delegate.getRefresh() : 50.0f;
    }

    public String getTitle() {
        return delegate != null ? delegate.getTitle() : xad.title;
    }

    public String getAuthor() {
        return delegate != null ? delegate.getAuthor() : xad.author;
    }

    public int getInstruments() {
        return delegate != null ? delegate.getInstruments() : 0;
    }

    public String getInstrument(int i) {
        return delegate != null ? delegate.getInstrument(i) : "";
    }

    public int getSpeed() {
        return delegate != null ? delegate.getSpeed() : 0;
    }

    protected String readString(byte[] buf, int offset, int length) {
        int len = 0;
        while (len < length && offset + len < buf.length && buf[offset + len] != 0) {
            len++;
        }
        return new String(buf, offset, len, StandardCharsets.US_ASCII);
    }
}

abstract class XadSubPlayer {
    protected final XadPlayer parent;

    protected XadSubPlayer(XadPlayer parent) {
        this.parent = parent;
    }

    public abstract boolean load() throws IOException;
    public abstract void rewind(int subSong) throws IOException;
    public abstract void update() throws IOException;
    public abstract float getRefresh();
    public abstract String getType();

    public String getTitle() {
        return parent.xad.title;
    }

    public String getAuthor() {
        return parent.xad.author;
    }

    public String getInstrument(int i) {
        return "";
    }

    public int getInstruments() {
        return 0;
    }

    public int getSpeed() {
        return 0;
    }

    protected int readLE16(byte[] buf, int offset) {
        return (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
    }

    protected int strnlen(byte[] array, int offset, int maxlen) {
        int len = 0;
        while (len < maxlen && offset + len < array.length && array[offset + len] != 0) {
            len++;
        }
        return len;
    }
}

class XadhypPlayer extends XadSubPlayer {

    private static final int[] HYP_ADLIB_REGISTERS = {
        0x20, 0x23, 0x40, 0x43, 0x60, 0x63, 0x80, 0x83, 0xA0, 0xB0, 0xC0,
        0x21, 0x24, 0x41, 0x44, 0x61, 0x64, 0x81, 0x84, 0xA1, 0xB1, 0xC1,
        0x22, 0x25, 0x42, 0x45, 0x62, 0x65, 0x82, 0x85, 0xA2, 0xB2, 0xC2,
        0x28, 0x2B, 0x48, 0x4B, 0x68, 0x6B, 0x88, 0x8B, 0xA3, 0xB3, 0xC3,
        0x29, 0x2C, 0x49, 0x4C, 0x69, 0x6C, 0x89, 0x8C, 0xA4, 0xB4, 0xC4,
        0x2A, 0x2D, 0x4A, 0x4D, 0x6A, 0x6D, 0x8A, 0x8D, 0xA5, 0xB5, 0xC5,
        0x30, 0x33, 0x50, 0x53, 0x70, 0x73, 0x90, 0x93, 0xA6, 0xB6, 0xC6,
        0x31, 0x34, 0x51, 0x54, 0x71, 0x74, 0x91, 0x94, 0xA7, 0xB7, 0xC7,
        0x32, 0x35, 0x52, 0x55, 0x72, 0x75, 0x92, 0x95, 0xA8, 0xB8, 0xC8
    };

    private static final int[] HYP_NOTES = {
        0x0000,
        0x0956, 0x096B, 0x0980, 0x0998, 0x09B1, 0x09C9, 0x09E5, 0x0A03, 0x0A21,
        0x0A41, 0x0A63, 0x0A86, 0x0D56, 0x0D6B, 0x0D80, 0x0D98, 0x0DB1, 0x0DC9,
        0x0DE5, 0x0E03, 0x0E21, 0x0E41, 0x0E63, 0x0E86, 0x1156, 0x116B, 0x1180,
        0x1198, 0x11B1, 0x11C9, 0x11E5, 0x1203, 0x1221, 0x1241, 0x1263, 0x1286,
        0x1556, 0x156B, 0x1580, 0x1598, 0x15B1, 0x15C9, 0x15E5, 0x1603, 0x1621,
        0x1641, 0x1663, 0x1686, 0x1956, 0x196B, 0x1980, 0x1998, 0x19B1, 0x19C9,
        0x19E5, 0x1A03, 0x1A21, 0x1A41, 0x1A63, 0x1A86, 0x1D56, 0x1D6B, 0x1D80,
        0x1D98, 0x1DB1, 0x1DC9, 0x1DE5, 0x1E03, 0x1E21, 0x1E41, 0x1E63, 0x1E86
    };

    private int hypPointer;

    public XadhypPlayer(XadPlayer parent) {
        super(parent);
    }

    @Override
    public boolean load() {
        return parent.xad.fmt == 1 && parent.tuneSize >= 0x69 + 9;
    }

    @Override
    public void rewind(int subSong) {
        parent.plrSpeed = parent.tune[5] & 0xff;
        parent.oplWrite(0xBD, 0xC0);

        for (int i = 0; i < 9; i++) {
            parent.adlib[0xB0 + i] = 0;
        }

        for (int i = 0; i < 99; i++) {
            parent.oplWrite(HYP_ADLIB_REGISTERS[i], parent.tune[6 + i] & 0xff);
        }

        hypPointer = 0x69;
    }

    @Override
    public void update() {
        for (int i = 0; i < 9; i++) {
            int event = parent.tune[hypPointer++] & 0xff;
            if (event != 0) {
                int freq = HYP_NOTES[event & 0x3F];
                int lofreq = freq & 0xFF;
                int hifreq = (freq >> 8) & 0xFF;

                parent.oplWrite(0xB0 + i, parent.adlib[0xB0 + i]);

                if ((event & 0x40) == 0) {
                    parent.oplWrite(0xA0 + i, lofreq);
                    parent.oplWrite(0xB0 + i, hifreq | 0x20);
                }

                parent.adlib[0xB0 + i] &= 0xDF;
            }
        }

        hypPointer += 3;

        if (hypPointer > parent.tuneSize - 9) {
            hypPointer = 0x69;
            parent.plrLooping = 1;
        }
    }

    @Override
    public float getRefresh() {
        return 60.0f;
    }

    @Override
    public String getType() {
        return "xad: hypnosis player";
    }
}

class XadpsiPlayer extends XadSubPlayer {

    private static final int[][] PSI_ADLIB_REGISTERS = {
        { 0x20, 0x23, 0x40, 0x43, 0x60, 0x63, 0x80, 0x83, 0xE0, 0xE3, 0xC0 },
        { 0x21, 0x24, 0x41, 0x44, 0x61, 0x64, 0x81, 0x84, 0xE1, 0xE4, 0xC1 },
        { 0x22, 0x25, 0x42, 0x45, 0x62, 0x65, 0x82, 0x85, 0xE2, 0xE5, 0xC2 },
        { 0x28, 0x2B, 0x48, 0x4B, 0x68, 0x6B, 0x88, 0x8B, 0xE8, 0xEB, 0xC3 },
        { 0x29, 0x2C, 0x49, 0x4C, 0x69, 0x6C, 0x89, 0x8C, 0xE9, 0xEC, 0xC4 },
        { 0x2A, 0x2D, 0x4A, 0x4D, 0x6A, 0x6D, 0x8A, 0x8D, 0xEA, 0xED, 0xC5 },
        { 0x30, 0x33, 0x50, 0x53, 0x70, 0x73, 0x90, 0x93, 0xF0, 0xF3, 0xC6 },
        { 0x31, 0x34, 0x51, 0x54, 0x71, 0x74, 0x91, 0x94, 0xF1, 0xF4, 0xC7 },
        { 0x32, 0x35, 0x52, 0x55, 0x72, 0x75, 0x92, 0x95, 0xF2, 0xF5, 0xC8 }
    };

    private static class Note {
        int hi, lo;
        Note(int hi, int lo) {
            this.hi = hi;
            this.lo = lo;
        }
    }

    private static final Note[] PSI_NOTES = {
        new Note(0x21, 0x6B), new Note(0x21, 0x81), new Note(0x21, 0x98), new Note(0x21, 0xB0),
        new Note(0x21, 0xCA), new Note(0x21, 0xE5), new Note(0x22, 0x02), new Note(0x22, 0x20),
        new Note(0x22, 0x41), new Note(0x22, 0x63), new Note(0x22, 0x87), new Note(0x23, 0x64),
    };

    private int instrPtr;
    private int seqPtr;

    private final int[] psiNoteDelay = new int[8];
    private final int[] psiNoteCurDelay = new int[8];
    private final int[] psiPtr = new int[8];
    private int psiLooping;

    public XadpsiPlayer(XadPlayer parent) {
        super(parent);
    }

    @Override
    public boolean load() {
        if (parent.xad.fmt != 2 || parent.tuneSize < 4) {
            return false;
        }

        instrPtr = readLE16(parent.tune, 0);
        seqPtr = readLE16(parent.tune, 2);

        if (instrPtr + 8 * 2 >= parent.tuneSize || seqPtr + 8 * 4 >= parent.tuneSize) {
            return false;
        }

        for (int i = 0; i < 8 * 2; i += 2) {
            if (readLE16(parent.tune, instrPtr + i) + 11 >= parent.tuneSize) {
                return false;
            }
        }
        for (int i = 0; i < 8 * 4; i += 2) {
            if (readLE16(parent.tune, seqPtr + i) >= parent.tuneSize) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void rewind(int subSong) {
        parent.oplWrite(0x01, 0x20);
        parent.oplWrite(0x08, 0x00);
        parent.oplWrite(0xBD, 0x00);

        for (int i = 0; i < 8; i++) {
            int inspos = readLE16(parent.tune, instrPtr + i * 2);
            for (int j = 0; j < 11; j++) {
                parent.oplWrite(PSI_ADLIB_REGISTERS[i][j], parent.tune[inspos + j] & 0xff);
            }

            parent.oplWrite(0xA0 + i, 0x00);
            parent.oplWrite(0xB0 + i, 0x00);

            psiPtr[i] = readLE16(parent.tune, seqPtr + i * 4);

            psiNoteDelay[i] = 1;
            psiNoteCurDelay[i] = 1;
        }
        psiLooping = 0;
    }

    @Override
    public void update() {
        for (int i = 0; i < 8; i++) {
            psiNoteCurDelay[i]--;
            if (psiNoteCurDelay[i] > 0) {
                continue;
            }

            parent.oplWrite(0xA0 + i, 0x00);
            parent.oplWrite(0xB0 + i, 0x00);

            int ptr = psiPtr[i];
            int event = ptr < parent.tuneSize ? (parent.tune[ptr++] & 0xff) : 0;
            psiPtr[i] = ptr;

            if (event == 0) {
                ptr = readLE16(parent.tune, seqPtr + i * 4 + 2);
                event = parent.tune[ptr++] & 0xff;
                psiPtr[i] = ptr;

                psiLooping |= 1 << i;
                parent.plrLooping = (psiLooping == (1 << 8) - 1) ? 1 : 0;
            }

            if ((event & 0x80) != 0) {
                psiNoteDelay[i] = event & 0x7F;

                ptr = psiPtr[i];
                event = ptr < parent.tuneSize ? (parent.tune[ptr++] & 0xff) : 0;
                psiPtr[i] = ptr;
            }

            psiNoteCurDelay[i] = psiNoteDelay[i];

            int noteIdx = event & 0x0F;
            Note note = (noteIdx < PSI_NOTES.length) ? PSI_NOTES[noteIdx] : new Note(0, 0);

            parent.oplWrite(0xA0 + i, note.lo);
            parent.oplWrite(0xB0 + i, note.hi + ((event & 0xF0) >> 2));
        }
    }

    @Override
    public float getRefresh() {
        return 70.0f;
    }

    @Override
    public String getType() {
        return "xad: psi player";
    }

    @Override
    public int getInstruments() {
        return 8;
    }
}

class XadflashPlayer extends XadSubPlayer {

    private static final int[][] FLASH_ADLIB_REGISTERS = {
        { 0x23, 0x20, 0x43, 0x40, 0x63, 0x60, 0x83, 0x80, 0xC0, 0xE3, 0xE0 },
        { 0x24, 0x21, 0x44, 0x41, 0x64, 0x61, 0x84, 0x81, 0xC1, 0xE4, 0xE1 },
        { 0x25, 0x22, 0x45, 0x42, 0x65, 0x62, 0x85, 0x82, 0xC2, 0xE5, 0xE2 },
        { 0x2B, 0x28, 0x4B, 0x48, 0x6B, 0x68, 0x8B, 0x88, 0xC3, 0xEB, 0xE8 },
        { 0x2C, 0x29, 0x4C, 0x49, 0x6C, 0x69, 0x8C, 0x89, 0xC4, 0xEC, 0xE9 },
        { 0x2D, 0x2A, 0x4D, 0x4A, 0x6D, 0x6A, 0x8D, 0x8A, 0xC5, 0xED, 0xEA },
        { 0x33, 0x30, 0x53, 0x50, 0x73, 0x70, 0x93, 0x90, 0xC6, 0xF3, 0xF0 },
        { 0x34, 0x31, 0x54, 0x51, 0x74, 0x71, 0x94, 0x91, 0xC7, 0xF4, 0xF1 },
        { 0x35, 0x32, 0x55, 0x52, 0x75, 0x72, 0x95, 0x92, 0xC8, 0xF5, 0xF2 }
    };

    private static final int[] FLASH_NOTES = {
        0x157, 0x16B, 0x181, 0x198, 0x1B0, 0x1CA,
        0x1E5, 0x202, 0x220, 0x241, 0x263, 0x287
    };

    private int flashOrderPos;
    private int flashPatternPos;

    public XadflashPlayer(XadPlayer parent) {
        super(parent);
    }

    @Override
    public boolean load() {
        return parent.xad.fmt == 3;
    }

    @Override
    public void rewind(int subSong) {
        parent.plrSpeed = parent.xad.speed;
        flashOrderPos = 0;
        flashPatternPos = 0;

        parent.oplWrite(0x08, 0x00);
        parent.oplWrite(0xBD, 0x00);

        for (int i = 0; i < 9; i++) {
            parent.oplWrite(0xA0 + i, 0x00);
            parent.oplWrite(0xB0 + i, 0x00);
        }

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 11; j++) {
                parent.oplWrite(FLASH_ADLIB_REGISTERS[i][j], parent.tune[i * 12 + j] & 0xff);
            }
        }
    }

    @Override
    public void update() {
        int orderOffset = 0x600;
        int orderPosVal = flashOrderPos;
        int patternPosVal = flashPatternPos;

        int patIdx = parent.tune[orderOffset + orderPosVal] & 0xff;
        int eventPos = 0x633 + patIdx * 1152 + patternPosVal * 18;

        for (int i = 0; i < 9; i++) {
            if (eventPos > parent.tuneSize - 2) {
                flashPatternPos = 0x3F;
                break;
            }

            int eventB0 = parent.tune[eventPos++] & 0xff;
            int eventB1 = parent.tune[eventPos++] & 0xff;

            if (eventB0 == 0x80) {
                if (eventB1 < 0x80) {
                    for (int j = 0; j < 11; j++) {
                        parent.oplWrite(FLASH_ADLIB_REGISTERS[i][j], parent.tune[eventB1 * 12 + j] & 0xff);
                    }
                }
                continue;
            }

            int slideFreq = 0;
            int fx = (eventB1 >> 4) & 0x0F;
            int fxP = eventB1 & 0x0F;

            switch (fx) {
                case 0x0 -> {
                    if (eventB1 == 0x01) {
                        flashPatternPos = 0x3F;
                    }
                }
                case 0x1 -> slideFreq = fxP << 1;
                case 0x2 -> slideFreq = -(fxP << 1);
                case 0xA -> parent.oplWrite(FLASH_ADLIB_REGISTERS[i][2], fxP << 2);
                case 0xB -> parent.oplWrite(FLASH_ADLIB_REGISTERS[i][3], fxP << 2);
                case 0xC -> {
                    parent.oplWrite(FLASH_ADLIB_REGISTERS[i][2], fxP << 2);
                    parent.oplWrite(FLASH_ADLIB_REGISTERS[i][3], fxP << 2);
                }
                case 0xF -> parent.plrSpeed = fxP + 1;
            }

            if (eventB0 != 0) {
                parent.oplWrite(0xA0 + i, parent.adlib[0xA0 + i]);
                parent.oplWrite(0xB0 + i, parent.adlib[0xB0 + i] & 0xDF);

                if (eventB0 != 0x7F) {
                    int note = (eventB0 - 1) % 12;
                    int octave = (eventB0 - 1) / 12;
                    int freq = FLASH_NOTES[note] | (octave << 10) | 0x2000;

                    parent.oplWrite(0xA0 + i, freq & 0xFF);
                    parent.oplWrite(0xB0 + i, freq >> 8);
                }
            }

            if (slideFreq != 0) {
                int freq = (parent.adlib[0xB0 + i] << 8) + parent.adlib[0xA0 + i];
                freq += slideFreq;

                parent.oplWrite(0xA0 + i, freq & 0xFF);
                parent.oplWrite(0xB0 + i, freq >> 8);
            }
        }

        flashPatternPos++;
        if (flashPatternPos >= 0x40) {
            flashPatternPos = 0;
            flashOrderPos++;

            if (flashOrderPos > 0x33 || (parent.tune[orderOffset + flashOrderPos] & 0xff) == 0xFF) {
                flashOrderPos = 0;
                parent.plrLooping = 1;
            }
        }
    }

    @Override
    public float getRefresh() {
        return 17.5f;
    }

    @Override
    public String getType() {
        return "xad: flash player";
    }

    @Override
    public int getInstruments() {
        return 128;
    }
}

class XadbmfPlayer extends XadSubPlayer {

    private static class BmfEvent {
        int note;
        int delay;
        int volume;
        int instrument;
        int cmd;
        int cmd_data;
    }

    private static class BmfInstrument {
        String name = "";
        final int[] data = new int[13];
    }

    private static final int[] BMF_DEFAULT_INSTRUMENT = {
        0x01, 0x01, 0x3F, 0x3F, 0x00, 0x00, 0xF0, 0xF0, 0x00, 0x00, 0x00, 0x00, 0x00
    };

    private static final int[] BMF_ADLIB_REGISTERS = {
        0x20, 0x23, 0x40, 0x43, 0x60, 0x63, 0x80, 0x83, 0xA0, 0xB0, 0xC0, 0xE0, 0xE3,
        0x21, 0x24, 0x41, 0x44, 0x61, 0x64, 0x81, 0x84, 0xA1, 0xB1, 0xC1, 0xE1, 0xE4,
        0x22, 0x25, 0x42, 0x45, 0x62, 0x65, 0x82, 0x85, 0xA2, 0xB2, 0xC2, 0xE2, 0xE5,
        0x28, 0x2B, 0x48, 0x4B, 0x68, 0x6B, 0x88, 0x8B, 0xA3, 0xB3, 0xC3, 0xE8, 0xEB,
        0x29, 0x2C, 0x49, 0x4C, 0x69, 0x6C, 0x89, 0x8C, 0xA4, 0xB4, 0xC4, 0xE9, 0xEC,
        0x2A, 0x2D, 0x4A, 0x4D, 0x6A, 0x6D, 0x8A, 0x8D, 0xA5, 0xB5, 0xC5, 0xEA, 0xED,
        0x30, 0x33, 0x50, 0x53, 0x70, 0x73, 0x90, 0x93, 0xA6, 0xB6, 0xC6, 0xF0, 0xF3,
        0x31, 0x34, 0x51, 0x54, 0x71, 0x74, 0x91, 0x94, 0xA7, 0xB7, 0xC7, 0xF1, 0xF4,
        0x32, 0x35, 0x52, 0x55, 0x72, 0x75, 0x92, 0x95, 0xA8, 0xB8, 0xC8, 0xF2, 0xF5
    };

    private static final int[] BMF_NOTES = {
        0x157, 0x16B, 0x181, 0x198, 0x1B0, 0x1CA,
        0x1E5, 0x202, 0x220, 0x241, 0x263, 0x287
    };

    private static final int[] BMF_NOTES_2 = {
        0x159, 0x16D, 0x183, 0x19A, 0x1B2, 0x1CC,
        0x1E8, 0x205, 0x223, 0x244, 0x267, 0x28B
    };

    private int bmfVersion;
    private float bmfTimer;
    private int bmfSpeed;
    private String bmfTitle = "";
    private String bmfAuthor = "";

    private final BmfInstrument[] bmfInstruments = new BmfInstrument[32];
    private final BmfEvent[][] bmfStreams = new BmfEvent[9][1024];

    private final int[] streamPosition = new int[9];
    private final int[] channelDelay = new int[9];
    private final int[] loopPosition = new int[9];
    private final int[] loopCounter = new int[9];
    private int activeStreams;

    public XadbmfPlayer(XadPlayer parent) {
        super(parent);
        for (int i = 0; i < 32; i++) {
            bmfInstruments[i] = new BmfInstrument();
        }
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 1024; j++) {
                bmfStreams[i][j] = new BmfEvent();
            }
        }
    }

    private boolean startsWith(byte[] array, int offset, String prefix) {
        if (offset + prefix.length() > array.length) {
            return false;
        }
        for (int i = 0; i < prefix.length(); i++) {
            if (array[offset + i] != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private int bmfConvertStream(int startOffset, int channel, int streamSize) {
        int stream = startOffset;
        int streamEnd = startOffset + streamSize;
        int lastPos = 1024 - 1;

        for (int pos = 0; pos <= lastPos; pos++) {
            BmfEvent event = bmfStreams[channel][pos];
            event.note = 0;
            event.delay = 0;
            event.volume = 0;
            event.instrument = 0;
            event.cmd = 0;
            event.cmd_data = 0;

            if (streamEnd - stream < 1) {
                return -1;
            }

            int b = parent.tune[stream] & 0xff;
            switch (b) {
                case 0xFE -> {
                    event.cmd = 0xFF;
                    stream++;
                    pos = lastPos + 1;
                }
                case 0xFC -> {
                    event.cmd = 0xFE;
                    if (streamEnd - stream < 2) {
                        return -1;
                    }
                    int val = parent.tune[stream + 1] & 0xff;
                    int mask = (bmfVersion == 0) ? 0x7F : 0x3F;
                    event.cmd_data = (val & mask) - 1;
                    stream += 2;
                }
                case 0x7D -> {
                    event.cmd = 0xFD;
                    stream++;
                }
                default -> {
                    event.note = b & 0x7F;
                    stream++;
                    if ((b & 0x80) == 0) {
                        break;
                    }

                    if (streamEnd - stream < 1) {
                        return -1;
                    }
                    int b2 = parent.tune[stream] & 0xff;
                    if ((b2 & 0x80) != 0) {
                        event.delay = b2 & 0x3F;
                        stream++;
                        if ((b2 & 0x40) == 0) {
                            break;
                        }
                    }

                    if (streamEnd - stream < 1) {
                        return -1;
                    }

                    int b3 = parent.tune[stream] & 0xff;
                    if (b3 >= 0x40) {
                        event.volume = b3 - 0x40 + 1;
                        stream++;
                    } else if (b3 >= 0x20) {
                        event.instrument = b3 - 0x20 + 1;
                        stream++;
                    } else {
                        if (bmfVersion == 0) {
                            stream++;
                        } else if (bmfVersion == 2) {
                            if (b3 >= 0x01 && b3 <= 0x06) {
                                if (streamEnd - stream < 2) {
                                    return -1;
                                }
                                int nextB = parent.tune[stream + 1] & 0xff;
                                switch (b3) {
                                    case 0x01 -> {
                                        event.cmd = 0x01;
                                        event.cmd_data = nextB;
                                    }
                                    case 0x04 -> {
                                        event.cmd = 0x10;
                                        event.cmd_data = nextB;
                                    }
                                    case 0x05, 0x06 -> {
                                        event.volume = nextB + 1;
                                    }
                                }
                                stream += 2;
                            }
                        }
                    }
                }
            }
        }
        return stream - startOffset;
    }

    @Override
    public boolean load() {
        int ptr = 0;

        if (parent.xad.fmt != 4) {
            return false;
        }

        if (parent.tuneSize < 6) {
            return false;
        }

        if (startsWith(parent.tune, 0, "BMF1.2")) {
            bmfVersion = 2;
            bmfTimer = 70.0f;
            ptr = 6;
        } else if (startsWith(parent.tune, 0, "BMF1.1")) {
            bmfVersion = 1;
            bmfTimer = 68.5f;
            ptr = 6;
        } else {
            bmfVersion = 0;
            bmfTimer = 18.2f;
        }

        if (bmfVersion > 0) {
            int len = strnlen(parent.tune, ptr, parent.tuneSize - ptr);
            if (ptr + len == parent.tuneSize) {
                return false;
            }
            bmfTitle = parent.readString(parent.tune, ptr, len);
            ptr += len + 1;

            len = strnlen(parent.tune, ptr, parent.tuneSize - ptr);
            if (ptr + len == parent.tuneSize) {
                return false;
            }
            bmfAuthor = parent.readString(parent.tune, ptr, len);
            ptr += len + 1;
        } else {
            bmfTitle = parent.xad.title;
            bmfAuthor = parent.xad.author;
        }

        if (parent.tuneSize - ptr < 1) {
            return false;
        }
        bmfSpeed = parent.tune[ptr++] & 0xff;
        if (bmfVersion == 0) {
            bmfSpeed /= 3;
        }

        if (bmfVersion > 0) {
            if (parent.tuneSize - ptr < 4) {
                return false;
            }
            long iflags = ((parent.tune[ptr] & 0xffL) << 24) |
                          ((parent.tune[ptr + 1] & 0xffL) << 16) |
                          ((parent.tune[ptr + 2] & 0xffL) << 8) |
                          (parent.tune[ptr + 3] & 0xffL);
            ptr += 4;

            for (int i = 0; i < 32; i++) {
                if ((iflags & (1L << (31 - i))) != 0) {
                    if (parent.tuneSize - ptr < 24) {
                        return false;
                    }
                    bmfInstruments[i].name = parent.readString(parent.tune, ptr, 10);
                    for (int j = 0; j < 13; j++) {
                        bmfInstruments[i].data[j] = parent.tune[ptr + 11 + j] & 0xff;
                    }
                    ptr += 24;
                } else if (bmfVersion == 1) {
                    bmfInstruments[i].name = "";
                    System.arraycopy(BMF_DEFAULT_INSTRUMENT, 0, bmfInstruments[i].data, 0, 13);
                } else {
                    bmfInstruments[i].name = "";
                    Arrays.fill(bmfInstruments[i].data, 0);
                }
            }
        } else {
            ptr = 6;
            if (parent.tuneSize - ptr < 32 * 15) {
                return false;
            }
            for (int i = 0; i < 32; i++) {
                bmfInstruments[i].name = "";
                Arrays.fill(bmfInstruments[i].data, 0);
            }
            for (int i = 0; i < 32; i++) {
                int ip = ptr + 15 * i;
                int instIdx = parent.tune[ip] & 0xff;
                if (instIdx < 32) {
                    for (int j = 0; j < 13; j++) {
                        bmfInstruments[instIdx].data[j] = parent.tune[ip + 2 + j] & 0xff;
                    }
                } else {
                    break;
                }
            }
            ptr += 32 * 15;
        }

        if (bmfVersion > 0) {
            if (parent.tuneSize - ptr < 4) {
                return false;
            }
            long sflags = ((parent.tune[ptr] & 0xffL) << 24) |
                          ((parent.tune[ptr + 1] & 0xffL) << 16) |
                          ((parent.tune[ptr + 2] & 0xffL) << 8) |
                          (parent.tune[ptr + 3] & 0xffL);
            ptr += 4;

            for (int i = 0; i < 9; i++) {
                if ((sflags & (1L << (31 - i))) != 0) {
                    int len = bmfConvertStream(ptr, i, parent.tuneSize - ptr);
                    if (len < 0) {
                        return false;
                    }
                    ptr += len;
                } else {
                    bmfStreams[i][0].cmd = 0xFF;
                }
            }
        } else {
            int numStreams = parent.tune[5] & 0xff;
            if (numStreams > 9) {
                return false;
            }
            for (int i = 0; i < numStreams; i++) {
                int len = bmfConvertStream(ptr, i, parent.tuneSize - ptr);
                if (len < 0) {
                    return false;
                }
                ptr += len;
            }
            for (int i = numStreams; i < 9; i++) {
                bmfStreams[i][0].cmd = 0xFF;
            }
        }

        return true;
    }

    @Override
    public void rewind(int subSong) {
        Arrays.fill(streamPosition, 0);
        Arrays.fill(channelDelay, 0);
        Arrays.fill(loopPosition, 0);
        Arrays.fill(loopCounter, 0);

        parent.plrSpeed = bmfSpeed;
        activeStreams = 9;

        if (bmfVersion > 0) {
            parent.oplWrite(0x01, 0x20);

            if (bmfVersion == 1) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 13; j++) {
                        parent.oplWrite(BMF_ADLIB_REGISTERS[13 * i + j], BMF_DEFAULT_INSTRUMENT[j]);
                    }
                }
            } else if (bmfVersion == 2) {
                for (int i = 0x20; i < 0x100; i++) {
                    parent.oplWrite(i, 0xFF);
                }
            }
        }

        parent.oplWrite(0x08, 0x00);
        parent.oplWrite(0xBD, 0xC0);
    }

    @Override
    public void update() {
        for (int i = 0; i < 9; i++) {
            int pos = streamPosition[i];
            if (pos == 0xFFFF) {
                continue;
            }

            if (channelDelay[i] > 0) {
                channelDelay[i]--;
                continue;
            }

            boolean loopAgain = true;
            while (loopAgain) {
                loopAgain = false;
                BmfEvent event = bmfStreams[i][pos];

                switch (event.cmd) {
                    case 0xFF -> {
                        pos = 0xFFFF;
                        activeStreams--;
                        continue;
                    }
                    case 0xFE -> {
                        pos++;
                        loopPosition[i] = pos;
                        loopCounter[i] = event.cmd_data;
                        loopAgain = true;
                    }
                    case 0xFD -> {
                        if (loopCounter[i] > 0) {
                            pos = loopPosition[i];
                            loopCounter[i]--;
                        } else {
                            pos++;
                        }
                        loopAgain = true;
                    }
                    case 0x01 -> {
                        int reg = BMF_ADLIB_REGISTERS[13 * i + 2];
                        parent.oplWrite(reg, (parent.adlib[reg] | 0x3F) - event.cmd_data);
                    }
                    case 0x10 -> {
                        parent.plrSpeed = event.cmd_data;
                        parent.plrSpeedCounter = parent.plrSpeed;
                    }
                }
            }

            if (pos == 0xFFFF) {
                streamPosition[i] = pos;
                continue;
            }

            BmfEvent event = bmfStreams[i][pos];

            channelDelay[i] = event.delay;

            if (event.instrument != 0) {
                int ins = event.instrument - 1;
                if (bmfVersion != 1) {
                    parent.oplWrite(0xB0 + i, parent.adlib[0xB0 + i] & 0xDF);
                }
                for (int j = 0; j < 13; j++) {
                    parent.oplWrite(BMF_ADLIB_REGISTERS[13 * i + j], bmfInstruments[ins].data[j]);
                }
            }

            if (event.volume != 0) {
                int vol = event.volume - 1;
                int reg = BMF_ADLIB_REGISTERS[13 * i + 3];
                parent.oplWrite(reg, (parent.adlib[reg] | 0x3F) - vol);
            }

            if (event.note != 0) {
                int note = event.note - 1;
                int freq = 0;

                parent.oplWrite(0xB0 + i, parent.adlib[0xB0 + i] & 0xDF);

                if (bmfVersion == 1) {
                    if (note < 0x60) {
                        freq = BMF_NOTES_2[note % 12];
                    }
                } else {
                    if (note != 0x7E) {
                        freq = BMF_NOTES[note % 12];
                    }
                }

                if (freq != 0) {
                    parent.oplWrite(0xB0 + i, (freq >> 8) | ((note / 12) << 2) | 0x20);
                    parent.oplWrite(0xA0 + i, freq & 0xFF);
                }
            }

            pos++;
            streamPosition[i] = pos;
        }

        if (activeStreams <= 0) {
            for (int j = 0; j < 9; j++) {
                streamPosition[j] = 0;
            }
            activeStreams = 9;
            parent.plrLooping = 1;
        }
    }

    @Override
    public float getRefresh() {
        return bmfTimer;
    }

    @Override
    public String getType() {
        return "xad: BMF Adlib Tracker";
    }

    @Override
    public String getTitle() {
        return bmfTitle;
    }

    @Override
    public String getAuthor() {
        return bmfAuthor;
    }

    @Override
    public int getInstruments() {
        return 32;
    }

    @Override
    public String getInstrument(int i) {
        return bmfInstruments[i].name;
    }

    @Override
    public int getSpeed() {
        return parent.plrSpeed;
    }
}

class XadratPlayer extends XadSubPlayer {

    private static class RatInstrument {
        int modCtrl, modVolume, modAD, modSR, modWave;
        int carCtrl, carVolume, carAD, carSR, carWave;
        int connect;
        final int[] freq = new int[2];
        int volume;
    }

    private static class RatEvent {
        int note;
        int instrument;
        int volume;
        int fx;
        int fxp;
    }

    private static class RatChannel {
        int instrument;
        int volume;
        int fx;
        int fxp;
    }

    private static final int[] RAT_ADLIB_BASES = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0A, 0x10, 0x11, 0x12,
        0x03, 0x04, 0x05, 0x0B, 0x0C, 0x0D, 0x13, 0x14, 0x15
    };

    private static final int[] RAT_NOTES = {
        0x157, 0x16B, 0x181, 0x198, 0x1B0, 0x1CA, 0x1E5, 0x202,
        0x220, 0x241, 0x263, 0x287, 0x000, 0x000, 0x000, 0x000
    };

    private int ratVersion;
    private String ratTitle = "";
    private int ratSpeed;
    private int ratNumChan;
    private int ratNumInst;
    private int ratNumPat;
    private int ratOrderStart;
    private int ratOrderLoop;
    private int ratOrderEnd;
    private int ratGlobalVolume;
    private int ratPatSeg;

    private int ratOrderOffset;
    private RatInstrument[] ratInstruments;
    private RatEvent[][][] ratTracks;

    private final RatChannel[] ratChannels = new RatChannel[9];
    private int ratOrderPos;
    private int ratPatternPos;
    private int ratVolume;

    public XadratPlayer(XadPlayer parent) {
        super(parent);
        for (int i = 0; i < 9; i++) {
            ratChannels[i] = new RatChannel();
        }
    }

    private int calcVolume(int ivol, int cvol, int gvol) {
        int vol = ivol & 0x3F;
        vol ^= 0x3F;
        vol *= cvol;
        vol >>= 6;
        vol *= gvol;
        vol >>= 6;
        vol ^= 0x3F;
        vol |= (ivol & 0xC0);
        return vol & 0xff;
    }

    @Override
    public boolean load() {
        if (parent.xad.fmt != 5 || parent.tuneSize < 0x140) {
            return false;
        }

        if (parent.tune[0] != 'R' || parent.tune[1] != 'A' || parent.tune[2] != 'T') {
            return false;
        }

        ratVersion = parent.tune[3] & 0xff;
        if (ratVersion != 0x10) {
            return false;
        }

        ratTitle = parent.readString(parent.tune, 4, 32);
        ratNumChan = parent.tune[36] & 0xff;
        ratOrderEnd = parent.tune[38] & 0xff;
        ratNumInst = parent.tune[40] & 0xff;
        ratNumPat = parent.tune[42] & 0xff;
        ratOrderStart = parent.tune[44] & 0xff;
        ratOrderLoop = parent.tune[46] & 0xff;
        ratGlobalVolume = parent.tune[48] & 0xff;
        ratSpeed = parent.tune[49] & 0xff;
        ratPatSeg = (parent.tune[62] & 0xff) | ((parent.tune[63] & 0xff) << 8);

        if (ratNumChan < 1 || ratNumChan > 9) {
            return false;
        }

        ratOrderOffset = 0x40;

        ratInstruments = new RatInstrument[ratNumInst];
        for (int i = 0; i < ratNumInst; i++) {
            int instOffset = 0x140 + i * 20;
            if (instOffset + 20 > parent.tuneSize) {
                return false;
            }
            RatInstrument inst = new RatInstrument();
            inst.freq[0] = parent.tune[instOffset] & 0xff;
            inst.freq[1] = parent.tune[instOffset + 1] & 0xff;
            // reserved_2 is at instOffset + 2, 3
            inst.modCtrl = parent.tune[instOffset + 4] & 0xff;
            inst.carCtrl = parent.tune[instOffset + 5] & 0xff;
            inst.modVolume = parent.tune[instOffset + 6] & 0xff;
            inst.carVolume = parent.tune[instOffset + 7] & 0xff;
            inst.modAD = parent.tune[instOffset + 8] & 0xff;
            inst.carAD = parent.tune[instOffset + 9] & 0xff;
            inst.modSR = parent.tune[instOffset + 10] & 0xff;
            inst.carSR = parent.tune[instOffset + 11] & 0xff;
            inst.modWave = parent.tune[instOffset + 12] & 0xff;
            inst.carWave = parent.tune[instOffset + 13] & 0xff;
            inst.connect = parent.tune[instOffset + 14] & 0xff;
            // reserved_F is at instOffset + 15
            inst.volume = parent.tune[instOffset + 16] & 0xff;
            // reserved_11 is at instOffset + 17, 18, 19
            ratInstruments[i] = inst;
        }

        if (ratPatSeg * 16 + 5 * ratNumPat * 64 * ratNumChan > parent.tuneSize) {
            return false;
        }

        int eventPtr = ratPatSeg * 16;
        ratTracks = new RatEvent[ratNumPat][64][ratNumChan];
        for (int i = 0; i < ratNumPat; i++) {
            for (int j = 0; j < 64; j++) {
                for (int k = 0; k < ratNumChan; k++) {
                    RatEvent event = new RatEvent();
                    event.note = parent.tune[eventPtr] & 0xff;
                    event.instrument = parent.tune[eventPtr + 1] & 0xff;
                    event.volume = parent.tune[eventPtr + 2] & 0xff;
                    event.fx = parent.tune[eventPtr + 3] & 0xff;
                    event.fxp = parent.tune[eventPtr + 4] & 0xff;
                    ratTracks[i][j][k] = event;
                    eventPtr += 5;
                }
            }
        }

        return true;
    }

    @Override
    public void rewind(int subSong) {
        ratOrderPos = ratOrderStart;
        ratPatternPos = 0;
        ratVolume = ratGlobalVolume;

        parent.plrSpeed = ratSpeed;

        for (int i = 0; i < 9; i++) {
            ratChannels[i].instrument = 0;
            ratChannels[i].volume = 0;
            ratChannels[i].fx = 0;
            ratChannels[i].fxp = 0;
        }

        parent.oplWrite(0x01, 0x20);
        parent.oplWrite(0x08, 0x00);
        parent.oplWrite(0xBD, 0x00);

        for (int i = 0; i < 9; i++) {
            parent.oplWrite(0xA0 + i, 0x00);
            parent.oplWrite(0xA3 + i, 0x00);
            parent.oplWrite(0xB0 + i, 0x00);
            parent.oplWrite(0xB3 + i, 0x00);
        }

        for (int i = 0; i < 0x1F; i++) {
            parent.oplWrite(0x40 + i, 0x3F);
        }
    }

    @Override
    public void update() {
        int pattern = parent.tune[ratOrderOffset + ratOrderPos] & 0xff;
        if (pattern >= ratNumPat) {
            ratPatternPos = 0;
            ratOrderPos++;
            if (ratOrderPos == ratOrderEnd) {
                ratOrderPos = ratOrderLoop;
                parent.plrLooping = 1;
            }
            return;
        }

        for (int i = 0; i < ratNumChan; i++) {
            RatEvent event = ratTracks[pattern][ratPatternPos][i];

            if (event.instrument != 0xFF) {
                ratChannels[i].instrument = event.instrument - 1;
                ratChannels[i].volume = ratInstruments[event.instrument - 1].volume;
            }

            if (event.volume != 0xFF) {
                ratChannels[i].volume = event.volume;
            }

            if (event.note != 0xFF) {
                parent.oplWrite(0xB0 + i, 0x00);
                parent.oplWrite(0xA0 + i, 0x00);

                if (event.note != 0xFE) {
                    int ins = ratChannels[i].instrument;
                    RatInstrument instrument = ratInstruments[ins];

                    parent.oplWrite(0xC0 + i, instrument.connect);

                    parent.oplWrite(0x20 + RAT_ADLIB_BASES[i], instrument.modCtrl);
                    parent.oplWrite(0x20 + RAT_ADLIB_BASES[i + 9], instrument.carCtrl);

                    parent.oplWrite(0x40 + RAT_ADLIB_BASES[i], calcVolume(instrument.modVolume, ratChannels[i].volume, ratVolume));
                    parent.oplWrite(0x40 + RAT_ADLIB_BASES[i + 9], calcVolume(instrument.carVolume, ratChannels[i].volume, ratVolume));

                    parent.oplWrite(0x60 + RAT_ADLIB_BASES[i], instrument.modAD);
                    parent.oplWrite(0x60 + RAT_ADLIB_BASES[i + 9], instrument.carAD);

                    parent.oplWrite(0x80 + RAT_ADLIB_BASES[i], instrument.modSR);
                    parent.oplWrite(0x80 + RAT_ADLIB_BASES[i + 9], instrument.carSR);

                    parent.oplWrite(0xE0 + RAT_ADLIB_BASES[i], instrument.modWave);
                    parent.oplWrite(0xE0 + RAT_ADLIB_BASES[i + 9], instrument.carWave);

                    int insfreq = (instrument.freq[1] << 8) | instrument.freq[0];
                    int freq = insfreq * RAT_NOTES[event.note & 0x0F] / 0x20AB;

                    parent.oplWrite(0xA0 + i, freq & 0xFF);
                    parent.oplWrite(0xB0 + i, ((freq >> 8) & 0xFF) | ((event.note & 0xF0) >> 2) | 0x20);
                }
            }

            if (event.fx != 0xFF) {
                ratChannels[i].fx = event.fx;
                ratChannels[i].fxp = event.fxp;
            }
        }

        ratPatternPos++;

        for (int i = 0; i < ratNumChan; i++) {
            switch (ratChannels[i].fx) {
                case 0x01 -> parent.plrSpeed = ratChannels[i].fxp;
                case 0x02 -> {
                    if (ratChannels[i].fxp < ratOrderEnd) {
                        if (ratChannels[i].fxp <= ratOrderPos) {
                            parent.plrLooping = 1;
                        }
                        ratOrderPos = ratChannels[i].fxp;
                    } else {
                        parent.plrLooping = 1;
                        ratOrderPos = 0;
                    }
                    ratPatternPos = 0;
                }
                case 0x03 -> ratPatternPos = 0x40;
            }
            ratChannels[i].fx = 0;
        }

        if (ratPatternPos >= 0x40) {
            ratPatternPos = 0;
            ratOrderPos++;
            if (ratOrderPos == ratOrderEnd) {
                ratOrderPos = ratOrderLoop;
                parent.plrLooping = 1;
            }
        }
    }

    @Override
    public float getRefresh() {
        return 60.0f;
    }

    @Override
    public String getType() {
        return "xad: rat player";
    }

    @Override
    public String getTitle() {
        return ratTitle;
    }

    @Override
    public int getInstruments() {
        return ratNumInst;
    }
}

class XadhybridPlayer extends XadSubPlayer {

    private static class HybInstrument {
        String name = "";
        final int[] data = new int[11];
    }

    private static class HybChannel {
        int freq;
        int freqSlide;
    }

    private static final int[] HYB_ADLIB_REGISTERS = {
        0xE0, 0x60, 0x80, 0x20, 0x40, 0xE3, 0x63, 0x83, 0x23, 0x43, 0xC0,
        0xE1, 0x61, 0x81, 0x21, 0x41, 0xE4, 0x64, 0x84, 0x24, 0x44, 0xC1,
        0xE2, 0x62, 0x82, 0x22, 0x42, 0xE5, 0x65, 0x85, 0x25, 0x45, 0xC2,
        0xE8, 0x68, 0x88, 0x28, 0x48, 0xEB, 0x6B, 0x8B, 0x2B, 0x4B, 0xC3,
        0xE9, 0x69, 0x89, 0x29, 0x49, 0xEC, 0x6C, 0x8C, 0x2C, 0x4C, 0xC4,
        0xEA, 0x6A, 0x8A, 0x2A, 0x4A, 0xED, 0x6D, 0x8D, 0x2D, 0x4D, 0xC5,
        0xF0, 0x70, 0x90, 0x30, 0x50, 0xF3, 0x73, 0x93, 0x33, 0x53, 0xC6,
        0xF1, 0x71, 0x91, 0x31, 0x51, 0xF4, 0x74, 0x94, 0x34, 0x54, 0xC7,
        0xF2, 0x72, 0x92, 0x32, 0x52, 0xF5, 0x75, 0x95, 0x35, 0x55, 0xC8
    };

    private static final int[] HYB_NOTES = {
        0x0000, 0x0000,
        0x016B, 0x0181, 0x0198, 0x01B0, 0x01CA, 0x01E5, 0x0202, 0x0220, 0x0241, 0x0263, 0x0287, 0x02AE,
        0x056B, 0x0581, 0x0598, 0x05B0, 0x05CA, 0x05E5, 0x0602, 0x0620, 0x0641, 0x0663, 0x0687, 0x06AE,
        0x096B, 0x0981, 0x0998, 0x09B0, 0x09CA, 0x09E5, 0x0A02, 0x0A20, 0x0A41, 0x0A63, 0x0A87, 0x0AAE,
        0x0D6B, 0x0D81, 0x0D98, 0x0DB0, 0x0DCA, 0x0DE5, 0x0E02, 0x0E20, 0x0E41, 0x0E63, 0x0E87, 0x0EAE,
        0x116B, 0x1181, 0x1198, 0x11B0, 0x11CA, 0x11E5, 0x1202, 0x1220, 0x1241, 0x1263, 0x1287, 0x12AE,
        0x156B, 0x1581, 0x1598, 0x15B0, 0x15CA, 0x15E5, 0x1602, 0x1620, 0x1641, 0x1663, 0x1687, 0x16AE,
        0x196B, 0x1981, 0x1998, 0x19B0, 0x19CA, 0x19E5, 0x1A02, 0x1A20, 0x1A41, 0x1A63, 0x1A87, 0x1AAE,
        0x1D6B, 0x1D81, 0x1D98, 0x1DB0, 0x1DCA, 0x1DE5, 0x1E02, 0x1E20, 0x1E41, 0x1E63, 0x1E87, 0x1EAE
    };

    private final HybInstrument[] hybInstruments = new HybInstrument[26];
    private final HybChannel[] hybChannels = new HybChannel[9];

    private int hybOrderOffset;
    private int hybOrderPos;
    private int hybPatternPos;
    private int hybSpeed;
    private int hybSpeedCounter;

    public XadhybridPlayer(XadPlayer parent) {
        super(parent);
        for (int i = 0; i < 26; i++) {
            hybInstruments[i] = new HybInstrument();
        }
        for (int i = 0; i < 9; i++) {
            hybChannels[i] = new HybChannel();
        }
    }

    @Override
    public boolean load() {
        if (parent.xad.fmt != 6) {
            return false;
        }

        for (int i = 0; i < 26; i++) {
            int instOffset = i * 18;
            if (instOffset + 18 > parent.tuneSize) {
                return false;
            }
            hybInstruments[i].name = parent.readString(parent.tune, instOffset, 7);
            for (int j = 0; j < 11; j++) {
                hybInstruments[i].data[j] = parent.tune[instOffset + 7 + j] & 0xff;
            }
        }

        hybOrderOffset = 0x1D4;
        return true;
    }

    @Override
    public void rewind(int subSong) {
        hybOrderPos = 0;
        hybPatternPos = 0;

        hybSpeed = 6;
        hybSpeedCounter = 1;

        parent.plrSpeed = 1;

        for (int i = 0; i < 9; i++) {
            hybChannels[i].freq = 0x2000;
            hybChannels[i].freqSlide = 0;
        }

        parent.oplWrite(0x01, 0x20);
        parent.oplWrite(0xBD, 0x40);
        parent.oplWrite(0x08, 0x00);

        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 11; j++) {
                parent.oplWrite(HYB_ADLIB_REGISTERS[i * 11 + j], 0x00);
            }
            parent.oplWrite(0xA0 + i, 0x00);
            parent.oplWrite(0xB0 + i, 0x20);
        }
    }

    @Override
    public void update() {
        hybSpeedCounter--;
        if (hybSpeedCounter > 0) {
            updateSlides();
            return;
        }

        hybSpeedCounter = hybSpeed;

        int patpos = hybPatternPos;
        int ordpos = hybOrderPos;

        for (int i = 0; i < 9; i++) {
            int trackIdx = parent.tune[hybOrderOffset + ordpos * 9 + i] & 0xff;
            int posOffset = 0xADE + trackIdx * 128 + patpos * 2;
            int event = (parent.tune[posOffset] & 0xff) | ((parent.tune[posOffset + 1] & 0xff) << 8);

            int note = event >> 9;
            int ins = (event & 0x01F0) >> 4;
            int slide = event & 0x000F;

            switch (note) {
                case 0x7D -> hybSpeed = event & 0xFF;
                case 0x7E -> {
                    hybOrderPos = event & 0xFF;
                    hybPatternPos = 0x3F;
                    if (hybOrderPos <= ordpos) {
                        parent.plrLooping = 1;
                    }
                }
                case 0x7F -> hybPatternPos = 0x3F;
                default -> {
                    if (ins != 0) {
                        for (int j = 0; j < 11; j++) {
                            parent.oplWrite(HYB_ADLIB_REGISTERS[i * 11 + j], hybInstruments[ins - 1].data[j]);
                        }
                    }

                    if (note != 0) {
                        hybChannels[i].freq = HYB_NOTES[note];
                        hybChannels[i].freqSlide = 0;
                    }

                    if (slide != 0) {
                        hybChannels[i].freqSlide = -(slide >> 3) * (slide & 7) * 2;
                    }

                    if ((hybChannels[i].freq & 0x2000) == 0) {
                        parent.oplWrite(0xA0 + i, hybChannels[i].freq & 0xFF);
                        parent.oplWrite(0xB0 + i, hybChannels[i].freq >> 8);

                        hybChannels[i].freq |= 0x2000;

                        parent.oplWrite(0xA0 + i, hybChannels[i].freq & 0xFF);
                        parent.oplWrite(0xB0 + i, hybChannels[i].freq >> 8);
                    }
                }
            }
        }

        hybPatternPos++;
        if (hybPatternPos >= 0x40) {
            hybPatternPos = 0;
            hybOrderPos++;
        }

        updateSlides();
    }

    private void updateSlides() {
        for (int i = 0; i < 9; i++) {
            if (hybChannels[i].freqSlide != 0) {
                hybChannels[i].freq = (((hybChannels[i].freq & 0x1FFF) + hybChannels[i].freqSlide) & 0x1FFF) | 0x2000;
                parent.oplWrite(0xA0 + i, hybChannels[i].freq & 0xFF);
                parent.oplWrite(0xB0 + i, hybChannels[i].freq >> 8);
            }
        }
    }

    @Override
    public float getRefresh() {
        if (hybSpeed == 2) return 34.0f;
        if (hybSpeed == 5) return 42.0f;
        if (hybSpeed == 6) return 43.0f;
        if (hybSpeed == 7) return 44.0f;
        return 50.0f;
    }

    @Override
    public String getType() {
        return "xad: Domark Player";
    }

    @Override
    public int getInstruments() {
        return 26;
    }

    @Override
    public String getInstrument(int i) {
        return hybInstruments[i].name;
    }

    @Override
    public int getSpeed() {
        return hybSpeed;
    }
}
