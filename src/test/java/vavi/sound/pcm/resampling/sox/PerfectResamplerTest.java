/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
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
 * PerfectResamplerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 081029 nsano initial version <br>
 * @see "rate.c"
 */
class PerfectResamplerTest {

    static String inFile = "src/test/resources/test.wav";
    static String outFile = "tmp/out.vavi.wav";

    /** */
    ByteUtil byteUtil = new ByteUtil();

    @Disabled("not completed yet")
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
        PerfectResampler resampler = new PerfectResampler(2, 25, 0, false, -1, sampleRate, resamplingRate);
        int[] results = new int[(int) (samples.length * resamplingRate / sampleRate)];
        int[] il = new int[] { samples.length };
        int[] ol = new int[] { results.length };
        resampler.flow(samples, results, il, ol);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        // int[] to byte[]
        byte[] dest = new byte[results.length * 2];
        for (int i = 0; i < ol[0] /*results.length*/; i++) {
            byteUtil.writeAsByteArray(dest, i * 2, results[i]);
//Debug.println("result[" + i + "]: " + results[i]);
        }
//Debug.println("dest:\n" + StringUtil.getDump(dest, 128));

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

        //----

        InputStream in = new ByteArrayInputStream(dest);
        AudioInputStream ais = new AudioInputStream(in, audioFormat, dest.length / 2);

        int r = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outFile));
Debug.println("result: " + r);

        //----

        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        assertEquals((int) resamplingRate, (int) resultAis.getFormat().getSampleRate());
    }
}

/* */
