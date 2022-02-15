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

package vavi.sound.opl3;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
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
import vavi.sound.midi.opl3.Opl3Soundbank;
import vavi.sound.midi.opl3.Opl3Synthesizer;
import vavi.sound.midi.opl3.Opl3Synthesizer.Context;
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
public class MidPlayer extends Opl3Player implements Sequencer {

    static Logger logger = Logger.getLogger(MidPlayer.class.getName());

    /** midi like file types */
    public enum FileType {
        LUCAS(new LucasFile(), "LucasArts AdLib MIDI"),
        MIDI(new MidiFile(), "General MIDI"),
        CMF(new CmfFile(), "Creative Music Format (CMF MIDI)"),
        SIERRA(new SierraFile(), "Sierra On-Line EGA MIDI"),
        ADVSIERRA(new AdvancedSierraFile(), "Sierra On-Line VGA MIDI"),
        OLDLUCAS(new OldLucasFile(), "Lucasfilm Adlib MIDI");
        public MidiTypeFile midiTypeFile;
        String desc;
        FileType(MidiTypeFile midiTypeFile, String desc) {
            this.midiTypeFile = midiTypeFile;
            this.desc = desc;
        }
        /**
         * is midi like file or not
         * @throws NoSuchElementException when not found
         */
        public static FileType getFileType(InputStream is) {
            return Arrays.stream(values()).filter(e -> e.midiTypeFile.matchFormat(is)).findFirst().get();
        }
    }

    public static abstract class MidiTypeFile {
        boolean matchFormat(InputStream bitStream) {
            DataInputStream dis = new DataInputStream(bitStream);
            try {
                dis.mark(markSize());
                return matchFormatImpl(dis);
            } catch (IOException e) {
Debug.println(Level.WARNING, e);
                return false;
            } finally {
                try {
                    dis.reset();
                } catch (IOException e) {
Debug.println(Level.SEVERE, e);
                }
            }
        }
        abstract int markSize();
        abstract boolean matchFormatImpl(DataInputStream dis) throws IOException;
        abstract void rewind(int subSong, MidPlayer player) throws IOException;
        public abstract void init(Context context);
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

    private static final int MAX_CHANNELS = 16;

    // data length
    int flen;
    private DataInputStream data;
    // data pos
    int pos;

    Opl3Soundbank soundBank = new Opl3Soundbank(Adlib.midi_fm_instruments);

    private FileType type;
    int subsongs;

    MidiTrack[] tracks = new MidiTrack[MAX_CHANNELS];

    int deltas;
    int msqtr;
    float fwait;
    private int iwait;
    boolean doing;
    // number of instruments
    int tins;

    private Opl3Synthesizer synthesizer = new Opl3Synthesizer();

    private Transmitter transmitter = new Opl3Transmitter();

