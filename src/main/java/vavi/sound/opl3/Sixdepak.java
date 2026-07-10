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

/**
 * Sixpack decompression decoder.
 * Ported from adplug's sixdepack.cpp / sixdepack.h by Simon Peter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class Sixdepak {

    public static final int COPYRANGES = 6;
    public static final int MINCOPY = 3;
    public static final int MAXCOPY = 255;
    public static final int CODESPERRANGE = MAXCOPY - MINCOPY + 1;
    public static final int ROOT = 1;
    public static final int TERMINATE = 256;
    public static final int FIRSTCODE = 257;
    public static final int MAXCHAR = FIRSTCODE + COPYRANGES * CODESPERRANGE - 1;
    public static final int SUCCMAX = MAXCHAR + 1;
    public static final int TWICEMAX = 2 * MAXCHAR + 1;
    public static final int MAXFREQ = 2000;
    public static final int MAXDISTANCE = 21839;
    public static final int MAXSIZE = MAXDISTANCE + MAXCOPY;
    public static final int MAXBUF = 42 * 1024;

    private static int bitvalue(int bit) {
        return 1 << bit;
    }

    private static int copybits(int range) {
        return 2 * range + 4;
    }

    private static int copymin(int range) {
        int[] table = { 0, 16, 80, 336, 1360, 5456 };
        return table[range];
    }

    private final int[] leftc = new int[MAXCHAR + 1];
    private final int[] rghtc = new int[MAXCHAR + 1];
    private final int[] dad = new int[TWICEMAX + 1];
    private final int[] freq = new int[TWICEMAX + 1];

    private int ibitcount;
    private int ibitbuffer;
    private int ibufcount;
    private final int input_size;
    private final int output_size;
    private final int[] wdbuf;
    private final byte[] obuf;

    private Sixdepak(int[] in, int isize, byte[] out, int osize) {
        this.input_size = isize;
        this.output_size = osize;
        this.wdbuf = in;
        this.obuf = out;
    }

    private void inittree() {
        for (int i = 2; i <= TWICEMAX; i++) {
            dad[i] = i / 2;
            freq[i] = 1;
        }
        for (int i = 1; i <= MAXCHAR; i++) {
            leftc[i] = 2 * i;
            rghtc[i] = 2 * i + 1;
        }
    }

    private void updatefreq(int a, int b) {
        for (;;) {
            freq[dad[a]] = freq[a] + freq[b];
            a = dad[a];
            if (a == ROOT) {
                break;
            }
            if (leftc[dad[a]] == a) {
                b = rghtc[dad[a]];
            } else {
                b = leftc[dad[a]];
            }
        }
        if (freq[ROOT] == MAXFREQ) {
            for (int i = 1; i <= TWICEMAX; i++) {
                freq[i] >>= 1;
            }
        }
    }

    private void updatemodel(int code) {
        int a = code + SUCCMAX;
        int b;
        int c;
        int code1;
        int code2;

        freq[a]++;
        if (dad[a] != ROOT) {
            code1 = dad[a];
            if (leftc[code1] == a) {
                updatefreq(a, rghtc[code1]);
            } else {
                updatefreq(a, leftc[code1]);
            }

            do {
                code2 = dad[code1];
                if (leftc[code2] == code1) {
                    b = rghtc[code2];
                } else {
                    b = leftc[code2];
                }

                if (freq[a] > freq[b]) {
                    if (leftc[code2] == code1) {
                        rghtc[code2] = a;
                    } else {
                        leftc[code2] = a;
                    }

                    if (leftc[code1] == a) {
                        leftc[code1] = b;
                        c = rghtc[code1];
                    } else {
                        rghtc[code1] = b;
                        c = leftc[code1];
                    }

                    dad[b] = code1;
                    dad[a] = code2;
                    updatefreq(b, c);
                    a = b;
                }

                a = dad[a];
                code1 = dad[a];
            } while (code1 != ROOT);
        }
    }

    private int inputcode(int bits) {
        int code = 0;
        for (int i = 1; i <= bits; i++) {
            if (ibitcount == 0) {
                if (ibufcount == input_size) {
                    return 0;
                }
                ibitbuffer = wdbuf[ibufcount];
                ibufcount++;
                ibitcount = 15;
            } else {
                ibitcount--;
            }

            if ((ibitbuffer & 0x8000) != 0) {
                code |= bitvalue(i - 1);
            }
            ibitbuffer <<= 1;
        }
        return code;
    }

    private int uncompress() {
        int a = 1;
        do {
            if (ibitcount == 0) {
                if (ibufcount == input_size) {
                    return TERMINATE;
                }
                ibitbuffer = wdbuf[ibufcount];
                ibufcount++;
                ibitcount = 15;
            } else {
                ibitcount--;
            }

            if ((ibitbuffer & 0x8000) != 0) {
                a = rghtc[a];
            } else {
                a = leftc[a];
            }
            ibitbuffer <<= 1;
        } while (a <= MAXCHAR);

        a -= SUCCMAX;
        updatemodel(a);
        return a;
    }

    private int doDecode() {
        int obufcount = 0;
        ibufcount = 0;
        ibitcount = 0;
        ibitbuffer = 0;

        inittree();

        for (;;) {
            int c = uncompress();

            if (c == TERMINATE) {
                return obufcount;
            } else if (c < 256) {
                if (obufcount == output_size) {
                    return output_size;
                }
                obuf[obufcount++] = (byte) c;
            } else {
                int t = c - FIRSTCODE;
                int index = t / CODESPERRANGE;
                int len = t + MINCOPY - index * CODESPERRANGE;
                int dist = inputcode(copybits(index)) + copymin(index) + len;

                for (int i = 0; i < len; i++) {
                    if (obufcount == output_size) {
                        return output_size;
                    }
                    obuf[obufcount] = dist > obufcount ? 0 : obuf[obufcount - dist];
                    obufcount++;
                }
            }
        }
    }

    public static int decode(int[] source, int srcbytes, byte[] dest, int dstbytes) {
        if (srcbytes < 2 || srcbytes > MAXBUF - 4096 || dstbytes < 1) {
            return 0;
        }
        if (dstbytes > MAXBUF) {
            dstbytes = MAXBUF;
        }

        Sixdepak decoder = new Sixdepak(source, srcbytes / 2, dest, dstbytes);
        return decoder.doDecode();
    }
}
