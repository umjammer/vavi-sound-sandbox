/*
 * Copyright (c) 2009 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.packetcast.audio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tritonus.dsp.ais.AmplitudeAudioInputStream;


/**
 * AmplitudeAudioInputStreamTest.
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2009/09/08 nsano initial version <br>
 */
public class AmplitudeAudioInputStreamTest {

    static final String strSourceFilename = "src/test/resources/test.wav";

    static final String strTargetFilename = "tmp/out.wav";

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get("tmp"));
    }

    @Test
    public void test00() throws Exception {
        boolean bAmplitudeIsLog = true;
        float fAmplitude = 4f;
        File sourceFile = new File(strSourceFilename);
        File targetFile = new File(strTargetFilename);

        AudioInputStream sourceAudioInputStream = AudioSystem.getAudioInputStream(sourceFile);
        if (sourceAudioInputStream == null) {
            throw new IllegalStateException("cannot open audio file");
        }

        AudioFileFormat aff = AudioSystem.getAudioFileFormat(sourceFile);
        AudioFileFormat.Type targetType = aff.getType();

        AmplitudeAudioInputStream amplifiedAudioInputStream = new AmplitudeAudioInputStream(sourceAudioInputStream);

        /*
         * Here, we set the desired amplification.
         */
        if (bAmplitudeIsLog) {
            amplifiedAudioInputStream.setAmplitudeLog(fAmplitude);
        } else {
            amplifiedAudioInputStream.setAmplitudeLinear(fAmplitude);
        }

        /*
         * And finally, we are writing the amplified stream to a new file.
         */
        AudioSystem.write(amplifiedAudioInputStream, targetType, targetFile);
    }
}

/* */
