/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.avfoundation;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Path;

import org.rococoa.cocoa.foundation.NSDictionary;
import vavix.rococoa.avfoundation.AVAudioFormat.AVAudioCommonFormat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * AVAudioFileTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-01-20 nsano initial version <br>
 */
class AVAudioFileTest {

    private static final Logger logger = System.getLogger(AVAudioFile.class.getName());

    @Test
    void test1() throws Exception {
        AVAudioFile audioFile = AVAudioFile.init(Path.of("tmp", "test.aiff").toUri(), NSDictionary.emptyDictionary(), AVAudioCommonFormat.pcmFormatInt16.ordinal(), false);
logger.log(Level.INFO, audioFile.url().absoluteString());
        assertNotNull(audioFile);
    }
}
