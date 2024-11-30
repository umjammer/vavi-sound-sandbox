/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.resampling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vavi.sound.sampled.MonauralInputFilter;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * SampleRateConversionProviderTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060726 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class SampleRateConversionProviderTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    String inFile = "src/test/resources/test.wav";
    String outFile = "tmp/out.wav";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get("tmp"));
    }

    @BeforeEach
    void setupEach() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    /**
     * <ul>
     *  <li>→ mono requires tritonus_remaining_###.jar
     *  <li>on eclipse, is the plugin working according to the jar ranking???
     * </ul>
     */
    @Test
    public void test1() throws Exception {

for (Type type : AudioSystem.getAudioFileTypes()) {
 Debug.println("type: " + type);
}

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
            inAudioFormat.getFrameRate(),
            inAudioFormat.isBigEndian());

        assertTrue(AudioSystem.isConversionSupported(outAudioFormat, inAudioFormat));

        //
        AudioInputStream secondAis = AudioSystem.getAudioInputStream(outAudioFormat, sourceAis);
Debug.println("OUT: " + secondAis.getFormat());
for (Type type : AudioSystem.getAudioFileTypes(secondAis)) {
 System.err.println("type: " + type);
}
Debug.println("secondAis: " + secondAis.getFormat());
        AudioInputStream thirdAis = new MonauralInputFilter().doFilter(secondAis);
Debug.println("thirdAis: " + thirdAis.getFormat());
        AudioSystem.write(thirdAis, AudioFileFormat.Type.WAVE, new File(outFile));

        // 2. play
        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        assertEquals(outSamplingRate, (int) resultAis.getFormat().getSampleRate());

        //
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, resultAis.getFormat());
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(resultAis.getFormat());
//        line.addLineListener(ev -> {
//Debug.println(ev.getType());
//                if (LineEvent.Type.STOP == ev.getType()) {
//                    System.exit(0);
//                }
//        });
        line.start();
        byte[] buf = new byte[1024];
        int l = 0;
        volume(line, volume);

        while (resultAis.available() > 0) {
            l = resultAis.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
    }
}
