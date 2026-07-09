/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import javax.sound.sampled.AudioFormat;


/**
 * Encoding that marks a PCM stream for Michael Kohn's peak normalizer.
 * <p>
 * The byte layout is ordinary 16bit / 8bit signed PCM; this encoding only tags a
 * stream so that {@link KohnNormalizingProvider} is selected to peak-normalize it
 * back to {@link AudioFormat.Encoding#PCM_SIGNED}.
 *
 * TODO consider more
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 */
public class KohnEncoding extends AudioFormat.Encoding {

    /** the normalize encoding */
    public static final KohnEncoding KOHN_NORMALIZE = new KohnEncoding("KOHN_NORMALIZE");

    /**
     * Constructs a new encoding.
     *
     * @param name name of the encoding
     */
    public KohnEncoding(String name) {
        super(name);
    }
}
