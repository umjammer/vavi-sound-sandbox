/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ilbc;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the iLBC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240704 nsano initial version <br>
 */
public class IlbcEncoding extends AudioFormat.Encoding {

    /** Specifies any iLBC encoded data. */
    public static final IlbcEncoding iLBC = new IlbcEncoding("iLBC");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the iLBC encoding.
     */
    public IlbcEncoding(String name) {
        super(name);
    }
}
