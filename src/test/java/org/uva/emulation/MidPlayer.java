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
 */

package org.uva.emulation;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import vavi.sound.midi.MidiConstants;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * MIDI & MIDI-like file player
 * <pre>
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
 * </pre>
 *
 * @author Phil Hassey (www.imitationpickles.org, philhassey@hotmail.com)
 * @version
 * <li> 6/2/2000 phil hassey - v1.0 relased
 * <pre>
 *      Status:  LAA is almost perfect
 *                      - some volumes are a bit off (instrument too quiet)
 *               MID is fine (who wants to listen to MIDI vid adlib anyway)
 *               CMF is okay (still needs the adlib rythm mode implemented
 *                            for real)
 * </pre>
 * <li> 6/6/2000 phil hassey -
 * <pre>
 *      Status:  SCI: there are two SCI formats, original and advanced.
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
 * </pre>
 * <li> 8/16/2000 phil hassey - LAA: now EGA and VGA lucas games work pretty well
 * <li> 10/15/2005 Simon Peter - Added rhythm mode support for CMF format.
 * <li> 10/15/2005 Phil Hassey - last updated
 * <li> 09/13/2008 Adam Nielsen (malvineous@shikadi.net) - Changes
 * <ul>
 *      <li> Fixed a couple of CMF rhythm mode bugs
 *      <li> Disabled note velocity for CMF files
 *      <li> Added support for nonstandard CMF AM+VIB controller (for VGFM CMFs)
 * </ul>
 * <li> Allegro - for the midi instruments and the midi volume table
 * <li> SCUMM Revisited - for getting the .LAA / .MIDs out of those LucasArts files.
 * <li> FreeSCI - for some information on the sci music files
 * <li> SD - the SCI Decoder (to get all .sci out of the Sierra files)
 */
class MidPlayer extends Opl3Player implements Sequencer {

    static Logger logger = Logger.getLogger(MidPlayer.class.getName());

    enum FileType {
        LUCAS(new LucasFile(), "LucasArts AdLib MIDI"),
        MIDI(new MidiFile(), "General MIDI"),
        CMF(new CmfFile(), "Creative Music Format (CMF MIDI)"),
        SIERRA(new SierraFile(), "Sierra On-Line EGA MIDI"),
        ADVSIERRA(new AdvancedSierraFile(), "Sierra On-Line VGA MIDI"),
        OLDLUCAS(new OldLucasFile(), "Lucasfilm Adlib MIDI");
        MidiTypeFile midiTypeFile;
        String desc;
        FileType(MidiTypeFile midiTypeFile, String desc) {
            this.midiTypeFile = midiTypeFile;
            this.desc = desc;
        }
        /** @throws NoSuchElementException when not found */
        static FileType getFileType(InputStream is) {
            return Arrays.stream(values()).filter(e -> e.midiTypeFile.matchFormat(is)).findFirst().get();
        }
    }

    static abstract class MidiTypeFile {
        boolean matchFormat(InputStream bitStream) {
            DataInputStream dis = new DataInputStream(bitStream);
            try {
                dis.mark(markSize());
                return matchFormatImpl(dis);
            } catch (IOException e) {
Debug.println(Level.FINE, e);
                return false;
            } finally {
                try {
                    dis.reset();
                } catch (IOException e) {
Debug.println(Level.FINE, e);
                }
            }
        }
        abstract int markSize();
        abstract boolean matchFormatImpl(DataInputStream dis) throws IOException;
        abstract void rewind(int subSong, MidPlayer player) throws IOException;
        abstract void init(Opl3Synthesizer synthesizer);
    }

    static class MidiTrack {
        int tend;
        int spos;
        int pos;
        int iwait;
        boolean on;
        int pv;
        void reset() {
            this.tend = 0;
            this.spos = 0;
            this.pos = 0;
            this.iwait = 0;
            this.on = false;
            this.pv = 0;
        }
        public void initOn() {
            if (this.on) {
                this.pos = this.spos;
                this.pv = 0;
                this.iwait = 0;
            }
        }
    }

    // data length
    int flen;
    DataInputStream data;
    // data pos
    int pos;

    Opl3SoundBank soundBank = new Opl3SoundBank();

    FileType type;
    int subsongs;

    MidiTrack[] tracks = new MidiTrack[16];

    int deltas;
    int msqtr;
    float fwait;
    int iwait;
    boolean doing;
    // number of instruments
    int tins;

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            type = FileType.getFileType(bitStream);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    @Override
    public int getTotalMiliseconds() {
        return 0;
    }

    public MidPlayer() {
        for (int t = 0; t < tracks.length; ++t) {
            tracks[t] = new MidiTrack();
        }
    }

