/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter, <dn.tlp@gmx.net>, et al.
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
 * Depackers for Adlib Tracker II modules (versions 2-3, 6-14).
 * Ported from adplug's depack.c (aPLib v0.26b decompressor by Joergen
 * Ibsen), unlzw.c / unlzss.c (custom LZW / LZSS from AT2 by Stanislav
 * Baranec) and unlzh.c (SCO compress -H, derived from ar002 by Haruhiko
 * Okumura, with AT2-tweaked table sizes).
 * Versions 1 and 5 use {@link Sixdepak}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
final class A2mV2Depack {

    private A2mV2Depack() {}

    // ---- aPLib (depack.c), versions 9-11

    private static class ApState {
        byte[] src;
        int srcPos;
        int srcLeft;
        byte[] dst;
        int dstPos;
        int dstLeft;
        int tag;
        int bitcount;
    }

    private static int apGetBit(ApState ud) {
        if (ud.bitcount-- == 0) {
            ud.tag = ud.srcLeft > 0 ? ud.src[ud.srcPos] & 0xff : 0;
            ud.srcPos++;
            ud.bitcount = 7;
        }
        int bit = (ud.tag >> 7) & 0x01;
        ud.tag = (ud.tag << 1) & 0xff;
        return bit;
    }

    private static int apGetGamma(ApState ud) {
        int result = 1;
        do {
            result = (result << 1) + apGetBit(ud);
        } while (apGetBit(ud) != 0);
        return result;
    }

    /** aPLib v0.26b decompressor */
    static int apDepack(byte[] source, int srcOff, int srcsize, byte[] destination, int dstsize) {
        ApState ud = new ApState();
        ud.src = source;
        ud.srcPos = srcOff;
        ud.srcLeft = srcsize;
        ud.dst = destination;
        ud.dstPos = 0;
        ud.dstLeft = dstsize;

        int offs, len;
        int r0 = -1;
        boolean done = false;

        // first byte verbatim
        if (srcsize == 0 || dstsize == 0) return ud.dstPos;
        ud.dst[ud.dstPos++] = ud.src[ud.srcPos++];
        ud.srcLeft--;
        ud.dstLeft--;

        // main decompression loop
        while (!done) {
            if (apGetBit(ud) != 0) {
                if (apGetBit(ud) != 0) {
                    if (apGetBit(ud) != 0) {
                        offs = 0;
                        for (int i = 4; i != 0; i--) {
                            offs = (offs << 1) + apGetBit(ud);
                        }

                        if (offs != 0) {
                            if (ud.dstLeft == 0) return ud.dstPos;
                            if (offs > ud.dstPos) return ud.dstPos;
                            ud.dst[ud.dstPos] = ud.dst[ud.dstPos - offs];
                            ud.dstPos++;
                            ud.dstLeft--;
                        } else {
                            if (ud.dstLeft == 0) return ud.dstPos;
                            ud.dst[ud.dstPos++] = 0;
                            ud.dstLeft--;
                        }
                    } else {
                        if (ud.srcLeft == 0) return ud.dstPos;
                        offs = ud.src[ud.srcPos++] & 0xff;
                        ud.srcLeft--;

                        len = 2 + (offs & 0x0001);
                        offs >>= 1;

                        if (offs != 0) {
                            for (; len != 0; len--) {
                                if (ud.dstLeft == 0) return ud.dstPos;
                                if (offs > ud.dstPos) return ud.dstPos;
                                ud.dst[ud.dstPos] = ud.dst[ud.dstPos - offs];
                                ud.dstPos++;
                                ud.dstLeft--;
                            }
                        } else {
                            done = true;
                        }

                        r0 = offs;
                    }
                } else {
                    offs = apGetGamma(ud);

                    if (offs == 2) {
                        offs = r0;
                        len = apGetGamma(ud);

                        for (; len != 0; len--) {
                            if (ud.dstLeft == 0) return ud.dstPos;
                            if (offs > ud.dstPos) return ud.dstPos;
                            ud.dst[ud.dstPos] = ud.dst[ud.dstPos - offs];
                            ud.dstPos++;
                            ud.dstLeft--;
                        }
                    } else {
                        offs -= 3;

                        offs <<= 8;
                        if (ud.srcLeft == 0) return ud.dstPos;
                        offs += ud.src[ud.srcPos++] & 0xff;
                        ud.srcLeft--;

                        len = apGetGamma(ud);

                        if (offs >= 32000) len++;
                        if (offs >= 1280) len++;
                        if (offs < 128) len += 2;

                        for (; len != 0; len--) {
                            if (ud.dstLeft == 0) return ud.dstPos;
                            if (offs > ud.dstPos) return ud.dstPos;
                            ud.dst[ud.dstPos] = ud.dst[ud.dstPos - offs];
                            ud.dstPos++;
                            ud.dstLeft--;
                        }

                        r0 = offs;
                    }
                }
            } else {
                if (ud.srcLeft == 0) return ud.dstPos;
                if (ud.dstLeft == 0) return ud.dstPos;
                ud.dst[ud.dstPos++] = ud.src[ud.srcPos++];
                ud.srcLeft--;
                ud.dstLeft--;
            }
        }

        return ud.dstPos;
    }

