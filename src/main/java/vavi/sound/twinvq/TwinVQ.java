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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import vavi.sound.twinvq.LibAV.AVCodecContext;
import vavi.sound.twinvq.LibAV.AVFloatDSPContext;
import vavi.sound.twinvq.LibAV.AVFrame;
import vavi.sound.twinvq.LibAV.AVPacket;
import vavi.sound.twinvq.LibAV.AVTXContext;
import vavi.sound.twinvq.TwinVQDec.TwinVQContext;
import vavi.sound.twinvq.TwinVQDec.TwinVQFrameData;
import vavi.sound.twinvq.TwinVQDec.TwinVQFrameType;
import vavi.sound.twinvq.TwinVQDec.TwinVQModeTab;

import static java.lang.System.getLogger;
import static vavi.sound.twinvq.LibAV.AVERROR_INVALIDDATA;
import static vavi.sound.twinvq.LibAV.AV_CODEC_FLAG_BITEXACT;
import static vavi.sound.twinvq.LibAV.AV_SAMPLE_FMT_FLTP;
import static vavi.sound.twinvq.LibAV.AV_TX_FLOAT_MDCT;
import static vavi.sound.twinvq.LibAV.FF_ARRAY_ELEMS;
import static vavi.sound.twinvq.LibAV.av_tx_init;
import static vavi.sound.twinvq.LibAV.ff_get_buffer;
import static vavi.sound.twinvq.LibAV.ff_init_ff_sine_windows;
import static vavi.sound.twinvq.LibAV.ff_sine_windows;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_AMP_MAX;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_CHANNELS_MAX;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_GAIN_BITS;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_LSP_COEFS_MAX;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_MAX_FRAMES_PER_PACKET;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_MULAW_MU;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_PPC_SHAPE_CB_SIZE;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_PPC_SHAPE_LEN_MAX;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_SUBBLOCKS_MAX;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_SUB_AMP_MAX;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_SUB_GAIN_BITS;
import static vavi.sound.twinvq.TwinVQDec.TWINVQ_WINDOW_TYPE_BITS;
import static vavi.sound.twinvq.TwinVQDec.TwinVQCodec.TWINVQ_CODEC_METASOUND;
import static vavi.sound.twinvq.TwinVQDec.TwinVQFrameType.TWINVQ_FT_LONG;
import static vavi.sound.twinvq.TwinVQDec.TwinVQFrameType.TWINVQ_FT_MEDIUM;
import static vavi.sound.twinvq.TwinVQDec.TwinVQFrameType.TWINVQ_FT_PPC;
import static vavi.sound.twinvq.TwinVQDec.TwinVQFrameType.TWINVQ_FT_SHORT;
import static vavi.sound.twinvq.TwinVQDec.twinvq_mulawinv;


/**
 * TwinVQ.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-04-06 nsano initial version <br>
 * @see "https://github.com/libav/libav/blob/master/libavcodec/twinvq.c"
 */
public class TwinVQ {

    private static final Logger logger = getLogger(TwinVQ.class.getName());

    enum TwinVQCodec {
        TWINVQ_CODEC_VQF,
        TWINVQ_CODEC_METASOUND,
    }

    /** @note not speed critical, hence not optimized */
    static void twinvq_memset_float(float[] buf, int bufP, float val, int size) {
        while (size-- != 0)
            buf[bufP++] = val;
    }

    /**
     * Evaluate a single LPC amplitude spectrum envelope coefficient from the line
     * spectrum pairs.
     *
     * @param lsp     a vector of the cosine of the LSP values
     * @param cos_val cos(PI*i/N) where i is the index of the LPC amplitude
     * @param order   the order of the LSP (and the size of the *lsp buffer). Must
     *                be a multiple of four.
     * @return the LPC value
     * <p>
     * TODO reuse code from Vorbis decoder: vorbis_floor0_decode
     */
    static float eval_lpc_spectrum(float[] lsp, int lspP, float cos_val, int order) {
        int j;
        float p = 0.5f;
        float q = 0.5f;
        float two_cos_w = 2.0f * cos_val;

        for (j = 0; j + 1 < order; j += 2 * 2) {
            // Unroll the loop once since order is a multiple of four
            q *= lsp[lspP + j] - two_cos_w;
            p *= lsp[lspP + j + 1] - two_cos_w;

            q *= lsp[lspP + j + 2] - two_cos_w;
            p *= lsp[lspP + j + 3] - two_cos_w;
        }

        p *= p * (2.0f - two_cos_w);
        q *= q * (2.0f + two_cos_w);

        return 0.5f / (p + q);
    }

