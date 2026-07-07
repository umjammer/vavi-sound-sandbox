/*
 * Ported from Echo (https://github.com/IsaacMarovitz/Echo), which is derived
 * from ffmpeg's WMA Pro decoder. Java port.
 */

package vavi.sound.xma;

import java.util.Arrays;


/**
 * Per-substream WMA Pro / XMA decoder. Operates on one stream of 1 or 2
 * channels; an N-channel XMA stream is decoded by running one of these per
 * substream (see {@link XmaContainer}).
 * <p>
 * Direct translation of the per-stream decode path in
 * {@code ffmpeg/libavcodec/wmaprodec.c} via {@code Echo/WmaPro/WmaProDecoder.cs}.
 *
 * @see "https://github.com/IsaacMarovitz/Echo"
 */
public final class WmaProDecoder {

    private static final int BLOCK_MIN_BITS = WmaProTables.BlockMinBits; // 6
    private static final int BLOCK_MAX_BITS = WmaProTables.BlockMaxBits; // 13
    private static final int BLOCK_SIZES = BLOCK_MAX_BITS - BLOCK_MIN_BITS + 1; // 8
    private static final int MAX_SUBFRAMES = WmaProTables.MaxSubframes; // 32
    private static final int MAX_BANDS = WmaProTables.MaxBands; // 29
    private static final int WMA_PRO_MAX_CHANNELS = 8;

    private static final int XMA_DECODE_FLAGS = 0x10D6;
    private static final int XMA_SAMPLES_PER_FRAME = 512;
    private static final int XMA_BITS_PER_SAMPLE = 16;
    private static final int XMA_BLOCK_ALIGN = 2048;

    private static final class ChannelCtx {
        int prevBlockLen;
        boolean transmitCoefs;
        int numSubframes;
        final int[] subframeLen = new int[MAX_SUBFRAMES];
        final int[] subframeOffset = new int[MAX_SUBFRAMES];
        int curSubframe;
        int decodedSamples;
        boolean grouped;
        int quantStep;
        boolean reuseSf;
        int scaleFactorStep;
        int maxScaleFactor;
        final int[][] savedScaleFactors;
        int scaleFactorIdx; // row holding "latest transmitted" SFs
        int scaleFactorRow; // row used by the current subframe
        int tableIdx;
        int coeffsBase; // index into out[] for current subframe
        int numVecCoeffs;
        final float[] out;

        ChannelCtx(int outSize) {
            out = new float[outSize];
            savedScaleFactors = new int[][] {new int[MAX_BANDS], new int[MAX_BANDS]};
        }
    }

    private static final class ChannelGrp {
        int numChannels;
        boolean transform;
        final byte[] transformBand = new byte[MAX_BANDS];
        final float[] decorrelationMatrix = new float[WMA_PRO_MAX_CHANNELS * WMA_PRO_MAX_CHANNELS];
        final int[] channelIndex = new int[WMA_PRO_MAX_CHANNELS];
    }

    private final int logFrameSize;
    private final int bitsPerSample;
    private final boolean lenPrefix;
    private final int maxNumSubframes;
    private final int subframeLenBits;
    private final int maxSubframeLenBit;
    private final int minSamplesPerSubframe;
    private final boolean dynamicRangeCompression;

    private final int[] numSfb = new int[BLOCK_SIZES];
    private final int[][] sfbOffsets = new int[BLOCK_SIZES][MAX_BANDS + 1];
    private final int[][][] sfOffsets = new int[BLOCK_SIZES][BLOCK_SIZES][MAX_BANDS];
    private final int[] subwooferCutoffs = new int[BLOCK_SIZES];
    private final Imdct[] imdcts = new Imdct[BLOCK_SIZES];
    private final float[][] windows = new float[BLOCK_SIZES][];

    private final ChannelCtx[] channelsCtx;
    private final ChannelGrp[] chgroup;
    private final float[] tmp;

    private int subframeLenField;
    private int numBands;
    private int tableIdxRuntime;
    private int escLen;
    private int numChgroups;
    private int channelsForCurSubframe;
    private final int[] channelIndexesForCurSubframe = new int[WMA_PRO_MAX_CHANNELS];
    private boolean transmitNumVecCoeffs;
    private boolean parsedAllSubframes;

    private final int channels;
    private final int samplesPerFrame;

