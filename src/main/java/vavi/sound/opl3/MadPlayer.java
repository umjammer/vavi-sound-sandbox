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
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Mlat Adlib Tracker Loader.
 * Ported from adplug's mad.cpp / mad.h by Riven the Mage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class MadPlayer extends ProtrackPlayer {

    private static final byte[] ID = { 'M', 'A', 'D', '+' };

    private final int[][] madInstData = new int[9][12];
    private int timer;

    public MadPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Mlat Adlib Tracker", "mad");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("MAD");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(4);
            byte[] id = new byte[4];
            if (bitStream.read(id) < 4) return false;
            return java.util.Arrays.equals(id, ID);
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
        int[] convInst = { 2, 1, 10, 9, 4, 3, 6, 5, 8, 7 };

        if (buf.length < 4 + 9 * 20 + 1 + 3) {
            throw new IllegalArgumentException("file too short");
        }
        for (int i = 0; i < 4; i++) {
            if (buf[i] != ID[i]) {
                throw new IllegalArgumentException("invalid signature");
            }
        }

        // load instruments (name 8 bytes + data 12 bytes)
        int offset = 4;
        for (int i = 0; i < 9; i++) {
            offset += 8; // skip name
            for (int j = 0; j < 12; j++) {
                madInstData[i][j] = buf[offset++] & 0xff;
            }
        }

        offset++; // ignore 1 byte

        // data for Protracker
        length = buf[offset++] & 0xff;
        nop = buf[offset++] & 0xff;
        timer = buf[offset++] & 0xff;

        // init CmodPlayer
        reallocInstruments(9);
        reallocOrder(length);
        reallocPatterns(nop, 32, 9);
        initTrackord();

        // load tracks
        for (int i = 0; i < nop; i++) {
            for (int k = 0; k < 32; k++) {
                for (int j = 0; j < 9; j++) {
                    int t = i * 9 + j;

                    int event = offset < buf.length ? buf[offset] & 0xff : 0;
                    offset++;

                    if (event < 0x61) {
                        tracks[t][k].note = event;
                    }
                    if (event == 0xFF) { // 0xFF: Release note
                        tracks[t][k].command = 8;
                    }
                    if (event == 0xFE) { // 0xFE: Pattern Break
                        tracks[t][k].command = 13;
                    }
                }
            }
        }

        // load order
        for (int i = 0; i < length; i++) {
            int b = offset < buf.length ? buf[offset] & 0xff : 0;
            offset++;
            order[i] = (b - 1) & 0xff;
        }

        // convert instruments
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 10; j++) {
                inst[i].data[convInst[j]] = madInstData[i][j];
            }
        }

        // data for Protracker
        restartpos = 0;
        initspeed = 1;

        rewind(0);
    }

    @Override
    public void rewind(int subSong) throws IOException {
        super.rewind(subSong);

        // default instruments
        for (int i = 0; i < 9; i++) {
            channel[i].inst = i;

            channel[i].vol1 = 63 - (inst[i].data[10] & 63);
            channel[i].vol2 = 63 - (inst[i].data[9] & 63);
        }
    }

    @Override
    public float getRefresh() {
        return timer;
    }
}
