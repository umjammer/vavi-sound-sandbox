/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2007 Simon Peter, <dn.tlp@gmx.net>, et al.
 *
 * adl.cpp - ADL player adaption by Simon Peter <dn.tlp@gmx.net>
 *
 * Original ADL player by Torbjorn Andersson and Johannes Schickel
 * 'lordhoto' of the ScummVM project.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * Westwood ADL Player (EoB, Dune II, Kyrandia, LoL).
 *
 * @author Torbjorn Andersson
 * @author Johannes Schickel
 */
public class AdlPlayer extends Opl3Player {

    private static final Logger logger = getLogger(AdlPlayer.class.getName());

    // effect callback codes
    private static final int EFF_NONE = 0;
    private static final int EFF_SLIDE = 1;
    private static final int EFF_VIBRATO = 2;
    private static final int SEFF_1 = 1;

    private static final int[] _regOffset = {0x00, 0x01, 0x02, 0x08, 0x09, 0x0A, 0x10, 0x11, 0x12};
    private static final int[] _freqTable = {
        0x0134, 0x0147, 0x015A, 0x016F, 0x0184, 0x019C, 0x01B4, 0x01CE, 0x01E9, 0x0207, 0x0225, 0x0246
    };

    private static final int[] _unkTable2_1 = new int[132];
    private static final int[] _unkTable2_2 = new int[128];
    private static final int[] _unkTable2_3 = new int[130];
    static {
        for (int i = 0; i < 132; i++) _unkTable2_1[i] = 0x50 - (i >> 1);
        for (int i = 0; i < 128; i++) _unkTable2_2[i] = i;
        _unkTable2_2[95] = 0x6F;
        for (int i = 0; i < 130; i++) _unkTable2_3[i] = 0x40 - (i / 3);
    }
    private static final int[][] _unkTable2 = {_unkTable2_1, _unkTable2_2, _unkTable2_1, _unkTable2_2, _unkTable2_3, _unkTable2_2};

    private static final int[][] _pitchBendTables = {
        {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21},
        {0x00, 0x01, 0x02, 0x03, 0x04, 0x06, 0x07, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x22, 0x24},
        {0x00, 0x01, 0x02, 0x03, 0x04, 0x06, 0x08, 0x09, 0x0A, 0x0C, 0x0D, 0x0E, 0x0F, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x19, 0x1A, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x22, 0x24, 0x25, 0x26},
        {0x00, 0x01, 0x02, 0x03, 0x04, 0x06, 0x08, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x1A, 0x1C, 0x1D, 0x1E, 0x1F, 0x20, 0x21, 0x23, 0x25, 0x27, 0x28},
        {0x00, 0x01, 0x02, 0x03, 0x04, 0x06, 0x08, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x11, 0x13, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1B, 0x1D, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x28, 0x2A},
        {0x00, 0x01, 0x02, 0x03, 0x05, 0x07, 0x09, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x13, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1B, 0x1D, 0x1F, 0x20, 0x21, 0x22, 0x23, 0x25, 0x27, 0x29, 0x2B, 0x2D},
        {0x00, 0x01, 0x02, 0x03, 0x05, 0x07, 0x09, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x13, 0x15, 0x16, 0x17, 0x18, 0x1A, 0x1C, 0x1E, 0x21, 0x24, 0x25, 0x26, 0x27, 0x29, 0x2B, 0x2D, 0x2F, 0x30},
        {0x00, 0x01, 0x02, 0x04, 0x06, 0x08, 0x0A, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x13, 0x15, 0x18, 0x19, 0x1A, 0x1C, 0x1D, 0x1F, 0x21, 0x23, 0x25, 0x26, 0x27, 0x29, 0x2B, 0x2D, 0x2F, 0x30, 0x32},
        {0x00, 0x01, 0x02, 0x04, 0x06, 0x08, 0x0A, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x14, 0x17, 0x1A, 0x19, 0x1A, 0x1C, 0x1E, 0x20, 0x22, 0x25, 0x28, 0x29, 0x2A, 0x2B, 0x2D, 0x2F, 0x31, 0x33, 0x35},
        {0x00, 0x01, 0x03, 0x05, 0x07, 0x09, 0x0B, 0x0E, 0x0F, 0x10, 0x12, 0x14, 0x16, 0x18, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x20, 0x22, 0x24, 0x26, 0x29, 0x2A, 0x2C, 0x2E, 0x30, 0x32, 0x34, 0x36, 0x39},
        {0x00, 0x01, 0x03, 0x05, 0x07, 0x09, 0x0B, 0x0E, 0x0F, 0x10, 0x12, 0x14, 0x16, 0x19, 0x1B, 0x1E, 0x1F, 0x21, 0x23, 0x25, 0x27, 0x29, 0x2B, 0x2D, 0x2E, 0x2F, 0x31, 0x32, 0x34, 0x36, 0x39, 0x3C},
        {0x00, 0x01, 0x03, 0x05, 0x07, 0x0A, 0x0C, 0x0F, 0x10, 0x11, 0x13, 0x15, 0x17, 0x19, 0x1B, 0x1E, 0x1F, 0x20, 0x22, 0x24, 0x26, 0x28, 0x2B, 0x2E, 0x2F, 0x30, 0x32, 0x34, 0x36, 0x39, 0x3C, 0x3F},
        {0x00, 0x02, 0x04, 0x06, 0x08, 0x0B, 0x0D, 0x10, 0x11, 0x12, 0x14, 0x16, 0x18, 0x1B, 0x1E, 0x21, 0x22, 0x23, 0x25, 0x27, 0x29, 0x2C, 0x2F, 0x32, 0x33, 0x34, 0x36, 0x38, 0x3B, 0x34, 0x41, 0x44},
        {0x00, 0x02, 0x04, 0x06, 0x08, 0x0B, 0x0D, 0x11, 0x12, 0x13, 0x15, 0x17, 0x1A, 0x1D, 0x20, 0x23, 0x24, 0x25, 0x27, 0x29, 0x2C, 0x2F, 0x32, 0x35, 0x36, 0x37, 0x39, 0x3B, 0x3E, 0x41, 0x44, 0x47},
    };

    /** number of value bytes for each parser opcode */
    private static final int[] PARSER_VALUES = {
        1, 2, 1, 1, 2, 2, 0, 1, 0, 1, 2, 2, 1, 5, 1, 1,
        1, 3, 0, 1, 0, 4, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0,
        1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 2, 2, 1, 1,
        1, 0, 0, 1, 0, 2, 0, 0, 0, 1, 0, 0, 1, 1, 0, 2,
        0, 9, 1, 0, 2, 2, 2, 1, 1, 2, 0
    };
    private static final int PARSER_SIZE = PARSER_VALUES.length;

