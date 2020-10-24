/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 *
 * MIDI & MIDI-like file player - Last Update: 10/15/2005
 *                  by Phil Hassey - www.imitationpickles.org
 *                                   philhassey@hotmail.com
 *
 * Can play the following
 *      .LAA - a raw save of a Lucas Arts Adlib music
 *             or
 *             a raw save of a LucasFilm Adlib music
 *      .MID - a "midi" save of a Lucas Arts Adlib music
 *           - or general MIDI files
 *      .CMF - Creative Music Format
 *      .SCI - the sierra "midi" format.
 *             Files must be in the form
 *             xxxNAME.sci
 *             So that the loader can load the right patch file:
 *             xxxPATCH.003  (patch.003 must be saved from the
 *                            sierra resource from each game.)
 *
 * 6/2/2000:  v1.0 relased by phil hassey
 *      Status:  LAA is almost perfect
 *                      - some volumes are a bit off (intrument too quiet)
 *               MID is fine (who wants to listen to MIDI vid adlib anyway)
 *               CMF is okay (still needs the adlib rythm mode implemented
 *                            for real)
 * 6/6/2000:
 *      Status:  SCI:  there are two SCI formats, orginal and advanced.
 *                    original:  (Found in SCI/EGA Sierra Adventures)
 *                               played almost perfectly, I believe
 *                               there is one mistake in the instrument
 *                               loader that causes some sounds to
 *                               not be quite right.  Most sounds are fine.
 *                    advanced:  (Found in SCI/VGA Sierra Adventures)
 *                               These are multi-track files.  (Thus the
 *                               player had to be modified to work with
 *                               them.)  This works fine.
 *                               There are also multiple tunes in each file.
 *                               I think some of them are supposed to be
 *                               played at the same time, but I'm not sure
 *                               when.
 * 8/16/2000:
 *      Status:  LAA: now EGA and VGA lucas games work pretty well
 *
 * 10/15/2005: Changes by Simon Peter
 *  Added rhythm mode support for CMF format.
 *
 * 09/13/2008: Changes by Adam Nielsen (malvineous@shikadi.net)
 *      Fixed a couple of CMF rhythm mode bugs
 *      Disabled note velocity for CMF files
 *      Added support for nonstandard CMF AM+VIB controller (for VGFM CMFs)
 *
 * Other acknowledgements:
 *  Allegro - for the midi instruments and the midi volume table
 *  SCUMM Revisited - for getting the .LAA / .MIDs out of those
 *                    LucasArts files.
 *  FreeSCI - for some information on the sci music files
 *  SD - the SCI Decoder (to get all .sci out of the Sierra files)
 */

package org.uva.emulation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import org.uva.emulation.Opl3SoundBank.Opl3Instrument;

import vavi.util.StringUtil;


class MidPlayer extends Opl3Player implements Sequencer {

    static Logger logger = Logger.getLogger(MidPlayer.class.getName());

    static final int FILE_LUCAS = 1;
    static final int FILE_MIDI = 2;
    static final int FILE_CMF = 3;
    static final int FILE_SIERRA = 4;
    static final int FILE_ADVSIERRA = 5;
    static final int FILE_OLDLUCAS = 6;

    static class MidiTrack {
        int tend;
        int spos;
        int pos;
        int iwait;
        boolean on;
        int pv;
    }

    // data length
    int flen;
    byte[] data;
    // data pos
    int pos;

    Opl3SoundBank soundBank = new Opl3SoundBank();

    Opl3Instrument[] smyinsbank = new Opl3Instrument[128];
    int sierra_pos;
    // sierra instruments
    int stins;

    int type;
    int subsongs;

    MidiTrack[] track = new MidiTrack[16];

    int deltas;
    int msqtr;
    float fwait;
    int iwait;
    boolean doing;
    // number of instruments
    int tins;

    int getInstruments() {
        return tins;
    }

    int getSubSongs() {
        return subsongs;
    }

    public int getTotalMiliseconds() {
        return 0;
    }

    public MidPlayer() {

        flen = 0;
        data = null;

        for (int t = 0; t < track.length; ++t) {
            track[t] = new MidiTrack();
        }
    }

    int lookData(int pos) {
        return pos >= 0 && pos < flen ? (data[pos] & 0xff) : 0;
    }

    int getNextI(int num) {
        int v = 0;

        for (int i = 0; i < num; ++i) {
            v += lookData(pos) << 8 * i;
            ++pos;
        }

        return v;
    }

