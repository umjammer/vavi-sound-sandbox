/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.opl3.MidPlayer.FileType;


/**
 * Opl3MidiFileReader.
 *
 * TODO wip
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201023 nsano initial version <br>
 */
public class Opl3MidiFileReader extends BasicMidiFileReader {

    @Override
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        try {
            FileType type = FileType.getFileType(is);
            return null;
        } catch (NoSuchElementException e) {
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

    static class OplSequence extends Sequence {

        public OplSequence(float divisionType, int resolution) throws InvalidMidiDataException {
            super(divisionType, resolution);
            // TODO Auto-generated constructor stub
        }
        
    }
}

/* */
