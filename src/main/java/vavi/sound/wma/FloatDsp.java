/*
 * WMA v1/v2 decoder. Scalar float DSP helpers (FFmpeg AVFloatDSPContext).
 */

package vavi.sound.wma;


/**
 * Scalar float DSP helpers used by WMA windowing and ms-stereo, mirroring
 * FFmpeg's {@code vector_fmul_add}, {@code vector_fmul_reverse} and
 * {@code butterflies_float}.
 */
final class FloatDsp {

    private FloatDsp() {}

    /** {@code dst[i] = src0[i] * src1[i] + add[i]}, all offset-based, len items. */
    static void vectorFmulAdd(float[] dst, int dstOff,
                              float[] src0, int s0Off,
                              float[] src1, int s1Off,
                              float[] add, int addOff, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOff + i] = src0[s0Off + i] * src1[s1Off + i] + add[addOff + i];
        }
    }

    /** {@code dst[i] = src[i] * win[len-1-i]} (window applied reversed). */
    static void vectorFmulReverse(float[] dst, int dstOff,
                                  float[] src, int srcOff,
                                  float[] win, int winOff, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstOff + i] = src[srcOff + i] * win[winOff + len - 1 - i];
        }
    }

    /**
     * In-place sum/difference butterfly used for ms-stereo:
     * {@code a' = a + b}, {@code b' = a - b}.
     */
    static void butterfliesFloat(float[] a, int aOff, float[] b, int bOff, int len) {
        for (int i = 0; i < len; i++) {
            float t = a[aOff + i];
            float u = b[bOff + i];
            a[aOff + i] = t + u;
            b[bOff + i] = t - u;
        }
    }
}
