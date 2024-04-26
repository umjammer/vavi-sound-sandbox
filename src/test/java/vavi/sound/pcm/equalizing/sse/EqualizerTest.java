/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing.sse;

import java.io.File;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * EqualizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060417 nsano initial version <br>
 */
@Disabled("not implemented yet")
class EqualizerTest {

    String inFile;
    String outFile = "tmp/out.vavi.wav";
    String correctFile = "src/test/java/resources/vavi/sound/pcm/equalizing/out.wav";

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    @BeforeEach
    void setUp() throws Exception {
        Properties props = new Properties();
        props.load(EqualizerTest.class.getResourceAsStream("local.properties"));
        inFile = props.getProperty("equalizer.in.wav");
    }

    @Test
    void test1() throws Exception {
        Equalizer.main(new String[] { inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        volume(line, volume);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(inFile)), Checksum.getChecksum(new File(outFile)));
    }
}
