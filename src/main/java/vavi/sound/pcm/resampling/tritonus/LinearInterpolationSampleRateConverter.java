/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.tritonus;

import vavi.util.Debug;


/**
 * SampleRateConverter. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060206 nsano initial version <br>
 */
public class LinearInterpolationSampleRateConverter implements SampleRateConverter {

    /** */
    public void convert(float[] inSamples, double inSampleOffset, int inSampleCount, double increment, float[] outSamples, int outSampleOffset, int outSampleCount, float[] history, int historyLength) {
//Debug.println("convertLinearInterpolate(inSamples[" + inSamples.length + "], " + ((int) Math.floor(inSampleOffset)) + " to " + ((int) Math.floor(inSampleOffset + increment * (outSampleCount - 1))) + ", " + "outSamples[" + outSamples.length + "], " + outSampleOffset + " to " + (outSampleOffset + outSampleCount - 1) + ")");
//System.out.flush();
        for (int i = 0; i < outSampleCount; i++) {
            try {
                double dInIndex = inSampleOffset + increment * i - 1;
                int iInIndex = (int) Math.floor(dInIndex);
                double factor = 1.0d - (dInIndex - iInIndex);
                float value = 0;
                for (int x = 0; x < 2; x++) {
                    if (iInIndex >= inSampleCount) {
                        // we clearly need more samples !
//Debug.println("linear interpolation: INDEX OUT OF BOUNDS iInIndex=" + iInIndex + " inSampleCount=" + inSampleCount);
                    } else if (iInIndex < 0) {
                        int histIndex = iInIndex + historyLength;
                        if (histIndex >= 0) {
                            value += history[histIndex] * factor;
//Debug.println("linear interpolation: using history[" + iInIndex + "]");
                        } else {
//Debug.println("linear interpolation: history INDEX OUT OF BOUNDS iInIndex=" + iInIndex + " histIndex=" + histIndex + " history length=" + historyLength);
                        }
                    } else {
                        value += inSamples[iInIndex] * factor;
                    }
                    factor = 1 - factor;
                    iInIndex++;
                }
                outSamples[i + outSampleOffset] = value;

            } catch (ArrayIndexOutOfBoundsException aioobe) {
Debug.println("**** REAL INDEX OUT OF BOUNDS ****** outSamples[" + i + "]=inSamples[roundDown(" + inSampleOffset + ")=" + ((int) Math.floor(inSampleOffset)) + "];");
            }
            // inSampleOffset+=increment; <- this produces too much rounding
            // errors...
        }
    }
}

/* */
