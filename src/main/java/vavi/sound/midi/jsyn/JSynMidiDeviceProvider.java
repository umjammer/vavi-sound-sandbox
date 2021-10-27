/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.jsyn;

import java.util.logging.Level;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.util.Debug;


/**
 * JSynMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201005 nsano initial version <br>
 */
public class JSynMidiDeviceProvider extends MidiDeviceProvider {

    /**  */
    public final static int MANUFACTURER_ID = 0x5e;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { JSynSynthesizer.info };

    /* */
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** */
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

        if (info == JSynSynthesizer.info) {
Debug.println(Level.FINE, "★1 info: " + info);
            JSynSynthesizer synthesizer = new JSynSynthesizer();
            return synthesizer;
        } else {
Debug.println(Level.FINE, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}

/* */
