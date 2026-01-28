/*
 * MDCT/IMDCT transforms
 * Copyright (c) 2002 Fabrice Bellard
 *
 * This file is part of Libav.
 *
 * Libav is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * Libav is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Libav; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package vavi.sound.twinvq;

/**
 * MDCT/IMDCT transforms ported from libav.
 */
public class MDCT {

    private final int mdctBits;
    private final int mdctSize;
    private final float[] tcos;
    private final float[] tsin;
    private final int[] revtab;

    // FFT context
    private final int fftBits;
    private final int fftSize;
    private final float[] fftCosTab;
    private final float[] fftSinTab;

    public MDCT(int nbits, boolean inverse, float scale) {
        this.mdctBits = nbits;
        this.mdctSize = 1 << nbits;
        int n = mdctSize;
        int n4 = n >> 2;

        // FFT size is n/4 for MDCT
        this.fftBits = nbits - 2;
        this.fftSize = 1 << fftBits;

        tcos = new float[n4];
        tsin = new float[n4];
        revtab = new int[n4];

        // Initialize twiddle factors for MDCT
        float theta = 1.0f / 8.0f + (scale < 0 ? n4 : 0);
        // Use sqrt(scale) so total scaling is scale (applied in pre and post rotation)
        float absScale = (float) Math.sqrt(Math.abs(scale));
        for (int i = 0; i < n4; i++) {
            float alpha = (float) (2 * Math.PI * (i + theta) / n);
            tcos[i] = (float) (-Math.cos(alpha) * absScale);
            tsin[i] = (float) (-Math.sin(alpha) * absScale);
        }

        // Initialize FFT tables
        // For inverse FFT (used in IMDCT), use +sin instead of -sin
        fftCosTab = new float[fftSize / 2];
        fftSinTab = new float[fftSize / 2];
        for (int i = 0; i < fftSize / 2; i++) {
            double angle = 2 * Math.PI * i / fftSize;
            fftCosTab[i] = (float) Math.cos(angle);
            // inverse=true (for IMDCT): use +sin(angle), inverse=false: use -sin(angle)
            fftSinTab[i] = (float) (inverse ? Math.sin(angle) : -Math.sin(angle));
        }

        // Initialize FFT permutation table (bit-reversal for Cooley-Tukey DIT FFT)
        initRevtab();
    }

    private void initRevtab() {
        int n = fftSize;
        // For Cooley-Tukey DIT FFT, input must be bit-reversed
        // revtab[k] gives the bit-reversed position for input k
        for (int i = 0; i < n; i++) {
            revtab[i] = bitReverse(i, fftBits);
        }
    }

    private static int bitReverse(int v, int bits) {
        int r = 0;
        for (int i = 0; i < bits; i++) {
            r = (r << 1) | (v & 1);
            v >>= 1;
        }
        return r;
    }

    /**
     * Compute FFT in-place on complex array z (interleaved real/imag).
     * Note: Input is expected to already be in bit-reversed order (done by MDCT pre-rotation).
     * Output will be in natural order.
     */
    private void fftCalc(float[] z) {
        int n = fftSize;

        // NO bit-reversal permutation here!
        // The MDCT pre-rotation already writes to bit-reversed positions using revtab.
        // A DIT FFT with bit-reversed input produces natural-order output.

        // Cooley-Tukey FFT (decimation-in-time)
        for (int m = 2; m <= n; m <<= 1) {
            int mh = m >> 1;
            int step = n / m;
            for (int k = 0; k < n; k += m) {
                int tabIdx = 0;
                for (int j = 0; j < mh; j++) {
                    int idx0 = k + j;
                    int idx1 = k + j + mh;

                    float re0 = z[2 * idx0];
                    float im0 = z[2 * idx0 + 1];
                    float re1 = z[2 * idx1];
                    float im1 = z[2 * idx1 + 1];

                    float cos = fftCosTab[tabIdx];
                    float sin = fftSinTab[tabIdx];

                    // Butterfly: compute W * z[idx1] where W = e^{-j*angle}
                    // With sin stored as -sin(angle), this computes:
                    // tre = re1*cos(angle) + im1*sin(angle) (real part of W * z1)
                    // tim = im1*cos(angle) - re1*sin(angle) (imag part of W * z1)
                    float tre = re1 * cos - im1 * sin;
                    float tim = re1 * sin + im1 * cos;

                    z[2 * idx0] = re0 + tre;
                    z[2 * idx0 + 1] = im0 + tim;
                    z[2 * idx1] = re0 - tre;
                    z[2 * idx1 + 1] = im0 - tim;

                    tabIdx += step;
                }
            }
        }
    }

