/*
 * This file is part of LAoE.
 *
 * LAoE is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * LAoE is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LAoE; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package vavi.sound.pcm.resampling.laoe;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * LAOE Resampler.
 *
 * TODO resample with interpolation
 *
 * @author olivier gaumann, neuchatel (switzerland)
 * @version 26.07.00 oli4 erster Entwurf<br>
 *          04.08.00 oli4 neuer Stil<br>
 *          26.10.00 oli4 neuer Stil<br>
 *          19.12.00 oli4 float audio samples<br>
 *          21.04.01 oli4 usage of toolkit<br>
 *          19.09.01 oli4 index-calculation double-precision<br>
 */
public class Resampler {

    private static final Logger logger = getLogger(Resampler.class.getName());

    /**
     * <li> sampleRateFactor = 1.0
     * <li> order = 2
     */
    public Resampler() {
    }

    /**
     * @param inRate input sample rate
     * @param outRate out put sample rate
     * @param order order of interpolate 0 ~ 3
     */
    public Resampler(float inRate, float outRate, int order) {
        this.sampleRateFactor = inRate / outRate;
        this.order = order;
    }

    /** samplerate factor */
    private double sampleRateFactor = 1.0;

    /** interpolation order 0 ~ 3 */
    private int order = 2;

    /**
     * performs constant resampling
     */
    public int[] resample(int[] samples) {
        int resampledLength = (int) (samples.length / sampleRateFactor);

        // create new samples
        int[] results = new int[resampledLength];

        // resampling indexing
        double oldIndex = 0;

        // resample each new point
        for (int i = 0; i < resampledLength; i++) {
            // resample
            if (((int) oldIndex + 1) < samples.length) {
                switch (order) {
                case 0:
                    results[i] = interpolate0(samples, oldIndex);
                    break;
                case 1:
                    results[i] = interpolate1(samples, oldIndex);
                    break;
                case 2:
                    results[i] = interpolate2(samples, oldIndex);
                    break;
                case 3:
                    results[i] = interpolate3(samples, oldIndex);
                    break;
                }
            } else {
                break; // end of source
            }

            // calculate next index
            oldIndex += sampleRateFactor;
        }

        // replace old samples with new samples
        return results;
    }

    /**
     * performs variable resampling
     */
    public int[] resample(int[] samples1, int[] samples2) {

        // calculate ressampled length
        int resampledLength = 0;
        for (int i = 0; i < samples1.length; i++) {
            try {
                resampledLength += 1 / samples2[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                resampledLength += 1;
            }
        }

        // create new samples
        int[] results = new int[resampledLength];

        // resampling indexing
        double oldIndex = 0;

        // resample each new point
        for (int i = 0; i < resampledLength; i++) {
            // resample
            if (((int) oldIndex + 1) < samples1.length) {
                switch (order) {
                case 0:
                    results[i] = interpolate0(samples1, oldIndex);
                    break;
                case 1:
                    results[i] = interpolate1(samples1, oldIndex);
                    break;
                case 2:
                    results[i] = interpolate2(samples1, oldIndex);
                    break;
                case 3:
                    results[i] = interpolate3(samples1, oldIndex);
                    break;
                }
            } else {
                break; // end of source
            }

            // calculate next index
            // resample curve range ok ?
            try {
                if (samples2[(int) oldIndex] > 0) {
                    oldIndex += samples2[(int) oldIndex];
                } else {
                    oldIndex += 1;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                oldIndex += 1;
            }
        }

        // replace old samples with new samples
        return results;
    }

    /**
     * zeroth order interpolation
     *
     * @param data seen as circular buffer when array out of bounds
     */
    private static int interpolate0(int[] data, double index) {
        try {
            return data[((int) index) % data.length];
        } catch (ArrayIndexOutOfBoundsException e) {
            return 0;
        }
    }

    /**
     * first order interpolation
     *
     * @param data seen as circular buffer when array out of bounds
     */
    private static int interpolate1(int[] data, double index) {
        try {
            int ip = (int) index;
            double fp = index - ip;

            return (int) (data[ip % data.length] * (1 - fp) + data[(ip + 1) % data.length] * fp);
        } catch (ArrayIndexOutOfBoundsException e) {
logger.log(Level.INFO, e.getMessage(), e);
            return 0;
        }
    }

    // ayy (a comment of moustique...)

    /**
     * second order interpolation
     *
     * @param data seen as circular buffer when array out of bounds
     */
    private static int interpolate2(int[] data, double index) {
        try {
            // Newton's 2nd order interpolation
            int ip = (int) index;
            double fp = index - ip;
//logger.log(Level.TRACE, "%f, %d".formatted(fp, ip));
            double d0 = data[ip % data.length];
            double d1 = data[(ip + 1) % data.length];
            double d2 = data[(ip + 2) % data.length];

            double a0 = d0;
            double a1 = d1 - d0;
            double a2 = (d2 - d1 - a1) / 2;

            return (int) (a0 + a1 * fp + a2 * fp * (fp - 1));

        } catch (ArrayIndexOutOfBoundsException e) {
logger.log(Level.INFO, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * third order interpolation
     *
     * @param data seen as circular buffer when array out of bounds
     */
    private static int interpolate3(int[] data, double index) {
        try {
            // cubic hermite interpolation
            int ip = (int) index;
            double fp = index - ip;

            double dm1 = data[(ip - 1) % data.length];
            double d0 = data[ip % data.length];
            double d1 = data[(ip + 1) % data.length];
            double d2 = data[(ip + 2) % data.length];

            double a = (3 * (d0 - d1) - dm1 + d2) / 2;
            double b = 2 * d1 + dm1 - (5 * d0 + d2) / 2;
            double c = (d1 - dm1) / 2;

            return (int) ((((a * fp) + b) * fp + c) * fp + data[ip % data.length]);

        } catch (ArrayIndexOutOfBoundsException e) {
logger.log(Level.INFO, e.getMessage(), e);
            return 0;
        }
    }
}
