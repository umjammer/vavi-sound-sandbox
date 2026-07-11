/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2002 Simon Peter, <dn.tlp@gmx.net>, et al.
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
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * Scream Tracker 3 Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class S3mPlayer extends Opl3Player {

    private static final Logger logger = getLogger(S3mPlayer.class.getName());

    private static final int[] chnresolv = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        0, 1, 2, 3, 4, 5, 6, 7, 8, -1, -1, -1, -1, -1, -1, -1
    };

    private static final int[] notetable = {
        340, 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647
    };

    private static final byte[] vibratotab = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
        16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1
    };

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    protected static class S3mHeader {
        String name;
        int kennung, typ;
        int ordnum, insnum, patnum, flags, cwtv, ffi;
        String scrm;
        int gv, is, it, mv, uc, dp;
        int special;
        final byte[] chanset = new byte[32];
    }

    protected static class S3mInst {
        int type;
        String filename;
        int d00, d01, d02, d03, d04, d05, d06, d07, d08, d09, d0a, d0b, volume, dsk;
        long c2spd;
        String name;
        String scri;
    }

    protected static class S3mPattern {
        int note = 0xFF;
        int oct = 0xFF;
        int instrument = 0;
        int volume = 0xFF;
        int command = 0xFF;
        int info = 0;
    }

    protected static class S3mChannel {
        int freq, nextfreq;
        int oct, vol, inst, fx, info, dualinfo, key, nextoct, trigger, note;
    }

    protected S3mHeader header;
    protected final S3mPattern[][][] pattern = new S3mPattern[99][64][32];
    protected final S3mInst[] inst = new S3mInst[99];
    protected final S3mChannel[] channel = new S3mChannel[9];

    protected final byte[] orders = new byte[256];
    protected int crow, ord, speed, tempo, del, songend, loopstart, loopcnt;

    public S3mPlayer() {
        for (int i = 0; i < 9; i++) {
            channel[i] = new S3mChannel();
        }
        for (int i = 0; i < 256; i++) {
            orders[i] = (byte) 0xFF;
        }
        for (int i = 0; i < 99; i++) {
            for (int j = 0; j < 64; j++) {
                for (int k = 0; k < 32; k++) {
                    pattern[i][j][k] = new S3mPattern();
                }
            }
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Scream Tracker 3 Format", "s3m");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("S3M");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".s3m")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            bitStream.mark(96);
            byte[] header = new byte[96];
            int r = bitStream.read(header);
            if (r == 96) {
                S3mHeader h = loadHeader(header, 0);
                if (h.kennung == 0x1a && h.typ == 16 && h.insnum <= 99 && h.scrm.equals("SCRM")) {
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
        byte[] buf = is.readAllBytes();

        S3mHeader checkhead = loadHeader(buf, 0);
        if (checkhead.kennung != 0x1a || checkhead.typ != 16 || checkhead.insnum > 99) {
            throw new IOException("invalid S3M file");
        }
        if (!checkhead.scrm.equals("SCRM")) {
            throw new IOException("invalid S3M file");
        }

        int off = 96;
        off += checkhead.ordnum;
        int[] insptr = new int[99];
        for (int i = 0; i < checkhead.insnum; i++) {
            insptr[i] = u16(buf, off);
            off += 2;
        }

        boolean adlibins = false;
        for (int i = 0; i < checkhead.insnum; i++) {
            int instOff = insptr[i] * 16;
            if (instOff < buf.length && buf[instOff] >= 2) {
                adlibins = true;
                break;
            }
        }
        if (!adlibins) {
            throw new IOException("no adlib instruments found");
        }

        header = checkhead;
        off = 96;
        for (int i = 0; i < 256; i++) {
            orders[i] = (byte) 0xff;
        }
        for (int i = 0; i < header.ordnum; i++) {
            orders[i] = buf[off++];
        }
        off += header.insnum * 2;
        int[] pattptr = new int[99];
        for (int i = 0; i < header.patnum; i++) {
            pattptr[i] = u16(buf, off);
            off += 2;
        }

        for (int i = 0; i < header.insnum; i++) {
            int instOff = insptr[i] * 16;
            inst[i] = new S3mInst();
            inst[i].type = buf[instOff++] & 0xff;
            inst[i].filename = readString(buf, instOff, 15); instOff += 15;
            inst[i].d00 = buf[instOff++] & 0xff; inst[i].d01 = buf[instOff++] & 0xff;
            inst[i].d02 = buf[instOff++] & 0xff; inst[i].d03 = buf[instOff++] & 0xff;
            inst[i].d04 = buf[instOff++] & 0xff; inst[i].d05 = buf[instOff++] & 0xff;
            inst[i].d06 = buf[instOff++] & 0xff; inst[i].d07 = buf[instOff++] & 0xff;
            inst[i].d08 = buf[instOff++] & 0xff; inst[i].d09 = buf[instOff++] & 0xff;
            inst[i].d0a = buf[instOff++] & 0xff; inst[i].d0b = buf[instOff++] & 0xff;
            inst[i].volume = buf[instOff++] & 0xff; inst[i].dsk = buf[instOff++] & 0xff;
            instOff += 2;
            inst[i].c2spd = u32(buf, instOff); instOff += 4;
            instOff += 12;
            inst[i].name = readString(buf, instOff, 28); instOff += 28;
            inst[i].scri = readString(buf, instOff, 4);
        }

        for (int i = 0; i < header.patnum; i++) {
            if (pattptr[i] == 0) continue;
            int pattOff = pattptr[i] * 16;
            int ppatlen = u16(buf, pattOff); pattOff += 2;
            loadPattern(i, buf, pattOff, ppatlen - 2);
        }

        rewind(0);
    }

    protected int loadPattern(int pat, byte[] buf, int offset, int length) {
        int pattOff = offset;
        for (int row = 0; (row < 64) && (pattOff < buf.length) && (pattOff - offset < length); row++) {
            int bufval;
            do {
                if (pattOff >= buf.length) break;
                bufval = buf[pattOff++] & 0xff;
                if (bufval == 0) break;
                int chan = bufval & 31;
                if ((bufval & 32) != 0) {
                    if (pattOff >= buf.length) break;
                    int bufval2 = buf[pattOff++] & 0xff;
                    pattern[pat][row][chan].note = bufval2 & 15;
                    pattern[pat][row][chan].oct = (bufval2 & 240) >> 4;
                    if (pattOff >= buf.length) break;
                    pattern[pat][row][chan].instrument = buf[pattOff++] & 0xff;
                }
                if ((bufval & 64) != 0) {
                    if (pattOff >= buf.length) break;
                    pattern[pat][row][chan].volume = buf[pattOff++] & 0xff;
                }
                if ((bufval & 128) != 0) {
                    if (pattOff >= buf.length) break;
                    pattern[pat][row][chan].command = buf[pattOff++] & 0xff;
                    if (pattOff >= buf.length) break;
                    pattern[pat][row][chan].info = buf[pattOff++] & 0xff;
                }
            } while (bufval != 0);
        }
        return pattOff - offset;
    }

    @Override
    public boolean update() {
        int pattbreak = 0, donote;
        int pattnr, chan, row, info;
        int realchan;

        for (realchan = 0; realchan < 9; realchan++) {
            info = channel[realchan].info;
            switch (channel[realchan].fx) {
                case 11:
                case 12:
                case 4:
                    if (channel[realchan].fx == 11) {
                        vibrato((byte) realchan, (byte) channel[realchan].dualinfo);
                    } else if (channel[realchan].fx == 12) {
                        tonePortamento((byte) realchan, (byte) channel[realchan].dualinfo);
                    }

                    if (info <= 0x0f) {
                        if (channel[realchan].vol - info >= 0) {
                            channel[realchan].vol -= info;
                        } else {
                            channel[realchan].vol = 0;
                        }
                    }
                    if ((info & 0x0f) == 0) {
                        if (channel[realchan].vol + (info >> 4) <= 63) {
                            channel[realchan].vol += (info >> 4);
                        } else {
                            channel[realchan].vol = 63;
                        }
                    }
                    setVolume((byte) realchan);
                    break;
                case 5:
                    if (info == 0xf0 || info <= 0xe0) {
                        slideDown((byte) realchan, (byte) info);
                        setFreq((byte) realchan);
                    }
                    break;
                case 6:
                    if (info == 0xf0 || info <= 0xe0) {
                        slideUp((byte) realchan, (byte) info);
                        setFreq((byte) realchan);
                    }
                    break;
                case 7:
                    tonePortamento((byte) realchan, (byte) channel[realchan].dualinfo);
                    break;
                case 8:
                    vibrato((byte) realchan, (byte) channel[realchan].dualinfo);
                    break;
                case 10:
                    channel[realchan].nextfreq = channel[realchan].freq;
                    channel[realchan].nextoct = channel[realchan].oct;
                    switch (channel[realchan].trigger) {
                        case 0: channel[realchan].freq = notetable[channel[realchan].note]; break;
                        case 1:
                            if (channel[realchan].note + ((info & 0xf0) >> 4) < 12) {
                                channel[realchan].freq = notetable[channel[realchan].note + ((info & 0xf0) >> 4)];
                            } else {
                                channel[realchan].freq = notetable[channel[realchan].note + ((info & 0xf0) >> 4) - 12];
                                channel[realchan].oct++;
                            }
                            break;
                        case 2:
                            if (channel[realchan].note + (info & 0x0f) < 12) {
                                channel[realchan].freq = notetable[channel[realchan].note + (info & 0x0f)];
                            } else {
                                channel[realchan].freq = notetable[channel[realchan].note + (info & 0x0f) - 12];
                                channel[realchan].oct++;
                            }
                            break;
                    }
                    if (channel[realchan].trigger < 2) {
                        channel[realchan].trigger++;
                    } else {
                        channel[realchan].trigger = 0;
                    }
                    setFreq((byte) realchan);
                    channel[realchan].freq = channel[realchan].nextfreq;
                    channel[realchan].oct = channel[realchan].nextoct;
                    break;
                case 21:
                    vibrato((byte) realchan, (byte) (info / 4));
                    break;
            }
        }

        if (del != 0) {
            del--;
            return songend == 0;
        }

        pattnr = orders[ord] & 0xff;
        if (pattnr == 0xff || ord > header.ordnum) {
            songend = 1;
            ord = 0;
            pattnr = orders[ord] & 0xff;
            if (pattnr == 0xff) {
                return songend == 0;
            }
        }
        if (pattnr == 0xfe) {
            ord++;
            pattnr = orders[ord] & 0xff;
        }

        row = (byte) crow;
        for (chan = 0; chan < 32; chan++) {
            if ((header.chanset[chan] & 128) == 0) {
                realchan = chnresolv[header.chanset[chan] & 127];
            } else {
                realchan = -1;
            }
            if (realchan != -1) {
                donote = 0;
                if (pattern[pattnr][row][chan].note < 14) {
                    if (pattern[pattnr][row][chan].command == 7 || pattern[pattnr][row][chan].command == 12) {
                        channel[realchan].nextfreq = notetable[pattern[pattnr][row][chan].note];
                        channel[realchan].nextoct = pattern[pattnr][row][chan].oct;
                    } else {
                        channel[realchan].note = pattern[pattnr][row][chan].note;
                        channel[realchan].freq = notetable[pattern[pattnr][row][chan].note];
                        channel[realchan].oct = pattern[pattnr][row][chan].oct;
                        channel[realchan].key = 1;
                        donote = 1;
                    }
                }
                if (pattern[pattnr][row][chan].note == 14) {
                    channel[realchan].key = 0;
                    setFreq((byte) realchan);
                }
                if ((channel[realchan].fx != 8 && channel[realchan].fx != 11) &&
                    (pattern[pattnr][row][chan].command == 8 || pattern[pattnr][row][chan].command == 11)) {
                    channel[realchan].nextfreq = channel[realchan].freq;
                    channel[realchan].nextoct = channel[realchan].oct;
                }
                if (pattern[pattnr][row][chan].note >= 14) {
                    if ((channel[realchan].fx == 8 || channel[realchan].fx == 11) &&
                        (pattern[pattnr][row][chan].command != 8 && pattern[pattnr][row][chan].command != 11)) {
                        channel[realchan].freq = channel[realchan].nextfreq;
                        channel[realchan].oct = channel[realchan].nextoct;
                        setFreq((byte) realchan);
                    }
                }
                if (pattern[pattnr][row][chan].instrument != 0) {
                    channel[realchan].inst = pattern[pattnr][row][chan].instrument - 1;
                    if (inst[channel[realchan].inst].volume < 64) {
                        channel[realchan].vol = inst[channel[realchan].inst].volume;
                    } else {
                        channel[realchan].vol = 63;
                    }
                    if (pattern[pattnr][row][chan].command != 7) {
                        donote = 1;
                    }
                }
                if (pattern[pattnr][row][chan].volume != 255) {
                    if (pattern[pattnr][row][chan].volume < 64) {
                        channel[realchan].vol = pattern[pattnr][row][chan].volume;
                    } else {
                        channel[realchan].vol = 63;
                    }
                }
                channel[realchan].fx = pattern[pattnr][row][chan].command;
                if (pattern[pattnr][row][chan].info != 0) {
                    channel[realchan].info = pattern[pattnr][row][chan].info;
                }

                switch (channel[realchan].fx) {
                    case 1:
                    case 2:
                    case 3:
                    case 20:
                        channel[realchan].info = pattern[pattnr][row][chan].info;
                        break;
                }

                if (donote != 0) {
                    playNote((byte) realchan);
                }
                if (pattern[pattnr][row][chan].volume != 255) {
                    setVolume((byte) realchan);
                }

                info = channel[realchan].info;
                switch (channel[realchan].fx) {
                    case 1: speed = info; break;
                    case 2: if (info <= ord) songend = 1; ord = info; crow = 0; pattbreak = 1; break;
                    case 3: if (pattbreak == 0) { crow = info; ord++; pattbreak = 1; } break;
                    case 4:
                        if (info > 0xf0) {
                            if (channel[realchan].vol - (info & 0x0f) >= 0) {
                                channel[realchan].vol -= (info & 0x0f);
                            } else {
                                channel[realchan].vol = 0;
                            }
                        }
                        if ((info & 0x0f) == 0x0f && info >= 0x1f) {
                            if (channel[realchan].vol + ((info & 0xf0) >> 4) <= 63) {
                                channel[realchan].vol += ((info & 0xf0) >> 4);
                            } else {
                                channel[realchan].vol = 63;
                            }
                        }
                        setVolume((byte) realchan);
                        break;
                    case 5:
                        if (info > 0xf0) {
                            slideDown((byte) realchan, (byte) (info & 0x0f));
                            setFreq((byte) realchan);
                        }
                        if (info > 0xe0 && info < 0xf0) {
                            slideDown((byte) realchan, (byte) ((info & 0x0f) / 4));
                            setFreq((byte) realchan);
                        }
                        break;
                    case 6:
                        if (info > 0xf0) {
                            slideUp((byte) realchan, (byte) (info & 0x0f));
                            setFreq((byte) realchan);
                        }
                        if (info > 0xe0 && info < 0xf0) {
                            slideUp((byte) realchan, (byte) ((info & 0x0f) / 4));
                            setFreq((byte) realchan);
                        }
                        break;
                    case 7:
                    case 8:
                        if ((channel[realchan].fx == 7 || channel[realchan].fx == 8) && pattern[pattnr][row][chan].info != 0) {
                            channel[realchan].dualinfo = info;
                        }
                        break;
                    case 10: channel[realchan].trigger = 0; break;
                    case 19:
                        if (info == 0xb0) {
                            loopstart = row;
                        }
                        if (info > 0xb0 && info <= 0xbf) {
                            if (loopcnt == 0) {
                                loopcnt = info & 0x0f;
                                crow = loopstart;
                                pattbreak = 1;
                            } else if (--loopcnt > 0) {
                                crow = loopstart;
                                pattbreak = 1;
                            }
                        }
                        if ((info & 0xf0) == 0xe0) {
                            del = speed * (info & 0x0f) - 1;
                        }
                        break;
                    case 20: tempo = info; break;
                }
            }
        }

        if (del == 0) {
            del = speed - 1;
        }
        if (pattbreak == 0) {
            crow++;
            if (crow > 63) {
                crow = 0;
                ord++;
                loopstart = 0;
            }
        }

        return songend == 0;
    }

    @Override
    public void rewind(int subsong) {
        songend = 0; ord = 0; crow = 0; tempo = header.it;
        speed = header.is; del = 0; loopstart = 0; loopcnt = 0;
        write(0, 1, 32);
    }

    private void setVolume(byte chan) {
        int op = op_table[chan];
        int insnr = channel[chan].inst;
        write(0, 0x43 + op, (int) (63 - ((63 - (inst[insnr].d03 & 63)) / 63.0) * channel[chan].vol) + (inst[insnr].d03 & 192));
        if ((inst[insnr].d0a & 1) != 0) {
            write(0, 0x40 + op, (int) (63 - ((63 - (inst[insnr].d02 & 63)) / 63.0) * channel[chan].vol) + (inst[insnr].d02 & 192));
        }
    }

    private void setFreq(byte chan) {
        write(0, 0xa0 + chan, channel[chan].freq & 255);
        if (channel[chan].key != 0) {
            write(0, 0xb0 + chan, (((channel[chan].freq & 768) >> 8) + (channel[chan].oct << 2)) | 32);
        } else {
            write(0, 0xb0 + chan, ((channel[chan].freq & 768) >> 8) + (channel[chan].oct << 2));
        }
    }

    private void playNote(byte chan) {
        int op = op_table[chan];
        int insnr = channel[chan].inst;
        write(0, 0xb0 + chan, 0);

        write(0, 0x20 + op, inst[insnr].d00);
        write(0, 0x23 + op, inst[insnr].d01);
        write(0, 0x40 + op, inst[insnr].d02);
        write(0, 0x43 + op, inst[insnr].d03);
        write(0, 0x60 + op, inst[insnr].d04);
        write(0, 0x63 + op, inst[insnr].d05);
        write(0, 0x80 + op, inst[insnr].d06);
        write(0, 0x83 + op, inst[insnr].d07);
        write(0, 0xe0 + op, inst[insnr].d08);
        write(0, 0xe3 + op, inst[insnr].d09);
        write(0, 0xc0 + chan, inst[insnr].d0a);

        channel[chan].key = 1;
        setFreq(chan);
    }

    private void slideDown(byte chan, byte amount) {
        if (channel[chan].freq - amount > 340) {
            channel[chan].freq -= amount;
        } else if (channel[chan].oct > 0) {
            channel[chan].oct--;
            channel[chan].freq = 684;
        } else {
            channel[chan].freq = 340;
        }
    }

    private void slideUp(byte chan, byte amount) {
        if (channel[chan].freq + amount < 686) {
            channel[chan].freq += amount;
        } else if (channel[chan].oct < 7) {
            channel[chan].oct++;
            channel[chan].freq = 341;
        } else {
            channel[chan].freq = 686;
        }
    }

    private void vibrato(byte chan, byte info) {
        int vspd = info >> 4;
        int depth = (info & 0x0f) / 2;

        for (int i = 0; i < vspd; i++) {
            channel[chan].trigger++;
            while (channel[chan].trigger >= 64) {
                channel[chan].trigger -= 64;
            }
            if (channel[chan].trigger >= 16 && channel[chan].trigger < 48) {
                slideDown(chan, (byte) (vibratotab[channel[chan].trigger - 16] / (16 - depth)));
            }
            if (channel[chan].trigger < 16) {
                slideUp(chan, (byte) (vibratotab[channel[chan].trigger + 16] / (16 - depth)));
            }
            if (channel[chan].trigger >= 48) {
                slideUp(chan, (byte) (vibratotab[channel[chan].trigger - 48] / (16 - depth)));
            }
        }
        setFreq(chan);
    }

    private void tonePortamento(byte chan, byte info) {
        if (channel[chan].freq + (channel[chan].oct << 10) < channel[chan].nextfreq + (channel[chan].nextoct << 10)) {
            slideUp(chan, info);
        }
        if (channel[chan].freq + (channel[chan].oct << 10) > channel[chan].nextfreq + (channel[chan].nextoct << 10)) {
            slideDown(chan, info);
        }
        setFreq(chan);
    }

    @Override
    public float getRefresh() {
        return tempo / 2.5f;
    }

    private static S3mHeader loadHeader(byte[] buf, int off) {
        S3mHeader h = new S3mHeader();
        h.name = readString(buf, off, 28);
        h.kennung = buf[off + 28] & 0xff;
        h.typ = buf[off + 29] & 0xff;
        h.ordnum = u16(buf, off + 32);
        h.insnum = u16(buf, off + 34);
        h.patnum = u16(buf, off + 36);
        h.flags = u16(buf, off + 38);
        h.cwtv = u16(buf, off + 40);
        h.ffi = u16(buf, off + 42);
        h.scrm = readString(buf, off + 44, 4);
        h.gv = buf[off + 48] & 0xff;
        h.is = buf[off + 49] & 0xff;
        h.it = buf[off + 50] & 0xff;
        h.mv = buf[off + 51] & 0xff;
        h.uc = buf[off + 52] & 0xff;
        h.dp = buf[off + 53] & 0xff;
        h.special = u16(buf, off + 62);
        System.arraycopy(buf, off + 64, h.chanset, 0, 32);
        return h;
    }

    protected static String readString(byte[] buf, int off, int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            if (off + i >= buf.length) break;
            char c = (char) buf[off + i];
            if (c == 0) break;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    protected static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    protected static long u32(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8) | ((b[off + 2] & 0xff) << 16) | ((b[off + 3] & 0xffL) << 24);
    }
}
