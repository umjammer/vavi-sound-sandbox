/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import vavi.sound.twinvq.LibAV.AVCodecContext;
import vavi.sound.twinvq.LibAV.AVFormatContext;
import vavi.sound.twinvq.LibAV.AVFrame;
import vavi.sound.twinvq.LibAV.AVInputFormat;
import vavi.sound.twinvq.LibAV.AVPacket;
import vavi.sound.twinvq.TwinVQDec.TwinVQContext;
import vavi.sound.twinvq.obsolate.TwinVQInputStream;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavi.util.properties.annotation.PropsEntity.Util;

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
            Util.bind(this);
        }
    }

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    // ----

    int somefun(int x) throws IOException { return 0; }

    @FunctionalInterface
    interface ThrowingFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    ThrowingFunction<Integer, Integer, IOException> fun;

    @Test
    void testF() {
        fun = this::somefun;
    }

    // ----

    @Test
    void test1() throws Exception {
        TwinVQData d = new TwinVQData();
//        IntStream.range(0, d.cb0808l0.length).map(i -> d.cb0808l0[i]).forEach(System.err::println);
    }

    String out = "tmp/twinvq-vavi-out.pcm";

    @Test
    @EnabledIfEnvironmentVariable(named = "vavi.test", matches = "ide")
    void test2() throws Exception {

        InputStream in = new BufferedInputStream(Files.newInputStream(Path.of(twinvq)));

        int sampleRate = 44100;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
                Encoding.PCM_SIGNED,
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

        Info info = new Info(SourceDataLine.class, audioFormat);
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

    @Test
    @EnabledIfEnvironmentVariable(named = "vavi.test", matches = "ide")
    void testFF() throws Exception {

        AVInputFormat inputFormat = VFQ.ff_vqf_demuxer;

        // probe
        DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(Path.of(twinvq))));
        dis.mark(12);
        byte[] probe = new byte[12];
        dis.readFully(probe);
        dis.reset();
        if (inputFormat.read_probe.apply(probe) == 0) {
            throw new IllegalArgumentException("not vfq");
        }

        // header
        AVFormatContext formatContext = new AVFormatContext();
        formatContext.pb = dis;
        inputFormat.read_header.apply(formatContext);

        // decoder
        AVCodecContext codecContext = formatContext.streams[0].codecpar;
        codecContext.priv_data = new TwinVQContext();
        TwinVQDec.twinvq_decode_init(codecContext);

        AVFrame frame = new AVFrame();
        int[] got_frame_ptr = new int[1];
        while (true) {
            AVPacket packet = inputFormat.read_packet.apply(formatContext);
            int r = TwinVQ.ff_twinvq_decode_frame(codecContext, frame, got_frame_ptr, packet);
            if (r == 0) break;
        }

        TwinVQ.ff_twinvq_decode_close(codecContext);
    }
}