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


/**
 * DOSBox Raw OPL Player (DRO).
 *
 * @author Sjoerd van der Berg <harekiet@zophar.net>
 * @author matthew gambrell <zeromus@zeromus.org>
 * @author Laurence Dougal Myers <jestarjokin@jestarjokin.net>
 * @author Wraithverge <liam82067@yahoo.com>
 * @version
 * <li> matthew gambrell - upgraded
 * <li> Laurence Dougal Myers - Refactored to better match dro2.cpp 
 * <li> 2012 - 2017 Wraithverge - Added Member pointers.
 * <li> 2012 - 2017 Wraithverge - Fixed incorrect operator.
 * <li> 2012 - 2017 Wraithverge - Finalized support for displaying arbitrary Tag data.
 * <li> 3-oct-04: the DRO format is not yet finalized. beware.
 * <li> 10-jun-12: the DRO 1 format is finalized, but capturing is buggy.
 */
class DroPlayer extends Opl3Player {

    private static final Logger logger = Logger.getLogger(DroPlayer.class.getName());

    protected static final String ID = "DBRAWOPL";

    private LittleEndianDataInputStream data;
    private int pos;

    private int length;
    private int mstotal;
    private int delay;
    private int opl3_mode = 0;

    @SuppressWarnings("unused")
    private int total = 0;
    private int currChip = 0;

    private static final int MARK_SIZE = 11;

    @Override
    public boolean matchFormat(InputStream bitStream) {
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(bitStream);
        try {
            dis.mark(MARK_SIZE);

            byte[] id = new byte[8];
            dis.readFully(id);
            if (!Arrays.equals(ID.getBytes(), id)) {
                return false;
            }

            dis.skipBytes(2);
            int v = dis.readUnsignedByte();
            if (v != 1) {
                return false;
            }

            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        } finally {
            try {
                dis.reset();
            } catch (IOException e) {
                logger.fine(e.toString());
            }
        }
    }

    @Override
    public int getTotalMilliseconds() {
        return mstotal;
    }

    @Override
    public void load(InputStream is) throws IOException {
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(is);

        dis.skipBytes(8); // id
        dis.skipBytes(4); // version

        mstotal = dis.readInt();
        length = dis.readInt();
        opl3_mode = dis.readUnsignedByte();
        dis.mark(3);
        byte[] zero = new byte[3];
        zero[0] = dis.readByte();
        zero[1] = dis.readByte();
        zero[2] = dis.readByte();
        if (zero[0] != 0 || zero[1] != 0 || zero[2] != 0) {
            // need these three bytes!
            dis.reset();
logger.fine("not zero: " + Arrays.toString(zero));
        }

logger.fine("id: " +  ID);
logger.fine("version: " + 1);
logger.fine("mstotal: " + mstotal);
logger.fine("length: " + length);
logger.fine("oplType: " + opl3_mode);

        data = dis;

        length -= 24;
        total = 0;

        rewind(0);
        if (opl3_mode == 1 || opl3_mode == 2) {
            write(1, 5, 1);
        }
    }

    @Override
    public boolean update() throws IOException {
        if (delay > 500) {
            delay -= 500;
            return true;
        } else {
            delay = 0;

            while (pos < length) {
                int iIndex = data.readUnsignedByte();
                ++pos;
                switch (iIndex) {
                case 0:
                    if (pos >= length) return false;
                    delay = 1 + data.readUnsignedByte();
                    ++pos;
                    return true;
                case 1:
                    if (pos + 1 >= length) return false;
                    delay = 1 + data.readUnsignedShort();
                    pos += 2;
                    return true;
                case 2:
                    currChip = 0;
                    break;
                case 3:
                    currChip = 1;
                    break;
                default:
                    if (iIndex == 4) {
                        if (pos >= length) return false;
                        iIndex = data.readUnsignedByte();
                        ++pos;
                    }

                    if (pos >= length) return false;
                    int v = data.readUnsignedByte();
logger.fine(String.format("%d, %d, %d, %02x", opl3_mode, currChip, iIndex, v));
                    ++pos;
                    if (opl3_mode == 0) {
                        write(0, iIndex, v);
                    } else {
                        write(currChip, iIndex, v);
                    }
                    total += currChip + iIndex + v;
                    break;
                }
            }
            return pos < length;
        }
    }

    @Override
    public void rewind(int subSong) {
        delay = 1;
        pos = 0;

        for (int i = 0; i < 256; ++i) {
            write(currChip, i, 0);
        }

        currChip = 1;

        for (int i = 0; i < 256; ++i) {
            write(currChip, i, 0);
        }

        currChip = 0;
        total = 0;
    }

    @Override
    public float getRefresh() {
        return delay > 500 ? 2.0F : 1000.0F / delay;
    }
}