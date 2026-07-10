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
import java.util.ArrayDeque;
import java.util.Deque;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat.Encoding;
import vavi.sound.sampled.opl3.Opl3Encoding;
import vavi.sound.sampled.opl3.Opl3FileFormatType;

/**
 * Ultima 6 Music Player.
 * Ported from adplug's u6m.cpp / u6m.h by Marc Winterrowd.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 */
public class U6mPlayer extends Opl3Player {

    private static final int MAX_CODEWORD_LENGTH = 12;

    private static final int[] adlib_channel_to_carrier_offset = {
        0x03, 0x04, 0x05, 0x0B, 0x0C, 0x0D, 0x13, 0x14, 0x15
    };
    private static final int[] adlib_channel_to_modulator_offset = {
        0x00, 0x01, 0x02, 0x08, 0x09, 0x0A, 0x10, 0x11, 0x12
    };

    /** packed frequency table ([n][0]: lo, [n][1]: hi) */
    private static final int[][] freq_table = {
        {0x00, 0x00}, {0x58, 0x01}, {0x82, 0x01}, {0xB0, 0x01},
        {0xCC, 0x01}, {0x03, 0x02}, {0x41, 0x02}, {0x86, 0x02},
        {0x00, 0x00}, {0x6A, 0x01}, {0x96, 0x01}, {0xC7, 0x01},
        {0xE4, 0x01}, {0x1E, 0x02}, {0x5F, 0x02}, {0xA8, 0x02},
        {0x00, 0x00}, {0x47, 0x01}, {0x6E, 0x01}, {0x9A, 0x01},
        {0xB5, 0x01}, {0xE9, 0x01}, {0x24, 0x02}, {0x66, 0x02}
    };

    private static class SubsongInfo {
        int continuePos;
        int subsongStart;
        int subsongRepetitions;
    }

    /** the uncompressed .m file (the "song") */
    private byte[] songData;
    private boolean driverActive;
    private boolean songend;
    /** current offset within the song */
    private int songPos;
    /** position of the loop point */
    private int loopPosition;
    /** delay (in timer ticks) before further song data is read */
    private int readDelay;
    private final Deque<SubsongInfo> subsongStack = new ArrayDeque<>();

    /** offsets of the adlib instrument data */
    private final int[] instrumentOffsets = new int[9];
    // vibrato ("vb")
    private final int[] vbCurrentValue = new int[9];
    private final int[] vbDoubleAmplitude = new int[9];
    private final int[] vbMultiplier = new int[9];
    private final int[] vbDirectionFlag = new int[9];
    // mute factor ("mf") = not(volume)
    private final int[] carrierMf = new int[9];
    private final int[] carrierMfSignedDelta = new int[9];
    private final int[] carrierMfModDelayBackup = new int[9];
    private final int[] carrierMfModDelay = new int[9];
    // frequency ([n][0]: lo, [n][1]: hi)
    private final int[][] channelFreq = new int[9][2];
    private final int[] channelFreqSignedDelta = new int[9];

    @Override
    public Type getType() {
        return new Opl3FileFormatType("Ultima 6 Music", "m");
    }

    @Override
    public Encoding getEncoding() {
        return new Opl3Encoding("U6M");
    }

    @Override
    public boolean matchFormat(InputStream bitStream) {
        try {
            bitStream.mark(Integer.MAX_VALUE);
            byte[] buf = bitStream.readAllBytes();
            return validate(buf);
        } catch (Exception e) {
            return false;
        } finally {
            try {
                bitStream.reset();
            } catch (IOException ignored) {}
        }
    }

    /** checks the LZW pseudo-header (matches adplug's necessary conditions) */
    private static boolean validate(byte[] buf) {
        if (buf.length < 6) return false;
        int decompressedSize = (buf[0] & 0xff) + ((buf[1] & 0xff) << 8);
        return buf[2] == 0 && buf[3] == 0 &&
                (buf[4] & 0xff) + ((buf[5] & 0x1) << 8) == 0x100 &&
                decompressedSize > buf.length - 4;
    }

    @Override
    public int getTotalMilliseconds() {
        return 0;
    }

    @Override
    public void load(InputStream is) throws IOException {
        byte[] buf = is.readAllBytes();
        if (!validate(buf)) {
            throw new IllegalArgumentException("invalid pseudo-header");
        }
        int decompressedSize = (buf[0] & 0xff) + ((buf[1] & 0xff) << 8);

        songData = new byte[decompressedSize];
        if (!lzwDecompress(buf, 4, buf.length - 4, songData)) {
            throw new IllegalArgumentException("LZW decompression failed");
        }

        rewind(0);
    }

