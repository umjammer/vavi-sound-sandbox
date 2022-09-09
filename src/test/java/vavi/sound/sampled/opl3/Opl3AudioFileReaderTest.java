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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;


/**
 * Opl3AudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/12 umjammer initial version <br>
 */
class Opl3AudioFileReaderTest {

    static {
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
            if (event.getType().equals(LineEvent.Type.START)) {
Debug.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
Debug.println("done");
                countDownLatch.countDown();
            }
        });
        clip.open(audioInputStream);
if (!(originalAudioFormat.getEncoding() instanceof Opl3Encoding)) {
// Debug.println("down volume: " + originalAudioFormat.getEncoding());
 volume(clip, .2d);
}
        clip.start();
if (Boolean.parseBoolean(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 clip.stop();
 Debug.println("stop");
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
            if (event.getType().equals(LineEvent.Type.START)) {
System.err.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
System.err.println("done");
                countDownLatch.countDown();
            }
        });
        clip.open(audioInputStream);
if (!(originalAudioFormat.getEncoding() instanceof Opl3Encoding)) {
// Debug.println("down volume: " + originalAudioFormat.getEncoding());
 volume(clip, .2d);
}
        clip.start();
if (Boolean.parseBoolean(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 clip.stop();
 Debug.println("stop");
} else {
            countDownLatch.await();
}
        clip.close();
    }

    // TODO both uses MIDPlayer ??? i forgot the purpose
    @ParameterizedTest
    @ValueSource(strings = {
        "true", "false"
    })
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
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat, AudioSystem.NOT_SPECIFIED);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Clip clip = (Clip) AudioSystem.getLine(info);
Debug.println(clip.getClass().getName());
        clip.addLineListener(event -> {
            if (event.getType().equals(LineEvent.Type.START)) {
Debug.println("play");
            }
            if (event.getType().equals(LineEvent.Type.STOP)) {
Debug.println("done");
                countDownLatch.countDown();
            }
        });
        clip.open(audioInputStream);
if (!(originalAudioFormat.getEncoding() instanceof Opl3Encoding)) {
// Debug.println("down volume: " + originalAudioFormat.getEncoding());
 volume(clip, .2d);
}
        clip.start();
if (Boolean.parseBoolean(System.getProperty("vavi.test"))) {
 Thread.sleep(10 * 1000);
 clip.stop();
 Debug.println("stop");
} else {
            countDownLatch.await();
}
        clip.close();
    }
}

/* */
