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

    /** 1 サンプル当たりの bit 数 */
    private final int nBitPerSample = 16;

    /** 周波数. */
    private final float nFreq;
    /** */
    private final float nSampleFreq;

    /**
     *
     * @param nSampleFreq PCM データサンプリング周波数. (in)
     * @param nAdpcmFreq エンコード後の ADPCM データのサンプリング周波数. (out)
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

        // 1サンプルのバイト数.
//        int nBytes = nBitPerSample / 8;

        // 周波数変換後のサンプル数.
        int nNewSampleNum = (int) (pbyPcmData.length * (nFreq / nSampleFreq));
//Debug.println(nFreq + ", "+  nSampleFreq + ", " + pbyPcmData.length + ", " + nNewSampleNum);

        // 領域確保.
        int[] pbyNewPcm = new int[nNewSampleNum];

        // 周波数変換.
        for (int i = 0; i < nNewSampleNum; i++) {

            // インデックス値.
            double dIndex = (double) i * pbyPcmData.length / nNewSampleNum;
            int nIndex1 = (int) dIndex;
            int nIndex2 = Math.min((nIndex1 + 1), pbyPcmData.length - 1);

            // 乗値算出.
            double dRat1 = dIndex - nIndex1;
            double dRat2 = 1.0 - dRat1;

            // 8bit.
            if (nBitPerSample == 8) {

                // インデックス1と2の値.
                int c1 = pbyPcmData[nIndex1] - 128;
                int c2 = pbyPcmData[nIndex2] - 128;

                // 値算出.
                double dRes = dRat1 * c2 + dRat2 * c1;
                if (dRes < -128) {
                    dRes = -128;
                }
                if (dRes > 127) {
                    dRes = 127;
                }

                // 値代入.
                short n = (short) dRes;
                n = (short) (n + 128);

                pbyNewPcm[i] = (byte) n;
            }

            // 16bit.
            else {

//Debug.println(nIndex1 + ", "+  nIndex2);
                // インデックス1と2の値.
                int n1 = pbyPcmData[nIndex1];
                int n2 = pbyPcmData[nIndex2];

                // 値算出.
                double dRes = dRat1 * n2 + dRat2 * n1;
                if (dRes < -32768) {
                    dRes = -32768;
                }
                if (dRes > 32767) {
                    dRes = 32767;
                }

                // 値代入.
                short n = (short) dRes;

                pbyNewPcm[i] = n;
            }
        }

        return pbyNewPcm;
    }
}