    /**
     * Compute inverse MDCT (IMDCT) of size N = 2^nbits, returning first N/2 samples.
     *
     * @param output output array (size N/2)
     * @param op     output offset
     * @param input  input array (size N/2)
     * @param ip     input offset
     */
    public void imdctHalf(float[] output, int op, float[] input, int ip) {
        int n = mdctSize;
        int n2 = n >> 1;
        int n4 = n >> 2;
        int n8 = n >> 3;

        float[] z = new float[n4 * 2]; // Complex array (n4 complex numbers)

        // Pre-rotation and bit-reversal combined
        // FFmpeg CMUL: z.re = inRe * tcos - inIm * tsin, z.im = inRe * tsin + inIm * tcos
        for (int k = 0; k < n4; k++) {
            int j = revtab[k];
            float inRe = input[ip + n2 - 1 - 2 * k];
            float inIm = input[ip + 2 * k];
            float re = inRe * tcos[k] - inIm * tsin[k];
            float im = inRe * tsin[k] + inIm * tcos[k];
            z[2 * j] = re;
            z[2 * j + 1] = im;
        }

        // FFT calculation
        fftCalc(z);

        // Post-rotation and reordering (exact FFmpeg ff_imdct_half algorithm)
        // FFmpeg does in-place modification of z array:
        //   CMUL(r0, i1, z[n8-k-1].im, z[n8-k-1].re, tsin[n8-k-1], tcos[n8-k-1]);
        //   CMUL(r1, i0, z[n8+k].im, z[n8+k].re, tsin[n8+k], tcos[n8+k]);
        //   z[n8-k-1].re = r0;  z[n8-k-1].im = i0;  // Note: i0 from idx1!
        //   z[n8+k].re = r1;    z[n8+k].im = i1;    // Note: i1 from idx0!
        // Where CMUL(dre, dim, are, aim, bre, bim) computes:
        //   dre = are * bre - aim * bim
        //   dim = are * bim + aim * bre
        // The output array is just the modified z array in natural complex order.
        for (int k = 0; k < n8; k++) {
            int idx0 = n8 - k - 1;
            int idx1 = n8 + k;
            // Read z values (r0,i0 from idx0; r1,i1 from idx1)
            float r0 = z[2 * idx0];      // z[idx0].re
            float i0 = z[2 * idx0 + 1];  // z[idx0].im
            float r1 = z[2 * idx1];      // z[idx1].re
            float i1 = z[2 * idx1 + 1];  // z[idx1].im

            // CMUL(r0_new, i1_new, i0, r0, tsin[idx0], tcos[idx0])
            //   r0_new = i0 * tsin[idx0] - r0 * tcos[idx0]
            //   i1_new = i0 * tcos[idx0] + r0 * tsin[idx0]
            float r0_new = i0 * tsin[idx0] - r0 * tcos[idx0];
            float i1_new = i0 * tcos[idx0] + r0 * tsin[idx0];

            // CMUL(r1_new, i0_new, i1, r1, tsin[idx1], tcos[idx1])
            //   r1_new = i1 * tsin[idx1] - r1 * tcos[idx1]
            //   i0_new = i1 * tcos[idx1] + r1 * tsin[idx1]
            float r1_new = i1 * tsin[idx1] - r1 * tcos[idx1];
            float i0_new = i1 * tcos[idx1] + r1 * tsin[idx1];

            // Write to z positions (which IS the output in FFmpeg)
            // z[idx0].re = r0_new, z[idx0].im = i0_new (cross-coupled!)
            // z[idx1].re = r1_new, z[idx1].im = i1_new (cross-coupled!)
            output[op + 2 * idx0] = r0_new;
            output[op + 2 * idx0 + 1] = i0_new;
            output[op + 2 * idx1] = r1_new;
            output[op + 2 * idx1 + 1] = i1_new;
        }
    }

    /**
     * Compute full inverse MDCT.
     *
     * @param output output array (size N)
     * @param op     output offset
     * @param input  input array (size N/2)
     * @param ip     input offset
     */
    public void imdctCalc(float[] output, int op, float[] input, int ip) {
        int n = mdctSize;
        int n2 = n >> 1;
        int n4 = n >> 2;

        imdctHalf(output, op + n4, input, ip);

        for (int k = 0; k < n4; k++) {
            output[op + k] = -output[op + n2 - k - 1];
            output[op + n - k - 1] = output[op + n2 + k];
        }
    }

    /**
     * Compute forward MDCT.
     *
     * @param output output array (size N/2)
     * @param op     output offset
     * @param input  input array (size N)
     * @param ip     input offset
     */
    public void mdctCalc(float[] output, int op, float[] input, int ip) {
        int n = mdctSize;
        int n2 = n >> 1;
        int n4 = n >> 2;
        int n8 = n >> 3;
        int n3 = 3 * n4;

        float[] x = new float[n4 * 2]; // Complex array

        // Pre-rotation
        for (int i = 0; i < n8; i++) {
            float re = -input[ip + 2 * i + n3] - input[ip + n3 - 1 - 2 * i];
            float im = -input[ip + n4 + 2 * i] + input[ip + n4 - 1 - 2 * i];
            int j = revtab[i];
            x[2 * j] = re * tcos[i] - im * tsin[i];
            x[2 * j + 1] = re * tsin[i] + im * tcos[i];

            re = input[ip + 2 * i] - input[ip + n2 - 1 - 2 * i];
            im = -input[ip + n2 + 2 * i] - input[ip + n - 1 - 2 * i];
            j = revtab[n8 + i];
            x[2 * j] = re * tcos[n8 + i] - im * tsin[n8 + i];
            x[2 * j + 1] = re * tsin[n8 + i] + im * tcos[n8 + i];
        }

        // FFT calculation
        fftCalc(x);

        // Post-rotation
        for (int i = 0; i < n8; i++) {
            float r0 = x[2 * (n8 - i - 1)];
            float i0 = x[2 * (n8 - i - 1) + 1];
            float r1 = x[2 * (n8 + i)];
            float i1 = x[2 * (n8 + i) + 1];

            output[op + 2 * i] = -r1 * tsin[n8 + i] - i1 * tcos[n8 + i];
            output[op + 2 * i + 1] = -r0 * tsin[n8 - i - 1] - i0 * tcos[n8 - i - 1];
            output[op + n2 - 2 - 2 * i] = r0 * tcos[n8 - i - 1] - i0 * tsin[n8 - i - 1];
            output[op + n2 - 1 - 2 * i] = r1 * tcos[n8 + i] - i1 * tsin[n8 + i];
        }
    }
}
