/*
 * TwinVQ decoder
 * Copyright (c) 2009 Vitor Sessak
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

package vavi.sound.twinvq;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.twinvq.GetBits.GetBitContext;
import vavi.sound.twinvq.LibAV.AVCodecContext;
import vavi.sound.twinvq.LibAV.AVFloatDSPContext;
import vavi.sound.twinvq.LibAV.AVTXContext;
import vavi.sound.twinvq.LibAV.HeptaConsumer;
import vavi.sound.twinvq.LibAV.HexaConsumer;
import vavi.sound.twinvq.LibAV.TetraFunction;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;
import static vavi.sound.twinvq.MetaSoundTwinVQData.ff_metasound_lsp11;
import static vavi.sound.twinvq.MetaSoundTwinVQData.ff_metasound_lsp16;
import static vavi.sound.twinvq.MetaSoundTwinVQData.ff_metasound_lsp22;
import static vavi.sound.twinvq.MetaSoundTwinVQData.ff_metasound_lsp44;
import static vavi.sound.twinvq.MetaSoundTwinVQData.ff_metasound_lsp8;
import static vavi.sound.twinvq.TwinVQ.ff_twinvq_decode_init;
import static vavi.sound.twinvq.TwinVQ.ff_twinvq_wtype_to_ftype_table;
import static vavi.sound.twinvq.TwinVQ.twinvq_memset_float;
import static vavi.sound.twinvq.TwinVQData.*;
import static vavi.sound.twinvq.TwinVQDec.TwinVQCodec.TWINVQ_CODEC_VQF;


/**
 * TwinVQDec.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-04-05 nsano initial version <br>
 */
public class TwinVQDec {

    private static final Logger logger = getLogger(TwinVQDec.class.getName());

    static final int AVERROR_INVALIDDATA = -1;

    /* assume b>0 */
    static int ROUNDED_DIV(int a, int b) {
        return a >= 0 ? a + (b >> 1) : a - (int) ((b >> 1) / (float) b);
    }

    static int FFSIGN(float a) { return a > 0 ? 1 : -1; }

    enum TwinVQCodec {
        TWINVQ_CODEC_VQF,
        TWINVQ_CODEC_METASOUND,
    };

    enum TwinVQFrameType {
        /** Short frame  (divided in n   sub-blocks) */
        TWINVQ_FT_SHORT,
        /** Medium frame (divided in m<n sub-blocks) */
        TWINVQ_FT_MEDIUM,
        /** Long frame   (single sub-block + PPC) */
        TWINVQ_FT_LONG,
        /** Periodic Peak Component (part of the long frame) */
        TWINVQ_FT_PPC,
    };

    static final int TWINVQ_PPC_SHAPE_CB_SIZE = 64;
    static final int TWINVQ_PPC_SHAPE_LEN_MAX = 60;
    static final float TWINVQ_SUB_AMP_MAX       = 4500.0f;
    static final float TWINVQ_MULAW_MU          = 100.0f;
    static final int TWINVQ_GAIN_BITS         = 8;
    static final float TWINVQ_AMP_MAX           = 13000.0f;
    static final int TWINVQ_SUB_GAIN_BITS     = 5;
    static final int TWINVQ_WINDOW_TYPE_BITS  = 4;
    static final int TWINVQ_PGAIN_MU          = 200;
    static final int TWINVQ_LSP_COEFS_MAX     = 20;
    static final int TWINVQ_LSP_SPLIT_MAX     = 4;
    static final int TWINVQ_CHANNELS_MAX      = 2;
    static final int TWINVQ_SUBBLOCKS_MAX     = 16;
    static final int TWINVQ_BARK_N_COEF_MAX   = 4;

    static final int TWINVQ_MAX_FRAMES_PER_PACKET = 2;

    /**
     * Parameters and tables that are different for each frame type
     */
    static class TwinVQFrameMode {

        /** Number subblocks in each frame */
        byte sub;
        short[] bark_tab;

        /** number of distinct bark scale envelope values */
        byte bark_env_size;