    @Override
    public boolean update() {
        if (!driverActive) {
            driverActive = true;
            readDelay--;
            if (readDelay < 0) readDelay = 0;
            if (readDelay == 0) {
                commandLoop();
            }

            // on all Adlib channels: freq slide/vibrato, mute factor slide
            for (int i = 0; i < 9; i++) {
                if (channelFreqSignedDelta[i] != 0) {
                    // frequency slide + mute factor slide
                    freqSlide(i);

                    if (carrierMfSignedDelta[i] != 0) {
                        mfSlide(i);
                    }
                } else {
                    // vibrato + mute factor slide
                    if (vbMultiplier[i] != 0 && (channelFreq[i][1] & 0x20) == 0x20) {
                        vibrato(i);
                    }

                    if (carrierMfSignedDelta[i] != 0) {
                        mfSlide(i);
                    }
                }
            }

            driverActive = false;
        }

        return !songend;
    }

    @Override
    public void rewind(int subSong) {
        songend = false;

        driverActive = false;
        songPos = 0;
        loopPosition = 0;
        readDelay = 0;

        for (int i = 0; i < 9; i++) {
            channelFreqSignedDelta[i] = 0;
            channelFreq[i][0] = 0;
            channelFreq[i][1] = 0;

            vbCurrentValue[i] = 0;
            vbDoubleAmplitude[i] = 0;
            vbMultiplier[i] = 0;
            vbDirectionFlag[i] = 0;

            carrierMf[i] = 0;
            carrierMfSignedDelta[i] = 0;
            carrierMfModDelayBackup[i] = 0;
            carrierMfModDelay[i] = 0;
        }

        subsongStack.clear();

        for (int i = 0; i < 256; i++) {
            write(0, i, 0);
        }
        outAdlib(1, 32); // go to OPL2 mode
    }

    @Override
    public float getRefresh() {
        return 60.0f; // the Ultima 6 music driver expects to be called at 60 Hz
    }

    // ---- functions called by load()

    /** decompresses U6 LZW data, false on error */
    private static boolean lzwDecompress(byte[] source, int srcOff, int srcSize, byte[] dest) {
        boolean endMarkerReached = false;
        int codewordSize = 9;
        long bitsRead = 0;
        int nextFreeCodeword = 0x102;
        int dictionarySize = 0x200;
        // dictionary: root and codeword per entry (roots 0-0xff are implicit)
        int[] dictRoot = new int[4096 - 0x100];
        int[] dictCodeword = new int[4096 - 0x100];
        int dictContains = 0x102;
        Deque<Integer> rootStack = new ArrayDeque<>();

        int bytesWritten = 0;

        int cW;
        int pW = 0;

        while (!endMarkerReached) {
            cW = getNextCodeword(bitsRead, source, srcOff, srcSize, codewordSize);
            bitsRead += codewordSize;
            switch (cW) {
            case 0x100:
                // re-init the dictionary
                codewordSize = 9;
                nextFreeCodeword = 0x102;
                dictionarySize = 0x200;
                dictContains = 0x102;
                cW = getNextCodeword(bitsRead, source, srcOff, srcSize, codewordSize);
                bitsRead += codewordSize;
                if (bytesWritten >= dest.length) return false;
                dest[bytesWritten++] = (byte) cW;
                break;
            case 0x101:
                // end of compressed file has been reached
                endMarkerReached = true;
                break;
            case -1:
                // no next code word available, i.e., truncated or invalid data
                return false;
            default:
                if (cW < nextFreeCodeword) { // codeword is already in the dictionary
                    // create the string associated with cW (on the stack)
                    getString(cW, dictRoot, dictCodeword, rootStack);
                    int c = rootStack.peek();
                    // output the string represented by cW
                    while (!rootStack.isEmpty()) {
                        if (bytesWritten >= dest.length) return false;
                        dest[bytesWritten++] = (byte) (int) rootStack.pop();
                    }
                    // add pW+C to the dictionary
                    if (dictContains < 4096) {
                        dictRoot[dictContains - 0x100] = c;
                        dictCodeword[dictContains - 0x100] = pW;
                        dictContains++;
                    }

                    nextFreeCodeword++;
                    if (nextFreeCodeword >= dictionarySize) {
                        if (codewordSize < MAX_CODEWORD_LENGTH) {
                            codewordSize += 1;
                            dictionarySize *= 2;
                        }
                    }
                } else { // codeword is not yet defined
                    // create the string associated with pW (on the stack)
                    getString(pW, dictRoot, dictCodeword, rootStack);
                    int c = rootStack.peek();
                    // output the string represented by pW
                    while (!rootStack.isEmpty()) {
                        if (bytesWritten >= dest.length) return false;
                        dest[bytesWritten++] = (byte) (int) rootStack.pop();
                    }
                    // output the char C
                    if (bytesWritten >= dest.length) return false;
                    dest[bytesWritten++] = (byte) c;

                    // the new dictionary entry must correspond to cW
                    if (cW != nextFreeCodeword) {
                        return false;
                    }
                    // add pW+C to the dictionary
                    if (dictContains < 4096) {
                        dictRoot[dictContains - 0x100] = c;
                        dictCodeword[dictContains - 0x100] = pW;
                        dictContains++;
                    }

                    nextFreeCodeword++;
                    if (nextFreeCodeword >= dictionarySize) {
                        if (codewordSize < MAX_CODEWORD_LENGTH) {
                            codewordSize += 1;
                            dictionarySize *= 2;
                        }
                    }
                }
                break;
            }
            // shift roles - the current cW becomes the new pW
            pW = cW;
        }

        return true;
    }

