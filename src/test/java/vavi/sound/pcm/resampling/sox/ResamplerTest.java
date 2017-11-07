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
import javax.sound.sampled.SourceDataLine;

import org.junit.Test;

import static org.junit.Assert.*;

import vavi.util.Debug;
import vavix.util.ByteUtil;


/**
 * ResamplerTest.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060203 nsano initial version <br>
 */
public class ResamplerTest {

//  String inFile = "C:\\Documents and Settings\\sano-n\\My Documents\\My Music\\1\\大塚 愛 - さくらんぼ.wav";
    String inFile = "C:\\WINDOWS\\Media\\BATTVLOW.WAV";
    String outFile = "out.wav";

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
        Resampler resampler = new Resampler(sampleRate, resamplingRate);
        int[] results1 = resampler.resample(samples);
        int[] results2 = resampler.drain();
Debug.println("results1: " + results1.length + ", 2: " + results2.length);
Debug.println("done: " + (System.currentTimeMillis() - time) + " ms");

        byte[] dest = new byte[(results1.length + results2.length) * 2];
        for (int i = 0; i < results1.length; i++) {
            byteUtil.writeAsByteArray(dest, i * 2, results1[i]);
//Debug.println("result[" + i + "]: " + results[i]);
        }
        for (int i = 0; i < results2.length; i++) {
            byteUtil.writeAsByteArray(dest, results1.length * 2 + i * 2, results2[i]);
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
Debug.println("sound done");

        //----

        InputStream in = new ByteArrayInputStream(dest);
        AudioInputStream ais = new AudioInputStream(in, audioFormat, dest.length / 2);

        int r = AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outFile));
Debug.println("result: " + r);

        //----

        AudioInputStream resultAis = AudioSystem.getAudioInputStream(new File(outFile));
        // TODO 少数以下が切り捨てられる、どこで？
        assertEquals((int) resamplingRate, (int) resultAis.getFormat().getSampleRate());
    }
}

/* */
