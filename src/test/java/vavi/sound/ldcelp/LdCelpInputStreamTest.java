/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ldcelp;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.*;

import vavix.util.Checksum;


/**
 * LdCelpInputStreamTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060219 nsano initial version <br>
 */
public class LdCelpInputStreamTest {

    String inFile = "f17.bit";
    String outFile = "f17.vavi.outnpf";
    String correctFile = "f17.outnpf";

    /** */
    @Test
    public void test1() throws Exception {
        LdCelp.main(new String[] { "-d", inFile, outFile });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    String outFile2 = "f17.vavi.outpf";
    String correctFile2 = "f17.outpf";

    /** */
    @Test
    public void test2() throws Exception {
        LdCelp.main(new String[] { "-dp", inFile, outFile2 });

        assertEquals(Checksum.getChecksum(new File(correctFile2)), Checksum.getChecksum(new File(outFile2)));
    }
}

/* */
