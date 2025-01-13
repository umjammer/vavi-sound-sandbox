/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * MmlMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 241218 nsano initial version <br>
 */
public class MmlMidiDeviceProvider extends MidiDeviceProvider {

    private static final Logger logger = getLogger(MmlMidiDeviceProvider.class.getName());

    /** */
    public final static int MANUFACTURER_ID = 0x5e;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { MmlSynthesizer.info };

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    @Override
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

        if (info == MmlSynthesizer.info) {
logger.log(Level.DEBUG, "★1 info: " + info);
            MmlSynthesizer synthesizer = new MmlSynthesizer();
            return synthesizer;
        } else {
logger.log(Level.DEBUG, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}
