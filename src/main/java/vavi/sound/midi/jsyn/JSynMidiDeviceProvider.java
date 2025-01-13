/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * JSynMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201005 nsano initial version <br>
 */
public class JSynMidiDeviceProvider extends MidiDeviceProvider {

    private static final Logger logger = getLogger(JSynMidiDeviceProvider.class.getName());

    /** */
    public final static int MANUFACTURER_ID = 0x5e;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { JSynSynthesizer.info };

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** */
    @Override
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

        if (info == JSynSynthesizer.info) {
logger.log(Level.DEBUG, "★1 info: " + info);
            JSynSynthesizer synthesizer = new JSynSynthesizer();
            return synthesizer;
        } else {
logger.log(Level.DEBUG, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}
