/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * Surprise! Adlib Tracker 2 (SAdT/SAdT2) Loader.
 * Ported from adplug's sa2.cpp / sa2.h by Simon Peter and Mamiya.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class Sa2Player extends ProtrackPlayer {

    private static final Logger logger = getLogger(Sa2Player.class.getName());

    private static final int HAS_ARPEGGIOLIST = 1 << 7;
    private static final int HAS_V7PATTERNS = 1 << 6;
    private static final int HAS_ACTIVECHANNELS = 1 << 5;
    private static final int HAS_TRACKORDER = 1 << 4;
    private static final int HAS_ARPEGGIO = 1 << 3;
    private static final int HAS_OLDBPM = 1 << 2;
    private static final int HAS_OLDPATTERNS = 1 << 1;
    private static final int HAS_UNKNOWN127 = 1 << 0;

    private int version;

    public Sa2Player() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Surprise! Adlib Tracker 2", "sat");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("SA2");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(5);
            byte[] id = new byte[5];
            if (bitStream.read(id) < 5) return false;
            if (id[0] != 'S' || id[1] != 'A' || id[2] != 'd' || id[3] != 'T') return false;
            int ver = id[4] & 0xff;
            return ver >= 1 && ver <= 9;
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
        int[] convfx = { 0, 1, 2, 3, 4, 5, 6, 255, 8, 255, 10, 11, 12, 13, 255, 15 };

        if (buf.length < 5) {
            throw new IllegalArgumentException("file too short");
        }
        if (buf[0] != 'S' || buf[1] != 'A' || buf[2] != 'd' || buf[3] != 'T') {
            throw new IllegalArgumentException("invalid signature");
        }
        version = buf[4] & 0xff;

        int notedis = 0;
        int satType;
        switch (version) {
        case 1:
            notedis = 0x18;
            satType = HAS_UNKNOWN127 | HAS_OLDPATTERNS | HAS_OLDBPM;
            break;
        case 2:
            notedis = 0x18;
            satType = HAS_OLDPATTERNS | HAS_OLDBPM;
            break;
        case 3:
            notedis = 0x0c;
            satType = HAS_OLDPATTERNS | HAS_OLDBPM;
            break;
        case 4:
            notedis = 0x0c;
            satType = HAS_ARPEGGIO | HAS_OLDPATTERNS | HAS_OLDBPM;
            break;
        case 5:
            notedis = 0x0c;
            satType = HAS_ARPEGGIO | HAS_ARPEGGIOLIST | HAS_OLDPATTERNS | HAS_OLDBPM;
            break;
        case 6:
            satType = HAS_ARPEGGIO | HAS_ARPEGGIOLIST | HAS_OLDPATTERNS | HAS_OLDBPM;
            break;
        case 7:
            satType = HAS_ARPEGGIO | HAS_ARPEGGIOLIST | HAS_V7PATTERNS;
            break;
        case 8:
            satType = HAS_ARPEGGIO | HAS_ARPEGGIOLIST | HAS_TRACKORDER;
            break;
        case 9:
            satType = HAS_ARPEGGIO | HAS_ARPEGGIOLIST | HAS_TRACKORDER | HAS_ACTIVECHANNELS;
            break;
        default:
            throw new IllegalArgumentException("unknown version: " + version);
        }

        int offset = 5;

        // instruments
        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 11; j++) {
                inst[i].data[j] = u8(buf, offset++);
            }
            if ((satType & HAS_ARPEGGIO) != 0) {
                inst[i].arpstart = u8(buf, offset++);
                inst[i].arpspeed = u8(buf, offset++);
                inst[i].arppos = u8(buf, offset++);
                inst[i].arpspdcnt = u8(buf, offset++);
            } else {
                inst[i].arpstart = 0;
                inst[i].arpspeed = 0;
                inst[i].arppos = 0;
                inst[i].arpspdcnt = 0;
            }
            inst[i].misc = 0;
            inst[i].slide = 0;
        }

        // instrument names (29 * 17, unused for playback)
        offset += 29 * 17;

        offset += 3; // dummy bytes
        for (int i = 0; i < 128; i++) { // pattern orders
            order[i] = u8(buf, offset++);
        }
        if ((satType & HAS_UNKNOWN127) != 0) {
            offset += 127;
        }

        // infos
        nop = u16(buf, offset); offset += 2;
        length = u8(buf, offset++);
        restartpos = u8(buf, offset++);
        // checks
        if (nop < 1 || nop > 64 || length < 1 || length > 128 || restartpos >= length) {
            throw new IllegalArgumentException("invalid infos");
        }
        for (int i = 0; i < length; i++) { // check order
            if (order[i] >= nop) {
                throw new IllegalArgumentException("invalid order entry");
            }
        }

        // bpm
        bpm = u16(buf, offset); offset += 2;
        if ((satType & HAS_OLDBPM) != 0) {
            bpm = bpm * 125 / 50; // cps -> bpm
        }

        if ((satType & HAS_ARPEGGIOLIST) != 0) {
            initSpecialarp();
            for (int i = 0; i < 256; i++) { // arpeggio list
                arplist[i] = u8(buf, offset++);
            }
            for (int i = 0; i < 256; i++) { // arpeggio commands
                arpcmd[i] = u8(buf, offset++);
            }
        }

        for (int i = 0; i < 64; i++) { // track orders
            for (int j = 0; j < 9; j++) {
                if ((satType & HAS_TRACKORDER) != 0) {
                    trackord[i][j] = u8(buf, offset++);
                } else {
                    trackord[i][j] = i * 9 + j; // + 1 ???
                }
            }
        }

        if ((satType & HAS_ACTIVECHANNELS) != 0) {
            activechan = u16(buf, offset) << 16; // active channels
            offset += 2;
        }