    public int channels() {
        return channels;
    }

    public int samplesPerFrame() {
        return samplesPerFrame;
    }

    public WmaProDecoder(XmaStreamInfo info, int streamIndex) {
        channels = info.channelsForStream(streamIndex);
        samplesPerFrame = XMA_SAMPLES_PER_FRAME;
        bitsPerSample = XMA_BITS_PER_SAMPLE;
        logFrameSize = log2(XMA_BLOCK_ALIGN) + 4;          // 15
        lenPrefix = (XMA_DECODE_FLAGS & 0x40) != 0;        // true
        dynamicRangeCompression = (XMA_DECODE_FLAGS & 0x80) != 0; // true

        int log2MaxNumSubframes = (XMA_DECODE_FLAGS & 0x38) >> 3; // 2
        maxNumSubframes = 1 << log2MaxNumSubframes;               // 4
        maxSubframeLenBit = maxNumSubframes == 16 || maxNumSubframes == 4 ? 1 : 0;
        subframeLenBits = log2(log2MaxNumSubframes) + 1;         // 2
        minSamplesPerSubframe = samplesPerFrame / maxNumSubframes; // 128

        int numPossibleBlockSizes = log2MaxNumSubframes + 1;     // 3

        int sampleRate = info.sampleRate;
        int rate = getRate(sampleRate);

        // sfb_offsets and num_sfb per block size
        for (int i = 0; i < numPossibleBlockSizes; i++) {
            int subframeLen = samplesPerFrame >> i;
            int band = 1;
            sfbOffsets[i][0] = 0;

            for (int x = 0; x < MAX_BANDS - 1 && sfbOffsets[i][band - 1] < subframeLen; x++) {
                int off = subframeLen * 2 * WmaProTables.CriticalFreq[x] / rate + 2;
                off &= ~3;
                if (off > sfbOffsets[i][band - 1]) {
                    sfbOffsets[i][band++] = off;
                }
                if (off >= subframeLen) {
                    break;
                }
            }
            sfbOffsets[i][band - 1] = subframeLen;
            numSfb[i] = band - 1;
        }

        // sf_offsets resample matrix
        for (int i = 0; i < numPossibleBlockSizes; i++) {
            for (int b = 0; b < numSfb[i]; b++) {
                int off = ((sfbOffsets[i][b] + sfbOffsets[i][b + 1] - 1) << i) >> 1;
                for (int x = 0; x < numPossibleBlockSizes; x++) {
                    int v = 0;
                    while (sfbOffsets[x][v + 1] << x < off) {
                        v++;
                    }
                    sfOffsets[i][x][b] = v;
                }
            }
        }

        // subwoofer cutoffs
        for (int i = 0; i < numPossibleBlockSizes; i++) {
            int blockSize = samplesPerFrame >> i;
            long num = 440L * blockSize + 3L * (sampleRate >> 1) - 1;
            int cutoff = (int) (num / sampleRate);
            if (cutoff < 4) {
                cutoff = 4;
            }
            if (cutoff > blockSize) {
                cutoff = blockSize;
            }
            subwooferCutoffs[i] = cutoff;
        }

        // IMDCT contexts + sine windows for the block sizes XMA can hit.
        int minBlockBits = log2(minSamplesPerSubframe);  // 7
        int maxBlockBits = log2(samplesPerFrame);        // 9
        for (int b = minBlockBits; b <= maxBlockBits; b++) {
            int k = b - BLOCK_MIN_BITS;
            int n = 1 << b;
            double scale = 1.0 / (1 << (b - 1)) / (1L << (bitsPerSample - 1));
            imdcts[k] = new Imdct(n, scale);
            windows[k] = SineWindow.get(n);
        }

        // Per-channel state
        int outSize = samplesPerFrame * 2 + samplesPerFrame / 2;
        channelsCtx = new ChannelCtx[channels];
        for (int c = 0; c < channels; c++) {
            channelsCtx[c] = new ChannelCtx(outSize);
            channelsCtx[c].prevBlockLen = samplesPerFrame;
        }

        chgroup = new ChannelGrp[WMA_PRO_MAX_CHANNELS];
        for (int i = 0; i < WMA_PRO_MAX_CHANNELS; i++) {
            chgroup[i] = new ChannelGrp();
        }

        tmp = new float[1 << maxBlockBits];
    }

