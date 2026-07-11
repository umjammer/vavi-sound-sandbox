/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2007 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * AMD Loader (AMUSIC Adlib Tracker).
 * Ported from adplug's amd.cpp / amd.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class AmdPlayer extends ProtrackPlayer {

    private String songname = "";
    private String author = "";
    private final String[] instname = new String[26];

    public AmdPlayer() {
        super();
        for (int i = 0; i < 26; i++) {
            instname[i] = "";
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("AMUSIC Adlib Tracker", "amd");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("AMD");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(2048);
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
        int len = is.available();
        if (len < 1062 + 9) {
            return false;
        }
        byte[] buf = new byte[9];
        is.skip(1062);
        is.read(buf);

        boolean match1 = true;
        byte[] magic1 = { '<', 'o', (byte) 0xef, 'Q', 'U', (byte) 0xee, 'R', 'o', 'R' };
        for (int i = 0; i < 9; i++) {
            if (buf[i] != magic1[i]) {
                match1 = false;
                break;
            }
        }
        if (match1) return true;

        boolean match2 = true;
        byte[] magic2 = { 'M', 'a', 'D', 'o', 'K', 'a', 'N', '9', '6' };
        for (int i = 0; i < 9; i++) {
            if (buf[i] != magic2[i]) {
                match2 = false;
                break;
            }
        }
        return match2;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        int read = buf.length;
        if (read < 1062 + 10) {
            throw new IllegalArgumentException("file too short");
        }

        boolean match1 = true;
        byte[] magic1 = { '<', 'o', (byte) 0xef, 'Q', 'U', (byte) 0xee, 'R', 'o', 'R' };
        for (int i = 0; i < 9; i++) {
            if (buf[1062 + i] != magic1[i]) {
                match1 = false;
                break;
            }
        }
        boolean match2 = true;
        byte[] magic2 = { 'M', 'a', 'D', 'o', 'K', 'a', 'N', '9', '6' };
        for (int i = 0; i < 9; i++) {
            if (buf[1062 + i] != magic2[i]) {
                match2 = false;
                break;
            }
        }
        if (!match1 && !match2) {
            throw new IllegalArgumentException("invalid signature");
        }
        int version = buf[1062 + 9] & 0xff;

        songname = readString(buf, 0, 24);
        author = readString(buf, 24, 24);

        int offset = 48;
        for (int i = 0; i < 26; i++) {
            byte[] nameBuf = new byte[23];
            System.arraycopy(buf, offset, nameBuf, 0, 23);
            for (int j = 0; j < 23; j++) {
                if (nameBuf[j] == (byte) 0xff) {
                    nameBuf[j] = ' ';
                }
            }
            instname[i] = new String(nameBuf, java.nio.charset.StandardCharsets.US_ASCII).trim();
            offset += 23;

            inst[i].data[1] = buf[offset] & 0xff;
            inst[i].data[9] = buf[offset + 1] & 0xff;
            inst[i].data[3] = buf[offset + 2] & 0xff;
            inst[i].data[5] = buf[offset + 3] & 0xff;
            inst[i].data[7] = buf[offset + 4] & 0xff;
            inst[i].data[2] = buf[offset + 5] & 0xff;
            inst[i].data[10] = buf[offset + 6] & 0xff;
            inst[i].data[4] = buf[offset + 7] & 0xff;
            inst[i].data[6] = buf[offset + 8] & 0xff;
            inst[i].data[8] = buf[offset + 9] & 0xff;
            inst[i].data[0] = buf[offset + 10] & 0xff;
            offset += 11;
        }

        length = buf[offset] & 0xff;
        nop = (buf[offset + 1] & 0xff) + 1;
        if (length > 128 || nop > 64) {
            throw new IllegalArgumentException("invalid size parameters");
        }
        offset += 2;

        for (int i = 0; i < 128; i++) {
            order[i] = buf[offset + i] & 0xff;
        }
        for (int i = 0; i < length; i++) {
            if ((order[i] & 0x7f) >= 64) {
                throw new IllegalArgumentException("invalid pattern in order");
            }
        }
        offset += 128;
        offset += 10; // ignore

        int maxi = 0;
        if (version == 0x10) {
            initTrackord();
            maxi = nop * 9;

            for (int t = 0; t < maxi && offset < buf.length; t += 9) {
                for (int j = 0; j < 64; j++) {
                    for (int i = t; i < t + 9; i++) {
                        if (offset + 3 > buf.length) break;
                        int val = buf[offset] & 0x7f;
                        tracks[i][j].param1 = val / 10;
                        tracks[i][j].param2 = val % 10;

                        int val2 = buf[offset + 1] & 0xff;
                        tracks[i][j].command = val2 & 0x0f;
                        tracks[i][j].inst = val2 >> 4;

                        int val3 = buf[offset + 2] & 0xff;
                        tracks[i][j].inst += (val3 & 1) << 4;
                        if ((val3 >> 4) != 0) {
                            tracks[i][j].note = ((val3 & 0x0e) >> 1) * 12 + (val3 >> 4);
                        } else {
                            tracks[i][j].note = 0;
                        }
                        offset += 3;
                    }
                }
            }
        } else {
            for (int i = 0; i < nop; i++) {
                for (int j = 0; j < 9; j++) {
                    if (offset + 2 > buf.length) break;
                    trackord[i][j] = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                    trackord[i][j]++;
                    if (trackord[i][j] > 64 * 9) {
                        trackord[i][j] = 0;
                    }
                    offset += 2;
                }
            }

            if (offset + 2 > buf.length) throw new IllegalArgumentException("corrupted file");
            int numtrax = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
            offset += 2;

            for (int k = 0; k < numtrax; k++) {
                if (offset + 2 > buf.length) break;
                int i = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                if (i >= 64 * 9) {
                    i = 64 * 9 - 1;
                }
                maxi = Math.max(i + 1, maxi);
                offset += 2;

                for (int j = 0; j < 64; j++) {
                    if (offset >= buf.length) break;
                    int b = buf[offset] & 0xff;
                    offset++;

                    if ((b & 0x80) != 0) {
                        int len = Math.min(b & 0x7f, 64 - j);
                        for (int m = 0; m < len; m++) {
                            tracks[i][j + m].note = 0;
                            tracks[i][j + m].command = 0;
                            tracks[i][j + m].inst = 0;
                            tracks[i][j + m].param1 = 0;
                            tracks[i][j + m].param2 = 0;
                        }
                        j += len - 1;
                        continue;
                    }

                    tracks[i][j].param1 = b / 10;
                    tracks[i][j].param2 = b % 10;

                    if (offset + 2 > buf.length) break;
                    int val2 = buf[offset] & 0xff;
                    tracks[i][j].command = val2 & 0x0f;
                    tracks[i][j].inst = val2 >> 4;

                    int val3 = buf[offset + 1] & 0xff;
                    tracks[i][j].inst += (val3 & 1) << 4;
                    if ((val3 >> 4) != 0) {
                        tracks[i][j].note = ((val3 & 0x0e) >> 1) * 12 + (val3 >> 4);
                    } else {
                        tracks[i][j].note = 0;
                    }
                    offset += 2;
                }
            }
        }

        bpm = 50;
        restartpos = 0;
        flags = Decimal;

        for (int i = 0; i < maxi; i++) {
            for (int j = 0; j < 64; j++) {
                int cmd = tracks[i][j].command;
                int[] convfx = { 0, 1, 2, 9, 17, 11, 13, 18, 3, 14 };
                if (cmd < convfx.length) {
                    tracks[i][j].command = convfx[cmd];
                } else {
                    tracks[i][j].command = 0;
                }

                if (tracks[i][j].command == 14) {
                    if (tracks[i][j].param1 == 2) {
                        tracks[i][j].command = 10;
                        tracks[i][j].param1 = tracks[i][j].param2;
                        tracks[i][j].param2 = 0;
                    }
                    if (tracks[i][j].param1 == 3) {
                        tracks[i][j].command = 10;
                        tracks[i][j].param1 = 0;
                    }
                }

                if (tracks[i][j].command == 17) {
                    int vol = tracks[i][j].param1 * 10 + tracks[i][j].param2;
                    int[] convvol = {
                        0x00, 0x00, 0x00, 0x01, 0x01, 0x02, 0x02, 0x03,
                        0x03, 0x04, 0x04, 0x05, 0x05, 0x06, 0x06, 0x07,
                        0x07, 0x08, 0x09, 0x09, 0x0a, 0x0a, 0x0b, 0x0c,
                        0x0c, 0x0d, 0x0e, 0x0e, 0x0f, 0x10, 0x10, 0x11,
                        0x12, 0x13, 0x14, 0x14, 0x15, 0x16, 0x17, 0x18,
                        0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f, 0x21,
                        0x22, 0x23, 0x25, 0x26, 0x28, 0x29, 0x2b, 0x2d,
                        0x2e, 0x30, 0x32, 0x35, 0x37, 0x3a, 0x3c, 0x3f
                    };
                    if (vol < convvol.length) {
                        vol = convvol[vol];
                    } else {
                        vol = 63;
                    }
                    tracks[i][j].param1 = vol / 10;
                    tracks[i][j].param2 = vol % 10;
                }
            }
        }

        rewind(0);
    }

    @Override
    public float getRefresh() {
        if (tempo != 0) {
            return (float) tempo;
        } else {
            return 18.2f;
        }
    }


}
