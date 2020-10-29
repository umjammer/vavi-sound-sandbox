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

import vavi.util.ByteUtil;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Sox ResamplerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060203 nsano initial version <br>
 */
public class ResamplerTest {

    static String inFile = "src/test/resources/test.wav";
    static String outFile = "tmp/out.vavi.wav";

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
long time = System.currentTimeMillis();
        Resampler resampler = new Resampler(sampleRate, resamplingRate);
        int[] results1 = resampler.resample(samples);
        int[] results2 = resampler.drain();
Debug.println("results1: " + results1.length + ", 2: " + results2.length);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        // int[] to byte[]
        byte[] dest = new byte[(results1.length + results2.length) * 2];
        for (int i = 0; i < results1.length; i++) {
            ByteUtil.writeLeShort((short) results1[i], dest, i * 2);
//Debug.println("result[" + i + "]: " + results[i]);
        }
        for (int i = 0; i < results2.length; i++) {
            ByteUtil.writeLeShort((short) results2[i], dest, results1.length * 2 + i * 2);
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
Debug.println(audioFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        int l = 0;
        while (l < dest.length) {
            l += line.write(dest, l, dest.length);
        }
        line.drain();
        line.stop();
        line.close();
Debug.println("sound done");

        //----

        InputStream in = new ByteArrayInputStream(dest);
        AudioInputStream ais = new AudioInputStream(in, audioFormat, dest.length / 2);

        int r = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outFile));
Debug.println("result: " + r);

        //----

        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));

        assertEquals((int) resamplingRate, (int) resultAis.getFormat().getSampleRate());
    }

    // noisy
    @Test
    @Disabled
    public void test2() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
        AudioFormat intFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            format.getSampleSizeInBits(),
            1,
            2,
            AudioSystem.NOT_SPECIFIED,
            format.isBigEndian());
        AudioInputStream inAis = AudioSystem.getAudioInputStream(intFormat, sourceAis);
Debug.println("IN: " + intFormat);

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

        InputStream in = new ResamplerInputStream(inAis, inAis.getFormat().getSampleRate(), audioFormat.getSampleRate());

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        int l;
        while (true) {
            l = in.read(buf, 0, buf.length);
            if (l < 0)
                break;
            line.write(buf, 0, l);
Debug.println("line.write: " + l);
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

        InputStream in = new ResamplerInputStream(bais, format.getSampleRate(), audioFormat.getSampleRate());

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
        line.start();
        byte[] buf = new byte[line.getBufferSize()];
        int l;
        while (true) {
            l = in.read(buf, 0, buf.length);
            if (l < 0)
                break;
            line.write(buf, 0, l);
//Debug.println("line.write: " + l);
        }
        line.drain();
        line.stop();
        line.close();

        sourceAis.close();
    }
}

/* */
