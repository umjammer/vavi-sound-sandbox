/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

/**
 * GetBits.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-04-07 nsano initial version <br>
 */
public class GetBits {

    final int size_in_bits;

    int pos;

    public final char[] buffer;
    final int buffer_len;
    int buffer_pos;

    /** Returns the number of bits that have been read from the bitstream */
    public int get_bits_count() {
        return buffer_pos * 8 + pos;
    }

    /** Returns total size of the bitstream in bits */
    public int get_bits_size() {
        return size_in_bits;
    }

    public byte get_bits1() {
        return (byte) get_bits(1);
    }

    public GetBits(byte[] buf, int bufSize) {
        buffer = new char[bufSize];
        buffer_len = bufSize;
        buffer_pos = 0;
        size_in_bits = 8 * bufSize;
        for (int i = 0; i < bufSize; i++) {
            buffer[i] = (char) (buf[i] & 0xff);
        }
    }

    public void skip_bits(int bits) {
        get_bits(bits);
    }

    /**
     * Read 1-25 bits.
     */
    int get_bits(int bitno) {
        int BitsLeft;
        int index = 0;

        while (bitno > 0) {

            // move forward in BitStream when the end of the
            // byte is reached

            if (pos == 8) {
                pos = 0;
                buffer_pos++;
            }

            BitsLeft = 8 - pos;

            // Extract bits to index

            if (BitsLeft >= bitno) {
                index += ((((buffer[buffer_pos]) << (pos)) & 0xFF) >>> (8 - bitno));
                pos += bitno;
                bitno = 0;
            } else {

                if ((8 - bitno) > 0) {
                    index += ((((buffer[buffer_pos]) << (pos)) & 0xFF) >>> (8 - bitno));
                    pos = 8;
                } else {
                    index += ((((buffer[buffer_pos]) << (pos)) & 0xFF) << (bitno - 8));
                    pos = 8;
                }
                bitno -= BitsLeft;
            }
        }

        return index;
    }
}
