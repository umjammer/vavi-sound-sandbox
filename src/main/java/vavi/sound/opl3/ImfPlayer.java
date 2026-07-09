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
import java.util.Map;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

import static java.lang.System.getLogger;


/**
 * IMF Player.
 *
 * @author Simon Peter <dn.tlp@gmx.net>
 */
public class ImfPlayer extends Opl3Player {

    private static final Logger logger = getLogger(ImfPlayer.class.getName());

    private String track_name, game_name, author_name, remarks;
    private int size;
    private Sdata[] data;
    private String footer;
    private float rate = 700.0f;
    private int pos;
    private boolean songend;
    private int del;
    private float refreshRate = 700.0f;

    private static class Sdata {
        int reg, val;
        int time;
    }

    public ImfPlayer() {
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("IMF File Format", "imf");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("IMF");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            java.net.URI uri = vavi.sound.SoundUtil.getSource(bitStream);
            if (uri != null) {
                String path = uri.getPath();
                if (path != null && (path.toLowerCase().endsWith(".imf") || path.toLowerCase().endsWith(".wlf"))) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        try {
            bitStream.mark(6);
            byte[] header = new byte[6];
            int r = bitStream.read(header);
            if (r == 6 && new String(header, 0, 5).equals("ADLIB") && header[5] == 1) {
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
        int mfsize = 0;
        int off = 0;

        if (buf.length >= 6 && new String(buf, 0, 5).equals("ADLIB") && buf[5] == 1) {
            off = 6;
            track_name = readString(buf, off);
            off += track_name.length() + 1;
            game_name = readString(buf, off);
            off += game_name.length() + 1;
            off++; // br.ReadByte()
            mfsize = off + 2;
        } else {
            off = 0;
        }

        int fsize;
        if (mfsize > 0) {
            fsize = (buf[off] & 0xff) | ((buf[off + 1] & 0xff) << 8) | ((buf[off + 2] & 0xff) << 16) | ((buf[off + 3] & 0xff) << 24);
            off += 4;
        } else {
            fsize = (buf[off] & 0xff) | ((buf[off + 1] & 0xff) << 8);
            off += 2;
        }
        int flsize = buf.length;
        if (fsize == 0) {
            if (mfsize != 0) {
                off -= 4;
            } else {
                off -= 2;
            }
            size = (flsize - mfsize) / 4;
        } else {
            size = fsize / 4;
        }

        data = new Sdata[size];
        for (int i = 0; i < size; i++) {
            data[i] = new Sdata();
            data[i].reg = buf[off] & 0xff;
            data[i].val = buf[off + 1] & 0xff;
            data[i].time = (buf[off + 2] & 0xff) | ((buf[off + 3] & 0xff) << 8);
            off += 4;
        }

        if (fsize != 0 && (fsize < flsize - 2 - mfsize)) {
            if (off < buf.length && buf[off] == 0x1a) {
                off++;
                track_name = readString(buf, off);
                off += track_name.length() + 1;
                author_name = readString(buf, off);
                off += author_name.length() + 1;
                remarks = readString(buf, off);
            } else {
                int footerlen = flsize - fsize - 2 - mfsize;
                footer = readString(buf, off, footerlen);
            }
        }

        String path = "";
        if (props.containsKey("uri")) {
            path = props.get("uri").toString();
        }
        float rate = 700.0f;
        if (path.toLowerCase().endsWith(".imf")) {
            rate = 560.0f;
        } else if (path.toLowerCase().endsWith(".wlf")) {
            rate = 700.0f;
        }
        this.rate = rate;

        rewind(0);
    }

    @Override
    public boolean update() {
        if (pos >= size) {
            pos = 0;
            songend = true;
            return false;
        }
        do {
            write(0, data[pos].reg, data[pos].val);
            del = data[pos].time;
            pos++;
        } while (del == 0 && pos < size);

        if (pos >= size) {
            pos = 0;
            songend = true;
        } else {
            refreshRate = rate / del;
        }

        return !songend;
    }

    @Override
    public void rewind(int subSong) {
        pos = 0;
        del = 0;
        refreshRate = rate;
        songend = false;
        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
            write(1, i, 0);
        }
        write(0, 1, 32);
    }

    @Override
    public float getRefresh() {
        return refreshRate;
    }

    private static String readString(byte[] buf, int off) {
        StringBuilder sb = new StringBuilder();
        while (off < buf.length && buf[off] != 0) {
            sb.append((char) buf[off]);
            off++;
        }
        return sb.toString();
    }

    private static String readString(byte[] buf, int off, int maxLen) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (off < buf.length && buf[off] != 0 && i < maxLen) {
            sb.append((char) buf[off]);
            off++;
            i++;
        }
        return sb.toString();
    }
}