    int getNext(int num) {
        int b = 0;

        for (int i = 0; i < num; ++i) {
            b <<= 8;
            b += lookData(pos);
            ++pos;
        }

        return b;
    }

    int getVal() {
        int b = getNext(1);
        int v = b & 0x7f;
        while ((b & 0x80) != 0) {
            b = getNext(1);
            v = (v << 7) + (b & 0x7F);
        }

        return v;
    }

    boolean loadSierraIns(String name) throws IOException {
        int[] buf = new int[28];
        StringBuffer sb = new StringBuffer(name.length() + 9);
        sb.append(name);
        int y = 0;

        for (int i = sb.length() - 1; i >= 0; --i) {
            if (sb.charAt(i) == '/' || sb.charAt(i) == '\\') {
                y = i + 1;
                break;
            }
        }

        sb.replace(y + 3, sb.length() - 1, "patch.003");
        File file = new File(sb.toString());
        FileInputStream fis = new FileInputStream(file);
        fis.skip(2);
        stins = 0;

        for (int j = 0; j < 2; ++j) {
            for (int k = 0; k < 48; ++k) {
                int p = j * 48 + k;
                logger.fine(String.format("%2d: ", p));

                for (int i = 0; i < 28; ++i) {
                    buf[i] = fis.read();
                }

                int[] x = new int[11];
                x[0] = buf[9] * 0x80 + buf[10] * 0x40 + buf[5] * 0x20 + buf[11] * 0x10 + buf[1];
                x[1] = buf[22] * 0x80 + buf[23] * 0x40 + buf[18] * 0x20 + buf[24] * 0x10 + buf[14];
                x[2] = (buf[0] << 6) + buf[8];
                x[3] = (buf[13] << 6) + buf[21];
                x[4] = (buf[3] << 4) + buf[6];
                x[5] = (buf[16] << 4) + buf[19];
                x[6] = (buf[4] << 4) + buf[7];
                x[7] = (buf[17] << 4) + buf[20];
                x[8] = buf[26];
                x[9] = buf[27];
                x[10] = (buf[2] << 1) + (1 - (buf[12] & 1));

                smyinsbank[p] = soundBank.newInstrument(0, p, "sierra." + p, x);

                ++stins;
            }

            fis.skip(2);
        }

        fis.close();
        return true;
    }

    void sierra_next_section() {
        for (int t = 0; t < 16; ++t) {
            track[t].on = false;
        }

        logger.info("next adv sierra section:");
        pos = sierra_pos;

        int t = 0;
        for (int i = 0; i != 255; i = getNext(1)) {
            getNext(1);
            track[t].on = true;
            track[t].spos = getNext(1);
            track[t].spos += (getNext(1) << 8) + 4; // 4 best usually +3? not 0,1,2 or 5
            track[t].tend = flen;
            track[t].iwait = 0;
            track[t].pv = 0;
            logger.info(String.format("track %d starts at %x", t, track[t].spos));
            t++;
            getNext(2);
        }

        getNext(2);
        deltas = 32;
        sierra_pos = pos;
        fwait = 0.0F;
        doing = true;
    }

    public void load(byte[] data) throws IOException {

        byte good = 0;
        switch (data[0] & 0xff) {
        case 'A':
            if (data[1] == 'D' && data[2] == 'L') {
                good = FILE_LUCAS;
            }
            break;
        case 'M':
            if (data[1] == 'T' && data[2] == 'h' && data[3] == 'd') {
                good = FILE_MIDI;
            }
            break;
        case 'C':
            if (data[1] == 'T' && data[2] == 'M' && data[3] == 'F') {
                good = FILE_CMF;
            }
            break;
        case 0x84:
            if (data[1] == 0 && loadSierraIns(data.toString())) {
                if ((data[2] & 0xff) == 0xf0) {
                    good = FILE_ADVSIERRA;
                } else {
                    good = FILE_SIERRA;
                }
            }
            break;
        default:
            if (data[4] == 'A' && data[5] == 'D') {
                good = FILE_OLDLUCAS;
            }
            break;
        }

        if (good == 0) {
            throw new IllegalArgumentException("unsupported type");
        }

        subsongs = 1;
        type = good;
logger.fine("type: " + type);
        flen = data.length;
        this.data = data;

        rewind(0);
    }

    Opl3Synthesizer synthesizer = new Opl3Synthesizer();
    Transmitter transmitter = new Opl3Transmitter();

