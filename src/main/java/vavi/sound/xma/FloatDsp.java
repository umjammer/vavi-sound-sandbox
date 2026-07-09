/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Scalar float DSP helpers (ff_vector_fmul_scalar_c, ff_vector_fmul_window_c).
 * <p>
 * Direct translation of {@code Echo/WmaPro/FloatDsp.cs} (SIMD path dropped).
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
final class FloatDsp {

    private FloatDsp() {}

    /**
     * {@code dst[dstOff+i] = src[srcOff+i] * mul}, i in [0,len). In-place safe
     * (dst may alias src).
     */
    static void vectorFmulScalar(float[] dst, int dstOff, float[] src, int srcOff, float mul, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOff + i] = src[srcOff + i] * mul;
        }
    }

    /**
     * Sine-window overlap-add. Writes {@code 2*len} samples to dst from two
     * {@code len}-sized input segments and a {@code 2*len}-sized window.
     * <pre>
     * for k in 0..len-1:
     *     dst[k]         = src0[k] * win[2*len-1-k] - src1[len-1-k] * win[k]
     *     dst[2*len-1-k] = src0[k] * win[k]         + src1[len-1-k] * win[2*len-1-k]
     * </pre>
     * Safe with src0 aliasing the first half of dst and src1 the second half —
     * that's how WMA Pro calls it.
     */
    static void vectorFmulWindow(float[] dst, int dstOff,
                                 float[] src0, int src0Off,
                                 float[] src1, int src1Off,
                                 float[] win, int len) {
        for (int k = 0; k < len; k++) {
            float s0 = src0[src0Off + k];
            float s1 = src1[src1Off + len - 1 - k];
            float wi = win[k];
            float wj = win[2 * len - 1 - k];

            dst[dstOff + k] = s0 * wj - s1 * wi;
            dst[dstOff + 2 * len - 1 - k] = s0 * wi + s1 * wj;
        }
    }
}
