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
 *
 * dro.c - DOSBox Raw OPL Player by Sjoerd van der Berg <harekiet@zophar.net>
 *
 * upgraded by matthew gambrell <zeromus@zeromus.org>
 * Refactored to better match dro2.cpp 
 *  by Laurence Dougal Myers <jestarjokin@jestarjokin.net>
 *
 * NOTES: 3-oct-04: the DRO format is not yet finalized. beware.
 *        10-jun-12: the DRO 1 format is finalized, but capturing is buggy.
 */

/*
 * Copyright (c) 2012 - 2017 Wraithverge <liam82067@yahoo.com>
 * - Added Member pointers.
 * - Fixed incorrect operator.
 * - Finalized support for displaying arbitrary Tag data.
 */

package org.uva.emulation;

import java.io.ByteArrayInputStream;
import java.io.IOException;


class DroPlayer extends Opl3Player {
    int[] data;
    long pos;
    long length;
    long msdone;
    long mstotal;
    int delay;
    int index;
    int opl3_mode;

    long total = 0L;
    int currChip = 0;
    boolean verbosePlayer;

    String gettype() {
        return "DOSBox Raw OPL";
    }

    public int getTotalMiliseconds() {
        return (int) mstotal;
    }

    public DroPlayer(boolean verbose) {
        verbosePlayer = verbose;
        opl3_mode = 0;
    }

    public void load(byte[] name) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(name);
        StringBuffer sb = new StringBuffer();
        byte[] id = new byte[8];
        is.read(id);

        int i;
        for (i = 0; i < 8; ++i) {
            sb.append((char) id[i]);
        }

        if (sb.indexOf("DBRAWOPL") != 0) {
            is.close();
            throw new IllegalArgumentException();
        } else {
            i = 0;

            int j;
            for (j = 0; j < 4; ++j) {
                i |= is.read() << j * 8;
            }

            mstotal = 0L;

            for (j = 0; j < 4; ++j) {
                mstotal |= is.read() << j * 8;
            }

            length = 0L;

            for (j = 0; j < 4; ++j) {
                length |= is.read() << j * 8;
            }

            opl3_mode = 0;

            for (j = 0; j < 4; ++j) {
                opl3_mode |= is.read() << j * 8;
            }

            if (verbosePlayer) {
                System.out.printf("id:%s\n", sb);
                System.out.printf("version:%x\n", i);
                System.out.printf("mstotal:%d\n", mstotal);
                System.out.printf("length:%d\n", length);
                System.out.printf("oplType:%d\n", opl3_mode);
            }

            data = new int[(int) length];

            long k;
            for (k = 0L; k < length; ++k) {
                data[(int) k] = is.read();
            }

            is.close();
            total = 0L;
            ByteArrayInputStream bais = new ByteArrayInputStream(name);
            int l = bais.available();
            int[] b = new int[l];

            for (k = 0L; k < l; ++k) {
                b[(int) k] = bais.read();
            }

            rewind(0);
            if (opl3_mode == 1 || opl3_mode == 2) {
                write(1, 5, 1);
            }
        }
    }

    public boolean update() {
        if (delay > 500) {
            delay -= 500;
            return true;
        } else {
            delay = 0;

            while (pos < length) {
                int iIndex = data[(int) pos];
                ++pos;
                switch (iIndex) {
                case 0:
                    delay = 1 + data[(int) pos];
                    ++pos;
                    return true;
                case 1:
                    delay = 1 + data[(int) pos] + (data[(int) pos + 1] << 8);
                    pos += 2L;
                    return true;
                case 2:
                    currChip = 0;
                    break;
                case 3:
                    currChip = 1;
                    break;
                default:
                    if (iIndex == 4) {
                        iIndex = data[(int) pos];
                        ++pos;
                    }

                    if (opl3_mode == 0) {
                        write(0, iIndex, data[(int) pos]);
                    } else {
                        write(currChip, iIndex, data[(int) pos]);
                    }

                    ++pos;
                    total += currChip + iIndex + data[(int) pos - 1];
                }
            }

            return pos < length;
        }
    }

    public void rewind(int subSong) {
        delay = 1;
        pos = index = 0;

        int i;
        for (i = 0; i < 256; ++i) {
            write(currChip, i, 0);
        }

        currChip = 1;

        for (i = 0; i < 256; ++i) {
            write(currChip, i, 0);
        }

        currChip = 0;
        total = 0L;
    }

    public float getRefresh() {
        return delay > 500 ? 2.0F : 1000.0F / delay;
    }
}