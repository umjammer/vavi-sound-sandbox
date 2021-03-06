/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.sox;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import vavix.util.ByteUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * PolyphaseTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060203 nsano initial version <br>
 */
public class PolyphaseTest {

    static String inFile = "src/test/resources/test.wav";
    static String outFile = "tmp/out.vavi.wav";

    static boolean isGui;

    static {
        isGui = Boolean.valueOf(System.getProperty("eclipse.editor", "false"));
    }

    /** */
    ByteUtil byteUtil = new ByteUtil();

    @Test
    void test0() {
        int i = 1, j = 2;
        assertEquals(1, i);
        assertEquals(2, j);
    }

    @Test
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
            samples[i] = byteUtil.readAsInt(sample, 0);
        }

        // resample
        final float resamplingRate = 8000;
Debug.println("factor: " + sampleRate / resamplingRate);
long time = System.currentTimeMillis();
        Polyphase resampler = new Polyphase(sampleRate, resamplingRate);
        int[] results = resampler.resample(samples);
        int[] results2 = resampler.drain();
Debug.println("drain: " + results2.length);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        // int[] to byte[]
        byte[] dest = new byte[results.length * 2];
        for (int i = 0; i < results.length; i++) {
            byteUtil.writeAsByteArray(dest, i * 2, results[i]);
//Debug.println("result[" + i + "]: " + results[i]);
        }

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
        if (isGui) {
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
        // TODO 少数以下が切り捨てられる、どこで？ < drain() やろ
        assertEquals((int) resamplingRate, (int) resultAis.getFormat().getSampleRate());
    }

    // TODO doesn't work
    @Test
    @Disabled
    public void test2() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
        AudioFormat outFormat = new AudioFormat(
            format.getEncoding(),
            8000,
            format.getSampleSizeInBits(),
            format.getChannels(),
            format.getFrameSize(),
            8000,
            format.isBigEndian());

        InputStream in = new PolyphaseInputStream(sourceAis, format.getSampleRate(), outFormat.getSampleRate());
Debug.println("IN: " + format);
Debug.println("OUT: " + outFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, outFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(outFormat);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[8192];
        int l;
        while (true) {
            l = in.read(buf, 0, buf.length);
            if (l < 0)
                break;
            if (isGui) {
                line.write(buf, 0, l);
Debug.println("line.write: " + l);
            }
        }
        in.close();
        line.drain();
        line.stop();
        line.close();
    }
}

/* */
