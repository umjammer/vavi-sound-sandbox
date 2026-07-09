/*
 * WMA v1/v2 decoder.
 *
 * Ported from FFmpeg (libavcodec/wma.c, wmadec.c, wma_common.c). This is the
 * float reference for the same decoder that RockBox's fixed-point wmadec
 * (used by the @audio/wma-decode npm package) is derived from.
 */

package vavi.sound.wma;


/**
 * Windows Media Audio v1 (0x0160) / v2 (0x0161) decoder producing planar float
 * PCM. Feed it {@code block_align}-sized frames (as split from ASF payloads by
 * {@link AsfDemuxer}) via {@link #decodeSuperframe}; call {@link #flush} once at
 * end of stream to drain the final MDCT overlap frame.
 * <p>
 * Direct port of FFmpeg {@code libavcodec/wmadec.c} and {@code wma.c}.
 *
 * @see "https://github.com/FFmpeg/FFmpeg libavcodec/wmadec.c"
 */
public final class WmaDecoder {

    private static final int BLOCK_MIN_BITS = 7;
    private static final int BLOCK_MAX_BITS = 11;
    private static final int BLOCK_MAX_SIZE = 1 << BLOCK_MAX_BITS;
    private static final int HIGH_BAND_MAX_SIZE = 16;
    private static final int NB_LSP_COEFS = 10;
    private static final int MAX_CODED_SUPERFRAME_SIZE = 32768;
    private static final int MAX_CHANNELS = 2;
    private static final int NOISE_TAB_SIZE = 8192;
    private static final int LSP_POW_BITS = 7;
    private static final int MIN_CACHE_BITS = 25;

    // ---- configuration ----
    private final int version;
    private final int channels;
    private final int sampleRate;
    private final int blockAlign;

    private final int frameLenBits;
    private final int frameLen;
    private final int nbBlockSizes;

    private final boolean useBitReservoir;
    private final boolean useVariableBlockLen;
    private final boolean useExpVlc;
    private boolean useNoiseCoding;
    private int byteOffsetBits;
    private final int coefsStart;

    private final int[] coefsEnd;
    private final int[][] exponentBands;
    private final int[] exponentSizes;
    private final int[] highBandStart;
    private final int[][] exponentHighBands;
    private final int[] exponentHighSizes;

    private final Vlc[] coefVlc = new Vlc[2];
    private final int[][] runTable = new int[2][];
    private final float[][] levelTable = new float[2][];
    private Vlc expVlc;
    private Vlc hgainVlc;

    private final float[] noiseTable = new float[NOISE_TAB_SIZE];
    private int noiseIndex;
    private float noiseMult;

    private final float[] lspCosTable;
    private final float[] lspPowETable = new float[256];
    private final float[] lspPowMTable1 = new float[1 << LSP_POW_BITS];
    private final float[] lspPowMTable2 = new float[1 << LSP_POW_BITS];

    private final Imdct[] mdct;
    private final float[][] windows;

    // ---- per-decode state ----
    private BitReader gb;
    private boolean msStereo;
    private final boolean[] channelCoded = new boolean[MAX_CHANNELS];
    private int blockLenBits;
    private int prevBlockLenBits;
    private int nextBlockLenBits;
    private int blockLen;
    private int blockNum;
    private int blockPos;
    private boolean resetBlockLengths = true;

    private final float[][] exponents = new float[MAX_CHANNELS][BLOCK_MAX_SIZE];
    private final float[] maxExponent = new float[MAX_CHANNELS];
    private final int[] exponentsBsize = new int[MAX_CHANNELS];
    private final boolean[] exponentsInitialized = new boolean[MAX_CHANNELS];
    private final float[][] coefs1 = new float[MAX_CHANNELS][BLOCK_MAX_SIZE];
    private final float[][] coefs = new float[MAX_CHANNELS][BLOCK_MAX_SIZE];
    private final float[] output = new float[BLOCK_MAX_SIZE * 2];
    private final float[][] frameOut = new float[MAX_CHANNELS][BLOCK_MAX_SIZE * 2];
    private final int[][] highBandCoded = new int[MAX_CHANNELS][HIGH_BAND_MAX_SIZE];
    private final int[][] highBandValues = new int[MAX_CHANNELS][HIGH_BAND_MAX_SIZE];

    private final byte[] lastSuperframe = new byte[MAX_CODED_SUPERFRAME_SIZE + 8];
    private int lastBitoffset;
    private int lastSuperframeLen;
    private boolean eofDone;

    /** Builds a decoder from ASF stream properties. */
    public WmaDecoder(AsfInfo info) {
        this(info.formatTag, info.channels, info.sampleRate, info.bitRate,
                info.blockAlign, info.codecData);
    }

