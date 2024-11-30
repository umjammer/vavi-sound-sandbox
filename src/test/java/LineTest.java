/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static vavi.sound.SoundUtil.volume;
import static vavix.util.DelayedWorker.later;


/**
 * line. (mic -> speaker)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/06/11 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class LineTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    static long time = System.getProperty("vavi.test", "").equals("ide") ? 1000 * 1000 : 10 * 1000;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /**
     * @param args none
     */
    public static void main(String[] args) throws Exception {
        LineTest app = new LineTest();
        app.setup();
        app.test0();
    }

    @Test
    void test0() throws Exception {
        // microphone
        AudioFormat targetFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, targetFormat);
System.err.println(targetInfo);
        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(targetInfo);
        microphone.open(targetFormat);
        microphone.start();
        AudioInputStream stream = new AudioInputStream(microphone);

        // speaker
        AudioFormat sourceFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, sourceFormat);
System.err.println(sourceInfo);
        SourceDataLine speaker = (SourceDataLine) AudioSystem.getLine(sourceInfo);
        speaker.open(sourceFormat);
        volume(speaker, volume);
        speaker.start();

        byte[] buf = new byte[speaker.getBufferSize()];
        while (!later(time).come()) {
            int r = stream.read(buf);
            if (r < 0) {
                break;
            }
            speaker.write(buf, 0, r);
        }

        speaker.drain();
        speaker.stop();
        speaker.close();
    }
}
