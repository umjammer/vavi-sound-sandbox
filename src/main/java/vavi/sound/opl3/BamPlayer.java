/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2002 Simon Peter, <dn.tlp@gmx.net>, et al.
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

import vavi.io.LittleEndianDataInputStream;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * Bob's Adlib Music Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class BamPlayer extends Opl3Player {

    private static final Logger logger = getLogger(BamPlayer.class.getName());

    private static final String ID = "CBMF";

    private static final int[] freq = {
        172, 182, 193, 205, 217, 230, 243, 258, 274,
        290, 307, 326, 345, 365, 387, 410, 435, 460, 489, 517, 547, 580, 614, 651, 1369, 1389, 1411,
        1434, 1459, 1484, 1513, 1541, 1571, 1604, 1638, 1675, 2393, 2413, 2435, 2458, 2483, 2508,
        2537, 2565, 2595, 2628, 2662, 2699, 3417, 3437, 3459, 3482, 3507, 3532, 3561, 3589, 3619,
        3652, 3686, 3723, 4441, 4461, 4483, 4506, 4531, 4556, 4585, 4613, 4643, 4676, 4710, 4747,
        5465, 5485, 5507, 5530, 5555, 5580, 5609, 5637, 5667, 5700, 5734, 5771, 6489, 6509, 6531,
        6554, 6579, 6604, 6633, 6661, 6691, 6724, 6758, 6795, 7513, 7533, 7555, 7578, 7603, 7628,
        7657, 7685, 7715, 7748, 7782, 7819, 7858, 7898, 7942, 7988, 8037, 8089, 8143, 8191, 8191,
        8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191, 8191
    };

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private byte[] song;
    private int del;
    private int pos, size, gosub;
    private boolean songend, chorus;

    private static class Label {
        long target;
        boolean defined;
        int count;
    }

    private final Label[] label = new Label[16];

    public BamPlayer() {
        for (int i = 0; i < 16; i++) {
            label[i] = new Label();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("BAM File Format", "bam");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("BAM");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bitStream);
        try {
            dis.mark(4);

            byte[] id = new byte[4];
            dis.readFully(id);
            return ID.equalsIgnoreCase(new String(id));
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        } finally {
            try {
                dis.reset();
            } catch (IOException e) {
                logger.log(Level.DEBUG, e.toString());
            }
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (buf.length < 4) {
            throw new IOException("invalid format");
        }
        String id = new String(buf, 0, 4);
        if (!ID.equalsIgnoreCase(id)) {
            throw new IOException("invalid format: " + id);
        }

        size = buf.length - 4;
        song = new byte[size];
        System.arraycopy(buf, 4, song, 0, size);

        rewind(0);
    }

    @Override
    public boolean update() {
        if (del != 0) {
            del--;
            return !songend;
        }

        if (pos >= size) { // EOF detection
            pos = 0;
            songend = true;
        }

        while (pos < size) {
            int songVal = song[pos] & 0xff;
            if (songVal >= 128) {
                break;
            }
            int cmd = songVal & 240;
            int c = songVal & 15;
            switch (cmd) {
                case 0: // stop song
                    pos = 0;
                    songend = true;
                    break;
                case 16: // start note
                    if (c < 9) {
                        write(0, 0xa0 + c, freq[song[++pos] & 0xff] & 255);
                        write(0, 0xb0 + c, (freq[song[pos] & 0xff] >> 8) + 32);
                    } else {
                        pos++;
                    }
                    pos++;
                    break;
                case 32: // stop note
                    if (c < 9) {
                        write(0, 0xb0 + c, 0);
                    }
                    pos++;
                    break;
                case 48: // define instrument
                    if (c < 9) {
                        write(0, 0x20 + op_table[c], song[pos + 1] & 0xff);
                        write(0, 0x23 + op_table[c], song[pos + 2] & 0xff);
                        write(0, 0x40 + op_table[c], song[pos + 3] & 0xff);
                        write(0, 0x43 + op_table[c], song[pos + 4] & 0xff);
                        write(0, 0x60 + op_table[c], song[pos + 5] & 0xff);
                        write(0, 0x63 + op_table[c], song[pos + 6] & 0xff);
                        write(0, 0x80 + op_table[c], song[pos + 7] & 0xff);
                        write(0, 0x83 + op_table[c], song[pos + 8] & 0xff);
                        write(0, 0xe0 + op_table[c], song[pos + 9] & 0xff);
                        write(0, 0xe3 + op_table[c], song[pos + 10] & 0xff);
                        write(0, 0xc0 + c, song[pos + 11] & 0xff);
                    }
                    pos += 12;
                    break;
                case 80: // set label
                    label[c].target = ++pos;
                    label[c].defined = true;
                    break;
                case 96: // jump
                    if (label[c].defined) {
                        int loopVal = song[pos + 1] & 0xff;
                        switch (loopVal) {
                            case 254: // infinite loop
                                // fall through...
                            case 255: // chorus
                                // fall through...
                            case 0: // end of loop
                                if (loopVal == 254 && label[c].defined) {
                                    pos = (int) label[c].target;
                                    songend = true;
                                    break;
                                }
                                if (loopVal == 255 && !chorus && label[c].defined) {
                                    chorus = true;
                                    gosub = pos + 2;
                                    pos = (int) label[c].target;
                                    break;
                                }
                                pos += 2;
                                break;
                            default: // finite loop
                                if (label[c].count == 0) { // loop elapsed
                                    label[c].count = 255;
                                    pos += 2;
                                    break;
                                }
                                if (label[c].count < 255) { // loop defined
                                    label[c].count--;
                                } else { // loop undefined
                                    label[c].count = (song[pos + 1] & 0xff) - 1;
                                }
                                pos = (int) label[c].target;
                                break;
                        }
                    }
                    break;
                case 112: // end of chorus
                    if (chorus) {
                        pos = gosub;
                        chorus = false;
                    } else {
                        pos++;
                    }
                    break;
                default: // reserved command (skip)
                    pos++;
                    break;
            }
        }

        if (pos < size && (song[pos] & 0xff) >= 128) {
            del = (song[pos] & 0xff) - 127;
            pos++;
        }

        return !songend;
    }

    @Override
    public void rewind(int subSong) {
        pos = 0;
        songend = false;
        del = 0;
        gosub = 0;
        chorus = false;
        for (int i = 0; i < 16; i++) {
            label[i].defined = false;
            label[i].target = 0;
            label[i].count = 255;
        }
        label[0].defined = true;

        for (int i = 0; i < 256; ++i) {
            write(0, i, 0);
            write(1, i, 0);
        }
        write(0, 1, 32);
    }

    @Override
    public float getRefresh() {
        return 25.0f;
    }
}
