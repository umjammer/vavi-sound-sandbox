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
 * eXtra Simple Music Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class XsmPlayer extends Opl3Player {

    private static final Logger logger = getLogger(XsmPlayer.class.getName());

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private static final int[] note_table = {
        363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647, 686
    };

    private static class Instrument {
        byte[] value;
    }

    private int songlen;
    private byte[] music;
    private int last, notenum;
    private boolean songend;
    private Instrument[] inst;

    public XsmPlayer() {
        inst = new Instrument[9];
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("eXtra Simple Music Format", "xsm");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("XSM");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".xsm")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            bitStream.mark(8);
            byte[] header = new byte[8];
            int r = bitStream.read(header);
            if (r == 8) {
                String id = new String(header, 0, 6);
                int len = (header[6] & 0xff) | ((header[7] & 0xff) << 8);
                if (id.equals("ofTAZ!") && len <= 3200) {
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
        int off = 0;
        String id = new String(buf, off, 6);
        off += 6;
        songlen = (buf[off] & 0xff) | ((buf[off + 1] & 0xff) << 8);
        off += 2;
        if (!id.equals("ofTAZ!") || songlen > 3200) {
            throw new IOException("invalid XSM file");
        }

        inst = new Instrument[9];
        for (int i = 0; i < 9; i++) {
            inst[i] = new Instrument();
            inst[i].value = new byte[11];
            System.arraycopy(buf, off, inst[i].value, 0, 11);
            off += 11;
            off += 5;
        }

        music = new byte[songlen * 9];
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < songlen; j++) {
                music[j * 9 + i] = buf[off++];
            }
        }

        rewind(0);
    }

    @Override
    public boolean update() {
        if (notenum >= songlen) {
            songend = true;
            notenum = last = 0;
            return false;
        }

        for (int c = 0; c < 9; c++) {
            if (music[notenum * 9 + c] != music[last * 9 + c]) {
                write(0, 0xb0 + c, 0);
            }
        }

        for (int c = 0; c < 9; c++) {
            int noteVal = music[notenum * 9 + c] & 0xff;
            if (noteVal != 0) {
                playNote(c, noteVal % 12, noteVal / 12);
            } else {
                playNote(c, 0, 0);
            }
        }

        last = notenum;
        notenum++;
        return !songend;
    }

    @Override
    public void rewind(int subsong) {
        notenum = last = 0;
        songend = false;
        write(0, 1, 32);
        for (int i = 0; i < 9; i++) {
            write(0, 0x20 + op_table[i], inst[i].value[0] & 0xff);
            write(0, 0x23 + op_table[i], inst[i].value[1] & 0xff);
            write(0, 0x40 + op_table[i], inst[i].value[2] & 0xff);
            write(0, 0x43 + op_table[i], inst[i].value[3] & 0xff);
            write(0, 0x60 + op_table[i], inst[i].value[4] & 0xff);
            write(0, 0x63 + op_table[i], inst[i].value[5] & 0xff);
            write(0, 0x80 + op_table[i], inst[i].value[6] & 0xff);
            write(0, 0x83 + op_table[i], inst[i].value[7] & 0xff);
            write(0, 0xe0 + op_table[i], inst[i].value[8] & 0xff);
            write(0, 0xe3 + op_table[i], inst[i].value[9] & 0xff);
            write(0, 0xc0 + op_table[i], inst[i].value[10] & 0xff);
        }
    }

    @Override
    public float getRefresh() {
        return 5.0f;
    }

    private void playNote(int c, int note, int octv) {
        int freq = note_table[note];
        if (note == 0 && octv == 0) {
            freq = 0;
        }
        write(0, 0xa0 + c, freq & 0xff);
        write(0, 0xb0 + c, (freq / 255) | 32 | (octv * 4));
    }
}