    @Override
    public boolean matchFormat(InputStream bitStream) {
logger.fine("\n" + StringUtil.getDump(bitStream, 0, 256));
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

    /**
     * variable length
     * TODO deprecate
     */
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

    @Override
    public boolean update() throws IOException {
        if (doing) {
            for (int t = 0; t < MAX_CHANNELS; ++t) {
                if (tracks[t].on) {
                    pos = tracks[t].pos; // TODO substitution for pos!!! track > 0 cause bug
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
        boolean eos = true;

        while (iwait == 0 && eos) {
            for (int t = 0; t < MAX_CHANNELS; ++t) {
                if (tracks[t].on && tracks[t].iwait == 0 && tracks[t].pos < tracks[t].tend) {
                    pos = tracks[t].pos; // TODO substitution for pos!!! track > 0 cause bug
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
                                b[1] = (byte) (l & 0xff); // TODO variable length not considered
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
                                    logger.info(String.format("meta: %02x", v));
                                    takeBE(data.available()); // TODO out of spec.
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

            eos = false; // end of song.
            iwait = 0;

            for (int t = 0; t < MAX_CHANNELS; ++t) {
                if (tracks[t].on && tracks[t].pos < tracks[t].tend) {
                    eos = true; // not yet...
                }
            }

            if (eos) {
                iwait = 0xffffff; // bigger than any wait can be!

                for (int t = 0; t < MAX_CHANNELS; ++t) {
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
        if (iwait != 0 && eos) {
            for (int t = 0; t < MAX_CHANNELS; ++t) {
                if (tracks[t].on) {
                    tracks[t].iwait -= iwait;
                }
            }

            fwait = 1.0F / ((float) iwait / (float) deltas * (msqtr / 1000000.0F));
        } else {
            fwait = 50.0F; // 1/50th of a second
        }

        for (int t = 0; t < MAX_CHANNELS; ++t) {
            if (tracks[t].on) {
                if (tracks[t].pos < tracks[t].tend) {
                    logger.fine(String.format("iwait: %d", tracks[t].iwait));
                } else {
                    logger.fine("stop");
                }
            }
        }

        return eos;
    }

    /**
     * @param data {@link SysexMessage#getData()}
     */
    public static int[] fromSysex(byte[] data) {
        int pos = 1;
        int[] x = new int[11];
        x[0] = ((data[pos + 4] & 0xff) << 4) + (data[pos + 5] & 0xff);
        x[2] = 0xff - (((data[pos + 6] & 0xff) << 4) + data[pos + 7] & 0x3f);
        x[4] = 0xff - (((data[pos + 8] & 0xff) << 4) + (data[pos + 9] & 0xff));
        x[6] = 0xff - (((data[pos + 10] & 0xff) << 4) + (data[pos + 11] & 0xff));
        x[8] = ((data[pos + 12] & 0xff) << 4) + (data[pos + 13] & 0xff);
        x[1] = ((data[pos + 14] & 0xff) << 4) + (data[pos + 15] & 0xff);
        x[3] = 0xff - (((data[pos + 16] & 0xff) << 4) + (data[pos + 17] & 0x3f));
        x[5] = 0xff - (((data[pos + 18] & 0xff) << 4) + (data[pos + 19] & 0xff));
        x[7] = 0xff - (((data[pos + 20] & 0xff) << 4) + (data[pos + 21] & 0xff));
        x[9] = ((data[pos + 22] & 0xff) << 4) + (data[pos + 23] & 0xff);
        x[10] = ((data[pos + 24] & 0xff) << 4) + (data[pos + 24] & 0xff);
        return x;
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

        for (int t = 0; t < MAX_CHANNELS; ++t) {
            tracks[t].reset();
        }

        pos = 0;
        type.midiTypeFile.rewind(subSong, this);

        for (int t = 0; t < MAX_CHANNELS; ++t) {
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
        @SuppressWarnings("unused")
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

    private static final String version = "0.0.1.";

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("OPL3 MIDI Sequencer",
                            "Vavisoft",
                            "Software sequencer for OPL3",
                            "Version " + version) {};

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        // TODO Auto-generated method stub

    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isOpen() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getMaxReceivers() {
        return 1;
    }

    @Override
    public int getMaxTransmitters() {
        return 1;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return transmitter.getReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        if (transmitter.getReceiver() != null) {
            return Arrays.asList(transmitter.getReceiver());
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return transmitter;
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return Arrays.asList(transmitter);
    }

    @Override
    public void setSequence(Sequence sequence) throws InvalidMidiDataException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
        load(stream);
    }

    @Override
    public Sequence getSequence() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isRunning() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void startRecording() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stopRecording() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isRecording() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void recordEnable(Track track, int channel) {
        // TODO Auto-generated method stub

    }

    @Override
    public void recordDisable(Track track) {
        // TODO Auto-generated method stub

    }

    @Override
    public float getTempoInBPM() {
        return msqtr;
    }

    @Override
    public void setTempoInBPM(float bpm) {
        this.msqtr = (int) bpm;
    }

    @Override
    public float getTempoInMPQ() {
        return msqtr;
    }

    @Override
    public void setTempoInMPQ(float mpq) {
        this.msqtr = (int) mpq;
    }

    @Override
    public void setTempoFactor(float factor) {
        // TODO Auto-generated method stub

    }

    @Override
    public float getTempoFactor() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTickLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getTickPosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setTickPosition(long tick) {
        // TODO Auto-generated method stub

    }

    @Override
    public long getMicrosecondLength() {
        return getTotalMiliseconds() / 10;
    }

    @Override
    public long getMicrosecondPosition() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMasterSyncMode(SyncMode sync) {
        // TODO Auto-generated method stub

    }

    @Override
    public SyncMode getMasterSyncMode() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SyncMode[] getMasterSyncModes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setSlaveSyncMode(SyncMode sync) {
        // TODO Auto-generated method stub

    }

    @Override
    public SyncMode getSlaveSyncMode() {
        // TODO Auto-generated method stub
        return null;
    }

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

    @Override
    public void setTrackSolo(int track, boolean solo) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getTrackSolo(int track) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addMetaEventListener(MetaEventListener listener) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeMetaEventListener(MetaEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setLoopStartPoint(long tick) {
        // TODO Auto-generated method stub

    }

    @Override
    public long getLoopStartPoint() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLoopEndPoint(long tick) {
        // TODO Auto-generated method stub

    }

    @Override
    public long getLoopEndPoint() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLoopCount(int count) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLoopCount() {
        // TODO Auto-generated method stub
        return 0;
    }
}