    // ---- custom LZW (unlzw.c), versions 2 and 6

    /** custom LZW decompression from AT2 */
    static int lzwDecompress(byte[] source, int srcOff, int srcSize, byte[] dest, int destSize) {
        byte[] stack = new byte[65636];
        byte[] work = new byte[65636];

        int bitshift = 9;
        long prevbitstring = 0;
        int[] bitmask = { 0x1ff, 0x3ff, 0x7ff, 0xfff, 0x1fff };

        int le76 = 0, le77 = 0;
        int le6a = 0, le6c = 0, le6e = 0, le70, stringlength, le74;

        int code;
        int outputIdx = 0;

        int sp = 65536 - 1;

        stringlength = 0;
        le70 = 0x102;
        le74 = 0x200;

        while (true) {
            // nextcode()
            int inputIdx = (int) (prevbitstring >> 3);
            long bitstring = in8(source, srcOff, srcSize, inputIdx)
                    + (in8(source, srcOff, srcSize, inputIdx + 1) << 8)
                    + (in8(source, srcOff, srcSize, inputIdx + 2) << 16);
            bitstring >>= prevbitstring & 7;
            prevbitstring += bitshift;
            code = (int) (bitstring & bitmask[bitshift - 9]);

            if (code == 0x101) break;

            if (code == 0x100) {
                bitshift = 9;
                le74 = 0x200;
                le70 = 0x102;
                // nextcode()
                inputIdx = (int) (prevbitstring >> 3);
                bitstring = in8(source, srcOff, srcSize, inputIdx)
                        + (in8(source, srcOff, srcSize, inputIdx + 1) << 8)
                        + (in8(source, srcOff, srcSize, inputIdx + 2) << 16);
                bitstring >>= prevbitstring & 7;
                prevbitstring += bitshift;
                code = (int) (bitstring & bitmask[bitshift - 9]);

                le6a = code;
                le6c = code;
                le77 = code & 0xff;
                le76 = code & 0xff;

                if (outputIdx >= destSize) break;
                dest[outputIdx++] = (byte) code;
                continue;
            }

            le6a = code;
            le6e = code;

            if (code >= le70) {
                code = le6c;
                le6a = code;
                code = (code & 0xff00) + le76;
                sp--;
                stack[sp] = (byte) code;
                stringlength++;
            }

            while (le6a > 0xff) {
                code = (code & 0xff00) + (work[(le6a * 3) + 2] & 0xff);
                sp--;
                stack[sp] = (byte) code;
                stringlength++;
                code = (work[le6a * 3] & 0xff) + ((work[(le6a * 3) + 1] & 0xff) << 8);
                le6a = code;
            }

            code = le6a;
            le76 = code & 0xff;
            le77 = code & 0xff;
            sp--;
            stack[sp] = (byte) code;
            stringlength++;

            boolean overflow = false;
            while (stringlength-- != 0) {
                if (outputIdx >= destSize) {
                    overflow = true;
                    break;
                }
                dest[outputIdx++] = stack[sp++];
            }
            if (overflow) break;

            stringlength = 0;

            work[(le70 * 3)] = (byte) le6c;
            work[(le70 * 3) + 1] = (byte) (le6c >> 8);
            work[(le70 * 3) + 2] = (byte) le77;
            le70++;

            code = le6e;
            le6c = code;

            if (le70 >= le74 && bitshift < 14) {
                bitshift++;
                le74 <<= 1;
            }
        }

        return outputIdx;
    }