    public boolean update() {
        if (doing) {
            for (int t = 0; t < 16; ++t) {
                if (track[t].on) {
                    pos = track[t].pos;
                    if (type != FILE_SIERRA && type != FILE_ADVSIERRA) {
                        track[t].iwait += getVal();
                    } else {
                        track[t].iwait += getNext(1);
                    }

                    track[t].pos = pos;
                }
            }

            doing = false;
        }

        iwait = 0;
        boolean ret = true;

        while (iwait == 0 && ret) {
            for (int t = 0; t < 16; ++t) {
                if (track[t].on && track[t].iwait == 0 && track[t].pos < track[t].tend) {
                    pos = track[t].pos;
                    int v = getNext(1);
                    if (v < 0x80) {
                        v = track[t].pv;
                        --pos;
                    }

                    track[t].pv = v;
                    int c = v & 0x0f;
                    logger.fine(String.format("[%2X]", v));

                    int data1;
                    int data2;

                    MidiMessage midiMessage = null;

                    try {
                        switch (v & 0xf0) {
                        case 0x80: // note off
                            data1 = getNext(1);
                            data2 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.NOTE_OFF, c, data1, data2);
                            break;
                        case 0x90: // note on
                            data1 = getNext(1);
                            data2 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.NOTE_ON, c, data1, data2);
                            break;
                        case 0xa0: // key after touch
                            data1 = getNext(1);
                            data2 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.POLY_PRESSURE, c, data1, data2);
                            break;
                        case 0xb0: // control change .. pitch bend?
                            int ctrl = getNext(1);
                            data2 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.CONTROL_CHANGE, c, ctrl, data2);
                            break;
                        case 0xc0: // patch change
                            data1 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.PROGRAM_CHANGE, c, data1);
                            break;
                        case 0xd0: // channel touch
                            data1 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.CHANNEL_PRESSURE, c, data1);
                            break;
                        case 0xe0: // pitch wheel
                            data1 = getNext(1);
                            data2 = getNext(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.PITCH_BEND, c, data1, data2);
                            break;
                        case 0xf0:
                            switch (v) {
                            case 0xf0:
                            case 0xf7: // sysex
                                boolean f = false;
                                int l = getVal();
                                if (lookData(pos + l) == 0xf7) {
                                    f = true;
                                }
                                byte[] b = new byte[l + 2];
                                b[0] = (byte) (v & 0xff);
                                b[1] = (byte) (l & 0xff);
                                for (int i = 2; i < b.length; i++) {
                                    b[i] = (byte) (getNext(1) & 0xff);
                                }
                                logger.fine("sysex:\n" + StringUtil.getDump(b));
                                midiMessage = new SysexMessage();
                                ((SysexMessage) midiMessage).setMessage(b, b.length);
                                logger.fine("sysex: " + midiMessage.getLength());
                                if (f) {
                                    getNext(1);
                                }
                                break;
                            case 0xf1:
                            case 0xf4:
                            case 0xf5:
                            case 0xfd:
                            case 0xfe:
                            default:
                                break;
                            case 0xf2:
                                data1 = getNext(1);
                                data2 = getNext(1);
                                midiMessage = new ShortMessage();
                                ((ShortMessage) midiMessage).setMessage(ShortMessage.SONG_POSITION_POINTER, c, data1, data2);
                                break;
                            case 0xf3:
                                data1 = getNext(1);
                                midiMessage = new ShortMessage();
                                ((ShortMessage) midiMessage).setMessage(ShortMessage.SONG_SELECT, c, data1);
                                break;
                            case 0xf6: // something
                            case 0xf8:
                            case 0xfa:
                            case 0xfb:
                            case 0xfc:
                                // this ends the track for sierra.
                                if (type == FILE_SIERRA || type == FILE_ADVSIERRA) {
                                    track[t].tend = pos;
                                    logger.info(String.format("endmark: %d -- %x\n", pos, pos));
                                }
                                break;
                            case 0xff: // meta
                                v = getNext(1);
                                l = getVal();
                                logger.fine("\n");
                                logger.fine(String.format("{%X_%X}", v, l));
                                if (v == 0x51) {
                                    msqtr = getNext(l); // set tempo
                                    logger.fine(String.format("(qtr=%d)", msqtr));
                                } else {
                                    for (int i = 0; i < l; ++i) {
                                        logger.fine(String.format("%2X ", getNext(1)));
                                    }
                                }
                                break;
                            }
                            break;
                        default:
                            // if we get down here, a error occurred
                            logger.warning(String.format("!: %02x", v));
                            break;
                        }
                    } catch (InvalidMidiDataException e) {
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                    }

                    if (midiMessage != null) {
                        transmitter.getReceiver().send(midiMessage, -1);
                    }

                    if (pos < track[t].tend) {
                        int w;
                        if (type != FILE_SIERRA && type != FILE_ADVSIERRA) {
                            w = getVal();
                        } else {
                            w = getNext(1);
                        }

                        track[t].iwait = w;
                    } else {
                        track[t].iwait = 0;
                    }

                    track[t].pos = pos;
                }
            }

            ret = false; // end of song.
            iwait = 0;

            for (int t = 0; t < 16; ++t) {
                if (track[t].on && track[t].pos < track[t].tend) {
                    ret = true; // not yet...
                }
            }

            if (ret) {
                iwait = 0xffffff; // bigger than any wait can be!

                for (int t = 0; t < 16; ++t) {
                    if (track[t].on && track[t].pos < track[t].tend && track[t].iwait < iwait) {
                        iwait = track[t].iwait;
                    }
                }
            }
        }

