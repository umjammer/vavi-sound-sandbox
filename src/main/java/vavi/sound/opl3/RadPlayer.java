/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2007 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 *
 * Aside from the wrapper, this is a direct port of the public domain RAD2
 * replayer by Shayde/Reality (updated to also support the original RAD v1
 * format), as used by AdPlug's rad2.cpp.
 */

package vavi.sound.opl3;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * Reality Adlib Tracker Player (RAD v1 and v2).
 *
 * @author Shayde/Reality
 */
public class RadPlayer extends Opl3Player {

    private static final Logger logger = getLogger(RadPlayer.class.getName());

    private static final String ID = "RAD by REALiTY!!";

    // command effects
    private static final int cmPortamentoUp = 0x1;
    private static final int cmPortamentoDwn = 0x2;
    private static final int cmToneSlide = 0x3;
    private static final int cmToneVolSlide = 0x5;
    private static final int cmVolSlide = 0xA;
    private static final int cmSetVol = 0xC;
    private static final int cmJumpToLine = 0xD;
    private static final int cmSetSpeed = 0xF;
    private static final int cmIgnore = 'I' - 55;
    private static final int cmMultiplier = 'M' - 55;
    private static final int cmRiff = 'R' - 55;
    private static final int cmTranspose = 'T' - 55;
    private static final int cmFeedback = 'U' - 55;
    private static final int cmVolume = 'V' - 55;

    // effect source
    private static final int SNone = 0, SRiff = 1, SIRiff = 2;

    // key flags
    private static final int fKeyOn = 1;
    private static final int fKeyOff = 1 << 1;
    private static final int fKeyedOn = 1 << 2;

    private static final int kTracks = 100;
    private static final int kChannels = 9;
    private static final int kTrackLines = 64;
    private static final int kRiffTracks = 10;
    private static final int kInstruments = 127;

    private static final int[] NoteSize = {0, 2, 1, 3, 1, 3, 2, 4};
    private static final int[] ChanOffsets3 = {0, 1, 2, 0x100, 0x101, 0x102, 6, 7, 8};
    private static final int[] Chn2Offsets3 = {3, 4, 5, 0x103, 0x104, 0x105, 0x106, 0x107, 0x108};
    private static final int[] NoteFreq = {0x16b, 0x181, 0x198, 0x1b0, 0x1ca, 0x1e5, 0x202, 0x220, 0x241, 0x263, 0x287, 0x2ae};
    private static final int[][] OpOffsets2 = {
        {0x003, 0x000}, {0x004, 0x001}, {0x005, 0x002},
        {0x00B, 0x008}, {0x00C, 0x009}, {0x00D, 0x00A},
        {0x013, 0x010}, {0x014, 0x011}, {0x015, 0x012}
    };
    private static final int[][] OpOffsets3 = {
        {0x00B, 0x008, 0x003, 0x000}, {0x00C, 0x009, 0x004, 0x001}, {0x00D, 0x00A, 0x005, 0x002},
        {0x10B, 0x108, 0x103, 0x100}, {0x10C, 0x109, 0x104, 0x101}, {0x10D, 0x10A, 0x105, 0x102},
        {0x113, 0x110, 0x013, 0x010}, {0x114, 0x111, 0x014, 0x011}, {0x115, 0x112, 0x015, 0x012}
    };
    private static final boolean[][] AlgCarriers = {
        {true, false, false, false},
        {true, true, false, false},
        {true, false, false, false},
        {true, false, false, true},
        {true, false, true, false},
        {true, false, true, true},
        {true, true, true, true},
    };
    private static final int[] BLANK = {0, 0x3F, 0, 0xF0, 0};

    private static class CInstrument {
        final int[] Feedback = new int[2];
        final int[] Panning = new int[2];
        int Algorithm;
        int Detune;
        int Volume;
        int RiffSpeed;
        int Riff = -1; // offset into tune (-1 == none)
        final int[][] Operators = new int[4][5];
    }

    private static class CEffects {
        int PortSlide;      // s8
        int VolSlide;       // s8
        int ToneSlideFreq;  // u16
        int ToneSlideOct;   // u8
        int ToneSlideSpeed; // u8
        int ToneSlideDir;   // s8
    }

    private static class CRiff {
        final CEffects FX = new CEffects();
        int Track = -1;      // offset (-1 == none)
        int TrackStart = -1;
        int Line;
        int Speed;
        int SpeedCnt;
        int TransposeOctave; // s8
        int TransposeNote;   // s8
        int LastInstrument;
    }