    /**
     * @param formatTag 0x0160 (WMAv1) or 0x0161 (WMAv2)
     * @param channels  1 or 2
     * @param bitRate   average bits/second
     * @param blockAlign WMA superframe size in bytes
     * @param extradata WAVEFORMATEX codec-specific bytes (flags2 lives here)
     */
    public WmaDecoder(int formatTag, int channels, int sampleRate, int bitRate,
                      int blockAlign, byte[] extradata) {
        if (formatTag != 0x0160 && formatTag != 0x0161) {
            throw new IllegalArgumentException(
                    String.format("Unsupported WMA format tag: 0x%x (only WMAv1/v2)", formatTag));
        }
        if (blockAlign <= 0) {
            throw new IllegalArgumentException("block_align not set");
        }
        if (sampleRate > 50000 || channels < 1 || channels > 2 || bitRate <= 0) {
            throw new IllegalArgumentException("unsupported WMA stream parameters");
        }
        this.version = formatTag == 0x0160 ? 1 : 2;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.blockAlign = blockAlign;

        int flags2 = 0;
        if (extradata != null) {
            if (version == 1 && extradata.length >= 4) {
                flags2 = (extradata[2] & 0xFF) | ((extradata[3] & 0xFF) << 8);
            } else if (version == 2 && extradata.length >= 6) {
                flags2 = (extradata[4] & 0xFF) | ((extradata[5] & 0xFF) << 8);
            }
        }
        this.useExpVlc = (flags2 & 0x0001) != 0;
        this.useBitReservoir = (flags2 & 0x0002) != 0;
        boolean varBlock = (flags2 & 0x0004) != 0;
        if (version == 2 && extradata != null && extradata.length >= 8) {
            int f4 = (extradata[4] & 0xFF) | ((extradata[5] & 0xFF) << 8);
            if (f4 == 0xd && varBlock) {
                varBlock = false; // issue1503
            }
        }
        this.useVariableBlockLen = varBlock;

        for (int i = 0; i < MAX_CHANNELS; i++) {
            maxExponent[i] = 1.0f;
        }

        // ---- ff_wma_init ----
        this.frameLenBits = getFrameLenBits(sampleRate, version);
        this.frameLen = 1 << frameLenBits;
        this.nextBlockLenBits = frameLenBits;
        this.prevBlockLenBits = frameLenBits;
        this.blockLenBits = frameLenBits;

        float bps = (float) bitRate / (float) (channels * sampleRate);
        this.byteOffsetBits = av_log2((int) (bps * frameLen / 8.0 + 0.5)) + 2;
        if (byteOffsetBits + 3 > MIN_CACHE_BITS) {
            throw new IllegalArgumentException("byte_offset_bits too large: " + byteOffsetBits);
        }

        if (useVariableBlockLen) {
            int nb = ((flags2 >> 3) & 3) + 1;
            if ((bitRate / channels) >= 32000) {
                nb += 2;
            }
            int nbMax = frameLenBits - BLOCK_MIN_BITS;
            if (nb > nbMax) {
                nb = nbMax;
            }
            this.nbBlockSizes = nb + 1;
        } else {
            this.nbBlockSizes = 1;
        }

        this.useNoiseCoding = true;
        double highFreq = sampleRate * 0.5;

        int sampleRate1 = sampleRate;
        if (version == 2) {
            if (sampleRate1 >= 44100) {
                sampleRate1 = 44100;
            } else if (sampleRate1 >= 22050) {
                sampleRate1 = 22050;
            } else if (sampleRate1 >= 16000) {
                sampleRate1 = 16000;
            } else if (sampleRate1 >= 11025) {
                sampleRate1 = 11025;
            } else if (sampleRate1 >= 8000) {
                sampleRate1 = 8000;
            }
        }

        double bps1 = bps;
        if (channels == 2) {
            bps1 = bps * 1.6;
        }
        if (sampleRate1 == 44100) {
            if (bps1 >= 0.61) {
                useNoiseCoding = false;
            } else {
                highFreq = highFreq * 0.4;
            }
        } else if (sampleRate1 == 22050) {
            if (bps1 >= 1.16) {
                useNoiseCoding = false;
            } else if (bps1 >= 0.72) {
                highFreq = highFreq * 0.7;
            } else {
                highFreq = highFreq * 0.6;
            }
        } else if (sampleRate1 == 16000) {
            if (bps > 0.5) {
                highFreq = highFreq * 0.5;
            } else {
                highFreq = highFreq * 0.3;
            }
        } else if (sampleRate1 == 11025) {
            highFreq = highFreq * 0.7;
        } else if (sampleRate1 == 8000) {
            if (bps <= 0.625) {
                highFreq = highFreq * 0.5;
            } else if (bps > 0.75) {
                useNoiseCoding = false;
            } else {
                highFreq = highFreq * 0.65;
            }
        } else {
            if (bps >= 0.8) {
                highFreq = highFreq * 0.75;
            } else if (bps >= 0.6) {
                highFreq = highFreq * 0.6;
            } else {
                highFreq = highFreq * 0.5;
            }
        }

        this.coefsStart = version == 1 ? 3 : 0;
        this.coefsEnd = new int[nbBlockSizes];
        this.exponentBands = new int[nbBlockSizes][];
        this.exponentSizes = new int[nbBlockSizes];
        this.highBandStart = new int[nbBlockSizes];
        this.exponentHighBands = new int[nbBlockSizes][HIGH_BAND_MAX_SIZE];
        this.exponentHighSizes = new int[nbBlockSizes];

        for (int k = 0; k < nbBlockSizes; k++) {
            int blockLenK = frameLen >> k;
            if (version == 1) {
                int[] bands = new int[25];
                int lpos = 0;
                int i;
                for (i = 0; i < 25; i++) {
                    int a = WmaData.CRITICAL_FREQS[i];
                    int b = sampleRate;
                    int pos = ((blockLenK * 2 * a) + (b >> 1)) / b;
                    if (pos > blockLenK) {
                        pos = blockLenK;
                    }
                    bands[i] = pos - lpos;
                    if (pos >= blockLenK) {
                        i++;
                        break;
                    }
                    lpos = pos;
                }
                exponentBands[k] = trim(bands, i);
                exponentSizes[k] = i;
            } else {
                int[] table = null;
                int a = frameLenBits - BLOCK_MIN_BITS - k;
                if (a < 3) {
                    if (sampleRate >= 44100) {
                        table = WmaData.EXPONENT_BAND_44100[a];
                    } else if (sampleRate >= 32000) {
                        table = WmaData.EXPONENT_BAND_32000[a];
                    } else if (sampleRate >= 22050) {
                        table = WmaData.EXPONENT_BAND_22050[a];
                    }
                }
                if (table != null) {
                    int n = table[0];
                    int[] bands = new int[n];
                    System.arraycopy(table, 1, bands, 0, n);
                    exponentBands[k] = bands;
                    exponentSizes[k] = n;
                } else {
                    int[] bands = new int[25];
                    int j = 0;
                    int lpos = 0;
                    for (int i = 0; i < 25; i++) {
                        int aa = WmaData.CRITICAL_FREQS[i];
                        int b = sampleRate;
                        int pos = ((blockLenK * 2 * aa) + (b << 1)) / (4 * b);
                        pos <<= 2;
                        if (pos > blockLenK) {
                            pos = blockLenK;
                        }
                        if (pos > lpos) {
                            bands[j++] = pos - lpos;
                        }
                        if (pos >= blockLenK) {
                            break;
                        }
                        lpos = pos;
                    }
                    exponentBands[k] = trim(bands, j);
                    exponentSizes[k] = j;
                }
            }

            coefsEnd[k] = (frameLen - ((frameLen * 9) / 100)) >> k;
            highBandStart[k] = (int) ((blockLenK * 2 * highFreq) / sampleRate + 0.5);

            int n = exponentSizes[k];
            int j = 0;
            int pos = 0;
            for (int i = 0; i < n; i++) {
                int start = pos;
                pos += exponentBands[k][i];
                int end = pos;
                if (start < highBandStart[k]) {
                    start = highBandStart[k];
                }
                if (end > coefsEnd[k]) {
                    end = coefsEnd[k];
                }
                if (end > start) {
                    exponentHighBands[k][j++] = end - start;
                }
            }
            exponentHighSizes[k] = j;
        }

        // MDCT windows (sine).
        this.windows = new float[nbBlockSizes][];
        for (int i = 0; i < nbBlockSizes; i++) {
            windows[i] = SineWindow.get(frameLen >> i);
        }

        // noise generator
        if (useNoiseCoding) {
            noiseMult = useExpVlc ? 0.02f : 0.04f;
            int seed = 1;
            double norm = (1.0 / (double) (1L << 31)) * Math.sqrt(3) * noiseMult;
            for (int i = 0; i < NOISE_TAB_SIZE; i++) {
                seed = seed * 314159 + 1;
                noiseTable[i] = (float) ((double) seed * norm);
            }
        }

        // coefficient VLC tables
        int coefVlcTable = 2;
        if (sampleRate >= 32000) {
            if (bps1 < 0.72) {
                coefVlcTable = 0;
            } else if (bps1 < 1.16) {
                coefVlcTable = 1;
            }
        }
        initCoefVlc(0, coefVlcTable * 2);
        initCoefVlc(1, coefVlcTable * 2 + 1);

        // MDCT (full IMDCT, scale 1/32768 as in wmadec.c)
        this.mdct = new Imdct[nbBlockSizes];
        for (int i = 0; i < nbBlockSizes; i++) {
            mdct[i] = new Imdct(1 << (frameLenBits - i), 1.0 / 32768.0);
        }

        if (useNoiseCoding) {
            hgainVlc = Vlc.fromLengths(column(WmaData.HGAIN_HUFFTAB, 1),
                    column(WmaData.HGAIN_HUFFTAB, 0), -18);
        }
        if (useExpVlc) {
            expVlc = Vlc.fromCodes(WmaData.AAC_SCALEFACTOR_CODE, WmaData.AAC_SCALEFACTOR_BITS);
            this.lspCosTable = null;
        } else {
            this.lspCosTable = new float[frameLen];
            wmaLspToCurveInit(frameLen);
        }
    }

