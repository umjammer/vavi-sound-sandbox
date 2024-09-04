/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.opl3;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;
import static vavix.util.DelayedWorker.later;


/**
 * Opl3AudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/12 umjammer initial version <br>
 */
class Opl3AudioFileReaderTest {

    static long time = System.getProperty("vavi.test", "").equals("ide") ? 1000 * 1000 : 10 * 1000;

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume", "0.2"));

    @BeforeAll
    static void setup() {
        System.setProperty("vavi.sound.opl3.MidiFile", "true");
Debug.println("volume: " + volume);
    }

    @AfterAll
    static void teardown() {
        System.setProperty("vavi.sound.opl3.MidiFile", "false");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/opl3/ice_thnk.sci",
        "/opl3/samurai.dro",
        "/opl3/michaeld.cmf",
        "/opl3/mi2.laa",
        "/opl3/2.cmf",
        "/opl3/dro_v2.dro",
    })
    @DisplayName("direct, clip")
    @Disabled("slow, bec play after load all")
    void test0(String filename) throws Exception {
Debug.println("------------------------------------------ " + filename + " ------------------------------------------------");
        Path path = Paths.get(Opl3AudioFileReaderTest.class.getResource(filename).toURI());
        AudioInputStream originalAudioInputStream = new Opl3AudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat(
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Clip clip = (Clip) AudioSystem.getLine(info);
Debug.println(clip.getClass().getName());
        clip.addLineListener(event -> {
Debug.println("LINE: " + event.getType());
            if (event.getType().equals(LineEvent.Type.STOP)) {
                countDownLatch.countDown();
            }
        });
        clip.open(audioInputStream);
try {
 volume(clip, volume);
} catch (Exception e) {
 Debug.println("volume: " + e);
}
        clip.start();
if (!System.getProperty("vavi.test", "").equals("ide")) {
 Thread.sleep(time);
 clip.stop();
 Debug.println("not on ide");
} else {
            countDownLatch.await();
}
        clip.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/opl3/ice_thnk.sci",
        "/opl3/dro_v2.dro",
        "/opl3/samurai.dro",
        "/opl3/michaeld.cmf",
        "/opl3/mi2.laa",
        "/opl3/2.cmf",
    })
    @DisplayName("spi, clip")
    @Disabled("slow, bec play after load all")
    void test1(String filename) throws Exception {
Debug.println("------------------------------------------ " + filename + " ------------------------------------------------");
        Path path = Paths.get(Opl3AudioFileReaderTest.class.getResource(filename).toURI());
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat(
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Clip clip = (Clip) AudioSystem.getLine(info);
Debug.println(clip.getClass().getName());
        clip.addLineListener(event -> {
Debug.println("LINE: " + event.getType());
            if (event.getType().equals(LineEvent.Type.STOP)) {
                countDownLatch.countDown();
            }
        });
        clip.open(audioInputStream);
try {
        volume(clip, volume);
} catch (Exception e) {
 Debug.println("volume: " + e);
}
        clip.start();
if (!System.getProperty("vavi.test", "").equals("ide")) {
 Thread.sleep(time);
 clip.stop();
 Debug.println("not on ide");
} else {
            countDownLatch.await();
}
        clip.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "true", "false"
    })
    @DisplayName("use opl3 midi player or not")
    void test3(String flag) throws Exception {
Debug.println("------------------------------------------ " + flag + " ------------------------------------------------");
        System.setProperty("vavi.sound.opl3.MidiFile", flag);
        Path path = Paths.get(Opl3AudioFileReaderTest.class.getResource("/opl3/ice_thnk.sci").toURI());
        AudioInputStream originalAudioInputStream = new Opl3AudioFileReader().getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat(
            originalAudioFormat.getSampleRate(),
            16,
            originalAudioFormat.getChannels(),
            true,
            false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioInputStream.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioInputStream.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = audioInputStream.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/opl3/dro_v2.dro",
            "/opl3/samurai.dro",
            "/opl3/ice_thnk.sci",
            "/opl3/michaeld.cmf",
            "/opl3/mi2.laa",
            "/opl3/2.cmf",
    })
    @DisplayName("spi, stream")
    void test4(String filename) throws Exception {
Debug.println("------------------------------------------ " + filename + " ------------------------------------------------");
        Path path = Paths.get(Opl3AudioFileReaderTest.class.getResource(filename).toURI());
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path)));
        AudioFormat originalAudioFormat = originalAudioInputStream.getFormat();
Debug.println(originalAudioFormat);
        AudioFormat targetAudioFormat = new AudioFormat(
                originalAudioFormat.getSampleRate(),
                16,
                originalAudioFormat.getChannels(),
                true,
                false);
Debug.println(targetAudioFormat);
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(targetAudioFormat, originalAudioInputStream);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioInputStream.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioInputStream.getFormat());
        line.addLineListener(ev -> Debug.println(ev.getType()));
        line.start();

        volume(line, volume);

        byte[] buf = new byte[1024];
        while (!later(time).come()) {
            int r = audioInputStream.read(buf, 0, 1024);
            if (r < 0) {
                break;
            }
            line.write(buf, 0, r);
        }
        line.drain();
        line.stop();
        line.close();
    }
}
