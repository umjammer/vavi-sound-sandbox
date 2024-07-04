/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ldcelp;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * LdCelpTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060219 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class LdCelpTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "g728")
    String g728 = "src/test/resources/ldcelp/sample.g728";

    @Property(name = "pcm")
    String pcm = "src/test/resources/ldcelp/sample_16k.pcm";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    String outFile = "tmp/ldcelp-vavi-out.pcm";
    String correctFile = "src/test/resources/ldcelp/sample-expected.pcm";

    @Test
    @DisplayName("non post-filtered")
    void test1() throws Exception {
Debug.println(g728);
        LdCelp.main(new String[] {"-d", g728, outFile});

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    String outFile2 = "tmp/ldcelp-vavi-out-pf.pcm";
    String correctFile2 = "src/test/resources/ldcelp/sample-pf-expected.pcm";

    @Test
    @DisplayName("post-filtered")
    void test2() throws Exception {
        LdCelp.main(new String[] {"-dp", g728, outFile2});

        assertEquals(Checksum.getChecksum(new File(correctFile2)), Checksum.getChecksum(new File(outFile2)));
    }

    String outFileE = "tmp/ldcelp-vavi-out.ldcelp";
    String correctFileE = "src/test/resources/ldcelp/f17.bit";

    @Test
    @DisplayName("encode")
    void test4() throws Exception {
        LdCelp.main(new String[] {"-e", pcm, outFileE});

        assertEquals(Checksum.getChecksum(new File(correctFileE)), Checksum.getChecksum(new File(outFileE)));
    }

    @Test
    void testX() throws Exception {
        int a = 6, b = 5;
        a = b = 10;
        Debug.println(a + ", " + b);
        assertEquals(10, a);
        assertEquals(10, b);
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test3() throws Exception {
        Path in = Path.of(g728);
        LittleEndianDataInputStream dis = new LittleEndianDataInputStream(Files.newInputStream(in));
        long size = Files.size(in);
Debug.println(size);
        Path out = Path.of("tmp/g728-pf-out.pcm");
        if (!Files.exists(out.getParent())) Files.createDirectory(out.getParent());
        DataOutputStream dos = new DataOutputStream(Files.newOutputStream(out));

        Decoder decoder = new Decoder(true);

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                16000,
                16,
                1,
                2,
                16000,
                false);
Debug.println(format);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        volume(line, volume);
        line.start();

        short[] inDataS = new short[1];
        short[] outDataS = new short[5];
        byte[] obuf = new byte[outDataS.length * Short.BYTES];
        ShortBuffer sb = ByteBuffer.wrap(obuf).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();

        int l = 0;
        while (true) {
            try {
                inDataS[0] = dis.readShort();
            } catch (IOException e) {
                break;
            }

            decoder.decode(inDataS, outDataS);
            sb.put(outDataS);
            sb.flip();

            line.write(obuf, 0, obuf.length);

            dos.write(obuf, 0, obuf.length);

            l += 1;
//System.err.println(l + "/" + size / Short.BYTES);
        }

        line.drain();
        line.stop();
        line.close();

        dos.flush();
        dos.close();

        dis.close();
    }
}
