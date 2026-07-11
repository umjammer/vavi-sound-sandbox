/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2007 Simon Peter <dn.tlp@gmx.net>, et al.
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
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * JBM Adlib Music Player.
 * Ported from adplug's jbm.cpp / jbm.h by Dennis Lindroos.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class JbmPlayer extends Opl3Player {

    private static final int[] notetable = {
        0x0158, 0x016d, 0x0183, 0x019a, 0x01b2, 0x01cc, 0x01e7, 0x0204,
        0x0223, 0x0244, 0x0266, 0x028b, 0x0558, 0x056d, 0x0583, 0x059a,
        0x05b2, 0x05cc, 0x05e7, 0x0604, 0x0623, 0x0644, 0x0666, 0x068b,
        0x0958, 0x096d, 0x0983, 0x099a, 0x09b2, 0x09cc, 0x09e7, 0x0a04,
        0x0a23, 0x0a44, 0x0a66, 0x0a8b, 0x0d58, 0x0d6d, 0x0d83, 0x0d9a,
        0x0db2, 0x0dcc, 0x0de7, 0x0e04, 0x0e23, 0x0e44, 0x0e66, 0x0e8b,
        0x1158, 0x116d, 0x1183, 0x119a, 0x11b2, 0x11cc, 0x11e7, 0x1204,
        0x1223, 0x1244, 0x1266, 0x128b, 0x1558, 0x156d, 0x1583, 0x159a,
        0x15b2, 0x15cc, 0x15e7, 0x1604, 0x1623, 0x1644, 0x1666, 0x168b,
        0x1958, 0x196d, 0x1983, 0x199a, 0x19b2, 0x19cc, 0x19e7, 0x1a04,
        0x1a23, 0x1a44, 0x1a66, 0x1a8b, 0x1d58, 0x1d6d, 0x1d83, 0x1d9a,
        0x1db2, 0x1dcc, 0x1de7, 0x1e04, 0x1e23, 0x1e44, 0x1e66, 0x1e8b
    };

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private static final int[] percmx_tab = { 0x14, 0x12, 0x15, 0x11 };
    private static final int[] perchn_tab = { 6, 7, 8, 8, 7 };
    private static final int[] percmaskoff = { 0xef, 0xf7, 0xfb, 0xfd, 0xfe };
    private static final int[] percmaskon = { 0x10, 0x08, 0x04, 0x02, 0x01 };

    private static class JBMVoice {
        int trkpos, trkstart, seqpos;
        int seqno, note;
        int vol;
        int delay;
        int instr;
        final int[] frq = new int[2];
    }

    private byte[] m;
    private float timer;
    private int flags, voicemask;
    private int seqtable, seqcount;
    private int instable, inscount;
    private int[] sequences;
    private int bdreg;
    private final JBMVoice[] voice = new JBMVoice[11];

    public JbmPlayer() {
        super();
        for (int i = 0; i < 11; i++) {
            voice[i] = new JBMVoice();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("JBM Adlib Music", "jbm");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("JBM");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(16);
            return matchFormatImpl(bitStream);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    private static int u16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    protected boolean matchFormatImpl(InputStream is) throws IOException {
        byte[] magic = new byte[2];
        int read = is.read(magic);
        if (read < 2) return false;
        int firstVal = (magic[0] & 0xff) | ((magic[1] & 0xff) << 8);
        return firstVal == 0x0002;
    }

    @Override
    public void load(InputStream is) throws IOException {
        m = is.readAllBytes();
        int filelen = m.length;
        if (filelen < 10) {
            throw new IllegalArgumentException("file too short");
        }

        if (u16(m, 0) != 0x0002) {
            throw new IllegalArgumentException("invalid header magic");
        }

        int tempo = u16(m, 2);
        timer = (float) (1193810.0 / (tempo != 0 ? tempo : 0xffff));

        seqtable = u16(m, 4);
        instable = u16(m, 6);
        flags = u16(m, 8);

        inscount = (filelen - instable) >> 4;

        seqcount = 0xffff;
        for (int i = 0; i < 11; i++) {
            int trk = u16(m, 10 + (i << 1));
            voice[i].trkpos = voice[i].trkstart = trk;
            if (trk != 0 && trk < seqcount) {
                seqcount = trk;
            }
        }
        seqcount = (seqcount - seqtable) >> 1;
        sequences = new int[seqcount];
        for (int i = 0; i < seqcount; i++) {
            sequences[i] = u16(m, seqtable + (i << 1));
        }

        rewind(0);
    }

    @Override
    public boolean update() throws IOException {
        for (int c = 0; c < 11; c++) {
            if (voice[c].trkpos == 0) {
                continue;
            }

            voice[c].delay--;
            if (voice[c].delay > 0) {
                continue;
            }

            if ((voice[c].note & 0x7f) != 0) {
                opl_noteonoff(c, voice[c], false);
            }

            int spos = voice[c].seqpos;
            while (voice[c].delay == 0) {
                int cmd = m[spos] & 0xff;
                switch (cmd) {
                    case 0xFD -> {
                        voice[c].instr = m[spos + 1] & 0xff;
                        set_opl_instrument(c, voice[c]);
                        spos += 2;
                    }
                    case 0xFF -> {
                        voice[c].trkpos++;
                        voice[c].seqno = m[voice[c].trkpos] & 0xff;
                        if (voice[c].seqno == 0xff) {
                            voice[c].trkpos = voice[c].trkstart;
                            voice[c].seqno = m[voice[c].trkpos] & 0xff;
                            voicemask &= ~(1 << c);
                        }
                        spos = voice[c].seqpos = sequences[voice[c].seqno];
                    }
                    default -> {
                        if ((m[spos] & 127) > 95) {
                            return false;
                        }
                        voice[c].note = m[spos] & 0xff;
                        voice[c].vol = m[spos + 1] & 0xff;
                        voice[c].delay = ((m[spos + 2] & 0xff) | ((m[spos + 3] & 0xff) << 8)) + 1;

                        int frq = notetable[voice[c].note & 127];
                        voice[c].frq[0] = frq & 0xff;
                        voice[c].frq[1] = (frq >> 8) & 0xff;
                        spos += 4;
                    }
                }
            }
            voice[c].seqpos = spos;

            if ((flags & 1) != 0 && c > 6) {
                write(0, 0x40 + percmx_tab[c - 7], (voice[c].vol ^ 0x3f) & 0xff);
            } else if (c < 9) {
                write(0, 0x43 + op_table[c], (voice[c].vol ^ 0x3f) & 0xff);
            }

            opl_noteonoff(c, voice[c], (voice[c].note & 0x80) == 0);
        }
        return voicemask != 0;
    }

    @Override
    public void rewind(int subsong) throws IOException {
        voicemask = 0;

        for (int c = 0; c < 11; c++) {
            voice[c].trkpos = voice[c].trkstart;
            if (voice[c].trkpos == 0) continue;

            voicemask |= (1 << c);
            voice[c].seqno = m[voice[c].trkpos] & 0xff;
            voice[c].seqpos = sequences[voice[c].seqno];
            voice[c].note = 0;
            voice[c].delay = 1;
        }

        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32);

        bdreg = 0xC0 | ((flags & 1) << 5);
        write(0, 0xbd, bdreg);
    }

    private void opl_noteonoff(int channel, JBMVoice v, boolean state) {
        if ((flags & 1) != 0 && channel > 5) {
            write(0, 0xa0 + perchn_tab[channel - 6], v.frq[0]);
            write(0, 0xb0 + perchn_tab[channel - 6], v.frq[1]);
            write(0, 0xbd, state ? bdreg | percmaskon[channel - 6] : bdreg & percmaskoff[channel - 6]);
        } else {
            write(0, 0xa0 + channel, v.frq[0]);
            write(0, 0xb0 + channel, state ? v.frq[1] | 0x20 : v.frq[1] & 0x1f);
        }
    }

    private void set_opl_instrument(int channel, JBMVoice v) {
        int i = instable + (v.instr << 4);
        if (v.instr >= inscount) return;

        if ((flags & 1) != 0 && channel > 6) {
            write(0, 0x20 + percmx_tab[channel - 7], m[i] & 0xff);
            write(0, 0x40 + percmx_tab[channel - 7], (m[i + 1] ^ 0x3f) & 0xff);
            write(0, 0x60 + percmx_tab[channel - 7], m[i + 2] & 0xff);
            write(0, 0x80 + percmx_tab[channel - 7], m[i + 3] & 0xff);
            write(0, 0xc0 + perchn_tab[channel - 6], m[i + 8] & 15);
            return;
        }

        if (channel >= 9) return;

        write(0, 0x20 + op_table[channel], m[i] & 0xff);
        write(0, 0x40 + op_table[channel], (m[i + 1] ^ 0x3f) & 0xff);
        write(0, 0x60 + op_table[channel], m[i + 2] & 0xff);
        write(0, 0x80 + op_table[channel], m[i + 3] & 0xff);

        write(0, 0x23 + op_table[channel], m[i + 4] & 0xff);
        write(0, 0x43 + op_table[channel], (m[i + 5] ^ 0x3f) & 0xff);
        write(0, 0x63 + op_table[channel], m[i + 6] & 0xff);
        write(0, 0x83 + op_table[channel], m[i + 7] & 0xff);

        write(0, 0xe0 + op_table[channel], (m[i + 8] >> 4) & 3);
        write(0, 0xe3 + op_table[channel], (m[i + 8] >> 6) & 3);

        write(0, 0xc0 + channel, m[i + 8] & 15);
    }

    @Override
    public float getRefresh() {
        return timer;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }
}
