/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opus;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vavi.sound.sampled.adpcm.ms.MsEncoding;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * OpusFormatConversionProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/20 umjammer initial version <br>
 */
class OpusFormatConversionProviderTest {

    static final String inFile = "/test.opus";

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    @Test
    @DisplayName("directly")
    void test0() throws Exception {

        Path path = Paths.get(OpusFormatConversionProviderTest.class.getResource(inFile).toURI());
        AudioInputStream sourceAis = new OpusAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getSampleRate(),
            16,
            inAudioFormat.getChannels(),
            true,
            false);
Debug.println("OUT: " + outAudioFormat);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream pcmAis = new OpusFormatConversionProvider().getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(pcmAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (true) {
            int r = pcmAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("as spi")
    void test1() throws Exception {

        Path path = Paths.get(OpusFormatConversionProviderTest.class.getResource(inFile).toURI());
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
            inAudioFormat.getSampleRate(),
            16,
            inAudioFormat.getChannels(),
            true,
            false);
Debug.println("OUT: " + outAudioFormat);

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream pcmAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(pcmAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (true) {
            int r = pcmAis.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @Test
    @DisplayName("another input type 2")
    void test2() throws Exception {
        URL url = Paths.get("src/test/resources/" + inFile).toUri().toURL();
        AudioInputStream ais = AudioSystem.getAudioInputStream(url);
        assertEquals(MsEncoding.MS, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("another input type 3")
    void test3() throws Exception {
        File file = Paths.get("src/test/resources/" + inFile).toFile();
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        assertEquals(MsEncoding.MS, ais.getFormat().getEncoding());
    }

    @Test
    @DisplayName("when unsupported file coming")
    void test5() throws Exception {
        InputStream is = OpusFormatConversionProviderTest.class.getResourceAsStream("/test.caf");
        int available = is.available();
        UnsupportedAudioFileException e = assertThrows(UnsupportedAudioFileException.class, () -> {
Debug.println(is);
            AudioSystem.getAudioInputStream(is);
        });
Debug.println(e.getMessage());
        assertEquals(available, is.available()); // spi must not consume input stream even one byte
    }
}
