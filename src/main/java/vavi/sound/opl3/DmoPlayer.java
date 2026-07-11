/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2006 Simon Peter, <dn.tlp@gmx.net>, et al.
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
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * TwinTeam DMO Loader.
 * Ported from adplug's dmo.cpp / dmo.h by Riven the Mage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class DmoPlayer extends S3mPlayer {

    public DmoPlayer() {
        super();
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("TwinTeam", "dmo");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("DMO");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(16);
            return matchFormatImpl(bitStream);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    protected boolean matchFormatImpl(InputStream is) throws IOException {
        byte[] chkhdr = new byte[16];
        int read = is.read(chkhdr);
        if (read < 16) return false;
        DmoUnpacker unpacker = new DmoUnpacker();
        return unpacker.decrypt(chkhdr, 16);
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] packedModule = is.readAllBytes();
        int packedLength = packedModule.length;

        if (packedLength < 16) {
            throw new IllegalArgumentException("file too short");
        }

        DmoUnpacker unpacker = new DmoUnpacker();
        byte[] chkhdr = new byte[16];
        System.arraycopy(packedModule, 0, chkhdr, 0, 16);
        if (!unpacker.decrypt(chkhdr, 16)) {
            throw new IllegalArgumentException("decryption verification failed");
        }

        if (!unpacker.decrypt(packedModule, packedLength)) {
            throw new IllegalArgumentException("decryption failed");
        }

        int unpackedLength = 0x2000 * u16(packedModule, 12);
        byte[] module = new byte[unpackedLength];

        int unpackedRealLength = unpacker.unpack(packedModule, packedLength, module, unpackedLength);
        if (unpackedRealLength == 0) {
            throw new IllegalArgumentException("unpack failed");
        }

        byte[] signature = "TwinTeam Module File\r\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int i = 0; i < 22; i++) {
            if (module[i] != signature[i]) {
                throw new IllegalArgumentException("invalid signature inside module");
            }
        }

        header = new S3mHeader();
        header.name = readString(module, 22, 28).trim();

        int off = 22 + 28;
        off += 2; // ignore _unk_1
        header.ordnum = u16(module, off); off += 2;
        header.insnum = u16(module, off); off += 2;
        header.patnum = u16(module, off); off += 2;
        off += 2; // ignore _unk_2
        header.is = u16(module, off); off += 2;
        header.it = u16(module, off); off += 2;

        if (header.ordnum >= 256 || header.insnum > 99 || header.patnum > 99) {
            throw new IllegalArgumentException("invalid header sizes");
        }

        Arrays.fill(header.chanset, (byte) 0xFF);
        for (int i = 0; i < 9; i++) {
            header.chanset[i] = (byte) (0x10 + i);
        }

        off += 32; // ignore panning settings

        for (int i = 0; i < 256; i++) {
            orders[i] = module[off++];
        }
        orders[header.ordnum] = (byte) 0xFF;

        int[] myPatlen = new int[100];
        for (int i = 0; i < 100; i++) {
            myPatlen[i] = u16(module, off); off += 2;
        }

        for (int i = 0; i < header.insnum; i++) {
            inst[i] = new S3mInst();
            inst[i].name = readString(module, off, 28).trim(); off += 28;
            inst[i].volume = module[off++] & 0xff;
            inst[i].dsk = module[off++] & 0xff;
            inst[i].c2spd = u32(module, off); off += 4;
            inst[i].type = module[off++] & 0xff;
            inst[i].d00 = module[off++] & 0xff;
            inst[i].d01 = module[off++] & 0xff;
            inst[i].d02 = module[off++] & 0xff;
            inst[i].d03 = module[off++] & 0xff;
            inst[i].d04 = module[off++] & 0xff;
            inst[i].d05 = module[off++] & 0xff;
            inst[i].d06 = module[off++] & 0xff;
            inst[i].d07 = module[off++] & 0xff;
            inst[i].d08 = module[off++] & 0xff;
            inst[i].d09 = module[off++] & 0xff;
            inst[i].d0a = module[off++] & 0xff;
            inst[i].d0b = module[off++] & 0xff;
        }

        for (int i = 0; i < header.patnum; i++) {
            loadPattern(i, module, off, myPatlen[i]);
            off += myPatlen[i];
        }

        rewind(0);
    }

    private static class DmoUnpacker {
        private long bseed;

        private int brand(int range) {
            bseed = (bseed * 0x08088405L + 1) & 0xffffffffL;
            long val = bseed * (long) range;
            return (int) ((val >> 32) & 0xffff);
        }

        public boolean decrypt(byte[] buf, int len) {
            if (len < 12) {
                return false;
            }

            bseed = u32(buf, 0);

            long seed = 0;
            int loopCount = u16(buf, 4);
            for (int i = 0; i <= loopCount; i++) {
                seed = (seed + brand(0xffff)) & 0xffffffffL;
            }

            bseed = seed ^ u32(buf, 6);

            if (u16(buf, 10) != brand(0xffff)) {
                return false;
            }

            for (int i = 12; i < len; i++) {
                buf[i] = (byte) ((buf[i] & 0xff) ^ brand(0x100));
            }

            if (len >= 2) {
                buf[len - 2] = 0;
                buf[len - 1] = 0;
            }

            return true;
        }

        private int unpackBlock(byte[] ibuf, int ibufOffset, int ilen,
                                 byte[] obuf, int obufOffset, int olen) {
            int ipos = 0, opos = 0;

            while (ipos < ilen) {
                int cpy = 0, ofs = 0, lit = 0;
                int code = ibuf[ibufOffset + ipos++] & 0xff;
                int par1 = (ipos < ilen) ? (ibuf[ibufOffset + ipos] & 0xff) : 0;
                int par2 = (ipos + 1 < ilen) ? (ibuf[ibufOffset + ipos + 1] & 0xff) : 0;

                switch (code >> 6) {
                    case 0 -> lit = (code & 0x3F) + 1;
                    case 1 -> {
                        ipos++;
                        ofs = ((code & 0x3F) << 3) + ((par1 & 0xE0) >> 5) + 1;
                        cpy = (par1 & 0x1F) + 3;
                    }
                    case 2 -> {
                        ipos++;
                        ofs = ((code & 0x3F) << 1) + (par1 >> 7) + 1;
                        cpy = ((par1 & 0x70) >> 4) + 3;
                        lit = par1 & 0x0F;
                    }
                    case 3 -> {
                        ipos += 2;
                        ofs = ((code & 0x3F) << 7) + (par1 >> 1);
                        cpy = ((par1 & 0x01) << 4) + (par2 >> 4) + 4;
                        lit = par2 & 0x0F;
                    }
                }

                if (ipos + lit > ilen || opos + cpy + lit > olen || ofs > opos) {
                    return -1;
                }

                for (int i = 0; i < cpy; i++) {
                    obuf[obufOffset + opos + i] = obuf[obufOffset + opos - ofs + i];
                }
                opos += cpy;

                while (lit-- > 0) {
                    obuf[obufOffset + opos++] = ibuf[ibufOffset + ipos++];
                }
            }

            return opos;
        }

        public int unpack(byte[] ibuf, int inputsize, byte[] obuf, int outputsize) {
            if (inputsize < 12 + 2) {
                return 0;
            }

            int blockCount = u16(ibuf, 12);
            int blockStart = 12 + 2 + 2 * blockCount;

            if (inputsize < blockStart) {
                return 0;
            }

            int blockLengthOffset = 12 + 2;
            int ibufOffset = blockStart;
            int remainingInputSize = inputsize - blockStart;

            int olen = 0;
            int obufOffset = 0;

            for (int i = 0; i < blockCount; i++) {
                int blen = u16(ibuf, blockLengthOffset + 2 * i);
                if (blen < 2 || blen > remainingInputSize) {
                    return 0;
                }

                int bul = u16(ibuf, ibufOffset);

                int unpacked = unpackBlock(ibuf, ibufOffset + 2, blen - 2, obuf, obufOffset, outputsize - olen);
                if (unpacked != bul) {
                    return 0;
                }

                obufOffset += bul;
                olen += bul;

                ibufOffset += blen;
                remainingInputSize -= blen;
            }

            return olen;
        }
    }
}
