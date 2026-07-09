/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.sound.pcm.resampling.sox.PolyphaseInputStream;


/**
 * SoxPolyphaseSamplingRateConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 * @see PolyphaseInputStream
 */
public class SoxPolyphaseSamplingRateConversionProvider extends SamplingRateConversionProvider {

    @Override
    public boolean isConversionSupported(Encoding targetEncoding, AudioFormat sourceFormat) {
        if (!Boolean.parseBoolean(System.getProperty("vavi.sound.sampled.spi.sox_polyphase", "false")))
            return false;

        return super.isConversionSupported(targetEncoding, sourceFormat);
    }

    @Override
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (!Boolean.parseBoolean(System.getProperty("vavi.sound.sampled.spi.sox_polyphase", "false")))
            return false;

        return super.isConversionSupported(targetFormat, sourceFormat);
    }

    @Override
    protected InputStream createStream(InputStream in, float inRate, float outRate) throws IOException {
        return new PolyphaseInputStream(in, inRate, outRate);
    }
}