    private static long in8(byte[] b, int off, int size, int idx) {
        // matches the C reading past the declared size within the buffer
        int p = off + idx;
        return p >= 0 && p < b.length ? b[p] & 0xffL : 0;
    }

    // ---- custom LZSS (unlzss.c), versions 3 and 7

    private static final int N_BITS = 12;
    private static final int THRESHOLD_LZSS = 2;
    private static final int N = 1 << N_BITS;
    private static final int F = (1 << 4) + THRESHOLD_LZSS;

    /** custom LZSS decompression from AT2 */
    static int lzssDecompress(byte[] source, int srcOff, int srcSize, byte[] dest, int destSize) {
        int inputIdx = 0;
        int code = 0, prevcode;
        int dx;
        int ebx, edi;

        byte[] work = new byte[65536];

        int outputSize = 0;
        dx = 0;
        edi = N - F;

        loop:
        while (true) {
            dx = dx >> 1;

            if ((dx >> 8) == 0) {
                if (inputIdx >= srcSize) break;
                code = source[srcOff + inputIdx++] & 0xff;
                dx = 0xff00 | code;
            }

            if ((dx & 1) != 0) {
                if (inputIdx >= srcSize) break;
                code = source[srcOff + inputIdx++] & 0xff;
                work[edi] = (byte) code;
                edi = (edi + 1) & (N - 1);
                if (outputSize >= destSize) break;
                dest[outputSize++] = (byte) code;
                continue;
            }

            if (inputIdx >= srcSize) break;
            prevcode = code = source[srcOff + inputIdx++] & 0xff;

            if (inputIdx >= srcSize) break;
            code = source[srcOff + inputIdx++] & 0xff;
            ebx = ((code << 4) & 0xff00) | prevcode;

            int length = (code & 0x0f) + THRESHOLD_LZSS + 1;

            do {
                if (outputSize >= destSize) break loop;
                dest[outputSize] = work[edi] = work[ebx];
                outputSize++;

                ebx = (ebx + 1) & (N - 1);
                edi = (edi + 1) & (N - 1);
            } while (--length > 0);
        }

        return outputSize;
    }

    // ---- LZH (unlzh.c), versions 12-14

    private static final int DICBIT = 14;
    private static final int DIC_SIZE = 1 << DICBIT;
    private static final int CHAR_BIT = 8;
    private static final int UCHAR_MAX = 255;
    private static final int BITBUFSIZ = CHAR_BIT * 2;
    private static final int MAXMATCH = 256;
    private static final int THRESHOLD_LZH = 2;
    private static final int NC = UCHAR_MAX + MAXMATCH + 2 - THRESHOLD_LZH;
    private static final int CBIT = 16;
    private static final int CODE_BIT = 16;
    private static final int NP = DICBIT + 1;
    private static final int NT = CODE_BIT + 3;
    private static final int PBIT = 14;
    private static final int TBIT = 15;
    private static final int NPT = 1 << TBIT;

    private static class Lzh {
        final int[] left = new int[2 * NC - 1];
        final int[] right = new int[2 * NC - 1];
        final int[] cLen = new int[NC];
        final int[] ptLen = new int[NPT];
        int blocksize;
        final int[] ptTable = new int[256];
        final int[] cTable = new int[4096];

        int bitbuf; // u16
        int subbitbuf;
        int bitcount;

        byte[] inputBuffer;
        int inputIdx;
        int inputSize;

        int j; // remaining bytes to copy
        int decodeI;

        int tryByte() {
            if (inputIdx < inputSize) {
                return inputBuffer[inputIdx++] & 0xff;
            } else {
                return 0;
            }
        }

        /** shift bitbuf n bits left, read n bits */
        void fillbuf(int n) {
            bitbuf = (bitbuf << n) & 0xffff;
            while (n > bitcount) {
                bitbuf |= (subbitbuf << (n -= bitcount)) & 0xffff;
                subbitbuf = tryByte();
                bitcount = CHAR_BIT;
            }
            bitbuf |= subbitbuf >> (bitcount -= n);
            bitbuf &= 0xffff;
        }