    /**
     * Decode one XMA frame. Writes {@link #samplesPerFrame()} floats into
     * {@code output[ch]} for each {@code ch} in {@code [0..channels())}. Returns
     * true on success.
     */
    public boolean decodeFrame(XmaFrame frame, float[][] output) {
        BitReader gb = new BitReader(frame.buffer(), frame.startBit(), frame.lengthBits());

        // 15-bit frame size prefix — read & discard.
        if (lenPrefix) {
            gb.read(logFrameSize);
        }

        if (!decodeTileHeader(gb)) {
            fillSilence(output);
            return false;
        }

        // postproc transform — must be 0 for XMA
        if (channels > 1 && gb.read(1) != 0) {
            if (gb.read(1) != 0) {
                int n = channels * channels;
                for (int i = 0; i < n; i++) {
                    gb.read(4);
                }
            }
        }

        // DRC gain (read & discard)
        if (dynamicRangeCompression) {
            gb.read(8);
        }

        // trim_start / trim_end (read & discard)
        if (gb.read(1) != 0) {
            int b = log2(samplesPerFrame * 2);
            if (gb.read(1) != 0) {
                gb.read(b);
            }
            if (gb.read(1) != 0) {
                gb.read(b);
            }
        }

        // Reset subframe state for all channels in this frame
        parsedAllSubframes = false;
        for (int i = 0; i < channels; i++) {
            channelsCtx[i].decodedSamples = 0;
            channelsCtx[i].curSubframe = 0;
            channelsCtx[i].reuseSf = false;
        }

        while (!parsedAllSubframes) {
            if (!decodeSubframe(gb)) {
                fillSilence(output);
                return false;
            }
        }

        for (int i = 0; i < channels; i++) {
            System.arraycopy(channelsCtx[i].out, 0, output[i], 0, samplesPerFrame);
            System.arraycopy(channelsCtx[i].out, samplesPerFrame, channelsCtx[i].out, 0,
                    samplesPerFrame / 2);
        }

        return true;
    }

    private void fillSilence(float[][] output) {
        for (int i = 0; i < channels; i++) {
            Arrays.fill(output[i], 0, samplesPerFrame, 0f);
        }
    }

    private boolean decodeTileHeader(BitReader gb) {
        int[] numSamples = new int[WMA_PRO_MAX_CHANNELS];
        boolean[] containsSubframe = new boolean[WMA_PRO_MAX_CHANNELS];

        int channelsForCurSubframeLocal = channels;
        boolean fixedChannelLayout = false;
        int minChannelLen = 0;

        for (int c = 0; c < channels; c++) {
            channelsCtx[c].numSubframes = 0;
            numSamples[c] = 0;
        }

        if (maxNumSubframes == 1 || gb.read(1) != 0) {
            fixedChannelLayout = true;
        }

        do {
            int subframeLen;

            // which channels contain this subframe?
            for (int c = 0; c < channels; c++) {
                if (numSamples[c] == minChannelLen) {
                    boolean oneChoice =
                            fixedChannelLayout
                            || channelsForCurSubframeLocal == 1
                            || minChannelLen == samplesPerFrame - minSamplesPerSubframe;

                    containsSubframe[c] = oneChoice || gb.read(1) != 0;
                } else {
                    containsSubframe[c] = false;
                }
            }

            subframeLen = decodeSubframeLength(gb, minChannelLen);
            if (subframeLen <= 0) {
                return false;
            }

            minChannelLen += subframeLen;
            for (int c = 0; c < channels; c++) {
                ChannelCtx chan = channelsCtx[c];
                if (containsSubframe[c]) {
                    if (chan.numSubframes >= MAX_SUBFRAMES) {
                        return false;
                    }
                    chan.subframeLen[chan.numSubframes] = subframeLen;
                    numSamples[c] += subframeLen;
                    chan.numSubframes++;
                    if (numSamples[c] > samplesPerFrame) {
                        return false;
                    }
                } else if (numSamples[c] <= minChannelLen) {
                    if (numSamples[c] < minChannelLen) {
                        channelsForCurSubframeLocal = 0;
                        minChannelLen = numSamples[c];
                    }
                    channelsForCurSubframeLocal++;
                }
            }
        } while (minChannelLen < samplesPerFrame);

        // populate subframe_offset
        for (int c = 0; c < channels; c++) {
            int offset = 0;
            ChannelCtx chan = channelsCtx[c];
            for (int i = 0; i < chan.numSubframes; i++) {
                chan.subframeOffset[i] = offset;
                offset += chan.subframeLen[i];
            }
        }

        return true;
    }