logger.log(Level.DEBUG, String.format("satType = %x, nop = %d, length = %d, restartpos = %d, activechan = %x, bpm = %d",
        satType, nop, length, restartpos, activechan, bpm));

        // track data
        if ((satType & HAS_OLDPATTERNS) != 0) {
            int i = 0;
            while (i < 64 * 9 && offset < buf.length) {
                for (int j = 0; j < 64; j++) {
                    for (int k = 0; k < 9; k++) {
                        int b = u8(buf, offset++);
                        tracks[i + k][j].note = b != 0 ? (b + notedis) : 0;
                        tracks[i + k][j].inst = u8(buf, offset++);
                        tracks[i + k][j].command = convfx[u8(buf, offset++) & 0xf];
                        tracks[i + k][j].param1 = u8(buf, offset++);
                        tracks[i + k][j].param2 = u8(buf, offset++);
                    }
                }
                i += 9;
            }
        } else if ((satType & HAS_V7PATTERNS) != 0) {
            int i = 0;
            while (i < 64 * 9 && offset < buf.length) {
                for (int j = 0; j < 64; j++) {
                    for (int k = 0; k < 9; k++) {
                        int b = u8(buf, offset++);
                        tracks[i + k][j].note = b >> 1;
                        tracks[i + k][j].inst = (b & 1) << 4;
                        b = u8(buf, offset++);
                        tracks[i + k][j].inst += b >> 4;
                        tracks[i + k][j].command = convfx[b & 0x0f];
                        b = u8(buf, offset++);
                        tracks[i + k][j].param1 = b >> 4;
                        tracks[i + k][j].param2 = b & 0x0f;
                    }
                }
                i += 9;
            }
        } else {
            int i = 0;
            while (i < 64 * 9 && offset < buf.length) {
                for (int j = 0; j < 64; j++) {
                    int b = u8(buf, offset++);
                    tracks[i][j].note = b >> 1;
                    tracks[i][j].inst = (b & 1) << 4;
                    b = u8(buf, offset++);
                    tracks[i][j].inst += b >> 4;
                    tracks[i][j].command = convfx[b & 0x0f];
                    b = u8(buf, offset++);
                    tracks[i][j].param1 = b >> 4;
                    tracks[i][j].param2 = b & 0x0f;
                }
                i++;
            }
        }

        rewind(0); // rewind module
    }

    /** unsigned byte, 0 past EOF (adplug's binistream reads 0 after EOF) */
    private static int u8(byte[] b, int off) {
        return off < b.length ? b[off] & 0xff : 0;
    }

    /** little-endian unsigned 16-bit, 0 past EOF */
    private static int u16(byte[] b, int off) {
        return u8(b, off) | (u8(b, off + 1) << 8);
    }

    @Override
    public float getRefresh() {
        return (float) (tempo / 2.5);
    }
}
