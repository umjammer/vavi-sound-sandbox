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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * Ken Silverman's Music Format Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class KsmPlayer extends Opl3Player {

    private static final Logger logger = getLogger(KsmPlayer.class.getName());

    private static final int[] adlibfreq = {
        0,
        2390, 2411, 2434, 2456, 2480, 2506, 2533, 2562, 2592, 2625, 2659, 2695,
        3414, 3435, 3458, 3480, 3504, 3530, 3557, 3586, 3616, 3649, 3683, 3719,
        4438, 4459, 4482, 4504, 4528, 4554, 4581, 4610, 4640, 4673, 4707, 4743,
        5462, 5483, 5506, 5528, 5552, 5578, 5605, 5634, 5664, 5697, 5731, 5767,
        6486, 6507, 6530, 6552, 6576, 6602, 6629, 6658, 6688, 6721, 6755, 6791,
        7510
    };

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private long count, countstop;
    private final long[] chanage = new long[18];
    private int[] note;
    private int numnotes;
    private int nownote, numchans, drumstat;
    private final byte[] trinst = new byte[16], trquant = new byte[16], trchan = new byte[16], trvol = new byte[16];
    private final byte[][] inst = new byte[256][11];
    private final byte[] databuf = new byte[2048], chanfreq = new byte[18], chantrack = new byte[18];
    private final String[] instname = new String[256];

    private boolean songend;

    public KsmPlayer() {
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("KSM File Format", "ksm");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("KSM");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".ksm")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        InputStream instsStream = null;
        if (props.containsKey("uri")) {
            try {
                URI uri = (URI) props.get("uri");
                Path ksmPath = Paths.get(uri);
                Path instsPath = ksmPath.getParent().resolve("insts.dat");
                if (Files.exists(instsPath)) {
                    instsStream = Files.newInputStream(instsPath);
                }
            } catch (Exception ignored) {
            }
        }
        if (instsStream == null) {
            instsStream = KsmPlayer.class.getResourceAsStream("/opl3/insts.dat");
        }
        if (instsStream == null) {
            instsStream = KsmPlayer.class.getResourceAsStream("insts.dat");
        }
        if (instsStream == null) {
            instsStream = KsmPlayer.class.getResourceAsStream("/insts.dat");
        }
        if (instsStream == null) {
            throw new IOException("insts.dat not found");
        }

        loadInsts(instsStream);

        byte[] buf = is.readAllBytes();
        int off = 0;
        for (int i = 0; i < 16; i++) trinst[i] = buf[off++];
        for (int i = 0; i < 16; i++) trquant[i] = buf[off++];
        for (int i = 0; i < 16; i++) trchan[i] = buf[off++];
        off += 16; // Skip 16 bytes
        for (int i = 0; i < 16; i++) trvol[i] = buf[off++];
        numnotes = (buf[off] & 0xff) | ((buf[off + 1] & 0xff) << 8);
        off += 2;
        note = new int[numnotes];
        for (int i = 0; i < numnotes; i++) {
            note[i] = (buf[off] & 0xff) | ((buf[off + 1] & 0xff) << 8) | ((buf[off + 2] & 0xff) << 16) | ((buf[off + 3] & 0xff) << 24);
            off += 4;
        }

        if (trchan[11] == 0) {
            drumstat = 0;
            numchans = 9;
        } else {
            drumstat = 32;
            numchans = 6;
        }

        rewind(0);
    }

    @Override
    public boolean update() {
        int quanter, chan = 0, drumnum = 0, freq, track, volevel, volval;
        int i, j, bufnum;
        long temp, templong;

        count++;
        if (count >= countstop) {
            bufnum = 0;
            while (count >= countstop) {
                templong = note[nownote];
                track = (int) ((templong >> 8) & 15);
                if ((templong & 192) == 0) {
                    i = 0;
                    while ((i < numchans) &&
                           ((chanfreq[i] != (templong & 63)) ||
                            (chantrack[i] != ((templong >> 8) & 15))))
                        i++;
                    if (i < numchans) {
                        databuf[bufnum] = 0; bufnum++;
                        databuf[bufnum] = (byte) (0xb0 + i); bufnum++;
                        databuf[bufnum] = (byte) ((adlibfreq[(int) (templong & 63)] >> 8) & 223); bufnum++;
                        chanfreq[i] = 0;
                        chanage[i] = 0;
                    }
                } else {
                    volevel = trvol[track] & 0xff;
                    if ((templong & 192) == 128) {
                        volevel -= 4;
                        if (volevel < 0)
                            volevel = 0;
                    }
                    if ((templong & 192) == 192) {
                        volevel += 4;
                        if (volevel > 63)
                            volevel = 63;
                    }
                    if (track < 11) {
                        temp = 0;
                        i = numchans;
                        for (j = 0; j < numchans; j++)
                            if ((countstop - chanage[j] >= temp) && (chantrack[j] == track)) {
                                temp = countstop - chanage[j];
                                i = j;
                            }
                        if (i < numchans) {
                            databuf[bufnum] = (byte) 0; bufnum++;
                            databuf[bufnum] = (byte) (0xb0 + i); bufnum++;
                            databuf[bufnum] = (byte) 0; bufnum++;
                            volval = (inst[trinst[track] & 0xff][1] & 192) + (volevel ^ 63);
                            databuf[bufnum] = (byte) 0; bufnum++;
                            databuf[bufnum] = (byte) (0x40 + op_table[i] + 3); bufnum++;
                            databuf[bufnum] = (byte) volval; bufnum++;
                            databuf[bufnum] = (byte) 0; bufnum++;
                            databuf[bufnum] = (byte) (0xa0 + i); bufnum++;
                            databuf[bufnum] = (byte) (adlibfreq[(int) (templong & 63)] & 255); bufnum++;
                            databuf[bufnum] = (byte) 0; bufnum++;
                            databuf[bufnum] = (byte) (0xb0 + i); bufnum++;
                            databuf[bufnum] = (byte) ((adlibfreq[(int) (templong & 63)] >> 8) | 32); bufnum++;
                            chanfreq[i] = (byte) (templong & 63);
                            chanage[i] = countstop;
                        }
                    } else if ((drumstat & 32) > 0) {
                        freq = (int) adlibfreq[(int) (templong & 63)];
                        switch (track) {
                            case 11: drumnum = 16; chan = 6; freq -= 2048; break;
                            case 12: drumnum = 8; chan = 7; freq -= 2048; break;
                            case 13: drumnum = 4; chan = 8; break;
                            case 14: drumnum = 2; chan = 8; break;
                            case 15: drumnum = 1; chan = 7; freq -= 2048; break;
                        }
                        databuf[bufnum] = (byte) 0; bufnum++;
                        databuf[bufnum] = (byte) (0xa0 + chan); bufnum++;
                        databuf[bufnum] = (byte) (freq & 255); bufnum++;
                        databuf[bufnum] = (byte) 0; bufnum++;
                        databuf[bufnum] = (byte) (0xb0 + chan); bufnum++;
                        databuf[bufnum] = (byte) ((freq >> 8) & 223); bufnum++;
                        databuf[bufnum] = (byte) 0; bufnum++;
                        databuf[bufnum] = (byte) (0xbd); bufnum++;
                        databuf[bufnum] = (byte) (drumstat & (255 - drumnum)); bufnum++;
                        drumstat |= drumnum;
                        if ((track == 11) || (track == 12) || (track == 14)) {
                            volval = (inst[trinst[track] & 0xff][1] & 192) + (volevel ^ 63);
                            databuf[bufnum] = (byte) 0; bufnum++;
                            databuf[bufnum] = (byte) (0x40 + op_table[chan] + 3); bufnum++;
                            databuf[bufnum] = (byte) (volval); bufnum++;
                        } else {
                            volval = (inst[trinst[track] & 0xff][6] & 192) + (volevel ^ 63);
                            databuf[bufnum] = (byte) 0; bufnum++;
                            databuf[bufnum] = (byte) (0x40 + op_table[chan]); bufnum++;
                            databuf[bufnum] = (byte) (volval); bufnum++;
                        }
                        databuf[bufnum] = (byte) 0; bufnum++;
                        databuf[bufnum] = (byte) (0xbd); bufnum++;
                        databuf[bufnum] = (byte) (drumstat); bufnum++;
                    }
                }
                nownote++;
                if (nownote >= numnotes) {
                    nownote = 0;
                    songend = true;
                }
                templong = note[nownote];
                if (nownote == 0)
                    count = (templong >> 12) - 1;
                int quantVal = trquant[(int) ((templong >> 8) & 15)] & 0xff;
                quanter = quantVal == 0 ? 240 : (240 / quantVal);
                countstop = (((templong >> 12) + (quanter >> 1)) / quanter) * quanter;
            }
            for (i = 0; i < bufnum; i += 3)
                write(0, databuf[i + 1] & 0xff, databuf[i + 2] & 0xff);
        }
        return !songend;
    }

    @Override
    public void rewind(int subsong) {
        int i, j, k;
        byte[] instbuf = new byte[11];
        int templong;

        songend = false;
        write(0, 1, 32);
        write(0, 4, 0);
        write(0, 8, 0);
        write(0, 0xbd, drumstat);

        if (trchan[11] == 1) {
            for (i = 0; i < 11; i++)
                instbuf[i] = inst[trinst[11] & 0xff][i];
            instbuf[1] = (byte) (((instbuf[1] & 192) | (trvol[11] & 0xff) ^ 63));
            setinst(6, instbuf[0], instbuf[1], instbuf[2], instbuf[3], instbuf[4], instbuf[5], instbuf[6], instbuf[7], instbuf[8], instbuf[9], instbuf[10]);
            for (i = 0; i < 5; i++)
                instbuf[i] = inst[trinst[12] & 0xff][i];
            for (i = 5; i < 11; i++)
                instbuf[i] = inst[trinst[15] & 0xff][i];
            instbuf[1] = (byte) (((instbuf[1] & 192) | (trvol[12] & 0xff) ^ 63));
            instbuf[6] = (byte) (((instbuf[6] & 192) | (trvol[15] & 0xff) ^ 63));
            setinst(7, instbuf[0], instbuf[1], instbuf[2], instbuf[3], instbuf[4], instbuf[5], instbuf[6], instbuf[7], instbuf[8], instbuf[9], instbuf[10]);
            for (i = 0; i < 5; i++)
                instbuf[i] = inst[trinst[14] & 0xff][i];
            for (i = 5; i < 11; i++)
                instbuf[i] = inst[trinst[13] & 0xff][i];
            instbuf[1] = (byte) (((instbuf[1] & 192) | (trvol[14] & 0xff) ^ 63));
            instbuf[6] = (byte) (((instbuf[6] & 192) | (trvol[13] & 0xff) ^ 63));
            setinst(8, instbuf[0], instbuf[1], instbuf[2], instbuf[3], instbuf[4], instbuf[5], instbuf[6], instbuf[7], instbuf[8], instbuf[9], instbuf[10]);
        }

        for (i = 0; i < numchans; i++) {
            chantrack[i] = 0;
            chanage[i] = 0;
        }
        j = 0;
        for (i = 0; i < 16; i++)
            if ((trchan[i] > 0) && (j < numchans)) {
                k = trchan[i];
                while ((j < numchans) && (k > 0)) {
                    chantrack[j] = (byte) i;
                    k--;
                    j++;
                }
            }
        for (i = 0; i < numchans; i++) {
            for (j = 0; j < 11; j++)
                instbuf[j] = inst[trinst[chantrack[i] & 0xff] & 0xff][j];
            instbuf[1] = (byte) ((instbuf[1] & 192) | (63 - (trvol[chantrack[i] & 0xff] & 0xff)));
            setinst(i, instbuf[0], instbuf[1], instbuf[2], instbuf[3], instbuf[4], instbuf[5], instbuf[6], instbuf[7], instbuf[8], instbuf[9], instbuf[10]);
            chanfreq[i] = 0;
        }
        k = 0;
        templong = note[0];
        count = (templong >> 12) - 1;
        countstop = (templong >> 12) - 1;
        nownote = 0;
    }

    private void setinst(int chan,
                 byte v0, byte v1, byte v2,
                 byte v3, byte v4, byte v5,
                 byte v6, byte v7, byte v8,
                 byte v9, byte v10) {
        write(0, 0xa0 + chan, 0);
        write(0, 0xb0 + chan, 0);
        write(0, 0xc0 + chan, v10 & 0xff);
        int offs = op_table[chan];
        write(0, 0x20 + offs, v5 & 0xff);
        write(0, 0x40 + offs, v6 & 0xff);
        write(0, 0x60 + offs, v7 & 0xff);
        write(0, 0x80 + offs, v8 & 0xff);
        write(0, 0xe0 + offs, v9 & 0xff);
        offs += 3;
        write(0, 0x20 + offs, v0 & 0xff);
        write(0, 0x40 + offs, v1 & 0xff);
        write(0, 0x60 + offs, v2 & 0xff);
        write(0, 0x80 + offs, v3 & 0xff);
        write(0, 0xe0 + offs, v4 & 0xff);
    }

    private void loadInsts(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        int off = 0;
        for (int i = 0; i < 256; i++) {
            instname[i] = new String(buf, off, 20).trim();
            off += 20;
            for (int j = 0; j < 11; j++) {
                inst[i][j] = buf[off];
                off++;
            }
            off += 2;
        }
    }

    @Override
    public float getRefresh() {
        return 240.0f;
    }
}