    private static int[] column(int[][] table, int col) {
        int[] out = new int[table.length];
        for (int i = 0; i < table.length; i++) {
            out[i] = table[i][col];
        }
        return out;
    }

    private static int[] trim(int[] src, int len) {
        int[] out = new int[len];
        System.arraycopy(src, 0, out, 0, len);
        return out;
    }

    private static int av_log2(int x) {
        return x <= 0 ? 0 : 31 - Integer.numberOfLeadingZeros(x);
    }

    private static int getFrameLenBits(int sampleRate, int version) {
        int b;
        if (sampleRate <= 16000) {
            b = 9;
        } else if (sampleRate <= 22050 || (sampleRate <= 32000 && version == 1)) {
            b = 10;
        } else {
            b = 11;
        }
        return b;
    }

    /** init_coef_vlc: build the coef VLC and its run/level tables. */
    private void initCoefVlc(int slot, int tableIndex) {
        int[] huffcodes;
        int[] huffbits;
        int[] levels;
        switch (tableIndex) {
        case 0 -> { huffcodes = WmaData.COEF0_HUFFCODES; huffbits = WmaData.COEF0_HUFFBITS; levels = WmaData.LEVELS0; }
        case 1 -> { huffcodes = WmaData.COEF1_HUFFCODES; huffbits = WmaData.COEF1_HUFFBITS; levels = WmaData.LEVELS1; }
        case 2 -> { huffcodes = WmaData.COEF2_HUFFCODES; huffbits = WmaData.COEF2_HUFFBITS; levels = WmaData.LEVELS2; }
        case 3 -> { huffcodes = WmaData.COEF3_HUFFCODES; huffbits = WmaData.COEF3_HUFFBITS; levels = WmaData.LEVELS3; }
        case 4 -> { huffcodes = WmaData.COEF4_HUFFCODES; huffbits = WmaData.COEF4_HUFFBITS; levels = WmaData.LEVELS4; }
        case 5 -> { huffcodes = WmaData.COEF5_HUFFCODES; huffbits = WmaData.COEF5_HUFFBITS; levels = WmaData.LEVELS5; }
        default -> throw new IllegalArgumentException("coef vlc " + tableIndex);
        }
        coefVlc[slot] = Vlc.fromCodes(huffcodes, huffbits);
        int n = huffbits.length;
        int[] run = new int[n];
        float[] level = new float[n];
        int i = 2;
        int lv = 1;
        int k = 0;
        while (i < n) {
            int l = levels[k++];
            for (int j = 0; j < l; j++) {
                run[i] = j;
                level[i] = lv;
                i++;
            }
            lv++;
        }
        runTable[slot] = run;
        levelTable[slot] = level;
    }

