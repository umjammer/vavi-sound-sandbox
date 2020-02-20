/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.alac;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the ALAC audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
public class AlacFileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an ALAC file.
     */
    public static final AudioFileFormat.Type ALAC = new AlacFileFormatType("ALAC", "m4a");

    /**
     * Constructs a file type.
     * 
     * @param name the name of the ALAC File Format.
     * @param extension the file extension for this ALAC File Format.
     */
    public AlacFileFormatType(String name, String extension) {
        super(name, extension);
    }
}

/* */