    private int decodeSubframeLength(BitReader gb, int offset) {
        int frameLenShift = 0;

        if (offset == samplesPerFrame - minSamplesPerSubframe) {
            return minSamplesPerSubframe;
        }

        if (maxSubframeLenBit != 0) {
            if (gb.read(1) != 0) {
                frameLenShift = 1 + gb.read(subframeLenBits - 1);
            }
        } else {
            frameLenShift = gb.read(subframeLenBits);
        }

        int subframeLen = samplesPerFrame >> frameLenShift;

        if (subframeLen < minSamplesPerSubframe || subframeLen > samplesPerFrame) {
            return -1;
        }
        return subframeLen;
    }

    private boolean decodeSubframe(BitReader gb) {
        int offset = samplesPerFrame;
        int subframeLen = samplesPerFrame;
        int totalSamples = samplesPerFrame * channels;
        boolean transmitCoeffs = false;

        for (int i = 0; i < channels; i++) {
            channelsCtx[i].grouped = false;
            if (offset > channelsCtx[i].decodedSamples) {
                offset = channelsCtx[i].decodedSamples;
                subframeLen = channelsCtx[i].subframeLen[channelsCtx[i].curSubframe];
            }
        }

        channelsForCurSubframe = 0;
        for (int i = 0; i < channels; i++) {
            int curSf = channelsCtx[i].curSubframe;
            totalSamples -= channelsCtx[i].decodedSamples;

            if (offset == channelsCtx[i].decodedSamples
                    && subframeLen == channelsCtx[i].subframeLen[curSf]) {
                totalSamples -= channelsCtx[i].subframeLen[curSf];
                channelsCtx[i].decodedSamples += channelsCtx[i].subframeLen[curSf];
                channelIndexesForCurSubframe[channelsForCurSubframe++] = i;
            }
        }

        if (totalSamples == 0) {
            parsedAllSubframes = true;
        }

        tableIdxRuntime = log2(samplesPerFrame / subframeLen);
        numBands = numSfb[tableIdxRuntime];

        offset += samplesPerFrame >> 1;

        for (int i = 0; i < channelsForCurSubframe; i++) {
            int c = channelIndexesForCurSubframe[i];
            channelsCtx[c].coeffsBase = offset;
        }

        subframeLenField = subframeLen;
        escLen = log2(subframeLen - 1) + 1;

        // Skip extended header (fill bits) if present
        if (gb.read(1) != 0) {
            int numFillBits = gb.read(2);
            if (numFillBits == 0) {
                int n = gb.read(4);
                numFillBits = (n > 0 ? gb.read(n) : 0) + 1;
            }
            for (int j = 0; j < numFillBits; j++) {
                gb.read(1);
            }
        }

        // Reserved bit
        if (gb.read(1) != 0) {
            return false;
        }

        if (!decodeChannelTransform(gb)) {
            return false;
        }

        for (int i = 0; i < channelsForCurSubframe; i++) {
            int c = channelIndexesForCurSubframe[i];
            channelsCtx[c].transmitCoefs = gb.read(1) != 0;
            if (channelsCtx[c].transmitCoefs) {
                transmitCoeffs = true;
            }
        }

        if (transmitCoeffs) {
            int quantStep = 90 * bitsPerSample >> 4;  // = 90

            transmitNumVecCoeffs = gb.read(1) != 0;
            if (transmitNumVecCoeffs) {
                int numBits = log2((subframeLen + 3) / 4) + 1;
                for (int i = 0; i < channelsForCurSubframe; i++) {
                    int c = channelIndexesForCurSubframe[i];
                    int n = gb.read(numBits) << 2;
                    if (n > subframeLen) {
                        return false;
                    }
                    channelsCtx[c].numVecCoeffs = n;
                }
            } else {
                for (int i = 0; i < channelsForCurSubframe; i++) {
                    int c = channelIndexesForCurSubframe[i];
                    channelsCtx[c].numVecCoeffs = subframeLen;
                }
            }

            // quantisation step (signed 6 bits, with escape ladder at +31/-32)
            int step = readSbits(gb, 6);
            quantStep += step;
            if (step == -32 || step == 31) {
                int sign = step == 31 ? 0 : -1;
                int extra = 0;
                int s2;
                while ((s2 = gb.read(5)) == 31) {
                    extra += 31;
                }
                quantStep += ((extra + s2) ^ sign) - sign;
            }

            if (channelsForCurSubframe == 1) {
                channelsCtx[channelIndexesForCurSubframe[0]].quantStep = quantStep;
            } else {
                int modifierLen = gb.read(3);
                for (int i = 0; i < channelsForCurSubframe; i++) {
                    int c = channelIndexesForCurSubframe[i];
                    channelsCtx[c].quantStep = quantStep;
                    if (gb.read(1) != 0) {
                        if (modifierLen != 0) {
                            channelsCtx[c].quantStep += gb.read(modifierLen) + 1;
                        } else {
                            channelsCtx[c].quantStep++;
                        }
                    }
                }
            }

            if (!decodeScaleFactors(gb)) {
                return false;
            }
        }

        // Decode (or zero) coefficient buffers
        for (int i = 0; i < channelsForCurSubframe; i++) {
            int c = channelIndexesForCurSubframe[i];
            if (channelsCtx[c].transmitCoefs && gb.remainingBits() > 0) {
                if (!decodeCoeffs(gb, c)) {
                    return false;
                }
            } else {
                Arrays.fill(channelsCtx[c].out, channelsCtx[c].coeffsBase,
                        channelsCtx[c].coeffsBase + subframeLen, 0f);
            }
        }

        if (transmitCoeffs) {
            inverseChannelTransform();

            int imdctIdx = log2(subframeLen) - BLOCK_MIN_BITS;
            Imdct imdct = imdcts[imdctIdx];

            for (int i = 0; i < channelsForCurSubframe; i++) {
                int c = channelIndexesForCurSubframe[i];
                ChannelCtx ch = channelsCtx[c];
                int[] sf = ch.savedScaleFactors[ch.scaleFactorRow];

                for (int b = 0; b < numBands; b++) {
                    int end = Math.min(sfbOffsets[tableIdxRuntime][b + 1], subframeLen);
                    int start = sfbOffsets[tableIdxRuntime][b];
                    if (end <= start) {
                        continue;
                    }
                    int sfVal = sf[b];
                    double exp = ch.quantStep
                            - (double) (ch.maxScaleFactor - sfVal) * ch.scaleFactorStep;
                    float quant = (float) Math.pow(10.0, exp / 20.0);

                    FloatDsp.vectorFmulScalar(
                            tmp, start,
                            ch.out, ch.coeffsBase + start,
                            quant,
                            end - start);
                }

                imdct.inverse(tmp, 0, ch.out, ch.coeffsBase);
            }
        }

        wmaProWindow();

        for (int i = 0; i < channelsForCurSubframe; i++) {
            int c = channelIndexesForCurSubframe[i];
            if (channelsCtx[c].curSubframe >= channelsCtx[c].numSubframes) {
                return false;
            }
            channelsCtx[c].curSubframe++;
        }

        return true;
    }