    // TODO deprecate
    int peek(int pos) throws IOException {
        int l = pos + 1 - this.pos;
        data.mark(l);
        int r = 0;
        if (pos >= 0 && pos < flen) {
            while (l-- > 0) {
                r = data.readUnsignedByte();
            }
        }
        data.reset();
        return r;
    }

    // TODO deprecate
    int takeLE(int num) throws IOException {
        int v = 0;

        for (int i = 0; i < num; ++i) {
            v += data.readUnsignedByte() << 8 * i;
            ++pos;
        }

        return v;
    }

    // TODO deprecate
    int takeBE(int num) throws IOException {
        int b = 0;

        for (int i = 0; i < num; ++i) {
            b <<= 8;
            b += data.readUnsignedByte();
            ++pos;
        }

        return b;
    }

    // TODO deprecate
    private int takeLen() throws IOException {
        int b = takeBE(1);
        int v = b & 0x7f;
        while ((b & 0x80) != 0) {
            b = takeBE(1);
            v = (v << 7) + (b & 0x7F);
        }

        return v;
    }

    @Override
    public void load(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);

        subsongs = 1;
logger.fine("type: " + type);

        dis.reset();
        flen = dis.available();
        this.data = dis;

        rewind(0);
    }

    Opl3Synthesizer synthesizer = new Opl3Synthesizer();
    Transmitter transmitter = new Opl3Transmitter();

    @Override
    public boolean update() throws IOException {
        if (doing) {
            for (int t = 0; t < 16; ++t) {
                if (tracks[t].on) {
                    pos = tracks[t].pos;
                    if (type != FileType.SIERRA && type != FileType.ADVSIERRA) {
                        tracks[t].iwait += takeLen();
                    } else {
                        tracks[t].iwait += takeBE(1);
                    }

                    tracks[t].pos = pos;
                }
            }

            doing = false;
        }

        iwait = 0;
        boolean ret = true;

        while (iwait == 0 && ret) {
            for (int t = 0; t < 16; ++t) {
                if (tracks[t].on && tracks[t].iwait == 0 && tracks[t].pos < tracks[t].tend) {
                    pos = tracks[t].pos;
                    data.mark(1);
                    int v = takeBE(1);
                    if (v < 0x80) {
                        // CMF
                        v = tracks[t].pv;
                        data.reset();
                        --pos;
                    }

                    tracks[t].pv = v;
                    int c = v & 0x0f;
                    logger.fine(String.format("[%2X]", v));

                    int data1;
                    int data2;

                    MidiMessage midiMessage = null;

                    try {
                        switch (v & 0xf0) {
                        case 0x80: // note off
                            data1 = takeBE(1);
                            data2 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.NOTE_OFF, c, data1, data2);
                            break;
                        case 0x90: // note on
                            data1 = takeBE(1);
                            data2 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.NOTE_ON, c, data1, data2);
                            break;
                        case 0xa0: // key after touch
                            data1 = takeBE(1);
                            data2 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.POLY_PRESSURE, c, data1, data2);
                            break;
                        case 0xb0: // control change .. pitch bend?
                            int ctrl = takeBE(1);
                            data2 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.CONTROL_CHANGE, c, ctrl, data2);
                            break;
                        case 0xc0: // patch change
                            data1 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.PROGRAM_CHANGE, c, data1);
                            break;
                        case 0xd0: // channel touch
                            data1 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.CHANNEL_PRESSURE, c, data1);
                            break;
                        case 0xe0: // pitch wheel
                            data1 = takeBE(1);
                            data2 = takeBE(1);
                            midiMessage = new ShortMessage();
                            ((ShortMessage) midiMessage).setMessage(ShortMessage.PITCH_BEND, c, data1, data2);
                            break;
                        case 0xf0:
                            switch (v) {
                            case 0xf0:
                            case 0xf7: // sysex
                                boolean f = false;
                                int l = takeLen();
                                if (peek(pos + l) == 0xf7) {
                                    f = true;
                                }
                                byte[] b = new byte[l + 2];
                                b[0] = (byte) (v & 0xff);
                                b[1] = (byte) (l & 0xff);
                                for (int i = 2; i < b.length; i++) {
                                    b[i] = (byte) (takeBE(1) & 0xff);
                                }
                                logger.fine("sysex:\n" + StringUtil.getDump(b));
                                midiMessage = new SysexMessage();
                                ((SysexMessage) midiMessage).setMessage(b, b.length);
                                logger.fine("sysex: " + midiMessage.getLength());
                                if (f) {
                                    takeBE(1);
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
                                data1 = takeBE(1);
                                data2 = takeBE(1);
                                midiMessage = new ShortMessage();
                                ((ShortMessage) midiMessage).setMessage(ShortMessage.SONG_POSITION_POINTER, c, data1, data2);
                                break;
                            case 0xf3:
                                data1 = takeBE(1);
                                midiMessage = new ShortMessage();
                                ((ShortMessage) midiMessage).setMessage(ShortMessage.SONG_SELECT, c, data1);
                                break;
                            case 0xf6: // something
                            case 0xf8:
                            case 0xfa:
                            case 0xfb:
                            case 0xfc:
                                // this ends the track for sierra.
                                if (type == FileType.SIERRA || type == FileType.ADVSIERRA) {
                                    tracks[t].tend = pos;
                                    logger.info(String.format("endmark: %d -- %x\n", pos, pos));
                                }
                                break;
                            case 0xff: // meta
                                logger.fine("meta:\n" + StringUtil.getDump(data, 0, 64));
                                v = takeBE(1);
                                switch (v) {
                                case 0x2f:
                                    msqtr = takeBE(1);
                                    break;
                                case 0x51:
                                    l = takeLen();
                                    msqtr = takeBE(l); // set tempo
                                    logger.fine(String.format("(qtr=%d)", msqtr));
                                    break;
                                default:
                                    l = takeLen();
                                    logger.fine(String.format("meta: %02x, %02x", v, l));
                                    for (int i = 0; i < l; ++i) {
                                        logger.fine(String.format("%2X ", takeBE(1)));
                                    }
                                    break;
                                }
                                break;
                            }
                            break;
                        default:
                            // if we get down here, a error occurred
                            logger.warning(String.format("!: %02x at %d", v, pos));
                            break;
                        }
                    } catch (InvalidMidiDataException e) {
                        logger.warning(e.getMessage());
                        e.printStackTrace();
                    }

                    if (midiMessage != null) {
                        transmitter.getReceiver().send(midiMessage, -1);
                    }

                    if (pos < tracks[t].tend) {
                        int w;
                        if (type != FileType.SIERRA && type != FileType.ADVSIERRA) {
                            w = takeLen();
                        } else {
                            w = takeBE(1);
                        }

                        tracks[t].iwait = w;
                    } else {
                        tracks[t].iwait = 0;
                    }

                    tracks[t].pos = pos;
                }
            }

            ret = false; // end of song.
            iwait = 0;

            for (int t = 0; t < 16; ++t) {
                if (tracks[t].on && tracks[t].pos < tracks[t].tend) {
                    ret = true; // not yet...
                }
            }

            if (ret) {
                iwait = 0xffffff; // bigger than any wait can be!

                for (int t = 0; t < 16; ++t) {
                    if (tracks[t].on && tracks[t].pos < tracks[t].tend && tracks[t].iwait < iwait) {
                        iwait = tracks[t].iwait;
                    }
                }
            } else {
                try {
                    MidiMessage midiMessage = new MetaMessage(MidiConstants.META_END_OF_TRACK, new byte[0], 0);
                    transmitter.getReceiver().send(midiMessage, -1);
                } catch (InvalidMidiDataException e) {
                    logger.warning(e.getMessage());
                    e.printStackTrace();
                }
            }
        }