    /**
     * Evaluate the LPC amplitude spectrum envelope from the line spectrum pairs.
     */
    static void eval_lpcenv(TwinVQContext tctx, float[] cos_vals, float[] lpc) {
        int i;
        TwinVQModeTab mtab = tctx.mtab;
        int size_s = mtab.size / mtab.fmode[TWINVQ_FT_SHORT.ordinal()].sub;

        for (i = 0; i < size_s / 2; i++) {
            float cos_i = tctx.cos_tabs[0][i];
            lpc[i] = eval_lpc_spectrum(cos_vals, 0, cos_i, mtab.n_lsp);
            lpc[size_s - i - 1] = eval_lpc_spectrum(cos_vals, 0, -cos_i, mtab.n_lsp);
        }
    }

    /** */
    static void interpolate(float[] out, int outP, float v1, float v2, int size) {
        float step = (v1 - v2) / (size + 1);

        for (int i = 0; i < size; i++) {
            v2 += step;
            out[outP + i] = v2;
        }
    }

    /** */
    static float get_cos(int idx, int part, float[] cos_tab, int size) {
        return part != 0 ? -cos_tab[size - idx - 1]
                : cos_tab[idx];
    }

    /**
     * Evaluate the LPC amplitude spectrum envelope from the line spectrum pairs.
     * Probably for speed reasons, the coefficients are evaluated as
     * <pre>
     * siiiibiiiisiiiibiiiisiiiibiiiisiiiibiiiis ...
     * </pre>
     * where s is an evaluated value, i is a value interpolated from the others
     * and b might be either calculated or interpolated, depending on an
     * unexplained condition.
     *
     * @param step the size of a block "siiiibiiii"
     * @param in   the cosine of the LSP data
     * @param part is 0 for 0...PI (positive cosine values) and 1 for PI...2PI
     *             (negative cosine values)
     * @param size the size of the whole output
     */
    static void eval_lpcenv_or_interp(TwinVQContext tctx, TwinVQFrameType ftype,
                                      float[] out, int outP, float[] in, int inP,
                                      int size, int step, int part) {
        TwinVQModeTab mtab = tctx.mtab;
        float[] cos_tab = tctx.cos_tabs[ftype.ordinal()];

        // Fill the 's'
        for (int i = 0; i < size; i += step)
            out[i] = eval_lpc_spectrum(in, inP, get_cos(i, part, cos_tab, size), mtab.n_lsp);

        // Fill the 'iiiibiiii'
        for (int i = step; i <= size - 2 * step; i += step) {
            if (out[i + step] + out[i - step] > 1.95 * out[i] || out[i + step] >= out[i - step]) {
                interpolate(out, outP + i - step + 1, out[i], out[i - step], step - 1);
            } else {
                out[i - step / 2] = eval_lpc_spectrum(in, inP,
                                get_cos(i - step / 2, part, cos_tab, size), mtab.n_lsp);
                interpolate(out, outP + i - step + 1, out[i - step / 2], out[i - step], step / 2 - 1);
                interpolate(out, outP + i - step / 2 + 1, out[i], out[i - step / 2], step / 2 - 1);
            }
        }

        interpolate(out, outP + size - 2 * step + 1, out[size - step], out[size - 2 * step], step - 1);
    }

    /** */
    static void eval_lpcenv_2parts(TwinVQContext tctx, TwinVQFrameType ftype,
                                   float[] buf, float[] lpc, int size, int step) {
        eval_lpcenv_or_interp(tctx, ftype, lpc, 0, buf, 0, size / 2, step, 0);
        eval_lpcenv_or_interp(tctx, ftype, lpc, size / 2, buf, 0, size / 2, 2 * step, 1);

        interpolate(lpc, size / 2 - step + 1, lpc[size / 2], lpc[size / 2 - step], step);

        twinvq_memset_float(lpc, size - 2 * step + 1, lpc[size - 2 * step], 2 * step - 1);
    }