    private boolean decodeChannelTransform(BitReader gb) {
        numChgroups = 0;
        if (channelsForCurSubframe <= 1) {
            return true;
        }

        int remainingChannels = channelsForCurSubframe;

        // "Channel transform bit" — must be 0 for our subset
        if (gb.read(1) != 0) {
            return false;
        }

        while (remainingChannels > 0 && numChgroups < channelsForCurSubframe) {
            ChannelGrp grp = chgroup[numChgroups];
            grp.numChannels = 0;
            grp.transform = false;
            int writeIdx = 0;

            if (remainingChannels > 2) {
                for (int i = 0; i < channelsForCurSubframe; i++) {
                    int ci = channelIndexesForCurSubframe[i];
                    if (!channelsCtx[ci].grouped && gb.read(1) != 0) {
                        grp.numChannels++;
                        channelsCtx[ci].grouped = true;
                        grp.channelIndex[writeIdx++] = ci;
                    }
                }
            } else {
                grp.numChannels = remainingChannels;
                for (int i = 0; i < channelsForCurSubframe; i++) {
                    int ci = channelIndexesForCurSubframe[i];
                    if (!channelsCtx[ci].grouped) {
                        grp.channelIndex[writeIdx++] = ci;
                    }
                    channelsCtx[ci].grouped = true;
                }
            }

            if (grp.numChannels == 2) {
                if (gb.read(1) != 0) {
                    if (gb.read(1) != 0) {
                        return false;  // patch-welcome
                    }
                } else {
                    grp.transform = true;
                    if (channels == 2) {
                        grp.decorrelationMatrix[0] = 1.0f;
                        grp.decorrelationMatrix[1] = -1.0f;
                        grp.decorrelationMatrix[2] = 1.0f;
                        grp.decorrelationMatrix[3] = 1.0f;
                    } else {
                        grp.decorrelationMatrix[0] = 0.70703125f;
                        grp.decorrelationMatrix[1] = -0.70703125f;
                        grp.decorrelationMatrix[2] = 0.70703125f;
                        grp.decorrelationMatrix[3] = 0.70703125f;
                    }
                }
            } else if (grp.numChannels > 2) {
                if (gb.read(1) != 0) {
                    grp.transform = true;
                    if (gb.read(1) != 0) {
                        decodeDecorrelationMatrix(gb, grp);
                    } else if (grp.numChannels <= 6) {
                        int srcOff = WmaProTables.DefaultDecorrOffsets[grp.numChannels];
                        int n = grp.numChannels * grp.numChannels;
                        System.arraycopy(WmaProTables.DefaultDecorrelationMatrices, srcOff,
                                grp.decorrelationMatrix, 0, n);
                    }
                }
            }

            if (grp.transform) {
                if (gb.read(1) == 0) {
                    for (int i = 0; i < numBands; i++) {
                        grp.transformBand[i] = (byte) gb.read(1);
                    }
                } else {
                    for (int i = 0; i < numBands; i++) {
                        grp.transformBand[i] = 1;
                    }
                }
            }

            remainingChannels -= grp.numChannels;
            numChgroups++;
        }
        return true;
    }

