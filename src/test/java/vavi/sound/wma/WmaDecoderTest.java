/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wma;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for the WMA v1/v2 (ASF) decoder port: the DSP building blocks, VLC
 * table transcription, the ASF demuxer, and a numerically exact end-to-end
 * decode of a real WMA v2 file (validated against FFmpeg 8.1 offline).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260707 nsano initial version <br>
 */
class WmaDecoderTest {

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
                sre += re[j] * Math.cos(ang) - im[j] * Math.sin(ang);
                sim += re[j] * Math.sin(ang) + im[j] * Math.cos(ang);
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

    /** Full IMDCT is finite, has length 2N, is linear, and its centre equals imdct_half. */
    @Test
    void imdctFullIsLinearAndConsistent() {
        int n = 128;
        Imdct imdct = new Imdct(n, 1.0 / 32768.0);
        Random r = new Random(3);
        float[] in = new float[n];
        float[] scaled = new float[n];
        for (int i = 0; i < n; i++) {
            in[i] = r.nextFloat() * 2 - 1;
            scaled[i] = in[i] * 3f;
        }
        float[] full1 = new float[2 * n];
        float[] full2 = new float[2 * n];
        float[] half = new float[n];
        imdct.inverseFull(in, 0, full1, 0);
        imdct.inverseFull(scaled, 0, full2, 0);
        imdct.inverseHalf(in, 0, half, 0);
        for (int i = 0; i < 2 * n; i++) {
            assertTrue(Float.isFinite(full1[i]), "finite");
            assertEquals(full1[i] * 3f, full2[i], 1e-5, "linear " + i);
        }
        // the middle N samples of the full IMDCT are the imdct_half result
        for (int i = 0; i < n; i++) {
            assertEquals(half[i], full1[n / 2 + i], 1e-6, "centre " + i);
        }
    }

    /** Canonical (from-lengths) VLC decodes back to its symbol. */
    @Test
    void vlcFromLengthsDecodes() {
        int[] lens = {1, 2, 3, 3};
        int[] syms = {10, 20, 30, 40};
        Vlc vlc = Vlc.fromLengths(lens, syms, 0);
        byte[] bits = {(byte) 0b0101_1011, (byte) 0b1000_0000};
        BitReader br = new BitReader(bits, 0, 9);
        assertEquals(10, vlc.decode(br));
        assertEquals(20, vlc.decode(br));
        assertEquals(30, vlc.decode(br));
        assertEquals(40, vlc.decode(br));
    }

    /** Explicit-code VLC returns the table index for each code. */
    @Test
    void vlcFromCodesDecodes() {
        int[] codes = {0b0, 0b10, 0b110, 0b111};
        int[] bitsLen = {1, 2, 3, 3};
        Vlc vlc = Vlc.fromCodes(codes, bitsLen);
        byte[] bits = {(byte) 0b0101_1011, (byte) 0b1000_0000};
        BitReader br = new BitReader(bits, 0, 9);
        assertEquals(0, vlc.decode(br));
        assertEquals(1, vlc.decode(br));
        assertEquals(2, vlc.decode(br));
        assertEquals(3, vlc.decode(br));
    }

    /** BitReader assembles bits MSB-first across byte boundaries. */
    @Test
    void bitReaderReadsMsbFirst() {
        byte[] data = {(byte) 0xB5, (byte) 0x3C};
        BitReader br = new BitReader(data, 0, 16);
        assertEquals(0b101, br.readBits(3));
        assertEquals(0b10101, br.readBits(5));
        assertEquals(0x3C, br.readBits(8));
        assertEquals(0, br.bitsLeft());
    }

