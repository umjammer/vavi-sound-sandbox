/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * MSB-first bit reader over a byte buffer.
 * <p>
 * Direct translation of {@code Echo/Bitstream/BitReader.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class BitReader {

    private final byte[] buffer;
    private final int endBit;
    private int bitPos;

    public BitReader(byte[] buffer, int startBit, int totalBits) {
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (startBit < 0) {
            throw new IndexOutOfBoundsException("startBit");
        }
        if (totalBits < 0 || startBit + (long) totalBits > (long) buffer.length * 8) {
            throw new IndexOutOfBoundsException("totalBits");
        }

        this.buffer = buffer;
        this.endBit = startBit + totalBits;
        this.bitPos = startBit;
    }

    public int remainingBits() {
        return endBit - bitPos;
    }

    /** Reads up to 32 bits, returning them as an unsigned value packed into an int. */
    public int readBits(int n) {
        if (n < 0 || n > 32) {
            throw new IllegalArgumentException("n");
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

    public int read(int n) {
        return readBits(n);
    }
}
