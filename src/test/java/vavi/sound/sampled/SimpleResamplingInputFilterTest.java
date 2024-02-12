/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.*;


/**
 * SimpleResamplingInputFilterTest.
 *
 * @require tritonus:tritonus_remaining
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060202 nsano initial version <br>
 */
public class SimpleResamplingInputFilterTest {

    static final String inFile = "src/test/resources/test.wav";
    static final String outFile = "tmp/out.wav";

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get("tmp"));
    }

    @Test
    public void test1() throws Exception {
        // source: any Hz, any bit, any, any bytes/frame, any
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
Debug.println("IN: " + sourceAis.getFormat());
        final int outSamplingRate = 8000;
        AudioInputStream secondAis = new SimpleResamplingInputFilter(outSamplingRate).doFilter(sourceAis);
        AudioInputStream thirdAis = new MonauralInputFilter().doFilter(secondAis);

        AudioSystem.write(thirdAis, AudioFileFormat.Type.WAVE, new File(outFile));

        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        assertEquals(outSamplingRate, (int) resultAis.getFormat().getSampleRate());
    }
}

/* */
