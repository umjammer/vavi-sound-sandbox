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
 * AdLib MSCplay Player.
 * Ported from adplug's msc.cpp / msc.h by Lubomir Bulej.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class MscPlayer extends Opl3Player {

    private static final Logger logger = getLogger(MscPlayer.class.getName());

    /** "Ceres \x13 MSCplay " */
    private static final byte[] SIGNATURE = {
        'C', 'e', 'r', 'e', 's', ' ', 0x13, ' ',
        'M', 'S', 'C', 'p', 'l', 'a', 'y', ' '
    };

    // file data
    private int version;
    private int nrBlocks;
    private int blockLen;
    private int timerDiv;
    /** compressed music data */
    private byte[][] mscData;

    // decoder state
    private int blockNum;
    private int blockPos;
    private int rawPos;
    private byte[] rawData;

    /** prefix / state */
    private int decPrefix;
    /** prefix distance */
    private int decDist;
    /** prefix length */
    private int decLen;

    // player state
    private int delay;
    private long playPos;

    @Override
    public Type getType() {
        return new Opl3FileFormatType("AdLib MSCplay", "msc");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MSC");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(18);
            byte[] hdr = new byte[18];
            if (bitStream.read(hdr) < 18) return false;
            for (int i = 0; i < SIGNATURE.length; i++) {
                if (hdr[i] != SIGNATURE[i]) return false;
            }
            int ver = (hdr[16] & 0xff) | ((hdr[17] & 0xff) << 8);
            return ver == 0;
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
        // header: sign(16) + ver(2) + desc(64) + timer(2) + nr_blocks(2) + block_len(2)
        if (buf.length < 88) {
            throw new IllegalArgumentException("file too short");
        }
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (buf[i] != SIGNATURE[i]) {
                throw new IllegalArgumentException("invalid signature");
            }
        }
        version = u16(buf, 16);
        if (version != 0) {
            throw new IllegalArgumentException("unsupported version: " + version);
        }
        timerDiv = u16(buf, 82);
        nrBlocks = u16(buf, 84);
        blockLen = u16(buf, 86);

        if (nrBlocks == 0) {
            throw new IllegalArgumentException("no music blocks");
        }

        // load compressed data blocks
        mscData = new byte[nrBlocks][];
        rawData = new byte[blockLen];

        int offset = 88;
        for (int blkNum = 0; blkNum < nrBlocks; blkNum++) {
            if (offset + 2 > buf.length) {
                throw new IllegalArgumentException("truncated block table");
            }
            int len = u16(buf, offset);
            offset += 2;
            mscData[blkNum] = new byte[len];
            for (int i = 0; i < len; i++) {
                mscData[blkNum][i] = offset < buf.length ? buf[offset] : 0;
                offset++;
            }
        }

logger.log(Level.DEBUG, "blocks: " + nrBlocks + ", blockLen: " + blockLen + ", timerDiv: " + timerDiv);

        rewind(0);
    }

    @Override
    public boolean update() {
        // output data
        while (delay == 0) {
            int cmnd = decodeOctet();
            if (cmnd < 0) {
                return false;
            }

            int data = decodeOctet();
            if (data < 0) {
                return false;
            }

            // check for special commands
            if (cmnd == 0xff) {
                // delay (u8 arithmetic: data == 0 wraps to 0)
                delay = (1 + ((data - 1) & 0xff)) & 0xff;
            } else {
                // play command & data
                write(0, cmnd, data);
            }
        }

        // count delays
        if (delay != 0) {
            delay--;
        }

        // advance player position
        playPos++;
        return true;
    }

    @Override
    public void rewind(int subSong) {
        // reset state
        decPrefix = 0;
        blockNum = 0;
        blockPos = 0;
        playPos = 0;
        rawPos = 0;
        delay = 0;

        // init the OPL chip and go to OPL2 mode
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        write(0, 1, 32);
    }

    @Override
    public float getRefresh() {
        // PC timer oscillator frequency / wait register
        return 1193180.0f / (timerDiv != 0 ? timerDiv : 0xffff);
    }

    /** decodes the next octet from the compressed stream, -1 on end of data */
    private int decodeOctet() {
        if (blockNum >= nrBlocks) {
            return -1;
        }

        byte[] blk = mscData[blockNum];
        int lenCorr = 0; // length correction
        while (true) {
            int octet; // decoded octet

            // advance to next block if necessary
            if (blockPos >= blk.length && decLen == 0) {
                blockNum++;
                if (blockNum >= nrBlocks) {
                    return -1;
                }

                blk = mscData[blockNum];
                blockPos = 0;
                rawPos = 0;
            }

            // decode the compressed music data
            switch (decPrefix) {

            // decode prefix
            case 155:
            case 175:
                octet = blk[blockPos++] & 0xff;
                if (octet == 0) {
                    // invalid prefix, output original
                    octet = decPrefix;
                    decPrefix = 0;
                    break;
                }

                // isolate length and distance
                decLen = octet & 0x0F;
                lenCorr = 2;

                decDist = (octet & 0xF0) >> 4;
                if (decPrefix == 155) {
                    decDist++;
                }

                // next decode step for respective prefix type
                decPrefix++;
                continue;

            // check for extended length
            case 156:
                if (decLen == 15) {
                    decLen += blk[blockPos++] & 0xff;
                }

                // add length correction and go for copy mode
                decLen += lenCorr;
                decPrefix = 255;
                continue;

            // get extended distance
            case 176:
                decDist += 17 + 16 * (blk[blockPos++] & 0xff);
                lenCorr = 3;

                // check for extended length
                decPrefix = 156;
                continue;

            // prefix copy mode
            case 255:
                if (rawPos >= decDist) {
                    octet = rawData[rawPos - decDist] & 0xff;
                } else {
logger.log(Level.DEBUG, "error! read before raw_data buffer.");
                    octet = 0;
                }

                decLen--;
                if (decLen == 0) {
                    // back to normal mode
                    decPrefix = 0;
                }
                break;

            // normal mode
            default:
                octet = blk[blockPos++] & 0xff;
                if (octet == 155 || octet == 175) {
                    // it's a prefix, restart
                    decPrefix = octet;
                    continue;
                }
            }

            // output the octet
            rawData[rawPos++] = (byte) octet;
            return octet;
        }
    }

    /** little-endian unsigned 16-bit */
    private static int u16(byte[] b, int off) {
        return (b[off] & 0xff) | ((b[off + 1] & 0xff) << 8);
    }
}
