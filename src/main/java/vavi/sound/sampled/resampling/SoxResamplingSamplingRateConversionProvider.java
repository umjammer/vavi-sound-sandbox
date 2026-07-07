/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.IOException;
import java.io.InputStream;

import vavi.sound.pcm.resampling.sox.ResamplerInputStream;


/**
 * SoxResamplingSamplingRateConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 * @see ResamplerInputStream
 */
public class SoxResamplingSamplingRateConversionProvider extends SamplingRateConversionProvider {

    @Override
    protected InputStream createStream(InputStream in, float inRate, float outRate) throws IOException {
        return new ResamplerInputStream(in, inRate, outRate);
    }
}
