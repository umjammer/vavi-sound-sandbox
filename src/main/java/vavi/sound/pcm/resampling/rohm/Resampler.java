/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.rohm;


/**
 * Resampler. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060131 nsano initial version <br>
 */
public class Resampler {

    /** 1 �T���v��������� bit �� */
    private int nBitPerSample = 16;

    /** ���g��. */
    private float nFreq;
    /** */
    private float nSampleFreq;

    /**
     * 
     * @param nAdpcmFreq �G���R�[�h��� ADPCM �f�[�^�̃T���v�����O���g��.
     * @param nSampleFreq PCM �f�[�^�T���v�����O���g��.
     */
    Resampler(float nAdpcmFreq, float nSampleFreq) {
        this.nFreq = nAdpcmFreq;
        this.nSampleFreq = nSampleFreq;
    }

    /**
     */
    int[] resample(int[] pbyPcmData) {

        // 1�T���v���̃o�C�g��.
        int nBytes = nBitPerSample / 8;

        // ���g���ϊ���̃T���v����.
        int nNewSampleNum = (int) (pbyPcmData.length * nFreq / nSampleFreq);

        // �̈�m��.
        int[] pbyNewPcm = new int[nNewSampleNum * nBytes];

        // ���g���ϊ�.
        for (int i = 0; i < nNewSampleNum; i++) {

            // �C���f�b�N�X�l.
            double dIndex = (double) i * pbyPcmData.length / nNewSampleNum;
            int nIndex1 = (int) dIndex;
            int nIndex2 = ((nIndex1 + 1) > pbyPcmData.length) ? pbyPcmData.length : (nIndex1 + 1);

            // ��l�Z�o.
            double dRat1 = dIndex - nIndex1;
            double dRat2 = 1.0 - dRat1;

            // 8bit.
            if (nBitPerSample == 8) {

                // �C���f�b�N�X1��2�̒l.
                int c1 = pbyPcmData[nIndex1] - 128;
                int c2 = pbyPcmData[nIndex2] - 128;

                // �l�Z�o.
                double dRes = dRat1 * c2 + dRat2 * c1;
                if (dRes < -128) {
                    dRes = -128;
                }
                if (dRes > 127) {
                    dRes = 127;
                }

                // �l���.
                short n = (short) dRes;
                n += 128;

                pbyNewPcm[i] = (byte) n;
            }

            // 16bit.
            else {

                // �C���f�b�N�X1��2�̒l.
                int n1 = pbyPcmData[nIndex1];
                int n2 = pbyPcmData[nIndex2];

                // �l�Z�o.
                double dRes = dRat1 * n2 + dRat2 * n1;
                if (dRes < -32768) {
                    dRes = -32768;
                }
                if (dRes > 32767) {
                    dRes = 32767;
                }

                // �l���.
                short n = (short) dRes;

                System.arraycopy(n, 0, pbyNewPcm, i * nBytes, nBytes);
            }
        }

        return pbyNewPcm;
    }
}

/* */
