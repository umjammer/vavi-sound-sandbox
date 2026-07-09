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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * MKJamz Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class MkjPlayer extends Opl3Player {

    private static final Logger logger = getLogger(MkjPlayer.class.getName());

    private static final int[] op_table = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0a, 0x10, 0x11, 0x12
    };

    private short maxchannel, maxnotes;
    private short[] songbuf;
    private boolean songend;

    private static class Channel {
        short defined, songptr, octave, waveform, pstat, speed, delay;
    }
    private final Channel[] channel = new Channel[9];

    public MkjPlayer() {
        for (int i = 0; i < 9; i++) {
            channel[i] = new Channel();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("MKJamz File Format", "mkj");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MKJ");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && path.toLowerCase().endsWith(".mkj")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            bitStream.mark(6);
            byte[] header = new byte[6];
            int r = bitStream.read(header);
            if (r == 6 && new String(header, 0, 6).equals("MKJamz")) {
                return true;
            }
        } catch (IOException ignored) {
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        int off = 0;

        String id = new String(buf, off, 6);
        off += 6;
        if (!id.equals("MKJamz")) {
            throw new IOException("invalid magic signature: " + id);
        }

        float ver = ByteBuffer.wrap(buf, off, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        off += 4;
        if (ver > 1.12f) {
            throw new IOException("invalid version: " + ver);
        }

        maxchannel = s16(buf, off); off += 2;
        write(0, 1, 32);

        short[] inst = new short[8];
        for (int i = 0; i < maxchannel; i++) {
            for (int j = 0; j < 8; j++) {
                inst[j] = s16(buf, off); off += 2;
            }
            write(0, 0x20 + op_table[i], inst[4] & 0xff);
            write(0, 0x23 + op_table[i], inst[0] & 0xff);
            write(0, 0x40 + op_table[i], inst[5] & 0xff);
            write(0, 0x43 + op_table[i], inst[1] & 0xff);
            write(0, 0x60 + op_table[i], inst[6] & 0xff);
            write(0, 0x63 + op_table[i], inst[2] & 0xff);
            write(0, 0x80 + op_table[i], inst[7] & 0xff);
            write(0, 0x83 + op_table[i], inst[3] & 0xff);
        }

        maxnotes = s16(buf, off); off += 2;
        songbuf = new short[(maxchannel + 1) * maxnotes];
        for (int i = 0; i < maxchannel; i++) {
            channel[i].defined = s16(buf, off); off += 2;
        }
        for (int i = 0; i < (maxchannel + 1) * maxnotes; i++) {
            songbuf[i] = s16(buf, off); off += 2;
        }

        rewind(0);
    }

    @Override
    public boolean update() {
        for (int c = 0; c < maxchannel; c++) {
            if (channel[c].defined == 0) {
                continue;
            }

            if (channel[c].pstat != 0) {
                channel[c].pstat--;
                continue;
            }

            write(0, 0xb0 + c, 0); // key off
            do {
                int note = songbuf[channel[c].songptr] & 0xffff;
                if (channel[c].songptr - c > maxchannel) {
                    if (note != 0 && note < 250) {
                        channel[c].pstat = channel[c].speed;
                    }
                }
                switch (note) {
                    case 68: write(0, 0xa0 + c, 0x81); write(0, 0xb0 + c, 0x21 + 4 * channel[c].octave); break;
                    case 69: write(0, 0xa0 + c, 0xb0); write(0, 0xb0 + c, 0x21 + 4 * channel[c].octave); break;
                    case 70: write(0, 0xa0 + c, 0xca); write(0, 0xb0 + c, 0x21 + 4 * channel[c].octave); break;
                    case 71: write(0, 0xa0 + c, 0x2);  write(0, 0xb0 + c, 0x22 + 4 * channel[c].octave); break;
                    case 65: write(0, 0xa0 + c, 0x41); write(0, 0xb0 + c, 0x22 + 4 * channel[c].octave); break;
                    case 66: write(0, 0xa0 + c, 0x87); write(0, 0xb0 + c, 0x22 + 4 * channel[c].octave); break;
                    case 67: write(0, 0xa0 + c, 0xae); write(0, 0xb0 + c, 0x22 + 4 * channel[c].octave); break;
                    case 17: write(0, 0xa0 + c, 0x6b); write(0, 0xb0 + c, 0x21 + 4 * channel[c].octave); break;
                    case 18: write(0, 0xa0 + c, 0x98); write(0, 0xb0 + c, 0x21 + 4 * channel[c].octave); break;
                    case 20: write(0, 0xa0 + c, 0xe5); write(0, 0xb0 + c, 0x21 + 4 * channel[c].octave); break;
                    case 21: write(0, 0xa0 + c, 0x20); write(0, 0xb0 + c, 0x22 + 4 * channel[c].octave); break;
                    case 15: write(0, 0xa0 + c, 0x63); write(0, 0xb0 + c, 0x22 + 4 * channel[c].octave); break;
                    case 255:
                        channel[c].songptr += maxchannel;
                        channel[c].pstat = songbuf[channel[c].songptr];
                        break;
                    case 254:
                        channel[c].songptr += maxchannel;
                        channel[c].octave = songbuf[channel[c].songptr];
                        break;
                    case 253:
                        channel[c].songptr += maxchannel;
                        channel[c].speed = songbuf[channel[c].songptr];
                        break;
                    case 252:
                        channel[c].songptr += maxchannel;
                        channel[c].waveform = (short) (songbuf[channel[c].songptr] - 300);
                        if (c > 2) {
                            write(0, 0xe0 + c + (c + 6), channel[c].waveform & 0xff);
                        } else {
                            write(0, 0xe0 + c, channel[c].waveform & 0xff);
                        }
                        break;
                    case 251:
                        for (int i = 0; i < maxchannel; i++) {
                            channel[i].songptr = (short) i;
                        }
                        songend = true;
                        return false;
                }

                if (channel[c].songptr - c < maxnotes) {
                    channel[c].songptr += maxchannel;
                } else {
                    channel[c].songptr = (short) c;
                }
            } while (channel[c].pstat == 0);
        }

        return !songend;
    }

    @Override
    public void rewind(int subsong) {
        for (int i = 0; i < maxchannel; i++) {
            channel[i].pstat = 0;
            channel[i].speed = 0;
            channel[i].waveform = 0;
            channel[i].songptr = (short) i;
            channel[i].octave = 4;
        }
        songend = false;
    }

    @Override
    public float getRefresh() {
        return 100.0f;
    }

    private static short s16(byte[] b, int off) {
        return (short) ((b[off] & 0xff) | ((b[off + 1] & 0xff) << 8));
    }
}