        /** codebook for the bark scale envelope (BSE) */
        short[] bark_cb;
        /** number of BSE CB coefficients to read */
        byte bark_n_coef;
        /** number of bits of the BSE coefs */
        byte bark_n_bit;

        //@{
        /** main codebooks for spectrum data */
        short[] cb0;
        short[] cb1;
        //@}

        /** number of spectrum coefficients to read */
        byte cb_len_read;

        public TwinVQFrameMode(byte sub, short[] bark_tab, byte bark_env_size, short[] bark_cb, byte bark_n_coef, byte bark_n_bit, short[] cb0, short[] cb1, byte cb_len_read) {
            this.sub = sub;
            this.bark_tab = bark_tab;
            this.bark_env_size = bark_env_size;
            this.bark_cb = bark_cb;
            this.bark_n_coef = bark_n_coef;
            this.bark_n_bit = bark_n_bit;
            this.cb0 = cb0;
            this.cb1 = cb1;
            this.cb_len_read = cb_len_read;
        }
    }

    static class TwinVQFrameData {

        int window_type;
        TwinVQFrameType ftype;

        byte[] main_coeffs = new byte[1024];
        byte[] ppc_coeffs = new byte[TWINVQ_PPC_SHAPE_LEN_MAX];

        byte[] gain_bits = new byte[TWINVQ_CHANNELS_MAX];
        byte[] sub_gain_bits = new byte[TWINVQ_CHANNELS_MAX * TWINVQ_SUBBLOCKS_MAX];

        byte[][][] bark1 = new byte[TWINVQ_CHANNELS_MAX][TWINVQ_SUBBLOCKS_MAX][TWINVQ_BARK_N_COEF_MAX];
        byte[][] bark_use_hist = new byte[TWINVQ_CHANNELS_MAX][TWINVQ_SUBBLOCKS_MAX];

        byte[] lpc_idx1 = new byte[TWINVQ_CHANNELS_MAX];
        byte[][] lpc_idx2 = new byte[TWINVQ_CHANNELS_MAX][TWINVQ_LSP_SPLIT_MAX];
        byte[] lpc_hist_idx = new byte[TWINVQ_CHANNELS_MAX];

        int[] p_coef = new int[TWINVQ_CHANNELS_MAX];
        int[] g_coef = new int[TWINVQ_CHANNELS_MAX];
    }

    /**
     * Parameters and tables that are different for every combination of
     * bitrate/sample rate
     */
    static class TwinVQModeTab {

        /** frame type-dependent parameters */
        TwinVQFrameMode[] fmode;

        /** frame size in samples */
        short size;
        /** number of lsp coefficients */
        byte n_lsp;
        float[] lspcodebook;

        // number of bits of the different LSP CB coefficients
        byte lsp_bit0;
        byte lsp_bit1;
        byte lsp_bit2;

        /** number of CB entries for the LSP decoding */
        byte lsp_split;
        /** PPC shape CB */
        short[] ppc_shape_cb;

        /** number of the bits for the PPC period value */
        byte ppc_period_bit;

        /** number of bits of the PPC shape CB coeffs */
        byte ppc_shape_bit;
        /** size of PPC shape CB */
        byte ppc_shape_len;
        /** bits for PPC gain */
        byte pgain_bit;

        /** finalant for peak period to peak width conversion */
        short peak_per2wid;

        public TwinVQModeTab(TwinVQFrameMode[] fmode, short size, byte n_lsp, float[] lspcodebook, byte lsp_bit0, byte lsp_bit1, byte lsp_bit2, byte lsp_split, short[] ppc_shape_cb, byte ppc_period_bit, byte ppc_shape_bit, byte ppc_shape_len, byte pgain_bit, short peak_per2wid) {
            this.fmode = fmode;
            this.size = size;
            this.n_lsp = n_lsp;
            this.lspcodebook = lspcodebook;
            this.lsp_bit0 = lsp_bit0;
            this.lsp_bit1 = lsp_bit1;
            this.lsp_bit2 = lsp_bit2;
            this.lsp_split = lsp_split;
            this.ppc_shape_cb = ppc_shape_cb;
            this.ppc_period_bit = ppc_period_bit;
            this.ppc_shape_bit = ppc_shape_bit;
            this.ppc_shape_len = ppc_shape_len;
            this.pgain_bit = pgain_bit;
            this.peak_per2wid = peak_per2wid;
        }
    }

