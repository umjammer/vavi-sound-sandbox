/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.sox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import vavi.util.ByteUtil;
import vavi.util.Debug;
import vavi.util.StringUtil;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vavi.sound.SoundUtil.volume;


/**
 * PolyphaseTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060203 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class PolyphaseTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    static String inFile = "src/test/resources/test.wav";
    static String outFile = "tmp/out.vavi.wav";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get("tmp"));
    }

    @BeforeEach
    void setupEach() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @Disabled
    @DisplayName("multiple assigning separated by comma")
    void test0() {
        int i = 1, j = 2;
        assertEquals(1, i);
        assertEquals(2, j);
    }

    @Test
    @Disabled
    public void test1() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
Debug.println("IN: " + format);

        float sampleRate = format.getSampleRate();
Debug.println("samplingRate: " + sampleRate);
        int channels = format.getChannels();
Debug.println("numberChannels: " + channels);
        int sampleSizeInBits = format.getSampleSizeInBits() / 8;
Debug.println("sampleSizeInBits: " + sampleSizeInBits);

        // to monaural
        int[] samples = new int[sourceAis.available() / format.getFrameSize()];
Debug.println("samples: " + samples.length + ", frameSize: " + format.getFrameSize());
        byte [] sample = new byte[format.getFrameSize()];
        for (int i = 0; i < samples.length; i++) {
            int r = sourceAis.read(sample);
            if (r < 0) {
                throw new EOFException();
            }
            // L
            samples[i] = ByteUtil.readLeShort(sample, 0);
        }

        // resample
        final float resamplingRate = 8000;
Debug.println("factor: " + sampleRate / resamplingRate);
Debug.println("samples: " + samples.length);
long time = System.currentTimeMillis();
        Polyphase resampler = new Polyphase(sampleRate, resamplingRate);
        int[] results = resampler.resample(samples);
        int[] results2 = resampler.drain();
Debug.println("results: " + results.length);
Debug.println("drain: " + results2.length);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        // int[] to byte[]
        byte[] dest = new byte[results.length * 2];
        for (int i = 0; i < results.length; i++) {
            ByteUtil.writeLeShort((short) results[i], dest, i * 2);
//Debug.println("result[" + i + "]: " + results[i]);
        }
//Debug.println("\n" + StringUtil.getDump(dest, 512));

        // play
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            resamplingRate,
            16,
            1,
            2,
            resamplingRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
Debug.println("OUT: " + audioFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.start();
        if ("ide".equals(System.getProperty("vavi.test"))) {
            int l = 0;
            while (l < dest.length) {
                l += line.write(dest, l, dest.length);
            }
        }
        line.drain();
        line.stop();
        line.close();

        //----

        InputStream in = new ByteArrayInputStream(dest);
        AudioInputStream ais = new AudioInputStream(in, audioFormat, dest.length / 2);

        int r = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outFile));
Debug.println("result: " + r);

        //----

        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));

        assertEquals((int) resamplingRate, (int) resultAis.getFormat().getSampleRate());
    }

    // TODO noisy
    @Test
    @Disabled
    public void test2() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
        AudioFormat inFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            format.getSampleSizeInBits(),
            1,
            2,
            format.getFrameRate(),
            format.isBigEndian());
        AudioInputStream inAis = AudioSystem.getAudioInputStream(inFormat, sourceAis); // this monauralize not works well
        AudioFormat outFormat = new AudioFormat(
            format.getEncoding(),
            8000,
            format.getSampleSizeInBits(),
            1,
            2,
            8000,
            format.isBigEndian());

        InputStream in = new PolyphaseInputStream(inAis, format.getSampleRate(), outFormat.getSampleRate());
Debug.println("IN: " + inAis.getFormat());
Debug.println("OUT: " + outFormat);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] b = new byte[8000];
        while (true) {
            int r = in.read(b, 0, b.length);
            if (r < 0) {
                break;
            }
            baos.write(b, 0, r);
        }
        byte[] out = baos.toByteArray();
Debug.println("out: " + out.length);
Debug.println("\n" + StringUtil.getDump(out, 512));

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outFormat);
        volume(line, volume);
        line.start();
//        byte[] buf = new byte[line.getBufferSize()];
        int l = 0;
        while (l < out.length) {
//            r = in.read(buf, 0, buf.length);
//            if (r < 0)
//                break;
//            line.write(buf, 0, r);
            int r = line.write(out, l, out.length - l);
Debug.println(Level.FINE, "line: " + r);
            l += r;
        }
        line.drain();
        line.stop();
        line.close();

        sourceAis.close();
    }

    @Test
    public void test3() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
Debug.println("IN: " + format);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // to monaural
        int[] samples = new int[sourceAis.available() / format.getFrameSize()];
        Debug.println("samples: " + samples.length + ", frameSize: " + format.getFrameSize());
        byte [] sample = new byte[format.getFrameSize()];
        for (int i = 0; i < samples.length; i++) {
            int r = sourceAis.read(sample);
            if (r < 0) {
                throw new EOFException();
            }
            // L
            baos.write(sample, 0, 2);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

        final float resamplingRate = 5512.5f;
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            format.getEncoding(),
            resamplingRate,
            format.getSampleSizeInBits(),
            1,
            2,
            resamplingRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
Debug.println(audioFormat);

        InputStream in = new PolyphaseInputStream(bais, format.getSampleRate(), audioFormat.getSampleRate());

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        volume(line, volume);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        int l;
        while (true) {
            l = in.read(buf, 0, buf.length);
            if (l < 0)
                break;
            line.write(buf, 0, l);
Debug.println(Level.FINER, "line.write: " + l);
        }
        line.drain();
        line.stop();
        line.close();

        sourceAis.close();
    }

    @Test
    @Disabled
    @DisplayName("via spi")
    public void test4() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
        AudioFormat inFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            format.getSampleSizeInBits(),
            1,
            2,
            format.getFrameRate(),
            format.isBigEndian());
        AudioInputStream inAis = AudioSystem.getAudioInputStream(inFormat, sourceAis);
        AudioFormat outFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            format.getSampleSizeInBits(),
            1,
            2,
            format.getFrameRate(),
            format.isBigEndian());

Debug.println("IN: " + inAis.getFormat());
Debug.println("OUT: " + outFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outFormat);
        volume(line, volume);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        while (true) {
            int r = inAis.read(buf, 0, buf.length);
            if (r < 0)
                break;
            line.write(buf, 0, r);
//Debug.println(Level.FINER, "line: " + r);
        }
        line.drain();
        line.stop();
        line.close();

        sourceAis.close();
    }
}
