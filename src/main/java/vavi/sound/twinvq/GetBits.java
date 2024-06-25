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

    static class GetBitContext {
        byte[] buffer;
        int buffer_end;
        int index;
        int size_in_bits;
        int size_in_bits_plus8;

        public int get_bits_count() {
            return 0;
        }

        public byte get_bits1() {
            return 0;
        }

        public int init_get_bits8(byte[] buf, int bufSize) {
            return 0;
        }

        public void skip_bits(int bits) {
        }

        /**
         * Read 1-25 bits.
         */
        int get_bits(int n) {
            int tmp = 0;
//            OPEN_READER(re, this);
//            assert n > 0 && n <= 25;
//            UPDATE_CACHE(re, this);
//            tmp = SHOW_UBITS(re, this, n);
//            LAST_SKIP_BITS(re, this, n);
//            CLOSE_READER(re, this);
//            assert tmp < UINT64_C(1) << n;
            return tmp;
        }
    }
}
