/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.ldclep;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the LD-CELP audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240628 nsano initial version <br>
 */
public class LdCelpEncoding extends AudioFormat.Encoding {

    /** Specifies any LD-CELP encoded data. */
    public static final LdCelpEncoding G728 = new LdCelpEncoding("G728");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the LD-CELP encoding.
     */
    public LdCelpEncoding(String name) {
        super(name);
    }
}
