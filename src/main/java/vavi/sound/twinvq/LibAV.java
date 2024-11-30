/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.io.DataInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import vavi.sound.twinvq.TwinVQDec.TwinVQContext;
import vavi.sound.twinvq.VFQ.VqfContext;

import static java.lang.System.getLogger;


/**
 * LibAV.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-06-25 nsano initial version <br>
 */
public class LibAV {

    private static final Logger logger = getLogger(LibAV.class.getName());

    interface TetraFunction<T, U, V, W, R> {

        R apply(T t, U u, V v, W w);
    }

    interface TetraConsumer<T, U, V, W, X> {

        void accept(T t, U u, V v, W w, X x);
    }

    interface HeptaConsumer<T, U, V, W, X, Y, Z> {

        void accept(T t, U u, V v, W w, X x, Y y, Z z);
    }

//#region common.h

    static final int AVERROR_INVALIDDATA = -1;

    static int MKTAG(int a, int b, int c, int d) {
        return a | (b << 8) | (c << 16) | (d << 24);
    }

//#endregion

//#region avformat

    static class AVInputFormat {
        String name;
        String long_name;
        int priv_data_size;
        Function<byte[], Integer> read_probe;
        Function<AVFormatContext, Integer> read_header;
        Function<AVFormatContext, AVPacket> read_packet;
        TetraFunction<AVFormatContext, Integer, Long, Integer, Integer> read_seek;
        String extensions;
    }

    static class AVFormatContext {

        public VqfContext priv_data = new VqfContext();
        public AVStream[] streams = new AVStream[1];
        public DataInputStream pb;
        public Map<String, Object> metadata = new HashMap<>();
    }

    static class AVStream {

        public AVCodecContext codecpar = new AVCodecContext();
        public long start_time;

        public AVStream(AVFormatContext s, Object o) {
            s.streams[0] = this;
        }
    }

    static class AVProbeData {

        public byte[] buf;
    }

//#endregion

//#region avcodec

    static final int AV_SAMPLE_FMT_FLTP = 0; // ???

    static final int AV_TX_FLOAT_MDCT = 1;

    static final int AV_CODEC_FLAG_BITEXACT = 1 << 23;

    static final int AV_INPUT_BUFFER_PADDING_SIZE = 8;

    static class AVChannelLayout {

        /**
         * Number of channels in this layout. Mandatory field.
         */
        int nb_channels;
    }

    static class AVCodecContext {

        public TwinVQContext priv_data;

        public int sample_rate;

        /**
         * the average bitrate
         * - encoding: Set by user; unused for constant quantizer encoding.
         * - decoding: Set by libavcodec. 0 or some bitrate if this info is available in the stream.
         */
        public int bit_rate;

        public int extradata_size;

        public byte[] extradata;

        public int block_align;
        public AVChannelLayout ch_layout = new AVChannelLayout();
        public Object sample_fmt;
        public int flags;
        public int codec_type;
        public Object pix_fmt;
        public Object sw_pix_fmt;
        public int channels;
        public int codec_id;
        int max_samples;
    }

    static class AVFloatDSPContext {

        public AVFloatDSPContext(int flags) {
        }

        /**
         * Calculate the product of two vectors of floats and store the result in
         * a vector of floats.
         *
         * @param dst  output vector
         *             constraints: 32-byte aligned
         * @param src0 first input vector
         *             constraints: 32-byte aligned
         * @param src0P first source vector start index
         * @param src1 second input vector
         *             constraints: 32-byte aligned
         * @param src1P second source vector start index
         * @param len  number of elements in the input
         *             constraints: multiple of 16
         */
        public void vector_fmul(float[] dst, int destP, float[] src0, int src0P, float[] src1, int src1P, int len) {
            for (int i = 0; i < len; i++) {
                dst[destP + i] = src0[src0P + i] * src1[src1P + i];
            }
        }

