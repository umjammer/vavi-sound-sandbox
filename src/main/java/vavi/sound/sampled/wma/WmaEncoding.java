/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.wma;

import javax.sound.sampled.AudioFormat;


/**
 * Encoding used by the WMA v1/v2 (ASF) audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class WmaEncoding extends AudioFormat.Encoding {

    /** Specifies any WMA v1/v2 encoded data (ASF container). */
    public static final WmaEncoding WMA = new WmaEncoding("WMA");

    /**
     * Constructs a new encoding.
     *
     * @param name name of the WMA encoding.
     */
    private WmaEncoding(String name) {
        super(name);
    }
}
