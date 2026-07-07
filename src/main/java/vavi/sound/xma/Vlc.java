/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;

import java.util.ArrayList;
import java.util.List;


/**
 * Canonical Huffman code reader, mirroring FFmpeg's
 * {@code ff_vlc_init_from_lengths}.
 * <p>
 * Direct translation of {@code Echo/WmaPro/Vlc.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class Vlc {

    private static final int EMPTY = Integer.MIN_VALUE;

    private final int[] left;
    private final int[] right;
    private final int[] syms;

    public Vlc(int[] lens, int[] symbols, int offset) {
        if (lens.length != symbols.length) {
            throw new IllegalArgumentException("lens and syms length mismatch.");
        }

        this.syms = new int[lens.length];
        for (int i = 0; i < symbols.length; i++) {
            this.syms[i] = symbols[i] + offset;
        }

        List<Integer> leftList = new ArrayList<>();
        List<Integer> rightList = new ArrayList<>();
        leftList.add(EMPTY);
        rightList.add(EMPTY);

        long counter = 0;
        int n = lens.length;
        for (int i = 0; i < n; i++) {
            int nbits = lens[i];
            if (nbits == 0) {
                continue;
            }

            if (nbits > 32 || (counter & ((1L << (32 - nbits)) - 1)) != 0) {
                throw new IllegalArgumentException("Invalid VLC (length " + nbits + " at " + i + ").");
            }

            int code = (int) (counter >>> (32 - nbits));

            int node = 0;
            for (int b = nbits - 1; b > 0; b--) {
                int bit = (code >>> b) & 1;
                int next = bit == 0 ? leftList.get(node) : rightList.get(node);
                if (next == EMPTY) {
                    next = leftList.size();
                    leftList.add(EMPTY);
                    rightList.add(EMPTY);
                    if (bit == 0) {
                        leftList.set(node, next);
                    } else {
                        rightList.set(node, next);
                    }
                } else if (next < 0) {
                    throw new IllegalArgumentException("Huffman code prefix collision at position " + i + ".");
                }
                node = next;
            }

            int lastBit = code & 1;
            int leaf = -(i + 1);
            if (lastBit == 0) {
                if (leftList.get(node) != EMPTY) {
                    throw new IllegalArgumentException("Duplicate Huffman code.");
                }
                leftList.set(node, leaf);
            } else {
                if (rightList.get(node) != EMPTY) {
                    throw new IllegalArgumentException("Duplicate Huffman code.");
                }
                rightList.set(node, leaf);
            }

            counter += 1L << (32 - nbits);
        }

        this.left = toIntArray(leftList);
        this.right = toIntArray(rightList);
    }

    private static int[] toIntArray(List<Integer> list) {
        int[] a = new int[list.size()];
        for (int i = 0; i < a.length; i++) {
            a[i] = list.get(i);
        }
        return a;
    }

    /**
     * Read one code and return its (offset-adjusted) symbol value. Throws if the
     * bitstream walks off a node before reaching a leaf.
     */
    public int decode(BitReader reader) {
        int node = 0;
        while (true) {
            int bit = reader.readBits(1);
            int next = bit == 0 ? left[node] : right[node];
            if (next < 0) {
                if (next == EMPTY) {
                    throw new IllegalStateException(
                            "Huffman decode walked off the tree (invalid code).");
                }
                int pos = -(next + 1);
                return syms[pos];
            }
            node = next;
        }
    }
}