    private void decodeDecorrelationMatrix(BitReader gb, ChannelGrp grp) {
        byte[] rotationOffset = new byte[WMA_PRO_MAX_CHANNELS * WMA_PRO_MAX_CHANNELS];
        int offset = 0;

        Arrays.fill(grp.decorrelationMatrix, 0, channels * channels, 0f);

        int n = grp.numChannels * (grp.numChannels - 1) / 2;
        for (int i = 0; i < n; i++) {
            rotationOffset[i] = (byte) gb.read(6);
        }

        for (int i = 0; i < grp.numChannels; i++) {
            grp.decorrelationMatrix[grp.numChannels * i + i] = gb.read(1) != 0 ? 1.0f : -1.0f;
        }

        float[] sin64 = WmaProTables.Sin64;
        for (int i = 1; i < grp.numChannels; i++) {
            for (int x = 0; x < i; x++) {
                for (int y = 0; y < i + 1; y++) {
                    float v1 = grp.decorrelationMatrix[x * grp.numChannels + y];
                    float v2 = grp.decorrelationMatrix[i * grp.numChannels + y];
                    int m = rotationOffset[offset + x];
                    float sinv;
                    float cosv;

                    if (m < 32) {
                        sinv = sin64[m];
                        cosv = sin64[32 - m];
                    } else {
                        sinv = sin64[64 - m];
                        cosv = -sin64[m - 32];
                    }

                    grp.decorrelationMatrix[y + x * grp.numChannels] = v1 * sinv - v2 * cosv;
                    grp.decorrelationMatrix[y + i * grp.numChannels] = v1 * cosv + v2 * sinv;
                }
            }
            offset += i;
        }
    }

