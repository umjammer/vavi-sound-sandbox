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
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import junit.framework.TestCase;

import vavix.util.Checksum;


/**
 * EqualizerTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060417 nsano initial version <br>
 */
public class EqualizerTest extends TestCase {

    String inFile;
    String outFile = "out.vavi.wav";
    String correctFile = "out.wav";

    /** @see junit.framework.TestCase#setUp() */
    protected void setUp() throws Exception {
        super.setUp();

        Properties props = new Properties();
        props.load(EqualizerTest.class.getResourceAsStream("local.properties"));
        inFile = props.getProperty("equalizer.in.wav");
    }

    /** */
    public void test1() throws Exception {
        Equalizer.main(new String[] { inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .3d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
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

/* */
