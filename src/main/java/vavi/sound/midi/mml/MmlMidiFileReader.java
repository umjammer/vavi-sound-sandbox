/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.Sequence;

import jp.or.rim.kt.kemusiro.sound.MMLException;
import vavi.sound.midi.BasicMidiFileReader;
import vavi.sound.midi.mfi.MfiMidiFileReader;

import static java.lang.System.getLogger;


/**
 * MmlMidiFileReader implemented by vavi.sound.mfi.vavi package
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241206 nsano initial version <br>
 */
public class MmlMidiFileReader extends BasicMidiFileReader {

    private static final Logger logger = getLogger(MfiMidiFileReader.class.getName());

    @Override
    public Sequence getSequence(InputStream is) throws InvalidMidiDataException, IOException {

        if (!is.markSupported()) {
            throw new IOException("mark not supported: " + is);
        }

        try {

            is.mark(8192);

            MmlSequence mml = new MmlSequence();
            mml.setScore(is);

            return mml.toMidiSequence();
        } catch (MMLException e) {
logger.log(Level.DEBUG, e);
logger.log(Level.TRACE, e.getMessage(), e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } finally {
            try {
                is.reset();
            } catch (IOException e) {
logger.log(Level.DEBUG, e);
            }
        }
    }
}