    /** reads the next code word from the source buffer, -1 on error */
    private static int getNextCodeword(long bitsRead, byte[] source, int srcOff, int srcSize, int codewordSize) {
        int byteIndex = (int) (bitsRead / 8);
        boolean needThird = bitsRead % 8 + codewordSize > 16;
        if (srcSize - byteIndex < 2 + (needThird ? 1 : 0)) {
            return -1; // source exhausted
        }

        int b0 = source[srcOff + byteIndex] & 0xff;
        int b1 = source[srcOff + byteIndex + 1] & 0xff;
        int b2 = needThird ? source[srcOff + byteIndex + 2] & 0xff : 0;

        int codeword = (b2 << 16) + (b1 << 8) + b0;
        codeword = codeword >> (int) (bitsRead % 8);
        switch (codewordSize) {
        case 0x9:
            codeword = codeword & 0x1ff;
            break;
        case 0xa:
            codeword = codeword & 0x3ff;
            break;
        case 0xb:
            codeword = codeword & 0x7ff;
            break;
        case 0xc:
            codeword = codeword & 0xfff;
            break;
        default:
            codeword = -1; // indicates that an error has occurred
            break;
        }

        return codeword;
    }

    /** pushes the string represented by a codeword (reversed) onto the stack */
    private static void getString(int codeword, int[] dictRoot, int[] dictCodeword, Deque<Integer> rootStack) {
        int currentCodeword = codeword;

        while (currentCodeword > 0xff) {
            int root = dictRoot[currentCodeword - 0x100];
            currentCodeword = dictCodeword[currentCodeword - 0x100];
            rootStack.push(root);
        }

        // push the root at the leaf
        rootStack.push(currentCodeword);
    }

    // ---- functions called by update()

    /** reads the song data and executes the embedded commands */
    private void commandLoop() {
        boolean repeatLoop = true;

        do {
            int commandByte = readSongByte(); // implicitly increments songPos
            if (commandByte < 0) { // handle invalid position
                songend = true;
                break;
            }
            int hi = commandByte >> 4;
            int lo = commandByte & 0xf;

            switch (hi) {
            case 0x0: command0(lo); break;
            case 0x1: command1(lo); break;
            case 0x2: command2(lo); break;
            case 0x3: command3(lo); break;
            case 0x4: command4(lo); break;
            case 0x5: command5(lo); break;
            case 0x6: command6(lo); break;
            case 0x7: command7(lo); break;
            case 0x8:
                switch (lo) {
                case 1: command81(); break;
                case 2: command82(); repeatLoop = false; break;
                case 3: command83(); break;
                case 5: command85(); break;
                case 6: command86(); break;
                default: break;
                }
                break;
            case 0xE: commandE(); break;
            case 0xF: commandF(); break;
            default: break;
            }
        } while (repeatLoop);
    }

    /** set octave and frequency, note off */
    private void command0(int channel) {
        int freqByte = readSongByte() & 0xff;
        int[] freqWord = expandFreqByte(freqByte);
        if (channel < 9) {
            setAdlibFreq(channel, freqWord[0], freqWord[1]);
        }
    }

    /** set octave and frequency, old note off, new note on */
    private void command1(int channel) {
        int freqByte = readSongByte() & 0xff;
        int[] freqWord = expandFreqByte(freqByte);

        if (channel < 9) {
            vbDirectionFlag[channel] = 0;
            vbCurrentValue[channel] = 0;

            setAdlibFreq(channel, freqWord[0], freqWord[1]);

            setAdlibFreq(channel, freqWord[0], freqWord[1] | 0x20); // note on
        }
    }

