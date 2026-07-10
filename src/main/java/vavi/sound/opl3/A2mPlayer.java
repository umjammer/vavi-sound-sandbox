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
 * Adlib Tracker 2 Player / Loader.
 * Ported from adplug's a2m.cpp / a2m.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class A2mPlayer extends ProtrackPlayer {

    private static final int NUMINST = 250;
    private static final int INSTDATASIZE = 13;

    private String songname = "";
    private String author = "";
    private final String[] instname = new String[NUMINST];

    public A2mPlayer() {
        super();
        for (int i = 0; i < NUMINST; i++) {
            instname[i] = "";
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Adlib Tracker 2", "a2m");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("A2M");
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
        byte[] id = new byte[16];
        int read = is.read(id);
        if (read < 16) return false;
        if (!"_A2module_".equals(new String(id, 0, 10, java.nio.charset.StandardCharsets.US_ASCII))) {
            return false;
        }
        // this loader only handles the versions load() supports; the other
        // versions (2, 3, 6, 7, 9-14) fall through to A2mV2Player
        int version = id[14] & 0xff;
        int numpats = id[15] & 0xff;
        return (version == 1 || version == 4 || version == 5 || version == 8) &&
                numpats >= 1 && numpats <= 64;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        int read = buf.length;
        if (read < 16) {
            throw new IllegalArgumentException("file too short");
        }

        String id = new String(buf, 0, 10, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"_A2module_".equals(id)) {
            throw new IllegalArgumentException("invalid signature");
        }

        int version = buf[14] & 0xff;
        int numpats = buf[15] & 0xff;

        if ((version != 1 && version != 5 && version != 4 && version != 8) || numpats < 1 || numpats > 64) {
            throw new IllegalArgumentException("unsupported version or invalid pattern count");
        }

        nop = numpats;
        length = 128;
        restartpos = 0;

        int[] len = new int[9];
        int t;
        int offset = 16;
        if (version < 5) {
            for (int i = 0; i < 5; i++) {
                len[i] = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                offset += 2;
            }
            t = 9;
        } else {
            for (int i = 0; i < 9; i++) {
                len[i] = (buf[offset] & 0xff) | ((buf[offset + 1] & 0xff) << 8);
                offset += 2;
            }
            t = 18;
        }

        int needed = 43 + 43 + NUMINST * 33 + NUMINST * INSTDATASIZE + length + 2 + (version >= 5 ? 1 : 0);
        byte[] org;
        int orgOffset = 0;

        if (version == 1 || version == 5) {
            int wordsCount = len[0] / 2;
            int[] secdata = new int[wordsCount];
            int secOffset = offset;
            for (int i = 0; i < wordsCount; i++) {
                secdata[i] = (buf[secOffset] & 0xff) | ((buf[secOffset + 1] & 0xff) << 8);
                secOffset += 2;
            }
            offset = secOffset;

            org = new byte[needed];
            int decoded = Sixdepak.decode(secdata, len[0], org, needed);
            if (decoded < needed) {
                throw new IllegalArgumentException("decoding failed");
            }
        } else {
            org = new byte[len[0]];
            System.arraycopy(buf, offset, org, 0, len[0]);
            offset += len[0];
        }

        songname = readPascalString(org, orgOffset, 42);
        orgOffset += 43;
        author = readPascalString(org, orgOffset, 42);
        orgOffset += 43;

        int instNameStart = orgOffset;
        orgOffset += NUMINST * 33;

        for (int i = 0; i < NUMINST; i++) {
            instname[i] = readPascalString(org, instNameStart + i * 33, 32);

            int instDataOffset = orgOffset + i * INSTDATASIZE;
            inst[i].data[0] = org[instDataOffset + 10] & 0xff;
            inst[i].data[1] = org[instDataOffset] & 0xff;
            inst[i].data[2] = org[instDataOffset + 1] & 0xff;
            inst[i].data[3] = org[instDataOffset + 4] & 0xff;
            inst[i].data[4] = org[instDataOffset + 5] & 0xff;
            inst[i].data[5] = org[instDataOffset + 6] & 0xff;
            inst[i].data[6] = org[instDataOffset + 7] & 0xff;
            inst[i].data[7] = org[instDataOffset + 8] & 0xff;
            inst[i].data[8] = org[instDataOffset + 9] & 0xff;
            inst[i].data[9] = org[instDataOffset + 2] & 0xff;
            inst[i].data[10] = org[instDataOffset + 3] & 0xff;

            if (version < 5) {
                inst[i].misc = org[instDataOffset + 11] & 0xff;
            } else {
                int pan = org[instDataOffset + 11] & 0xff;
                if (pan != 0) {
                    inst[i].data[0] |= (pan & 3) << 4;
                } else {
                    inst[i].data[0] |= 0x30;
                }
            }

            inst[i].slide = org[instDataOffset + 12];
        }
        orgOffset += NUMINST * INSTDATASIZE;

        for (int i = 0; i < length; i++) {
            order[i] = org[orgOffset + i] & 0xff;
            if ((order[i] & 0x7f) >= numpats) {
                order[i] &= 0x80;
            }
        }
        orgOffset += length;

        bpm = org[orgOffset] & 0xff;
        initspeed = org[orgOffset + 1] & 0xff;
        int flagsByte = 0;
        if (version >= 5) {
            flagsByte = org[orgOffset + 2] & 0xff;
        }

        int ppb = version < 5 ? 16 : 8;
        int blocks = (numpats + ppb - 1) / ppb;
        int alength = len[1];
        for (int i = 2; i <= blocks; i++) {
            alength += len[i];
        }

        int neededPatterns = numpats * 64 * t * 4;
        byte[] patternsData;
        int patternsOffset = 0;

        if (version == 1 || version == 5) {
            patternsData = new byte[neededPatterns];
            int wordsCount = alength / 2;
            int[] secdata = new int[wordsCount];
            int secOffset = offset;
            for (int i = 0; i < wordsCount; i++) {
                if (secOffset + 2 > buf.length) break;
                secdata[i] = (buf[secOffset] & 0xff) | ((buf[secOffset + 1] & 0xff) << 8);
                secOffset += 2;
            }
            offset = secOffset;

            int decPtr = 0;
            int secPtr = 0;
            for (int i = 1; i <= blocks; i++) {
                int[] blockData = new int[len[i] / 2];
                System.arraycopy(secdata, secPtr, blockData, 0, len[i] / 2);
                secPtr += len[i] / 2;

                int blockNeeded = neededPatterns - decPtr;
                byte[] blockDecoded = new byte[blockNeeded];
                int decoded = Sixdepak.decode(blockData, len[i], blockDecoded, blockNeeded);
                System.arraycopy(blockDecoded, 0, patternsData, decPtr, decoded);
                decPtr += decoded;
            }
        } else {
            patternsData = new byte[alength];
            System.arraycopy(buf, offset, patternsData, 0, alength);
            offset += alength;
            patternsOffset = alength;
        }

        if (version < 5) {
            int[] convfx = { 0, 1, 2, 23, 24, 3, 5, 4, 6, 9, 17, 13, 11, 19, 7, 14 };
            int[] convinf1 = { 0, 1, 2, 6, 7, 8, 9, 4, 5, 3, 10, 11, 12, 13, 14, 15 };

            for (int i = 0; i < numpats; i++) {
                for (int j = 0; j < 64; j++) {
                    for (int k = 0; k < t; k++) {
                        Tracks track = tracks[i * t + k][j];
                        int oIdx = (i * 64 * t + j * t + k) * 4;
                        if (version != 1 && version != 5) {
                            oIdx = patternsOffset - oIdx - 4;
                        }
                        if (oIdx < 0 || oIdx + 4 > patternsData.length) continue;

                        int o0 = patternsData[oIdx] & 0xff;
                        int o1 = patternsData[oIdx + 1] & 0xff;
                        int o2 = patternsData[oIdx + 2] & 0xff;
                        int o3 = patternsData[oIdx + 3] & 0xff;

                        track.note = o0 == 255 ? 127 : o0;
                        track.inst = o1 <= NUMINST ? o1 : 0;
                        track.command = o2 < convfx.length ? convfx[o2] : 255;
                        track.param2 = o3 & 0x0f;
                        track.param1 = o3 >> 4;

                        if (track.command == 14) {
                            track.param1 = convinf1[track.param1];
                            switch (track.param1) {
                                case 2 -> {
                                    track.command = 25;
                                    track.param1 = track.param2;
                                    track.param2 = 0xf;
                                }
                                case 8 -> {
                                    track.command = 26;
                                    track.param1 = track.param2;
                                    track.param2 = 0;
                                }
                                case 9 -> {
                                    track.command = 26;
                                    track.param1 = 0;
                                }
                                case 15 -> {
                                    if (track.param2 == 0) {
                                        track.command = 8;
                                        track.param1 = 0;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            reallocPatterns(numpats, 64, t);
            int[] newconvfx = {
                0, 1, 2, 3, 4, 5, 6, 23, 24, 21, 10, 11, 17, 13, 7, 19,
                255, 255, 22, 25, 255, 15, 255, 255, 255, 255, 255,
                255, 255, 255, 255, 255, 255, 255, 255, 14, 255
            };

            for (int i = 0; i < numpats; i++) {
                for (int j = 0; j < t; j++) {
                    for (int k = 0; k < 64; k++) {
                        Tracks track = tracks[i * t + j][k];
                        int oIdx = (i * 64 * t + j * 64 + k) * 4;
                        if (version != 1 && version != 5) {
                            oIdx = patternsOffset - oIdx - 4;
                        }
                        if (oIdx < 0 || oIdx + 4 > patternsData.length) continue;

                        int o0 = patternsData[oIdx] & 0xff;
                        int o1 = patternsData[oIdx + 1] & 0xff;
                        int o2 = patternsData[oIdx + 2] & 0xff;
                        int o3 = patternsData[oIdx + 3] & 0xff;

                        track.note = o0 == 255 ? 127 : o0;
                        track.inst = o1 <= NUMINST ? o1 : 0;
                        track.command = o2 < newconvfx.length ? newconvfx[o2] : 255;
                        track.param1 = o3 >> 4;
                        track.param2 = o3 & 0x0f;

                        if (o2 == 36) {
                            switch (track.param1) {
                                case 0 -> {
                                    track.command = 29;
                                    track.param1 = 0;
                                }
                                case 1 -> {
                                    track.command = 14;
                                    track.param1 = 8;
                                }
                            }
                        }
                    }
                }
            }
        }

        initTrackord();

        if (version >= 5) {
            this.flags |= Opl3;
            if ((flagsByte & 8) != 0) this.flags |= Tremolo;
            if ((flagsByte & 16) != 0) this.flags |= Vibrato;
        }

        rewind(0);
    }

    private String readPascalString(byte[] buf, int offset, int maxLength) {
        int len = buf[offset] & 0xff;
        if (len > maxLength) {
            len = maxLength;
        }
        return new String(buf, offset + 1, len, java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Override
    public float getRefresh() {
        if (tempo == 18) {
            return 18.2f;
        } else {
            return (float) (tempo);
        }
    }
}
