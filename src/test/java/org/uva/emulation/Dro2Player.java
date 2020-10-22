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
 * dro2.cpp - DOSBox Raw OPL v2.0 player by Adam Nielsen <malvineous@shikadi.net>
 */

/*
 * Copyright (c) 2017 Wraithverge <liam82067@yahoo.com>
 * - Finalized support for displaying arbitrary Tag data.
 */

package org.uva.emulation;

import java.io.ByteArrayInputStream;
import java.io.IOException;

class Dro2Player extends Opl3Player {
    int[] data;
    long pos;
    long length;
    long msdone;
    long msTotal;
    int delay;
    int delay256;
    int delayShift8;
    int[] toReg;
    int opl3Type;
    long total = 0L;
    boolean verbose;

    public int getTotalMiliseconds() {
        return (int) msTotal;
    }

    public Dro2Player(boolean verbose) {
        this.verbose = verbose;
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

            if (i != 2) {
                is.close();
                throw new IllegalArgumentException();
            } else {
                j = 0;

                int k;
                for (k = 0; k < 4; ++k) {
                    j |= is.read() << k * 8;
                }

                length = j * 2;
                msTotal = 0L;

                for (k = 0; k < 4; ++k) {
                    msTotal |= is.read() << k * 8;
                }

                opl3Type = is.read();
                if (is.read() != 0) {
                    is.close();
                    throw new IllegalArgumentException();
                } else if (is.read() != 0) {
                    is.close();
                    throw new IllegalArgumentException();
                } else {
                    delay256 = is.read();
                    delayShift8 = is.read();
                    k = is.read();
                    toReg = new int[k];

                    for (int l = 0; l < k; ++l) {
                        toReg[l] = is.read();
                    }

                    if (verbose) {
                        System.out.printf("id:%s\n", sb);
                        System.out.printf("version:%x\n", i);
                        System.out.printf("length:%d\n", length);
                        System.out.printf("mstotal:%d\n", msTotal);
                        System.out.printf("opl3Type:%d\n", opl3Type);
                        System.out.printf("delay256:%d\n", delay256);
                        System.out.printf("delayShift8:%d\n", delayShift8);
                    }

                    data = new int[(int) length];

                    long l;
                    for (l = 0L; l < length; ++l) {
                        data[(int) l] = is.read();
                    }

                    is.close();
                    total = 0L;
                    ByteArrayInputStream baos = new ByteArrayInputStream(name);
                    int len = baos.available();
                    int[] b = new int[len];

                    for (l = 0L; l < len; ++l) {
                        b[(int) l] = baos.read();
                    }

                    rewind(0);
                    if (opl3Type != 0) {
                        write(1, 5, 1);
                    }
                }
            }
        }
    }

    public boolean update() {
        delay = 0;

        while (pos < length) {
            int iIndex = data[(int) pos] & 255;
            ++pos;
            if (iIndex == delay256) {
                delay += 1 + (data[(int) pos] & 255);
                ++pos;
                return true;
            }

            if (iIndex == delayShift8) {
                delay = 1 + (data[(int) pos] & 255) << 8;
                ++pos;
                return true;
            }

            int iReg = toReg[iIndex & 127] & 255;
            int i = iIndex >> 7 & 1;
            int iValue = data[(int) pos] & 255;
            ++pos;
            write(i, iReg, iValue);
        }

        return pos < length;
    }

    public void rewind(int subSong) {
        delay = 1;
        pos = 0L;

        for (int i = 0; i < 256; ++i) {
            write(0, i, 0);
            write(1, i, 0);
        }

        total = 0L;
    }

    public float getRefresh() {
        return 1000.0F / delay;
    }
}