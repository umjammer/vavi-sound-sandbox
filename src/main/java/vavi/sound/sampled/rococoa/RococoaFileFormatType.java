/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the ROCOCOA audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class RococoaFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an ROCOCOA file.
     */
    public static final AudioFileFormat.Type ROCOCOA = new RococoaFileFormatType("ROCOCOA", "caf");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the Rococoa File Format.
     * @param extension the file extension for this Flac File Format.
     */
    public RococoaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