    private void wmaLspToCurveInit(int frameLen) {
        double wdel = Math.PI / frameLen;
        for (int i = 0; i < frameLen; i++) {
            lspCosTable[i] = (float) (2.0 * Math.cos(wdel * i));
        }
        for (int i = 0; i < 256; i++) {
            int e = i - 126;
            lspPowETable[i] = (float) Math.pow(2.0, e * -0.25);
        }
        double b = 1.0;
        for (int i = (1 << LSP_POW_BITS) - 1; i >= 0; i--) {
            int m = (1 << LSP_POW_BITS) + i;
            double a = m * (0.5 / (1 << LSP_POW_BITS));
            a = 1.0 / Math.sqrt(Math.sqrt(a));
            lspPowMTable1[i] = (float) (2 * a - b);
            lspPowMTable2[i] = (float) (b - a);
            b = a;
        }
    }

    private float powM14(float x) {
        int v = Float.floatToRawIntBits(x);
        int e = v >>> 23;
        int m = (v >> (23 - LSP_POW_BITS)) & ((1 << LSP_POW_BITS) - 1);
        int tv = ((v << LSP_POW_BITS) & ((1 << 23) - 1)) | (127 << 23);
        float t = Float.intBitsToFloat(tv);
        float a = lspPowMTable1[m];
        float bb = lspPowMTable2[m];
        return lspPowETable[e] * (a + bb * t);
    }

    // ===================== decode =====================

    /** Number of audio channels. */
    public int channels() {
        return channels;
    }

    /** Output sample rate. */
    public int sampleRate() {
        return sampleRate;
    }

