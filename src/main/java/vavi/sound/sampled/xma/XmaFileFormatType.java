/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.xma;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the WMA Pro / XMA audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
public class XmaFileFormatType extends AudioFileFormat.Type {

    /** Specifies an XMA file (wma for WMA Pro). */
    public static final AudioFileFormat.Type XMA = new XmaFileFormatType("XMA", "xma,wma");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the XMA File Format.
     * @param extension the file extension for this XMA File Format.
     */
    private XmaFileFormatType(String name, String extension) {
        super(name, extension);
    }
}
