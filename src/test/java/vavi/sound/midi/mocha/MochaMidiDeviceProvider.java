/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mocha;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * MochaMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class MochaMidiDeviceProvider extends MidiDeviceProvider {

    private static final Logger logger = getLogger(MochaMidiDeviceProvider.class.getName());

    /** */
    public final static int MANUFACTURER_ID = 0x5e;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { MochaSynthesizer.info };

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** */
    @Override
    public MidiDevice getDevice(MidiDevice.Info info) {

        if (info == MochaSynthesizer.info) {
logger.log(Level.DEBUG, "★1 info: " + info);
            return new MochaSynthesizer();
        } else {
logger.log(Level.DEBUG, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}
