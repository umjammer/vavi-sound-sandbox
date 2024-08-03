/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vavi.sound.twinvq.obsolate.TwinVQInputStream;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static javax.sound.sampled.LineEvent.Type.STOP;
import static vavi.sound.SoundUtil.volume;


@PropsEntity(url = "file:local.properties")
class TwinVQTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "twinvq")
    String twinvq = "src/test/resources/test.vqf";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static double volume = Double.parseDouble(System.getProperty("vavi.test.volume",  "0.2"));

    // ----

    @Test
    void test1() throws Exception {
        TwinVQData d = new TwinVQData();
//        IntStream.range(0, d.cb0808l0.length).map(i -> d.cb0808l0[i]).forEach(System.err::println);
    }

    String out = "tmp/twinvq-vavi-out.pcm";

    @Test
    void test2() throws Exception {

        InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(twinvq)));

        int sampleRate = 44100;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                1,
                2,
                sampleRate,
                byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(audioFormat);

        InputStream is = new TwinVQInputStream(in,
                4,
                2,
                4,
                byteOrder);

        OutputStream os = new BufferedOutputStream(Files.newOutputStream(Path.of(out)));

        int bufferSize = 2048;

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        CountDownLatch cdl = new CountDownLatch(1);
        line.addLineListener(e -> { Debug.println(e.getType()); if (STOP == e.getType()) { cdl.countDown(); }});
        line.start();
        volume(line, volume);

        byte[] buf = new byte[bufferSize];
        int l = 0;
        while (is.available() > 0) {
            l = is.read(buf, 0, bufferSize);
            line.write(buf, 0, l);
            os.write(buf, 0, l);
        }

        cdl.await();

        line.drain();
        line.close();
        os.close();
        is.close();
    }
}