        int getbits(int n) {
            int x = bitbuf >> (BITBUFSIZ - n);
            fillbuf(n);
            return x;
        }

        void initGetbits() {
            bitbuf = 0;
            subbitbuf = 0;
            bitcount = 0;
            fillbuf(BITBUFSIZ);
        }

        void makeTable(int nchar, int[] bitlen, int tablebits, int[] table) {
            int[] count = new int[17];
            int[] weight = new int[17];
            int[] start = new int[18];
            int i, k, len, ch, jutbits, avail, nextcode, mask;

            for (i = 1; i <= 16; i++) count[i] = 0;
            for (i = 0; i < nchar; i++) count[bitlen[i]]++;

            start[1] = 0;
            for (i = 1; i <= 16; i++) {
                start[i + 1] = (start[i] + (count[i] << (16 - i)));
            }
            // (bad table check omitted; C only prints)

            jutbits = 16 - tablebits;
            for (i = 1; i <= tablebits; i++) {
                start[i] >>>= jutbits;
                weight[i] = 1 << (tablebits - i);
            }
            while (i <= 16) {
                weight[i] = 1 << (16 - i);
                i++;
            }

            i = (start[tablebits + 1] & 0xffff) >>> jutbits;
            if (i != 0) {
                k = 1 << tablebits;
                while (i != k) table[i++] = 0;
            }

            avail = nchar;
            mask = 1 << (15 - tablebits);
            for (ch = 0; ch < nchar; ch++) {
                if ((len = bitlen[ch]) == 0) continue;
                nextcode = start[len] + weight[len];
                if (len <= tablebits) {
                    for (i = start[len]; i < nextcode; i++) table[i] = ch;
                } else {
                    k = start[len];
                    int pIdx = k >>> jutbits;
                    int[] pArr = table;
                    i = len - tablebits;
                    while (i != 0) {
                        if (pArr[pIdx] == 0) {
                            right[avail] = left[avail] = 0;
                            pArr[pIdx] = avail++;
                        }
                        int next = pArr[pIdx];
                        if ((k & mask) != 0) {
                            pArr = right;
                        } else {
                            pArr = left;
                        }
                        pIdx = next;
                        k = (k << 1) & 0xffff;
                        i--;
                    }
                    pArr[pIdx] = ch;
                }
                start[len] = nextcode & 0xffff;
            }
        }

        void readPtLen(int nn, int nbit, int iSpecial) {
            int i, c, n;
            int mask;

            n = getbits(nbit);
            if (n == 0) {
                c = getbits(nbit);
                for (i = 0; i < nn; i++) ptLen[i] = 0;
                for (i = 0; i < 256; i++) ptTable[i] = c;
            } else {
                i = 0;
                while (i < n) {
                    c = bitbuf >> (BITBUFSIZ - 3);
                    if (c == 7) {
                        mask = 1 << (BITBUFSIZ - 1 - 3);
                        while ((mask & bitbuf) != 0) {
                            mask >>= 1;
                            c++;
                        }
                    }
                    fillbuf(c < 7 ? 3 : c - 3);
                    ptLen[i++] = c;
                    if (i == iSpecial) {
                        c = getbits(2);
                        while (--c >= 0) ptLen[i++] = 0;
                    }
                }
                while (i < nn) ptLen[i++] = 0;
                makeTable(nn, ptLen, 8, ptTable);
            }
        }

        void readCLen() {
            int i, c, n;
            int mask;

            n = getbits(CBIT);
            if (n == 0) {
                c = getbits(CBIT);
                for (i = 0; i < NC; i++) cLen[i] = 0;
                for (i = 0; i < 4096; i++) cTable[i] = c;
            } else {
                i = 0;
                while (i < n) {
                    c = ptTable[bitbuf >> (BITBUFSIZ - 8)];
                    if (c >= NT) {
                        mask = 1 << (BITBUFSIZ - 1 - 8);
                        do {
                            if ((bitbuf & mask) != 0) c = right[c];
                            else c = left[c];
                            mask >>= 1;
                        } while (c >= NT);
                    }
                    fillbuf(ptLen[c]);
                    if (c <= 2) {
                        if (c == 0) c = 1;
                        else if (c == 1) c = getbits(4) + 3;
                        else c = getbits(CBIT) + 20;
                        while (--c >= 0) cLen[i++] = 0;
                    } else {
                        cLen[i++] = c - 2;
                    }
                }
                while (i < NC) cLen[i++] = 0;
                makeTable(NC, cLen, 12, cTable);
            }
        }

