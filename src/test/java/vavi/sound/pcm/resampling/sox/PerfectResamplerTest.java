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
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;

import vavix.util.ByteUtil;


/**
 * PerfectResamplerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 081029 nsano initial version <br>
 * @see "rate.c"
 */
public class PerfectResamplerTest {

    static String inFile = "/Users/nsano/Music/0/wyolica - 星.wav";
    static String outFile = "tmp/out.vavi.wav";

//    {
//        try {
//            Properties props = new Properties();
//            props.load(this.getClass().getResourceAsStream("/vavi/sound/pcm/resampling/sox/local.properties"));
//            inFile = props.getProperty("inFile");
//            outFile = props.getProperty("outFile");
//        } catch (Exception e) {
//            throw new IllegalStateException(e);
//        }
//    }

    /** */
    ByteUtil byteUtil = new ByteUtil();

    /** */
    @Test
    public void test1() throws Exception {
        AudioInputStream sourceAis = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = sourceAis.getFormat();
System.err.println("IN: " + format);

        float sampleRate = format.getSampleRate();
Debug.println("samplingRate: " + sampleRate);
        int channels = format.getChannels();
Debug.println("numberChannels: " + channels);
        int sampleSizeInBits = format.getSampleSizeInBits() / 8;
Debug.println("sampleSizeInBits: " + sampleSizeInBits);

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
        final float resamplingRate = 8000;
Debug.println("factor: " + sampleRate / resamplingRate);
long time = System.currentTimeMillis();
        PerfectResampler resampler = new PerfectResampler(2, 25, 0, false, -1, sampleRate, resamplingRate);
        int[] results = new int[(int) (samples.length * resamplingRate / sampleRate)];
        int[] il = new int[] { samples.length };
        int[] ol = new int[] { results.length };
        resampler.flow(samples, results, il, ol);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        byte[] dest = new byte[results.length * 2];
        for (int i = 0; i < ol[0] /*results.length*/; i++) {
            byteUtil.writeAsByteArray(dest, i * 2, results[i]);
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
System.err.println(audioFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
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
}

/* */
