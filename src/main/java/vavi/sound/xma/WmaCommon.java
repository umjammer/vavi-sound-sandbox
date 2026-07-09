/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Helpers shared with the WMA family (ff_wma_get_large_val, WMA-Pro branch of
 * ff_wma_run_level_decode).
 * <p>
 * Direct translation of {@code Echo/WmaPro/WmaCommon.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
final class WmaCommon {

    private WmaCommon() {}

    /** Decode an uncompressed coefficient. Consumes up to 31 bits. */
    static int getLargeVal(BitReader reader) {
        int nBits = 8;
        if (reader.read(1) != 0) {
            nBits += 8;
            if (reader.read(1) != 0) {
                nBits += 8;
                if (reader.read(1) != 0) {
                    nBits += 7;
                }
            }
        }
        return reader.readBits(nBits);
    }

    /**
     * Decode the WMA-Pro run-level coefficient stream into {@code ptr} (a
     * subframe-local region starting at {@code ptrOff}) from {@code offset}.
     * Returns true on success.
     *
     * @param ptr backing coefficient array
     * @param ptrOff offset of the subframe region within {@code ptr}
     */
    static boolean runLevelDecode(
            BitReader reader,
            Vlc vlc,
            float[] levelTable,
            int[] runTable,
            float[] ptr,
            int ptrOff,
            int offset,
            int numCoefs,
            int blockLen,
            int frameLenBits) {
        int coefMask = blockLen - 1;

        for (; offset < numCoefs; offset++) {
            int code = vlc.decode(reader);

            if (code > 1) {
                // normal: pre-quantised float, sign-flip via IEEE bit XOR
                offset += runTable[code];
                int sign = reader.read(1) - 1;     // 0 or -1
                int bits = Float.floatToRawIntBits(levelTable[code]) ^ ((sign & 1) << 31);
                ptr[ptrOff + (offset & coefMask)] = Float.intBitsToFloat(bits);
            } else if (code == 1) {
                // EOB
                break;
            } else {
                // escape (version=1 / wmapro branch)
                int level = getLargeVal(reader);

                if (reader.read(1) != 0) {
                    if (reader.read(1) != 0) {
                        if (reader.read(1) != 0) {
                            return false; // broken escape
                        }
                        offset += reader.read(frameLenBits) + 4;
                    } else {
                        offset += reader.read(2) + 1;
                    }
                }

                int sign = reader.read(1) - 1; // 0 or -1
                int val = (level ^ sign) - sign;
                ptr[ptrOff + (offset & coefMask)] = val;
            }
        }

        return offset <= numCoefs;
    }
}