    private static class Channel {
        boolean lock;
        boolean repeating;
        int opExtraLevel2;
        int dataptr = -1;
        int duration;
        int repeatCounter;
        int baseOctave; // s8
        int priority;
        int dataptrStackPos;
        final int[] dataptrStack = new int[4];
        int baseNote; // s8
        int slideTempo;
        int slideTimer;
        int slideStep; // s16
        int vibratoStep; // s16
        int vibratoStepRange;
        int vibratoStepsCountdown;
        int vibratoNumSteps;
        int vibratoDelay;
        int vibratoTempo;
        int vibratoTimer;
        int vibratoDelayCountdown;
        int opExtraLevel1;
        int spacing2;
        int baseFreq;
        int tempo;
        int timer;
        int regAx;
        int regBx;
        int primaryEffect;
        int secondaryEffect;
        int fractionalSpacing;
        int opLevel1;
        int opLevel2;
        int opExtraLevel3;
        int twoChan;
        int unk39;
        int unk40;
        int spacing1;
        int durationRandomness;
        int secondaryEffectTempo;
        int secondaryEffectTimer;
        int secondaryEffectSize; // s8
        int secondaryEffectPos; // s8
        int secondaryEffectRegbase;
        int secondaryEffectData;
        int tempoReset;
        int rawNote;
        int pitchBend; // s8
        int volumeModifier;

        void reset() {
            int backupEL2 = opExtraLevel2;
            lock = false; repeating = false;
            opExtraLevel2 = 0; dataptr = -1; duration = 0; repeatCounter = 0;
            baseOctave = 0; priority = 0; dataptrStackPos = 0;
            for (int i = 0; i < 4; i++) dataptrStack[i] = 0;
            baseNote = 0; slideTempo = 0; slideTimer = 0; slideStep = 0;
            vibratoStep = 0; vibratoStepRange = 0; vibratoStepsCountdown = 0;
            vibratoNumSteps = 0; vibratoDelay = 0; vibratoTempo = 0; vibratoTimer = 0;
            vibratoDelayCountdown = 0; opExtraLevel1 = 0; spacing2 = 0; baseFreq = 0;
            tempo = 0; timer = 0; regAx = 0; regBx = 0; primaryEffect = 0; secondaryEffect = 0;
            fractionalSpacing = 0; opLevel1 = 0; opLevel2 = 0; opExtraLevel3 = 0; twoChan = 0;
            unk39 = 0; unk40 = 0; spacing1 = 0; durationRandomness = 0;
            secondaryEffectTempo = 0; secondaryEffectTimer = 0; secondaryEffectSize = 0;
            secondaryEffectPos = 0; secondaryEffectRegbase = 0; secondaryEffectData = 0;
            tempoReset = 0; rawNote = 0; pitchBend = 0; volumeModifier = 0;
            // initChannel overrides:
            opExtraLevel2 = backupEL2;
            tempo = 0xFF; priority = 0; primaryEffect = EFF_NONE; secondaryEffect = EFF_NONE;
            spacing1 = 1;
        }
    }

    private static class QueueEntry {
        int data = -1;
        int id;
        int volume;
    }

    // ---- player state ----
    private int numsubsongs, cursubsong;
    private int _version;
    private final int[] _trackEntries = new int[500];
    private byte[] _soundData;
    private int _soundDataSize;
    private int _numPrograms;

    // ---- driver state ----
    private int _curChannel;
    private int _soundTrigger;
    private int _rnd;
    private int _beatDivider, _beatDivCnt, _callbackTimer, _beatCounter, _beatWaiting;
    private int _opLevelBD, _opLevelHH, _opLevelSD, _opLevelTT, _opLevelCY;
    private int _opExtraLevel1HH, _opExtraLevel2HH, _opExtraLevel1CY, _opExtraLevel2CY;
    private int _opExtraLevel2TT, _opExtraLevel1TT, _opExtraLevel1SD, _opExtraLevel2SD;
    private int _opExtraLevel1BD, _opExtraLevel2BD;
    private final QueueEntry[] _programQueue = new QueueEntry[16];
    private int _programStartTimeout, _programQueueStart, _programQueueEnd;
    private boolean _retrySounds;
    private int _sfxPointer = -1;
    private int _sfxPriority, _sfxVelocity;
    private final Channel[] _channels = new Channel[10];
    private int _vibratoAndAMDepthBits;
    private int _rhythmSectionBits;
    private int _curRegOffset;
    private int _tempo;
    private int _tablePtr1, _tablePtr2; // indices into _unkTable2 rows
    private int _tablePtr1Row, _tablePtr2Row;
    private int _syncJumpMask;
    private int _musicVolume, _sfxVolume;

    private boolean advWrapped;

    public AdlPlayer() {
        for (int i = 0; i < 10; i++) {
            _channels[i] = new Channel();
        }
        for (int i = 0; i < 16; i++) {
            _programQueue[i] = new QueueEntry();
        }
        _rnd = 0x1234;
        _tempo = 0;
        _soundTrigger = 0;
        _programStartTimeout = 0;
        _callbackTimer = 0xFF;
        _musicVolume = _sfxVolume = 0xFF;
        _programQueueStart = _programQueueEnd = 0;
        _retrySounds = false;
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Westwood ADL", "adl");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("ADL");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(1 << 22);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = bitStream.read(buf)) != -1) {
                baos.write(buf, 0, r);
            }
            return detect(baos.toByteArray()) != 0;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException e) {
                logger.log(Level.DEBUG, e.toString());
            }
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    /** returns detected version (1..4) or 0 if the data is not a valid ADL file */
    private static int detect(byte[] f) {
        long fileSize = f.length;
        if (fileSize < 720) {
            return 0;
        }
        int version = 4, ofs = 500;
        for (int i = 0; i < 500; i += 2) {
            int w = le16(f, i);
            if (w >= 500 && w < 0xFFFF) {
                version = 3;
                ofs = 120;
                break;
            }
        }
        int soundDataSize = (int) fileSize - ofs;
        // The format has no magic number, so besides the offset-plausibility checks
        // below we also require at least one valid in-range program offset. This
        // rejects otherwise size-compatible files (e.g. HSC) whose program table
        // region is all zeroes.
        int valid = 0;
        if (version < 4) {
            int numProgs = 150;
            for (int i = 0; i < numProgs * 2; i += 2) {
                int w = le16(f, ofs + i);
                if (w > 0 && w < 600) return 0;
                if (w > 0 && w < 1000) version = 1;
                if (w != 0 && w != 0xFFFF && w < soundDataSize) valid++;
            }
            if (version > 1) {
                if (fileSize < 1120) return 0;
                numProgs = 250;
                for (int i = 150 * 2; i < numProgs * 2; i += 2) {
                    int w = le16(f, ofs + i);
                    if (w > 0 && w < 1000) return 0;
                    if (w != 0 && w != 0xFFFF && w < soundDataSize) valid++;
                }
            }
        } else {
            if (fileSize < 2500) return 0;
            for (int i = 0; i < 500 * 2; i += 2) {
                int w = le16(f, ofs + i);
                if (w > 0 && w < 2000) return 0;
                if (w != 0 && w != 0xFFFF && w < soundDataSize) valid++;
            }
        }
        if (valid == 0) {
            return 0;
        }
        return version;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] f = is.readAllBytes();
        long fileSize = f.length;

        _version = detect(f);
        if (_version == 0) {
            throw new IllegalArgumentException("adl: not a valid ADL file");
        }

        int ofs = _version == 4 ? 500 : 120;

        // read track entries (up to 500 bytes)
        for (int i = 0; i < 500; i++) {
            _trackEntries[i] = i < f.length ? f[i] & 0xff : 0xFF;
        }

        // build soundData = file[ofs..end]
        int soundDataSize = (int) fileSize - ofs;
        _soundData = new byte[soundDataSize];
        System.arraycopy(f, ofs, _soundData, 0, soundDataSize);
        // trackEntries tail beyond ofs is cleared to 0xFF
        for (int i = ofs; i < 500; i++) {
            _trackEntries[i] = 0xFF;
        }

        // (offset plausibility already validated by detect())

        setVersion(_version);
        setSoundData(_soundData, soundDataSize);

        int numProgs = _numPrograms; // 150 (v1), 250 (v2/3), 500 (v4)

        // find last subsong
        if (_version == 4) {
            for (int i = 2 * 250; i > 0; i -= 2) {
                if (le16e(_trackEntries, i - 2) < numProgs) {
                    numsubsongs = i / 2;
                    break;
                }
            }
        } else {
            for (int i = 120; i > 0; i--) {
                if (_trackEntries[i - 1] < numProgs) {
                    numsubsongs = i;
                    break;
                }
            }
        }

