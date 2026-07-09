/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Inverse Modified Discrete Cosine Transform (imdct_half).
 * <p>
 * {@code n} is the MDCT length N. The natural full-IMDCT block size is 2N; only
 * the middle N samples (the ones the sine-window OLA needs) are computed, so
 * input and output regions are both N long.
 * <p>
 * Direct translation of {@code Echo/WmaPro/Imdct.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class Imdct {

    private final Fft fft;
    private final int n;
    private final float[] tcos;
    private final float[] tsin;

    public Imdct(int n, double scale) {
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

    /**
     * Compute one imdct_half: {@code input} has N coefficients starting at
     * {@code inOff}, {@code output} receives N samples starting at
     * {@code outOff} (the middle half of the natural 2N-sample IMDCT). The
     * input and output regions must not overlap.
     */
    public void inverse(float[] input, int inOff, float[] output, int outOff) {
        int n2 = n >> 1;
        int n4 = n >> 2;

        if (input.length - inOff < n) {
            throw new IllegalArgumentException("input too short.");
        }
        if (output.length - outOff < n) {
            throw new IllegalArgumentException("output too short.");
        }

        // -- Pre-rotation --------------------------------------------------
        for (int k = 0; k < n2; k++) {
            float ar = input[inOff + n - 1 - 2 * k];
            float ai = input[inOff + 2 * k];
            float br = tcos[k];
            float bi = tsin[k];
            output[outOff + 2 * k] = ar * br - ai * bi;
            output[outOff + 2 * k + 1] = ar * bi + ai * br;
        }

        // -- N/2-point inverse FFT (in place over output) ------------------
        fft.transform(output, outOff);

        // -- Post-rotation + reordering ------------------------------------
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
}
