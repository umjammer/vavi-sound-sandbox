/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter <dn.tlp@gmx.net>, et al.
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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URI;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;

/**
 * AdLib SMF (MDI) Player.
 * Ported from adplug's mdi.cpp / mdi.h by Stas'M, based on MIDIPLAY.C by
 * Dale Glowinski, Ad Lib Inc.
 *
 * MDI files are Standard MIDI Files (type 0, single track) with AdLib
 * instrument data embedded in sequencer-specific meta events; plain .mid
 * files are left to MidPlayer via the extension gate.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class MdiPlayer extends ComposerPlayer {

    private static final Logger logger = getLogger(MdiPlayer.class.getName());

    /** MThd(4) + size(4) + head(6) + MTrk(4) + size(4) */
    private static final int MIDI_MIN_SIZE = 22;
    private static final int MIDI_DEF_TEMPO = 500000;

    private static final int NOTE_OFF = 0x80;
    private static final int NOTE_ON = 0x90;
    private static final int AFTER_TOUCH = 0xA0;
    private static final int CONTROL_CHANGE = 0xB0;
    private static final int PROG_CHANGE = 0xC0;
    private static final int CHANNEL_PRESSURE = 0xD0;
    private static final int PITCH_BEND = 0xE0;
    private static final int SYSEX_F0 = 0xf0;
    private static final int SYSEX_F7 = 0xf7;
    private static final int STOP_FC = 0xfc;
    private static final int META = 0xff;

    private static final int END_OF_TRACK = 0x2f;
    private static final int TEMPO = 0x51;
    private static final int SEQ_SPECIFIC = 0x7f;

    /** meta sign(3) + code(2) + 1 */
    private static final int META_MIN_SIZE = 6;

    private static final int ADLIB_TIMBRE = 1;
    private static final int ADLIB_RHYTHM = 2;
    private static final int ADLIB_PITCH = 3;

    private int pos;
    private int size;
    private boolean songend;
    private float timer;
    /** division in PPQN */
    private int division;
    /** MIDI data */
    private byte[] data;

    /** tick counter */
    private long counter;
    /** ticks to wait for next event */
    private long ticks;
    /** running status byte */
    private int status;
    /** actual volume of all voices */
    private final int[] volume = new int[MAX_VOICES];

    @Override
    public Type getType() {
        return new Opl3FileFormatType("AdLib Visual Composer: MIDIPlay File", "mdi");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MDI");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            // MDI shares the SMF "MThd" magic with plain .mid files
            // (MidPlayer); gate on the extension when the URI is known
            URI uri = SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && !path.toLowerCase().endsWith(".mdi")) {
                    return false;
                }
            }
            bitStream.mark(14);
            byte[] hdr = new byte[14];
            if (bitStream.read(hdr) < 14) return false;
            return hdr[0] == 'M' && hdr[1] == 'T' && hdr[2] == 'h' && hdr[3] == 'd' &&
                    u32be(hdr, 4) == 6 && // chunk size
                    u16be(hdr, 8) == 0 && // MIDI type must be 0
                    u16be(hdr, 10) == 1;  // track count must be 1
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < MIDI_MIN_SIZE) {
            throw new IllegalArgumentException("file too short");
        }

        // header validation
        if (buf[0] != 'M' || buf[1] != 'T' || buf[2] != 'h' || buf[3] != 'd') {
            throw new IllegalArgumentException("invalid signature");
        }
        if (u32be(buf, 4) != 6 || // chunk size
                u16be(buf, 8) != 0 || // MIDI type must be 0
                u16be(buf, 10) != 1) { // track count must be 1
            throw new IllegalArgumentException("invalid MIDI header");
        }
        division = u16be(buf, 12);
        // track validation
        if (buf[14] != 'M' || buf[15] != 'T' || buf[16] != 'r' || buf[17] != 'k') {
            throw new IllegalArgumentException("invalid track chunk");
        }
        size = (int) u32be(buf, 18);
        // data size validation
        if (buf.length < MIDI_MIN_SIZE + size) {
            throw new IllegalArgumentException("truncated track data");
        }
        // load section
        data = new byte[size];
        System.arraycopy(buf, MIDI_MIN_SIZE, data, 0, size);

