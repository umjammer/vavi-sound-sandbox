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
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;


/**
 * DOSBox Raw OPL v2.0 player (DRO).
 * <p>
 * <li> 2017 Wraithverge - Finalized support for displaying arbitrary Tag data.
 *
 * @author Adam Nielsen <malvineous@shikadi.net>
 * @author Wraithverge <liam82067@yahoo.com>
 */
class Dro2Player extends Opl3Player {
    private static Logger logger = Logger.getLogger(Dro2Player.class.getName());

    private LittleEndianDataInputStream data;
    private int pos;

    private int length;
    /** length in milliseconds */
    private int msTotal;
    private int delay;
    private int delay256;
    private int delayShift8;
    /** the OPL data */
    private int[] toReg;
    private int opl3Type;
    @SuppressWarnings("unused")
    private int total = 0;

    private static final int MARK_SIZE = 9;

    @Override
    public boolean matchFormat(InputStream bitStream) {
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bitStream);
        try {
            dis.mark(MARK_SIZE);

            byte[] id = new byte[8];
            dis.readFully(id);
            if (!Arrays.equals(DroPlayer.ID.getBytes(), id)) {
                return false;
            }

            int v = dis.readUnsignedByte();
            if (v != 2) {
                return false;
            }

            return true;
        } catch (IOException e) {
Debug.println(Level.WARNING, e);
            return false;
        } finally {
            try {
                dis.reset();
            } catch (IOException e) {
Debug.println(Level.SEVERE, e);
            }
        }
    }

    @Override
    public int getTotalMiliseconds() {
        return msTotal;
    }

    @Override
    public void load(InputStream is) throws IOException {
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(is);

        dis.skipBytes(8); // id
        dis.skipBytes(4); // version

        length = dis.readInt() * 2;
        msTotal = dis.readInt();

        opl3Type = dis.readUnsignedByte();
        if (dis.readUnsignedByte() != 0) { // format
            dis.reset();
            throw new IllegalArgumentException();
        }
        if (dis.readUnsignedByte() != 0) { // compression
            dis.reset();
            throw new IllegalArgumentException();
        }

        delay256 = dis.readUnsignedByte();
        delayShift8 = dis.readUnsignedByte();
        int l = dis.readUnsignedByte();
        toReg = new int[l];

        for (int i = 0; i < l; ++i) {
            toReg[i] = dis.readUnsignedByte();
        }

        logger.info("id: " + DroPlayer.ID);
        logger.info("version: " + 2);
        logger.info("length: " + length);
        logger.info("mstotal: " + msTotal);
        logger.info("opl3Type: " + opl3Type);
        logger.info("delay256: " + delay256);
        logger.info("delayShift8: " + delayShift8);

        data = dis;

        // TODO after data, title, author, desc tag

        length -= 26 + l;
        total = 0;

        rewind(0);
        if (opl3Type != 0) {
            write(1, 5, 1);
        }
    }

    @Override
    public boolean update() throws IOException {
        delay = 0;

        while (pos < length) {
            int iIndex = data.readUnsignedByte();
            ++pos;
            if (iIndex == delay256) {
                delay += 1 + data.readUnsignedByte();
                ++pos;
                return true;
            }

            if (iIndex == delayShift8) {
                delay = (1 + data.readUnsignedByte()) << 8;
                ++pos;
                return true;
            }

            int iReg = toReg[iIndex & 127] & 255;
            int i = iIndex >> 7 & 1;
            int iValue = data.readUnsignedByte();
            ++pos;
            write(i, iReg, iValue);
        }

        return pos < length;
    }

    @Override
    public void rewind(int subSong) {
        delay = 1;
        pos = 0;

        for (int i = 0; i < 256; ++i) {
            write(0, i, 0);
            write(1, i, 0);
        }

        total = 0;
    }

    @Override
    public float getRefresh() {
        return 1000.0F / delay;
    }
}