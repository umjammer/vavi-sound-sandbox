/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.wma;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatType used by the WMA v1/v2 (ASF) audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class WmaFileFormatType extends AudioFileFormat.Type {

    /** Specifies a WMA (ASF) file. */
    public static final AudioFileFormat.Type WMA = new WmaFileFormatType("WMA", "wma");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the WMA File Format.
     * @param extension the file extension for this WMA File Format.
     */
    private WmaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
