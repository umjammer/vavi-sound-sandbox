/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import javax.sound.sampled.AudioFormat;


/**
 * Encoding that marks a PCM stream for the Shibatch Super Equalizer.
 * <p>
 * The byte layout is ordinary 16bit signed PCM; this encoding only tags a stream
 * so that {@link SSEEqualizingProvider} is selected to run it through the
 * equalizer, producing {@link AudioFormat.Encoding#PCM_SIGNED}.
 *
 * TODO consider more
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 250707 nsano initial version <br>
 */
public class SSEEncoding extends AudioFormat.Encoding {

    /** the equalize encoding */
    public static final SSEEncoding SSE_EQUALIZE = new SSEEncoding("SSE_EQUALIZE");

    /**
     * Constructs a new encoding.
     *
     * @param name name of the encoding
     */
    public SSEEncoding(String name) {
        super(name);
    }
}
