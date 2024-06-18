/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opus;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the OPUS audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class OpusFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an OPUS file.
     */
    public static final AudioFileFormat.Type OPUS = new OpusFileFormatType("OPUS", "ogg");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the OPUS File Format.
     * @param extension the file extension for this OPUS File Format.
     */
    public OpusFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
