/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import junit.framework.TestCase;


/**
 * SimpleResamplingInputFilterTest.
 * 
 * @require tritonus_remaining-XXX.jar
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060202 nsano initial version <br>
 */
public class SimpleResamplingInputFilterTest extends TestCase {

    String inFile = "C:\\Documents and Settings\\sano-n\\My Documents\\My Music\\1\\��� �� - ��������.wav";
    String outFile = "out.wav";

    /** */
    public void test1() throws Exception {
        // source: any any Hz, any bit, any, any bytes/frame, any
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
System.err.println("IN: " + sourceAis.getFormat());
        final int outSamplingRate = 8000;
        AudioInputStream secondAis = new SimpleResamplingInputFilter(outSamplingRate).doFilter(sourceAis);
        AudioInputStream thirdAis = new MonauralInputFilter().doFilter(secondAis);

        AudioSystem.write(thirdAis, AudioFileFormat.Type.WAVE, new File(outFile));

        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        assertEquals(outSamplingRate, (int) resultAis.getFormat().getSampleRate());
    }
}

/* */
