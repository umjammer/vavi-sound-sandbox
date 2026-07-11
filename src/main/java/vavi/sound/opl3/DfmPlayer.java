/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2003 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * Digital-FM Loader.
 * Ported from adplug's dfm.cpp / dfm.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class DfmPlayer extends ProtrackPlayer {

    private String songTitle = "";
    private int hiVer;
    private int loVer;

    public DfmPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Digital-FM", "dfm");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("DFM");
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

    protected boolean matchFormatImpl(InputStream is) throws IOException {
        byte[] id = new byte[4];
        int read = is.read(id);
        if (read < 4) return false;
        if (id[0] != 'D' || id[1] != 'F' || id[2] != 'M' || id[3] != 0x1a) {
            return false;
        }
        int hi = is.read();
        return hi >= 0 && hi <= 1;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 6 + 33 + 1 + 32 * 12 + 32 * 11 + 128 + 1) {
            throw new IllegalArgumentException("file too short");
        }

        if (buf[0] != 'D' || buf[1] != 'F' || buf[2] != 'M' || buf[3] != 0x1a) {
            throw new IllegalArgumentException("invalid signature");
        }

        hiVer = buf[4] & 0xff;
        loVer = buf[5] & 0xff;

        if (hiVer > 1) {
            throw new IllegalArgumentException("unsupported version");
        }

        int songinfoLen = buf[6] & 0xff;
        if (songinfoLen > 32) {
            throw new IllegalArgumentException("invalid song title length");
        }
        songTitle = new String(buf, 7, songinfoLen, java.nio.charset.StandardCharsets.US_ASCII).trim();

        int offset = 6 + 33;
        initspeed = buf[offset] & 0xff;
        offset++;

        for (int i = 0; i < 32; i++) {
            int instnameLen = buf[offset] & 0xff;
            if (instnameLen > 11) {
                throw new IllegalArgumentException("invalid instrument name length");
            }
            offset += 12;
        }

        reallocInstruments(32);
        for (int i = 0; i < 32; i++) {
            inst[i].data[1] = buf[offset] & 0xff;
            inst[i].data[2] = buf[offset + 1] & 0xff;
            inst[i].data[9] = buf[offset + 2] & 0xff;
            inst[i].data[10] = buf[offset + 3] & 0xff;
            inst[i].data[3] = buf[offset + 4] & 0xff;
            inst[i].data[4] = buf[offset + 5] & 0xff;
            inst[i].data[5] = buf[offset + 6] & 0xff;
            inst[i].data[6] = buf[offset + 7] & 0xff;
            inst[i].data[7] = buf[offset + 8] & 0xff;
            inst[i].data[8] = buf[offset + 9] & 0xff;
            inst[i].data[0] = buf[offset + 10] & 0xff;
            offset += 11;
        }

        reallocOrder(128);
        for (int i = 0; i < 128; i++) {
            order[i] = buf[offset + i] & 0xff;
        }
        offset += 128;

        int len = 0;
        for (int i = 0; i < 128 && order[i] != 128; i++) {
            len = i + 1;
        }
        length = len;

        int npats = buf[offset] & 0xff;
        offset++;

        if (npats > 64) {
            throw new IllegalArgumentException("too many patterns");
        }

        reallocPatterns(npats, 64, 9);
        initTrackord();
        flags = Standard;
        bpm = 0;

        int[] convfx = { 255, 255, 17, 19, 23, 24, 255, 13 };

        for (int i = 0; i < npats; i++) {
            if (offset >= buf.length) break;
            int n = buf[offset] & 0xff;
            offset++;

            if (n >= npats) {
                throw new IllegalArgumentException("invalid pattern index");
            }

            for (int r = 0; r < 64; r++) {
                for (int c = 0; c < 9; c++) {
                    if (offset >= buf.length) break;
                    int note = buf[offset] & 0xff;
                    offset++;

                    Tracks track = tracks[n * 9 + c][r];
                    if ((note & 15) == 15) {
                        track.note = 127;
                    } else {
                        track.note = ((note & 127) >> 4) * 12 + (note & 15);
                    }

                    if ((note & 128) != 0) {
                        if (offset >= buf.length) break;
                        int fx = buf[offset] & 0xff;
                        offset++;

                        if ((fx >> 5) == 1) {
                            track.inst = (fx & 31) + 1;
                        } else {
                            track.command = convfx[fx >> 5];
                            if (track.command == 17) {
                                int param = fx & 31;
                                param = 63 - param * 2;
                                track.param1 = param >> 4;
                                track.param2 = param & 15;
                            } else {
                                track.param1 = (fx & 31) >> 4;
                                track.param2 = fx & 15;
                            }
                        }
                    }
                }
            }
        }

        rewind(0);
    }

    @Override
    public float getRefresh() {
        return 125.0f;
    }
}
