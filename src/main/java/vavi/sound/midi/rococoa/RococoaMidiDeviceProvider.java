/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.rococoa;

import java.util.logging.Level;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.util.Debug;


/**
 * RococoaMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201003 nsano initial version <br>
 */
public class RococoaMidiDeviceProvider extends MidiDeviceProvider {

    /** Apple Computer */
    public final static int MANUFACTURER_ID = 0x11;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { RococoaSynthesizer.info };

    /* */
    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** */
    @Override
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

        if (info == RococoaSynthesizer.info) {
Debug.println(Level.FINE, "★1 info: " + info);
            RococoaSynthesizer synthesizer = new RococoaSynthesizer();
            return synthesizer;
        } else {
Debug.println(Level.FINE, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}

/* */