    /** Samples per WMA frame (before any superframe multiplication). */
    public int frameLength() {
        return frameLen;
    }

    /**
     * Decodes one {@code block_align}-sized superframe.
     *
     * @param buf the superframe bytes (should be {@code block_align} long)
     * @return planar PCM {@code [channels][nbFrames * frameLen]}; the second
     *         dimension may be 0 when the frame is buffered by the bit reservoir
     */
    public float[][] decodeSuperframe(byte[] buf) {
        if (buf == null || buf.length < blockAlign) {
            throw new IllegalArgumentException("input smaller than block_align");
        }
        int bufSize = blockAlign;
        gb = new BitReader(buf, 0, bufSize * 8);

        int nbFrames;
        if (useBitReservoir) {
            gb.skipBits(4); // super frame index
            nbFrames = gb.readBits(4) - (lastSuperframeLen <= 0 ? 1 : 0);
            if (nbFrames <= 0) {
                // buffer this whole packet's payload into the reservoir
                int len = bufSize - 1;
                int q = lastSuperframeLen;
                while (len > 0) {
                    lastSuperframe[q++] = (byte) gb.readBits(8);
                    len--;
                }
                lastSuperframeLen += 8 * bufSize - 8;
                return new float[channels][0];
            }
        } else {
            nbFrames = 1;
        }

        float[][] samples = new float[channels][nbFrames * frameLen];
        int samplesOffset = 0;

        if (useBitReservoir) {
            int bitOffset = gb.readBits(byteOffsetBits + 3);

            if (lastSuperframeLen > 0) {
                int q = lastSuperframeLen;
                int len = bitOffset;
                while (len > 7) {
                    lastSuperframe[q++] = (byte) gb.readBits(8);
                    len -= 8;
                }
                if (len > 0) {
                    lastSuperframe[q++] = (byte) (gb.readBits(len) << (8 - len));
                }
                BitReader saved = gb;
                gb = new BitReader(lastSuperframe, 0, lastSuperframeLen * 8 + bitOffset);
                if (lastBitoffset > 0) {
                    gb.skipBits(lastBitoffset);
                }
                decodeFrame(samples, samplesOffset);
                samplesOffset += frameLen;
                nbFrames--;
                gb = saved;
            }

            int pos = bitOffset + 4 + 4 + byteOffsetBits + 3;
            gb = new BitReader(buf, (pos >> 3) * 8, (bufSize - (pos >> 3)) * 8);
            int len = pos & 7;
            if (len > 0) {
                gb.skipBits(len);
            }

            resetBlockLengths = true;
            for (int i = 0; i < nbFrames; i++) {
                decodeFrame(samples, samplesOffset);
                samplesOffset += frameLen;
            }

            pos = gb.bitsCount() + (((bitOffset + 4 + 4 + byteOffsetBits + 3)) & ~7);
            lastBitoffset = pos & 7;
            pos >>= 3;
            len = bufSize - pos;
            lastSuperframeLen = len;
            System.arraycopy(buf, pos, lastSuperframe, 0, len);
        } else {
            decodeFrame(samples, samplesOffset);
        }
        return samples;
    }

    /** Drains the final overlap frame at end of stream. */
    public float[][] flush() {
        if (eofDone) {
            return new float[channels][0];
        }
        eofDone = true;
        float[][] out = new float[channels][frameLen];
        for (int ch = 0; ch < channels; ch++) {
            System.arraycopy(frameOut[ch], 0, out[ch], 0, frameLen);
        }
        return out;
    }

    private void decodeFrame(float[][] samples, int samplesOffset) {
        blockNum = 0;
        blockPos = 0;
        while (true) {
            int ret = decodeBlock();
            if (ret != 0) {
                break;
            }
        }
        for (int ch = 0; ch < channels; ch++) {
            System.arraycopy(frameOut[ch], 0, samples[ch], samplesOffset, frameLen);
            System.arraycopy(frameOut[ch], frameLen, frameOut[ch], 0, frameLen);
        }
    }

