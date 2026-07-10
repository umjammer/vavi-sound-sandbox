/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2005 Simon Peter, <dn.tlp@gmx.net>, et al.
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
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Raw AdLib Capture (RdosPlay RAW) Player.
 * Ported from adplug's raw.cpp / raw.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class RawPlayer extends Opl3Player {

    private static final byte[] ID = { 'R', 'A', 'W', 'A', 'D', 'A', 'T', 'A' };

    /** register/value pairs ([n][0]: param, [n][1]: command) */
    private int[][] data;
    private int pos;
    private int length;
    private int clock;
    private int speed;
    private int del;
    private boolean songend;
    /** selected OPL chip (RAW command 2 supports dual OPL2) */
    private int curChip;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Raw AdLib Capture", "raw");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("RAW");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(8);
            byte[] id = new byte[8];
            if (bitStream.read(id) < 8) return false;
            return java.util.Arrays.equals(id, ID);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length <= 10) {
            throw new IllegalArgumentException("file too short");
        }
        for (int i = 0; i < 8; i++) {
            if (buf[i] != ID[i]) {
                throw new IllegalArgumentException("invalid signature");
            }
        }

        clock = (buf[8] & 0xff) | ((buf[9] & 0xff) << 8);
        length = (buf.length - 10) / 2;
        data = new int[length][2];

        // copy pairs, stopping at the RAW EOF marker followed by a tag block
        int off = 10;
        boolean tagdata = false;
        for (int i = 0; i < length; i++) {
            data[i][0] = tagdata ? 0xFF : (off < buf.length ? buf[off] & 0xff : 0); off++;
            data[i][1] = tagdata ? 0xFF : (off < buf.length ? buf[off] & 0xff : 0); off++;

            if (!tagdata && data[i][0] == 0xFF && data[i][1] == 0xFF) {
                int tagCode = off < buf.length ? buf[off] & 0xff : -1;
                off++;
                if (tagCode == 0x1A) {
                    // tag marker found
                    tagdata = true;
                } else if (tagCode == 0) {
                    // old comment (music archive 2004): skip to NUL, max 1023
                    int n = 0;
                    while (n < 1022 && off < buf.length && buf[off] != 0) {
                        off++;
                        n++;
                    }
                    if (off < buf.length) off++; // consume NUL
                } else {
                    // this is not a tag marker, revert
                    off--;
                }
            }
        }
        // (the tag strings themselves are not needed for playback)

        rewind(0);
    }

    @Override
    public boolean update() {
        boolean setspeed;

        if (pos >= length) return false;

        if (del != 0) {
            del--;
            return !songend;
        }

        do {
            setspeed = false;
            if (pos >= length) return false;

            switch (data[pos][1]) {
            case 0:
                del = (data[pos][0] - 1) & 0xff; // del is u8 in adplug, param 0 wraps to 255
                break;

            case 2:
                if (data[pos][0] == 0) {
                    pos++;
                    if (pos >= length) return false;
                    speed = data[pos][0] + (data[pos][1] << 8);
                    setspeed = true;
                } else {
                    curChip = (data[pos][0] - 1) & 1;
                }
                break;

            case 0xff:
                if (data[pos][0] == 0xff) {
                    rewind(0); // auto-rewind song
                    songend = true;
                    return !songend;
                }
                break;

            default:
                write(curChip, data[pos][1], data[pos][0]);
                break;
            }
        } while (data[pos++][1] != 0 || setspeed);

        return !songend;
    }

    @Override
    public void rewind(int subSong) {
        pos = 0;
        del = 0;
        speed = clock;
        songend = false;
        curChip = 0;
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32); // go to 9 channel mode
    }

    @Override
    public float getRefresh() {
        // timer oscillator speed / wait register = clock frequency
        return 1193180.0f / (speed != 0 ? speed : 0xffff);
    }
}