    private void inverseChannelTransform() {
        float[] data = new float[WMA_PRO_MAX_CHANNELS];

        for (int gi = 0; gi < numChgroups; gi++) {
            ChannelGrp grp = chgroup[gi];
            if (!grp.transform) {
                continue;
            }

            int nc = grp.numChannels;

            for (int sfb = 0; sfb < numBands; sfb++) {
                int bandStart = sfbOffsets[tableIdxRuntime][sfb];
                int bandEnd = Math.min(sfbOffsets[tableIdxRuntime][sfb + 1], subframeLenField);

                if (grp.transformBand[sfb] == 1) {
                    for (int y = bandStart; y < bandEnd; y++) {
                        for (int c = 0; c < nc; c++) {
                            int ci = grp.channelIndex[c];
                            data[c] = channelsCtx[ci].out[channelsCtx[ci].coeffsBase + y];
                        }
                        int matIdx = 0;
                        for (int c = 0; c < nc; c++) {
                            float sum = 0;
                            for (int k = 0; k < nc; k++) {
                                sum += data[k] * grp.decorrelationMatrix[matIdx++];
                            }
                            int ci = grp.channelIndex[c];
                            channelsCtx[ci].out[channelsCtx[ci].coeffsBase + y] = sum;
                        }
                    }
                } else if (channels == 2) {
                    int len = bandEnd - bandStart;
                    if (len <= 0) {
                        continue;
                    }
                    int c0 = grp.channelIndex[0];
                    int c1 = grp.channelIndex[1];
                    float k = 181.0f / 128.0f;

                    FloatDsp.vectorFmulScalar(
                            channelsCtx[c0].out, channelsCtx[c0].coeffsBase + bandStart,
                            channelsCtx[c0].out, channelsCtx[c0].coeffsBase + bandStart,
                            k, len);
                    FloatDsp.vectorFmulScalar(
                            channelsCtx[c1].out, channelsCtx[c1].coeffsBase + bandStart,
                            channelsCtx[c1].out, channelsCtx[c1].coeffsBase + bandStart,
                            k, len);
                }
            }
        }
    }

    private static final int[] FVAL_TAB = {
            0x00000000, 0x3F800000, 0x40000000, 0x40400000,
            0x40800000, 0x40A00000, 0x40C00000, 0x40E00000,
            0x41000000, 0x41100000, 0x41200000, 0x41300000,
            0x41400000, 0x41500000, 0x41600000, 0x41700000,
    };

    private boolean decodeCoeffs(BitReader gb, int c) {
        ChannelCtx chan = channelsCtx[c];
        float[] coeffsBuf = chan.out;
        int coeffsOff = chan.coeffsBase;

        int vlcTable = gb.read(1);
        Vlc vlc;
        int[] runTab;
        float[] levelTab;

        if (vlcTable != 0) {
            vlc = WmaProVlc.coef1();
            runTab = WmaProTables.Coef1Run;
            levelTab = WmaProTables.Coef1Level;
        } else {
            vlc = WmaProVlc.coef0();
            runTab = WmaProTables.Coef0Run;
            levelTab = WmaProTables.Coef0Level;
        }

        int rlMode = 0;
        int curCoeff = 0;
        int numZeros = 0;

        int[] vals = new int[4];

        while ((transmitNumVecCoeffs || rlMode == 0)
                && curCoeff + 3 < chan.numVecCoeffs) {
            int idx = WmaProVlc.vec4().decode(gb);
            if (idx < 0) {
                for (int j = 0; j < 4; j += 2) {
                    int idx2 = WmaProVlc.vec2().decode(gb);
                    if (idx2 < 0) {
                        int v0 = WmaProVlc.vec1().decode(gb);
                        if (v0 == WmaProTables.HuffVec1Size - 1) {
                            v0 += WmaCommon.getLargeVal(gb);
                        }
                        int v1 = WmaProVlc.vec1().decode(gb);
                        if (v1 == WmaProTables.HuffVec1Size - 1) {
                            v1 += WmaCommon.getLargeVal(gb);
                        }
                        vals[j] = Float.floatToRawIntBits((float) v0);
                        vals[j + 1] = Float.floatToRawIntBits((float) v1);
                    } else {
                        vals[j] = FVAL_TAB[(idx2 >> 4) & 0xF];
                        vals[j + 1] = FVAL_TAB[idx2 & 0xF];
                    }
                }
            } else {
                vals[0] = FVAL_TAB[(idx >> 12) & 0xF];
                vals[1] = FVAL_TAB[(idx >> 8) & 0xF];
                vals[2] = FVAL_TAB[(idx >> 4) & 0xF];
                vals[3] = FVAL_TAB[idx & 0xF];
            }

            for (int j = 0; j < 4; j++) {
                if (vals[j] != 0) {
                    int sign = (gb.read(1) - 1) & 1;
                    int bits = vals[j] ^ (sign << 31);
                    coeffsBuf[coeffsOff + curCoeff] = Float.intBitsToFloat(bits);
                    numZeros = 0;
                } else {
                    coeffsBuf[coeffsOff + curCoeff] = 0;
                    if (++numZeros > subframeLenField >> 8) {
                        rlMode = 1;
                    }
                }
                curCoeff++;
            }
        }

        if (curCoeff < subframeLenField) {
            Arrays.fill(coeffsBuf, coeffsOff + curCoeff, coeffsOff + subframeLenField, 0f);

            if (!WmaCommon.runLevelDecode(
                    gb, vlc, levelTab, runTab,
                    coeffsBuf, coeffsOff,
                    curCoeff,
                    subframeLenField,
                    subframeLenField,
                    escLen)) {
                return false;
            }
        }
        return true;
    }

