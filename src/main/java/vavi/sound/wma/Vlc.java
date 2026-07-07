/*
 * WMA v1/v2 decoder. Huffman VLC reader (FFmpeg vlc_init / vlc_init_from_lengths).
 */

package vavi.sound.wma;

import java.util.ArrayList;
import java.util.List;


/**
 * Huffman VLC decoder backed by a binary tree walked MSB-first.
 * <p>
 * {@link #fromCodes} builds from explicit {@code (code, bits)} pairs (FFmpeg
 * {@code vlc_init}); returned symbol is the table index. {@link #fromLengths}
 * builds canonical codes from lengths (FFmpeg {@code ff_vlc_init_from_lengths})
 * and returns the supplied symbol plus an offset.
 */
final class Vlc {

    private static final int EMPTY = Integer.MIN_VALUE;

    private final int[] left;
    private final int[] right;
    private final int[] syms;

    private Vlc(int[] left, int[] right, int[] syms) {
        this.left = left;
        this.right = right;
        this.syms = syms;
    }

    /** Build from explicit Huffman codes and bit lengths; symbol == index. */
    static Vlc fromCodes(int[] codes, int[] bits) {
        if (codes.length != bits.length) {
            throw new IllegalArgumentException("codes/bits length mismatch.");
        }
        Builder b = new Builder();
        for (int i = 0; i < codes.length; i++) {
            int nbits = bits[i];
            if (nbits == 0) {
                continue;
            }
            b.insert(codes[i], nbits, i);
        }
        return b.build();
    }

    /** Build canonical codes from lengths (FFmpeg ff_vlc_init_from_lengths). */
    static Vlc fromLengths(int[] lengths, int[] symbols, int offset) {
        if (lengths.length != symbols.length) {
            throw new IllegalArgumentException("lengths/symbols length mismatch.");
        }
        Builder b = new Builder();
        long counter = 0;
        for (int i = 0; i < lengths.length; i++) {
            int nbits = lengths[i];
            if (nbits == 0) {
                continue;
            }
            if (nbits > 32 || (counter & ((1L << (32 - nbits)) - 1)) != 0) {
                throw new IllegalArgumentException("Invalid VLC length " + nbits + " at " + i);
            }
            int code = (int) (counter >>> (32 - nbits));
            b.insert(code, nbits, symbols[i] + offset);
            counter += 1L << (32 - nbits);
        }
        return b.build();
    }

    /** Reads one code and returns its symbol. Throws on an invalid code. */
    int decode(BitReader reader) {
        int node = 0;
        while (true) {
            int bit = reader.readBit();
            int next = bit == 0 ? left[node] : right[node];
            if (next < 0) {
                if (next == EMPTY) {
                    throw new IllegalStateException("VLC decode walked off the tree.");
                }
                return syms[-(next + 1)];
            }
            node = next;
        }
    }

    private static final class Builder {
        final List<Integer> left = new ArrayList<>();
        final List<Integer> right = new ArrayList<>();
        final List<Integer> syms = new ArrayList<>();

        Builder() {
            left.add(EMPTY);
            right.add(EMPTY);
        }

        void insert(int code, int nbits, int symbol) {
            int node = 0;
            for (int b = nbits - 1; b > 0; b--) {
                int bit = (code >>> b) & 1;
                int next = bit == 0 ? left.get(node) : right.get(node);
                if (next == EMPTY) {
                    next = left.size();
                    left.add(EMPTY);
                    right.add(EMPTY);
                    if (bit == 0) {
                        left.set(node, next);
                    } else {
                        right.set(node, next);
                    }
                } else if (next < 0) {
                    throw new IllegalArgumentException("VLC prefix collision.");
                }
                node = next;
            }
            int leaf = -(syms.size() + 1);
            syms.add(symbol);
            int lastBit = code & 1;
            if (lastBit == 0) {
                if (left.get(node) != EMPTY) {
                    throw new IllegalArgumentException("Duplicate VLC code.");
                }
                left.set(node, leaf);
            } else {
                if (right.get(node) != EMPTY) {
                    throw new IllegalArgumentException("Duplicate VLC code.");
                }
                right.set(node, leaf);
            }
        }

        Vlc build() {
            return new Vlc(toArray(left), toArray(right), toArray(syms));
        }

        static int[] toArray(List<Integer> list) {
            int[] a = new int[list.size()];
            for (int i = 0; i < a.length; i++) {
                a[i] = list.get(i);
            }
            return a;
        }
    }
}
