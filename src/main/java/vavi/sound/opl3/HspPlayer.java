/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2004 Simon Peter, <dn.tlp@gmx.net>, et al.
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
import java.util.Arrays;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * HSP (HSC Packed) Loader.
 * Ported from adplug's hsp.cpp / hsp.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class HspPlayer extends HscPlayer {

    public HspPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("HSC Packed", "hsp");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("HSP");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(256);
            return matchFormatImpl(bitStream);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    protected boolean matchFormatImpl(InputStream is) throws IOException {
        byte[] buf = new byte[256]; // read enough for RLE validation
        int read = is.read(buf);
        if (read < 4) return false;
        int orgsize = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
        // Decompressed size must be at least 128*12 + 51 = 1587 bytes
        if (orgsize < 128 * 12 + 51 || orgsize > 59187) return false;
        // Validate RLE structure: sum of count bytes should approach orgsize
        int sum = 0;
        for (int i = 2; i < read; i += 2) {
            sum += buf[i] & 0xff;
        }
        // RLE count sum must be large enough to produce valid HSC data
        return sum > 0 && sum <= orgsize;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 2) {
            throw new IllegalArgumentException("file too short");
        }

        int orgsize = (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
        if (orgsize > 59187) {
            throw new IllegalArgumentException("invalid decompressed size");
        }

        byte[] org = new byte[orgsize];
        int j = 0;
        for (int i = 2; i < buf.length; i += 2) {
            if (j >= orgsize) break;
            int countByte = buf[i] & 0xff;
            int valByte = (i + 1 < buf.length) ? (buf[i + 1] & 0xff) : 0;
            int count = (j + countByte < orgsize) ? countByte : Math.max(0, orgsize - j - 1);
            Arrays.fill(org, j, j + count, (byte) valByte);
            j += countByte;
        }
        if (j < orgsize) {
            orgsize = j;
        }

        if (orgsize < 128 * 12 + 51) {
            throw new IllegalArgumentException("invalid decompressed size limit");
        }

        // load instruments
        for (int i = 0; i < 128 * 12; i++) {
            instr[i / 12][i % 12] = org[i] & 0xff;
        }
        for (int i = 0; i < 128; i++) {
            instr[i][2] ^= (instr[i][2] & 0x40) << 1;
            instr[i][3] ^= (instr[i][3] & 0x40) << 1;
            instr[i][11] >>= 4; // slide
        }

        // load song (tracklist)
        int total_patterns_in_hsp = (orgsize - 128 * 12 - 51) / (64 * 9 * 2);
        for (int i = 0; i < 51; i++) {
            song[i] = org[128 * 12 + i] & 0xff;
            if (((song[i] & 0x7f) > 0x31) || ((song[i] & 0x7f) >= total_patterns_in_hsp)) {
                song[i] = 0xff;
            }
        }

        // load patterns
        int patternDataStart = 128 * 12 + 51;
        int patternDataLen = orgsize - patternDataStart;
        for (int i = 0; i < 50 * 64 * 9; i++) {
            int idx = i >> 1;
            patterns[idx / (64 * 9)][idx % (64 * 9)][i & 1] = 0;
        }
        for (int i = 0; i < patternDataLen; i++) {
            int b = org[patternDataStart + i] & 0xff;
            int idx = i >> 1;
            patterns[idx / (64 * 9)][idx % (64 * 9)][i & 1] = b;
        }

        rewind(0);
    }
}