    private boolean decodeScaleFactors(BitReader gb) {
        for (int i = 0; i < channelsForCurSubframe; i++) {
            int c = channelIndexesForCurSubframe[i];
            ChannelCtx chan = channelsCtx[c];

            int destRow = 1 - chan.scaleFactorIdx;
            int[] dest = chan.savedScaleFactors[destRow];
            int[] srcLatest = chan.savedScaleFactors[chan.scaleFactorIdx];
            chan.scaleFactorRow = destRow;

            if (chan.reuseSf) {
                for (int b = 0; b < numBands; b++) {
                    int srcBand = sfOffsets[tableIdxRuntime][chan.tableIdx][b];
                    dest[b] = srcLatest[srcBand];
                }
            }

            boolean readNewSf = chan.curSubframe == 0 || gb.read(1) != 0;
            if (readNewSf) {
                if (!chan.reuseSf) {
                    chan.scaleFactorStep = gb.read(2) + 1;
                    int val = 45 / chan.scaleFactorStep;
                    for (int b = 0; b < numBands; b++) {
                        val += WmaProVlc.scale().decode(gb);
                        dest[b] = val;
                    }
                } else {
                    for (int b = 0; b < numBands; b++) {
                        int idx = WmaProVlc.scaleRl().decode(gb);
                        int skip;
                        int val;
                        int sign;

                        if (idx == 0) {
                            int code = gb.read(14);
                            val = code >>> 6;
                            sign = (code & 1) - 1;
                            skip = (code & 0x3F) >> 1;
                        } else if (idx == 1) {
                            break;
                        } else {
                            skip = WmaProTables.ScaleRlRun[idx];
                            val = WmaProTables.ScaleRlLevel[idx];
                            sign = gb.read(1) - 1;
                        }

                        b += skip;
                        if (b >= numBands) {
                            return false;
                        }
                        dest[b] += (val ^ sign) - sign;
                    }
                }
                chan.scaleFactorIdx = destRow;
                chan.tableIdx = tableIdxRuntime;
                chan.reuseSf = true;
            }

            int max = dest[0];
            for (int b = 1; b < numBands; b++) {
                if (dest[b] > max) {
                    max = dest[b];
                }
            }
            chan.maxScaleFactor = max;
        }
        return true;
    }

    private void wmaProWindow() {
        for (int i = 0; i < channelsForCurSubframe; i++) {
            int c = channelIndexesForCurSubframe[i];
            ChannelCtx ch = channelsCtx[c];

            int winlen = ch.prevBlockLen;
            int start = ch.coeffsBase - (winlen >> 1);

            if (subframeLenField < winlen) {
                start += (winlen - subframeLenField) >> 1;
                winlen = subframeLenField;
            }

            int winIdx = log2(winlen) - BLOCK_MIN_BITS;
            float[] window = windows[winIdx];

            int halfLen = winlen >> 1;

            FloatDsp.vectorFmulWindow(
                    ch.out, start,
                    ch.out, start,
                    ch.out, start + halfLen,
                    window, halfLen);

            ch.prevBlockLen = subframeLenField;
        }
    }

    private static int log2(int v) {
        int n = 0;
        while ((v >>= 1) != 0) {
            n++;
        }
        return n;
    }

    private static int readSbits(BitReader gb, int n) {
        int v = gb.read(n);
        int shift = 32 - n;
        return (v << shift) >> shift;
    }

    private static int getRate(int sampleRate) {
        if (sampleRate > 44100) {
            return 48000;
        } else if (sampleRate > 32000) {
            return 44100;
        } else if (sampleRate > 24000) {
            return 32000;
        } else {
            return 24000;
        }
    }
}
