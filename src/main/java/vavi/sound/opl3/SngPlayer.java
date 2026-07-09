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

import vavi.io.LittleEndianDataInputStream;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * SNG Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class SngPlayer extends Opl3Player {

    private static final Logger logger = getLogger(SngPlayer.class.getName());

    private static final String ID = "ObsM";

    /** length, start and loop counted in reg/val pairs */
    private int length, start, loop;
    private int delay;
    private boolean compressed;
    /** register/value stream ([n][0]: val, [n][1]: reg) */
    private int[][] data;

    private int del;
    private int pos;
    private boolean songend;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("SNG File Format", "sng");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("SNG");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bitStream);
        try {
            dis.mark(4);

            byte[] id = new byte[4];
            dis.readFully(id);
            return ID.equals(new String(id));
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        } finally {
            try {
                dis.reset();
            } catch (IOException e) {
                logger.log(Level.DEBUG, e.toString());
            }
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();

        // load header
        length = u16(buf, 4);
        start = u16(buf, 6);
        loop = u16(buf, 8);
        delay = buf[10] & 0xff;
        compressed = (buf[11] & 0xff) != 0;

        // load section (the declared length may run slightly past EOF; pad with 0)
        length /= 2; start /= 2; loop /= 2;
        data = new int[length][2];
        int off = 12;
        for (int i = 0; i < length; i++) {
            data[i][0] = off < buf.length ? buf[off] & 0xff : 0; off++; // val
            data[i][1] = off < buf.length ? buf[off] & 0xff : 0; off++; // reg
        }

logger.log(Level.DEBUG, "length: " + length + ", start: " + start + ", loop: " + loop + ", compressed: " + compressed);

        rewind(0);
    }

    @Override
    public boolean update() {
        if (compressed && del != 0) {
            del--;
            return !songend;
        }

        while (data[pos][1] != 0) {
            write(0, data[pos][1], data[pos][0]);
            pos++;
            if (pos >= length) {
                songend = true;
                pos = loop;
            }
        }

        if (!compressed) {
            write(0, data[pos][1], data[pos][0]);
        }

        if (data[pos][0] != 0) {
            del = data[pos][0] - 1;
        }
        pos++;

        if (pos >= length) {
            songend = true;
            pos = loop;
        }
        return !songend;
    }

    @Override
    public void rewind(int subSong) {
        pos = start; del = delay; songend = false;
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32); // go to OPL2 mode
    }

    @Override
    public float getRefresh() {
        return 70.0f;
    }

    /** little-endian unsigned 16-bit */
    private static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }
}
