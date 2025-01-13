/*
 * Copyright (c) 2006 by ROHM, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.rohm;

/**
 * Rohm Resampler.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060131 nsano initial version <br>
 */
public class Resampler {

    /** number of bits per sample */
    private static final int nBitPerSample = 16;

    /** frequency */
    private final float nFreq;
    /** */
    private final float nSampleFreq;

    /**
     *
     * @param nSampleFreq PCM data sampling frequency (in)
     * @param nAdpcmFreq sampling frequency of ADPCM data after encoding. (out)
     */
    Resampler(float nSampleFreq, float nAdpcmFreq) {
        this.nFreq = nAdpcmFreq;
        this.nSampleFreq = nSampleFreq;
    }

    /**
     * assumed nBitPerSample = 16
     * @param pbyPcmData {@link #nBitPerSample} bit data array
     * @return the same bit as input data array
     */
    int[] resample(int[] pbyPcmData) {

        // number of bytes per sample
//        int nBytes = nBitPerSample / 8;

        // number of samples after frequency conversion.
        int nNewSampleNum = (int) (pbyPcmData.length * (nFreq / nSampleFreq));
//logger.log(Level.TRACE, nFreq + ", "+  nSampleFreq + ", " + pbyPcmData.length + ", " + nNewSampleNum);

        // memory allocation
        int[] pbyNewPcm = new int[nNewSampleNum];

        // frequency conversion.
        for (int i = 0; i < nNewSampleNum; i++) {

            // index value
            double dIndex = (double) i * pbyPcmData.length / nNewSampleNum;
            int nIndex1 = (int) dIndex;
            int nIndex2 = Math.min((nIndex1 + 1), pbyPcmData.length - 1);

            // multiplier calculation.
            double dRat1 = dIndex - nIndex1;
            double dRat2 = 1.0 - dRat1;

            // 8bit
            if (nBitPerSample == 8) {

                // values of index 1 and 2
                int c1 = pbyPcmData[nIndex1] - 128;
                int c2 = pbyPcmData[nIndex2] - 128;

                // value calculation
                double dRes = dRat1 * c2 + dRat2 * c1;
                if (dRes < -128) {
                    dRes = -128;
                }
                if (dRes > 127) {
                    dRes = 127;
                }

                // value assignment
                short n = (short) dRes;
                n = (short) (n + 128);

                pbyNewPcm[i] = (byte) n;
            }

            // 16bit.
            else {

//logger.log(Level.TRACE, nIndex1 + ", "+  nIndex2);
                // values of index 1 and 2
                int n1 = pbyPcmData[nIndex1];
                int n2 = pbyPcmData[nIndex2];

                // value calculation
                double dRes = dRat1 * n2 + dRat2 * n1;
                if (dRes < -32768) {
                    dRes = -32768;
                }
                if (dRes > 32767) {
                    dRes = 32767;
                }

                // value assignment
                short n = (short) dRes;

                pbyNewPcm[i] = n;
            }
        }

        return pbyNewPcm;
    }
}
