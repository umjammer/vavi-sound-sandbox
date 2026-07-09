/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mml;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;
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

    static {
        try {
            try (InputStream is = MmlMidiDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sandbox/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static final String version;

    /** */
    public final static int MANUFACTURER_ID = 0x5e;

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return new MidiDevice.Info[] { MmlSynthesizer.info };
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
