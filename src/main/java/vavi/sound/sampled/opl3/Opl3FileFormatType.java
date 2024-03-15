/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opl3;

import javax.sound.sampled.AudioFileFormat;


/**
 * FileFormatTypes used by the OPL3 audio decoder.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201022 nsano initial version <br>
 */
public class Opl3FileFormatType extends AudioFileFormat.Type {

    /**
     * Specifies an OPL3 file.
     */
    public static final AudioFileFormat.Type MID = new Opl3FileFormatType("MID", "laa,cmf"); // TODO is comma separated is right way?
    public static final AudioFileFormat.Type DRO1 = new Opl3FileFormatType("DOSBox Raw OPL", "dro");
    public static final AudioFileFormat.Type DRO2 = new Opl3FileFormatType("DRO2", "dro");

    /**
     * Constructs a file type.
     *
     * @param name      the name of the OPL3 File Format.
     * @param extension the file extension for this OPL3 File Format.
     */
    public Opl3FileFormatType(String name, String extension) {
        super(name, extension);
    }
}