        int decodeC() {
            int j, mask;

            if (blocksize == 0) {
                blocksize = getbits(16);
                if (blocksize == 0) {
                    return NC; // end of file
                }
                readPtLen(NT, TBIT, 3);
                readCLen();
                readPtLen(NP, PBIT, -1);
            }
            blocksize--;
            j = cTable[bitbuf >> (BITBUFSIZ - 12)];
            if (j >= NC) {
                mask = 1 << (BITBUFSIZ - 1 - 12);
                do {
                    if ((bitbuf & mask) != 0) j = right[j];
                    else j = left[j];
                    mask >>= 1;
                } while (j >= NC);
            }
            fillbuf(cLen[j]);
            return j;
        }

        int decodeP() {
            int j, mask;

            j = ptTable[bitbuf >> (BITBUFSIZ - 8)];
            if (j >= NP) {
                mask = 1 << (BITBUFSIZ - 1 - 8);
                do {
                    if ((bitbuf & mask) != 0) j = right[j];
                    else j = left[j];
                    mask >>= 1;
                } while (j >= NP);
            }
            fillbuf(ptLen[j]);
            if (j != 0) j = (1 << (j - 1)) + getbits(j - 1);
            return j;
        }

        void decodeStart() {
            initGetbits();
            blocksize = 0;
            j = 0;
        }

        /** decodes count bytes (or fewer at EOF) into buffer */
        int decode(int count, byte[] buffer) {
            int r, c;

            r = 0;
            while (--j >= 0) {
                buffer[r] = buffer[decodeI];
                decodeI = (decodeI + 1) & (DIC_SIZE - 1);
                if (++r == count) return r;
            }
            while (true) {
                c = decodeC();
                if (c == NC) {
                    return r;
                }
                if (c <= UCHAR_MAX) {
                    buffer[r] = (byte) c;
                    if (++r == count) return r;
                } else {
                    j = c - (UCHAR_MAX + 1 - THRESHOLD_LZH);
                    decodeI = (r - decodeP() - 1) & (DIC_SIZE - 1);
                    while (--j >= 0) {
                        buffer[r] = buffer[decodeI];
                        decodeI = (decodeI + 1) & (DIC_SIZE - 1);
                        if (++r == count) return r;
                    }
                }
            }
        }
    }

    /** LZH decompression (SCO compress -H with AT2 table sizes) */
    static int lzhDecompress(byte[] source, int srcOff, int srcSize, byte[] dest, int destSize) {
        Lzh lzh = new Lzh();
        lzh.inputBuffer = source;
        lzh.inputIdx = srcOff;
        lzh.inputSize = srcOff + srcSize;

        lzh.inputIdx++; // "ultra" flag byte, unused with fixed table sizes

        long sizeUnpacked = (source[lzh.inputIdx] & 0xffL) |
                ((source[lzh.inputIdx + 1] & 0xffL) << 8) |
                ((source[lzh.inputIdx + 2] & 0xffL) << 16) |
                ((source[lzh.inputIdx + 3] & 0xffL) << 24);
        lzh.inputIdx += 4;

        byte[] ptr = new byte[DIC_SIZE];
        int outIdx = 0;

        lzh.decodeStart();
        long size = sizeUnpacked;
        int destLeft = destSize;
        while (size > 0 && destLeft != 0) {
            int sizeTemp = size > DIC_SIZE ? DIC_SIZE : (int) size;

            lzh.decode(sizeTemp, ptr);
            if (destLeft >= sizeTemp) {
                System.arraycopy(ptr, 0, dest, outIdx, sizeTemp);
                outIdx += sizeTemp;
                destLeft -= sizeTemp;
            } else {
                System.arraycopy(ptr, 0, dest, outIdx, destLeft);
                outIdx += destLeft;
                destLeft = 0;
            }
            size -= sizeTemp;
        }

        return (int) sizeUnpacked;
    }
}
