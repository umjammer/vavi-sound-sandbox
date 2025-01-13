/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.spi.MixerProvider;

import static java.lang.System.getLogger;


/**
 * RococoaMixerProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/21 umjammer initial version <br>
 */
public class RococoaMixerProvider extends MixerProvider {

    private static final Logger logger = getLogger(RococoaMixerProvider.class.getName());

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
