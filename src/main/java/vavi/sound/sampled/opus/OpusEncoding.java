/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opus;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the Opus audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class OpusEncoding extends AudioFormat.Encoding {

    /** Specifies any Opus encoded data. */
    public static final OpusEncoding OPUS = new OpusEncoding("OPUS");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the Opus encoding.
     */
    public OpusEncoding(String name) {
        super(name);
    }
}
