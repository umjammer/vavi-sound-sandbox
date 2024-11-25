/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the cocoa audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050722 nsano initial version <br>
 */
public class RococoaFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies a cocoa file.
     */
    public static final AudioFileFormat.Type ROCOCOA = new RococoaFileFormatType("ROCOCOA", "caf");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the cocoa File Format.
     * @param extension the file extension for this Flac File Format.
     */
    private RococoaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
