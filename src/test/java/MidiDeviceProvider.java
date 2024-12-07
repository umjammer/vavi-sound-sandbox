/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.util.ServiceLoader;

import javax.sound.midi.MidiDevice.Info;


/**
 * MidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 020703 umjammer initial version <br>
 */
public class MidiDeviceProvider {

    /**
     * This main class only runs a test to list the found ports by each
     * MidiDeviceProvider
     */
    public static void main(String[] args) {
        ServiceLoader<javax.sound.midi.spi.MidiDeviceProvider> serviceLoader =
                ServiceLoader.load(javax.sound.midi.spi.MidiDeviceProvider.class);
        for (javax.sound.midi.spi.MidiDeviceProvider midiDeviceProvider : serviceLoader) {
            Info[] deviceInfo = midiDeviceProvider.getDeviceInfo();
            System.err.println(midiDeviceProvider.getClass().getName() + ": " + deviceInfo.length);
            int i = 0;
            for (Info info : deviceInfo) {
                System.err.println("[" + i++ + "]: " + info.getName());
            }
            System.err.println("---------------");
        }
    }
}
