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
 */

package vavi.sound.opl3;

import java.io.IOException;

/**
 * Generic Protracker Player base class.
 * Ported from adplug's protrack.cpp / protrack.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public abstract class ProtrackPlayer extends Opl3Player {

    protected static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    protected static final int[] sa2_notetable = {
        340, 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647
    };

    protected static final int[] vibratotab = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
        16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };

    protected static final int SPECIALARPLEN = 256;
    protected static final int JUMPMARKER = 0x80;

    protected static final int Standard = 0;
    protected static final int Decimal = 1 << 0;
    protected static final int Faust = 1 << 1;
    protected static final int NoKeyOn = 1 << 2;
    protected static final int Opl3 = 1 << 3;
    protected static final int Tremolo = 1 << 4;
    protected static final int Vibrato = 1 << 5;
    protected static final int Percussion = 1 << 6;

    protected static class Instrument {
        public final int[] data = new int[11];
        public int arpstart;
        public int arpspeed;
        public int arppos;
        public int arpspdcnt;
        public int misc;
        public int slide; // signed
    }

    protected static class Tracks {
        public int note;
        public int command;
        public int inst;
        public int param2;
        public int param1;
    }

    protected static class Channel {
        public int freq;
        public int nextfreq;
        public int oct;
        public int vol1;
        public int vol2;
        public int inst;
        public int fx;
        public int info1;
        public int info2;
        public int key;
        public int nextoct;
        public int note;
        public int portainfo;
        public int vibinfo1;
        public int vibinfo2;
        public int arppos;
        public int arpspdcnt;
        public int trigger; // signed
    }

    protected Instrument[] inst;
    protected Tracks[][] tracks;
    protected int[] order;
    protected int[] arplist;
    protected int[] arpcmd;
    protected Channel[] channel;
    protected int initspeed = 6;
    protected int nop;
    protected int activechan = 0xffffffff;
    protected int flags = Standard;
    protected int curchip;

    protected int[][] trackord;
    protected int tempo;
    protected int bpm;
    protected int length;
    protected int restartpos;
    
    protected int npats;
    protected int nrows;
    protected int nchans;

    private int speed;
    private int del;
    private int songend;
    private int regbd;
    private final int[] notetable = new int[12];
    
    private int rw;
    private int ord;

    protected ProtrackPlayer() {
        super();
        curchip = 0;
        reallocOrder(128);
        reallocPatterns(64, 64, 9);
        reallocInstruments(250);
        initNotetable(sa2_notetable);
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public boolean update() throws IOException {
        int pattbreak = 0;
        int donote;
        int pattnr;
        int oplchan;
        int info1;
        int info2;
        int info;
        int pattern_delay = 0;
        int row;

        if (speed == 0) {
            return false;
        }

        for (int chan = 0; chan < nchans; chan++) {
            oplchan = setOplChip(chan);

            if (arplist != null && arpcmd != null && inst[channel[chan].inst].arpstart != 0) {
                if (channel[chan].arpspdcnt > 0) {
                    channel[chan].arpspdcnt--;
                } else {
                    int arppos = channel[chan].arppos;
                    int cmdVal = arpcmd[arppos] & 0xff;
                    if (cmdVal != 255) {
                        switch (cmdVal) {
                            case 252 -> {
                                channel[chan].vol1 = arplist[arppos] & 0xff;
                                if (channel[chan].vol1 > 63) {
                                    channel[chan].vol1 = 63;
                                }
                                channel[chan].vol2 = channel[chan].vol1;
                                setvolume(chan);
                            }
                            case 253 -> {
                                channel[chan].key = 0;
                                setfreq(chan);
                            }
                            case 254 -> {
                                channel[chan].arppos = arplist[arppos] & 0xff;
                            }
                            default -> {
                                if (cmdVal != 0) {
                                    if (cmdVal / 10 != 0) {
                                        write(curchip, 0xe3 + op_table[oplchan], cmdVal / 10 - 1);
                                    }
                                    if (cmdVal % 10 != 0) {
                                        write(curchip, 0xe0 + op_table[oplchan], (cmdVal % 10) - 1);
                                    }
                                    if (cmdVal < 10) {
                                        write(curchip, 0xe0 + op_table[oplchan], cmdVal - 1);
                                    }
                                }
                            }
                        }
                        if (cmdVal != 252) {
                            int arpListVal = arplist[channel[chan].arppos] & 0xff;
                            if (arpListVal <= 96) {
                                setnote(chan, channel[chan].note + arpListVal);
                            }
                            if (arpListVal >= 100) {
                                setnote(chan, arpListVal - 100);
                            }
                        } else {
                            setnote(chan, channel[chan].note);
                        }
                        setfreq(chan);
                        if (cmdVal != 255) {
                            channel[chan].arppos++;
                        }
                        channel[chan].arpspdcnt = inst[channel[chan].inst].arpspeed - 1;
                    }
                }
            }

            info1 = channel[chan].info1;
            info2 = channel[chan].info2;
            if ((flags & Decimal) != 0) {
                info = info1 * 10 + info2;
            } else {
                info = (info1 << 4) + info2;
            }

            switch (channel[chan].fx) {
                case 0 -> {
                    if (info != 0) {
                        if (channel[chan].trigger < 2) {
                            channel[chan].trigger++;
                        } else {
                            channel[chan].trigger = 0;
                        }
                        switch (channel[chan].trigger) {
                            case 0 -> setnote(chan, channel[chan].note);
                            case 1 -> setnote(chan, channel[chan].note + info1);
                            case 2 -> setnote(chan, channel[chan].note + info2);
                        }
                        setfreq(chan);
                    }
                }
                case 1 -> {
                    slideUp(chan, info);
                    setfreq(chan);
                }
                case 2 -> {
                    slideDown(chan, info);
                    setfreq(chan);
                }
                case 3 -> tonePortamento(chan, channel[chan].portainfo);
                case 4 -> vibrato(chan, channel[chan].vibinfo1, channel[chan].vibinfo2);
                case 5, 6 -> {
                    if (channel[chan].fx == 5) {
                        tonePortamento(chan, channel[chan].portainfo);
                    } else {
                        vibrato(chan, channel[chan].vibinfo1, channel[chan].vibinfo2);
                    }
                    processCase10(chan, info1, info2);
                }
                case 10 -> processCase10(chan, info1, info2);
                case 14 -> {
                    if (info1 == 3) {
                        if ((del % (info2 + 1)) == 0) {
                            playnote(chan);
                        }
                    }
                }
                case 16 -> {
                    if ((del % 4) == 0) {
                        if (info1 != 0) {
                            volUpAlt(chan, info1);
                        } else {
                            volDownAlt(chan, info2);
                        }
                        setvolume(chan);
                    }
                }
                case 20 -> {
                    if (info < 50) {
                        volDownAlt(chan, info);
                    } else {
                        volUpAlt(chan, info - 50);
                    }
                    setvolume(chan);
                }
                case 26 -> {
                    if (info1 != 0) {
                        volUp(chan, info1);
                    } else {
                        volDown(chan, info2);
                    }
                    setvolume(chan);
                }
                case 28 -> {
                    if (info1 != 0) {
                        slideUp(chan, 1);
                        channel[chan].info1--;
                    }
                    if (info2 != 0) {
                        slideDown(chan, 1);
                        channel[chan].info2--;
                    }
                    setfreq(chan);
                }
            }
        }

        if (del > 0) {
            del--;
            return songend == 0;
        }

        if (!resolveOrder()) {
            return songend == 0;
        }
        pattnr = order[ord];

        row = rw;
        for (int chan = 0; chan < nchans; chan++) {
            oplchan = setOplChip(chan);

            if (((activechan >> (31 - chan)) & 1) == 0) {
                continue;
            }
            int track = trackord[pattnr][chan];
            if (track == 0) {
                continue;
            }
            track--;

            donote = 0;
            int instVal = tracks[track][row].inst;
            if (instVal != 0) {
                channel[chan].inst = instVal - 1;
                if ((flags & Faust) == 0) {
                    channel[chan].vol1 = 63 - (inst[channel[chan].inst].data[10] & 63);
                    channel[chan].vol2 = 63 - (inst[channel[chan].inst].data[9] & 63);
                    setvolume(chan);
                }
            }

            int noteVal = tracks[track][row].note;
            if (noteVal != 0 && tracks[track][row].command != 3) {
                channel[chan].note = noteVal;
                setnote(chan, noteVal);
                channel[chan].nextfreq = channel[chan].freq;
                channel[chan].nextoct = channel[chan].oct;
                channel[chan].arppos = inst[channel[chan].inst].arpstart;
                channel[chan].arpspdcnt = 0;
                if (noteVal != 127) {
                    donote = 1;
                }
            }
            channel[chan].fx = tracks[track][row].command;
            channel[chan].info1 = tracks[track][row].param1;
            channel[chan].info2 = tracks[track][row].param2;

            if (donote != 0) {
                playnote(chan);
            }

            info1 = channel[chan].info1;
            info2 = channel[chan].info2;
            if ((flags & Decimal) != 0) {
                info = info1 * 10 + info2;
            } else {
                info = (info1 << 4) + info2;
            }

            switch (channel[chan].fx) {
                case 3 -> {
                    if (noteVal != 0) {
                        if (noteVal < 13) {
                            channel[chan].nextfreq = notetable[noteVal - 1];
                        } else {
                            if (noteVal % 12 > 0) {
                                channel[chan].nextfreq = notetable[(noteVal % 12) - 1];
                            } else {
                                channel[chan].nextfreq = notetable[11];
                            }
                        }
                        channel[chan].nextoct = (noteVal - 1) / 12;
                        if (noteVal == 127) {
                            channel[chan].nextfreq = channel[chan].freq;
                            channel[chan].nextoct = channel[chan].oct;
                        }
                    }
                    if (info != 0) {
                        channel[chan].portainfo = info;
                    }
                }
                case 4 -> {
                    if (info != 0) {
                        channel[chan].vibinfo1 = info1;
                        channel[chan].vibinfo2 = info2;
                    }
                }
                case 7 -> tempo = info;
                case 8 -> {
                    channel[chan].key = 0;
                    setfreq(chan);
                }
                case 9 -> {
                    if (info1 != 0) {
                        channel[chan].vol1 = info1 * 7;
                    } else {
                        channel[chan].vol2 = info2 * 7;
                    }
                    setvolume(chan);
                }
                case 11 -> {
                    pattbreak = 1;
                    rw = 0;
                    if (info <= ord) {
                        songend = 1;
                    }
                    ord = info;
                }
                case 12 -> {
                    channel[chan].vol1 = info;
                    channel[chan].vol2 = info;
                    if (channel[chan].vol1 > 63) channel[chan].vol1 = 63;
                    if (channel[chan].vol2 > 63) channel[chan].vol2 = 63;
                    setvolume(chan);
                }
                case 13 -> {
                    if (pattbreak == 0) {
                        pattbreak = 1;
                        rw = info < nrows ? info : 0;
                        ord++;
                    }
                }
                case 14 -> {
                    switch (info1) {
                        case 0 -> {
                            if (info2 != 0) {
                                regbd |= 128;
                            } else {
                                regbd &= 127;
                            }
                            write(curchip, 0xbd, regbd);
                        }
                        case 1 -> {
                            if (info2 != 0) {
                                regbd |= 64;
                            } else {
                                regbd &= 191;
                            }
                            write(curchip, 0xbd, regbd);
                        }
                        case 4 -> {
                            volUpAlt(chan, info2);
                            setvolume(chan);
                        }
                        case 5 -> {
                            volDownAlt(chan, info2);
                            setvolume(chan);
                        }
                        case 6 -> {
                            slideUp(chan, info2);
                            setfreq(chan);
                        }
                        case 7 -> {
                            slideDown(chan, info2);
                            setfreq(chan);
                        }
                        case 8 -> pattern_delay = info2 * speed;
                    }
                }
                case 15 -> {
                    if (info <= 0x1f) {
                        speed = info;
                    }
                    if (info >= 0x32) {
                        tempo = info;
                    }
                    if (info == 0) {
                        songend = 1;
                    }
                }
                case 17 -> {
                    channel[chan].vol1 = info;
                    if (channel[chan].vol1 > 63) channel[chan].vol1 = 63;
                    if ((inst[channel[chan].inst].data[0] & 1) != 0) {
                        channel[chan].vol2 = info;
                        if (channel[chan].vol2 > 63) channel[chan].vol2 = 63;
                    }
                    setvolume(chan);
                }
                case 18 -> {
                    if (info <= 31 && info > 0) {
                        speed = info;
                    }
                    if (info > 31 || info == 0) {
                        tempo = info;
                    }
                }
                case 19 -> {
                    speed = (info != 0 ? info : 1);
                }
                case 21 -> {
                    if (info <= 63) {
                        channel[chan].vol2 = info;
                    } else {
                        channel[chan].vol2 = 63;
                    }
                    setvolume(chan);
                }
                case 22 -> {
                    if (info <= 63) {
                        channel[chan].vol1 = info;
                    } else {
                        channel[chan].vol1 = 63;
                    }
                    setvolume(chan);
                }
                case 23 -> {
                    slideUp(chan, info);
                    setfreq(chan);
                }
                case 24 -> {
                    slideDown(chan, info);
                    setfreq(chan);
                }
                case 25 -> {
                    if (info1 != 0x0f) {
                        write(curchip, 0xe3 + op_table[oplchan], info1);
                    }
                    if (info2 != 0x0f) {
                        write(curchip, 0xe0 + op_table[oplchan], info2);
                    }
                }
                case 27 -> {
                    if (info1 != 0) {
                        regbd |= 128;
                    } else {
                        regbd &= 127;
                    }
                    if (info2 != 0) {
                        regbd |= 64;
                    } else {
                        regbd &= 191;
                    }
                    write(curchip, 0xbd, regbd);
                }
                case 29 -> pattern_delay = info;
            }
        }

        del = speed - 1 + pattern_delay;

        if (pattbreak == 0) {
            rw++;
            if (rw >= nrows) {
                rw = 0;
                ord++;
            }
        }

        resolveOrder();
        return songend == 0;
    }

    private void processCase10(int chan, int info1, int info2) {
        if ((del % 4) == 0) {
            if (info1 != 0) {
                volUp(chan, info1);
            } else {
                volDown(chan, info2);
            }
            setvolume(chan);
        }
    }

    protected int setOplChip(int chan) {
        int newchip = chan < 9 ? 0 : 1;
        if (newchip != curchip) {
            curchip = newchip;
        }
        return chan % 9;
    }

    protected boolean resolveOrder() {
        if (ord < length) {
            while (order[ord] >= JUMPMARKER) {
                int neword = order[ord] - JUMPMARKER;
                if (neword <= ord) {
                    songend = 1;
                }
                if (neword == ord) {
                    return false;
                }
                ord = neword;
            }
        } else {
            songend = 1;
            ord = restartpos;
        }
        return true;
    }

    @Override
    public void rewind(int subSong) throws IOException {
        songend = 0;
        del = 0;
        ord = 0;
        rw = 0;
        regbd = 0;
        tempo = bpm;
        speed = initspeed;

        for (int i = 0; i < nchans; i++) {
            channel[i] = new Channel();
        }

        if (nop == 0) {
            for (int i = 0; i < length; i++) {
                nop = Math.max(order[i], nop);
            }
        }

        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
            write(1, i, 0);
        }
        write(0, 1, 32);

        if ((flags & Opl3) != 0) {
            write(1, 1, 32);
            write(1, 5, 1);
        }

        if ((flags & Tremolo) != 0) regbd |= 128;
        if ((flags & Vibrato) != 0) regbd |= 64;
        if (regbd != 0) {
            write(0, 0xbd, regbd);
        }
    }

    protected void initTrackord() {
        for (int i = 0; i < npats * nchans; i++) {
            trackord[i / nchans][i % nchans] = i + 1;
        }
    }

    protected boolean initSpecialarp() {
        arplist = new int[SPECIALARPLEN];
        arpcmd = new int[SPECIALARPLEN];
        return true;
    }

    protected void initNotetable(int[] newnotetable) {
        System.arraycopy(newnotetable, 0, notetable, 0, 12);
    }

    protected boolean reallocOrder(int len) {
        order = new int[len];
        return true;
    }

    protected boolean reallocPatterns(int pats, int rows, int chans) {
        npats = pats;
        nrows = rows;
        nchans = chans;

        tracks = new Tracks[pats * chans][rows];
        for (int i = 0; i < pats * chans; i++) {
            tracks[i] = new Tracks[rows];
            for (int j = 0; j < rows; j++) {
                tracks[i][j] = new Tracks();
            }
        }
        trackord = new int[pats][chans];
        channel = new Channel[chans];
        for (int i = 0; i < chans; i++) {
            channel[i] = new Channel();
        }

        return true;
    }

    protected boolean reallocInstruments(int len) {
        inst = new Instrument[len];
        for (int i = 0; i < len; i++) {
            inst[i] = new Instrument();
        }
        return true;
    }

    private void setvolume(int chan) {
        int oplchan = setOplChip(chan);
        if ((flags & Faust) != 0) {
            setvolumeAlt(chan);
        } else {
            write(curchip, 0x40 + op_table[oplchan], 63 - channel[chan].vol2 + (inst[channel[chan].inst].data[9] & 192));
            write(curchip, 0x43 + op_table[oplchan], 63 - channel[chan].vol1 + (inst[channel[chan].inst].data[10] & 192));
        }
    }

    private void setvolumeAlt(int chan) {
        int oplchan = setOplChip(chan);
        int ivol2 = inst[channel[chan].inst].data[9] & 63;
        int ivol1 = inst[channel[chan].inst].data[10] & 63;

        write(curchip, 0x40 + op_table[oplchan], (((63 - (channel[chan].vol2 & 63)) + ivol2) >> 1) + (inst[channel[chan].inst].data[9] & 192));
        write(curchip, 0x43 + op_table[oplchan], (((63 - (channel[chan].vol1 & 63)) + ivol1) >> 1) + (inst[channel[chan].inst].data[10] & 192));
    }

    private void setfreq(int chan) {
        int oplchan = setOplChip(chan);
        write(curchip, 0xa0 + oplchan, channel[chan].freq & 255);
        if (channel[chan].key != 0) {
            write(curchip, 0xb0 + oplchan, (((channel[chan].freq & 768) >> 8) + (channel[chan].oct << 2)) | 32);
        } else {
            write(curchip, 0xb0 + oplchan, ((channel[chan].freq & 768) >> 8) + (channel[chan].oct << 2));
        }
    }

    private void playnote(int chan) {
        int oplchan = setOplChip(chan);
        int op = op_table[oplchan];
        int insnr = channel[chan].inst;

        if ((flags & NoKeyOn) == 0) {
            write(curchip, 0xb0 + oplchan, 0); // stop old note
        }

        write(curchip, 0x20 + op, inst[insnr].data[1]);
        write(curchip, 0x23 + op, inst[insnr].data[2]);
        write(curchip, 0x60 + op, inst[insnr].data[3]);
        write(curchip, 0x63 + op, inst[insnr].data[4]);
        write(curchip, 0x80 + op, inst[insnr].data[5]);
        write(curchip, 0x83 + op, inst[insnr].data[6]);
        write(curchip, 0xe0 + op, inst[insnr].data[7]);
        write(curchip, 0xe3 + op, inst[insnr].data[8]);
        write(curchip, 0xc0 + oplchan, inst[insnr].data[0]);
        write(curchip, 0xbd, inst[insnr].misc);

        channel[chan].key = 1;
        setfreq(chan);

        if ((flags & Faust) != 0) {
            channel[chan].vol2 = 63;
            channel[chan].vol1 = 63;
        }
        setvolume(chan);
    }

    private void setnote(int chan, int note) {
        if (note == 127) {
            channel[chan].key = 0;
            setfreq(chan);
            return;
        }
        if (note > 96) note = 96;
        else if (note < 1) note = 1;

        channel[chan].freq = notetable[(note - 1) % 12];
        channel[chan].oct = (note - 1) / 12;
        channel[chan].freq = (channel[chan].freq + inst[channel[chan].inst].slide) & 0xffff;
    }

    // freq is unsigned short in adplug: slides wrap mod 65536 and the
    // range checks are unsigned comparisons on the wrapped value

    private void slideDown(int chan, int amount) {
        channel[chan].freq = (channel[chan].freq - amount) & 0xffff;
        if (channel[chan].freq <= 342) {
            if (channel[chan].oct != 0) {
                channel[chan].oct--;
                channel[chan].freq = (channel[chan].freq << 1) & 0xffff;
            } else {
                channel[chan].freq = 342;
            }
        }
    }

    private void slideUp(int chan, int amount) {
        channel[chan].freq = (channel[chan].freq + amount) & 0xffff;
        if (channel[chan].freq >= 686) {
            if (channel[chan].oct < 7) {
                channel[chan].oct++;
                channel[chan].freq >>= 1;
            } else {
                channel[chan].freq = 686;
            }
        }
    }

    private void tonePortamento(int chan, int info) {
        if (channel[chan].freq + (channel[chan].oct << 10) < channel[chan].nextfreq + (channel[chan].nextoct << 10)) {
            slideUp(chan, info);
            if (channel[chan].freq + (channel[chan].oct << 10) > channel[chan].nextfreq + (channel[chan].nextoct << 10)) {
                channel[chan].freq = channel[chan].nextfreq;
                channel[chan].oct = channel[chan].nextoct;
            }
        }
        if (channel[chan].freq + (channel[chan].oct << 10) > channel[chan].nextfreq + (channel[chan].nextoct << 10)) {
            slideDown(chan, info);
            if (channel[chan].freq + (channel[chan].oct << 10) < channel[chan].nextfreq + (channel[chan].nextoct << 10)) {
                channel[chan].freq = channel[chan].nextfreq;
                channel[chan].oct = channel[chan].nextoct;
            }
        }
        setfreq(chan);
    }

    private void vibrato(int chan, int speed, int depth) {
        if (speed == 0 || depth == 0) {
            return;
        }

        if (depth > 14) {
            depth = 14;
        }

        for (int i = 0; i < speed; i++) {
            channel[chan].trigger++;
            while (channel[chan].trigger >= 64) {
                channel[chan].trigger -= 64;
            }
            if (channel[chan].trigger >= 16 && channel[chan].trigger < 48) {
                slideDown(chan, vibratotab[channel[chan].trigger - 16] / (16 - depth));
            }
            if (channel[chan].trigger < 16) {
                slideUp(chan, vibratotab[channel[chan].trigger + 16] / (16 - depth));
            }
            if (channel[chan].trigger >= 48) {
                slideUp(chan, vibratotab[channel[chan].trigger - 48] / (16 - depth));
            }
        }
        setfreq(chan);
    }

    private void volUp(int chan, int amount) {
        if (channel[chan].vol1 + amount < 63) {
            channel[chan].vol1 += amount;
        } else {
            channel[chan].vol1 = 63;
        }

        if (channel[chan].vol2 + amount < 63) {
            channel[chan].vol2 += amount;
        } else {
            channel[chan].vol2 = 63;
        }
    }

    private void volDown(int chan, int amount) {
        if (channel[chan].vol1 - amount > 0) {
            channel[chan].vol1 -= amount;
        } else {
            channel[chan].vol1 = 0;
        }

        if (channel[chan].vol2 - amount > 0) {
            channel[chan].vol2 -= amount;
        } else {
            channel[chan].vol2 = 0;
        }
    }

    private void volUpAlt(int chan, int amount) {
        if (channel[chan].vol1 + amount < 63) {
            channel[chan].vol1 += amount;
        } else {
            channel[chan].vol1 = 63;
        }
        if ((inst[channel[chan].inst].data[0] & 1) != 0) {
            if (channel[chan].vol2 + amount < 63) {
                channel[chan].vol2 += amount;
            } else {
                channel[chan].vol2 = 63;
            }
        }
    }

    private void volDownAlt(int chan, int amount) {
        if (channel[chan].vol1 - amount > 0) {
            channel[chan].vol1 -= amount;
        } else {
            channel[chan].vol1 = 0;
        }
        if ((inst[channel[chan].inst].data[0] & 1) != 0) {
            if (channel[chan].vol2 - amount > 0) {
                channel[chan].vol2 -= amount;
            } else {
                channel[chan].vol2 = 0;
            }
        }
    }

    protected String readString(byte[] buf, int offset, int length) {
        int len = 0;
        while (len < length && offset + len < buf.length && buf[offset + len] != 0) {
            len++;
        }
        return new String(buf, offset, len, java.nio.charset.StandardCharsets.US_ASCII);
    }
}
