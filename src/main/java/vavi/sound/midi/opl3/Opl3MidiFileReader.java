/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import vavi.sound.midi.BasicMidiFileReader;
import vavi.util.Debug;


/**
 * Opl3MidiFileReader implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201023 nsano initial version <br>
 */
public class Opl3MidiFileReader extends BasicMidiFileReader {

    /* */
    public Sequence getSequence(InputStream is)
        throws InvalidMidiDataException,
               IOException {

        try {
            if (!is.markSupported()) {
                throw new IOException("mark not supported: " + is);
            }

            is.mark(4);

            Sequence sequence = getSequence(is);
Debug.println(sequence);
            return sequence;
        } catch (Exception e) {
            is.reset();
Debug.println(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }
}

/* */
