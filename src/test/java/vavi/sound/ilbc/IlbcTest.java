/*
 * Copyright (c) 2017 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ilbc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * IlbcTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2017/12/01 umjammer initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class IlbcTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "ilbc")
    String ilbc = "src/test/resources/ilbc/sample-30.ilbc";

    @Property(name = "pcm")
    String pcm;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    // ----

    String encFile = "tmp/vavi-out-30.ilbc";
    String outFile = "tmp/vavi-out-ilbc-30.pcm";
    String correctFile = "src/test/resources/ilbc/sample-ilbc-30-out.pcm";

    @Test
    void test1() throws Exception {
        Ilbc.main(new String[] {"30", pcm, encFile, outFile});

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test2() throws Exception {
        Path in = Path.of(ilbc);
        DataInputStream dis = new DataInputStream(Files.newInputStream(in));
        long size = Files.size(in);
Debug.println(size);
        Path out = Path.of(outFile);
        if (!Files.exists(out.getParent())) Files.createDirectory(out.getParent());
        DataOutputStream dos = new DataOutputStream(Files.newOutputStream(out));

        Decoder decoder = new Decoder(30, 1);

        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                8000,
                16,
                1,
                2,
                8000,
                false);
        Debug.println(format);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        volume(line, volume);
        line.start();

        byte[] decoded = new byte[decoder.getDecodedLength()];
        byte[] buf = new byte[decoder.getEncodedLength()];

        int l = 0;
        while (true) {
            int r = dis.read(buf, 0, buf.length);
            if (r < 0) break;

            decoder.decode(buf, decoded);
//Debug.println("\n" + StringUtil.getDump(decoded, 32));

            line.write(decoded, 0, decoded.length);

            dos.write(decoded, 0, decoded.length);

            l += r;
//Debug.println(l + "/" + size);
        }

        line.drain();
        line.stop();
        line.close();

        dos.flush();
        dos.close();

        dis.close();

        Path expected = Path.of("src/test/resources/ilbc/sample-ilbc-30-out.pcm"); // TODO different from test1
Debug.println("expected: " + Files.size(expected));
        assertEquals(Checksum.getChecksum(expected), Checksum.getChecksum(out));
    }
}
