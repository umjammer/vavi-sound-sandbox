/*
 * Copyright (c) 2007 robs@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package vavi.sound.pcm.resampling.sox;


/**
 * Addressable FIFO buffer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 081028 nsano initial version <br>
 */
class Fifo {

    /** */
    double[] data;
    /** Number of bytes allocated for data. */
    int allocation;
    /** Offset of the first byte to read. */
    int begin;
    /** 1 + Offset of the last byte byte to read. */
    int end;

    /** */
    static final int FIFO_MIN = 0x4000;

    /** */
    void clear() {
        end = begin = 0;
    }

    /** */
    int reserve(int n) {
if (n > 0x10000000) {
 new Exception("*** DUMMY ***").printStackTrace();
}
        if (begin == end) {
            clear();
        }

        while (true) {
            if (end + n <= allocation) {
                int p = end;

                end += n;
//Debug.printf("fifo: length: %d, start: %d, end: %d, point: %d, n: %02x", allocation, begin, end, p, n);
                return p;
            }
            if (begin > FIFO_MIN) {
                System.arraycopy(data, begin, data, 0, end - begin);
                end -= begin;
//                assert end >= 0;
                begin = 0;
                continue;
            }
            allocation += n;
            data = new double[allocation];
        }
    }

    /** */
    int write(int n, final double[] data) {
        int s = reserve(n);
        if (data != null) {
            System.arraycopy(data, 0, this.data, s, n);
        }
        return s;
    }

    /** */
    void trim_to(int n) {
        end = begin + n;
    }

    /** */
    void trim_by(int n) {
        end -= n;
//        assert end >= 0;
    }

    /** */
    int occupancy() {
        int n = end - begin;
//        assert n >= 0 : "fifo: length " + allocation + ", begin: " + begin + ", end: " + end;
        return n;
    }

    /** */
    int read(int n, double[] data) {
        if (n > end - begin) {
            throw new IndexOutOfBoundsException();
        }
        if (data != null) {
            System.arraycopy(this.data, begin, data, 0, n);
        }
        begin += n;
        return begin;
    }

    /** */
    int read_ptr() {
        return read(0, null);
    }

    /** */
    Fifo() {
        allocation = FIFO_MIN;
        data = new double[allocation];
        clear();
    }
}

/* */
