/*
 * WMA v1/v2 decoder. Java port of FFmpeg's get_bits reader semantics.
 */

package vavi.sound.wma;


/**
 * MSB-first bit reader over a byte buffer, mirroring FFmpeg's
 * {@code GetBitContext}. Reads are big-endian within the byte stream, matching
 * {@code get_bits}/{@code get_bits_long}.
 * <p>
 * Ported to support the WMA v1/v2 decoder ({@code libavcodec/wmadec.c}).
 *
 * @see "https://github.com/FFmpeg/FFmpeg libavcodec/get_bits.h"
 */
final class BitReader {

    private final byte[] buffer;
    private final int startBit;
    private final int endBit;
    private int bitPos;

    BitReader(byte[] buffer, int startBit, int totalBits) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (startBit < 0 || totalBits < 0 || startBit + (long) totalBits > (long) buffer.length * 8) {
            throw new IndexOutOfBoundsException("bit range");
        }
        this.buffer = buffer;
        this.startBit = startBit;
        this.endBit = startBit + totalBits;
        this.bitPos = startBit;
    }

    /** Bits consumed since construction ({@code get_bits_count}). */
    int bitsCount() {
        return bitPos - startBit;
    }

    /** Bits remaining before end ({@code get_bits_left}). */
    int bitsLeft() {
        return endBit - bitPos;
    }

    /** Reads up to 32 bits MSB-first, returned as an unsigned value in an int. */
    int readBits(int n) {
        if (n < 0 || n > 32) {
            throw new IllegalArgumentException("n=" + n);
        }
        if (n == 0) {
            return 0;
        }
        if (bitPos + n > endBit) {
            throw new IllegalStateException("BitReader read past end.");
        }
        int value = 0;
        int remaining = n;
        int pos = bitPos;
        while (remaining > 0) {
            int byteIndex = pos >> 3;
            int bitInByte = pos & 7;
            int bitsAvailableInByte = 8 - bitInByte;
            int take = Math.min(remaining, bitsAvailableInByte);
            int shift = bitsAvailableInByte - take;
            int mask = (1 << take) - 1;
            int chunk = ((buffer[byteIndex] & 0xFF) >> shift) & mask;
            value = (value << take) | chunk;
            pos += take;
            remaining -= take;
        }
        bitPos = pos;
        return value;
    }

    /** Reads a single bit. */
    int readBit() {
        return readBits(1);
    }

    /** Skips {@code n} bits ({@code skip_bits}). */
    void skipBits(int n) {
        if (n < 0 || bitPos + n > endBit) {
            throw new IllegalStateException("skip past end.");
        }
        bitPos += n;
    }

    /** Advances to the next byte boundary ({@code align_get_bits}). */
    void alignToByte() {
        int rem = bitPos & 7;
        if (rem != 0) {
            skipBits(8 - rem);
        }
    }
}