    /** @return 1 if the last block of the frame was decoded, else 0. */
    private int decodeBlock() {
        // compute current block length
        if (useVariableBlockLen) {
            int n = av_log2(nbBlockSizes - 1) + 1;
            if (resetBlockLengths) {
                resetBlockLengths = false;
                int v = gb.readBits(n);
                if (v >= nbBlockSizes) {
                    throw new IllegalStateException("prev_block_len_bits out of range");
                }
                prevBlockLenBits = frameLenBits - v;
                v = gb.readBits(n);
                if (v >= nbBlockSizes) {
                    throw new IllegalStateException("block_len_bits out of range");
                }
                blockLenBits = frameLenBits - v;
            } else {
                prevBlockLenBits = blockLenBits;
                blockLenBits = nextBlockLenBits;
            }
            int v = gb.readBits(n);
            if (v >= nbBlockSizes) {
                throw new IllegalStateException("next_block_len_bits out of range");
            }
            nextBlockLenBits = frameLenBits - v;
        } else {
            nextBlockLenBits = frameLenBits;
            prevBlockLenBits = frameLenBits;
            blockLenBits = frameLenBits;
        }

        if (frameLenBits - blockLenBits >= nbBlockSizes) {
            throw new IllegalStateException("block_len_bits invalid");
        }

        blockLen = 1 << blockLenBits;
        if (blockPos + blockLen > frameLen) {
            throw new IllegalStateException("frame_len overflow");
        }

        if (channels == 2) {
            msStereo = gb.readBit() != 0;
        }
        int v = 0;
        for (int ch = 0; ch < channels; ch++) {
            int a = gb.readBit();
            channelCoded[ch] = a != 0;
            v |= a;
        }

        int bsize = frameLenBits - blockLenBits;

        if (v == 0) {
            reconstructWindows(bsize);
            blockNum++;
            blockPos += blockLen;
            return blockPos >= frameLen ? 1 : 0;
        }

        // total gain + coef escape bits
        int totalGain = 1;
        for (;;) {
            if (gb.bitsLeft() < 7) {
                throw new IllegalStateException("total_gain overread");
            }
            int a = gb.readBits(7);
            totalGain += a;
            if (a != 127) {
                break;
            }
        }
        int coefNbBits = totalGainToBits(totalGain);

        int nCoefs = coefsEnd[bsize] - coefsStart;
        int[] nbCoefs = new int[MAX_CHANNELS];
        for (int ch = 0; ch < channels; ch++) {
            nbCoefs[ch] = nCoefs;
        }

        // complex (noise) coding
        if (useNoiseCoding) {
            for (int ch = 0; ch < channels; ch++) {
                if (channelCoded[ch]) {
                    int n = exponentHighSizes[bsize];
                    for (int i = 0; i < n; i++) {
                        int a = gb.readBit();
                        highBandCoded[ch][i] = a;
                        if (a != 0) {
                            nbCoefs[ch] -= exponentHighBands[bsize][i];
                        }
                    }
                }
            }
            for (int ch = 0; ch < channels; ch++) {
                if (channelCoded[ch]) {
                    int n = exponentHighSizes[bsize];
                    int val = 0x80000000;
                    for (int i = 0; i < n; i++) {
                        if (highBandCoded[ch][i] != 0) {
                            if (val == 0x80000000) {
                                val = gb.readBits(7) - 19;
                            } else {
                                val += hgainVlc.decode(gb);
                            }
                            highBandValues[ch][i] = val;
                        }
                    }
                }
            }
        }

        // exponents (reused in short blocks)
        if (blockLenBits == frameLenBits || gb.readBit() != 0) {
            for (int ch = 0; ch < channels; ch++) {
                if (channelCoded[ch]) {
                    if (useExpVlc) {
                        decodeExpVlc(ch, bsize);
                    } else {
                        decodeExpLsp(ch);
                    }
                    exponentsBsize[ch] = bsize;
                    exponentsInitialized[ch] = true;
                }
            }
        }
        for (int ch = 0; ch < channels; ch++) {
            if (channelCoded[ch] && !exponentsInitialized[ch]) {
                throw new IllegalStateException("exponents not initialized");
            }
        }

        // spectral coefficients (RLE)
        for (int ch = 0; ch < channels; ch++) {
            if (channelCoded[ch]) {
                int tindex = (ch == 1 && msStereo) ? 1 : 0;
                float[] ptr = coefs1[ch];
                java.util.Arrays.fill(ptr, 0, blockLen, 0f);
                runLevelDecode(coefVlc[tindex], levelTable[tindex], runTable[tindex],
                        ptr, nbCoefs[ch], coefNbBits);
            }
            if (version == 1 && channels >= 2) {
                gb.alignToByte();
            }
        }

        // normalize
        int n4 = blockLen / 2;
        float mdctNorm = 1.0f / (float) n4;
        if (version == 1) {
            mdctNorm *= (float) Math.sqrt(n4);
        }

        // reconstruct MDCT coefficients
        for (int ch = 0; ch < channels; ch++) {
            if (channelCoded[ch]) {
                reconstructCoefs(ch, bsize, totalGain, mdctNorm);
            }
        }

        // ms stereo
        if (msStereo && channelCoded[1]) {
            if (!channelCoded[0]) {
                java.util.Arrays.fill(coefs[0], 0, blockLen, 0f);
                channelCoded[0] = true;
            }
            FloatDsp.butterfliesFloat(coefs[0], 0, coefs[1], 0, blockLen);
        }

        reconstructWindows(bsize);

        blockNum++;
        blockPos += blockLen;
        return blockPos >= frameLen ? 1 : 0;
    }

