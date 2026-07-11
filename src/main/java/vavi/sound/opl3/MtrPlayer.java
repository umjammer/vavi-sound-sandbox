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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;

/**
 * Master Tracker Loader.
 * Ported from adplug's mtr.cpp / mtr.h by Dmitry Smagin.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class MtrPlayer extends ProtrackPlayer {

    private static final Logger logger = getLogger(MtrPlayer.class.getName());

    private int timer;
    private int version;
    private int ninstruments;

    public MtrPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Master Tracker", "mtr");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MTR");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(9);
            byte[] id = new byte[9];
            int r = bitStream.read(id);
            if (r < 9) return false;
            String s = new String(id, StandardCharsets.US_ASCII);
            return s.startsWith("MTRAC ") || s.startsWith("MTRACK NC");
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
        if (buf.length < 53) {
            throw new IllegalArgumentException("file too short");
        }

        int offset = 0;
        String header = new String(buf, 0, 50, StandardCharsets.US_ASCII);
        offset += 50;

        int nvoices, npatterns, orderlen, restart;
        int timervalue = 0x428F;
        String mtitle;

        if (header.startsWith("MTRAC ")) {
            version = 1;
            // "%02x %02x %02x %02x %08x" at header+26
            int[] v = parseHexFields(header.substring(26), 5);
            if (v == null) {
                throw new IllegalArgumentException("invalid MTR v1 header");
            }
            nvoices = v[0];
            npatterns = v[1];
            orderlen = v[2];
            restart = v[3];
            // v[4] is flength, unused

            mtitle = header.substring(6, 26);
            timervalue = u16(buf, offset); offset += 2;
            offset++; // 0=SPK 1=ADL 2=SBP
        } else if (header.startsWith("MTRACK NC")) {
            version = 2;
            // "%02x %02x %02x %02x %02x %02x %04x %08x" at header+10
            int[] v = parseHexFields(header.substring(10), 8);
            if (v == null) {
                throw new IllegalArgumentException("invalid MTR v2 header");
            }
            nvoices = v[0];
            // v[1] is ndigvoices, unused
            npatterns = v[2];
            orderlen = v[3];
            ninstruments = v[4];
            restart = v[5];
            timervalue = v[6];
            // v[7] is flength, unused

            mtitle = new String(buf, offset, 20, StandardCharsets.US_ASCII);
            offset += 20;
        } else {
            throw new IllegalArgumentException("invalid signature");
        }

logger.log(Level.DEBUG, "version: " + version + ", title: " + mtitle.trim());

        // corrections for read data
        nvoices++;
        ninstruments = version == 2 ? ninstruments : 64;

        // data for Protracker
        length = orderlen + 1;
        nop = npatterns + 1;
        timer = 1193180 / (timervalue != 0 ? timervalue : 0x428F);

        // init CmodPlayer
        reallocInstruments(ninstruments);
        reallocOrder(length);
        reallocPatterns(nop, 64, nvoices);
        initTrackord();

        // load order
        for (int i = 0; i < length; i++) {
            order[i] = buf[offset++] & 0xff;
        }
        offset += 256 - length;

        int[] table = { 4, 0, 6, 2, 8, 3, 9, 5, 11, 1, 7 };

        // read instruments (name 20 + is_used 1 + data 12 + 31 padding)
        for (int i = 0; i < ninstruments; i++) {
            offset += 20; // name
            int isUsed = buf[offset++] & 0xff;
            int dataOff = offset;
            offset += 12;
            offset += 31;

            // convert
            if (isUsed == 2) {
                for (int j = 0; j < 11; j++) {
                    inst[i].data[j] = buf[dataOff + table[j]] & 0xff;
                }
            }
        }

        // load tracks
        for (int i = 0; i < nop; i++) {
            for (int k = 0; k < 64; k++) {
                for (int j = 0; j < nvoices; j++) {
                    // event bytes are signed chars in adplug; the shifts below
                    // must sign-extend to match
                    int e0 = offset < buf.length ? buf[offset] : 0; offset++;
                    int e1 = offset < buf.length ? buf[offset] : 0; offset++;
                    int e2 = offset < buf.length ? buf[offset] : 0; offset++;
                    int val = offset < buf.length ? buf[offset] : 0; offset++;

                    int note = e0 != 0 ? ((e0 & 0xf) + ((e0 >> 4) * 12)) & 0xff : 0;
                    int instVal = e1 & 0x3f;
                    int fx = e2 & 0xf;

                    int t = i * nvoices + j;
                    tracks[t][k].note = note;
                    tracks[t][k].inst = instVal;

                    // translate effects
                    switch (fx) {
                    case 0: // 0xy, arp
                    // 1 and 2 never occur in any .mtr, so might sound wrong
                    case 1: // 1xy, slide up
                    case 2: // 2xy, slide down
                        tracks[t][k].command = fx;
                        tracks[t][k].param1 = (val >> 4) & 0xff;
                        tracks[t][k].param2 = val & 0xf;
                        break;
                    case 3: // 3xy, fine slide up
                    case 4: // 4xy, fine slide down
                        tracks[t][k].command = fx == 3 ? 0x17 : 0x18;
                        tracks[t][k].param1 = (val >> 4) & 0xff;
                        tracks[t][k].param2 = val & 0xf;
                        break;
                    case 5: // 5xy -> C(63-xy), set volume
                        tracks[t][k].command = 0xc;
                        tracks[t][k].param1 = ((63 - val) >> 4) & 0xff;
                        tracks[t][k].param2 = (63 - val) & 0xf;
                        break;
                    case 0xB: // Bxy -> Fxy, set speed
                        tracks[t][k].command = 0xf;
                        tracks[t][k].param1 = (val >> 4) & 0xff;
                        tracks[t][k].param2 = val & 0xf;
                        break;
                    case 0xF:
                        if (val == 1) { // F01 -> D00, pattern break
                            tracks[t][k].command = 0xd;
                            tracks[t][k].param1 = 0;
                            tracks[t][k].param2 = 0;
                            break;
                        } else if (val == 2) { // F02 -> note off
                            tracks[t][k].note = 0x7f;
                            tracks[t][k].inst = 0;
                            break;
                        }
                        // fall through
                    default:
                        // Unsupported:
                        // Axy - retrigger
                        // Cxy - go to order position
                        // F00 - stop playing and restart
                        if ((fx | val) != 0) {
logger.log(Level.DEBUG, String.format("Unsupported effect: %02x-%02x", fx, val));
                        }
                    }
                }
            }
        }

        // data for Protracker
        restartpos = restart;
        initspeed = 6;

        rewind(0);
    }

    /** parses n whitespace-separated hex fields, null on failure */
    private static int[] parseHexFields(String s, int n) {
        int nul = s.indexOf(0);
        if (nul >= 0) {
            s = s.substring(0, nul);
        }
        String[] tokens = s.trim().split("\\s+");
        if (tokens.length < n) return null;
        int[] result = new int[n];
        try {
            for (int i = 0; i < n; i++) {
                result[i] = Integer.parseInt(tokens[i], 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return result;
    }

    /** little-endian unsigned 16-bit */
    private static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }

    @Override
    public float getRefresh() {
        return timer;
    }
}
