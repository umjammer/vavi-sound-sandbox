/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.equalizing.sse;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import vavi.sound.pcm.equalizing.sse.Equalizer.Parameter;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;
import vavix.util.Checksum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vavi.sound.SoundUtil.volume;


/**
 * EqualizerTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060417 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
class EqualizerTest {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "equalizer.in.wav")
    String inFile;
    String outFile = "tmp/out_equalizing_vavi.wav";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeEach
    void setUp() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    static final int FS = 44100;

    /** window length bits passed to {@link Equalizer#Equalizer(int)} */
    static final int WB = 14;

    /** winlen = (1 << (WB - 1)) - 1, see {@link Equalizer#Equalizer(int)} */
    static final int WINLEN = (1 << (WB - 1)) - 1;

    /** output lags input by one window (buffering) plus the fir group delay */
    static final int DELAY = WINLEN + WINLEN / 2;

    /** stereo 16bit little endian pcm, same sine on both channels */
    static byte[] sine(int nsamples, double freq, int amp) {
        byte[] buf = new byte[nsamples * 4];
        for (int i = 0; i < nsamples; i++) {
            int v = (int) Math.round(amp * Math.sin(2 * Math.PI * freq * i / FS));
            buf[i * 4    ] = (byte)  (v       & 0xff);
            buf[i * 4 + 1] = (byte) ((v >> 8) & 0xff);
            buf[i * 4 + 2] = (byte)  (v       & 0xff);
            buf[i * 4 + 3] = (byte) ((v >> 8) & 0xff);
        }
        return buf;
    }

    /** left channel sample at index */
    static int sampleAt(byte[] buf, int index) {
        int v = (buf[index * 4] & 0xff) | ((buf[index * 4 + 1] & 0xff) << 8);
        return v >= 0x8000 ? v - 0x10000 : v;
    }

    /** rms of left channel over [from, to) */
    static double rms(byte[] buf, int from, int to) {
        double sum = 0;
        for (int i = from; i < to; i++) {
            double s = sampleAt(buf, i);
            sum += s * s;
        }
        return Math.sqrt(sum / (to - from));
    }

    @Test
    void testIdentity() {
        Equalizer equ = new Equalizer(WB);
        double[] gains = new double[Equalizer.getBandsCount() + 1];
        Arrays.fill(gains, 1.0);
        equ.equ_makeTable(gains, gains, new ArrayList<>(), FS);

        int nsamples = 32768;
        byte[] buf = sine(nsamples, 440, 8000);
        byte[] expected = buf.clone();

        int m = equ.equ_modifySamples(buf, nsamples, 2, 16);
        assertEquals(nsamples, m);

        // with all gains 1 the fir is a pure delta, so output is just the input delayed
        for (int i = 0; i < nsamples - DELAY; i++) {
            assertEquals(sampleAt(expected, i), sampleAt(buf, i + DELAY), 2,
                         "sample " + i);
        }
    }

    @Test
    void testHighBandCut() {
        // pass bands up to 880 Hz, cut everything above
        double[] gains = new double[Equalizer.getBandsCount() + 1];
        for (int i = 0; i < gains.length; i++) {
            double upper = i == Equalizer.getBandsCount() ? FS : Equalizer.getBand(i);
            gains[i] = upper <= 880 ? 1.0 : 0.0;
        }

        int nsamples = 32768;
        int from = DELAY + 2000, to = nsamples - 2000; // steady state region

        // 440 Hz is inside the pass region: should come through nearly unchanged
        Equalizer equ = new Equalizer(WB);
        equ.equ_makeTable(gains, gains, new ArrayList<>(), FS);
        byte[] buf = sine(nsamples, 440, 8000);
        double rmsIn = rms(buf, from - DELAY, to - DELAY);
        equ.equ_modifySamples(buf, nsamples, 2, 16);
        double ratio = rms(buf, from, to) / rmsIn;
        assertTrue(0.8 < ratio && ratio < 1.2, "440 Hz should pass, ratio: " + ratio);

        // 8 kHz is deep in the cut region: should be strongly attenuated
        equ = new Equalizer(WB);
        equ.equ_makeTable(gains, gains, new ArrayList<>(), FS);
        buf = sine(nsamples, 8000, 8000);
        rmsIn = rms(buf, from - DELAY, to - DELAY);
        equ.equ_modifySamples(buf, nsamples, 2, 16);
        ratio = rms(buf, from, to) / rmsIn;
        assertTrue(ratio < 0.05, "8 kHz should be cut, ratio: " + ratio);
    }

    @Test
    void testParametricBoost() {
        // flat band gains plus a +12 dB parametric boost from 100 Hz to 1 kHz,
        // exercises the segment splitting in process_param()
        double[] gains = new double[Equalizer.getBandsCount() + 1];
        Arrays.fill(gains, 1.0);

        Parameter boost = new Parameter();
        boost.left = boost.right = true;
        boost.lower = 100;
        boost.upper = 1000;
        boost.gain = 12; // dB

        List<Parameter> params = new ArrayList<>();
        params.add(boost);

        Equalizer equ = new Equalizer(WB);
        equ.equ_makeTable(gains, gains, params, FS);

        int nsamples = 32768;
        int from = DELAY + 2000, to = nsamples - 2000;

        byte[] buf = sine(nsamples, 440, 4000);
        double rmsIn = rms(buf, from - DELAY, to - DELAY);
        equ.equ_modifySamples(buf, nsamples, 2, 16);
        double ratio = rms(buf, from, to) / rmsIn;
        double expected = Math.pow(10, 12 / 20.0); // ~3.98
        assertTrue(Math.abs(ratio - expected) < expected * 0.1,
                   "440 Hz should be boosted by ~12 dB, ratio: " + ratio);

        // outside the boosted range nothing should change
        equ = new Equalizer(WB);
        equ.equ_makeTable(gains, gains, params, FS);
        buf = sine(nsamples, 4000, 4000);
        rmsIn = rms(buf, from - DELAY, to - DELAY);
        equ.equ_modifySamples(buf, nsamples, 2, 16);
        ratio = rms(buf, from, to) / rmsIn;
        assertTrue(0.9 < ratio && ratio < 1.1, "4 kHz should be unchanged, ratio: " + ratio);
    }

    @Test
    @Disabled("spi not implemented yet")
    void test1() throws Exception {
        Equalizer.main(new String[] { inFile, outFile });

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File(outFile));
        AudioFormat format = ais.getFormat();
System.err.println(format);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        volume(line, volume);
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

        assertEquals(Checksum.getChecksum(new File(inFile)), Checksum.getChecksum(new File(outFile)));
    }
}
