/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.rococoa;

import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;


/**
 * RococaFormatConversionProviderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060726 nsano initial version <br>
 * @see "https://github.com/iterate-ch/rococoa"
 */
@EnabledOnOs(OS.MAC)
public class RococoaFormatConversionProviderTest {

    static final String inFile = "/test.caf";

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    @Test
    @Disabled("not implemented yet")
    @DisplayName("directly")
    public void test0() throws Exception {

        //
        Path path = Paths.get(RococoaFormatConversionProviderTest.class.getResource(inFile).toURI());
        AudioInputStream sourceAis = new RococoaAudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
            44100,
            16,
            2,
            true,
            false);
Debug.println("OUT: " + outAudioFormat);

//        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = new RococoaFormatConversionProvider().getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, secondAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(secondAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (true) {
            int r = secondAis.read(buf, 0, 1024);
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
    @Disabled("not implemented yet")
    @DisplayName("by spi")
    public void test1() throws Exception {

        //
        Path path = Paths.get(RococoaFormatConversionProviderTest.class.getResource(inFile).toURI());
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));

        AudioFormat inAudioFormat = sourceAis.getFormat();
Debug.println("IN: " + inAudioFormat);
        AudioFormat outAudioFormat = new AudioFormat(
            44100,
            16,
            2,
            true,
            false);
Debug.println("OUT: " + outAudioFormat);

//        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, secondAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(secondAis.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (true) {
            int r = secondAis.read(buf, 0, 1024);
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
    @Disabled("not completed yet")
    public void test2() throws Exception {
        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
    }
}
