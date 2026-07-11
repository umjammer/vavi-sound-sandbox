/*
 * Adplug - Replayer for many OPL2/OPL3 audio file formats.
 * Copyright (C) 1999 - 2008 Simon Peter <dn.tlp@gmx.net>, et al.
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
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioFileFormat.Type;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * BoomTracker 4 Loader.
 * Ported from adplug's cff.cpp / cff.h by Riven the Mage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class CffPlayer extends ProtrackPlayer {

    private static class CffInstrument {
        final byte[] data = new byte[12];
        final byte[] name = new byte[21];
    }

    private final CffInstrument[] instruments = new CffInstrument[47];
    private String songTitle = "";
    private String songAuthor = "";
    private boolean isPacked;

    public CffPlayer() {
        super();
        for (int i = 0; i < 47; i++) {
            instruments[i] = new CffInstrument();
        }
    }

    @Override
    public Type getType() {
        return new Opl3FileFormatType("BoomTracker 4", "cff");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("CFF");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(65536);
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
        byte[] id = new byte[16];
        int read = is.read(id);
        if (read < 16) return false;
        byte[] expected = {
            '<', 'C', 'U', 'D', '-', 'F', 'M', '-', 'F', 'i', 'l', 'e', '>',
            0x1a, (byte) 0xde, (byte) 0xe0
        };
        for (int i = 0; i < 16; i++) {
            if (id[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void load(InputStream is) throws IOException {
        int[] convInst = { 2, 1, 10, 9, 4, 3, 6, 5, 0, 8, 7 };
        int[] convNote = {
            0x16B, 0x181, 0x198, 0x1B0, 0x1CA, 0x1E5,
            0x202, 0x220, 0x241, 0x263, 0x287, 0x2AE
        };

        byte[] buf = is.readAllBytes();
        if (buf.length < 32) {
            throw new IllegalArgumentException("header too short");
        }

        byte[] expected = {
            '<', 'C', 'U', 'D', '-', 'F', 'M', '-', 'F', 'i', 'l', 'e', '>',
            0x1a, (byte) 0xde, (byte) 0xe0
        };
        for (int i = 0; i < 16; i++) {
            if (buf[i] != expected[i]) {
                throw new IllegalArgumentException("invalid signature");
            }
        }

        int version = buf[16] & 0xff;
        int size = (buf[17] & 0xff) | ((buf[18] & 0xff) << 8);
        isPacked = (buf[19] & 0xff) != 0;

        if (size < 16) {
            throw new IllegalArgumentException("size too small");
        }

        if (32 + size > buf.length) {
            throw new IllegalArgumentException("module data truncated");
        }

        byte[] module = new byte[size + 8];
        System.arraycopy(buf, 32, module, 0, size);
        int moduleSize = size;

        if (isPacked) {
            byte[] packedModule = new byte[size + 8];
            System.arraycopy(module, 0, packedModule, 0, size);
            module = new byte[0x10000];
            CffUnpacker unpacker = new CffUnpacker();
            moduleSize = unpacker.unpack(packedModule, module);
            if (moduleSize == 0) {
                throw new IllegalArgumentException("unpack failed");
            }
        }

        if (moduleSize < 0x669 + 64 * 9 * 3) {
            throw new IllegalArgumentException("module data too short");
        }

        byte[] signature = "CUD-FM-File - SEND A POSTCARD -".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int i = 0; i < 31; i++) {
            if (module[0x5E1 + i] != signature[i]) {
                throw new IllegalArgumentException("invalid signature inside module");
            }
        }

        reallocInstruments(47);
        reallocOrder(64);
        reallocPatterns(36, 64, 9);
        initNotetable(convNote);
        initTrackord();

        for (int i = 0; i < 47; i++) {
            System.arraycopy(module, i * 32, instruments[i].data, 0, 12);
            System.arraycopy(module, i * 32 + 12, instruments[i].name, 0, 20);
            instruments[i].name[20] = 0;

            for (int j = 0; j < 11; j++) {
                inst[i].data[convInst[j]] = instruments[i].data[j] & 0xff;
            }
        }

        nop = module[0x5E0] & 0xff;
        if (nop < 1 || nop > 36 || 0x669 + nop * 64 * 9 * 3 > moduleSize) {
            throw new IllegalArgumentException("invalid pattern count or size");
        }

        songTitle = readPascalString(module, 0x614, 20);
        songAuthor = readPascalString(module, 0x600, 20);

        for (int i = 0; i < 64; i++) {
            order[i] = module[0x628 + i] & 0xff;
        }

        int eventsOffset = 0x669;
        for (int i = 0, t = 0; i < nop; i++) {
            int[] oldEventByte2 = new int[9];

            for (int j = 0; j < 9; j++, t++) {
                for (int k = 0; k < 64; k++) {
                    int eventIdx = eventsOffset + (i * 64 * 9 + k * 9 + j) * 3;
                    if (eventIdx + 3 > moduleSize) continue;

                    int byte0 = module[eventIdx] & 0xff;
                    int byte1 = module[eventIdx + 1] & 0xff;
                    int byte2 = module[eventIdx + 2] & 0xff;

                    Tracks track = tracks[t][k];

                    if (byte0 == 0x6D) {
                        track.note = 127;
                    } else {
                        track.note = byte0;
                    }

                    if (byte2 != 0) {
                        oldEventByte2[j] = byte2;
                    }

                    switch (byte1) {
                        case 'I' -> {
                            if (byte2 < 47) {
                                track.inst = byte2 + 1;
                            }
                            track.param1 = 0;
                            track.param2 = 0;
                        }
                        case 'H' -> {
                            track.command = 7;
                            if (byte2 < 16) {
                                track.param1 = 0x07;
                                track.param2 = 0x0D;
                            }
                        }
                        case 'A' -> {
                            track.command = 19;
                            track.param1 = byte2 >> 4;
                            track.param2 = byte2 & 15;
                        }
                        case 'L' -> {
                            track.command = 13;
                            track.param1 = byte2 >> 4;
                            track.param2 = byte2 & 15;
                        }
                        case 'K' -> {
                            track.command = 11;
                            track.param1 = byte2 >> 4;
                            track.param2 = byte2 & 15;
                        }
                        case 'M' -> {
                            track.command = 27;
                            track.param1 = byte2 >> 4;
                            track.param2 = byte2 & 15;
                        }
                        case 'C' -> {
                            track.command = 21;
                            track.param1 = (0x3F - byte2) >> 4;
                            track.param2 = (0x3F - byte2) & 15;
                        }
                        case 'G' -> {
                            track.command = 22;
                            track.param1 = (0x3F - byte2) >> 4;
                            track.param2 = (0x3F - byte2) & 15;
                        }
                        case 'B' -> {
                            track.command = 25;
                            track.param1 = byte2;
                            track.param2 = 0x0F;
                        }
                        case 'E' -> {
                            track.command = 24;
                            track.param1 = oldEventByte2[j] >> 4;
                            track.param2 = oldEventByte2[j] & 15;
                        }
                        case 'F' -> {
                            track.command = 23;
                            track.param1 = oldEventByte2[j] >> 4;
                            track.param2 = oldEventByte2[j] & 15;
                        }
                        case 'D' -> {
                            track.command = 14;
                            if ((oldEventByte2[j] & 15) != 0) {
                                track.param1 = 5;
                                track.param2 = oldEventByte2[j] & 15;
                            } else {
                                track.param1 = 4;
                                track.param2 = oldEventByte2[j] >> 4;
                            }
                        }
                        case 'J' -> {
                            track.param1 = oldEventByte2[j] >> 4;
                            track.param2 = oldEventByte2[j] & 15;
                        }
                    }
                }
            }
        }

        restartpos = 0;

        if (order[0] >= 0x36) {
            throw new IllegalArgumentException("invalid initial order");
        }
        for (length = 1; length < 64; length++) {
            if ((order[length] & 0x80) != 0) {
                break;
            }
            if (order[length] >= 36) {
                throw new IllegalArgumentException("invalid order");
            }
        }

        bpm = 0x7D;

        rewind(0);
    }

    @Override
    public void rewind(int subSong) throws IOException {
        super.rewind(subSong);
        for (int i = 0; i < 9; i++) {
            channel[i].inst = i;
            channel[i].vol1 = 63 - (inst[i].data[10] & 63);
            channel[i].vol2 = 63 - (inst[i].data[9] & 63);
        }
    }

    private String readPascalString(byte[] buf, int offset, int maxLength) {
        int len = 0;
        while (len < maxLength && offset + len < buf.length && buf[offset + len] != 0) {
            len++;
        }
        return new String(buf, offset, len, java.nio.charset.StandardCharsets.US_ASCII).trim();
    }

    @Override
    public float getRefresh() {
        return (float) (tempo / 2.5);
    }



    private static class CffUnpacker {
        private byte[] input;
        private byte[] output;
        private int inputPtr;
        private int outputLength;
        private int codeLength;
        private int bitsLeft;
        private long bitsBuffer;
        private byte[] heap;
        private byte[][] dictionary;
        private int heapLength;
        private int dictionaryLength;
        private final byte[] theString = new byte[256];

        public int unpack(byte[] ibuf, byte[] obuf) {
            byte[] sig = { 'Y', 's', 'C', 'o', 'm', 'p', 0x07, 'C', 'U', 'D', '1', '9', '9', '7', 0x1a, 0x04 };
            for (int i = 0; i < 16; i++) {
                if (ibuf[i] != sig[i]) {
                    return 0;
                }
            }

            input = ibuf;
            inputPtr = 16;
            output = obuf;
            outputLength = 0;

            heap = new byte[0x10000];
            dictionary = new byte[0x8000][];

            if (!startBlock()) {
                return 0;
            }

            for (;;) {
                long newCode = getCode();
                if (newCode == 0x00) {
                    break;
                } else if (newCode == 0x01) {
                    if (!startBlock()) {
                        return 0;
                    }
                } else if (newCode == 0x02) {
                    codeLength++;
                    if (codeLength > 16) {
                        return 0;
                    }
                } else if (newCode == 0x03) {
                    int repeatLength = (int) getCode(2) + 1;
                    int length = 4 << getCode(2);
                    int repeatCounter = (int) getCode(length);

                    int end = outputLength + repeatCounter * repeatLength;
                    if (repeatLength > outputLength || repeatCounter > 0x10000 || end > 0x10000) {
                        return 0;
                    }

                    while (outputLength < end) {
                        byte[] str = new byte[repeatLength];
                        System.arraycopy(output, outputLength - repeatLength, str, 0, repeatLength);
                        putByteString(str, repeatLength);
                    }

                    if (!startString()) {
                        return 0;
                    }
                } else {
                    int dictLen = theString[0] & 0xff;
                    if (newCode >= 0x104 + dictionaryLength) {
                        theString[dictLen + 1] = theString[1];
                        theString[0]++;
                    } else {
                        byte[] tempString = new byte[256];
                        translateCode(newCode, tempString);
                        theString[dictLen + 1] = tempString[1];
                        theString[0]++;
                    }

                    expandDictionary(theString);

                    translateCode(newCode, theString);
                    if (!putPascalString(theString)) {
                        return 0;
                    }
                }
            }

            return outputLength;
        }

        private long getCode(int bitlength) {
            long code;
            long bits = bitsBuffer & 0xffffffffL;
            while (bitsLeft < bitlength) {
                if (inputPtr < input.length) {
                    bits |= ((long) (input[inputPtr] & 0xff)) << bitsLeft;
                    inputPtr++;
                }
                bitsLeft += 8;
            }
            code = bits & ((1L << bitlength) - 1);
            bitsBuffer = bits >> bitlength;
            bitsLeft -= bitlength;
            return code;
        }

        private long getCode() {
            return getCode(codeLength);
        }

        private void translateCode(long code, byte[] string) {
            if (code >= 0x104 + dictionaryLength) {
                string[0] = 0;
                string[1] = 0;
            } else if (code >= 0x104) {
                byte[] entry = dictionary[(int) (code - 0x104)];
                int len = entry[0] & 0xff;
                System.arraycopy(entry, 0, string, 0, len + 1);
            } else {
                string[0] = 1;
                string[1] = (byte) ((code - 4) & 0xff);
            }
        }

        private boolean putByteString(byte[] string, int length) {
            if (outputLength + length > 0x10000) {
                return false;
            }
            System.arraycopy(string, 0, output, outputLength, length);
            outputLength += length;
            return true;
        }

        private boolean putPascalString(byte[] pascalString) {
            int len = pascalString[0] & 0xff;
            byte[] content = new byte[len];
            System.arraycopy(pascalString, 1, content, 0, len);
            return putByteString(content, len);
        }

        private boolean startBlock() {
            codeLength = 9;
            bitsBuffer = 0;
            bitsLeft = 0;
            heapLength = 0;
            dictionaryLength = 0;
            return startString();
        }

        private boolean startString() {
            translateCode(getCode(), theString);
            return putPascalString(theString);
        }

        private void expandDictionary(byte[] string) {
            int len = string[0] & 0xff;
            if (len >= 0xf0 || heapLength + len + 1 > 0x10000) {
                return;
            }
            System.arraycopy(string, 0, heap, heapLength, len + 1);
            byte[] entry = new byte[len + 1];
            System.arraycopy(heap, heapLength, entry, 0, len + 1);
            dictionary[dictionaryLength++] = entry;
            heapLength += len + 1;
        }
    }
}
