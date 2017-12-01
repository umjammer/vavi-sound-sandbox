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
public class SampleAndHoldSampleRateConverter implements SampleRateConverter {

    /** */
    public void convert(float[] inSamples, double inSampleOffset, int inSampleCount, double increment, float[] outSamples, int outSampleOffset, int outSampleCount, float[] history, int historyLength) {
Debug.println("convertSampleAndHold(inSamples[" + inSamples.length + "], " + ((int) Math.floor(inSampleOffset)) + " to " + ((int) Math.floor(inSampleOffset + increment * (outSampleCount - 1))) + ", " + "outSamples[" + outSamples.length + "], " + outSampleOffset + " to " + (outSampleOffset + outSampleCount - 1) + ")");
System.out.flush();
        for (int i = 0; i < outSampleCount; i++) {
            int iInIndex = (int) Math.floor(inSampleOffset + increment * i);
            if (iInIndex < 0) {
                outSamples[i + outSampleOffset] = history[iInIndex + historyLength];
Debug.println("convertSampleAndHold: using history[" + (iInIndex + historyLength) + " because inIndex=" + iInIndex);
            } else if (iInIndex >= inSampleCount) {
Debug.println("convertSampleAndHold: INDEX OUT OF BOUNDS outSamples[" + i + "]=inSamples[roundDown(" + inSampleOffset + ")=" + iInIndex + "];");
            } else {
                outSamples[i + outSampleOffset] = inSamples[iInIndex];
            }
        }
    }
}

/* */