    static class TwinVQContext {
        AVCodecContext avctx;
        AVFloatDSPContext fdsp;
        AVTXContext[] tx = new AVTXContext[3];
        HexaConsumer[] tx_fn = new HexaConsumer[3];

        TwinVQModeTab mtab;

        int is_6kbps;

        // history
        /** LSP coefficients of the last frame */
        float[][] lsp_hist = new float[2][20];
        /** BSE coefficients of last frame */
        float[][][] bark_hist = new float[3][2][40];

        // bitstream parameters
        short[][] permut = new short[4][4096];
        /** main codebook stride */
        byte[][] length = new byte[4][2];
        byte[] length_change = new byte[4];
        /** bits for the main codebook */
        byte[][][] bits_main_spec = new byte[2][4][2];
        int[] bits_main_spec_change = new int[4];
        int[] n_div = new int[4];

        float[] spectrum;
        /** non-interleaved output */
        float[] curr_frame;
        /** non-interleaved previous frame */
        float[] prev_frame;
        int[] last_block_pos = new int[2];
        int discarded_packets;

        float[][] cos_tabs = new float[3][];

        // scratch buffers
        float[] tmp_buf;

        int frame_size, frames_per_packet, cur_frame;
        TwinVQFrameData[] bits = new TwinVQFrameData[TWINVQ_MAX_FRAMES_PER_PACKET];

        TwinVQCodec codec;

        // int (read_bitstream)(AVCodecContext avctx, TwinVQContext tctx, byte[] buf, int buf_size);
        TetraFunction<AVCodecContext, TwinVQContext, byte[], Integer, Integer> read_bitstream;
        // void (dec_bark_env)( TwinVQContext tctx, byte[][] in, int use_hist, int ch, float[] out, float gain, TwinVQFrameType ftype);
        HeptaConsumer<TwinVQContext, byte[], Integer, Integer, float[], Float, TwinVQFrameType> dec_bark_env;
        // void (decode_ppc)(TwinVQContext tctx, int period_coef, int g_coef, float[] shape, float[] speech);
        HeptaConsumer<TwinVQContext, Integer, Integer, float[], Integer, float[], Integer> decode_ppc;
    }

    /**
     * Clip a signed integer value into the amin-amax range.
     * @param a value to clip
     * @param amin minimum value of the clip range
     * @param amax maximum value of the clip range
     * @return clipped value
     */
    static int av_clip_c(int a, int amin, int amax) {
        if      (a < amin) return amin;
        else if (a > amax) return amax;
        else               return a;
    }

    static byte[] ff_log2_tab = new byte[256];

    static int ff_log2_c(int v) {
        int n = 0;
        if ((v & 0xffff0000) != 0) {
            v >>= 16;
            n += 16;
        }
        if ((v & 0xff00) != 0) {
            v >>= 8;
            n += 8;
        }
        n += ff_log2_tab[v];

        return n;
    }

    static float twinvq_mulawinv(float y, float clip, float mu) {
        y = av_clip_c((int) (y / clip), -1, 1);
        return (float) (clip * FFSIGN(y) * (Math.exp(Math.log(1 + mu) * Math.abs(y)) - 1) / mu);
    }

