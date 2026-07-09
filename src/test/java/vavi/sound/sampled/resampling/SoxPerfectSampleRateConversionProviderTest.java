/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import vavi.sound.pcm.resampling.sox.PerfectResamplerInputStream;
import vavi.util.Debug;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * SoxPerfectSampleRateConversionProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-09 nsano initial version <br>
 */
public class SoxPerfectSampleRateConversionProviderTest {

    String inFile = "src/test/resources/mono.wav";

    @BeforeAll
    static void setup() throws IOException {
        // SoxPolyphaseSampleRateConversionProviderTest is off as default
        System.setProperty("vavi.sound.sampled.spi.sox_perfect", "true");
    }

    @AfterAll
    static void tearDown() throws IOException {
        System.setProperty("vavi.sound.sampled.spi.sox_perfect", "false");
    }

    /**
     * `--add-exports=java.desktop/javax.sound.sampled` and
     * `--add-open=java.desktop/javax.sound.sampled` are needed
     */
    @Test
    public void test1() throws Exception {

        //
        int outSamplingRate = 8000;

        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
Debug.println("IN: " + sourceAis.getFormat());

        AudioFormat inAudioFormat = sourceAis.getFormat();
        AudioFormat outAudioFormat = new AudioFormat(
                inAudioFormat.getEncoding(),
                outSamplingRate,
                inAudioFormat.getSampleSizeInBits(),
                inAudioFormat.getChannels(),
                inAudioFormat.getFrameSize(),
                outSamplingRate,
                inAudioFormat.isBigEndian());

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        // resampled stream by sox polyphase
        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
Debug.println("OUT: " + secondAis.getFormat());

        assertInstanceOf(AudioInputStream.class, secondAis);

        Field stream = AudioInputStream.class.getDeclaredField("stream");
        stream.setAccessible(true);
        InputStream is = (InputStream) stream.get(secondAis);

Debug.println(is.getClass().getName());
        assertInstanceOf(PerfectResamplerInputStream.class, is);

        assertEquals(outSamplingRate, secondAis.getFormat().getSampleRate());
    }
}