    /**
     * Inverse quantization. Read CB coefficients for cb1 and cb2 from the
     * bitstream, sum the corresponding vectors and write the result to *out
     * after permutation.
     */
    static void dequant(TwinVQContext tctx, byte[] cb_bits, float[] out,
                        TwinVQFrameType ftype, short[] cb0, short[] cb1, int cb1P, int cb_len) {
        int pos = 0;

        int cb_bitsP = 0;
        for (int i = 0; i < tctx.n_div[ftype.ordinal()]; i++) {
            int sign0 = 1;
            int sign1 = 1;
            int length = tctx.length[ftype.ordinal()][i >= (tctx.length_change[ftype.ordinal()] & 0xff) ? 1 : 0];
            int bitstream_second_part = (i >= tctx.bits_main_spec_change[ftype.ordinal()]) ? 1 : 0;

            int bits = tctx.bits_main_spec[0][ftype.ordinal()][bitstream_second_part];
            int tmp0 = cb_bits[cb_bitsP++];
            if (bits == 7) {
                if ((tmp0 & 0x40) != 0)
                    sign0 = -1;
                tmp0 &= 0x3F;
            }

            bits = tctx.bits_main_spec[1][ftype.ordinal()][bitstream_second_part];
            int tmp1 = cb_bits[cb_bitsP++];
            if (bits == 7) {
                if ((tmp1 & 0x40) != 0)
                    sign1 = -1;
                tmp1 &= 0x3F;
            }

            int tab0 = tmp0 * cb_len; // cb0
            int tab1 = tmp1 * cb_len; // cb1
//logger.log(Level.TRACE, "dq[%3d]: %d, %02x, %d, %02x, %d".formatted(i, tctx.bits_main_spec[0][ftype.ordinal()][bitstream_second_part], tmp0, tctx.bits_main_spec[1][ftype.ordinal()][bitstream_second_part], tmp1, bitstream_second_part));
//logger.log(Level.TRACE, "bits: " + bits + ", tmp0: " + tmp0 + ", tmp1: " + tmp1 + ", cb_len: " + cb_len + ", tab0: " + tab0 + ", tab1: " + tab1 + ", cb0: " + cb0.length + ", cb1: " + cb1.length + ", cb1P: " + cb1P);

            for (int j = 0; j < length; j++) {
//logger.log(Level.TRACE, "%d, %d, %d, %d".formatted(pos + j, tab0 + j, cb1P + tab1 + j, tctx.permut[ftype.ordinal()][pos + j] & 0xffff));
                out[tctx.permut[ftype.ordinal()][pos + j] & 0xffff] = sign0 * cb0[tab0 + j] +
                        sign1 * cb1[cb1P + tab1 + j];
            }

            pos += length;
        }
    }

    /** */
    static void dec_gain(TwinVQContext tctx, TwinVQFrameType ftype, float[] out) {
        TwinVQModeTab mtab = tctx.mtab;
        TwinVQFrameData bits = tctx.bits[tctx.cur_frame];
        int channels = tctx.avctx.ch_layout.nb_channels;
        int sub = mtab.fmode[ftype.ordinal()].sub;
        float step = TWINVQ_AMP_MAX / ((1 << TWINVQ_GAIN_BITS) - 1);
        float sub_step = TWINVQ_SUB_AMP_MAX / ((1 << TWINVQ_SUB_GAIN_BITS) - 1);

        if (ftype == TWINVQ_FT_LONG) {
            for (int i = 0; i < channels; i++)
                out[i] = (1.0f / (1 << 13)) *
                        twinvq_mulawinv(step * 0.5f + step * bits.gain_bits[i], TWINVQ_AMP_MAX, TWINVQ_MULAW_MU);
        } else {
            for (int i = 0; i < channels; i++) {
                float val = (1.0f / (1 << 23)) *
                        twinvq_mulawinv(step * 0.5f + step * bits.gain_bits[i], TWINVQ_AMP_MAX, TWINVQ_MULAW_MU);

                for (int j = 0; j < sub; j++)
                    out[i * sub + j] =
                            val * twinvq_mulawinv(sub_step * 0.5f + sub_step * bits.sub_gain_bits[i * sub + j],
                                    TWINVQ_SUB_AMP_MAX, TWINVQ_MULAW_MU);
            }
        }
    }

    /**
     * Rearrange the LSP coefficients so that they have a minimum distance of
     * min_dist. This function does it exactly as described in section of 3.2.4
     * of the G.729 specification (but interestingly is different from what the
     * reference decoder actually does).
     */
    static void rearrange_lsp(int order, float[] lsp, float min_dist) {
        float min_dist2 = min_dist * 0.5f;
        for (int i = 1; i < order; i++)
            if (lsp[i] - lsp[i - 1] < min_dist) {
                float avg = (lsp[i] + lsp[i - 1]) * 0.5f;

                lsp[i - 1] = avg - min_dist2;
                lsp[i] = avg + min_dist2;
            }
    }