    private static class CChannel {
        int LastInstrument;
        CInstrument Instrument; // null == none
        int Volume;
        int DetuneA;
        int DetuneB;
        int KeyFlags;
        int CurrFreq;    // u16
        int CurrOctave;  // s8
        final CEffects FX = new CEffects();
        final CRiff Riff = new CRiff();
        final CRiff IRiff = new CRiff();
    }

    // ---- player state ----
    private byte[] tune;
    private int version;
    private boolean useOPL3;
    private final CInstrument[] instruments = new CInstrument[kInstruments];
    private int numInstruments;
    private final CChannel[] channels = new CChannel[kChannels];
    private int playTime;
    private final int[] orderMap = new int[4];
    private boolean repeating;
    private float hertz;
    private int orderListOff;
    private final int[] tracks = new int[kTracks];
    private int numTracks;
    private final int[][] riffs = new int[kRiffTracks][kChannels];
    private int track; // offset (-1 == none)
    private boolean initialised;
    private int speed;
    private int orderListSize;
    private int speedCnt;
    private int order;
    private int line;
    private int entrances;
    private int masterVol;
    private int lineJump;
    private final int[] opl3Regs = new int[512];

    // values exported by unpackNote()
    private int noteNum;    // s8
    private int octaveNum;  // s8
    private int instNum;
    private int effectNum;
    private int param;
    private boolean lastNote;
    private int lastInstrumentIO;