logger.log(Level.DEBUG, "division: " + division + ", track size: " + size);

        rewind(0);
    }

    @Override
    protected void frontend_rewind(int subsong) {
        // set default MIDI tempo
        setTempo(MIDI_DEF_TEMPO);
        pos = 0;
        songend = false;

        // midiplay uses rhythm mode by default
        SetRhythmMode(1);

        for (int i = 0; i < MAX_VOICES; i++) {
            volume[i] = 0;
            SetDefaultInstrument(i);
        }
        counter = 0;
        ticks = 0;
    }

    /** changes the tempo */
    private void setTempo(long tempo) {
        if (tempo == 0) tempo = MIDI_DEF_TEMPO;
        timer = division * 1000000 / (float) tempo;
    }

    private int d(int p) {
        return p < size ? data[p] & 0xff : 0;
    }

    /** variable-length quantity */
    private long getVarVal() {
        long result = 0;
        do {
            result <<= 7;
            result |= d(pos) & 0x7F;
        } while ((d(pos++) & 0x80) != 0 && pos < size);
        return result;
    }

    private void executeCommand() {
        int newStatus;

        // execute MIDI command
        if (d(pos) < 0x80) {
            // running status
            newStatus = status;
        } else {
            newStatus = d(pos); pos++;
        }
        if (newStatus == STOP_FC) {
            pos = size;
        } else if (newStatus == SYSEX_F0 || newStatus == SYSEX_F7) {
            // skip over system exclusive event
            long len = getVarVal();
            pos += len;
        } else if (newStatus == META) {
            // process meta-event
            int meta = d(pos); pos++;
            int len = (int) getVarVal();
            switch (meta) {
            case END_OF_TRACK:
                pos = size - len; // pos incremented later
                break;
            case TEMPO:
                if (len >= 3) {
                    long tempo = ((long) d(pos) << 16) | (d(pos + 1) << 8) | d(pos + 2);
                    setTempo(tempo);
                }
                break;
            case SEQ_SPECIFIC:
                if (len >= META_MIN_SIZE) {
                    // Ad Lib midi ID is 00 00 3f
                    if (d(pos) == 0 && d(pos + 1) == 0 && d(pos + 2) == 0x3f) {
                        // the first two bytes after the ID contain the Ad Lib
                        // event code; the following bytes contain the data
                        // pertaining to the event
                        int code = (d(pos + 3) << 8) | d(pos + 4);
                        if (code == ADLIB_TIMBRE && len >= META_MIN_SIZE + ADLIB_INST_LEN) {
                            // instrument change code: first byte of data
                            // contains voice number, following bytes contain
                            // instrument parameters
                            int voice = d(pos + 5);
                            byte[] ins = new byte[ADLIB_INST_LEN];
                            for (int i = 0; i < ADLIB_INST_LEN; i++) {
                                ins[i] = (byte) d(pos + META_MIN_SIZE + i);
                            }
                            int index = load_instrument_data(ins, ins.length);
                            SetInstrument(voice, index);
                        } else if (code == ADLIB_RHYTHM) {
                            // melo/perc mode code: 0 is melodic, !0 is percussive
                            SetRhythmMode(d(pos + 5));
                        } else if (code == ADLIB_PITCH) {
                            // sets the interval over which pitch bend changes will be applied
                            SetPitchRange(d(pos + 5));
                        }
                    }
                }
                break;
            }
            pos += len;
        } else {
            status = newStatus;
            int voice = status & 0xF;
            int note, vol, pitch;
            switch (status & 0xF0) {
            case NOTE_OFF:
                pos += 2;
                if (voice >= MAX_VOICES) {
                    break;
                }
                NoteOff(voice);
                break;
            case NOTE_ON:
                note = d(pos); pos++;
                vol = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                if (vol == 0) {
                    // a note-on with a volume of 0 is equivalent to a note-off
                    NoteOff(voice);
                    volume[voice] = vol;
                } else {
                    // regular note-on
                    if (vol != volume[voice]) {
                        SetVolume(voice, vol);
                        volume[voice] = vol;
                    }
                    NoteOn(voice, note);
                }
                break;
            case AFTER_TOUCH:
                pos++; // skip note
                vol = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                if (vol != volume[voice]) {
                    SetVolume(voice, vol);
                    volume[voice] = vol;
                }
                break;
            case CONTROL_CHANGE:
                // unused
                pos += 2;
                break;
            case PROG_CHANGE:
                // unused
                pos += 1;
                break;
            case CHANNEL_PRESSURE:
                vol = d(pos); pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                if (vol != volume[voice]) {
                    SetVolume(voice, vol);
                    volume[voice] = vol;
                }
                break;
            case PITCH_BEND:
                pitch = d(pos); pos++;
                pitch |= d(pos) << 7; pos++;
                if (voice >= MAX_VOICES) {
                    break;
                }
                ChangePitch(voice, pitch);
                break;
            default:
                // a bad status byte (or unimplemented MIDI command) has been
                // encountered; skip bytes until next timing byte followed by
                // status byte
                while (d(pos++) < NOTE_OFF && pos < size);
                break;
            }
        }
    }

    @Override
    public boolean update() throws IOException {
        if (counter == 0) {
            ticks = getVarVal();
        }
        if (++counter >= ticks) {
            counter = 0;
            while (pos < size) {
                executeCommand();
                if (pos >= size) {
                    pos = 0;
                    songend = true;
                    break;
                } else if (data[pos] == 0) { // if next delay is zero
                    pos++;
                } else {
                    break;
                }
            }
        }
        return !songend;
    }

    @Override
    public float getRefresh() {
        return timer;
    }

    // ---- byte helpers

    /** big-endian unsigned 16-bit */
    private static int u16be(byte[] b, int off) {
        return ((b[off] & 0xff) << 8) | (b[off + 1] & 0xff);
    }

    /** big-endian unsigned 32-bit */
    private static long u32be(byte[] b, int off) {
        return ((b[off] & 0xffL) << 24) | ((b[off + 1] & 0xffL) << 16) | ((b[off + 2] & 0xffL) << 8) | (b[off + 3] & 0xffL);
    }
}
