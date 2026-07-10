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
import java.net.URI;
import java.util.zip.CRC32;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.SoundUtil;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * God of Thunder music player.
 * Ported from adplug's got.cpp / got.h by Stas'M (based on the IMF player).
 *
 * The format is a stream of 3-byte records: time(1), reg(1), val(1),
 * after a 2-byte header (always 1).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class GotPlayer extends Opl3Player {

    /** zlib crc32 of the menu music (adplug database key) */
    private static final long MENU_MUSIC_CRC32 = 0x72036C41L;

    /** records: [n][0]=time, [n][1]=reg, [n][2]=val */
    private int[][] data;
    private int size;
    private int pos;
    private int del;
    private float timer;
    private float rate;
    private boolean songend;

    public GotPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("God of Thunder music", "got");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("GOT");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            // GOT has no signature; adplug validates by file extension plus
            // structural checks. Gate on the extension when the URI is known.
            URI uri = SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && !path.toLowerCase().endsWith(".got")) {
                    return false;
                }
            }
            bitStream.mark(Integer.MAX_VALUE);
            byte[] buf = bitStream.readAllBytes();
            return validate(buf);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    private static boolean validate(byte[] buf) {
        if (buf.length % 3 != 0 || buf.length < 9) return false;
        if (((buf[0] & 0xff) | ((buf[1] & 0xff) << 8)) != 1) return false;
        // last 4 bytes must be zero
        for (int i = buf.length - 4; i < buf.length; i++) {
            if (buf[i] != 0) return false;
        }
        return true;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (!validate(buf)) {
            throw new IllegalArgumentException("not a GOT file");
        }

        // load section
        size = buf.length / 3 - 1;
        data = new int[size][3];
        int off = 2;
        for (int i = 0; i < size; i++) {
            data[i][0] = buf[off++] & 0xff; // time
            data[i][1] = buf[off++] & 0xff; // reg
            data[i][2] = buf[off++] & 0xff; // val
        }

        // 140Hz for the menu music, else 120Hz (adplug uses a database key:
        // the standard zlib crc32 of the whole file plus one 0xff byte, an
        // artifact of its binistream reading (Byte)EOF before noticing eof)
        CRC32 crc = new CRC32();
        crc.update(buf);
        crc.update(0xff);
        rate = crc.getValue() == MENU_MUSIC_CRC32 ? 140.0f : 120.0f;

        rewind(0);
    }

    @Override
    public boolean update() throws IOException {
        do {
            del = data[pos][0];
            write(0, data[pos][1], data[pos][2]);
            pos++;
        } while (del == 0 && pos < size);

        if (pos >= size) {
            pos = 0;
            songend = true;
        } else {
            timer = rate / (float) del;
        }

        return !songend;
    }

    @Override
    public void rewind(int subsong) throws IOException {
        pos = 0;
        del = 0;
        timer = rate;
        songend = false;
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32); // go to OPL2 mode
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