//        logger.info(String.format("iwait: %d, deltas: %d, msqtr: %d", iwait, deltas, msqtr));
        if (iwait != 0 && ret) {
            for (int t = 0; t < 16; ++t) {
                if (track[t].on) {
                    track[t].iwait -= iwait;
                }
            }

            fwait = 1.0F / ((float) iwait / (float) deltas * (msqtr / 1000000.0F));
        } else {
            fwait = 50.0F; // 1/50th of a second
        }

        logger.fine("\n");

        for (int t = 0; t < 16; ++t) {
            if (track[t].on) {
                if (track[t].pos < track[t].tend) {
                    logger.fine(String.format("<%d>", track[t].iwait));
                } else {
                    logger.fine("stop");
                }
            }
        }

        return ret;
    }

    public float getRefresh() {
        return fwait > 0.01F ? fwait : 0.01F;
    }

    public void rewind(int subSong) {
        pos = 0;

        tins = 0;
        subsongs = 1;

        deltas = 250; // just a number, not a standard
        msqtr = 500000;
        fwait = 123.0F; // gotta be a small thing... sorta like nothing
        iwait = 0;

        for (int t = 0; t < 16; ++t) {
            track[t].tend = 0;
            track[t].spos = 0;
            track[t].pos = 0;
            track[t].iwait = 0;
            track[t].on = false;
            track[t].pv = 0;
        }

        Map<String, Object> info = new HashMap<>();

        pos = 0;
        int v = getNext(1);
        switch (type) {
        case FILE_LUCAS:
            getNext(24); //skip junk and get to the midi.
            // note: no break, we go right into midi headers...
        case FILE_MIDI:
            if (type != FILE_LUCAS) {
                tins = 128;
            }
            getNext(11); // skip header
            deltas = getNext(2);
            logger.fine(String.format("deltas: %d", deltas));
            getNext(4);

            track[0].on = true;
            track[0].tend = getNext(4);
            track[0].spos = pos;
            logger.info(String.format("tracklen: %d", track[0].tend));
            break;
        case FILE_CMF:
            getNext(3); // ctmf
            getNextI(2); // version
            int n = getNextI(2); // instrument offset
            int m = getNextI(2); // music offset
            deltas = getNextI(2);  //ticks/qtr note
            //the stuff in the cmf is click ticks per second...
            msqtr = 1000000 / getNextI(2) * deltas;

            v = getNextI(2);
            if (v != 0) {
//                title = new String(data, v, ByteUtil.strlen(data, v));
            }

            v = getNextI(2);
            if (v != 0) {
//                author = new String(data, v, ByteUtil.strlen(data, v));
            }

            v = getNextI(2);
            if (v != 0) {
//                remarks = new String(data, v, ByteUtil.strlen(data, v));
            }

            getNext(16); // channel in use table...
            v = getNextI(2); // num instr
            if (v > 128) { // to ward of bad numbers...
                v = 128;
            }
            getNextI(2); //basic tempo

            logger.info(String.format("ioff: %d, moff: %d, deltas: %d, msqtr: %d, numi: %d", n, m, deltas, msqtr, v));
            pos = n; // jump to instruments
            tins = v;
            info.put("cmf.tins", tins);

            for (int p = 0; p < v; ++p) {
                logger.fine(String.format("\n%d: ", p));

                int[] x = new int[16];
                for (int j = 0; j < 16; ++j) {
                    x[j] = getNext(1);
                }
                info.put("cmf.myinsbank." + p, soundBank.newInstrument(0, p, "oldlucas." + p, x));
            }

            track[0].on = true;
            track[0].tend = flen; // music until the end of the file
            track[0].spos = m; // jump to midi music
            break;
        case FILE_SIERRA:
            info.put("sierra.myinsbank", smyinsbank);
            tins = stins;
            getNext(2);
            deltas = 32;
            track[0].on = true;
            track[0].tend = flen; // music until the end of the file

            for (int c = 0; c < 16; ++c) {
                info.put("sierra." + c + ".on", getNext(1) != 0);
                info.put("sierra." + c + ".inum", getNext(1));
            }

            track[0].spos = pos;
            break;
        case FILE_ADVSIERRA:
            info.put("sierra.myinsbank", smyinsbank);
            tins = stins;
            deltas = 32;
            getNext(11); // worthless empty space and "stuff" :)
            int o_sierra_pos = sierra_pos = pos;
            sierra_next_section();

            while (lookData(sierra_pos - 2) != 255) {
                sierra_next_section();
                ++subsongs;
            }

            if (subSong < 0 || subSong >= subsongs) {
                subSong = 0;
            }

            sierra_pos = o_sierra_pos;
            sierra_next_section();

            for (int i = 0; i != subSong; ++i) {
                sierra_next_section();
            }
            break;
        case FILE_OLDLUCAS:
            msqtr = 250000;
            pos = 9;
            deltas = getNext(1);
            v = 8;
            pos = 0x19; // jump to instruments
            tins = v;
            info.put("old_lucas.tins", tins);

            for (int p = 0; p < v; ++p) {
                logger.fine(String.format("\n%d: ", p));

                int[] ins = new int[16];

                for (int i = 0; i < 16; ++i) {
                    ins[i] = getNext(1);
                }

                int[] x = Opl3SoundBank.fromOldLucas(ins);
                info.put("old_lucas.myinsbank." + p, soundBank.newInstrument(0, p, "oldlucas." + p, x));
            }

            track[0].on = true;
            track[0].tend = flen; // music until the end of the file
            track[0].spos = 0x98; // jump to midi music
            break;
        }

        for (int t = 0; t < 16; ++t) {
            if (track[t].on) {
                track[t].pos = track[t].spos;
                track[t].pv = 0;
                track[t].iwait = 0;
            }
        }

        doing = true;
        try {
            info.put("type", type);
            Adlib.Writer writer = this::write;
            info.put("adlib.writer", writer);
            synthesizer.open(info);
            transmitter.setReceiver(synthesizer.getReceiver());
        } catch (MidiUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }

    String getType() {
        switch (type) {
        case 1:
            return "LucasArts AdLib MIDI";
        case 2:
            return "General MIDI";
        case 3:
            return "Creative Music Format (CMF MIDI)";
        case 4:
            return "Sierra On-Line EGA MIDI";
        case 5:
            return "Sierra On-Line VGA MIDI";
        case 6:
            return "Lucasfilm Adlib MIDI";
        default:
            return "MIDI unknown";
        }
    }

    private class Opl3Transmitter implements Transmitter {
        private boolean isOpen;
        Receiver receiver;

        public Opl3Transmitter() {
            isOpen = true;
        }

        @Override
        public void setReceiver(Receiver receiver) {
            this.receiver = receiver;
        }

        @Override
        public Receiver getReceiver() {
            return receiver;
        }

        @Override
        public void close() {
            isOpen = false;
        }
    }

    /* @see javax.sound.midi.MidiDevice#getDeviceInfo() */
    @Override
    public Info getDeviceInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.MidiDevice#open() */
    @Override
    public void open() throws MidiUnavailableException {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.MidiDevice#close() */
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.MidiDevice#isOpen() */
    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see javax.sound.midi.MidiDevice#getMaxReceivers() */
    @Override
    public int getMaxReceivers() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.MidiDevice#getMaxTransmitters() */
    @Override
    public int getMaxTransmitters() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.MidiDevice#getReceiver() */
    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.MidiDevice#getReceivers() */
    @Override
    public List<Receiver> getReceivers() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.MidiDevice#getTransmitter() */
    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.MidiDevice#getTransmitters() */
    @Override
    public List<Transmitter> getTransmitters() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#setSequence(javax.sound.midi.Sequence) */
    @Override
    public void setSequence(Sequence sequence) throws InvalidMidiDataException {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#setSequence(java.io.InputStream) */
    @Override
    public void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getSequence() */
    @Override
    public Sequence getSequence() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#start() */
    @Override
    public void start() {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#stop() */
    @Override
    public void stop() {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#isRunning() */
    @Override
    public boolean isRunning() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see javax.sound.midi.Sequencer#startRecording() */
    @Override
    public void startRecording() {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#stopRecording() */
    @Override
    public void stopRecording() {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#isRecording() */
    @Override
    public boolean isRecording() {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see javax.sound.midi.Sequencer#recordEnable(javax.sound.midi.Track, int) */
    @Override
    public void recordEnable(Track track, int channel) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#recordDisable(javax.sound.midi.Track) */
    @Override
    public void recordDisable(Track track) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getTempoInBPM() */
    @Override
    public float getTempoInBPM() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#setTempoInBPM(float) */
    @Override
    public void setTempoInBPM(float bpm) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getTempoInMPQ() */
    @Override
    public float getTempoInMPQ() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#setTempoInMPQ(float) */
    @Override
    public void setTempoInMPQ(float mpq) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#setTempoFactor(float) */
    @Override
    public void setTempoFactor(float factor) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getTempoFactor() */
    @Override
    public float getTempoFactor() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#getTickLength() */
    @Override
    public long getTickLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#getTickPosition() */
    @Override
    public long getTickPosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#setTickPosition(long) */
    @Override
    public void setTickPosition(long tick) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getMicrosecondLength() */
    @Override
    public long getMicrosecondLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#getMicrosecondPosition() */
    @Override
    public long getMicrosecondPosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#setMicrosecondPosition(long) */
    @Override
    public void setMicrosecondPosition(long microseconds) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#setMasterSyncMode(javax.sound.midi.Sequencer.SyncMode) */
    @Override
    public void setMasterSyncMode(SyncMode sync) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getMasterSyncMode() */
    @Override
    public SyncMode getMasterSyncMode() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#getMasterSyncModes() */
    @Override
    public SyncMode[] getMasterSyncModes() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#setSlaveSyncMode(javax.sound.midi.Sequencer.SyncMode) */
    @Override
    public void setSlaveSyncMode(SyncMode sync) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getSlaveSyncMode() */
    @Override
    public SyncMode getSlaveSyncMode() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#getSlaveSyncModes() */
    @Override
    public SyncMode[] getSlaveSyncModes() {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#setTrackMute(int, boolean) */
    @Override
    public void setTrackMute(int track, boolean mute) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getTrackMute(int) */
    @Override
    public boolean getTrackMute(int track) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see javax.sound.midi.Sequencer#setTrackSolo(int, boolean) */
    @Override
    public void setTrackSolo(int track, boolean solo) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getTrackSolo(int) */
    @Override
    public boolean getTrackSolo(int track) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see javax.sound.midi.Sequencer#addMetaEventListener(javax.sound.midi.MetaEventListener) */
    @Override
    public boolean addMetaEventListener(MetaEventListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    /* @see javax.sound.midi.Sequencer#removeMetaEventListener(javax.sound.midi.MetaEventListener) */
    @Override
    public void removeMetaEventListener(MetaEventListener listener) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#addControllerEventListener(javax.sound.midi.ControllerEventListener, int[]) */
    @Override
    public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#removeControllerEventListener(javax.sound.midi.ControllerEventListener, int[]) */
    @Override
    public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
        // TODO Auto-generated method stub
        return null;
    }

    /* @see javax.sound.midi.Sequencer#setLoopStartPoint(long) */
    @Override
    public void setLoopStartPoint(long tick) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getLoopStartPoint() */
    @Override
    public long getLoopStartPoint() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#setLoopEndPoint(long) */
    @Override
    public void setLoopEndPoint(long tick) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getLoopEndPoint() */
    @Override
    public long getLoopEndPoint() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* @see javax.sound.midi.Sequencer#setLoopCount(int) */
    @Override
    public void setLoopCount(int count) {
        // TODO Auto-generated method stub
        
    }

    /* @see javax.sound.midi.Sequencer#getLoopCount() */
    @Override
    public int getLoopCount() {
        // TODO Auto-generated method stub
        return 0;
    }
}