//        logger.info(String.format("iwait: %d, deltas: %d, msqtr: %d", iwait, deltas, msqtr));
        if (iwait != 0 && ret) {
            for (int t = 0; t < 16; ++t) {
                if (tracks[t].on) {
                    tracks[t].iwait -= iwait;
                }
            }

            fwait = 1.0F / ((float) iwait / (float) deltas * (msqtr / 1000000.0F));
        } else {
            fwait = 50.0F; // 1/50th of a second
        }

        for (int t = 0; t < 16; ++t) {
            if (tracks[t].on) {
                if (tracks[t].pos < tracks[t].tend) {
                    logger.fine(String.format("iwait: %d", tracks[t].iwait));
                } else {
                    logger.fine("stop");
                }
            }
        }

        return ret;
    }

    @Override
    public float getRefresh() {
        return fwait > 0.01F ? fwait : 0.01F;
    }

    @Override
    public void rewind(int subSong) throws IOException {
        pos = 0;

        tins = 0;
        subsongs = 1;

        deltas = 250; // just a number, not a standard
        msqtr = 500000;
        fwait = 123.0F; // gotta be a small thing... sorta like nothing
        iwait = 0;

        for (int t = 0; t < 16; ++t) {
            tracks[t].reset();
        }

        pos = 0;
        type.midiTypeFile.rewind(subSong, this);

        for (int t = 0; t < 16; ++t) {
            tracks[t].initOn();
        }

        doing = true;
        try {
            synthesizer.open(type, this::write);
            transmitter.setReceiver(synthesizer.getReceiver());
        } catch (MidiUnavailableException e) {
            throw new IOException(e);
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

    @Override
    public void setTrackMute(int track, boolean mute) {
        this.tracks[track].on = mute;
    }

    @Override
    public boolean getTrackMute(int track) {
        return this.tracks[track].on;
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
