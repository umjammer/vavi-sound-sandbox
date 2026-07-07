/*
 * WMA v1/v2 decoder. Inverse MDCT (half + full), port of FFmpeg's ff_imdct.
 */

package vavi.sound.wma;


/**
 * Inverse Modified Discrete Cosine Transform. {@code n} is the number of MDCT
 * coefficients (block length). {@link #inverseHalf} computes the middle
 * {@code n} samples (imdct_half); {@link #inverseFull} extends them by symmetry
 * to the full {@code 2n} samples that WMA v1/v2 windowing needs.
 * <p>
 * The scale/twiddle convention matches FFmpeg's classic {@code ff_mdct_init}
 * (twiddles scaled by {@code sqrt(|scale|)}), which {@code AV_TX_FLOAT_MDCT}
 * reproduces; passing {@code scale = 1/32768} yields float PCM in roughly
 * [-1, 1].
 */
final class Imdct {

    private final Fft fft;
    private final int n;
    private final float[] tcos;
    private final float[] tsin;

    Imdct(int n, double scale) {
        if (n < 4 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("IMDCT length must be a power of two >= 4.");
        }
        this.n = n;
        int n2 = n >> 1;
        fft = new Fft(n2, true);

        tcos = new float[n2];
        tsin = new float[n2];
        double theta = 1.0 / 8.0;
        double sqScale = Math.sqrt(Math.abs(scale));
        for (int i = 0; i < n2; i++) {
            double alpha = 2.0 * Math.PI * (i + theta) / (2.0 * n);
            tcos[i] = (float) (-Math.cos(alpha) * sqScale);
            tsin[i] = (float) (-Math.sin(alpha) * sqScale);
        }
    }

    /** Number of MDCT coefficients (block length). */
    int length() {
        return n;
    }

    /**
     * imdct_half: {@code input} has {@code n} coefficients at {@code inOff};
     * {@code output} receives {@code n} samples at {@code outOff} (the region is
     * also used as FFT scratch, so it must not overlap the input).
     */
    void inverseHalf(float[] input, int inOff, float[] output, int outOff) {
        int n2 = n >> 1;
        int n4 = n >> 2;
        if (input.length - inOff < n || output.length - outOff < n) {
            throw new IllegalArgumentException("buffer too short.");
        }
        // Pre-rotation.
        for (int k = 0; k < n2; k++) {
            float ar = input[inOff + n - 1 - 2 * k];
            float ai = input[inOff + 2 * k];
            float br = tcos[k];
            float bi = tsin[k];
            output[outOff + 2 * k] = ar * br - ai * bi;
            output[outOff + 2 * k + 1] = ar * bi + ai * br;
        }
        // N/2-point inverse FFT in place.
        fft.transform(output, outOff);
        // Post-rotation + reorder.
        for (int k = 0; k < n4; k++) {
            int ia = outOff + 2 * (n4 - k - 1);
            int ib = outOff + 2 * (n4 + k);
            float aRe = output[ia];
            float aIm = output[ia + 1];
            float bRe = output[ib];
            float bIm = output[ib + 1];
            float taS = tsin[n4 - k - 1];
            float taC = tcos[n4 - k - 1];
            float tbS = tsin[n4 + k];
            float tbC = tcos[n4 + k];
            float r0 = aIm * taS - aRe * taC;
            float i1 = aIm * taC + aRe * taS;
            float r1 = bIm * tbS - bRe * tbC;
            float i0 = bIm * tbC + bRe * tbS;
            output[ia] = r0;
            output[ia + 1] = i0;
            output[ib] = r1;
            output[ib + 1] = i1;
        }
    }

    /**
     * Full IMDCT: {@code input} has {@code n} coefficients at {@code inOff};
     * {@code output} receives {@code 2n} samples at {@code outOff}. Mirrors
     * FFmpeg's {@code ff_imdct_calc_c}.
     */
    void inverseFull(float[] input, int inOff, float[] output, int outOff) {
        if (output.length - outOff < 2 * n) {
            throw new IllegalArgumentException("output too short for full IMDCT.");
        }
        int n4 = n >> 1;   // ffmpeg n4 = (2n)>>2 = n/2
        // Half result placed at outOff + n4.
        inverseHalf(input, inOff, output, outOff + n4);
        for (int k = 0; k < n4; k++) {
            output[outOff + k] = -output[outOff + n - k - 1];
            output[outOff + 2 * n - k - 1] = output[outOff + n + k];
        }
    }
}