    static void decode_lsp(TwinVQContext tctx, int lpc_idx1, byte[] lpc_idx2,
                           int lpc_hist_idx, float[] lsp, float[] hist) {
        TwinVQModeTab mtab = tctx.mtab;

        float[] cb = mtab.lspcodebook;
        int cb2 = (1 << (mtab.lsp_bit1 & 0xff)) * (mtab.n_lsp & 0xff); // cb
        int cb3 = cb2 + (1 << (mtab.lsp_bit2 & 0xff)) * (mtab.n_lsp & 0xff); // cb

        byte[] funny_rounding = new byte[] {
                (byte) -2,
                (byte) (mtab.lsp_split == 4 ? -2 : 1),
                (byte) (mtab.lsp_split == 4 ? -2 : 1),
                (byte) 0
        };

        int j = 0;
        for (int i = 0; i < mtab.lsp_split; i++) {
            int chunk_end = ((i + 1) * (mtab.n_lsp & 0xff) + funny_rounding[i]) / (mtab.lsp_split & 0xff);
            for (; j < chunk_end; j++)
                lsp[j] = cb[lpc_idx1 * (mtab.n_lsp & 0xff) + j] +
                        cb[cb2 + lpc_idx2[i] * (mtab.n_lsp & 0xff) + j];
        }

        rearrange_lsp(mtab.n_lsp, lsp, 0.0001f);

        for (int i = 0; i < mtab.n_lsp; i++) {
            float tmp1 = 1.0f - cb[cb3 + lpc_hist_idx * (mtab.n_lsp & 0xff) + i];
            float tmp2 = hist[i] * cb[cb3 + lpc_hist_idx * (mtab.n_lsp & 0xff) + i];
            hist[i] = lsp[i];
            lsp[i] = lsp[i] * tmp1 + tmp2;
        }

        rearrange_lsp(mtab.n_lsp & 0xff, lsp, 0.0001f);
        rearrange_lsp(mtab.n_lsp & 0xff, lsp, 0.000095f);
        ff_sort_nearly_sorted_floats(lsp, mtab.n_lsp & 0xff);
    }

    static void ff_sort_nearly_sorted_floats(float[] vals, int len) {
        for (int i = 0; i < len - 1; i++)
            for (int j = i; j >= 0 && vals[j] > vals[j + 1]; j--) {
                float tmp = vals[j];
                vals[j] = vals[j + 1];
                vals[j + 1] = tmp;
            }
    }

    /** */
    static void dec_lpc_spectrum_inv(TwinVQContext tctx, float[] lsp, TwinVQFrameType ftype, float[] lpc) {
        int size = tctx.mtab.size / tctx.mtab.fmode[ftype.ordinal()].sub;

        for (int i = 0; i < tctx.mtab.n_lsp; i++)
            lsp[i] = (float) (2 * Math.cos(lsp[i]));

        switch (ftype) {
            case TWINVQ_FT_LONG:
                eval_lpcenv_2parts(tctx, ftype, lsp, lpc, size, 8);
                break;
            case TWINVQ_FT_MEDIUM:
                eval_lpcenv_2parts(tctx, ftype, lsp, lpc, size, 2);
                break;
            case TWINVQ_FT_SHORT:
                eval_lpcenv(tctx, lsp, lpc);
                break;
        }
    }

    static final byte[] wtype_to_wsize = new byte[] {0, 0, 2, 2, 2, 1, 0, 1, 1};

    /** */
    static void imdct_and_window(TwinVQContext tctx, TwinVQFrameType ftype,
                                 int wtype, float[] in, int inP, float[] prev, int prev_bufP, int ch) {
        AVTXContext tx = tctx.tx[ftype.ordinal()];
        AVTXContext.TXFunction tx_fn = tctx.tx_fn[ftype.ordinal()];
logger.log(Level.DEBUG, "ftype: " + ftype + "(" + ftype.ordinal() + ")");
        TwinVQModeTab mtab = tctx.mtab;
        int bsize = mtab.size / mtab.fmode[ftype.ordinal()].sub;
        int size = mtab.size;
        float[] buf1 = tctx.tmp_buf;
        int j, first_wsize, wsize; // Window size
        int out = 2 * ch * mtab.size; // tctx.curr_frame
        int out2 = out;
        float[] prev_buf;
        int[] types_sizes = {
                mtab.size / mtab.fmode[TWINVQ_FT_LONG.ordinal()].sub,
                mtab.size / mtab.fmode[TWINVQ_FT_MEDIUM.ordinal()].sub,
                mtab.size / (mtab.fmode[TWINVQ_FT_SHORT.ordinal()].sub * 2),
        };

        wsize = types_sizes[wtype_to_wsize[wtype]];
        first_wsize = wsize;
        prev_buf = prev;
        prev_bufP += (size - bsize) / 2; // prev_buf

        for (j = 0; j < mtab.fmode[ftype.ordinal()].sub; j++) {
            int sub_wtype = ftype == TWINVQ_FT_MEDIUM ? 8 : wtype;

            if (j == 0 && wtype == 4)
                sub_wtype = 4;
            else if (j == mtab.fmode[ftype.ordinal()].sub - 1 && wtype == 7)
                sub_wtype = 7;

            wsize = types_sizes[wtype_to_wsize[sub_wtype]];

            tx_fn.accept(tx, buf1, bsize * j, in, bsize * j /*, Float.BYTES */); // TODO not implemented

System.err.printf("j: %d, win: %d, size: %d%n", j, (int) Math.log(wsize), wsize / 2);
            tctx.fdsp.vector_fmul_window(tctx.curr_frame, out2, prev_buf, (bsize - wsize) / 2,
                    buf1, bsize * j,
                    ff_sine_windows.get((int) Math.log(wsize)),
                    wsize / 2);
            out2 += wsize;

            System.arraycopy(buf1, bsize * j + wsize / 2, tctx.curr_frame, out2, (bsize - wsize / 2) * Float.BYTES);

            out2 += ftype == TWINVQ_FT_MEDIUM ? (bsize - wsize) / 2 : bsize - wsize;

            prev_buf = buf1;
            prev_bufP = bsize * j + bsize / 2;
        }

        tctx.last_block_pos[ch] = (size + first_wsize) / 2;
    }

