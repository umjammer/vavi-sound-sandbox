/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.opl3;

import java.util.logging.Level;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.util.Debug;


/**
 * Opl3MidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201028 nsano initial version <br>
 */
public class Opl3MidiDeviceProvider extends MidiDeviceProvider {

    /** */
    public final static int MANUFACTURER_ID = 0x43;

    /** */
    private static final MidiDevice.Info[] infos = new MidiDevice.Info[] { Opl3Synthesizer.info };

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return infos;
    }

    /** */
    @Override
    public MidiDevice getDevice(MidiDevice.Info info)
        throws IllegalArgumentException {

        if (info == Opl3Synthesizer.info) {
Debug.println(Level.FINE, "★1 info: " + info);
            Opl3Synthesizer synthesizer = new Opl3Synthesizer();
            return synthesizer;
        } else {
Debug.println(Level.FINE, "★1 here: " + info);
            throw new IllegalArgumentException();
        }
    }
}
