/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ldcelp;

import java.io.File;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * LdCelpInputStreamTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060219 nsano initial version <br>
 */
@Disabled("not implemented yet")
class LdCelpInputStreamTest {

    String inFile = "src/test/resources/vavi/sound/ldcelp/f17.bit";
    String outFile = "tmp/f17.vavi.outnpf";
    String correctFile = "src/test/resources/vavi/sound/ldcelp/f17.outnpf";

    @Test
    void test1() throws Exception {
        LdCelp.main(new String[] { "-d", inFile, outFile });

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    String outFile2 = "tmp/f17.vavi.outpf";
    String correctFile2 = "src/test/resources/vavi/sound/ldcelp/f17.outpf";

    @Test
    void test2() throws Exception {
        LdCelp.main(new String[] { "-dp", inFile, outFile2 });

        assertEquals(Checksum.getChecksum(new File(correctFile2)), Checksum.getChecksum(new File(outFile2)));
    }
}
