/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MDCTTest - Unit test for MDCT/IMDCT implementation.
 */
class MDCTTest {

    /**
     * Test FFT with a simple impulse input using reflection.
     * For a 4-point FFT with input [1, 0, 0, 0], the DFT should be [1, 1, 1, 1].
     */
    @Test
    void testFFTSimple() throws Exception {
        // Create a minimal MDCT to access the FFT
        // nbits = 4, so mdctSize = 16, fftSize = 4, fftBits = 2
        MDCT mdct = new MDCT(4, true, -1.0f);

        // Use reflection to access private fftCalc method
        java.lang.reflect.Method fftCalc = MDCT.class.getDeclaredMethod("fftCalc", float[].class);
        fftCalc.setAccessible(true);

        // Create a simple 4-point input in bit-reversed order
        // For natural order [1,0,0,0], bit-reversed is [1,0,0,0] (index 0 maps to 0)
        // Actually for DIT FFT with bit-reversed input:
        // Natural input: x[0]=1, x[1]=0, x[2]=0, x[3]=0
        // Bit-reversed:  x[0]=1, x[2]=0, x[1]=0, x[3]=0
        // So z = [1,0,0,0] (complex interleaved: re0,im0,re1,im1,re2,im2,re3,im3)
        float[] z = new float[8];  // 4 complex numbers
        z[0] = 1.0f;  // z[0].re = 1
        z[1] = 0.0f;  // z[0].im = 0
        // Rest are zeros

        System.out.println("FFT Simple Test:");
        System.out.println("Input (bit-reversed): [" + z[0] + "+" + z[1] + "j, " + z[2] + "+" + z[3] + "j, " + z[4] + "+" + z[5] + "j, " + z[6] + "+" + z[7] + "j]");

        fftCalc.invoke(mdct, (Object) z);

        System.out.println("Output (natural order):");
        for (int i = 0; i < 4; i++) {
            System.out.printf("  X[%d] = %.6f + %.6fj%n", i, z[2*i], z[2*i+1]);
        }

        // For input [1,0,0,0], the DFT should be [1,1,1,1]
        // (DC component at all frequencies)
        float tolerance = 1e-5f;
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0f, z[2*i], tolerance, "Real part of X[" + i + "]");
            assertEquals(0.0f, z[2*i+1], tolerance, "Imag part of X[" + i + "]");
        }
    }

    /**
     * Test FFT with a sinusoid input.
     */
    @Test
    void testFFTSinusoid() throws Exception {
        MDCT mdct = new MDCT(4, true, -1.0f);
        java.lang.reflect.Method fftCalc = MDCT.class.getDeclaredMethod("fftCalc", float[].class);
        fftCalc.setAccessible(true);

        // Get the revtab for bit-reversal
        java.lang.reflect.Field revtabField = MDCT.class.getDeclaredField("revtab");
        revtabField.setAccessible(true);
        int[] revtab = (int[]) revtabField.get(mdct);

        // Create input: x[k] = cos(2*PI*k/4) for k=0,1,2,3
        // This should produce a peak at frequency bin 1
        float[] naturalX = new float[8];  // 4 complex, all real
        for (int k = 0; k < 4; k++) {
            naturalX[2*k] = (float) Math.cos(2 * Math.PI * k / 4);
            naturalX[2*k+1] = 0;
        }
        System.out.println("\nFFT Sinusoid Test:");
        System.out.println("Natural order input: [" + naturalX[0] + ", " + naturalX[2] + ", " + naturalX[4] + ", " + naturalX[6] + "]");

        // Bit-reverse the input
        float[] z = new float[8];
        for (int k = 0; k < 4; k++) {
            z[2*revtab[k]] = naturalX[2*k];
            z[2*revtab[k]+1] = naturalX[2*k+1];
        }
        System.out.println("Bit-reversed input: [" + z[0] + ", " + z[2] + ", " + z[4] + ", " + z[6] + "]");

        fftCalc.invoke(mdct, (Object) z);

        System.out.println("FFT Output:");
        for (int i = 0; i < 4; i++) {
            float mag = (float) Math.sqrt(z[2*i]*z[2*i] + z[2*i+1]*z[2*i+1]);
            System.out.printf("  X[%d] = %.3f + %.3fj (mag=%.3f)%n", i, z[2*i], z[2*i+1], mag);
        }

        // For cos(2*PI*k/4), the DFT should have peaks at bins 1 and 3
        // X[0] = 0, X[1] = 2, X[2] = 0, X[3] = 2 (for a 4-point DFT)
    }

    /**
     * Test IMDCT with DC-only input.
     * Input: [1, 0, 0, 0, 0, 0, 0, 0] (first coefficient = 1, rest = 0)
     * This tests the low-frequency response.
     */
    @Test
    void testIMDCTLowFrequency() {
        int nbits = 4; // mdctSize = 16
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;   // 16
        int n2 = n >> 1;       // 8

        float[] input = new float[n2];  // 8 coefficients
        input[0] = 1000.0f;  // DC-like component

        float[] output = new float[n2]; // 8 output samples
        mdct.imdctHalf(output, 0, input, 0);

        System.out.println("IMDCT Low Frequency Test (input[0]=1000, rest=0):");
        System.out.println("Output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  [%d] = %.6f%n", i, output[i]);
        }

        // All outputs should be non-zero for a DC input
        float sum = 0;
        for (int i = 0; i < n2; i++) {
            sum += Math.abs(output[i]);
        }
        System.out.println("Sum of absolute values: " + sum);
        assertTrue(sum > 0, "IMDCT output should not be all zeros");
    }

    /**
     * Test IMDCT with high-frequency input.
     * Input: [0, 0, 0, 0, 0, 0, 0, 1] (last coefficient = 1, rest = 0)
     * This tests the high-frequency response.
     */
    @Test
    void testIMDCTHighFrequency() {
        int nbits = 4; // mdctSize = 16
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;   // 16
        int n2 = n >> 1;       // 8

        float[] input = new float[n2];  // 8 coefficients
        input[n2 - 1] = 1000.0f;  // High-frequency component

        float[] output = new float[n2]; // 8 output samples
        mdct.imdctHalf(output, 0, input, 0);

        System.out.println("\nIMDCT High Frequency Test (input[7]=1000, rest=0):");
        System.out.println("Output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  [%d] = %.6f%n", i, output[i]);
        }

        // All outputs should be non-zero
        float sum = 0;
        for (int i = 0; i < n2; i++) {
            sum += Math.abs(output[i]);
        }
        System.out.println("Sum of absolute values: " + sum);
        assertTrue(sum > 0, "IMDCT output should not be all zeros");
    }

    /**
     * Test IMDCT with all-ones input to verify overall response.
     */
    @Test
    void testIMDCTAllOnes() {
        int nbits = 4; // mdctSize = 16
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;   // 16
        int n2 = n >> 1;       // 8

        float[] input = new float[n2];
        for (int i = 0; i < n2; i++) {
            input[i] = 1000.0f;
        }

        float[] output = new float[n2];
        mdct.imdctHalf(output, 0, input, 0);

        System.out.println("\nIMDCT All-Ones Test:");
        System.out.println("Output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  [%d] = %.6f%n", i, output[i]);
        }

        float maxAbs = 0;
        for (int i = 0; i < n2; i++) {
            if (Math.abs(output[i]) > maxAbs) maxAbs = Math.abs(output[i]);
        }
        System.out.println("Max absolute value: " + maxAbs);
    }

    /**
     * Test IMDCT with alternating +/- pattern (highest frequency).
     */
    @Test
    void testIMDCTAlternating() {
        int nbits = 4; // mdctSize = 16
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;   // 16
        int n2 = n >> 1;       // 8

        float[] input = new float[n2];
        for (int i = 0; i < n2; i++) {
            input[i] = (i % 2 == 0) ? 1000.0f : -1000.0f;
        }

        float[] output = new float[n2];
        mdct.imdctHalf(output, 0, input, 0);

        System.out.println("\nIMDCT Alternating Test (+1000, -1000, ...):");
        System.out.println("Output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  [%d] = %.6f%n", i, output[i]);
        }
    }

    /**
     * Test IMDCT with larger size (256 points, similar to TwinVQ).
     */
    @Test
    void testIMDCTLargerSize() {
        int nbits = 9; // mdctSize = 512
        MDCT mdct = new MDCT(nbits, true, -1.0f / (1 << 15));  // Similar scale to TwinVQ

        int n = 1 << nbits;   // 512
        int n2 = n >> 1;       // 256

        // Create a simple spectrum: first few coefficients non-zero
        float[] input = new float[n2];
        input[0] = 10000.0f;   // DC
        input[1] = 5000.0f;    // Low freq 1
        input[2] = 2500.0f;    // Low freq 2
        input[3] = 1250.0f;    // Low freq 3
        // Rest are zero

        float[] output = new float[n2];
        mdct.imdctHalf(output, 0, input, 0);

        System.out.println("\nIMDCT 512-point Test (low-freq content):");
        System.out.println("First 20 output samples:");
        for (int i = 0; i < 20; i++) {
            System.out.printf("  [%d] = %.6f%n", i, output[i]);
        }

        // Check for smooth low-frequency output (adjacent samples should be similar)
        float maxDiff = 0;
        for (int i = 1; i < n2; i++) {
            float diff = Math.abs(output[i] - output[i-1]);
            if (diff > maxDiff) maxDiff = diff;
        }
        System.out.println("Max difference between adjacent samples: " + maxDiff);

        // For low-frequency content, adjacent samples should be fairly similar
        // (not oscillating wildly)
    }

    /**
     * Detailed trace test to find where the error occurs.
     * Compares Java IMDCT with theoretical MDCT-IV basis function.
     */
    @Test
    void testIMDCTTrace() throws Exception {
        int nbits = 4; // Small size for easy tracing: mdctSize = 16
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        // Access private fields via reflection for tracing
        java.lang.reflect.Field tcosField = MDCT.class.getDeclaredField("tcos");
        java.lang.reflect.Field tsinField = MDCT.class.getDeclaredField("tsin");
        java.lang.reflect.Field revtabField = MDCT.class.getDeclaredField("revtab");
        tcosField.setAccessible(true);
        tsinField.setAccessible(true);
        revtabField.setAccessible(true);
        float[] tcos = (float[]) tcosField.get(mdct);
        float[] tsin = (float[]) tsinField.get(mdct);
        int[] revtab = (int[]) revtabField.get(mdct);

        int n = 16, n2 = 8, n4 = 4, n8 = 2;

        System.out.println("\n=== IMDCT Trace Test (n=16) ===");
        System.out.println("\nTwiddle factors (with scale):");
        for (int i = 0; i < n4; i++) {
            System.out.printf("  tcos[%d] = %.6f, tsin[%d] = %.6f%n", i, tcos[i], i, tsin[i]);
        }
        System.out.println("\nRevtab (bit-reversal table):");
        for (int i = 0; i < n4; i++) {
            System.out.printf("  revtab[%d] = %d%n", i, revtab[i]);
        }

        // Create input with single frequency bin
        float[] input = new float[n2];  // 8 coefficients
        input[1] = 100.0f;  // Put energy at index 1

        System.out.println("\nInput spectrum:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  input[%d] = %.2f%n", i, input[i]);
        }

        // Manually trace pre-rotation
        System.out.println("\nPre-rotation trace:");
        float[] z = new float[n4 * 2];
        for (int k = 0; k < n4; k++) {
            int j = revtab[k];
            float inRe = input[n2 - 1 - 2 * k];
            float inIm = input[2 * k];
            float re = inRe * tcos[k] - inIm * tsin[k];
            float im = inRe * tsin[k] + inIm * tcos[k];
            z[2 * j] = re;
            z[2 * j + 1] = im;
            System.out.printf("  k=%d: inRe=input[%d]=%.2f, inIm=input[%d]=%.2f, j=%d%n",
                k, n2 - 1 - 2 * k, inRe, 2 * k, inIm, j);
            System.out.printf("       z[%d] = (%.4f, %.4f)%n", j, re, im);
        }

        System.out.println("\nz after pre-rotation (before FFT):");
        for (int i = 0; i < n4; i++) {
            System.out.printf("  z[%d] = (%.4f, %.4f)%n", i, z[2*i], z[2*i+1]);
        }

        // Invoke FFT
        java.lang.reflect.Method fftCalc = MDCT.class.getDeclaredMethod("fftCalc", float[].class);
        fftCalc.setAccessible(true);
        fftCalc.invoke(mdct, (Object) z);

        System.out.println("\nz after FFT:");
        for (int i = 0; i < n4; i++) {
            System.out.printf("  z[%d] = (%.4f, %.4f)%n", i, z[2*i], z[2*i+1]);
        }

        // Trace post-rotation (FFmpeg-style: writes to z positions directly)
        System.out.println("\nPost-rotation trace (FFmpeg-style):");
        float[] output = new float[n2];
        for (int k = 0; k < n8; k++) {
            int idx0 = n8 - k - 1;
            int idx1 = n8 + k;
            float r0 = z[2 * idx0];
            float i0 = z[2 * idx0 + 1];
            float r1 = z[2 * idx1];
            float i1 = z[2 * idx1 + 1];

            System.out.printf("  k=%d: z[%d]=(%.4f,%.4f), z[%d]=(%.4f,%.4f)%n",
                k, idx0, r0, i0, idx1, r1, i1);

            // FFmpeg: CMUL(r0_new, i1_new, z.im, z.re, tsin, tcos) for idx0
            //         CMUL(r1_new, i0_new, z.im, z.re, tsin, tcos) for idx1
            float r0_new = i0 * tsin[idx0] - r0 * tcos[idx0];
            float i1_new = i0 * tcos[idx0] + r0 * tsin[idx0];
            float r1_new = i1 * tsin[idx1] - r1 * tcos[idx1];
            float i0_new = i1 * tcos[idx1] + r1 * tsin[idx1];

            // Write to z positions (cross-coupled: i0_new -> idx0.im, i1_new -> idx1.im)
            output[2 * idx0] = r0_new;
            output[2 * idx0 + 1] = i0_new;
            output[2 * idx1] = r1_new;
            output[2 * idx1 + 1] = i1_new;

            System.out.printf("       output[%d]=%.4f, output[%d]=%.4f, output[%d]=%.4f, output[%d]=%.4f%n",
                2 * idx0, output[2 * idx0],
                2 * idx0 + 1, output[2 * idx0 + 1],
                2 * idx1, output[2 * idx1],
                2 * idx1 + 1, output[2 * idx1 + 1]);
        }

        System.out.println("\nFinal IMDCT output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  output[%d] = %.4f%n", i, output[i]);
        }

        // Also run through the actual imdctHalf method to compare
        float[] actualOutput = new float[n2];
        mdct.imdctHalf(actualOutput, 0, input, 0);
        System.out.println("\nActual imdctHalf output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  actual[%d] = %.4f (diff = %.4f)%n", i, actualOutput[i], actualOutput[i] - output[i]);
        }
    }

    /**
     * Compare FFT-based IMDCT with direct mathematical formula.
     * IMDCT-IV formula for the first N/2 samples:
     * x[n] = Σ(k=0 to N/2-1) X[k] * cos(π/N * (n + 0.5 + N/4) * (2k + 1))
     */
    @Test
    void testIMDCTVsReference() {
        int nbits = 4; // mdctSize = 16
        int N = 16;
        int N2 = N / 2;  // 8

        // Test input: single frequency bin
        float[] input = new float[N2];
        input[1] = 100.0f;

        // Compute reference IMDCT directly
        float[] reference = new float[N2];
        for (int n = 0; n < N2; n++) {
            double sum = 0;
            for (int k = 0; k < N2; k++) {
                // IMDCT-IV basis: cos(π/N * (n + 0.5 + N/4) * (2k + 1))
                double angle = Math.PI / N * (n + 0.5 + N / 4.0) * (2 * k + 1);
                sum += input[k] * Math.cos(angle);
            }
            reference[n] = (float) sum;
        }

        System.out.println("\n=== Reference IMDCT vs Java IMDCT ===");
        System.out.println("Input: input[1] = 100.0");
        System.out.println("\nReference (direct formula):");
        for (int i = 0; i < N2; i++) {
            System.out.printf("  ref[%d] = %.6f%n", i, reference[i]);
        }

        // Now compute using our MDCT class
        // Scale = -1.0f means inverse, with sqrt(|scale|) applied to twiddles
        // For a fair comparison, we use scale = -1.0f/N to match normalization
        // Actually let's use scale = -1.0 first to see raw output
        MDCT mdct = new MDCT(nbits, true, -1.0f);
        float[] actual = new float[N2];
        mdct.imdctHalf(actual, 0, input, 0);

        System.out.println("\nActual (FFT-based, scale=-1.0):");
        for (int i = 0; i < N2; i++) {
            System.out.printf("  act[%d] = %.6f (ratio=%.4f)%n", i, actual[i],
                reference[i] != 0 ? actual[i] / reference[i] : 0);
        }

        // Check correlation
        double sumRef = 0, sumAct = 0, sumRefSq = 0, sumActSq = 0, sumProd = 0;
        for (int i = 0; i < N2; i++) {
            sumRef += reference[i];
            sumAct += actual[i];
            sumRefSq += reference[i] * reference[i];
            sumActSq += actual[i] * actual[i];
            sumProd += reference[i] * actual[i];
        }
        double meanRef = sumRef / N2;
        double meanAct = sumAct / N2;
        double varRef = sumRefSq / N2 - meanRef * meanRef;
        double varAct = sumActSq / N2 - meanAct * meanAct;
        double cov = sumProd / N2 - meanRef * meanAct;
        double corr = cov / Math.sqrt(varRef * varAct);
        System.out.println("\nCorrelation: " + corr);
    }

    /**
     * Trace FFmpeg's exact post-rotation algorithm.
     * FFmpeg does: CMULS(z[i].im, z[i].re, z[i].re, z[i].im, tsin[i], tcos[i])
     * Which computes: new_z.im = old_z.re * tsin - old_z.im * tcos
     *                 new_z.re = old_z.re * tcos + old_z.im * tsin
     * Then writes: output[2k] = z[n8+k].im, output[2k+1] = z[n8-k-1].im,
     *              output[n2-2k-1] = z[n8+k].re, output[n2-2k-2] = z[n8-k-1].re
     */
    @Test
    void testFFmpegPostRotation() throws Exception {
        int nbits = 4; // mdctSize = 16
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        // Access private fields via reflection for tracing
        java.lang.reflect.Field tcosField = MDCT.class.getDeclaredField("tcos");
        java.lang.reflect.Field tsinField = MDCT.class.getDeclaredField("tsin");
        java.lang.reflect.Field revtabField = MDCT.class.getDeclaredField("revtab");
        tcosField.setAccessible(true);
        tsinField.setAccessible(true);
        revtabField.setAccessible(true);
        float[] tcos = (float[]) tcosField.get(mdct);
        float[] tsin = (float[]) tsinField.get(mdct);
        int[] revtab = (int[]) revtabField.get(mdct);

        int n = 16, n2 = 8, n4 = 4, n8 = 2;

        // Create input
        float[] input = new float[n2];
        input[1] = 100.0f;

        // Pre-rotation (same as before)
        float[] z = new float[n4 * 2];
        for (int k = 0; k < n4; k++) {
            int j = revtab[k];
            float inRe = input[n2 - 1 - 2 * k];
            float inIm = input[2 * k];
            float re = inRe * tcos[k] - inIm * tsin[k];
            float im = inRe * tsin[k] + inIm * tcos[k];
            z[2 * j] = re;
            z[2 * j + 1] = im;
        }

        // FFT
        java.lang.reflect.Method fftCalc = MDCT.class.getDeclaredMethod("fftCalc", float[].class);
        fftCalc.setAccessible(true);
        fftCalc.invoke(mdct, (Object) z);

        System.out.println("\n=== FFmpeg Post-Rotation Analysis ===");
        System.out.println("\nz after FFT:");
        for (int i = 0; i < n4; i++) {
            System.out.printf("  z[%d] = (re=%.4f, im=%.4f)%n", i, z[2*i], z[2*i+1]);
        }

        // Now trace FFmpeg's post-rotation
        // FFmpeg does CMULS on z[i] in-place, then writes to output
        // But we can compute the final output directly:
        // output[2*k] = z[n8+k].re * tsin[n8+k] - z[n8+k].im * tcos[n8+k]
        // output[2*k+1] = z[n8-k-1].re * tsin[n8-k-1] - z[n8-k-1].im * tcos[n8-k-1]
        // output[n2-2*k-1] = z[n8+k].re * tcos[n8+k] + z[n8+k].im * tsin[n8+k]
        // output[n2-2*k-2] = z[n8-k-1].re * tcos[n8-k-1] + z[n8-k-1].im * tsin[n8-k-1]
        float[] ffmpegOutput = new float[n2];
        for (int k = 0; k < n8; k++) {
            int idx1 = n8 + k;       // z index for output[2k], output[n2-2k-1]
            int idx0 = n8 - k - 1;   // z index for output[2k+1], output[n2-2k-2]
            float r1 = z[2 * idx1];
            float i1 = z[2 * idx1 + 1];
            float r0 = z[2 * idx0];
            float i0 = z[2 * idx0 + 1];

            // After CMULS: new_im = r * tsin - i * tcos, new_re = r * tcos + i * tsin
            ffmpegOutput[2 * k] = r1 * tsin[idx1] - i1 * tcos[idx1];
            ffmpegOutput[2 * k + 1] = r0 * tsin[idx0] - i0 * tcos[idx0];
            ffmpegOutput[n2 - 2 * k - 1] = r1 * tcos[idx1] + i1 * tsin[idx1];
            ffmpegOutput[n2 - 2 * k - 2] = r0 * tcos[idx0] + i0 * tsin[idx0];
        }

        System.out.println("\nFFmpeg-style output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  ffmpeg[%d] = %.6f%n", i, ffmpegOutput[i]);
        }

        // Compare with Java output
        float[] javaOutput = new float[n2];
        mdct.imdctHalf(javaOutput, 0, input, 0);
        System.out.println("\nCurrent Java output:");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  java[%d] = %.6f%n", i, javaOutput[i]);
        }

        // Compute reference
        float[] reference = new float[n2];
        for (int nn = 0; nn < n2; nn++) {
            double sum = 0;
            for (int kk = 0; kk < n2; kk++) {
                double angle = Math.PI / n * (nn + 0.5 + n / 4.0) * (2 * kk + 1);
                sum += input[kk] * Math.cos(angle);
            }
            reference[nn] = (float) sum;
        }
        System.out.println("\nReference (direct formula):");
        for (int i = 0; i < n2; i++) {
            System.out.printf("  ref[%d] = %.6f%n", i, reference[i]);
        }
    }

    /**
     * Verify IMDCT linearity: IMDCT(a*x) = a*IMDCT(x)
     */
    @Test
    void testIMDCTLinearity() {
        int nbits = 6; // mdctSize = 64
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;
        int n2 = n >> 1;

        float[] input1 = new float[n2];
        float[] input2 = new float[n2];
        float scale = 2.0f;

        // Random-ish input
        for (int i = 0; i < n2; i++) {
            input1[i] = (float) Math.sin(i * 0.1) * 100;
            input2[i] = input1[i] * scale;
        }

        float[] output1 = new float[n2];
        float[] output2 = new float[n2];

        mdct.imdctHalf(output1, 0, input1, 0);
        mdct.imdctHalf(output2, 0, input2, 0);

        System.out.println("\nIMDCT Linearity Test:");
        float maxError = 0;
        for (int i = 0; i < n2; i++) {
            float expected = output1[i] * scale;
            float actual = output2[i];
            float error = Math.abs(expected - actual);
            if (error > maxError) {
                maxError = error;
            }
        }
        System.out.println("Max linearity error: " + maxError);
        assertTrue(maxError < 1e-4, "IMDCT should be linear");
    }

    /**
     * Test MDCT -> IMDCT round-trip.
     * Apply windowed signal to MDCT, then IMDCT, should recover original signal.
     */
    @Test
    void testMDCTRoundTrip() {
        int nbits = 6; // mdctSize = 64
        float scale = 1.0f / 32;  // Forward MDCT
        float invScale = -1.0f / 32;  // Inverse MDCT

        MDCT forwardMdct = new MDCT(nbits, false, scale);
        MDCT inverseMdct = new MDCT(nbits, true, invScale);

        int n = 1 << nbits;   // 64
        int n2 = n >> 1;       // 32

        // Create a time-domain signal (full n samples)
        float[] timeSignal = new float[n];
        for (int i = 0; i < n; i++) {
            // Low-frequency sine wave
            timeSignal[i] = (float) Math.sin(2 * Math.PI * 2 * i / n) * 1000;
        }

        // Apply forward MDCT
        float[] spectrum = new float[n2];
        forwardMdct.mdctCalc(spectrum, 0, timeSignal, 0);

        System.out.println("\nMDCT Round-Trip Test:");
        System.out.println("First 10 spectrum coefficients:");
        for (int i = 0; i < 10; i++) {
            System.out.printf("  spectrum[%d] = %.3f%n", i, spectrum[i]);
        }

        // Apply inverse MDCT
        float[] recovered = new float[n2];
        inverseMdct.imdctHalf(recovered, 0, spectrum, 0);

        System.out.println("First 10 recovered samples (should match input[0..9] scaled):");
        System.out.println("Index\tOriginal\tRecovered");
        for (int i = 0; i < 10; i++) {
            // Note: imdctHalf only returns first n/2 samples
            System.out.printf("  %d\t%.3f\t\t%.3f%n", i, timeSignal[i], recovered[i]);
        }
    }

    /**
     * Test IMDCT output sample pattern to check for alternating issues.
     */
    @Test
    void testIMDCTOutputPattern() {
        int nbits = 6; // mdctSize = 64
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;
        int n2 = n >> 1;

        // Create a simple spectrum: DC + low frequency
        float[] spectrum = new float[n2];
        spectrum[0] = 100.0f;  // DC-ish
        spectrum[1] = 50.0f;   // Low freq

        float[] output = new float[n2];
        mdct.imdctHalf(output, 0, spectrum, 0);

        System.out.println("\n=== IMDCT Output Pattern Test ===");
        System.out.println("Input: spectrum[0]=100, spectrum[1]=50, rest=0");
        System.out.println("\nOutput pattern (checking for alternating):");
        System.out.println("Index\tValue\t\tDiff from prev");
        float prevVal = 0;
        int alternatingCount = 0;
        for (int i = 0; i < n2; i++) {
            float diff = output[i] - prevVal;
            System.out.printf("  %d\t%.4f\t\t%.4f%n", i, output[i], diff);
            // Check if sign alternates rapidly
            if (i > 0 && (output[i] * output[i-1] < 0)) {
                alternatingCount++;
            }
            prevVal = output[i];
        }
        System.out.println("Sign alternations: " + alternatingCount + " out of " + (n2-1));

        // For low-frequency content, we shouldn't have many sign alternations
        assertTrue(alternatingCount < n2 / 4, "Too many sign alternations for low-frequency content");
    }

    /**
     * Test vector_fmul_window to verify overlap-add windowing is correct.
     */
    @Test
    void testVectorFmulWindow() {
        LibAV.AVFloatDSPContext fdsp = new LibAV.AVFloatDSPContext(0);

        // Simple test: 4 samples from each source, half-window length = 2
        int len = 2;  // Half window length
        float[] dst = new float[2 * len];  // Output: 4 samples
        float[] src0 = new float[len];     // Previous frame's end: 2 samples
        float[] src1 = new float[len];     // Current frame's start: 2 samples
        float[] win = new float[2 * len];  // Full window: 4 samples

        // Initialize with simple values
        // Sine window for len=2: sin(pi*(i+0.5)/(2*2)) for i=0,1,2,3
        for (int i = 0; i < 2 * len; i++) {
            win[i] = (float) Math.sin(Math.PI * (i + 0.5) / (2 * len));
        }
        System.out.println("\n=== Vector FMUL Window Test ===");
        System.out.println("Window values:");
        for (int i = 0; i < 2 * len; i++) {
            System.out.printf("  win[%d] = %.6f%n", i, win[i]);
        }

        // src0 = [1, 2] (previous frame's last samples)
        src0[0] = 1.0f;
        src0[1] = 2.0f;

        // src1 = [3, 4] (current frame's first samples)
        src1[0] = 3.0f;
        src1[1] = 4.0f;

        System.out.println("src0 = [" + src0[0] + ", " + src0[1] + "]");
        System.out.println("src1 = [" + src1[0] + ", " + src1[1] + "]");

        // Apply window
        fdsp.vector_fmul_window(dst, 0, src0, 0, src1, 0, win, len);

        System.out.println("Output:");
        for (int i = 0; i < 2 * len; i++) {
            System.out.printf("  dst[%d] = %.6f%n", i, dst[i]);
        }

        // Verify manually:
        // According to FFmpeg's formula (after pointer adjustments):
        // i=-2, j=1: s0=src0[0]=1, s1=src1[1]=4, wi=win[0], wj=win[3]
        //   dst[0] = 1*win[3] - 4*win[0]
        //   dst[3] = 1*win[0] + 4*win[3]
        // i=-1, j=0: s0=src0[1]=2, s1=src1[0]=3, wi=win[1], wj=win[2]
        //   dst[1] = 2*win[2] - 3*win[1]
        //   dst[2] = 2*win[1] + 3*win[2]
        float expected0 = src0[0] * win[3] - src1[1] * win[0];
        float expected3 = src0[0] * win[0] + src1[1] * win[3];
        float expected1 = src0[1] * win[2] - src1[0] * win[1];
        float expected2 = src0[1] * win[1] + src1[0] * win[2];

        System.out.println("\nExpected (manual calculation):");
        System.out.printf("  dst[0] = %.6f (1*%.4f - 4*%.4f)%n", expected0, win[3], win[0]);
        System.out.printf("  dst[1] = %.6f (2*%.4f - 3*%.4f)%n", expected1, win[2], win[1]);
        System.out.printf("  dst[2] = %.6f (2*%.4f + 3*%.4f)%n", expected2, win[1], win[2]);
        System.out.printf("  dst[3] = %.6f (1*%.4f + 4*%.4f)%n", expected3, win[0], win[3]);

        assertEquals(expected0, dst[0], 1e-5f, "dst[0]");
        assertEquals(expected1, dst[1], 1e-5f, "dst[1]");
        assertEquals(expected2, dst[2], 1e-5f, "dst[2]");
        assertEquals(expected3, dst[3], 1e-5f, "dst[3]");
    }

    /**
     * Test that compares Java IMDCT output structure with expected pattern.
     * For a pure sine wave input at a specific frequency, the output should
     * show that frequency in the time domain.
     */
    @Test
    void testIMDCTSineWave() {
        int nbits = 8; // mdctSize = 256
        MDCT mdct = new MDCT(nbits, true, -1.0f);

        int n = 1 << nbits;
        int n2 = n >> 1;

        // Put energy at a single frequency bin
        int freqBin = 10;  // Low frequency
        float[] input = new float[n2];
        input[freqBin] = 1000.0f;

        float[] output = new float[n2];
        mdct.imdctHalf(output, 0, input, 0);

        System.out.println("\nIMDCT Sine Wave Test (freq bin " + freqBin + "):");
        System.out.println("First 30 output samples:");
        for (int i = 0; i < 30; i++) {
            System.out.printf("  [%d] = %.6f%n", i, output[i]);
        }

        // Calculate zero crossings - should have a specific pattern for this frequency
        int zeroCrossings = 0;
        for (int i = 1; i < n2; i++) {
            if ((output[i] >= 0) != (output[i-1] >= 0)) {
                zeroCrossings++;
            }
        }
        System.out.println("Zero crossings: " + zeroCrossings);

        // For frequency bin k, we expect roughly k cycles in n/2 samples
        // So roughly 2*k zero crossings
        int expectedCrossings = 2 * freqBin;
        System.out.println("Expected ~" + expectedCrossings + " zero crossings");
    }
}
