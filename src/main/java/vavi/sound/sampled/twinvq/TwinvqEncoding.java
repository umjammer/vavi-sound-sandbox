/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.twinvq;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the TwinVQ audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260129 nsano initial version <br>
 */
public class TwinvqEncoding extends AudioFormat.Encoding {

    /** Specifies any iLBC encoded data. */
    public static final TwinvqEncoding TWINVQ = new TwinvqEncoding("TWINVQ");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the TwinVQ encoding.
     */
    private TwinvqEncoding(String name) {
        super(name);
    }
}
