/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.xma;

import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for the pure-DSP building blocks of the WMA Pro / XMA port.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
class WmaProDecoderTest {

    /** Forward FFT must match a naive DFT. */
    @Test
    void fftForwardMatchesDft() {
        int n = 64;
        Random r = new Random(1);
        float[] re = new float[n];
        float[] im = new float[n];
        float[] data = new float[2 * n];
        for (int i = 0; i < n; i++) {
            re[i] = r.nextFloat() * 2 - 1;
            im[i] = r.nextFloat() * 2 - 1;
            data[2 * i] = re[i];
            data[2 * i + 1] = im[i];
        }

        new Fft(n, false).transform(data, 0);

        for (int k = 0; k < n; k++) {
            double sre = 0;
            double sim = 0;
            for (int j = 0; j < n; j++) {
                double ang = -2.0 * Math.PI * k * j / n;
                double c = Math.cos(ang);
                double s = Math.sin(ang);
                sre += re[j] * c - im[j] * s;
                sim += re[j] * s + im[j] * c;
            }
            assertEquals(sre, data[2 * k], 1e-3, "re[" + k + "]");
            assertEquals(sim, data[2 * k + 1], 1e-3, "im[" + k + "]");
        }
    }

    /** Inverse of the forward FFT reproduces the input scaled by N. */
    @Test
    void fftRoundTrip() {
        int n = 128;
        Random r = new Random(2);
        float[] original = new float[2 * n];
        float[] data = new float[2 * n];
        for (int i = 0; i < 2 * n; i++) {
            original[i] = r.nextFloat() * 2 - 1;
            data[i] = original[i];
        }

        new Fft(n, false).transform(data, 0);
        new Fft(n, true).transform(data, 0);

        for (int i = 0; i < 2 * n; i++) {
            assertEquals(original[i], data[i] / n, 1e-3, "sample " + i);
        }
    }

    /** The FFT can be run at a non-zero offset without touching neighbours. */
    @Test
    void fftHonoursOffset() {
        int n = 32;
        float[] buf = new float[2 * n + 4];
        buf[0] = 42f;
        buf[2 * n + 3] = 43f;
        new Fft(n, false).transform(buf, 2);
        assertEquals(42f, buf[0]);
        assertEquals(43f, buf[2 * n + 3]);
    }

    /** All WMA Pro VLC tables build into valid prefix codes (validates transcription). */
    @Test
    void vlcTablesAreValidPrefixCodes() {
        assertDoesNotThrow(WmaProVlc::scale);
        assertDoesNotThrow(WmaProVlc::scaleRl);
        assertDoesNotThrow(WmaProVlc::coef0);
        assertDoesNotThrow(WmaProVlc::coef1);
        assertDoesNotThrow(WmaProVlc::vec4);
        assertDoesNotThrow(WmaProVlc::vec2);
        assertDoesNotThrow(WmaProVlc::vec1);
    }

    /** A hand-rolled canonical code decodes back to its symbol. */
    @Test
    void vlcDecodesCanonicalCode() {
        // lengths {1,2,3,3} -> codes 0, 10, 110, 111 (MSB-first)
        int[] lens = {1, 2, 3, 3};
        int[] syms = {10, 20, 30, 40};
        Vlc vlc = new Vlc(lens, syms, 0);

        // bit stream: 0 | 10 | 110 | 111  packed MSB-first = 0 10 110 111 = 0101 1011 1
        byte[] bits = {(byte) 0b0101_1011, (byte) 0b1000_0000};
        BitReader br = new BitReader(bits, 0, 9);
        assertEquals(10, vlc.decode(br));
        assertEquals(20, vlc.decode(br));
        assertEquals(30, vlc.decode(br));
        assertEquals(40, vlc.decode(br));
    }

    /** BitReader assembles bits MSB-first across byte boundaries. */
    @Test
    void bitReaderReadsMsbFirst() {
        byte[] data = {(byte) 0xB5, (byte) 0x3C}; // 1011 0101 0011 1100
        BitReader br = new BitReader(data, 0, 16);
        assertEquals(0b101, br.readBits(3));
        assertEquals(0b10101, br.readBits(5));
        assertEquals(0x3C, br.readBits(8));
        assertEquals(0, br.remainingBits());
    }

    /** IMDCT output is finite and linear (imdct(a*x) == a*imdct(x)). */
    @Test
    void imdctIsFiniteAndLinear() {
        int n = 128;
        Imdct imdct = new Imdct(n, 1.0);
        Random r = new Random(3);
        float[] in = new float[n];
        for (int i = 0; i < n; i++) {
            in[i] = r.nextFloat() * 2 - 1;
        }

        float[] out1 = new float[n];
        float[] out2 = new float[n];
        float[] scaled = new float[n];
        for (int i = 0; i < n; i++) {
            scaled[i] = in[i] * 3f;
        }
        imdct.inverse(in, 0, out1, 0);
        imdct.inverse(scaled, 0, out2, 0);

        for (int i = 0; i < n; i++) {
            assertTrue(Float.isFinite(out1[i]), "finite");
            assertEquals(out1[i] * 3f, out2[i], 1e-4, "linear " + i);
        }
    }

    /** The decoder constructs (building all per-rate tables) for the common layouts. */
    @Test
    void decoderConstructsForCommonLayouts() {
        int[] rates = {22050, 32000, 44100, 48000};
        int[] channels = {1, 2, 6};
        for (int rate : rates) {
            for (int ch : channels) {
                XmaStreamInfo info = new XmaStreamInfo();
                info.version = XmaVersion.Xma2;
                info.sampleRate = rate;
                info.channels = ch;
                info.numStreams = (ch + 1) / 2;
                for (int s = 0; s < info.numStreams; s++) {
                    WmaProDecoder dec = new WmaProDecoder(info, s);
                    assertEquals(512, dec.samplesPerFrame());
                    assertTrue(dec.channels() == 1 || dec.channels() == 2);
                }
            }
        }
    }
}