    /** set octave and frequency, note on */
    private void command2(int channel) {
        int freqByte = readSongByte() & 0xff;
        int[] freqWord = expandFreqByte(freqByte);
        if (channel < 9) {
            setAdlibFreq(channel, freqWord[0], freqWord[1] | 0x20); // note on
        }
    }

    /** set "carrier mute factor" == not(volume) */
    private void command3(int channel) {
        int mfByte = readSongByte() & 0xff;

        if (channel < 9) {
            carrierMfSignedDelta[channel] = 0;
            setCarrierMf(channel, mfByte);
        }
    }

    /** set "modulator mute factor" == not(volume) */
    private void command4(int channel) {
        int mfByte = readSongByte() & 0xff;

        if (channel < 9) {
            setModulatorMf(channel, mfByte);
        }
    }

    /** set portamento (pitch slide) */
    private void command5(int channel) {
        int delta = readSignedSongByte();

        if (channel < 9) {
            channelFreqSignedDelta[channel] = delta;
        }
    }

    /** set vibrato parameters */
    private void command6(int channel) {
        int vbParameters = readSongByte() & 0xff;

        if (channel < 9) {
            vbDoubleAmplitude[channel] = vbParameters >> 4; // high nibble
            vbMultiplier[channel] = vbParameters & 0xF; // low nibble
        }
    }

    /** assign Adlib instrument to Adlib channel */
    private void command7(int channel) {
        int instrumentNumber = readSongByte() & 0xff;

        if (channel < 9 && instrumentNumber < 9) {
            int off = instrumentOffsets[instrumentNumber];
            outAdlibOpcell(channel, false, 0x20, sd(off));
            outAdlibOpcell(channel, false, 0x40, sd(off + 1));
            outAdlibOpcell(channel, false, 0x60, sd(off + 2));
            outAdlibOpcell(channel, false, 0x80, sd(off + 3));
            outAdlibOpcell(channel, false, 0xE0, sd(off + 4));
            outAdlibOpcell(channel, true, 0x20, sd(off + 5));
            outAdlibOpcell(channel, true, 0x40, sd(off + 6));
            outAdlibOpcell(channel, true, 0x60, sd(off + 7));
            outAdlibOpcell(channel, true, 0x80, sd(off + 8));
            outAdlibOpcell(channel, true, 0xE0, sd(off + 9));
            outAdlib(0xC0 + channel, sd(off + 10));
        }
    }

    /** branch to a new subsong */
    private void command81() {
        SubsongInfo ss = new SubsongInfo();

        ss.subsongRepetitions = readSongByte() & 0xff;
        ss.subsongStart = readSongByte() & 0xff;
        ss.subsongStart += (readSongByte() & 0xff) << 8;
        ss.continuePos = songPos;

        subsongStack.push(ss);
        songPos = ss.subsongStart;
    }

    /** stop interpreting commands for this timer tick */
    private void command82() {
        readDelay = readSongByte() & 0xff;
    }

    /** Adlib instrument data follows */
    private void command83() {
        int instrumentNumber = readSongByte() & 0xff;

        if (instrumentNumber < 9 && songData.length > 11 && songPos < songData.length - 11) {
            instrumentOffsets[instrumentNumber] = songPos;
            songPos += 11;
        }
    }

    /** set -1 mute factor slide (upward volume slide) */
    private void command85() {
        int dataByte = readSongByte() & 0xff;
        int channel = dataByte >> 4; // high nibble
        int slideDelay = dataByte & 0xF; // low nibble

        if (channel < 9) {
            carrierMfSignedDelta[channel] = +1;
            carrierMfModDelay[channel] = slideDelay + 1;
            carrierMfModDelayBackup[channel] = slideDelay + 1;
        }
    }

    /** set +1 mute factor slide (downward volume slide) */
    private void command86() {
        int dataByte = readSongByte() & 0xff;
        int channel = dataByte >> 4; // high nibble
        int slideDelay = dataByte & 0xF; // low nibble

        if (channel < 9) {
            carrierMfSignedDelta[channel] = -1;
            carrierMfModDelay[channel] = slideDelay + 1;
            carrierMfModDelayBackup[channel] = slideDelay + 1;
        }
    }

    /** set loop point */
    private void commandE() {
        loopPosition = songPos;
    }

    /** return from current subsong */
    private void commandF() {
        if (!subsongStack.isEmpty()) {
            SubsongInfo temp = subsongStack.pop();
            temp.subsongRepetitions--;
            if (temp.subsongRepetitions == 0) {
                songPos = temp.continuePos;
            } else {
                songPos = temp.subsongStart;
                subsongStack.push(temp);
            }
        } else {
            songPos = loopPosition;
            songend = true;
        }
    }

