/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mp3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import vavix.util.Checksum;


/**
 * Mp3InputStreamTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060219 nsano initial version <br>
 */
@PropsEntity(url = "file://${user.dir}/local.properties")
public class Mp3InputStreamTest {

    Properties props = new Properties();

    /** */
    @BeforeEach
    public void setUp() throws Exception {
        PropsEntity.Util.bind(this);
    }

    @Property(name = "vavi.sound.mp3.in.mp3")
    String inFile;
    String outFile = "out.vavi.pcm";
    String correctFile = "out.pcm";

    /** */
    @Test
    public void test1() throws Exception {
        main(new String[] { inFile, outFile });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    //----

    /**
     * The program entry point.
     */
    public static void main(String[] args) throws Exception {

        int sampleRate = 41100;
        ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
Debug.println(format);

        InputStream is = new Mp3InputStream(new BufferedInputStream(new FileInputStream(args[0])));
Debug.println("available: " + is.available());

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        byte[] buf = new byte[4608];
        int l = 0;

        while (is.available() > 0) {
            l = is.read(buf, 0, buf.length);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        is.close();

        System.exit(0);
    }
}

/* */
