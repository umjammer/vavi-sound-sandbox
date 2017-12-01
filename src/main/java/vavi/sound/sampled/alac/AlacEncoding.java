/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.alac;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the ALAC audio decoder.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class AlacEncoding extends AudioFormat.Encoding {

    /** Specifies any ALAC encoded data. */
    public static final AlacEncoding ALAC = new AlacEncoding("ALAC");

    /**
     * Constructs a new encoding.
     * 
     * @param name Name of the ALAC encoding.
     */
    public AlacEncoding(String name) {
        super(name);
    }
}

/* */
