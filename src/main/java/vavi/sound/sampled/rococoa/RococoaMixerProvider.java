/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Properties;

import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.spi.MixerProvider;

import vavi.sound.midi.rococoa.RococoaSynthesizer;

import static java.lang.System.getLogger;


/**
 * RococoaMixerProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/21 umjammer initial version <br>
 */
public class RococoaMixerProvider extends MixerProvider {

    private static final Logger logger = getLogger(RococoaMixerProvider.class.getName());

    static {
        try {
            try (InputStream is = RococoaMixerProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound-sandbox/pom.properties")) {
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
    public Info[] getMixerInfo() {
        return new Info[] {RococoaMixer.mixerInfo};
    }

    @Override
    public Mixer getMixer(Info info) {
        if (info == RococoaMixer.mixerInfo) {
logger.log(Level.DEBUG, "★1 info: " + info);
            RococoaMixer mixer = new RococoaMixer();
            return mixer;
        } else {
logger.log(Level.DEBUG, "not suitable for this provider: " + info);
            throw new IllegalArgumentException("info is not suitable for this provider");
        }
    }
}