    /** Every coef / exponent / hgain VLC table builds into a valid prefix code. */
    @Test
    void vlcTablesAreValidPrefixCodes() {
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.COEF0_HUFFCODES, WmaData.COEF0_HUFFBITS));
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.COEF1_HUFFCODES, WmaData.COEF1_HUFFBITS));
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.COEF2_HUFFCODES, WmaData.COEF2_HUFFBITS));
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.COEF3_HUFFCODES, WmaData.COEF3_HUFFBITS));
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.COEF4_HUFFCODES, WmaData.COEF4_HUFFBITS));
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.COEF5_HUFFCODES, WmaData.COEF5_HUFFBITS));
        assertDoesNotThrow(() -> Vlc.fromCodes(WmaData.AAC_SCALEFACTOR_CODE, WmaData.AAC_SCALEFACTOR_BITS));
        assertDoesNotThrow(() -> Vlc.fromLengths(col(WmaData.HGAIN_HUFFTAB, 1), col(WmaData.HGAIN_HUFFTAB, 0), -18));
    }

    /** The decoder constructs (building all per-rate tables) for common layouts. */
    @Test
    void decoderConstructsForCommonLayouts() {
        int[] tags = {0x0160, 0x0161};
        int[] rates = {8000, 16000, 22050, 32000, 44100, 48000};
        int[] channels = {1, 2};
        for (int tag : tags) {
            for (int rate : rates) {
                for (int ch : channels) {
                    WmaDecoder dec = new WmaDecoder(tag, ch, rate, 128000, 2048, new byte[10]);
                    assertEquals(ch, dec.channels());
                    assertEquals(rate, dec.sampleRate());
                    assertTrue(dec.frameLength() >= 256);
                }
            }
        }
    }

    /** The ASF demuxer parses a hand-built minimal container and its packet. */
    @Test
    void asfDemuxerParsesMinimalContainer() {
        byte[] asf = buildMinimalAsf(44100, 2, 2048, 4096);
        assertTrue(AsfDemuxer.isAsf(asf));
        AsfInfo info = AsfDemuxer.demux(asf);
        assertEquals(0x0161, info.formatTag);
        assertEquals(2, info.channels);
        assertEquals(44100, info.sampleRate);
        assertEquals(2048, info.blockAlign);
        assertEquals(1, info.packets.size());
        List<byte[]> payloads = AsfDemuxer.parsePacket(info.packets.get(0));
        assertEquals(1, payloads.size());
        assertTrue(payloads.get(0).length >= 2048);
    }

    // ---- real-file end-to-end decode (numerically validated against FFmpeg 8.1) ----

    static final String WMA = "src/test/resources/test.wma";

    static boolean wmaExists() {
        return Files.exists(Paths.get(WMA));
    }

    /**
     * Decodes the committed WMA v2 fixture in full and checks the output against
     * FFmpeg's reference decode (sample count including the 2*frame_len priming,
     * RMS, peak, and spot samples). FFmpeg trims the priming delay; this decoder
     * (like the {@code @audio/wma-decode} package) keeps it, hence the offset.
     */
    @Test
    @EnabledIf("wmaExists")
    void decodesRealWmaV2MatchingFfmpeg() throws Exception {
        byte[] data = Files.readAllBytes(Paths.get(WMA));
        AsfInfo info = AsfDemuxer.demux(data);
        assertEquals(0x0161, info.formatTag);
        WmaDecoder dec = new WmaDecoder(info);
        int ch = dec.channels();

        List<byte[]> frames = new ArrayList<>();
        int ba = info.blockAlign;
        for (byte[] pkt : info.packets) {
            for (byte[] payload : AsfDemuxer.parsePacket(pkt)) {
                for (int off = 0; off + ba <= payload.length; off += ba) {
                    frames.add(Arrays.copyOfRange(payload, off, off + ba));
                }
            }
        }

        // interleaved output
        List<float[]> chunks = new ArrayList<>();
        int total = 0;
        for (byte[] f : frames) {
            total += collect(dec.decodeSuperframe(f), ch, chunks);
        }
        total += collect(dec.flush(), ch, chunks);
        float[] pcm = concat(chunks);

        int priming = dec.frameLength() * 2; // ffmpeg trims this many samples/ch
        int refSamplesPerCh = 696320;         // ffmpeg -f f32le sample count / ch
        assertEquals(refSamplesPerCh + priming, total, "samples/ch");

        // stats over the ffmpeg-equivalent region (skip priming)
        double sumsq = 0;
        double peak = 0;
        int base = priming * ch;
        for (int i = 0; i < refSamplesPerCh * ch; i++) {
            float v = pcm[base + i];
            sumsq += (double) v * v;
            peak = Math.max(peak, Math.abs(v));
        }
        double rms = Math.sqrt(sumsq / (refSamplesPerCh * ch));
        assertEquals(0.123630934, rms, 1e-4, "rms");
        assertEquals(0.962232113, peak, 1e-3, "peak");
        assertTrue(peak <= 1.0f, "no clipping past unity");

        // spot samples: {refIndexPerCh, channel, expected} from ffmpeg
        float[][] spots = {
                {1000, 0, 3.43410611e-05f}, {1000, 1, -5.93050245e-05f},
                {50000, 0, 8.02454799e-02f}, {50000, 1, -3.28672538e-03f},
                {123456, 0, -3.66584957e-02f}, {123457, 1, -2.81139351e-02f},
                {300000, 0, -1.13536991e-01f}, {300001, 1, -2.30162159e-01f},
                {600000, 0, 1.40294128e-06f}, {696000, 1, -5.15785814e-06f},
        };
        for (float[] s : spots) {
            int idx = (int) s[0];
            int c = (int) s[1];
            float got = pcm[(priming + idx) * ch + c];
            assertEquals(s[2], got, 1e-3, "spot " + idx + "/" + c);
        }
    }

    // ---- helpers ----

    private static int[] col(int[][] table, int c) {
        int[] out = new int[table.length];
        for (int i = 0; i < table.length; i++) {
            out[i] = table[i][c];
        }
        return out;
    }

    private static int collect(float[][] planar, int ch, List<float[]> chunks) {
        int n = planar.length > 0 ? planar[0].length : 0;
        if (n == 0) {
            return 0;
        }
        float[] inter = new float[n * ch];
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < ch; c++) {
                inter[i * ch + c] = planar[c][i];
            }
        }
        chunks.add(inter);
        return n;
    }

    private static float[] concat(List<float[]> chunks) {
        int total = 0;
        for (float[] c : chunks) {
            total += c.length;
        }
        float[] out = new float[total];
        int p = 0;
        for (float[] c : chunks) {
            System.arraycopy(c, 0, out, p, c.length);
            p += c.length;
        }
        return out;
    }

    /** Builds a minimal ASF/WMA v2 container with one all-zero data packet. */
    static byte[] buildMinimalAsf(int sampleRate, int channels, int blockAlign, int packetSize) {
        int[] gHeader = {0x30, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11, 0xa6, 0xd9, 0x00, 0xaa, 0x00, 0x62, 0xce, 0x6c};
        int[] gFileProps = {0xa1, 0xdc, 0xab, 0x8c, 0x47, 0xa9, 0xcf, 0x11, 0x8e, 0xe4, 0x00, 0xc0, 0x0c, 0x20, 0x53, 0x65};
        int[] gStreamProps = {0x91, 0x07, 0xdc, 0xb7, 0xb7, 0xa9, 0xcf, 0x11, 0x8e, 0xe6, 0x00, 0xc0, 0x0c, 0x20, 0x53, 0x65};
        int[] gAudioMedia = {0x40, 0x9e, 0x69, 0xf8, 0x4d, 0x5b, 0xcf, 0x11, 0xa8, 0xfd, 0x00, 0x80, 0x5f, 0x5c, 0x44, 0x2b};
        int[] gData = {0x36, 0x26, 0xb2, 0x75, 0x8e, 0x66, 0xcf, 0x11, 0xa6, 0xd9, 0x00, 0xaa, 0x00, 0x62, 0xce, 0x6c};

        int fileObjSize = 24 + 80;
        int streamObjSize = 24 + 82;
        int headerSize = 30 + fileObjSize + streamObjSize;
        int dataObjSize = 50 + packetSize;
        byte[] buf = new byte[headerSize + dataObjSize];
        int p = 0;

        // header object
        p = putGuid(buf, p, gHeader);
        p = putU64(buf, p, headerSize);
        p = putU32(buf, p, 2); // numObjects
        buf[p++] = 1;
        buf[p++] = 2;

        // file properties object
        int fpStart = p;
        p = putGuid(buf, p, gFileProps);
        p = putU64(buf, p, fileObjSize);
        // body: MaxPktSize at body offset 72
        putU32(buf, fpStart + 24 + 72, packetSize);
        p = fpStart + fileObjSize;

        // stream properties object
        int spStart = p;
        p = putGuid(buf, p, gStreamProps);
        p = putU64(buf, p, streamObjSize);
        int body = spStart + 24;
        putGuid(buf, body, gAudioMedia);
        int wave = body + 54;
        putU16(buf, wave, 0x0161);
        putU16(buf, wave + 2, channels);
        putU32(buf, wave + 4, sampleRate);
        putU32(buf, wave + 8, 16000); // avgBytesPerSec -> 128 kbps
        putU16(buf, wave + 12, blockAlign);
        putU16(buf, wave + 14, 16);
        putU16(buf, wave + 16, 10);   // cbSize
        p = spStart + streamObjSize;

        // data object
        int dStart = p;
        p = putGuid(buf, p, gData);
        p = putU64(buf, p, dataObjSize);
        // FileID(16) + TotalDataPackets(8) + Reserved(2)
        putU64(buf, dStart + 24 + 16, 1); // total data packets
        // packet already zero
        return buf;
    }

    private static int putGuid(byte[] b, int p, int[] g) {
        for (int i = 0; i < 16; i++) {
            b[p++] = (byte) g[i];
        }
        return p;
    }

    private static int putU16(byte[] b, int p, int v) {
        b[p] = (byte) v;
        b[p + 1] = (byte) (v >> 8);
        return p + 2;
    }

    private static int putU32(byte[] b, int p, int v) {
        b[p] = (byte) v;
        b[p + 1] = (byte) (v >> 8);
        b[p + 2] = (byte) (v >> 16);
        b[p + 3] = (byte) (v >> 24);
        return p + 4;
    }

    private static int putU64(byte[] b, int p, long v) {
        for (int i = 0; i < 8; i++) {
            b[p + i] = (byte) (v >> (8 * i));
        }
        return p + 8;
    }
}
