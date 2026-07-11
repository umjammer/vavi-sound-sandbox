/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.openDoja;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * FuetrekMidiDeviceProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026/07/09 nsano initial version <br>
 */
public class FuetrekMidiDeviceProvider extends MidiDeviceProvider {

    private static final Logger logger = getLogger(FuetrekMidiDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = FuetrekMidiDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sandbox/pom.properties")) {
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

    @Override
    public MidiDevice.Info[] getDeviceInfo() {
        return new MidiDevice.Info[] { FuetrekSynthesizer.info };
    }

    @Override
    public MidiDevice getDevice(MidiDevice.Info info) throws IllegalArgumentException {
        if (info == FuetrekSynthesizer.info) {
            logger.log(Level.DEBUG, "info: " + info);
            return new FuetrekSynthesizer();
        } else {
            throw new IllegalArgumentException();
        }
    }
}
