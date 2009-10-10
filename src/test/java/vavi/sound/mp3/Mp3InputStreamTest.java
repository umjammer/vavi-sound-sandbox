/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mp3;

import java.io.File;
import java.util.Properties;

import junit.framework.TestCase;

import vavi.sound.Checksum;


/**
 * Mp3InputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060219 nsano initial version <br>
 */
public class Mp3InputStreamTest extends TestCase {

    Properties props = new Properties();

    /** */
    protected void setUp() throws Exception {
        props.load(Mp3InputStreamTest.class.getResourceAsStream("local.properties"));
        inFile = props.getProperty("vavi.sound.mp3.in.mp3");
    }

    String inFile;
    String outFile = "out.vavi.pcm";
    String correctFile = "out.pcm";

    /** */
    public void test1() throws Exception {
        Mp3InputStream.main(new String[] { inFile, outFile });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }
}

/* */