    private void reconstructWindows(int bsize) {
        for (int ch = 0; ch < channels; ch++) {
            int n4 = blockLen / 2;
            if (channelCoded[ch]) {
                mdct[bsize].inverseFull(coefs[ch], 0, output, 0);
            } else if (!(msStereo && ch == 1)) {
                java.util.Arrays.fill(output, 0, 2 * blockLen, 0f);
            }
            int index = (frameLen / 2) + blockPos - n4;
            wmaWindow(frameOut[ch], index);
        }
    }

    private void reconstructCoefs(int ch, int bsize, int totalGain, float mdctNorm) {
        float[] coefs1c = coefs1[ch];
        float[] exps = exponents[ch];
        int esize = exponentsBsize[ch];
        float mult = (float) (Math.pow(10, totalGain * 0.05) / maxExponent[ch]);
        mult *= mdctNorm;
        float[] outc = coefs[ch];
        int outIdx = 0;
        int c1Idx = 0;

        if (useNoiseCoding) {
            // very low freqs : noise
            for (int i = 0; i < coefsStart; i++) {
                outc[outIdx++] = noiseTable[noiseIndex] * exps[(i << bsize) >> esize] * mult;
                noiseIndex = (noiseIndex + 1) & (NOISE_TAB_SIZE - 1);
            }
            int n1 = exponentHighSizes[bsize];

            // power of high bands
            int expIdx = (highBandStart[bsize] << bsize) >> esize;
            int lastHighBand = 0;
            float[] expPower = new float[HIGH_BAND_MAX_SIZE];
            for (int j = 0; j < n1; j++) {
                int n = exponentHighBands[bsize][j];
                if (highBandCoded[ch][j] != 0) {
                    float e2 = 0;
                    for (int i = 0; i < n; i++) {
                        float vv = exps[expIdx + ((i << bsize) >> esize)];
                        e2 += vv * vv;
                    }
                    expPower[j] = e2 / n;
                    lastHighBand = j;
                }
                expIdx += (n << bsize) >> esize;
            }

            // main + high freqs
            expIdx = (coefsStart << bsize) >> esize;
            for (int j = -1; j < n1; j++) {
                int n;
                if (j < 0) {
                    n = highBandStart[bsize] - coefsStart;
                } else {
                    n = exponentHighBands[bsize][j];
                }
                if (j >= 0 && highBandCoded[ch][j] != 0) {
                    float m1 = (float) Math.sqrt(expPower[j] / expPower[lastHighBand]);
                    m1 = (float) (m1 * Math.pow(10, highBandValues[ch][j] * 0.05));
                    m1 = m1 / (maxExponent[ch] * noiseMult);
                    m1 *= mdctNorm;
                    for (int i = 0; i < n; i++) {
                        float noise = noiseTable[noiseIndex];
                        noiseIndex = (noiseIndex + 1) & (NOISE_TAB_SIZE - 1);
                        outc[outIdx++] = noise * exps[expIdx + ((i << bsize) >> esize)] * m1;
                    }
                    expIdx += (n << bsize) >> esize;
                } else {
                    for (int i = 0; i < n; i++) {
                        float noise = noiseTable[noiseIndex];
                        noiseIndex = (noiseIndex + 1) & (NOISE_TAB_SIZE - 1);
                        outc[outIdx++] = (coefs1c[c1Idx++] + noise) * exps[expIdx + ((i << bsize) >> esize)] * mult;
                    }
                    expIdx += (n << bsize) >> esize;
                }
            }

            // very high freqs : noise
            int n = blockLen - coefsEnd[bsize];
            float mm = mult * exps[expIdx + ((-(1 << bsize)) >> esize)];
            for (int i = 0; i < n; i++) {
                outc[outIdx++] = noiseTable[noiseIndex] * mm;
                noiseIndex = (noiseIndex + 1) & (NOISE_TAB_SIZE - 1);
            }
        } else {
            for (int i = 0; i < coefsStart; i++) {
                outc[outIdx++] = 0f;
            }
            int n = coefsEnd[bsize] - coefsStart; // nb_coefs[ch] (no noise coding)
            for (int i = 0; i < n; i++) {
                outc[outIdx++] = coefs1c[i] * exps[(i << bsize) >> esize] * mult;
            }
            n = blockLen - coefsEnd[bsize];
            for (int i = 0; i < n; i++) {
                outc[outIdx++] = 0f;
            }
        }
    }

    private void runLevelDecode(Vlc vlc, float[] levelTab, int[] runTab,
                                float[] ptr, int numCoefs, int coefNbBits) {
        int offset = 0;
        int coefMask = blockLen - 1;
        while (offset < numCoefs) {
            int code = vlc.decode(gb);
            if (code > 1) {
                offset += runTab[code];
                int sign = gb.readBit() - 1; // -1 or 0
                int lvlBits = Float.floatToRawIntBits(levelTab[code]);
                ptr[offset & coefMask] = Float.intBitsToFloat(lvlBits ^ (sign & 0x80000000));
            } else if (code == 1) {
                break; // EOB
            } else {
                // escape (version 0: wma1/2)
                int level = gb.readBits(coefNbBits);
                offset += gb.readBits(frameLenBits);
                int sign = gb.readBit() - 1; // -1 or 0
                ptr[offset & coefMask] = (level ^ sign) - sign;
            }
            offset++;
        }
        if (offset > numCoefs) {
            throw new IllegalStateException("spectral RLE overflow");
        }
    }