    static final TwinVQModeTab mode_08_08 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 8, bark_tab_s08_64, (byte) 10, fcb08s, (byte) 1, (byte) 5, cb0808s0, cb0808s1, (byte) 18),
                    new TwinVQFrameMode((byte) 2, bark_tab_m08_256, (byte) 20, fcb08m, (byte) 2, (byte) 5, cb0808m0, cb0808m1, (byte) 16),
                    new TwinVQFrameMode((byte) 1, bark_tab_l08_512, (byte) 30, fcb08l, (byte) 3, (byte) 6, cb0808l0, cb0808l1, (byte) 17)
            },
            (short) 512, (byte) 12, ff_metasound_lsp8, (byte) 1, (byte) 5, (byte) 3, (byte) 3, shape08, (byte) 8, (byte) 28, (byte) 20, (byte) 6, (short) 40
    );

    static final TwinVQModeTab mode_11_08 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 8, bark_tab_s11_64, (byte) 10, fcb11s, (byte) 1, (byte) 5, cb1108s0, cb1108s1, (byte) 29),
                    new TwinVQFrameMode((byte) 2, bark_tab_m11_256, (byte) 20, fcb11m, (byte) 2, (byte) 5, cb1108m0, cb1108m1, (byte) 24),
                    new TwinVQFrameMode((byte) 1, bark_tab_l11_512, (byte) 30, fcb11l, (byte) 3, (byte) 6, cb1108l0, cb1108l1, (byte) 27)
            },
            (short) 512, (byte) 16, ff_metasound_lsp11, (byte) 1, (byte) 6, (byte) 4, (byte) 3, shape11, (byte) 9, (byte) 36, (byte) 30, (byte) 7, (short) 90
    );

    static final TwinVQDec.TwinVQModeTab mode_11_10 = new TwinVQModeTab(
            new TwinVQDec.TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 8, bark_tab_s11_64, (byte) 10, fcb11s, (byte) 1, (byte) 5, cb1110s0, cb1110s1, (byte) 21),
                    new TwinVQFrameMode((byte) 2, bark_tab_m11_256, (byte) 20, fcb11m, (byte) 2, (byte) 5, cb1110m0, cb1110m1, (byte) 18),
                    new TwinVQFrameMode((byte) 1, bark_tab_l11_512, (byte) 30, fcb11l, (byte) 3, (byte) 6, cb1110l0, cb1110l1, (byte) 20)
            },
            (short) 512, (byte) 16, ff_metasound_lsp11, (byte) 1, (byte) 6, (byte) 4, (byte) 3, shape11, (byte) 9, (byte) 36, (byte) 30, (byte) 7, (short) 90
    );

    static final TwinVQModeTab mode_16_16 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 8, bark_tab_s16_128, (byte) 10, fcb16s, (byte) 1, (byte) 5, cb1616s0, cb1616s1, (byte) 16),
                    new TwinVQFrameMode((byte) 2, bark_tab_m16_512, (byte) 20, fcb16m, (byte) 2, (byte) 5, cb1616m0, cb1616m1, (byte) 15),
                    new TwinVQFrameMode((byte) 1, bark_tab_l16_1024, (byte) 30, fcb16l, (byte) 3, (byte) 6, cb1616l0, cb1616l1, (byte) 16)
            },
            (short) 1024, (byte) 16, ff_metasound_lsp16, (byte) 1, (byte) 6, (byte) 4, (byte) 3, shape16, (byte) 9, (byte) 56, (byte) 60, (byte) 7, (short) 180
    );

    static final TwinVQModeTab mode_22_20 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 8, bark_tab_s22_128, (byte) 10, fcb22s_1, (byte) 1, (byte) 6, cb2220s0, cb2220s1, (byte) 18),
                    new TwinVQFrameMode((byte) 2, bark_tab_m22_512, (byte) 20, fcb22m_1, (byte) 2, (byte) 6, cb2220m0, cb2220m1, (byte) 17),
                    new TwinVQFrameMode((byte) 1, bark_tab_l22_1024, (byte) 32, fcb22l_1, (byte) 4, (byte) 6, cb2220l0, cb2220l1, (byte) 18)
            },
            (short) 1024, (byte) 16, ff_metasound_lsp22, (byte) 1, (byte) 6, (byte) 4, (byte) 3, shape22_1, (byte) 9, (byte) 56, (byte) 36, (byte) 7, (short) 144
    );

    static final TwinVQModeTab mode_22_24 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 8, bark_tab_s22_128, (byte) 10, fcb22s_1, (byte) 1, (byte) 6, cb2224s0, cb2224s1, (byte) 15),
                    new TwinVQFrameMode((byte) 2, bark_tab_m22_512, (byte) 20, fcb22m_1, (byte) 2, (byte) 6, cb2224m0, cb2224m1, (byte) 14),
                    new TwinVQFrameMode((byte) 1, bark_tab_l22_1024, (byte) 32, fcb22l_1, (byte) 4, (byte) 6, cb2224l0, cb2224l1, (byte) 15)
            },
            (short) 1024, (byte) 16, ff_metasound_lsp22, (byte) 1, (byte) 6, (byte) 4, (byte) 3, shape22_1, (byte) 9, (byte) 56, (byte) 36, (byte) 7, (short) 144
    );

    static final TwinVQModeTab mode_22_32 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 4, bark_tab_s22_128, (byte) 10, fcb22s_2, (byte) 1, (byte) 6, cb2232s0, cb2232s1, (byte) 11),
                    new TwinVQFrameMode((byte) 2, bark_tab_m22_256, (byte) 20, fcb22m_2, (byte) 2, (byte) 6, cb2232m0, cb2232m1, (byte) 11),
                    new TwinVQFrameMode((byte) 1, bark_tab_l22_512, (byte) 32, fcb22l_2, (byte) 4, (byte) 6, cb2232l0, cb2232l1, (byte) 12)
            },
            (short) 512, (byte) 16, lsp22_2, (byte) 1, (byte) 6, (byte) 4, (byte) 4, shape22_2, (byte) 9, (byte) 56, (byte) 36, (byte) 7, (byte) 72
        );

    static final TwinVQModeTab mode_44_40 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 16, bark_tab_s44_128, (byte) 10, fcb44s, (byte) 1, (byte) 6, cb4440s0, cb4440s1, (byte) 18),
                    new TwinVQFrameMode((byte) 4, bark_tab_m44_512, (byte) 20, fcb44m, (byte) 2, (byte) 6, cb4440m0, cb4440m1, (byte) 17),
                    new TwinVQFrameMode((byte) 1, bark_tab_l44_2048, (byte) 40, fcb44l, (byte) 4, (byte) 6, cb4440l0, cb4440l1, (byte) 17)
            },
            (short) 2048, (byte) 20, ff_metasound_lsp44, (byte) 1, (byte) 6, (byte) 4, (byte) 4, shape44, (byte) 9, (byte) 84, (byte) 54, (byte) 7, (byte) 432
        );

    static final TwinVQModeTab mode_44_48 = new TwinVQModeTab(
            new TwinVQFrameMode[] {
                    new TwinVQFrameMode((byte) 16, bark_tab_s44_128, (byte) 10, fcb44s, (byte) 1, (byte) 6, cb4448s0, cb4448s1, (byte) 15),
                    new TwinVQFrameMode((byte) 4, bark_tab_m44_512, (byte) 20, fcb44m, (byte) 2, (byte) 6, cb4448m0, cb4448m1, (byte) 14),
                    new TwinVQFrameMode((byte) 1, bark_tab_l44_2048, (byte) 40, fcb44l, (byte) 4, (byte) 6, cb4448l0, cb4448l1, (byte) 14)
            },
            (short) 2048, (byte) 20, ff_metasound_lsp44, (byte) 1, (byte) 6, (byte) 4, (byte) 4, shape44, (byte) 9, (byte) 84, (byte) 54, (byte) 7, (short) 432
    );

    /**
     * Evaluate a * b / 400 rounded to the nearest integer. When, for example,
     * a * b == 200 and the nearest integer is ill-defined, use a table to emulate
     * the following broken float-based implementation used by the binary decoder:
     *
     * @code static int very_broken_op(int a, int b)
     * {
     * static float test; // Ugh, force gcc to do the division first...
     * <p>
     * test = a / 400.0;
     * return b * test + 0.5;
     * }
     * @endcode
     * @note if this function is replaced by just ROUNDED_DIV(a * b, 400.0), the
     * stddev between the original file (before encoding with Yamaha encoder) and
     * the decoded output increases, which leads one to believe that the encoder
     * expects exactly this broken calculation.
     */
    private int very_broken_op(int a, int b) {
        int x = a * b + 200;
        int size;
        final byte[] rtab;

        if (x % 400 != 0 || b % 5 != 0)
            return x / 400;

        x /= 400;

        size = tabs[b / 5].size;
        rtab = tabs[b / 5].tab;
        return x - rtab[size * ff_log2_c(2 * (x - 1) / size) + (x - 1) % size];
    }

    /**
     * Sum to data a periodic peak of a given period, width and shape.
     *
     * @param period the period of the peak divided by 400.0
     */
    private void add_peak(int period, int width, final float[] shape,
                          float ppc_gain, float[] speech, int len) {
        int i, j;

        final int shape_end = len; // shape
        int shapeP = 0;
        int center;

        // First peak centered around zero
        for (i = 0; i < width / 2; i++)
            speech[i] += ppc_gain * shape[shapeP++];

        for (i = 1; i < ROUNDED_DIV(len, width); i++) {
            center = very_broken_op(period, i);
            for (j = -width / 2; j < (width + 1) / 2; j++)
                speech[j + center] += ppc_gain * shape[shapeP++];
        }

        // For the last block, be careful not to go beyond the end of the buffer
        center = very_broken_op(period, i);
        for (j = -width / 2; j < (width + 1) / 2 && shapeP < shape_end; j++)
            speech[j + center] += ppc_gain * shape[shapeP++];
    }

    public void decode_ppc(TwinVQDec.TwinVQContext tctx, int period_coef, int g_coef, float[] shape, int shapeP, float[] speech, int speechP) {
        TwinVQModeTab mtab = tctx.mtab;
        int isampf = tctx.avctx.sample_rate / 1000;
        int ibps = tctx.avctx.bit_rate / (1000 * tctx.avctx.ch_layout.nb_channels);
        int min_period = ROUNDED_DIV(40 * 2 * mtab.size, isampf);
        int max_period = ROUNDED_DIV(40 * 2 * mtab.size * 6, isampf);
        int period_range = max_period - min_period;
        float pgain_step = 25000.0f / ((1 << mtab.pgain_bit) - 1);
        float ppc_gain = 1.0f / 8192 *
                twinvq_mulawinv(pgain_step * g_coef + pgain_step / 2, 25000.0f, TWINVQ_PGAIN_MU);

        // This is actually the period multiplied by 400. It is just linearly coded
        // between its maximum and minimum value.
        int period = min_period +
                ROUNDED_DIV(period_coef * period_range, (1 << mtab.ppc_period_bit) - 1);
        int width;

        if (isampf == 22 && ibps == 32) {
            // For some unknown reason, NTT decided to code this case differently...
            width = ROUNDED_DIV((period + 800) * mtab.peak_per2wid, 400 * mtab.size);
        } else
            width = period * mtab.peak_per2wid / (400 * mtab.size);

        add_peak(period, width, shape, ppc_gain, speech, mtab.ppc_shape_len);
    }

    public void dec_bark_env(TwinVQContext tctx, byte[] in, int use_hist,
                              int ch, float[] out, float gain, TwinVQDec.TwinVQFrameType ftype) {
        final TwinVQModeTab mtab = tctx.mtab;
        int i, j;
        float[] hist = tctx.bark_hist[ftype.ordinal()][ch];
        float val = new float[] {0.4f, 0.35f, 0.28f}[ftype.ordinal()];
        int bark_n_coef = mtab.fmode[ftype.ordinal()].bark_n_coef;
        int fw_cb_len = (mtab.fmode[ftype.ordinal()].bark_env_size & 0xff) / bark_n_coef;
        int idx = 0;
        int outP = 0; // TODO out

        for (i = 0; i < fw_cb_len; i++)
            for (j = 0; j < bark_n_coef; j++, idx++) {
                float tmp2 = mtab.fmode[ftype.ordinal()].bark_cb[fw_cb_len * in[j] + i] * (1.0f / 4096);
                float st = use_hist != 0 ? (1.0f - val) * tmp2 + val * hist[idx] + 1.0f : tmp2 + 1.0f;

                hist[idx] = tmp2;
                if (st < -1.0)
                    st = 1.0f;

                twinvq_memset_float(out, outP, st * gain, mtab.fmode[ftype.ordinal()].bark_tab[idx]);
                outP += mtab.fmode[ftype.ordinal()].bark_tab[idx];
            }
    }

    private void read_cb_data(TwinVQContext tctx, GetBitContext gb, byte[] dst, TwinVQFrameType ftype) {
        int dstP = 0;

        for (int i = 0; i < tctx.n_div[ftype.ordinal()]; i++) {
            int bs_second_part = (i >= tctx.bits_main_spec_change[ftype.ordinal()]) ? 1 : 0;

            dst[dstP++] = (byte) gb.get_bits(tctx.bits_main_spec[0][ftype.ordinal()][bs_second_part]);
            dst[dstP++] = (byte) gb.get_bits(tctx.bits_main_spec[1][ftype.ordinal()][bs_second_part]);
        }
    }

    /** */
    public int twinvq_read_bitstream(AVCodecContext avctx, TwinVQContext tctx, byte[] buf, int buf_size) {
        TwinVQDec.TwinVQFrameData bits = tctx.bits[0];
        final TwinVQModeTab mtab = tctx.mtab;
        int channels = tctx.avctx.ch_layout.nb_channels;
        int sub;
        GetBitContext gb = new GetBitContext();
        int i, j, k, ret;


        if ((ret = gb.init_get_bits8(buf, buf_size)) < 0)
            return ret;
        gb.skip_bits(gb.get_bits(8));

        bits.window_type = gb.get_bits(TWINVQ_WINDOW_TYPE_BITS);

        if (bits.window_type > 8) {
            logger.log(Level.ERROR, "Invalid window type, broken sample?");
            return AVERROR_INVALIDDATA;
        }

        bits.ftype = ff_twinvq_wtype_to_ftype_table[tctx.bits[0].window_type];

        sub = mtab.fmode[bits.ftype.ordinal()].sub & 0xff;

        read_cb_data(tctx, gb, bits.main_coeffs, bits.ftype);

        for (i = 0; i < channels; i++)
            for (j = 0; j < sub; j++)
                for (k = 0; k < mtab.fmode[bits.ftype.ordinal()].bark_n_coef; k++)
                    bits.bark1[i][j][k] =
                            (byte) gb.get_bits(mtab.fmode[bits.ftype.ordinal()].bark_n_bit);

        for (i = 0; i < channels; i++)
            for (j = 0; j < sub; j++)
                bits.bark_use_hist[i][j] = gb.get_bits1();

        if (bits.ftype == TwinVQFrameType.TWINVQ_FT_LONG) {
            for (i = 0; i < channels; i++)
                bits.gain_bits[i] = (byte) gb.get_bits(TWINVQ_GAIN_BITS);
        } else {
            for (i = 0; i < channels; i++) {
                bits.gain_bits[i] = (byte) gb.get_bits(TWINVQ_GAIN_BITS);
                for (j = 0; j < sub; j++)
                    bits.sub_gain_bits[i * sub + j] = (byte) gb.get_bits(TWINVQ_SUB_GAIN_BITS);
            }
        }

        for (i = 0; i < channels; i++) {
            bits.lpc_hist_idx[i] = (byte) gb.get_bits(mtab.lsp_bit0);
            bits.lpc_idx1[i] = (byte) gb.get_bits(mtab.lsp_bit1);

            for (j = 0; j < mtab.lsp_split; j++)
                bits.lpc_idx2[i][j] = (byte) gb.get_bits(mtab.lsp_bit2);
        }

        if (bits.ftype == TwinVQFrameType.TWINVQ_FT_LONG) {
            read_cb_data(tctx, gb, bits.ppc_coeffs, TwinVQFrameType.TWINVQ_FT_PPC);
            for (i = 0; i < channels; i++) {
                bits.p_coef[i] = gb.get_bits(mtab.ppc_period_bit);
                bits.g_coef[i] = gb.get_bits(mtab.pgain_bit);
            }
        }

        return (gb.get_bits_count() + 7) / 8;
    }

    private int twinvq_decode_init(AVCodecContext avctx) {
        int isampf, ibps, channels;
        TwinVQContext tctx = avctx.priv_data;

        if (avctx.extradata == null || avctx.extradata_size < 12) {
            logger.log(Level.ERROR, "Missing or incomplete extradata");
            return AVERROR_INVALIDDATA;
        }
        channels = ByteUtil.readLeInt(avctx.extradata) + 1;
        avctx.bit_rate = (short) (ByteUtil.readLeInt(avctx.extradata, 4) * 1000);
        isampf = ByteUtil.readLeInt(avctx.extradata, 8);

        if (isampf < 8 || isampf > 44) {
            logger.log(Level.ERROR, "Unsupported sample rate");
            return AVERROR_INVALIDDATA;
        }
        switch (isampf) {
            case 44:
                avctx.sample_rate = 44100;
                break;
            case 22:
                avctx.sample_rate = 22050;
                break;
            case 11:
                avctx.sample_rate = 11025;
                break;
            default:
                avctx.sample_rate = isampf * 1000;
                break;
        }

        if (channels <= 0 || channels > TWINVQ_CHANNELS_MAX) {
            logger.log(Level.ERROR, "Unsupported number of channels: %i", channels);
            return -1;
        }
//        av_channel_layout_uninit(avctx.ch_layout);
//        av_channel_layout_default(avctx.ch_layout, channels);

        ibps = (int) (avctx.bit_rate / (1000 * channels));
        if (ibps < 8 || ibps > 48) {
            logger.log(Level.ERROR, "Bad bitrate per channel value %d", ibps);
            return AVERROR_INVALIDDATA;
        }

        switch ((isampf << 8) + ibps) {
            case (8 << 8) + 8:
                tctx.mtab = mode_08_08;
                break;
            case (11 << 8) + 8:
                tctx.mtab = mode_11_08;
                break;
            case (11 << 8) + 10:
                tctx.mtab = mode_11_10;
                break;
            case (16 << 8) + 16:
                tctx.mtab = mode_16_16;
                break;
            case (22 << 8) + 20:
                tctx.mtab = mode_22_20;
                break;
            case (22 << 8) + 24:
                tctx.mtab = mode_22_24;
                break;
            case (22 << 8) + 32:
                tctx.mtab = mode_22_32;
                break;
            case (44 << 8) + 40:
                tctx.mtab = mode_44_40;
                break;
            case (44 << 8) + 48:
                tctx.mtab = mode_44_48;
                break;
            default:
                logger.log(Level.ERROR, "This version does not support %d kHz - %d kbit/s/ch mode.",
                        isampf, isampf);
                return -1;
        }

        tctx.codec = TWINVQ_CODEC_VQF;
        tctx.read_bitstream = this::twinvq_read_bitstream;
        tctx.dec_bark_env = this::dec_bark_env;
        tctx.decode_ppc = this::decode_ppc;
        tctx.frame_size = avctx.bit_rate * tctx.mtab.size / avctx.sample_rate + 8;
        tctx.is_6kbps = 0;
        if (avctx.block_align != 0 && avctx.block_align * 8L / tctx.frame_size > 1) {
            logger.log(Level.ERROR, "VQF TwinVQ should have only one frame per packet");
            return AVERROR_INVALIDDATA;
        }

        return ff_twinvq_decode_init(avctx);
    }
}