        /**
         * Overlap/add with window function.
         * Used primarily by MDCT-based audio codecs.
         * Source and destination vectors must overlap exactly or not at all.
         *
         * @param dst  result vector
         *             constraints: 16-byte aligned
         * @param src0 first source vector
         *             constraints: 16-byte aligned
         * @param src0P first source vector start index
         * @param src1 second source vector
         *             constraints: 16-byte aligned
         * @param src1P second source vector start index
         * @param win  half-window vector
         *             constraints: 16-byte aligned
         * @param len  length of vector
         *             constraints: multiple of 4
         */
        public void vector_fmul_window(float[] dst, int dstP, float[] src0, int src0P, float[] src1, int src1P, float[] win, int len) {
            int halfLen = len / 2;
            int winP = 0;  // Starting index for the window array

            // Perform the vector multiply with window and accumulate
            for (int i = 0; i < halfLen; i++) {
                // Multiply src0[i] with win[i] and accumulate in the destination
                float win0 = win[winP + i];
                dst[dstP + i] = src0[src0P + i] * win0 + src1[src1P + i] * win[winP + len - 1 - i];
            }

            for (int i = halfLen; i < len; i++) {
                // Continue multiplying and accumulating for the second half
                float win1 = win[winP + i];
                dst[dstP + i] = src0[src0P + i] * win1 - src1[src1P + i] * win[winP + len - 1 - i];
            }
        }

        /**
         * Calculate the sum and difference of two vectors of floats.
         *
         * @param v1  first input vector, sum output, 16-byte aligned
         * @param p1 first input vector start index
         * @param v2  second input vector, difference output, 16-byte aligned
         * @param p2 second input vector start index
         * @param len length of vectors, multiple of 4
         */
        public void butterflies_float(float[] v1, int p1, float[] v2, int p2, short len) {
            for (int i = 0; i < len; i++) {
                float a = v1[p1 + i];
                float b = v2[p2 + i];
                v1[p1 + i] = a + b;  // sum stored in v1
                v2[p2 + i] = a - b;  // difference stored in v2
            }
        }
    }

    static class AVTXContext {
        interface TXFunction extends LibAV.TetraConsumer<AVTXContext, float[], Integer, float[], Integer> {}
    }

    static class AVFrame {

        public int nb_samples;
        public Object extended_data;
        public byte[] data;
    }

    static class AVPacket {

        public byte[] data;
        public int size;
        public int duration;
        public int stream_index;
        public long pos;

        public AVPacket(int size) {
            this.size = size;
            data = new byte[size];
        }
    }

    static Map<Integer, float[]> ff_sine_windows = new HashMap<>();

    static {
        for (int i = 5; i < 14; i++)
            ff_init_ff_sine_windows(i);
    }

    static void ff_init_ff_sine_windows(int index) {
        float[] windows = new float[1 << index];
        ff_sine_window_init(windows, 1 << index);
logger.log(Level.DEBUG, "index: " + index + ", windows: " + windows.length);
        ff_sine_windows.put(index, windows);
    }

    // Generate a sine window.
    static void ff_sine_window_init(float[] window, int n) {
        for (int i = 0; i < n; i++)
            window[i] = (float) Math.sin((i + 0.5) * (Math.PI / (2.0 * n)));
    }

    static int ff_get_buffer(LibAV.AVCodecContext avctx, AVFrame frame, int flags) {
        int ret = 0;

        if (frame.nb_samples * (long) avctx.ch_layout.nb_channels > avctx.max_samples) {
            logger.log(Level.ERROR, "samples per frame %d, exceeds max_samples %d", frame.nb_samples, avctx.max_samples);
            return -1;
        }
//        ret = ff_decode_frame_props(avctx, frame);
        if (ret < 0)
            return -1;

        avctx.sw_pix_fmt = avctx.pix_fmt;

//        ret = avctx.get_buffer2(avctx, frame, flags);
        if (ret < 0)
            return -1;

//        validate_avframe_allocation(avctx, frame);

//        ret = ff_attach_decode_data(frame);
        if (ret < 0)
            return -1;

        return ret;
    }

    static int av_tx_init(AVTXContext[] ctx, AVTXContext.TXFunction[] tx, int index, int /*AVTXType*/ type,
                          int inv, int len, float[] scale, long flags) {
logger.log(Level.DEBUG, "type: " + type);
        scale[0] = 1f;
        MDCT mdct = new MDCT(Float.SIZE, false, scale[0]);
        tx[index] = (x, in, inp, out, op) -> mdct.imdctHalf(in, inp, out, op);
        return 0;
    }

    static int FF_ARRAY_ELEMS(float[][][] barkHist) {
        return 0;
    }

//#endregion
}
