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
import java.util.Arrays;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * DeFy Adlib Tracker DTM Loader.
 * Ported from adplug's dtm.cpp / dtm.h by Riven the Mage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class DtmPlayer extends ProtrackPlayer {

    private String songTitle = "";
    private String author = "";
    private String description = "";

    public DtmPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("DeFy Adlib Tracker", "dtm");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("DTM");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(32);
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
        byte[] buf = new byte[13];
        int read = is.read(buf);
        if (read < 13) return false;
        String sig = new String(buf, 0, 9, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"DeFy DTM ".equals(sig)) {
            return false;
        }
        int ver = buf[12] & 0xff;
        return ver == 0x10;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 12 + 1 + 20 + 20 + 1 + 1) {
            throw new IllegalArgumentException("file too short");
        }

        String sig = new String(buf, 0, 9, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"DeFy DTM ".equals(sig)) {
            throw new IllegalArgumentException("invalid signature");
        }

        int version = buf[12] & 0xff;
        if (version != 0x10) {
            throw new IllegalArgumentException("unsupported version");
        }

        songTitle = readPascalString(buf, 13, 20);
        author = readPascalString(buf, 33, 20);

        int numpat = buf[53] & 0xff;
        int numinst = (buf[54] & 0xff) + 1;

        if (numinst > 128 || numinst < 9 || numpat == 0) {
            throw new IllegalArgumentException("invalid counts");
        }

        int offset = 55;

        // load description
        StringBuilder descBuilder = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            if (offset >= buf.length) break;
            int bufstrLength = buf[offset++] & 0xff;
            if (bufstrLength > 80) {
                throw new IllegalArgumentException("description line too long");
            }
            if (offset + bufstrLength > buf.length) {
                throw new IllegalArgumentException("description truncated");
            }
            byte[] lineBuf = new byte[bufstrLength];
            System.arraycopy(buf, offset, lineBuf, 0, bufstrLength);
            offset += bufstrLength;

            for (int j = 0; j < bufstrLength; j++) {
                if (lineBuf[j] == 0) {
                    lineBuf[j] = 0x20;
                }
            }
            descBuilder.append(new String(lineBuf, java.nio.charset.StandardCharsets.US_ASCII));
            descBuilder.append('\n');
        }
        description = descBuilder.toString();

        reallocInstruments(numinst);
        reallocOrder(100);
        reallocPatterns(numpat, 64, 9);

        int[] convInst = { 2, 1, 10, 9, 4, 3, 6, 5, 0, 8, 7 };
        int[] convNote = {
            0x16B, 0x181, 0x198, 0x1B0, 0x1CA, 0x1E5,
            0x202, 0x220, 0x241, 0x263, 0x287, 0x2AE
        };
        initNotetable(convNote);
        initTrackord();

        // load instruments
        for (int i = 0; i < numinst; i++) {
            if (offset >= buf.length) throw new IllegalArgumentException("truncated instruments");
            int nameLength = buf[offset++] & 0xff;
            if (nameLength >= 13) {
                throw new IllegalArgumentException("instrument name too long");
            }
            String instName = "";
            if (nameLength > 0) {
                if (offset + nameLength > buf.length) throw new IllegalArgumentException("truncated instrument name");
                instName = new String(buf, offset, nameLength, java.nio.charset.StandardCharsets.US_ASCII);
                offset += nameLength;
            }
            if (offset + 12 > buf.length) throw new IllegalArgumentException("truncated instrument data");
            byte[] instData = new byte[12];
            System.arraycopy(buf, offset, instData, 0, 12);
            offset += 12;

            for (int j = 0; j < convInst.length; j++) {
                inst[i].data[convInst[j]] = instData[j] & 0xff;
            }
        }

        if (offset + 100 > buf.length) {
            throw new IllegalArgumentException("truncated order table");
        }
        for (int i = 0; i < 100; i++) {
            order[i] = buf[offset + i] & 0xff;
        }
        offset += 100;

        // load tracks
        nop = numpat;
        int t = 0;
        for (int i = 0; i < nop; i++) {
            if (offset + 2 > buf.length) throw new IllegalArgumentException("truncated pattern length");
            int packedLength = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
            offset += 2;

            byte[] unpackedPattern = new byte[64 * 9 * 2];
            if (!unpackPattern(buf, offset, packedLength, unpackedPattern, unpackedPattern.length)) {
                throw new IllegalArgumentException("pattern unpack failed");
            }
            offset += packedLength;

            int patOffset = 0;
            for (int j = 0; j < 9; j++, t++) {
                for (int k = 0; k < 64; k++) {
                    int eventIdx = (k * 9 + j) * 2;
                    int byte0 = unpackedPattern[eventIdx] & 0xff;
                    int byte1 = unpackedPattern[eventIdx + 1] & 0xff;

                    Tracks track = tracks[t][k];

                    if (byte0 == 0x80) {
                        if (byte1 < numinst) {
                            track.inst = byte1 + 1;
                        }
                    } else {
                        track.note = byte0;
                        if (byte0 != 0 && byte0 != 127) {
                            track.note++;
                        }

                        int evParam = byte1 & 0x0F;
                        switch (byte1 >> 4) {
                            case 0x0 -> {
                                if (evParam == 1) {
                                    track.command = 13;
                                }
                            }
                            case 0x1 -> {
                                track.command = 28;
                                track.param1 = evParam;
                            }
                            case 0x2 -> {
                                track.command = 28;
                                track.param2 = evParam;
                            }
                            case 0xA, 0xC -> {
                                track.command = 22;
                                track.param1 = (0x3F - evParam) >> 4;
                                track.param2 = (0x3F - evParam) & 0xF;
                            }
                            case 0xB -> {
                                track.command = 21;
                                track.param1 = (0x3F - evParam) >> 4;
                                track.param2 = (0x3F - evParam) & 0xF;
                            }
                            case 0xF -> {
                                track.command = 13;
                                track.param2 = evParam;
                            }
                        }
                    }
                }
            }
        }

        // order length
        length = 100;
        for (int i = 0; i < 100; i++) {
            if ((order[i] & 0x80) != 0) {
                length = i;
                if ((order[i] & 0xff) == 0xFF) {
                    restartpos = 0;
                } else {
                    restartpos = (order[i] & 0xff) - 0x80;
                }
                if (restartpos >= i) {
                    throw new IllegalArgumentException("invalid restart position");
                }
                break;
            } else if ((order[i] & 0xff) >= nop) {
                throw new IllegalArgumentException("invalid pattern index in order list");
            }
        }

        initspeed = 2;

        rewind(0);
    }

    private boolean unpackPattern(byte[] ibuf, int ibufOffset, int ilen, byte[] obuf, int olen) {
        int ipos = 0;
        int opos = 0;

        while (ilen > 0) {
            int repeatCounter = 1;
            if (ibufOffset + ipos >= ibuf.length) return false;
            int repeatByte = ibuf[ibufOffset + ipos++] & 0xff;
            ilen--;

            if ((repeatByte & 0xF0) == 0xD0) {
                if (ilen == 0) return false;
                ilen--;
                if (ibufOffset + ipos >= ibuf.length) return false;
                repeatCounter = repeatByte & 0x0F;
                repeatByte = ibuf[ibufOffset + ipos++] & 0xff;
            }

            int count = Math.min(repeatCounter, olen - opos);
            Arrays.fill(obuf, opos, opos + count, (byte) repeatByte);
            opos += count;
        }

        return opos == olen;
    }

    private String readPascalString(byte[] buf, int offset, int maxLength) {
        int len = 0;
        while (len < maxLength && offset + len < buf.length && buf[offset + len] != 0) {
            len++;
        }
        return new String(buf, offset, len, java.nio.charset.StandardCharsets.US_ASCII).trim();
    }

    @Override
    public void rewind(int subsong) throws IOException {
        super.rewind(subsong);
        for (int i = 0; i < 9; i++) {
            channel[i].inst = i;
            channel[i].vol1 = 63 - (inst[i].data[10] & 63);
            channel[i].vol2 = 63 - (inst[i].data[9] & 63);
        }
    }

    @Override
    public float getRefresh() {
        return 18.2f;
    }
}
