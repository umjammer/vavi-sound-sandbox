/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.stream.IntStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Test;
import vavi.sound.twinvq.obsolate.TwinVQInputStream;
import vavi.util.Debug;

import static vavi.sound.SoundUtil.volume;


class TwinVQTest {

    @Test
    void test1() throws Exception {
        TwinVQData d = new TwinVQData();
//        IntStream.range(0, d.cb0808l0.length).map(i -> d.cb0808l0[i]).forEach(System.err::println);
    }

    /**
     * Play TwinVQ.
     *
     * @param args 0:ima wave, 1:output pcm, 2:test or not, use "test"
     */
    public static void main(String[] args) throws Exception {

        boolean isTest = args[2].equals("test");
        InputStream in = new BufferedInputStream(new FileInputStream(args[0]));

        //----

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
        OutputStream os =
                new BufferedOutputStream(new FileOutputStream(args[1]));

        int bufferSize = 2048;

        DataLine.Info info =
                new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line =
                (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(ev -> {
            Debug.println(ev.getType());
            if (LineEvent.Type.STOP == ev.getType()) {
                if (!isTest) {
                    System.exit(0);
                }
            }
        });
        line.start();
        byte[] buf = new byte[bufferSize];
        int l = 0;
        volume(line, .2d);

        while (is.available() > 0) {
            l = is.read(buf, 0, bufferSize);
            line.write(buf, 0, l);
            os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
        os.close();
        is.close();
    }
}