/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the ROCOCOA audio decoder.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class RcococaEncoding extends AudioFormat.Encoding {

    /** Specifies any ROCOCOA encoded data. */
    public static final RcococaEncoding ROCOCOA = new RcococaEncoding("ROCOCOA");

    /**
     * Constructs a new encoding.
     * 
     * @param name Name of the ROCOCOA encoding.
     */
    public RcococaEncoding(String name) {
        super(name);
    }
}

/* */
