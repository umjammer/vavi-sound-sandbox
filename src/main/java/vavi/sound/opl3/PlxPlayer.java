/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2024 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * PALLADIX Sound System Player.
 * Ported from adplug's plx.cpp / plx.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class PlxPlayer extends Opl3Player {

    private static final Logger logger = getLogger(PlxPlayer.class.getName());

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    /**
     * These frequencies are written directly into the low (A0h) and high
     * byte (B0h) AdLib frequency registers. They include the frequency block.
     */
    private static final int[] frequency = {
        343, 363, 385, 408, 432, 458, 485, 514, 544, 577, 611, 647,
        1367, 1387, 1409, 1432, 1456, 1482, 1509, 1538, 1568, 1601, 1635, 1671,
        2391, 2411, 2433, 2456, 2480, 2506, 2533, 2562, 2592, 2625, 2659, 2695,
        3415, 3435, 3457, 3480, 3504, 3530, 3557, 3586, 3616, 3649, 3683, 3719,
        4439, 4459, 4481, 4504, 4528, 4554, 4581, 4610, 4640, 4673, 4707, 4743,
        5463, 5483, 5505, 5528, 5552, 5578, 5605, 5634, 5664, 5697, 5731, 5767,
        6487, 6507, 6529, 6552, 6576, 6602, 6629, 6658, 6688, 6721, 6755, 6791,
        7511, 7531, 7553, 7576, 7600, 7626, 7653, 7682, 7712, 7745, 7779, 7815
    };

    private static final int[] opl2_init_regs = {
        0xff, 0x20, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0x40, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0xff, 0xff, 0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0xff, 0xff,
        0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0x3f, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xff, 0xff, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xff, 0xff,
        0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xff, 0xff, 0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xff, 0xff,
        0xee, 0xee, 0xee, 0xee, 0xee, 0xee, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x57, 0x02, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x09, 0x0e, 0xff, 0xff, 0xff, 0xff, 0x00, 0xff, 0xff,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff
    };

    private final int[] fmchip = new int[0x100];
    private byte[] songdata;
    private int speed;
    private int speedScale;
    private final int[] chanVolume = new int[9];
    private final int[] chanStartOffset = new int[9];
    private final int[] chanOffset = new int[9];
    private final int[] chanPos = new int[9];
    private int songpos;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("PALLADIX Sound System", "plx");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("PLX");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(4);
            byte[] id = new byte[4];
            if (bitStream.read(id) < 4) return false;
            return id[0] == 'P' && id[1] == 'L' && id[2] == 'X' && id[3] == 0;
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
        // header: id(3) + type(1) + speed_scale(1) + speed(2) + chan_start_offset(9 * 2)
        if (buf.length < 25) {
            throw new IllegalArgumentException("file too short");
        }
        if (buf[0] != 'P' || buf[1] != 'L' || buf[2] != 'X' || buf[3] != 0) {
            throw new IllegalArgumentException("invalid signature");
        }

        speedScale = buf[4] & 0xff;
        speed = u16(buf, 5);

        // starting file offsets for all 9 channels
        for (int i = 0; i < 9; i++) {
            chanStartOffset[i] = u16(buf, 7 + i * 2);
        }

        if (speedScale == 0) {
logger.log(Level.DEBUG, "Detected speed_scale==0, adjust to 1 to avoid division by zero");
            speedScale = 1;
        }
        if (speed == 0) {
logger.log(Level.DEBUG, "Detected speed==0, adjust to 1 to avoid division by zero");
            speed = 1;
        }

        // the song is replayed directly out of the file image
        songdata = buf;

        rewind(0);
    }

    @Override
    public boolean update() {
        boolean endsong = false;

        // for all channels
        for (int i = 0; i < 9; i++) {
            // only if chan_offset was non-zero for this channel
            if (chanOffset[i] == 0) {
                continue;
            }

            // if we haven't reached the channel's pattern position, we skip
            if (songpos < chanPos[i]) {
                continue;
            }

            // read next flags
            int pos = chanOffset[i];
            int flags = u8(pos++);

            if (flags != 0x80) { // if bit 7 is set, we skip
                if (flags == 0) { // reset channel / song end
                    chanOffset[i] = chanStartOffset[i];
                    setregs(0xb0 + i, fmchip[0xb0 + i] & ~(1 << 5)); // key off
                    endsong = true;
                    continue;
                }

                if ((flags & (1 << 0)) != 0) { // set instrument
                    int instOff = u16s(pos); pos += 2;

                    int ip = instOff + 1; // skip 1 byte
                    int feedbackAlg = u8(ip++);

                    // set modulator
                    setregs(0x20 + op_table[i], u8(ip++));
                    setregs(0x40 + op_table[i], u8(ip++));
                    setregs(0x60 + op_table[i], u8(ip++));
                    setregs(0x80 + op_table[i], u8(ip++));
                    setregs(0xe0 + op_table[i], u8(ip++));

                    // set feedback & algorithm
                    setregs(0xc0 + i, feedbackAlg);

                    // set carrier
                    setregs(0x23 + op_table[i], u8(ip++));
                    chanVolume[i] = u8(ip++); setregs(0x43 + op_table[i], chanVolume[i]);
                    setregs(0x63 + op_table[i], u8(ip++));
                    setregs(0x83 + op_table[i], u8(ip++));
                    setregs(0xe3 + op_table[i], u8(ip));
                }

                if ((flags & (1 << 1)) != 0) { // set volume
                    chanVolume[i] = u8(pos++);
                    setregs(0x43 + op_table[i], chanVolume[i]);
                }

                if ((flags & (1 << 2)) != 0) { // key off
                    if ((fmchip[0xb0 + i] & (1 << 5)) != 0) { // if key on is currently set
                        setregs(0xb0 + i, fmchip[0xb0 + i] & ~(1 << 5)); // mask out "key on" bit
                    }
                }

                if ((flags & (7 << 3)) != 0) { // make any sound?
                    int freq = (fmchip[0xb0 + i] << 8) | fmchip[0xa0 + i];

                    if ((flags & (1 << 3)) != 0) { // set note
                        int note = u8(pos++) / 2; // must be divisible by 2
                        freq = frequency[note % 96];
                    }

                    if ((flags & (1 << 4)) != 0) { // set frequency
                        freq = u16s(pos); pos += 2;
                    }

                    if ((flags & (1 << 5)) != 0) { // key on
                        freq |= 1 << 13;
                    }

                    setregs(0xa0 + i, freq & 0xff); // set frequency low byte
                    setregs(0xb0 + i, (freq >> 8) & 0xff); // set frequency high byte
                }

                if ((flags & (1 << 6)) != 0) { // set global tempo
                    speed = u16s(pos); pos += 2;
                    if (speed == 0) {
logger.log(Level.DEBUG, "Detected speed==0, adjust to 1 to avoid division by zero");
                        speed = 1;
                    }
                }
            }

            // how many pattern positions to skip?
            chanPos[i] += u8(pos++);
            chanOffset[i] = pos;
        }

        songpos++;
        return !endsong;
    }

    @Override
    public void rewind(int subSong) {
        // reset OPL chip and init OPL2 registers
        for (int i = 0; i < 0x100; i++) {
            fmchip[i] = opl2_init_regs[i];
            write(0, i, opl2_init_regs[i]);
        }

        // reset channels
        for (int i = 0; i < 9; i++) {
            chanOffset[i] = chanStartOffset[i];
            chanPos[i] = 0;
        }

        songpos = 0;
    }

    @Override
    public float getRefresh() {
        return 1193182.0f / (speedScale * speed);
    }

    // ----

    private void setregs(int reg, int val) {
        reg &= 0xff;
        val &= 0xff;
        if (fmchip[reg] == val) return;

        fmchip[reg] = val;
        write(0, reg, val);
    }

    /** unsigned byte from song data (0 when out of bounds) */
    private int u8(int off) {
        return off < songdata.length ? songdata[off] & 0xff : 0;
    }

    /** little-endian unsigned 16-bit from song data */
    private int u16s(int off) {
        return u8(off) | (u8(off + 1) << 8);
    }

    /** little-endian unsigned 16-bit */
    private static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }
}
