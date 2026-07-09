/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo). Java port.
 */

package vavi.sound.xma;


/**
 * Iterative radix-2 Cooley-Tukey complex FFT, in-place, interleaved
 * {@code [re0,im0,re1,im1,...]}.
 * <p>
 * Direction is fixed at construction. With {@code inverse=false} the transform
 * computes {@code X[k] = sum_n x[n] * exp(-j*2pi*kn/N)}. With
 * {@code inverse=true} the sign of the exponent is flipped; no {@code 1/N}
 * scaling is applied (the IMDCT caller handles output scaling via its twiddle
 * tables).
 * <p>
 * Direct translation of {@code Echo/WmaPro/Fft.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class Fft {

    private final int n;
    private final int logN;
    private final float[] twCos;
    private final float[] twSin;
    private final int[] bitRev;

    public Fft(int n, boolean inverse) {
        if (n < 2 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("FFT size must be a power of two >= 2.");
        }

        this.n = n;
        this.logN = Integer.numberOfTrailingZeros(n);

        twCos = new float[n / 2];
        twSin = new float[n / 2];
        double dir = inverse ? 1.0 : -1.0;
        for (int k = 0; k < n / 2; k++) {
            double angle = dir * 2.0 * Math.PI * k / n;
            twCos[k] = (float) Math.cos(angle);
            twSin[k] = (float) Math.sin(angle);
        }

        bitRev = new int[n];
        for (int i = 0; i < n; i++) {
            int rev = 0;
            int v = i;
            for (int b = 0; b < logN; b++) {
                rev = (rev << 1) | (v & 1);
                v >>= 1;
            }
            bitRev[i] = rev;
        }
    }

    /**
     * Transform {@code data} in-place, starting at {@code off}. The region must
     * contain 2*N floats (N complex pairs). Decimation-in-time radix-2
     * butterflies.
     */
    public void transform(float[] data, int off) {
        if (data.length - off < n * 2) {
            throw new IllegalArgumentException("Data buffer too small for FFT size.");
        }

        // Bit-reverse permutation.
        for (int i = 0; i < n; i++) {
            int j = bitRev[i];
            if (j <= i) {
                continue;
            }
            float t0 = data[off + 2 * i];
            data[off + 2 * i] = data[off + 2 * j];
            data[off + 2 * j] = t0;
            float t1 = data[off + 2 * i + 1];
            data[off + 2 * i + 1] = data[off + 2 * j + 1];
            data[off + 2 * j + 1] = t1;
        }

        // log2(N) butterfly passes.
        for (int stage = 1; stage <= logN; stage++) {
            int m = 1 << stage;
            int half = m >> 1;
            int twStep = n / m;

            for (int g = 0; g < n; g += m) {
                for (int k = 0; k < half; k++) {
                    int twi = k * twStep;
                    float wRe = twCos[twi];
                    float wIm = twSin[twi];

                    int ia = off + (g + k) * 2;
                    int ib = off + (g + k + half) * 2;

                    float aRe = data[ia];
                    float aIm = data[ia + 1];
                    float bRe = data[ib];
                    float bIm = data[ib + 1];

                    float tRe = bRe * wRe - bIm * wIm;
                    float tIm = bRe * wIm + bIm * wRe;

                    data[ia] = aRe + tRe;
                    data[ia + 1] = aIm + tIm;
                    data[ib] = aRe - tRe;
                    data[ib + 1] = aIm - tIm;
                }
            }
        }
    }
}
