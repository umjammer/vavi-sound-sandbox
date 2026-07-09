/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.xma;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the WMA Pro / XMA audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class XmaEncoding extends AudioFormat.Encoding {

    /** Specifies any WMA Pro / XMA encoded data. */
    public static final XmaEncoding XMA = new XmaEncoding("XMA");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the WMA encoding.
     */
    private XmaEncoding(String name) {
        super(name);
    }
}
