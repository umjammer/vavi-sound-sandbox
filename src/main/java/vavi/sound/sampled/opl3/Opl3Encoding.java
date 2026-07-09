/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opl3;

import javax.sound.sampled.AudioFormat;


/**
 * Encodings used by the OPL3 audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201022 nsano initial version <br>
 */
public class Opl3Encoding extends AudioFormat.Encoding {

    /**
     * Constructs a new encoding.
     *
     * @param name Name of the OPL3 encoding.
     */
    public Opl3Encoding(String name) {
        super(name);
    }
}
