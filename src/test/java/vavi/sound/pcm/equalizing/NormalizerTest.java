/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.util.Debug;

import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * NormalizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060623 nsano initial version <br>
 */
class NormalizerTest {

    static String inFile = "src/test/resources/test.wav";
    static final String outFile = "tmp/out_normalizer_vavi.wav";
    @Deprecated
    static final String correctFile = "src/test/resources/vavi/sound/sampled/out.wav";

    @Test
    @Disabled("spi not implemented yet")
    void test1() throws Exception {
        main(new String[] { inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFile));
        AudioFormat format = ais.getFormat();
Debug.println("IN: " + format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        byte[] buf = new byte[1024];
        int l;
        while (ais.available() > 0) {
            l = ais.read(buf, 0, 1024);
            line.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();

        assertEquals(Checksum.getChecksum(new File(correctFile)), Checksum.getChecksum(new File(outFile)));
    }

    private static int getPeak(File file) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        AudioFormat format = ais.getFormat();
        int bits = format.getSampleSizeInBits();
        byte[] buffer = new byte[4096];
        int read;
        int peak = 0;
        while ((read = ais.read(buffer)) != -1) {
            if (bits == 16) {
                for (int i = 0; i < read; i += 2) {
                    if (i + 1 < read) {
                        int b1 = buffer[i] & 0xff;
                        int b2 = buffer[i + 1];
                        short val = (short) ((b2 << 8) | b1);
                        int absVal = Math.abs(val);
                        if (absVal > peak) {
                            peak = absVal;
                        }
                    }
                }
            } else if (bits == 8) {
                for (int i = 0; i < read; i++) {
                    int val = (buffer[i] & 0xff) - 128;
                    int absVal = Math.abs(val);
                    if (absVal > peak) {
                        peak = absVal;
                    }
                }
            }
        }
        ais.close();
        return peak;
    }

    private static void createQuietWav(File src, File dest, double scale) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(src);
        AudioFormat format = ais.getFormat();
        int bits = format.getSampleSizeInBits();
        byte[] buffer = new byte[4096];
        int read;
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        while ((read = ais.read(buffer)) != -1) {
            if (bits == 16) {
                for (int i = 0; i < read; i += 2) {
                    if (i + 1 < read) {
                        int b1 = buffer[i] & 0xff;
                        int b2 = buffer[i + 1];
                        short val = (short) ((b2 << 8) | b1);
                        short scaled = (short) (val * scale);
                        baos.write(scaled & 0xff);
                        baos.write((scaled >> 8) & 0xff);
                    }
                }
            } else if (bits == 8) {
                for (int i = 0; i < read; i++) {
                    int val = (buffer[i] & 0xff) - 128;
                    int scaled = (int) (val * scale);
                    baos.write(scaled + 128);
                }
            }
        }
        ais.close();
        byte[] scaledBytes = baos.toByteArray();
        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(scaledBytes);
        AudioInputStream scaledAis = new AudioInputStream(bais, format, scaledBytes.length / format.getFrameSize());
        AudioSystem.write(scaledAis, javax.sound.sampled.AudioFileFormat.Type.WAVE, dest);
        scaledAis.close();
    }

    @Test
    void testNormalize16BitStereo() throws Exception {
        File quietFile = new File("target/quiet_stereo.wav");
        File out = new File("target/out_stereo.wav");
        if (quietFile.exists()) {
            quietFile.delete();
        }
        if (out.exists()) {
            out.delete();
        }
        File in = new File(inFile);

        createQuietWav(in, quietFile, 0.5);

        int inPeak = getPeak(quietFile);
        Debug.println("Input Peak (stereo): " + inPeak);
        assertTrue(inPeak < 30000, "Input is already nearly normalized or loud");

        Normalizer.normalize(quietFile.getAbsolutePath(), out.getAbsolutePath());

        int outPeak = getPeak(out);
        Debug.println("Output Peak (stereo): " + outPeak);

        assertTrue(outPeak >= 32766 && outPeak <= 32768, "Peak is not normalized: " + outPeak);
    }

    @Test
    void testNormalize16BitMono() throws Exception {
        File quietFile = new File("target/quiet_mono.wav");
        File out = new File("target/out_mono.wav");
        if (quietFile.exists()) {
            quietFile.delete();
        }
        if (out.exists()) {
            out.delete();
        }
        String inMono = "src/test/resources/mono.wav";
        File in = new File(inMono);

        createQuietWav(in, quietFile, 0.25);

        int inPeak = getPeak(quietFile);
        Debug.println("Input Peak (mono): " + inPeak);
        assertTrue(inPeak < 30000, "Input is already nearly normalized or loud");

        Normalizer.normalize(quietFile.getAbsolutePath(), out.getAbsolutePath());

        int outPeak = getPeak(out);
        Debug.println("Output Peak (mono): " + outPeak);

        assertTrue(outPeak >= 32766 && outPeak <= 32768, "Peak is not normalized: " + outPeak);
    }

    /** */
    public static void main(String[] argv) throws Exception {
        String inname;
        String outname;

        System.out.print("\nnormalize - Copyright 2002 Michael Kohn (mike@naken.cc)\n");

        int argc = argv.length;
        if (argc != 1 && argc != 2) {
            System.out.print("Usage: normalize <input filename.wav> <output filename.wav>\n");
            System.out.print("-- If you exclude the output filename normalize will only analyze\n\n");
            System.exit(1);
        }

        inname = argv[0];
        if (argc == 2) {
            outname = argv[1];
        } else {
            outname = null;
        }

        Normalizer.normalize(inname, outname);
    }
}