    // ---- additional functions

    /** returns the byte at the current song position (-1 past end); increments songPos */
    private int readSongByte() {
        if (songPos >= 0 && songPos < songData.length) {
            return songData[songPos++] & 0xff;
        } else {
            return -1;
        }
    }

    /** same as readSongByte(), except that it returns a signed byte */
    private int readSignedSongByte() {
        int songByte = readSongByte();

        if (songByte >= 0x80) {
            songByte -= 0x100;
        }
        return songByte;
    }

    /** song data byte (0 when out of bounds) */
    private int sd(int off) {
        return off >= 0 && off < songData.length ? songData[off] & 0xff : 0;
    }

    /** expands a packed frequency byte, returns {lo, hi} */
    private static int[] expandFreqByte(int freqByte) {
        int packedFreq = freqByte & 0x1F;
        int octave = freqByte >> 5;

        // range check (not present in the original U6 music driver)
        if (packedFreq >= 24) packedFreq = 0;

        return new int[] {
            freq_table[packedFreq][0],
            (freq_table[packedFreq][1] + (octave << 2)) & 0xff
        };
    }

    private void setAdlibFreq(int channel, int lo, int hi) {
        outAdlib(0xA0 + channel, lo);
        outAdlib(0xB0 + channel, hi);
        // update the Adlib register backups
        channelFreq[channel][0] = lo;
        channelFreq[channel][1] = hi;
    }

    /** sets the Adlib frequency, but does not update the register backups */
    private void setAdlibFreqNoUpdate(int channel, int lo, int hi) {
        outAdlib(0xA0 + channel, lo);
        outAdlib(0xB0 + channel, hi);
    }

    private void setCarrierMf(int channel, int muteFactor) {
        outAdlibOpcell(channel, true, 0x40, muteFactor);
        carrierMf[channel] = muteFactor;
    }

    private void setModulatorMf(int channel, int muteFactor) {
        outAdlibOpcell(channel, false, 0x40, muteFactor);
    }

    private void freqSlide(int channel) {
        int freqWord = channelFreq[channel][0] + (channelFreq[channel][1] << 8) + channelFreqSignedDelta[channel];
        if (freqWord < 0) freqWord += 0x10000;
        if (freqWord > 0xFFFF) freqWord -= 0x10000;

        setAdlibFreq(channel, freqWord & 0xFF, (freqWord >> 8) & 0xFF);
    }

    private void vibrato(int channel) {
        if (vbCurrentValue[channel] >= vbDoubleAmplitude[channel]) {
            vbDirectionFlag[channel] = 1;
        } else if (vbCurrentValue[channel] <= 0) {
            vbDirectionFlag[channel] = 0;
        }

        // vb_current_value is u8 in adplug and may wrap
        if (vbDirectionFlag[channel] == 0) {
            vbCurrentValue[channel] = (vbCurrentValue[channel] + 1) & 0xff;
        } else {
            vbCurrentValue[channel] = (vbCurrentValue[channel] - 1) & 0xff;
        }

        int freqWord = channelFreq[channel][0] + (channelFreq[channel][1] << 8);
        freqWord += (vbCurrentValue[channel] - (vbDoubleAmplitude[channel] >> 1)) * vbMultiplier[channel];
        if (freqWord < 0) freqWord += 0x10000;
        if (freqWord > 0xFFFF) freqWord -= 0x10000;

        setAdlibFreqNoUpdate(channel, freqWord & 0xFF, (freqWord >> 8) & 0xFF);
    }

    private void mfSlide(int channel) {
        carrierMfModDelay[channel]--;
        if (carrierMfModDelay[channel] == 0) {
            carrierMfModDelay[channel] = carrierMfModDelayBackup[channel];
            int currentMf = carrierMf[channel] + carrierMfSignedDelta[channel];
            if (currentMf > 0x3F) {
                currentMf = 0x3F;
                carrierMfSignedDelta[channel] = 0;
            } else if (currentMf < 0) {
                currentMf = 0;
                carrierMfSignedDelta[channel] = 0;
            }

            setCarrierMf(channel, currentMf);
        }
    }

    private void outAdlib(int adlibRegister, int adlibData) {
        write(0, adlibRegister & 0xff, adlibData & 0xff);
    }

    private void outAdlibOpcell(int channel, boolean carrier, int adlibRegister, int outByte) {
        if (carrier) {
            outAdlib(adlibRegister + adlib_channel_to_carrier_offset[channel], outByte);
        } else {
            outAdlib(adlibRegister + adlib_channel_to_modulator_offset[channel], outByte);
        }
    }
}