    static void imdct_output(TwinVQContext tctx, TwinVQFrameType ftype, int wtype, float[][] out, int offset) {
        TwinVQModeTab mtab = tctx.mtab;
        int prev_buf = tctx.last_block_pos[0]; // tctx.prev_frame
        int channels = tctx.avctx.ch_layout.nb_channels;
        int size1, size2, i;
        int out1, out2;

        for (i = 0; i < channels; i++)
            imdct_and_window(tctx, ftype, wtype,
                    tctx.spectrum, i * mtab.size,
                    tctx.prev_frame, prev_buf + 2 * i * mtab.size,
                    i);

        if (out == null)
            return;

        size2 = tctx.last_block_pos[0];
        size1 = mtab.size - size2;

        out1 = offset; // out[0]
        System.arraycopy(prev_buf, 0, out1, 0, size1 * out1);
        System.arraycopy(tctx.curr_frame, 0, out1, size1, size2 * out1);

        if (channels == 2) {
            out2 = offset; // out[1]
            System.arraycopy(prev_buf, 2 * mtab.size, out2, 0, size1 * out2);
            System.arraycopy(tctx.curr_frame, 2 * mtab.size, out2, size1, size2 * out2);
            tctx.fdsp.butterflies_float(out[0], out1, out[1], out2, mtab.size);
        }
    }

    static void read_and_decode_spectrum(TwinVQContext tctx, float[] out, TwinVQFrameType ftype) {
        TwinVQModeTab mtab = tctx.mtab;
        TwinVQFrameData bits = tctx.bits[tctx.cur_frame];
        int channels = tctx.avctx.ch_layout.nb_channels;
        int sub = mtab.fmode[ftype.ordinal()].sub;
        int block_size = mtab.size / sub;
        float[] gain = new float[TWINVQ_CHANNELS_MAX * TWINVQ_SUBBLOCKS_MAX];
        float[] ppc_shape = new float[TWINVQ_PPC_SHAPE_LEN_MAX * TWINVQ_CHANNELS_MAX * 4];

        dequant(tctx, bits.main_coeffs, out, ftype,
                mtab.fmode[ftype.ordinal()].cb0, mtab.fmode[ftype.ordinal()].cb1, 0,
                mtab.fmode[ftype.ordinal()].cb_len_read & 0xff);

        dec_gain(tctx, ftype, gain);

        if (ftype == TWINVQ_FT_LONG) {
            int cb_len_p = (tctx.n_div[3] + (mtab.ppc_shape_len & 0xff) * channels - 1) / tctx.n_div[3];
            dequant(tctx, bits.ppc_coeffs, ppc_shape,
                    TWINVQ_FT_PPC, mtab.ppc_shape_cb,
                    mtab.ppc_shape_cb,cb_len_p * TWINVQ_PPC_SHAPE_CB_SIZE,
                    cb_len_p);
        }

        for (int i = 0; i < channels; i++) {
            int chunk = mtab.size * i; // out
            float[] lsp = new float[TWINVQ_LSP_COEFS_MAX];

            for (int j = 0; j < sub; j++) {
                tctx.dec_bark_env.accept(tctx, bits.bark1[i][j],
                        bits.bark_use_hist[i][j] & 0xff, i,
                        tctx.tmp_buf, gain[sub * i + j], ftype);

                tctx.fdsp.vector_fmul(out, chunk + block_size * j,
                        out, chunk + block_size * j,
                        tctx.tmp_buf, 0, block_size);
            }

            if (ftype == TWINVQ_FT_LONG)
                tctx.decode_ppc.accept(tctx, bits.p_coef[i], bits.g_coef[i],
                        ppc_shape, + i * mtab.ppc_shape_len, out, chunk);

            decode_lsp(tctx, bits.lpc_idx1[i], bits.lpc_idx2[i],
                    bits.lpc_hist_idx[i], lsp, tctx.lsp_hist[i]);

            dec_lpc_spectrum_inv(tctx, lsp, ftype, tctx.tmp_buf);

            for (int j = 0; j < mtab.fmode[ftype.ordinal()].sub; j++) {
                tctx.fdsp.vector_fmul(out, chunk, out, chunk, tctx.tmp_buf, 0, block_size);
                chunk += block_size;
            }
        }
    }

