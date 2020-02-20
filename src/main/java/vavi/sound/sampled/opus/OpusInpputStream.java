/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opus;

import java.io.IOException;
import java.io.InputStream;

import org.gagravarr.ogg.OggFile;
import org.gagravarr.opus.OpusFile;


/**
 * OpusInpputStream.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/11/21 umjammer initial version <br>
 */
public class OpusInpputStream extends OpusFile {

    /**
     * Opens the given stream for reading (OGG only)
     */
    public OpusInpputStream(InputStream in) throws IOException {
        super(new OggFile(in));
    }
}

/* */
