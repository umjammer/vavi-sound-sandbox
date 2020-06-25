/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.alac;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Test;

import com.beatofthedrum.alacdecoder.Alac;
import com.beatofthedrum.alacdecoder.AlacContext;
import com.beatofthedrum.alacdecoder.AlacUtils;

import vavi.util.Debug;


/**
 * Test001.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 111022 nsano initial version <br>
 */
class Test001 {

    byte[] format_samples(int bps, int[] src, int samcnt) {
        int temp = 0;
        int counter = 0;
        int counter2 = 0;
        byte[] dst = new byte[65536];

        switch (bps) {
        case 1:
            while (samcnt > 0) {
                dst[counter] = (byte) (0x00FF & (src[counter] + 128));
                counter++;
                samcnt--;
            }
            break;
        case 2:
            while (samcnt > 0) {
                temp = src[counter2];
                dst[counter] = (byte) temp;
                counter++;
                dst[counter] = (byte) (temp >>> 8);
                counter++;
                counter2++;
                samcnt = samcnt - 2;
            }
            break;
        case 3:
            while (samcnt > 0) {
                dst[counter] = (byte) src[counter2];
                counter++;
                counter2++;
                samcnt--;
            }
            break;
        }

        return dst;
    }

    static final String inFile = "src/test/resources/alac.m4a";

    @Test
    void test1() throws Exception {

        AlacContext ac = AlacUtils.AlacOpenFileInput(inFile);
        if (ac.error) {
            throw new IllegalStateException(ac.error_message);
        }
        int num_channels = AlacUtils.AlacGetNumChannels(ac);
        int total_samples = AlacUtils.AlacGetNumSamples(ac);
        int byteps = AlacUtils.AlacGetBytesPerSample(ac);
        int sample_rate = AlacUtils.AlacGetSampleRate(ac);
        int bitps = AlacUtils.AlacGetBitsPerSample(ac);
Debug.println("num_channels: " + num_channels +
                   ", total_samples: " + total_samples +
                   ", byteps: " + byteps +
                   ", sample_rate: " + sample_rate +
                   ", bitps: " + bitps);

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sample_rate,
            bitps,
            num_channels,
            byteps * num_channels,
            sample_rate,
            false);
Debug.println(audioFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
                System.err.println(ev.getType());
            }
        });
        line.start();

        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double gain = .2d; // number between 0 and 1 (loudest)
        float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
        gainControl.setValue(dB);

        byte[] pcmBuffer = null;
        int[] pDestBuffer = new int[1024 * 24 * 3]; // 24kb buffer = 4096 frames = 1 opus sample (we support max 24bps)
        int bps = AlacUtils.AlacGetBytesPerSample(ac);
        while (true) {
            int bytes_unpacked = AlacUtils.AlacUnpackSamples(ac, pDestBuffer);
            if (bytes_unpacked > 0) {
                pcmBuffer = format_samples(bps, pDestBuffer, bytes_unpacked);
            }

            if (bytes_unpacked == 0) {
                break;
            }

            line.write(pcmBuffer, 0, bytes_unpacked);
        }
        line.drain();
        line.stop();
        line.close();

        AlacUtils.AlacCloseFile(ac);
    }

    @Test
    void test2() throws Exception {
        InputStream is = new FileInputStream(inFile);

        Alac decoder = new Alac(is);
        int num_channels = decoder.getNumChannels();
        int total_samples = decoder.getNumSamples();
        int byteps = decoder.getBytesPerSample();
        int sample_rate = decoder.getSampleRate();
        int bitps = decoder.getBitsPerSample();
Debug.println("num_channels: " + num_channels +
                   ", total_samples: " + total_samples +
                   ", byteps: " + byteps +
                   ", sample_rate: " + sample_rate +
                   ", bitps: " + bitps);

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sample_rate,
            bitps,
            num_channels,
            byteps * num_channels,
            sample_rate,
            false);
Debug.println(audioFormat);

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
                System.err.println(ev.getType());
            }
        });
        line.start();

        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        double gain = .2d; // number between 0 and 1 (loudest)
        float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
        gainControl.setValue(dB);

        byte[] pcmBuffer = new byte[0xffff];
        int[] pDestBuffer = new int[1024 * 24 * 3]; // 24kb buffer = 4096 frames = 1 opus sample (we support max 24bps)
        while (true) {
            int bytes_unpacked = decoder.decode(pDestBuffer, pcmBuffer);
            if (bytes_unpacked == 0) {
                break;
            }
//Debug.println("bytes_unpacked: " + bytes_unpacked + "\n" + StringUtil.getDump(pcmBuffer, 64));
            line.write(pcmBuffer, 0, bytes_unpacked);
        }
        line.drain();
        line.stop();
        line.close();

        is.close();
    }
}

/* */