    static final TwinVQFrameType[] ff_twinvq_wtype_to_ftype_table = {
            TWINVQ_FT_LONG, TWINVQ_FT_LONG, TWINVQ_FT_SHORT, TWINVQ_FT_LONG,
            TWINVQ_FT_MEDIUM, TWINVQ_FT_LONG, TWINVQ_FT_LONG, TWINVQ_FT_MEDIUM,
            TWINVQ_FT_MEDIUM
    };

    /**
     * @override decode
     * @return block align
     */
    static int ff_twinvq_decode_frame(AVCodecContext avctx, AVFrame frame, int[] got_frame_ptr, AVPacket avpkt) {
        byte[] buf = avpkt.data;
        int buf_size = avpkt.size;
        TwinVQContext tctx = avctx.priv_data;
        TwinVQModeTab mtab = tctx.mtab;
        float[][] out = null;
        int ret;

        // get output buffer
        if (tctx.discarded_packets >= 2) {
            frame.nb_samples = mtab.size * tctx.frames_per_packet;
            if ((ret = ff_get_buffer(avctx, frame, 0)) < 0)
                return ret;
            out = (float[][]) frame.extended_data;
        }

        if (buf_size < avctx.block_align) {
            logger.log(Level.ERROR, "Frame too small (%d bytes). Truncated file?".formatted(buf_size));
            return -1;
        }

        if ((ret = tctx.read_bitstream.apply(avctx, tctx, buf, buf_size)) < 0)
            return ret;

        for (tctx.cur_frame = 0; tctx.cur_frame < tctx.frames_per_packet; tctx.cur_frame++) {
            read_and_decode_spectrum(tctx, tctx.spectrum, tctx.bits[tctx.cur_frame].ftype);

            imdct_output(tctx, tctx.bits[tctx.cur_frame].ftype,
                    tctx.bits[tctx.cur_frame].window_type, out,
                    tctx.cur_frame * mtab.size);

            float[] tmp = tctx.curr_frame;
            tctx.curr_frame = tctx.prev_frame;
            tctx.prev_frame = tmp;
        }

        if (tctx.discarded_packets < 2) {
            tctx.discarded_packets++;
            got_frame_ptr[0] = 0;
            return buf_size;
        }

        got_frame_ptr[0] = 1;

        // VQF can deliver packets 1 byte greater than block align
        if (buf_size == avctx.block_align + 1)
            return buf_size;
        return avctx.block_align;
    }

    /**
     * Init IMDCT and windowing tables
     */
    static int init_mdct_win(TwinVQContext tctx) {
        int ret;
        TwinVQModeTab mtab = tctx.mtab;
        int size_s = mtab.size / mtab.fmode[TWINVQ_FT_SHORT.ordinal()].sub;
        int size_m = mtab.size / mtab.fmode[TWINVQ_FT_MEDIUM.ordinal()].sub;
        int channels = tctx.avctx.ch_layout.nb_channels;
        float norm = channels == 1 ? 2.0f : 1.0f;
        int table_size = 2 * mtab.size * channels;

        for (int i = 0; i < 3; i++) {
            int bsize = tctx.mtab.size / tctx.mtab.fmode[i].sub;
            float[] scale = new float[] { (float) (-Math.sqrt(norm / bsize) / (1 << 15)) };
            if ((ret = av_tx_init(tctx.tx, tctx.tx_fn, i, AV_TX_FLOAT_MDCT, 1, bsize, scale, 0)) != 0)
                return ret;
        }

        tctx.tmp_buf = new float[mtab.size];
        tctx.spectrum = new float[table_size];
        tctx.curr_frame = new float[table_size];
        tctx.prev_frame = new float[table_size];

        for (int i = 0; i < 3; i++) {
            int m = 4 * mtab.size / mtab.fmode[i].sub;
            double freq = 2 * Math.PI / m;
            tctx.cos_tabs[i] = new float[m / 4];
            for (int j = 0; j <= m / 8; j++)
                tctx.cos_tabs[i][j] = (float) Math.cos((2 * j + 1) * freq);
            for (int j = 1; j < m / 8; j++)
                tctx.cos_tabs[i][m / 4 - j] = tctx.cos_tabs[i][j];
        }

        ff_init_ff_sine_windows((int) Math.log(size_m));
        ff_init_ff_sine_windows((int) Math.log(size_s / 2d));
        ff_init_ff_sine_windows((int) Math.log(mtab.size));

        return 0;
    }

