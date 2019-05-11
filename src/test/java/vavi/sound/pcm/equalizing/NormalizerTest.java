/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing;

import java.io.File;
import java.util.Properties;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import vavix.util.Checksum;


/**
 * NormalizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060623 nsano initial version <br>
 */
public class NormalizerTest {

    String inFile;
    String outFile = "tmp/out.vavi.wav";
    String correctFile = "src/test/resources/vavi/sound/sampled/out.wav";

    @BeforeAll
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.load(NormalizerTest.class.getResourceAsStream("local.properties"));
        inFile = props.getProperty("normalizer.in.wav");
    }

    /** */
    @Test
    public void test1() throws Exception {
        main(new String[] { inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
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

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    /** */
    public static void main(String[] argv) throws Exception {
        String inname;
        String outname;

        System.out.printf("\nnormalize - Copyright 2002 Michael Kohn (mike@naken.cc)\n");

        int argc = argv.length;
        if (argc != 1 && argc != 2) {
            System.out.printf("Usage: normalize <input filename.wav> <output filename.wav>\n");
            System.out.printf("-- If you exclude the output filename normalize will only analyze\n\n");
            System.exit(1);
        }

        inname = argv[0];
        if (argc == 2) {
            outname = argv[1];
        } else {
            outname = null;
        }

        new Normalizer().normalize(inname, outname);
    }
}

/* */