logger.log(Level.DEBUG, "version: " + _version + ", subsongs: " + numsubsongs);

        rewind(2); // subsong 2 is selected by default
    }

    @Override
    public boolean update() {
        callback();

        for (int i = 0; i < 10; i++) {
            if (_channels[i].dataptr != -1 && !_channels[i].repeating) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void rewind(int subSong) {
        // stop current song and re-initialize the opl
        resetAdLibState();
        stopAllChannels();
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32);

        if (subSong >= numsubsongs) {
            subSong = 0;
        }
        if (subSong >= 0) {
            cursubsong = subSong;
        }

        play(cursubsong, 0xFF);
    }

    @Override
    public float getRefresh() {
        return 72.0f;
    }

    // ---- helpers ----

    private static int le16(byte[] b, int off) {
        if (off < 0 || off + 1 >= b.length) {
            return 0;
        }
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    private static int le16e(int[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    private int sd(int off) {
        return off >= 0 && off < _soundDataSize ? _soundData[off] & 0xff : 0;
    }

    private void sdSet(int off, int val) {
        if (off >= 0 && off < _soundDataSize) {
            _soundData[off] = (byte) val;
        }
    }

    private int sdLe16(int off) {
        return sd(off) | (sd(off + 1) << 8);
    }

    private int sdBe16(int off) {
        return (sd(off) << 8) | sd(off + 1);
    }

    private static int clip(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    private void writeOPL(int reg, int val) {
        write(0, reg & 0xff, val & 0xff);
    }

    /** add tempo to timer (mod 256); sets {@link #advWrapped} to whether it wrapped */
    private int advance(int timer, int tempo) {
        int old = timer & 0xff;
        int nt = (old + tempo) & 0xff;
        advWrapped = nt < old;
        return nt;
    }

    private int checkDataOffset(int ptr, long n) {
        if (ptr != -1) {
            long offset = ptr;
            if (n >= -offset && n <= (long) _soundDataSize - offset) {
                return (int) (ptr + n);
            }
        }
        return -1;
    }

    private int getProgram(int progId) {
        if (progId < 0 || progId >= _soundDataSize / 2) {
            return -1;
        }
        int offset = sdLe16(2 * progId);
        if (offset == 0 || offset >= _soundDataSize) {
            return -1;
        }
        return offset;
    }

    private int getInstrument(int instrumentId) {
        return getProgram(_numPrograms + instrumentId);
    }

    private void setVersion(int v) {
        _version = v;
        _numPrograms = (v == 1) ? 150 : ((v == 4) ? 500 : 250);
    }

    private void setSoundData(byte[] data, int size) {
        _programQueueStart = _programQueueEnd = 0;
        _programQueue[0] = new QueueEntry();
        _sfxPointer = -1;
        _soundData = data;
        _soundDataSize = size;
    }

    // ---- driver ----

    private void play(int track, int volume) {
        int soundId;
        if (track >= numsubsongs) {
            return;
        }
        if (_version == 4) {
            soundId = le16e(_trackEntries, track << 1);
        } else {
            soundId = _trackEntries[track];
        }
        if ((soundId == 0xFFFF && _version == 4) || (soundId == 0xFF && _version < 4) || _soundData == null) {
            return;
        }
        startSound(soundId, volume);
    }

    private void startSound(int track, int volume) {
        int trackData = getProgram(track);
        if (trackData == -1) {
            return;
        }
        if (_programQueueEnd == _programQueueStart && _programQueue[_programQueueEnd].data != -1) {
            return;
        }
        QueueEntry e = new QueueEntry();
        e.data = trackData;
        e.id = track;
        e.volume = volume;
        _programQueue[_programQueueEnd] = e;
        _programQueueEnd = (_programQueueEnd + 1) & 15;
    }

    private void stopAllChannels() {
        for (int channel = 0; channel <= 9; ++channel) {
            _curChannel = channel;
            Channel chan = _channels[_curChannel];
            chan.priority = 0;
            chan.dataptr = -1;
            if (channel != 9) {
                noteOff(chan);
            }
        }
        _retrySounds = false;
        _programQueueStart = _programQueueEnd = 0;
        _programQueue[0] = new QueueEntry();
        _programStartTimeout = 0;
    }

    private void callback() {
        if (_programStartTimeout != 0) {
            --_programStartTimeout;
        } else {
            setupPrograms();
        }
        executePrograms();

        _callbackTimer = advance(_callbackTimer, _tempo);
        if (advWrapped) {
            _beatDivCnt = (_beatDivCnt - 1) & 0xff;
            if (_beatDivCnt == 0) {
                _beatDivCnt = _beatDivider;
                _beatCounter = (_beatCounter + 1) & 0xff;
            }
        }
    }

    private void setupPrograms() {
        QueueEntry entry = _programQueue[_programQueueStart];
        int ptr = entry.data;

        if (_programQueueStart == _programQueueEnd && ptr == -1) {
            return;
        }

        QueueEntry retrySound = null;
        if (entry.id == 0) {
            _retrySounds = true;
        } else if (_retrySounds) {
            retrySound = entry;
        }

        entry.data = -1;
        _programQueueStart = (_programQueueStart + 1) & 15;

        if (checkDataOffset(ptr, 2) == -1) {
            return;
        }

        int chan = sd(ptr);
        if (chan > 9 || (chan < 9 && checkDataOffset(ptr, 4) == -1)) {
            return;
        }

        Channel channel = _channels[chan];

        adjustSfxData(ptr, entry.volume);

        int priority = sd(ptr + 1);
        int dataStart = ptr + 2;

        if (priority >= channel.priority) {
            initChannel(channel);
            channel.priority = priority;
            channel.dataptr = dataStart;
            channel.tempo = 0xFF;
            channel.timer = 0xFF;
            channel.duration = 1;

            channel.volumeModifier = chan <= 5 ? _musicVolume : _sfxVolume;

            initAdlibChannel(chan);
            _programStartTimeout = 2;
            retrySound = null;
        }

        if (retrySound != null && retrySound.data != -1) {
            startSound(retrySound.id, retrySound.volume);
        }
    }

    private void adjustSfxData(int ptr, int volume) {
        if (_sfxPointer != -1) {
            sdSet(_sfxPointer + 1, _sfxPriority);
            sdSet(_sfxPointer + 3, _sfxVelocity);
            _sfxPointer = -1;
        }

        if (sd(ptr) == 9) {
            return;
        }

        _sfxPointer = ptr;
        _sfxPriority = sd(ptr + 1);
        _sfxVelocity = sd(ptr + 3);

        if (volume != 0xFF) {
            if (_version >= 3) {
                int newVal = (((sd(ptr + 3) + 63) * volume) >> 8) & 0xFF;
                sdSet(ptr + 3, -newVal + 63);
                sdSet(ptr + 1, ((sd(ptr + 1) * volume) >> 8) & 0xFF);
            } else {
                int newVal = ((_sfxVelocity << 2) ^ 0xFF) * volume;
                sdSet(ptr + 3, (newVal >> 10) ^ 0x3F);
                sdSet(ptr + 1, newVal >> 11);
            }
        }
    }

    private void executePrograms() {
        if (_syncJumpMask != 0) {
            for (_curChannel = 9; _curChannel >= 0; --_curChannel) {
                if ((_syncJumpMask & (1 << _curChannel)) != 0 && _channels[_curChannel].dataptr != -1 && !_channels[_curChannel].lock) {
                    break;
                }
            }
            if (_curChannel < 0) {
                for (_curChannel = 9; _curChannel >= 0; --_curChannel) {
                    if ((_syncJumpMask & (1 << _curChannel)) != 0) {
                        _channels[_curChannel].lock = false;
                    }
                }
            }
        }

        for (_curChannel = 9; _curChannel >= 0; --_curChannel) {
            Channel channel = _channels[_curChannel];

            if (channel.dataptr == -1) {
                continue;
            }
            if (channel.lock && (_syncJumpMask & (1 << _curChannel)) != 0) {
                continue;
            }

            _curRegOffset = _curChannel == 9 ? 0 : _regOffset[_curChannel];

            if (channel.tempoReset != 0) {
                channel.tempo = _tempo;
            }

            int result = 1;
            channel.timer = advance(channel.timer, channel.tempo);
            if (advWrapped) {
                channel.duration = (channel.duration - 1) & 0xff;
                if (channel.duration != 0) {
                    if (channel.duration == channel.spacing2) {
                        noteOff(channel);
                    }
                    if (channel.duration == channel.spacing1 && _curChannel != 9) {
                        noteOff(channel);
                    }
                } else {
                    result = 0;
                }
            }

            while (result == 0 && channel.dataptr != -1) {
                int opcode = 0xFF;
                if (checkDataOffset(channel.dataptr, 1) != -1) {
                    opcode = sd(channel.dataptr);
                    channel.dataptr++;
                }

                if ((opcode & 0x80) != 0) {
                    opcode = clip(opcode & 0x7F, 0, PARSER_SIZE - 1);
                    int nvals = PARSER_VALUES[opcode];
                    if (checkDataOffset(channel.dataptr, nvals) == -1) {
                        result = update_stopChannel(channel, channel.dataptr);
                        break;
                    }
                    int values = channel.dataptr;
                    channel.dataptr += nvals;
                    result = parserOpcode(opcode, channel, values);
                } else {
                    if (checkDataOffset(channel.dataptr, 1) == -1) {
                        result = update_stopChannel(channel, channel.dataptr);
                        break;
                    }
                    int duration = sd(channel.dataptr);
                    channel.dataptr++;
                    setupNote(opcode, channel, false);
                    noteOn(channel);
                    setupDuration(duration, channel);
                    result = duration != 0 ? 1 : 0;
                }
            }

            if (result == 1) {
                if (channel.primaryEffect == EFF_SLIDE) {
                    primaryEffectSlide(channel);
                } else if (channel.primaryEffect == EFF_VIBRATO) {
                    primaryEffectVibrato(channel);
                }
                if (channel.secondaryEffect == SEFF_1) {
                    secondaryEffect1(channel);
                }
            }
        }
    }

    private void resetAdLibState() {
        _rnd = 0x1234;
        writeOPL(0x01, 0x20);
        writeOPL(0x08, 0x00);
        writeOPL(0xBD, 0x00);

        initChannel(_channels[9]);
        for (int loop = 8; loop >= 0; loop--) {
            writeOPL(0x40 + _regOffset[loop], 0x3F);
            writeOPL(0x43 + _regOffset[loop], 0x3F);
            initChannel(_channels[loop]);
        }
    }

    private void initChannel(Channel channel) {
        channel.reset();
    }

    private void noteOff(Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        if (_rhythmSectionBits != 0 && _curChannel >= 6) {
            return;
        }
        channel.regBx &= 0xDF;
        writeOPL(0xB0 + _curChannel, channel.regBx);
    }

    private void initAdlibChannel(int chan) {
        if (chan >= 9) {
            return;
        }
        if (_rhythmSectionBits != 0 && chan >= 6) {
            return;
        }
        int offset = _regOffset[chan];
        writeOPL(0x60 + offset, 0xFF);
        writeOPL(0x63 + offset, 0xFF);
        writeOPL(0x80 + offset, 0xFF);
        writeOPL(0x83 + offset, 0xFF);
        writeOPL(0xB0 + chan, 0x00);
        writeOPL(0xB0 + chan, 0x20);
    }

    private int getRandomNr() {
        _rnd = (_rnd + 0x9248) & 0xffff;
        int lowBits = _rnd & 7;
        _rnd = ((_rnd >> 3) | (lowBits << 13)) & 0xffff;
        return _rnd;
    }

    private void setupDuration(int duration, Channel channel) {
        if (channel.durationRandomness != 0) {
            channel.duration = (duration + (getRandomNr() & channel.durationRandomness)) & 0xff;
            return;
        }
        if (channel.fractionalSpacing != 0) {
            channel.spacing2 = ((duration >> 3) * channel.fractionalSpacing) & 0xff;
        }
        channel.duration = duration & 0xff;
    }

    private void setupNote(int rawNote, Channel channel, boolean flag) {
        if (_curChannel >= 9) {
            return;
        }
        channel.rawNote = rawNote & 0xff;

        int note = (byte) ((rawNote & 0x0F) + channel.baseNote);
        int octave = (((rawNote & 0xff) + channel.baseOctave) >> 4) & 0x0F;

        if (note >= 12) {
            octave += note / 12;
            note %= 12;
        } else if (note < 0) {
            int octaves = -(note + 1) / 12 + 1;
            octave -= octaves;
            note += 12 * octaves;
        }

        int freq = _freqTable[note] + channel.baseFreq;

        if (channel.pitchBend != 0 || flag) {
            int indexNote = clip(rawNote & 0x0F, 0, 11);
            if (channel.pitchBend >= 0) {
                freq += _pitchBendTables[indexNote + 2][clip(channel.pitchBend, 0, 31)];
            } else {
                freq -= _pitchBendTables[indexNote][clip(-channel.pitchBend, 0, 31)];
            }
        }

        octave = clip(octave, 0, 7) << 2;

        channel.regAx = freq & 0xFF;
        channel.regBx = (channel.regBx & 0x20) | octave | ((freq >> 8) & 0x03);

        writeOPL(0xA0 + _curChannel, channel.regAx);
        writeOPL(0xB0 + _curChannel, channel.regBx);
    }

    private void setupInstrument(int regOffset, int dataptr, Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        if (checkDataOffset(dataptr, 11) == -1) {
            return;
        }

        writeOPL(0x20 + regOffset, sd(dataptr++));
        writeOPL(0x23 + regOffset, sd(dataptr++));

        int temp = sd(dataptr++);
        writeOPL(0xC0 + _curChannel, temp);
        channel.twoChan = temp & 1;

        writeOPL(0xE0 + regOffset, sd(dataptr++));
        writeOPL(0xE3 + regOffset, sd(dataptr++));

        channel.opLevel1 = sd(dataptr++);
        channel.opLevel2 = sd(dataptr++);

        writeOPL(0x40 + regOffset, calculateOpLevel1(channel));
        writeOPL(0x43 + regOffset, calculateOpLevel2(channel));

        writeOPL(0x60 + regOffset, sd(dataptr++));
        writeOPL(0x63 + regOffset, sd(dataptr++));

        writeOPL(0x80 + regOffset, sd(dataptr++));
        writeOPL(0x83 + regOffset, sd(dataptr));
    }

    private void noteOn(Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        channel.regBx |= 0x20;
        writeOPL(0xB0 + _curChannel, channel.regBx);

        int shift = 9 - clip(channel.vibratoStepRange, 0, 9);
        int freq = ((channel.regBx << 8) | channel.regAx) & 0x3FF;
        channel.vibratoStep = (freq >> shift) & 0xFF;
        channel.vibratoDelayCountdown = channel.vibratoDelay;
    }

    private void adjustVolume(Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        writeOPL(0x43 + _regOffset[_curChannel], calculateOpLevel2(channel));
        if (channel.twoChan != 0) {
            writeOPL(0x40 + _regOffset[_curChannel], calculateOpLevel1(channel));
        }
    }

    private void primaryEffectSlide(Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        channel.slideTimer = advance(channel.slideTimer, channel.slideTempo);
        if (!advWrapped) {
            return;
        }

        int freq = ((channel.regBx & 0x03) << 8) | channel.regAx;
        int octave = channel.regBx & 0x1C;
        int note_on = channel.regBx & 0x20;

        freq += clip((short) channel.slideStep, -0x3FF, 0x3FF);

        if ((short) channel.slideStep >= 0 && freq >= 734) {
            freq >>= 1;
            if ((freq & 0x3FF) == 0) {
                ++freq;
            }
            octave += 4;
        } else if ((short) channel.slideStep < 0 && freq < 388) {
            if (freq < 0) {
                freq = 0;
            }
            freq <<= 1;
            if ((freq & 0x3FF) == 0) {
                --freq;
            }
            octave -= 4;
        }

        channel.regAx = freq & 0xFF;
        channel.regBx = note_on | (octave & 0x1C) | ((freq >> 8) & 0x03);

        writeOPL(0xA0 + _curChannel, channel.regAx);
        writeOPL(0xB0 + _curChannel, channel.regBx);
    }

    private void primaryEffectVibrato(Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        if (channel.vibratoDelayCountdown != 0) {
            --channel.vibratoDelayCountdown;
            return;
        }

        channel.vibratoTimer = advance(channel.vibratoTimer, channel.vibratoTempo);
        if (advWrapped) {
            channel.vibratoStepsCountdown = (channel.vibratoStepsCountdown - 1) & 0xff;
            if (channel.vibratoStepsCountdown == 0) {
                channel.vibratoStep = (-(byte) channel.vibratoStep) & 0xff;
                channel.vibratoStepsCountdown = channel.vibratoNumSteps;
            }

            int freq = ((channel.regBx << 8) | channel.regAx) & 0x3FF;
            freq += (byte) channel.vibratoStep;

            channel.regAx = freq & 0xFF;
            channel.regBx = (channel.regBx & 0xFC) | ((freq >> 8) & 0xff);

            writeOPL(0xA0 + _curChannel, channel.regAx);
            writeOPL(0xB0 + _curChannel, channel.regBx);
        }
    }

    private void secondaryEffect1(Channel channel) {
        if (_curChannel >= 9) {
            return;
        }
        channel.secondaryEffectTimer = advance(channel.secondaryEffectTimer, channel.secondaryEffectTempo);
        if (advWrapped) {
            channel.secondaryEffectPos = (byte) (channel.secondaryEffectPos - 1);
            if (channel.secondaryEffectPos < 0) {
                channel.secondaryEffectPos = channel.secondaryEffectSize;
            }
            writeOPL(channel.secondaryEffectRegbase + _curRegOffset,
                    sd(channel.secondaryEffectData + channel.secondaryEffectPos));
        }
    }

    private int calculateOpLevel1(Channel channel) {
        int value = channel.opLevel1 & 0x3F;

        if (channel.twoChan != 0) {
            value += channel.opExtraLevel1;
            value += channel.opExtraLevel2;

            int level3 = (channel.opExtraLevel3 ^ 0x3F) * channel.volumeModifier;
            if (level3 != 0) {
                level3 += 0x3F;
                level3 >>= 8;
            }
            value += level3 ^ 0x3F;
        }

        value = clip(value & 0xff, 0, 0x3F);
        if (channel.volumeModifier == 0) {
            value = 0x3F;
        }
        return value | (channel.opLevel1 & 0xC0);
    }

    private int calculateOpLevel2(Channel channel) {
        int value = channel.opLevel2 & 0x3F;

        value += channel.opExtraLevel1;
        value += channel.opExtraLevel2;

        int level3 = (channel.opExtraLevel3 ^ 0x3F) * channel.volumeModifier;
        if (level3 != 0) {
            level3 += 0x3F;
            level3 >>= 8;
        }
        value += level3 ^ 0x3F;

        value = clip(value & 0xff, 0, 0x3F);
        if (channel.volumeModifier == 0) {
            value = 0x3F;
        }
        return value | (channel.opLevel2 & 0xC0);
    }

    private int checkValue(int val) {
        return clip(val, 0, 0x3F);
    }

    // ---- parser opcode dispatch ----

    private int parserOpcode(int op, Channel channel, int values) {
        switch (op) {
        case 0: return update_setRepeat(channel, values);
        case 1: return update_checkRepeat(channel, values);
        case 2: return update_setupProgram(channel, values);
        case 3: return update_setNoteSpacing(channel, values);
        case 4: return update_jump(channel, values);
        case 5: return update_jumpToSubroutine(channel, values);
        case 6: return update_returnFromSubroutine(channel, values);
        case 7: return update_setBaseOctave(channel, values);
        case 9: return update_playRest(channel, values);
        case 10: return update_writeAdLib(channel, values);
        case 11: return update_setupNoteAndDuration(channel, values);
        case 12: return update_setBaseNote(channel, values);
        case 13: return update_setupSecondaryEffect1(channel, values);
        case 14: return update_stopOtherChannel(channel, values);
        case 15: return update_waitForEndOfProgram(channel, values);
        case 16: return update_setupInstrument(channel, values);
        case 17: return update_setupPrimaryEffectSlide(channel, values);
        case 18: return update_removePrimaryEffectSlide(channel, values);
        case 19: return update_setBaseFreq(channel, values);
        case 21: return update_setupPrimaryEffectVibrato(channel, values);
        case 26: return update_setPriority(channel, values);
        case 28: return update_setBeat(channel, values);
        case 29: return update_waitForNextBeat(channel, values);
        case 30: return update_setExtraLevel1(channel, values);
        case 32: return update_setupDuration(channel, values);
        case 33: return update_playNote(channel, values);
        case 36: return update_setFractionalNoteSpacing(channel, values);
        case 38: return update_setTempo(channel, values);
        case 39: return update_removeSecondaryEffect1(channel, values);
        case 41: return update_setChannelTempo(channel, values);
        case 43: return update_setExtraLevel3(channel, values);
        case 44: return update_setExtraLevel2(channel, values);
        case 45: return update_changeExtraLevel2(channel, values);
        case 46: return update_setAMDepth(channel, values);
        case 47: return update_setVibratoDepth(channel, values);
        case 48: return update_changeExtraLevel1(channel, values);
        case 51: return update_clearChannel(channel, values);
        case 53: return update_changeNoteRandomly(channel, values);
        case 54: return update_removePrimaryEffectVibrato(channel, values);
        case 57: return update_pitchBend(channel, values);
        case 58: return update_resetToGlobalTempo(channel, values);
        case 59: return update_nop(channel, values);
        case 60: return update_setDurationRandomness(channel, values);
        case 61: return update_changeChannelTempo(channel, values);
        case 63: return updateCallback46(channel, values);
        case 64: return update_nop(channel, values);
        case 65: return update_setupRhythmSection(channel, values);
        case 66: return update_playRhythmSection(channel, values);
        case 67: return update_removeRhythmSection(channel, values);
        case 68: return update_setRhythmLevel2(channel, values);
        case 69: return update_changeRhythmLevel1(channel, values);
        case 70: return update_setRhythmLevel1(channel, values);
        case 71: return update_setSoundTrigger(channel, values);
        case 72: return update_setTempoReset(channel, values);
        case 73: return updateCallback56(channel, values);
        default: return update_stopChannel(channel, values); // 8,20,22-25,27,31,34,35,37,40,42,49,50,52,55,56,62,74
        }
    }

    private int update_setRepeat(Channel channel, int values) {
        channel.repeatCounter = sd(values);
        return 0;
    }

    private int update_checkRepeat(Channel channel, int values) {
        channel.repeatCounter = (channel.repeatCounter - 1) & 0xff;
        if (channel.repeatCounter != 0) {
            int add = (short) sdLe16(values);
            int p = checkDataOffset(channel.dataptr, add);
            if (p != -1) {
                channel.dataptr = p;
            }
        }
        return 0;
    }

    private int update_setupProgram(Channel channel, int values) {
        if (sd(values) == 0xFF) {
            return 0;
        }
        int ptr = getProgram(sd(values));
        if (checkDataOffset(ptr, 2) == -1) {
            return 0;
        }
        int chan = sd(ptr++);
        int priority = sd(ptr++);
        if (chan > 9) {
            return 0;
        }
        Channel channel2 = _channels[chan];
        if (priority >= channel2.priority) {
            int dataptrBackUp = channel.dataptr;
            _programStartTimeout = 2;
            initChannel(channel2);
            channel2.priority = priority;
            channel2.dataptr = ptr;
            channel2.tempo = 0xFF;
            channel2.timer = 0xFF;
            channel2.duration = 1;
            channel2.volumeModifier = chan <= 5 ? _musicVolume : _sfxVolume;
            initAdlibChannel(chan);
            channel.dataptr = dataptrBackUp;
        }
        return 0;
    }

    private int update_setNoteSpacing(Channel channel, int values) {
        channel.spacing1 = sd(values);
        return 0;
    }

    private int update_jump(Channel channel, int values) {
        int add = (short) sdLe16(values);
        if (_version == 1) {
            channel.dataptr = checkDataOffset(0, add - 191);
        } else {
            channel.dataptr = checkDataOffset(channel.dataptr, add);
        }
        if (channel.dataptr == -1) {
            return update_stopChannel(channel, values);
        }
        int idx = channelIndex(channel);
        if ((_syncJumpMask & (1 << idx)) != 0) {
            channel.lock = true;
        }
        if (add < 0) {
            channel.repeating = true;
        }
        return 0;
    }

    private int update_jumpToSubroutine(Channel channel, int values) {
        int add = (short) sdLe16(values);
        if (channel.dataptrStackPos >= channel.dataptrStack.length) {
            return 0;
        }
        channel.dataptrStack[channel.dataptrStackPos++] = channel.dataptr;
        if (_version < 3) {
            channel.dataptr = checkDataOffset(0, add - 191);
        } else {
            channel.dataptr = checkDataOffset(channel.dataptr, add);
        }
        if (channel.dataptr == -1) {
            channel.dataptr = channel.dataptrStack[--channel.dataptrStackPos];
        }
        return 0;
    }

    private int update_returnFromSubroutine(Channel channel, int values) {
        if (channel.dataptrStackPos == 0) {
            return update_stopChannel(channel, values);
        }
        channel.dataptr = channel.dataptrStack[--channel.dataptrStackPos];
        return 0;
    }

    private int update_setBaseOctave(Channel channel, int values) {
        channel.baseOctave = (byte) sd(values);
        return 0;
    }

    private int update_stopChannel(Channel channel, int values) {
        channel.priority = 0;
        if (_curChannel != 9) {
            noteOff(channel);
        }
        channel.dataptr = -1;
        return 2;
    }

    private int update_playRest(Channel channel, int values) {
        setupDuration(sd(values), channel);
        noteOff(channel);
        return sd(values) != 0 ? 1 : 0;
    }

    private int update_writeAdLib(Channel channel, int values) {
        writeOPL(sd(values), sd(values + 1));
        return 0;
    }

    private int update_setupNoteAndDuration(Channel channel, int values) {
        setupNote(sd(values), channel, false);
        setupDuration(sd(values + 1), channel);
        return sd(values + 1) != 0 ? 1 : 0;
    }

    private int update_setBaseNote(Channel channel, int values) {
        channel.baseNote = (byte) sd(values);
        return 0;
    }

    private int update_setupSecondaryEffect1(Channel channel, int values) {
        channel.secondaryEffectTimer = channel.secondaryEffectTempo = sd(values);
        channel.secondaryEffectSize = channel.secondaryEffectPos = (byte) sd(values + 1);
        channel.secondaryEffectRegbase = sd(values + 2);
        channel.secondaryEffectData = sdLe16(values + 3) - 191;
        channel.secondaryEffect = SEFF_1;

        int start = channel.secondaryEffectData + channel.secondaryEffectSize;
        if (start < 0 || start >= _soundDataSize) {
            channel.secondaryEffect = EFF_NONE;
        }
        return 0;
    }

    private int update_stopOtherChannel(Channel channel, int values) {
        if (sd(values) > 9) {
            return 0;
        }
        int dataptrBackUp = channel.dataptr;
        Channel channel2 = _channels[sd(values)];
        channel2.duration = 0;
        channel2.priority = 0;
        channel2.dataptr = -1;
        channel.dataptr = dataptrBackUp;
        return 0;
    }

    private int update_waitForEndOfProgram(Channel channel, int values) {
        int ptr = getProgram(sd(values));
        if (ptr == -1) {
            return 0;
        }
        int chan = sd(ptr);
        if (chan > 9 || _channels[chan].dataptr == -1) {
            return 0;
        }
        if (_channels[chan].repeating) {
            channel.repeating = true;
        }
        channel.dataptr -= 2;
        return 2;
    }

    private int update_setupInstrument(Channel channel, int values) {
        int instrument = getInstrument(sd(values));
        if (instrument == -1) {
            return 0;
        }
        setupInstrument(_curRegOffset, instrument, channel);
        return 0;
    }

    private int update_setupPrimaryEffectSlide(Channel channel, int values) {
        channel.slideTempo = sd(values);
        channel.slideStep = (short) sdBe16(values + 1);
        channel.primaryEffect = EFF_SLIDE;
        channel.slideTimer = 0xFF;
        return 0;
    }

    private int update_removePrimaryEffectSlide(Channel channel, int values) {
        channel.primaryEffect = EFF_NONE;
        channel.slideStep = 0;
        return 0;
    }

    private int update_setBaseFreq(Channel channel, int values) {
        channel.baseFreq = sd(values);
        return 0;
    }

    private int update_setupPrimaryEffectVibrato(Channel channel, int values) {
        channel.vibratoTempo = sd(values);
        channel.vibratoStepRange = sd(values + 1);
        channel.vibratoStepsCountdown = sd(values + 2) + 1;
        channel.vibratoNumSteps = (sd(values + 2) << 1) & 0xff;
        channel.vibratoDelay = sd(values + 3);
        channel.primaryEffect = EFF_VIBRATO;
        return 0;
    }

    private int update_setPriority(Channel channel, int values) {
        channel.priority = sd(values);
        return 0;
    }

    private int update_setBeat(Channel channel, int values) {
        _beatDivider = _beatDivCnt = sd(values) >> 1;
        _callbackTimer = 0xFF;
        _beatCounter = _beatWaiting = 0;
        return 0;
    }

    private int update_waitForNextBeat(Channel channel, int values) {
        if ((_beatCounter & sd(values)) != 0 && _beatWaiting != 0) {
            _beatWaiting = 0;
            return 0;
        }
        if ((_beatCounter & sd(values)) == 0) {
            ++_beatWaiting;
        }
        channel.dataptr -= 2;
        channel.duration = 1;
        return 2;
    }

    private int update_setExtraLevel1(Channel channel, int values) {
        channel.opExtraLevel1 = sd(values);
        adjustVolume(channel);
        return 0;
    }

    private int update_setupDuration(Channel channel, int values) {
        setupDuration(sd(values), channel);
        return sd(values) != 0 ? 1 : 0;
    }

    private int update_playNote(Channel channel, int values) {
        setupDuration(sd(values), channel);
        noteOn(channel);
        return sd(values) != 0 ? 1 : 0;
    }

    private int update_setFractionalNoteSpacing(Channel channel, int values) {
        channel.fractionalSpacing = sd(values) & 7;
        return 0;
    }

    private int update_setTempo(Channel channel, int values) {
        _tempo = sd(values);
        return 0;
    }

    private int update_removeSecondaryEffect1(Channel channel, int values) {
        channel.secondaryEffect = EFF_NONE;
        return 0;
    }

    private int update_setChannelTempo(Channel channel, int values) {
        channel.tempo = sd(values);
        return 0;
    }

    private int update_setExtraLevel3(Channel channel, int values) {
        channel.opExtraLevel3 = sd(values);
        return 0;
    }

    private int update_setExtraLevel2(Channel channel, int values) {
        if (sd(values) > 9) {
            return 0;
        }
        int channelBackUp = _curChannel;
        _curChannel = sd(values);
        Channel channel2 = _channels[_curChannel];
        channel2.opExtraLevel2 = sd(values + 1);
        adjustVolume(channel2);
        _curChannel = channelBackUp;
        return 0;
    }

    private int update_changeExtraLevel2(Channel channel, int values) {
        if (sd(values) > 9) {
            return 0;
        }
        int channelBackUp = _curChannel;
        _curChannel = sd(values);
        Channel channel2 = _channels[_curChannel];
        channel2.opExtraLevel2 = (channel2.opExtraLevel2 + sd(values + 1)) & 0xff;
        adjustVolume(channel2);
        _curChannel = channelBackUp;
        return 0;
    }

    private int update_setAMDepth(Channel channel, int values) {
        if ((sd(values) & 1) != 0) {
            _vibratoAndAMDepthBits |= 0x80;
        } else {
            _vibratoAndAMDepthBits &= 0x7F;
        }
        writeOPL(0xBD, _vibratoAndAMDepthBits);
        return 0;
    }

    private int update_setVibratoDepth(Channel channel, int values) {
        if ((sd(values) & 1) != 0) {
            _vibratoAndAMDepthBits |= 0x40;
        } else {
            _vibratoAndAMDepthBits &= 0xBF;
        }
        writeOPL(0xBD, _vibratoAndAMDepthBits);
        return 0;
    }

    private int update_changeExtraLevel1(Channel channel, int values) {
        channel.opExtraLevel1 = (channel.opExtraLevel1 + sd(values)) & 0xff;
        adjustVolume(channel);
        return 0;
    }

    private int update_clearChannel(Channel channel, int values) {
        if (sd(values) > 9) {
            return 0;
        }
        int channelBackUp = _curChannel;
        _curChannel = sd(values);
        int dataptrBackUp = channel.dataptr;
        Channel channel2 = _channels[_curChannel];
        channel2.duration = channel2.priority = 0;
        channel2.dataptr = -1;
        channel2.opExtraLevel2 = 0;
        if (_curChannel != 9) {
            int regOff = _regOffset[_curChannel];
            writeOPL(0xC0 + _curChannel, 0x00);
            writeOPL(0x43 + regOff, 0x3F);
            writeOPL(0x83 + regOff, 0xFF);
            writeOPL(0xB0 + _curChannel, 0x00);
        }
        _curChannel = channelBackUp;
        channel.dataptr = dataptrBackUp;
        return 0;
    }

    private int update_changeNoteRandomly(Channel channel, int values) {
        if (_curChannel >= 9) {
            return 0;
        }
        int mask = sdBe16(values);
        int note = ((channel.regBx & 0x1F) << 8) | channel.regAx;
        note += mask & getRandomNr();
        note |= ((channel.regBx & 0x20) << 8);
        writeOPL(0xA0 + _curChannel, note & 0xFF);
        writeOPL(0xB0 + _curChannel, (note & 0xFF00) >> 8);
        return 0;
    }

    private int update_removePrimaryEffectVibrato(Channel channel, int values) {
        channel.primaryEffect = EFF_NONE;
        return 0;
    }

    private int update_pitchBend(Channel channel, int values) {
        channel.pitchBend = (byte) sd(values);
        setupNote(channel.rawNote, channel, true);
        return 0;
    }

    private int update_resetToGlobalTempo(Channel channel, int values) {
        channel.tempo = _tempo;
        return 0;
    }

    private int update_nop(Channel channel, int values) {
        return 0;
    }

    private int update_setDurationRandomness(Channel channel, int values) {
        channel.durationRandomness = sd(values);
        return 0;
    }

    private int update_changeChannelTempo(Channel channel, int values) {
        channel.tempo = clip(channel.tempo + (byte) sd(values), 1, 255);
        return 0;
    }

    private int updateCallback46(Channel channel, int values) {
        int entry = sd(values + 1);
        if (entry + 2 > _unkTable2.length) {
            return 0;
        }
        _tablePtr1Row = entry;
        _tablePtr2Row = entry + 1;
        if (sd(values) == 2) {
            writeOPL(0xA0, _unkTable2[_tablePtr2Row][0]);
        }
        return 0;
    }

    private int update_setupRhythmSection(Channel channel, int values) {
        int channelBackUp = _curChannel;
        int regOffsetBackUp = _curRegOffset;

        _curChannel = 6;
        _curRegOffset = _regOffset[6];
        int instrument = getInstrument(sd(values));
        if (instrument != -1) {
            setupInstrument(_curRegOffset, instrument, channel);
        }
        _opLevelBD = channel.opLevel2;

        _curChannel = 7;
        _curRegOffset = _regOffset[7];
        instrument = getInstrument(sd(values + 1));
        if (instrument != -1) {
            setupInstrument(_curRegOffset, instrument, channel);
        }
        _opLevelHH = channel.opLevel1;
        _opLevelSD = channel.opLevel2;

        _curChannel = 8;
        _curRegOffset = _regOffset[8];
        instrument = getInstrument(sd(values + 2));
        if (instrument != -1) {
            setupInstrument(_curRegOffset, instrument, channel);
        }
        _opLevelTT = channel.opLevel1;
        _opLevelCY = channel.opLevel2;

        _channels[6].regBx = sd(values + 3) & 0x2F;
        writeOPL(0xB6, _channels[6].regBx);
        writeOPL(0xA6, sd(values + 4));

        _channels[7].regBx = sd(values + 5) & 0x2F;
        writeOPL(0xB7, _channels[7].regBx);
        writeOPL(0xA7, sd(values + 6));

        _channels[8].regBx = sd(values + 7) & 0x2F;
        writeOPL(0xB8, _channels[8].regBx);
        writeOPL(0xA8, sd(values + 8));

        _rhythmSectionBits = 0x20;

        _curRegOffset = regOffsetBackUp;
        _curChannel = channelBackUp;
        return 0;
    }

    private int update_playRhythmSection(Channel channel, int values) {
        writeOPL(0xBD, (_rhythmSectionBits & ~(sd(values) & 0x1F)) | 0x20);
        _rhythmSectionBits |= sd(values);
        writeOPL(0xBD, _vibratoAndAMDepthBits | 0x20 | _rhythmSectionBits);
        return 0;
    }

    private int update_removeRhythmSection(Channel channel, int values) {
        _rhythmSectionBits = 0;
        writeOPL(0xBD, _vibratoAndAMDepthBits);
        return 0;
    }

    private int update_setRhythmLevel2(Channel channel, int values) {
        int ops = sd(values), v = sd(values + 1);
        if ((ops & 1) != 0) {
            _opExtraLevel2HH = v;
            writeOPL(0x51, checkValue(v + _opLevelHH + _opExtraLevel1HH + _opExtraLevel2HH));
        }
        if ((ops & 2) != 0) {
            _opExtraLevel2CY = v;
            writeOPL(0x55, checkValue(v + _opLevelCY + _opExtraLevel1CY + _opExtraLevel2CY));
        }
        if ((ops & 4) != 0) {
            _opExtraLevel2TT = v;
            writeOPL(0x52, checkValue(v + _opLevelTT + _opExtraLevel1TT + _opExtraLevel2TT));
        }
        if ((ops & 8) != 0) {
            _opExtraLevel2SD = v;
            writeOPL(0x54, checkValue(v + _opLevelSD + _opExtraLevel1SD + _opExtraLevel2SD));
        }
        if ((ops & 16) != 0) {
            _opExtraLevel2BD = v;
            writeOPL(0x53, checkValue(v + _opLevelBD + _opExtraLevel1BD + _opExtraLevel2BD));
        }
        return 0;
    }

    private int update_changeRhythmLevel1(Channel channel, int values) {
        int ops = sd(values), v = sd(values + 1);
        if ((ops & 1) != 0) {
            _opExtraLevel1HH = checkValue(v + _opLevelHH + _opExtraLevel1HH + _opExtraLevel2HH);
            writeOPL(0x51, _opExtraLevel1HH);
        }
        if ((ops & 2) != 0) {
            _opExtraLevel1CY = checkValue(v + _opLevelCY + _opExtraLevel1CY + _opExtraLevel2CY);
            writeOPL(0x55, _opExtraLevel1CY);
        }
        if ((ops & 4) != 0) {
            _opExtraLevel1TT = checkValue(v + _opLevelTT + _opExtraLevel1TT + _opExtraLevel2TT);
            writeOPL(0x52, _opExtraLevel1TT);
        }
        if ((ops & 8) != 0) {
            _opExtraLevel1SD = checkValue(v + _opLevelSD + _opExtraLevel1SD + _opExtraLevel2SD);
            writeOPL(0x54, _opExtraLevel1SD);
        }
        if ((ops & 16) != 0) {
            _opExtraLevel1BD = checkValue(v + _opLevelBD + _opExtraLevel1BD + _opExtraLevel2BD);
            writeOPL(0x53, _opExtraLevel1BD);
        }
        return 0;
    }

    private int update_setRhythmLevel1(Channel channel, int values) {
        int ops = sd(values), v = sd(values + 1);
        if ((ops & 1) != 0) {
            _opExtraLevel1HH = v;
            writeOPL(0x51, checkValue(v + _opLevelHH + _opExtraLevel2HH));
        }
        if ((ops & 2) != 0) {
            _opExtraLevel1CY = v;
            writeOPL(0x55, checkValue(v + _opLevelCY + _opExtraLevel2CY));
        }
        if ((ops & 4) != 0) {
            _opExtraLevel1TT = v;
            writeOPL(0x52, checkValue(v + _opLevelTT + _opExtraLevel2TT));
        }
        if ((ops & 8) != 0) {
            _opExtraLevel1SD = v;
            writeOPL(0x54, checkValue(v + _opLevelSD + _opExtraLevel2SD));
        }
        if ((ops & 16) != 0) {
            _opExtraLevel1BD = v;
            writeOPL(0x53, checkValue(v + _opLevelBD + _opExtraLevel2BD));
        }
        return 0;
    }

    private int update_setSoundTrigger(Channel channel, int values) {
        _soundTrigger = sd(values);
        return 0;
    }

    private int update_setTempoReset(Channel channel, int values) {
        channel.tempoReset = sd(values);
        return 0;
    }

    private int updateCallback56(Channel channel, int values) {
        channel.unk39 = sd(values);
        channel.unk40 = sd(values + 1);
        return 0;
    }

    private int channelIndex(Channel channel) {
        for (int i = 0; i < _channels.length; i++) {
            if (_channels[i] == channel) {
                return i;
            }
        }
        return 0;
    }
}