    /**
     * Interpret the data as if it were a {@code num_blocks x line_len[0]} matrix and for
     * each line do a cyclic permutation, i.e.
     * <pre>
     * abcdefghijklm -> defghijklmabc
     * </pre>
     * where the amount to be shifted is evaluated depending on the column.
     *
     * @param tab output
     */
    static void permutate_in_line(short[] tab, int num_vect, int num_blocks,
                                  int block_size,
                                  byte[] line_len,
                                  int length_div,
                                  TwinVQFrameType ftype) {
        for (int i = 0; i < line_len[0]; i++) {
            int shift;

            if (num_blocks == 1 ||
                    (ftype == TWINVQ_FT_LONG && (num_vect % num_blocks) != 0) ||
                    (ftype != TWINVQ_FT_LONG && (num_vect & 1) != 0) ||
                    i == line_len[1]) {
                shift = 0;
            } else if (ftype == TWINVQ_FT_LONG) {
                shift = i;
            } else
                shift = i * i;

            for (int j = 0; j < num_vect && (j + num_vect * i < block_size * num_blocks); j++)
                tab[i * num_vect + j] = (short) (i * num_vect + (j + shift) % num_vect);
        }
    }

    /**
     * Interpret the input data as in the following table:
     *
     * <pre>
     * abcdefgh
     * ijklmnop
     * qrstuvw
     * x123456
     * </pre>
     *
     * and transpose it, giving the output
     * <pre>
     * aiqxbjr1cks2dlt3emu4fvn5gow6hp
     * </pre>
     */
    static void transpose_perm(short[] out, short[] in, int num_vect, byte[] line_len, int length_div) {
        int cont = 0;

        for (int i = 0; i < num_vect; i++)
            for (int j = 0; j < line_len[i >= length_div ? 1 : 0]; j++)
                out[cont++] = in[j * num_vect + i];
    }

    /** */
    static void linear_perm(short[] out, short[] in, int n_blocks, int size) {
        int block_size = size / n_blocks;

        for (int i = 0; i < size; i++)
            out[i] = (short) (block_size * (in[i] % n_blocks) + in[i] / n_blocks);
    }

