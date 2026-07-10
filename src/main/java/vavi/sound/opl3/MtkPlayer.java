/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2006 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * MPU-401 Trakker Loader.
 * Ported from adplug's mtk.cpp / mtk.h by Simon Peter.
 *
 * MTK is an LZ-compressed container around HSC replay data; this loader
 * decompresses it and reuses the HSC replayer (with the MPU-401 Trakker
 * note-off-by-one quirk enabled).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class MtkPlayer extends HscPlayer {

    private static final Logger logger = getLogger(MtkPlayer.class.getName());

    /** "mpu401tr\x92kk\xeer@data" */
    private static final int[] ID = {
        'm', 'p', 'u', '4', '0', '1', 't', 'r', 0x92, 'k', 'k', 0xee, 'r', '@', 'd', 'a', 't', 'a'
    };

    /** decompressed data layout: songname(34) + composername(34) + instname(128*34) + insts(128*12) + order(128) + dummy(1) */
    private static final int DATA_SIZE = 34 + 34 + 128 * 34 + 128 * 12 + 128 + 1;

    public MtkPlayer() {
        super();
        mtkmode = 1;
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("MPU-401 Trakker", "mtk");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MTK");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(22);
            byte[] hdr = new byte[22];
            if (bitStream.read(hdr) < 22) return false;
            for (int i = 0; i < ID.length; i++) {
                if ((hdr[i] & 0xff) != ID[i]) return false;
            }
            int size = (hdr[20] & 0xff) | ((hdr[21] & 0xff) << 8);
            return size >= DATA_SIZE;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 22) {
            throw new IllegalArgumentException("file too short");
        }

        // read header: id(18), crc(2), size(2)
        for (int i = 0; i < ID.length; i++) {
            if ((buf[i] & 0xff) != ID[i]) {
                throw new IllegalArgumentException("invalid signature");
            }
        }
        int size = (buf[20] & 0xff) | ((buf[21] & 0xff) << 8);
        if (size < DATA_SIZE) {
            throw new IllegalArgumentException("declared size too small");
        }

        // load & decompress section
        byte[] org = new byte[size];
        int pos = 22;
        int ctrlbits = 0, ctrlmask = 0;
        int orgptr = 0;
        while (orgptr < size) {
            if (pos >= buf.length) {
                throw new IllegalArgumentException("truncated compressed stream");
            }

            ctrlmask >>= 1;
            if (ctrlmask == 0) {
                if (pos + 2 > buf.length) {
                    throw new IllegalArgumentException("truncated compressed stream");
                }
                ctrlbits = (buf[pos] & 0xff) | ((buf[pos + 1] & 0xff) << 8);
                pos += 2;
                ctrlmask = 0x8000;
            }

            if ((ctrlbits & ctrlmask) == 0) { // uncompressed data
                org[orgptr++] = buf[pos++];
                continue;
            }

            // compressed data
            int cmd = buf[pos++] & 0xff;
            int cnt = (cmd & 0x0f) + 3;
            int offs;

            switch (cmd >> 4) {
            case 0: // repeat a byte 3..18 times
                repeatByte(org, orgptr, size, buf[pos++], cnt);
                break;

            case 1: // repeat a byte 19..4114 times
                cnt += ((buf[pos++] & 0xff) << 4) + 16;
                repeatByte(org, orgptr, size, buf[pos++], cnt);
                break;

            case 2: // copy range (16..271 bytes)
                offs = cnt + ((buf[pos++] & 0xff) << 4);
                cnt = (buf[pos++] & 0xff) + 16;
                copyRange(org, orgptr, size, offs, cnt);
                break;

            default: // copy range (3..15 bytes)
                offs = cnt + ((buf[pos++] & 0xff) << 4);
                cnt = cmd >> 4;
                copyRange(org, orgptr, size, offs, cnt);
                break;
            }
            orgptr += cnt;
        }

        // convert to HSC replay data (offsets into the mtkdata struct)
        int instsOff = 34 + 34 + 128 * 34;
        int orderOff = instsOff + 128 * 12;
        int patternsOff = DATA_SIZE;

        for (int i = 0; i < 128; i++) { // load instruments
            for (int j = 0; j < 12; j++) {
                instr[i][j] = org[instsOff + i * 12 + j] & 0xff;
            }
            // correct instruments
            instr[i][2] ^= (instr[i][2] & 0x40) << 1;
            instr[i][3] ^= (instr[i][3] & 0x40) << 1;
            instr[i][11] >>= 4; // make unsigned
        }

        for (int i = 0; i < 0x80; i++) { // load tracklist
            song[i] = org[orderOff + i] & 0xff;
        }

        // load patterns
        int cnt = Math.min(size - DATA_SIZE, 50 * 64 * 9 * 2);
        for (int i = 0; i < cnt; i++) {
            int idx = i >> 1; // hscnote index
            patterns[idx / (64 * 9)][idx % (64 * 9)][i & 1] = org[patternsOff + i] & 0xff;
        }

logger.log(Level.DEBUG, "size: " + size + ", pattern bytes: " + cnt);

        rewind(0);
    }

    private static void repeatByte(byte[] org, int orgptr, int size, byte val, int cnt) {
        if (orgptr + cnt > size) {
            throw new IllegalArgumentException("decompression overflow");
        }
        java.util.Arrays.fill(org, orgptr, orgptr + cnt, val);
    }

    private static void copyRange(byte[] org, int orgptr, int size, int offs, int cnt) {
        if (orgptr + cnt > size || offs > orgptr) {
            throw new IllegalArgumentException("decompression overflow");
        }
        // may overlap, so copy bytewise
        for (int i = 0; i < cnt; i++) {
            org[orgptr + i] = org[orgptr - offs + i];
        }
    }
}
