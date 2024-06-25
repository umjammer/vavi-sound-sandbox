/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.twinvq;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.twinvq.TwinVQDec.TwinVQContext;

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

    interface HexaConsumer<T, U, V, W, X, Y> {

        void accept(T t, U u, V v, W w, X x, Y y);
    }

    interface HeptaConsumer<T, U, V, W, X, Y, Z> {

        void accept(T t, U u, V v, W w, X x, Y y, Z z);
    }

    static final int AV_SAMPLE_FMT_FLTP = 0; // ???

    static final int AV_TX_FLOAT_MDCT = 1;

    static final int AV_CODEC_FLAG_BITEXACT = 1 << 23;

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
        public short bit_rate;

        public int extradata_size;

        public byte[] extradata;

        public int block_align;
        public AVChannelLayout ch_layout;
        public Object sample_fmt;
        public int flags;
        public int codec_type;
    }

    static class AVFloatDSPContext {

        public AVFloatDSPContext(int i) {
        }

        public void vector_fmul(int i, int i1, float[] tmpBuf, int blockSize) {
        }

        public void vector_fmul_window(int out2, float[] prev_buf, int i, float[] buf1, int i1, float ffSineWindow, int i2) {

        }

        public void butterflies_float(float[] out1, int p1, float[] out2, int p2, short size) {
        }
    }

    static class AVTXContext {

    }

    static class AVFrame {

        public int nb_samples;
        public Object extended_data;
        public byte[] data;
    }

    static class AVPacket {

        public byte[] data;
        public int size;
    }

    static void ff_init_ff_sine_windows(double log) {

    }

    // Generate a sine window.
    static void ff_sine_window_init(float[] window, int n) {
        for (int i = 0; i < n; i++)
            window[i] = (float) Math.sin((i + 0.5) * (Math.PI / (2.0 * n)));
    }

    static float[] ff_sine_windows;

    static int ff_get_buffer(AVCodecContext avctx, AVFrame frame, int flags) {
//        int override_dimensions = 1;
        int ret = 0;
//
//        assert av_codec_is_decoder(avctx.codec);
//
//        if (avctx.codec_type == AVMEDIA_TYPE_VIDEO) {
//            if (avctx.width > INT_MAX - STRIDE_ALIGN ||
//                    (ret = av_image_check_size2(FFALIGN(avctx.width, STRIDE_ALIGN), avctx.height, avctx.max_pixels, AV_PIX_FMT_NONE, 0, avctx)) < 0 || avctx.pix_fmt < 0) {
//                logger.log(Level.ERROR, "video_get_buffer: image parameters invalid");
//                return -1;
//            }
//
//            if (frame.width <= 0 || frame.height <= 0) {
//                frame.width = FFMAX(avctx.width, AV_CEIL_RSHIFT(avctx.coded_width, avctx.lowres));
//                frame.height = FFMAX(avctx.height, AV_CEIL_RSHIFT(avctx.coded_height, avctx.lowres));
//                override_dimensions = 0;
//            }
//
//            if (frame.data[0] != 0 || frame.data[1] != 0  || frame.data[2] != 0  || frame.data[3] != 0 ) {
//                logger.log(Level.ERROR, "pic.data[*]!=NULL in get_buffer_internal\n");
//                return -1;
//            }
//        } else if (avctx.codec_type == AVMEDIA_TYPE_AUDIO) {
//            if (frame.nb_samples * (long) avctx.ch_layout.nb_channels > avctx.max_samples) {
//                logger.log(Level.ERROR, "samples per frame %d, exceeds max_samples %d", frame.nb_samples, avctx.max_samples);
//                return -1;
//            }
//        }
//        ret = ff_decode_frame_props(avctx, frame);
//        if (ret < 0)
//            return -1;
//
//        avctx.sw_pix_fmt = avctx.pix_fmt;
//
//        ret = avctx.get_buffer2(avctx, frame, flags);
//        if (ret < 0)
//            return -1;
//
//        validate_avframe_allocation(avctx, frame);
//
//        ret = ff_attach_decode_data(frame);
//        if (ret < 0)
//            return -1;
//
//        if (avctx.codec_type == AVMEDIA_TYPE_VIDEO && override_dimensions == 0 &&
//                !(ffcodec(avctx.codec).caps_internal & FF_CODEC_CAP_EXPORTS_CROPPING)) {
//            frame.width = avctx.width;
//            frame.height = avctx.height;
//        }
//
        return ret;
    }

    static int av_tx_init(AVTXContext ctx, HexaConsumer tx, int /*AVTXType*/ type,
                   int inv, int len, float[] scale, long flags) {
        return 0;
    }

    static int FF_ARRAY_ELEMS(float[][][] barkHist) {
        return 0;
    }
}