    public RadPlayer() {
        for (int i = 0; i < kInstruments; i++) {
            instruments[i] = new CInstrument();
        }
        for (int i = 0; i < kChannels; i++) {
            channels[i] = new CChannel();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Reality ADlib Tracker", "rad");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("RAD");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(16);
            byte[] id = new byte[16];
            int n = bitStream.read(id);
            return n == 16 && ID.equals(new String(id, java.nio.charset.StandardCharsets.ISO_8859_1));
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

    @Override
    public void load(InputStream is) throws IOException {
        byte[] raw = is.readAllBytes();
        // some old tunes have a truncated final note; add an extra empty byte at the end
        tune = Arrays.copyOf(raw, raw.length + 1);

        init();
        if (hertz <= 0) {
            throw new IllegalArgumentException("rad: unsupported version");
        }
logger.log(Level.DEBUG, "version: " + version + ", hertz: " + hertz + ", instruments: " + numInstruments + ", tracks: " + numTracks);
    }

    @Override
    public boolean update() {
        return !radUpdate();
    }

    @Override
    public void rewind(int subSong) {
        stop();
    }

    @Override
    public float getRefresh() {
        return hertz;
    }

    // ---- tune accessors ----

    private int u8(int off) {
        return tune[off] & 0xff;
    }

    private void setOPL3(int reg, int val) {
        opl3Regs[reg] = val & 0xff;
        write(reg >> 8, reg & 0xff, val & 0xff);
    }

    private int getOPL3(int reg) {
        return opl3Regs[reg];
    }

    // ---- Init ----

    private void init() {
        initialised = false;

        int ver = u8(0x10);
        if (ver != 0x10 && ver != 0x21) {
            hertz = -1;
            return;
        }
        version = ver >> 4;
        useOPL3 = version >= 2;

        Arrays.fill(tracks, -1);
        for (int[] r : riffs) {
            Arrays.fill(r, -1);
        }

        int s = 0x11;

        int flags = u8(s++);
        speed = flags & 0x1F;

        hertz = 50.0f;
        if (version >= 2 && (flags & 0x20) != 0) {
            hertz = (float) (u8(s) | (u8(s + 1) << 8)) * 2 / 5;
            s += 2;
        }
        if ((flags & 0x40) != 0) {
            hertz = 18.2f; // slow timer tune
        }

        // skip any description
        if (version >= 2 || (flags & 0x80) != 0) {
            while (u8(s) != 0) {
                s++;
            }
            s++;
        }

        // blank the instrument table
        for (CInstrument inst : instruments) {
            inst.Feedback[0] = inst.Feedback[1] = 0;
            inst.Panning[0] = inst.Panning[1] = 0;
            inst.Algorithm = inst.Detune = inst.Volume = inst.RiffSpeed = 0;
            inst.Riff = -1;
            for (int[] op : inst.Operators) {
                Arrays.fill(op, 0);
            }
        }

        // unpack the instruments
        numInstruments = 0;
        while (true) {
            int instNum = u8(s++);
            if (instNum == 0) {
                break;
            }
            if (instNum > numInstruments) {
                numInstruments = instNum;
            }

            CInstrument inst = instruments[instNum - 1];

            if (version >= 2) {
                int nameLen = u8(s++);
                s += nameLen; // ignore name

                int alg = u8(s++);
                inst.Algorithm = alg & 7;
                inst.Panning[0] = (alg >> 3) & 3;
                inst.Panning[1] = (alg >> 5) & 3;

                if (inst.Algorithm < 7) {
                    int b = u8(s++);
                    inst.Feedback[0] = b & 15;
                    inst.Feedback[1] = b >> 4;

                    b = u8(s++);
                    inst.Detune = b >> 4;
                    inst.RiffSpeed = b & 15;

                    inst.Volume = u8(s++);

                    for (int i = 0; i < 4; i++) {
                        for (int j = 0; j < 5; j++) {
                            inst.Operators[i][j] = u8(s++);
                        }
                    }
                } else {
                    s += 6; // ignore MIDI instrument data
                }

                if ((alg & 0x80) != 0) { // instrument riff
                    int size = u8(s) | (u8(s + 1) << 8);
                    s += 2;
                    inst.Riff = s;
                    s += size;
                } else {
                    inst.Riff = -1;
                }
            } else {
                // version 1 instrument
                inst.Algorithm = u8(s + 8) & 1;
                inst.Panning[0] = 0;
                inst.Panning[1] = 0;
                inst.Feedback[0] = (u8(s + 8) >> 1) & 0x7;
                inst.Feedback[1] = 0;
                inst.Detune = 0;
                inst.RiffSpeed = 0;
                inst.Volume = 64;

                inst.Operators[0][0] = u8(s);
                inst.Operators[1][0] = u8(s + 1);
                inst.Operators[0][1] = u8(s + 2);
                inst.Operators[1][1] = u8(s + 3);
                inst.Operators[0][2] = u8(s + 4);
                inst.Operators[1][2] = u8(s + 5);
                inst.Operators[0][3] = u8(s + 6);
                inst.Operators[1][3] = u8(s + 7);
                inst.Operators[0][4] = u8(s + 9);
                inst.Operators[1][4] = u8(s + 10);
                for (int i = 2; i < 4; i++) {
                    Arrays.fill(inst.Operators[i], 0);
                }
                inst.Riff = -1;
                s += 11;
            }
        }

        // order list
        orderListSize = u8(s++);
        orderListOff = s;
        s += orderListSize;

        // locate the tracks
        numTracks = 0;
        if (version >= 2) {
            while (true) {
                int trackNum = u8(s++);
                if (trackNum >= kTracks) {
                    break;
                }
                if (trackNum + 1 > numTracks) {
                    numTracks = trackNum + 1;
                }
                int size = u8(s) | (u8(s + 1) << 8);
                s += 2;
                tracks[trackNum] = s;
                s += size;
            }
        } else {
            for (int i = 0; i < 32; i++) {
                int pos = u8(s) | (u8(s + 1) << 8);
                s += 2;
                if (pos != 0) {
                    numTracks = i + 1;
                    tracks[i] = pos;
                }
            }
        }

        // locate the riffs
        if (version >= 2) {
            while (true) {
                int riffid = u8(s++);
                int riffnum = riffid >> 4;
                int channum = riffid & 15;
                if (riffnum >= kRiffTracks || channum > kChannels) {
                    break;
                }
                int size = u8(s) | (u8(s + 1) << 8);
                s += 2;
                riffs[riffnum][channum - 1] = s;
                s += size;
            }
        }

        // done parsing, set up for play
        Arrays.fill(opl3Regs, 255);
        stop();
        initialised = true;
    }

    private void stop() {
        // clear all registers
        for (int reg = 0x20; reg < 0xF6; reg++) {
            int val = (reg >= 0x60 && reg < 0xA0) ? 0xFF : 0;
            setOPL3(reg, val);
            setOPL3(reg + 0x100, val);
        }

        setOPL3(1, 0x20);   // allow waveforms
        setOPL3(8, 0);      // no split point
        setOPL3(0xbd, 0);   // no drums, etc.
        setOPL3(0x104, 0);  // everything 2-op by default
        setOPL3(0x105, 1);  // OPL3 mode on

        playTime = 0;
        repeating = false;
        Arrays.fill(orderMap, 0);

        speedCnt = 1;
        order = 0;
        track = getTrack();
        line = 0;
        entrances = 0;
        masterVol = 64;

        for (CChannel chan : channels) {
            chan.LastInstrument = 0;
            chan.Instrument = null;
            chan.Volume = 0;
            chan.DetuneA = 0;
            chan.DetuneB = 0;
            chan.KeyFlags = 0;
            chan.Riff.SpeedCnt = 0;
            chan.IRiff.SpeedCnt = 0;
        }
    }

    private boolean radUpdate() {
        if (!initialised) {
            return false;
        }

        for (int i = 0; i < kChannels; i++) {
            CChannel chan = channels[i];
            tickRiff(i, chan.IRiff, false);
            tickRiff(i, chan.Riff, true);
        }

        playLine();

        for (int i = 0; i < kChannels; i++) {
            CChannel chan = channels[i];
            continueFX(i, chan.IRiff.FX);
            continueFX(i, chan.Riff.FX);
            continueFX(i, chan.FX);
        }

        playTime++;

        return repeating;
    }

    /** unpacks a single RAD note starting at {@code s}, returns the advanced position */
    private int unpackNote(int s) {
        int chanid = u8(s++);

        instNum = 0;
        effectNum = 0;
        param = 0;

        int note = 0;

        if (version >= 2) {
            if ((chanid & 0x40) != 0) {
                int n = u8(s++);
                note = n & 0x7F;
                if ((n & 0x80) != 0) {
                    instNum = lastInstrumentIO;
                }
            }
            if ((chanid & 0x20) != 0) {
                instNum = u8(s++);
                lastInstrumentIO = instNum;
            }
            if ((chanid & 0x10) != 0) {
                effectNum = u8(s++);
                param = u8(s++);
            }
        } else {
            int n = u8(s++);
            note = n & 0x7f;
            if ((n & 0x80) != 0) {
                instNum = 16;
            }
            n = u8(s++);
            instNum |= n >> 4;
            if (instNum != 0) {
                lastInstrumentIO = instNum;
            }
            effectNum = n & 0xf;
            if (effectNum != 0) {
                param = u8(s++);
            }
        }

        noteNum = note & 15;
        octaveNum = note >> 4;
        lastNote = (chanid & 0x80) != 0;
        return s;
    }

    private int getTrack() {
        if (order >= orderListSize) {
            order = 0;
        }

        int trackNum = u8(orderListOff + order);

        if ((trackNum & 0x80) != 0) { // jump marker
            order = trackNum & 0x7F;
            trackNum = u8(orderListOff + order) & 0x7F;
        }

        if (order < 128) {
            int b = order >> 5;
            int bit = 1 << (order & 31);
            if ((orderMap[b] & bit) != 0) {
                repeating = true;
            } else {
                orderMap[b] |= bit;
            }
        }

        return trackNum < kTracks ? tracks[trackNum] : -1;
    }

    private int skipToLine(int trk, int linenum, boolean chanRiff) {
        while (true) {
            int lineid = u8(trk);
            if ((lineid & 0x7F) >= linenum) {
                return trk;
            }
            if ((lineid & 0x80) != 0) {
                break;
            }
            trk++;

            int chanid;
            do {
                chanid = u8(trk++);
                if (version >= 2) {
                    trk += NoteSize[(chanid >> 4) & 7];
                } else if ((u8(trk + 1) & 0xf) != 0) {
                    trk += 3;
                } else {
                    trk += 2;
                }
            } while ((chanid & 0x80) == 0 && !chanRiff);
        }

        return -1;
    }

    private void playLine() {
        speedCnt--;
        if (speedCnt > 0) {
            return;
        }
        speedCnt = speed;

        for (int i = 0; i < kChannels; i++) {
            resetFX(channels[i].FX);
        }

        lineJump = -1;

        int trk = track;
        if (trk >= 0 && (u8(trk) & 0x7F) <= line) {
            int lineid = u8(trk++);

            boolean last;
            do {
                int channum = u8(trk) & 15;
                CChannel chan = channels[channum];
                lastInstrumentIO = chan.LastInstrument;
                trk = unpackNote(trk);
                chan.LastInstrument = lastInstrumentIO;
                last = lastNote;
                playNote(channum, noteNum, octaveNum, instNum, effectNum, param, SNone, 0);
            } while (!last);

            if ((lineid & 0x80) != 0) {
                trk = -1;
            }
            track = trk;
        }

        line++;
        if (line >= kTrackLines || lineJump >= 0) {
            line = lineJump >= 0 ? lineJump : 0;

            order++;
            track = getTrack();
            if (line > 0) {
                track = skipToLine(track, line, false);
            }
        }
    }

    private void playNote(int channum, int notenum, int octave, int instnum, int cmd, int param, int src, int op) {
        CChannel chan = channels[channum];

        if (entrances >= 8) {
            return;
        }
        entrances++;
        try {
            CEffects fx = chan.FX;
            if (src == SRiff) {
                fx = chan.Riff.FX;
            } else if (src == SIRiff) {
                fx = chan.IRiff.FX;
            }

            boolean transposing = false;

            if (cmd == cmToneSlide) {
                // for tone-slides the note is the target
                if (notenum > 0 && notenum <= 12) {
                    fx.ToneSlideOct = octave;
                    fx.ToneSlideFreq = NoteFreq[notenum - 1];
                }
            } else {
                // playing a new instrument?
                if (instnum > 0) {
                    CInstrument oldinst = chan.Instrument;
                    CInstrument inst = instruments[instnum - 1];
                    chan.Instrument = inst;

                    if (inst.Algorithm < 7) {
                        loadInstrumentOPL3(channum);

                        chan.KeyFlags |= fKeyOff | fKeyOn;

                        resetFX(chan.IRiff.FX);

                        if (src != SIRiff || inst != oldinst) {
                            if (inst.Riff >= 0 && inst.RiffSpeed > 0) {
                                chan.IRiff.Track = chan.IRiff.TrackStart = inst.Riff;
                                chan.IRiff.Line = 0;
                                chan.IRiff.Speed = inst.RiffSpeed;
                                chan.IRiff.LastInstrument = 0;

                                if (notenum >= 1 && notenum <= 12) {
                                    chan.IRiff.TransposeOctave = octave;
                                    chan.IRiff.TransposeNote = notenum;
                                    transposing = true;
                                } else {
                                    chan.IRiff.TransposeOctave = 3;
                                    chan.IRiff.TransposeNote = 12;
                                }

                                chan.IRiff.SpeedCnt = 1;
                                tickRiff(channum, chan.IRiff, false);
                            } else {
                                chan.IRiff.SpeedCnt = 0;
                            }
                        }
                    } else {
                        chan.Instrument = null; // ignore MIDI instruments
                    }
                }

                // starting a channel riff?
                if (cmd == cmRiff || cmd == cmTranspose) {
                    resetFX(chan.Riff.FX);

                    int p0 = param / 10;
                    int p1 = param % 10;
                    chan.Riff.Track = p1 > 0 ? riffs[p0][p1 - 1] : -1;
                    if (chan.Riff.Track >= 0) {
                        chan.Riff.TrackStart = chan.Riff.Track;
                        chan.Riff.Line = 0;
                        chan.Riff.Speed = speed;
                        chan.Riff.LastInstrument = 0;

                        if (cmd == cmTranspose && notenum >= 1 && notenum <= 12) {
                            chan.Riff.TransposeOctave = octave;
                            chan.Riff.TransposeNote = notenum;
                            transposing = true;
                        } else {
                            chan.Riff.TransposeOctave = 3;
                            chan.Riff.TransposeNote = 12;
                        }

                        chan.Riff.SpeedCnt = 1;
                        tickRiff(channum, chan.Riff, true);
                    } else {
                        chan.Riff.SpeedCnt = 0;
                    }
                }

                // play the note
                if (!transposing && notenum > 0) {
                    if (notenum == 15) {
                        chan.KeyFlags |= fKeyOff;
                    }
                    if (chan.Instrument == null || chan.Instrument.Algorithm < 7) {
                        playNoteOPL3(channum, octave, notenum);
                    }
                }
            }

            // process effect
            switch (cmd) {
            case cmSetVol:
                setVolume(channum, param);
                break;
            case cmSetSpeed:
                if (src == SNone) {
                    speed = param;
                    speedCnt = param;
                } else if (src == SRiff) {
                    chan.Riff.Speed = param;
                    chan.Riff.SpeedCnt = param;
                } else if (src == SIRiff) {
                    chan.IRiff.Speed = param;
                    chan.IRiff.SpeedCnt = param;
                }
                break;
            case cmPortamentoUp:
                fx.PortSlide = (byte) param;
                break;
            case cmPortamentoDwn:
                fx.PortSlide = (byte) -param;
                break;
            case cmToneVolSlide:
            case cmVolSlide: {
                int val = param;
                if (val >= 50) {
                    val = -(val - 50);
                }
                fx.VolSlide = (byte) val;
                if (cmd == cmToneVolSlide) {
                    toneSlideEffect(channum, fx, param);
                }
                break;
            }
            case cmToneSlide:
                toneSlideEffect(channum, fx, param);
                break;
            case cmJumpToLine:
                if (param < kTrackLines && src == SNone) {
                    lineJump = param;
                }
                break;
            case cmMultiplier:
                if (src == SIRiff) {
                    loadInstMultiplierOPL3(channum, op, param);
                }
                break;
            case cmVolume:
                if (src == SIRiff) {
                    loadInstVolumeOPL3(channum, op, param);
                }
                break;
            case cmFeedback:
                if (src == SIRiff) {
                    loadInstFeedbackOPL3(channum, param / 10, param % 10);
                }
                break;
            default:
                break;
            }
        } finally {
            entrances--;
        }
    }

    private void toneSlideEffect(int channum, CEffects fx, int param) {
        int speed = param;
        if (speed != 0) {
            fx.ToneSlideSpeed = speed;
        }
        getSlideDir(channum, fx);
    }

    private void loadInstrumentOPL3(int channum) {
        CChannel chan = channels[channum];
        CInstrument inst = chan.Instrument;
        if (inst == null) {
            return;
        }

        int alg = inst.Algorithm;
        chan.Volume = inst.Volume;
        chan.DetuneA = (inst.Detune + 1) >> 1;
        chan.DetuneB = inst.Detune >> 1;

        if (useOPL3 && channum < 6) {
            int mask = 1 << channum;
            setOPL3(0x104, (getOPL3(0x104) & ~mask) | (alg == 2 || alg == 3 ? mask : 0));
        }

        if (useOPL3) {
            setOPL3(0xC0 + ChanOffsets3[channum], ((inst.Panning[1] ^ 3) << 4) | inst.Feedback[1] << 1 | (alg == 3 || alg == 5 || alg == 6 ? 1 : 0));
            setOPL3(0xC0 + Chn2Offsets3[channum], ((inst.Panning[0] ^ 3) << 4) | inst.Feedback[0] << 1 | (alg == 1 || alg == 6 ? 1 : 0));
        } else {
            setOPL3(0xC0 + channum, ((inst.Panning[0] ^ 3) << 4) | inst.Feedback[0] << 1 | (alg == 1 ? 1 : 0));
        }

        for (int i = 0; i < (useOPL3 ? 4 : 2); i++) {
            int[] op = (alg < 2 && i >= 2) ? BLANK : inst.Operators[i];
            int reg = useOPL3 ? OpOffsets3[channum][i] : OpOffsets2[channum][i];

            int vol = ~op[1] & 0x3F;

            if (AlgCarriers[alg][i]) {
                vol = vol * inst.Volume / 64;
                vol = vol * masterVol / 64;
            }

            setOPL3(reg + 0x20, op[0]);
            setOPL3(reg + 0x40, (op[1] & 0xC0) | ((vol ^ 0x3F) & 0x3F));
            setOPL3(reg + 0x60, op[2]);
            setOPL3(reg + 0x80, op[3]);
            setOPL3(reg + 0xE0, op[4]);
        }
    }

    private void playNoteOPL3(int channum, int octave, int note) {
        CChannel chan = channels[channum];

        int o1, o2;
        if (useOPL3) {
            o1 = ChanOffsets3[channum];
            o2 = Chn2Offsets3[channum];
        } else {
            o1 = 0;
            o2 = channum;
        }

        if ((chan.KeyFlags & fKeyOff) != 0) {
            chan.KeyFlags &= ~(fKeyOff | fKeyedOn);
            if (useOPL3) {
                setOPL3(0xB0 + o1, getOPL3(0xB0 + o1) & ~0x20);
            }
            setOPL3(0xB0 + o2, getOPL3(0xB0 + o2) & ~0x20);
        }

        if (note > 12) {
            return;
        }

        boolean op4 = useOPL3 && chan.Instrument != null && chan.Instrument.Algorithm >= 2;

        int freq = NoteFreq[note - 1];
        int frq2 = freq;

        chan.CurrFreq = freq;
        chan.CurrOctave = octave;

        freq = (freq + chan.DetuneA) & 0xffff;
        frq2 = (frq2 - chan.DetuneB) & 0xffff;

        if (op4) {
            setOPL3(0xA0 + o1, frq2 & 0xFF);
        }
        setOPL3(0xA0 + o2, freq & 0xFF);

        if ((chan.KeyFlags & fKeyOn) != 0) {
            chan.KeyFlags = (chan.KeyFlags & ~fKeyOn) | fKeyedOn;
        }
        if (op4) {
            setOPL3(0xB0 + o1, (frq2 >> 8) | (octave << 2) | ((chan.KeyFlags & fKeyedOn) != 0 ? 0x20 : 0));
        } else if (useOPL3) {
            setOPL3(0xB0 + o1, 0);
        }
        setOPL3(0xB0 + o2, (freq >> 8) | (octave << 2) | ((chan.KeyFlags & fKeyedOn) != 0 ? 0x20 : 0));
    }

    private void resetFX(CEffects fx) {
        fx.PortSlide = 0;
        fx.VolSlide = 0;
        fx.ToneSlideDir = 0;
    }

    private void tickRiff(int channum, CRiff riff, boolean chanRiff) {
        if (riff.SpeedCnt == 0) {
            resetFX(riff.FX);
            return;
        }

        riff.SpeedCnt--;
        if (riff.SpeedCnt > 0) {
            return;
        }
        riff.SpeedCnt = riff.Speed;

        int line = riff.Line++;
        if (riff.Line >= kTrackLines) {
            riff.SpeedCnt = 0;
        }

        resetFX(riff.FX);

        int trk = riff.Track;
        int lineid = 0;
        if (trk >= 0 && (u8(trk) & 0x7F) == line) {
            lineid = u8(trk++);

            if (chanRiff) {
                lastInstrumentIO = riff.LastInstrument;
                trk = unpackNote(trk);
                riff.LastInstrument = lastInstrumentIO;
                transpose(riff.TransposeNote, riff.TransposeOctave);
                playNote(channum, noteNum, octaveNum, instNum, effectNum, param, SRiff, 0);
            } else {
                boolean last;
                do {
                    int col = u8(trk) & 15;
                    lastInstrumentIO = riff.LastInstrument;
                    trk = unpackNote(trk);
                    riff.LastInstrument = lastInstrumentIO;
                    last = lastNote;
                    if (effectNum != cmIgnore) {
                        transpose(riff.TransposeNote, riff.TransposeOctave);
                    }
                    playNote(channum, noteNum, octaveNum, instNum, effectNum, param, SIRiff, col > 0 ? (col - 1) & 3 : 0);
                } while (!last);
            }

            if ((lineid & 0x80) != 0) {
                trk = -1;
            }
            riff.Track = trk;
        }

        // special case: if next line has a jump command, run it now
        if (trk < 0 || (u8(trk++) & 0x7F) != riff.Line) {
            return;
        }

        lastInstrumentIO = 0; // dummy
        trk = unpackNote(trk);
        if (effectNum == cmJumpToLine && param < kTrackLines) {
            riff.Line = param;
            riff.Track = skipToLine(riff.TrackStart, param, chanRiff);
        }
    }

    private void continueFX(int channum, CEffects fx) {
        CChannel chan = channels[channum];

        if (fx.PortSlide != 0) {
            portamento(channum, fx, fx.PortSlide, false);
        }

        if (fx.VolSlide != 0) {
            int vol = chan.Volume;
            vol -= fx.VolSlide;
            if (vol < 0) {
                vol = 0;
            }
            setVolume(channum, vol);
        }

        if (fx.ToneSlideDir != 0) {
            portamento(channum, fx, fx.ToneSlideDir, true);
        }
    }

    private void setVolume(int channum, int vol) {
        CChannel chan = channels[channum];

        if (vol > 64) {
            vol = 64;
        }
        chan.Volume = vol;
        vol = vol * masterVol / 64;

        CInstrument inst = chan.Instrument;
        if (inst == null) {
            return;
        }
        int alg = inst.Algorithm;

        for (int i = 0; i < 4; i++) {
            int[] op = inst.Operators[i];
            if (!AlgCarriers[alg][i]) {
                continue;
            }
            int opvol = ((op[1] & 63) ^ 63) * vol / 64;
            int reg = 0x40 + (useOPL3 ? OpOffsets3[channum][i] : OpOffsets2[channum][i]);
            setOPL3(reg, (getOPL3(reg) & 0xC0) | (opvol ^ 0x3F));
        }
    }

    private void getSlideDir(int channum, CEffects fx) {
        CChannel chan = channels[channum];

        int speed = (byte) fx.ToneSlideSpeed;
        if (speed > 0) {
            int oct = fx.ToneSlideOct;
            int freq = fx.ToneSlideFreq;

            int oldfreq = chan.CurrFreq;
            int oldoct = chan.CurrOctave;

            if (oldoct > oct) {
                speed = -speed;
            } else if (oldoct == oct) {
                if (oldfreq > freq) {
                    speed = -speed;
                } else if (oldfreq == freq) {
                    speed = 0;
                }
            }
        }

        fx.ToneSlideDir = (byte) speed;
    }

    private void loadInstMultiplierOPL3(int channum, int op, int mult) {
        int reg = 0x20 + OpOffsets3[channum][op];
        setOPL3(reg, (getOPL3(reg) & 0xF0) | (mult & 15));
    }

    private void loadInstVolumeOPL3(int channum, int op, int vol) {
        int reg = 0x40 + OpOffsets3[channum][op];
        setOPL3(reg, (getOPL3(reg) & 0xC0) | ((vol & 0x3F) ^ 0x3F));
    }

    private void loadInstFeedbackOPL3(int channum, int which, int fb) {
        if (which == 0) {
            int reg = 0xC0 + Chn2Offsets3[channum];
            setOPL3(reg, (getOPL3(reg) & 0x31) | ((fb & 7) << 1));
        } else if (which == 1) {
            int reg = 0xC0 + ChanOffsets3[channum];
            setOPL3(reg, (getOPL3(reg) & 0x31) | ((fb & 7) << 1));
        }
    }

    private void portamento(int channum, CEffects fx, int amount, boolean toneslide) {
        CChannel chan = channels[channum];

        int freq = chan.CurrFreq;
        int oct = chan.CurrOctave;

        freq = (freq + amount) & 0xffff;

        if (freq < 0x156) {
            if (oct > 0) {
                oct--;
                freq += 0x2AE - 0x156;
            } else {
                freq = 0x156;
            }
        } else if (freq > 0x2AE) {
            if (oct < 7) {
                oct++;
                freq -= 0x2AE - 0x156;
            } else {
                freq = 0x2AE;
            }
        }

        if (toneslide) {
            if (amount >= 0) {
                if (oct > fx.ToneSlideOct || (oct == fx.ToneSlideOct && freq >= fx.ToneSlideFreq)) {
                    freq = fx.ToneSlideFreq;
                    oct = fx.ToneSlideOct;
                }
            } else {
                if (oct < fx.ToneSlideOct || (oct == fx.ToneSlideOct && freq <= fx.ToneSlideFreq)) {
                    freq = fx.ToneSlideFreq;
                    oct = fx.ToneSlideOct;
                }
            }
        }

        chan.CurrFreq = freq;
        chan.CurrOctave = oct;

        int frq2 = (freq - chan.DetuneB) & 0xffff;
        freq = (freq + chan.DetuneA) & 0xffff;

        int chanOffset = useOPL3 ? Chn2Offsets3[channum] : channum;
        setOPL3(0xA0 + chanOffset, freq & 0xFF);
        setOPL3(0xB0 + chanOffset, (freq >> 8 & 3) | oct << 2 | (getOPL3(0xB0 + chanOffset) & 0xE0));

        if (useOPL3) {
            chanOffset = ChanOffsets3[channum];
            setOPL3(0xA0 + chanOffset, frq2 & 0xFF);
            setOPL3(0xB0 + chanOffset, (frq2 >> 8 & 3) | oct << 2 | (getOPL3(0xB0 + chanOffset) & 0xE0));
        }
    }

    private void transpose(int note, int octave) {
        if (noteNum >= 1 && noteNum <= 12) {
            int toct = octave - 3;
            if (toct != 0) {
                octaveNum += toct;
                if (octaveNum < 0) {
                    octaveNum = 0;
                } else if (octaveNum > 7) {
                    octaveNum = 7;
                }
            }

            int tnot = note - 12;
            if (tnot != 0) {
                noteNum += tnot;
                if (noteNum < 1) {
                    noteNum += 12;
                    if (octaveNum > 0) {
                        octaveNum--;
                    } else {
                        noteNum = 1;
                    }
                }
            }
        }
    }
}
