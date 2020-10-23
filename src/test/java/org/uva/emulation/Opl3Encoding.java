/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.uva.emulation;


import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the OPL3 audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class Opl3Encoding extends AudioFormat.Encoding {

    /** Specifies any OPL3 encoded data. */
    public static final Opl3Encoding MID = new Opl3Encoding("MID");
    public static final Opl3Encoding DRO1 = new Opl3Encoding("DRO1");
    public static final Opl3Encoding DRO2 = new Opl3Encoding("DRO2");

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the OPL3 encoding.
     */
    public Opl3Encoding(String name) {
        super(name);
    }
}

/* */