    /** */
    static void construct_perm_table(TwinVQContext tctx, TwinVQFrameType ftype) {
        int block_size, size;
        TwinVQModeTab mtab = tctx.mtab;
        ByteBuffer bbf = ByteBuffer.allocate(tctx.tmp_buf.length * Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        FloatBuffer tmp_perm = bbf.asFloatBuffer();
        tmp_perm.put(tctx.tmp_buf);

        if (ftype == TWINVQ_FT_PPC) {
            size = tctx.avctx.ch_layout.nb_channels;
            block_size = mtab.ppc_shape_len;
        } else {
            size = tctx.avctx.ch_layout.nb_channels * mtab.fmode[ftype.ordinal()].sub;
            block_size = mtab.size / mtab.fmode[ftype.ordinal()].sub;
        }
logger.log(Level.DEBUG, "size: " + size + ", block_size: " + block_size);

        short[] bbfs = new short[bbf.capacity() / Short.BYTES];
        permutate_in_line(bbfs, tctx.n_div[ftype.ordinal()], size,
                block_size, tctx.length[ftype.ordinal()],
                tctx.length_change[ftype.ordinal()] & 0xff, ftype);

        transpose_perm(tctx.permut[ftype.ordinal()], bbfs, tctx.n_div[ftype.ordinal()],
                tctx.length[ftype.ordinal()], tctx.length_change[ftype.ordinal()] & 0xff);
        bbf.asShortBuffer().get(bbfs);

        linear_perm(tctx.permut[ftype.ordinal()], tctx.permut[ftype.ordinal()], size, size * block_size);
    }

    /** */
    static void init_bitstream_params(TwinVQContext tctx) {
        TwinVQModeTab mtab = tctx.mtab;
        int n_ch = tctx.avctx.ch_layout.nb_channels;
        int total_fr_bits = tctx.avctx.bit_rate * mtab.size / tctx.avctx.sample_rate;

        int lsp_bits_per_block = n_ch * (mtab.lsp_bit0 + mtab.lsp_bit1 + mtab.lsp_split * mtab.lsp_bit2);

        int ppc_bits = n_ch * (mtab.pgain_bit + mtab.ppc_shape_bit + mtab.ppc_period_bit);

        int[] bsize_no_main_cb = new int[3], bse_bits = new int[3];

        for (int i = 0; i < 3; i++)
            // +1 for history usage switch
            bse_bits[i] = n_ch * (mtab.fmode[i].bark_n_coef * mtab.fmode[i].bark_n_bit + 1);

        bsize_no_main_cb[2] = bse_bits[2] + lsp_bits_per_block + ppc_bits +
                TWINVQ_WINDOW_TYPE_BITS + n_ch * TWINVQ_GAIN_BITS;

        for (int i = 0; i < 2; i++)
            bsize_no_main_cb[i] = lsp_bits_per_block + n_ch * TWINVQ_GAIN_BITS + TWINVQ_WINDOW_TYPE_BITS +
                            mtab.fmode[i].sub * (bse_bits[i] + n_ch * TWINVQ_SUB_GAIN_BITS);

        if (tctx.codec == TWINVQ_CODEC_METASOUND && tctx.is_6kbps == 0) {
            bsize_no_main_cb[1] += 2;
            bsize_no_main_cb[2] += 2;
        }

        // The remaining bits are all used for the main spectrum coefficients
        for (int i = 0; i < 4; i++) {
            int bit_size, vect_size;
            int rounded_up, rounded_down, num_rounded_down, num_rounded_up;
            if (i == 3) {
                bit_size = n_ch * mtab.ppc_shape_bit;
                vect_size = n_ch * mtab.ppc_shape_len;
            } else {
                bit_size = total_fr_bits - bsize_no_main_cb[i];
                vect_size = n_ch * mtab.size;
            }

            tctx.n_div[i] = (bit_size + 13) / 14;

            rounded_up = (bit_size + tctx.n_div[i] - 1) / tctx.n_div[i];
            rounded_down = (bit_size) / tctx.n_div[i];
            num_rounded_down = rounded_up * tctx.n_div[i] - bit_size;
            num_rounded_up = tctx.n_div[i] - num_rounded_down;
            tctx.bits_main_spec[0][i][0] = (byte) ((rounded_up + 1) / 2);
            tctx.bits_main_spec[1][i][0] = (byte) (rounded_up / 2);
            tctx.bits_main_spec[0][i][1] = (byte) ((rounded_down + 1) / 2);
            tctx.bits_main_spec[1][i][1] = (byte) (rounded_down / 2);
            tctx.bits_main_spec_change[i] = num_rounded_up;

            rounded_up = (vect_size + tctx.n_div[i] - 1) / tctx.n_div[i];
            rounded_down = (vect_size) / tctx.n_div[i];
            num_rounded_down = rounded_up * tctx.n_div[i] - vect_size;
            num_rounded_up = tctx.n_div[i] - num_rounded_down;
            tctx.length[i][0] = (byte) rounded_up;
            tctx.length[i][1] = (byte) rounded_down;
            tctx.length_change[i] = (byte) num_rounded_up;
logger.log(Level.DEBUG, "rounded_up: " + rounded_up + ", rounded_down: " + rounded_down + ", num_rounded_up: " + num_rounded_up);
        }

        for (int frametype = TWINVQ_FT_SHORT.ordinal(); frametype <= TWINVQ_FT_PPC.ordinal(); frametype++)
            construct_perm_table(tctx, TwinVQFrameType.values()[frametype]);
    }

    /** @override close */
    static int ff_twinvq_decode_close(AVCodecContext avctx) {
        TwinVQContext tctx = avctx.priv_data;

        return 0;
    }

    /** */
    static int ff_twinvq_decode_init(AVCodecContext avctx) {
        int ret;
        TwinVQContext tctx = avctx.priv_data;
        long frames_per_packet;

        tctx.avctx = avctx;
        avctx.sample_fmt = AV_SAMPLE_FMT_FLTP;

        if (avctx.block_align == 0) {
            avctx.block_align = tctx.frame_size + 7 >> 3;
        }
        frames_per_packet = avctx.block_align * 8L / tctx.frame_size;
        if (frames_per_packet <= 0) {
            logger.log(Level.ERROR, "Block align is %d bits, expected %d".formatted(avctx.block_align * 8L, tctx.frame_size));
            return AVERROR_INVALIDDATA;
        }
        if (frames_per_packet > TWINVQ_MAX_FRAMES_PER_PACKET) {
            logger.log(Level.ERROR, "Too many frames per packet (%d)".formatted(frames_per_packet));
            return AVERROR_INVALIDDATA;
        }
        tctx.frames_per_packet = (int) frames_per_packet;

        tctx.fdsp = new AVFloatDSPContext(avctx.flags & AV_CODEC_FLAG_BITEXACT);
        if ((ret = init_mdct_win(tctx)) != 0) {
            logger.log(Level.ERROR, "Error initializing MDCT");
            return ret;
        }
        init_bitstream_params(tctx);

        twinvq_memset_float(tctx.bark_hist[0][0], 0, 0.1f, FF_ARRAY_ELEMS(tctx.bark_hist));

        return 0;
    }
}
