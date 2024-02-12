/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import vavi.sound.DebugInputStream;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;


/**
 * clip.
 * <p>
 * read all input stream then play
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
@DisabledIfEnvironmentVariable(named = "GITHUB_WORKFLOW", matches = ".*")
public class ClipTest {

    static {
        System.setProperty("vavi.util.logging.VaviFormatter.extraClassMethod", "org.tritonus.share.TDebug#out");

        System.setProperty("vavi.sound.opl3.MidiFile", "true"); // true: means using opl3 midi device when SMF format 0

//        TDebug.TraceAudioFileReader = true;
    }

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    @Property(name = "clip.test")
    String clipTest = "src/test/resources/test.flac";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    void test1() throws Exception {
        main(new String[] {clipTest});
    }

    /**
     * @param args 0: clip in
     */
    public static void main(String[] args) throws Exception {
        String inFile = args[0];

        for (AudioFileFormat.Type type : AudioSystem.getAudioFileTypes()) {
            System.err.println(type);
        }
        Path file = Paths.get(inFile);

//        URL clipURL = new URL(inFile);
//        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(clipURL);
//        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new File(inFile).toURI().toURL());
        AudioInputStream originalAudioInputStream = AudioSystem.getAudioInputStream(new DebugInputStream(new BufferedInputStream(Files.newInputStream(file))));
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
 Thread.sleep(10 * 1000);
 clip.stop();
 Debug.println("not on ide");
} else {
        countDownLatch.await();
}
        clip.close();
    }
}

/* */
