/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.tritonus;


/**
 * SampleRateConverter.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060206 nsano initial version <br>
 */
public interface SampleRateConverter {

    /**
     *
     * @param inSamples
     * @param inSampleOffset
     * @param inSampleCount
     * @param increment
     * @param outSamples
     * @param outSampleOffset
     * @param outSampleCount
     * @param history
     * @param historyLength
     */
    void convert(float[] inSamples, double inSampleOffset, int inSampleCount, double increment, float[] outSamples, int outSampleOffset, int outSampleCount, float[] history, int historyLength);

    /** */
    class Factory {
        /** TODO properties とかから指定できる */
        public static SampleRateConverter getInstance() {
            return new vavi.sound.pcm.resampling.tritonus.LinearInterpolationSampleRateConverter();
        }
    }
}

/* */