    private void decodeExpVlc(int ch, int bsize) {
        int[] bands = exponentBands[bsize];
        int bandIdx = 0;
        float[] exps = exponents[ch];
        int q = 0;
        int qEnd = blockLen;
        float maxScale = 0;
        int lastExp;
        if (version == 1) {
            lastExp = gb.readBits(5) + 10;
            float vv = WmaData.POW_TAB[lastExp + 60];
            maxScale = vv;
            int n = bands[bandIdx++];
            for (int t = 0; t < n; t++) {
                exps[q++] = vv;
            }
        } else {
            lastExp = 36;
        }
        while (q < qEnd) {
            int code = expVlc.decode(gb);
            lastExp += code - 60;
            if (lastExp + 60 < 0 || lastExp + 60 >= WmaData.POW_TAB.length) {
                throw new IllegalStateException("exponent out of range: " + lastExp);
            }
            float vv = WmaData.POW_TAB[lastExp + 60];
            if (vv > maxScale) {
                maxScale = vv;
            }
            int n = bands[bandIdx++];
            for (int t = 0; t < n; t++) {
                exps[q++] = vv;
            }
        }
        maxExponent[ch] = maxScale;
    }

    private void decodeExpLsp(int ch) {
        float[] lspCoefs = new float[NB_LSP_COEFS];
        for (int i = 0; i < NB_LSP_COEFS; i++) {
            int val;
            if (i == 0 || i >= 8) {
                val = gb.readBits(3);
            } else {
                val = gb.readBits(4);
            }
            lspCoefs[i] = WmaData.LSP_CODEBOOK[i][val];
        }
        lspToCurve(exponents[ch], ch, blockLen, lspCoefs);
    }

    private void lspToCurve(float[] out, int ch, int n, float[] lsp) {
        float valMax = 0;
        for (int i = 0; i < n; i++) {
            float p = 0.5f;
            float q = 0.5f;
            float w = lspCosTable[i];
            for (int j = 1; j < NB_LSP_COEFS; j += 2) {
                q *= w - lsp[j - 1];
                p *= w - lsp[j];
            }
            p *= p * (2.0f - w);
            q *= q * (2.0f + w);
            float vv = p + q;
            vv = powM14(vv);
            if (vv > valMax) {
                valMax = vv;
            }
            out[i] = vv;
        }
        maxExponent[ch] = valMax;
    }

    private void wmaWindow(float[] out, int outOff) {
        float[] in = output;
        int inIdx = 0;
        int bl;
        int nn;
        int bsizeW;

        // left part
        if (blockLenBits <= prevBlockLenBits) {
            bl = blockLen;
            bsizeW = frameLenBits - blockLenBits;
            FloatDsp.vectorFmulAdd(out, outOff, in, 0, windows[bsizeW], 0, out, outOff, bl);
        } else {
            bl = 1 << prevBlockLenBits;
            nn = (blockLen - bl) / 2;
            bsizeW = frameLenBits - prevBlockLenBits;
            FloatDsp.vectorFmulAdd(out, outOff + nn, in, nn, windows[bsizeW], 0, out, outOff + nn, bl);
            System.arraycopy(in, nn + bl, out, outOff + nn + bl, nn);
        }

        int outOff2 = outOff + blockLen;
        inIdx += blockLen;

        // right part
        if (blockLenBits <= nextBlockLenBits) {
            bl = blockLen;
            bsizeW = frameLenBits - blockLenBits;
            FloatDsp.vectorFmulReverse(out, outOff2, in, inIdx, windows[bsizeW], 0, bl);
        } else {
            bl = 1 << nextBlockLenBits;
            nn = (blockLen - bl) / 2;
            bsizeW = frameLenBits - nextBlockLenBits;
            System.arraycopy(in, inIdx, out, outOff2, nn);
            FloatDsp.vectorFmulReverse(out, outOff2 + nn, in, inIdx + nn, windows[bsizeW], 0, bl);
            for (int i = 0; i < nn; i++) {
                out[outOff2 + nn + bl + i] = 0f;
            }
        }
    }

    private static int totalGainToBits(int totalGain) {
        if (totalGain < 15) {
            return 13;
        } else if (totalGain < 32) {
            return 12;
        } else if (totalGain < 40) {
            return 11;
        } else if (totalGain < 45) {
            return 10;
        } else {
            return 9;
        }
    }
}
