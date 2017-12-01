/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ilbc;

import java.io.File;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

import vavix.util.Checksum;


/**
 * IlbcTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/12/01 umjammer initial version <br>
 */
public class IlbcTest {

    String inFile = "src/test/resources/vavi/sound/ldcelp/f17.bit";
    String outFile = "tmp/f17.vavi.outnpf";
    String correctFile = "src/test/resources/vavi/sound/ldcelp/f17.outnpf";

    /** */
    @Test
    public void test1() throws Exception {
        Ilbc.main(new String[] { "-d", inFile, outFile });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    String outFile2 = "f17.vavi.outpf";
    String correctFile2 = "f17.outpf";

    /** */
    @Test
    public void test2() throws Exception {
        Ilbc.main(new String[] { "-dp", inFile, outFile2 });

        assertEquals(Checksum.getChecksum(new File(correctFile2)), Checksum.getChecksum(new File(outFile2)));
    }

}

/* */
