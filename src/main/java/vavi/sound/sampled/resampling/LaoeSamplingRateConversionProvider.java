/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;

import vavi.sound.pcm.resampling.laoe.LaoeInputStream;


/**
 * LaoeSamplingRateConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 * @see LaoeInputStream
 */
public class LaoeSamplingRateConversionProvider extends SamplingRateConversionProvider {

    @Override
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (!Boolean.parseBoolean(System.getProperty("vavi.sound.sampled.spi.laoe", "false")))
            return false;

        return super.isConversionSupported(targetFormat, sourceFormat);
    }

    @Override
    protected InputStream createStream(InputStream in, float inRate, float outRate) throws IOException {
        return new LaoeInputStream(in, inRate, outRate);
    }
}
