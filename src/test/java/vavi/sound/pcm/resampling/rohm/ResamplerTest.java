/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.rohm;

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

import org.junit.jupiter.api.Test;

import vavi.util.ByteUtil;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Rohm ResamplerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060125 nsano initial version <br>
 */
class ResamplerTest {

    static final String inFile = "src/test/resources/test.wav";
    static final String outFile = "tmp/out.wav";

    @Test
    void test1() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
Debug.println("IN: " + format);

        float sampleRate = format.getSampleRate();
Debug.println("samplingRate: " + sampleRate);
        int channels = format.getChannels();
Debug.println("numberChannels: " + channels);
        int sampleSizeInBits = format.getSampleSizeInBits() / 8;
Debug.println("sampleSizeInBits: " + sampleSizeInBits);

        int[] samples = new int[sourceAis.available() / format.getFrameSize()];
Debug.println("samples: " + samples.length + ", frameSize: " + format.getFrameSize());
        byte[] sample = new byte[format.getFrameSize()];
        for (int i = 0; i < samples.length; i++) {
            int r = sourceAis.read(sample);
            if (r < 0) {
                throw new EOFException();
            }
            // L monauralize
            samples[i] = ByteUtil.readLeShort(sample, 0);
        }
        final float resamplingRate = 5512.5f;
Debug.println("factor: " + sampleRate / resamplingRate);
long time = System.currentTimeMillis();
        Resampler resampler = new Resampler(sampleRate, resamplingRate);
        int[] results = resampler.resample(samples);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        byte[] dest = new byte[results.length * 2];
        for (int i = 0; i < results.length; i++) {
            ByteUtil.writeLeShort((short) results[i], dest, i * 2);
//Debug.println("result[" + i + "]: " + results[i]);
        }

        //----

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
        line.write(dest, 0, dest.length);
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

    @Test
    void test2() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
        AudioFormat intFormat = new AudioFormat(
            format.getEncoding(),
            format.getSampleRate(),
            format.getSampleSizeInBits(),
            1,
            2,
            format.getFrameRate(),
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

        InputStream in = new RohmInputStream(inAis, inAis.getFormat().getSampleRate(), audioFormat.getSampleRate());

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
}

/* */
