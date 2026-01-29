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
                int outIdx = tctx.permut[ftype.ordinal()][pos + j] & 0xffff;
                out[outIdx] = sign0 * cb0[tab0 + j] + sign1 * cb1[cb1P + tab1 + j];
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
                        twinvq_mulawinv(step * 0.5f + step * (bits.gain_bits[i] & 0xff), TWINVQ_AMP_MAX, TWINVQ_MULAW_MU);
        } else {
            for (int i = 0; i < channels; i++) {
                float val = (1.0f / (1 << 23)) *
                        twinvq_mulawinv(step * 0.5f + step * (bits.gain_bits[i] & 0xff), TWINVQ_AMP_MAX, TWINVQ_MULAW_MU);

                for (int j = 0; j < sub; j++)
                    out[i * sub + j] =
                            val * twinvq_mulawinv(sub_step * 0.5f + sub_step * (bits.sub_gain_bits[i * sub + j] & 0xff),
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
                        cb[cb2 + (lpc_idx2[i] & 0xff) * (mtab.n_lsp & 0xff) + j];
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

    /** Integer log base 2 (floor). */
    static int ff_log2(int v) {
        int n = 0;
        if ((v & 0xffff_0000) != 0) {
            v >>>= 16;
            n += 16;
        }
        if ((v & 0xff00) != 0) {
            v >>>= 8;
            n += 8;
        }
        if ((v & 0xf0) != 0) {
            v >>>= 4;
            n += 4;
        }
        if ((v & 0xc) != 0) {
            v >>>= 2;
            n += 2;
        }
        if ((v & 0x2) != 0) {
            n += 1;
        }
        return n;
    }

    static final byte[] wtype_to_wsize = new byte[] {0, 0, 2, 2, 2, 1, 0, 1, 1};

    /** */
    static void imdct_and_window(TwinVQContext tctx, TwinVQFrameType ftype,
                                 int wtype, float[] in, int inP, float[] prev, int prev_bufP, int ch) {
        AVTXContext tx = tctx.tx[ftype.ordinal()];
        AVTXContext.TXFunction tx_fn = tctx.tx_fn[ftype.ordinal()];
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

            // Debug: check spectrum input for this channel/subframe
            float specMax = 0;
            for (int k = 0; k < bsize; k++) {
                if (Math.abs(in[inP + bsize * j + k]) > Math.abs(specMax)) specMax = in[inP + bsize * j + k];
            }
            logger.log(Level.TRACE, "IMDCT input ch=" + ch + ", sub=" + j + ", inP=" + inP + ", spec max=" + specMax);

            tx_fn.accept(tx, buf1, bsize * j, in, inP + bsize * j);

            // Debug: check IMDCT output for this channel
            // IMDCT writes to output[0..n2-1] where n2=bsize/2 for imdctHalf
            int n2 = bsize / 2;
            float imdctMaxFirstHalf = 0;
            float imdctMaxSecondHalf = 0;
            for (int k = 0; k < n2; k++) {
                if (Math.abs(buf1[bsize * j + k]) > Math.abs(imdctMaxFirstHalf)) imdctMaxFirstHalf = buf1[bsize * j + k];
            }
            for (int k = n2; k < bsize; k++) {
                if (Math.abs(buf1[bsize * j + k]) > Math.abs(imdctMaxSecondHalf)) imdctMaxSecondHalf = buf1[bsize * j + k];
            }
            logger.log(Level.TRACE, "IMDCT output ch=" + ch + ", sub=" + j + ", buf1[0..n2-1] max=" + imdctMaxFirstHalf + ", buf1[n2..bsize-1] max=" + imdctMaxSecondHalf);

            int wsize_log2 = ff_log2(wsize);
            tctx.fdsp.vector_fmul_window(tctx.curr_frame, out2, prev_buf, prev_bufP + (bsize - wsize) / 2,
                    buf1, bsize * j,
                    ff_sine_windows.get(wsize_log2),
                    wsize / 2);
            out2 += wsize;

            System.arraycopy(buf1, bsize * j + wsize / 2, tctx.curr_frame, out2, bsize - wsize / 2);

            out2 += ftype == TWINVQ_FT_MEDIUM ? (bsize - wsize) / 2 : bsize - wsize;

            prev_buf = buf1;
            prev_bufP = bsize * j + bsize / 2;
        }

        tctx.last_block_pos[ch] = (size + first_wsize) / 2;
    }

    static void imdct_output(TwinVQContext tctx, TwinVQFrameType ftype, int wtype, float[][] out, int offset) {
        TwinVQModeTab mtab = tctx.mtab;
        int prev_bufP = tctx.last_block_pos[0]; // offset into tctx.prev_frame
        int channels = tctx.avctx.ch_layout.nb_channels;
        int size1, size2;

        for (int i = 0; i < channels; i++) {
            imdct_and_window(tctx, ftype, wtype,
                    tctx.spectrum, i * mtab.size,
                    tctx.prev_frame, prev_bufP + 2 * i * mtab.size,
                    i);
            // Debug: check curr_frame values for this channel
            float currMax = 0;
            int currStart = 2 * i * mtab.size;
            for (int k = 0; k < mtab.size; k++) {
                if (Math.abs(tctx.curr_frame[currStart + k]) > Math.abs(currMax)) currMax = tctx.curr_frame[currStart + k];
            }
            logger.log(Level.TRACE, "After imdct_and_window ch=" + i + ", curr_frame[" + currStart + "..] max=" + currMax);
        }

        if (out == null)
            return;

        size2 = tctx.last_block_pos[0];
        size1 = mtab.size - size2;

        // Copy from prev_frame and curr_frame to output buffer for channel 0
        System.arraycopy(tctx.prev_frame, prev_bufP, out[0], offset, size1);
        System.arraycopy(tctx.curr_frame, 0, out[0], offset + size1, size2);

        if (channels == 2) {
            // Debug: check curr_frame ch1 values before copy
            float maxCurrCh0 = 0, maxCurrCh1 = 0;
            for (int i = 0; i < size2; i++) {
                if (Math.abs(tctx.curr_frame[i]) > Math.abs(maxCurrCh0))
                    maxCurrCh0 = tctx.curr_frame[i];
                if (Math.abs(tctx.curr_frame[2 * mtab.size + i]) > Math.abs(maxCurrCh1))
                    maxCurrCh1 = tctx.curr_frame[2 * mtab.size + i];
            }
            float maxPrevCh0 = 0, maxPrevCh1 = 0;
            for (int i = 0; i < size1; i++) {
                if (Math.abs(tctx.prev_frame[prev_bufP + i]) > Math.abs(maxPrevCh0))
                    maxPrevCh0 = tctx.prev_frame[prev_bufP + i];
                if (Math.abs(tctx.prev_frame[prev_bufP + 2 * mtab.size + i]) > Math.abs(maxPrevCh1))
                    maxPrevCh1 = tctx.prev_frame[prev_bufP + 2 * mtab.size + i];
            }
            logger.log(Level.TRACE, "Source frames: prev_bufP=" + prev_bufP + ", size1=" + size1 + ", size2=" + size2 +
                ", prev_ch0=" + maxPrevCh0 + ", prev_ch1=" + maxPrevCh1 +
                ", curr_ch0=" + maxCurrCh0 + ", curr_ch1=" + maxCurrCh1);

            // Copy from prev_frame and curr_frame to output buffer for channel 1
            System.arraycopy(tctx.prev_frame, prev_bufP + 2 * mtab.size, out[1], offset, size1);
            System.arraycopy(tctx.curr_frame, 2 * mtab.size, out[1], offset + size1, size2);

            // Debug: check if Mid and Side have identical values (which would cause R to be zero)
            float maxMid = 0, maxSide = 0;
            int identicalCount = 0;
            float maxDiff = 0;
            int firstIdenticalIdx = -1;
            for (int i = 0; i < mtab.size; i++) {
                if (Math.abs(out[0][offset + i]) > Math.abs(maxMid)) maxMid = out[0][offset + i];
                if (Math.abs(out[1][offset + i]) > Math.abs(maxSide)) maxSide = out[1][offset + i];
                if (out[0][offset + i] == out[1][offset + i]) {
                    identicalCount++;
                    if (firstIdenticalIdx < 0 && out[0][offset + i] != 0) firstIdenticalIdx = i;
                }
                float diff = Math.abs(out[0][offset + i] - out[1][offset + i]);
                if (diff > maxDiff) maxDiff = diff;
            }
            logger.log(Level.TRACE, "Before butterflies: Mid max=" + maxMid + ", Side max=" + maxSide +
                ", identical=" + identicalCount + "/" + mtab.size + ", maxDiff=" + maxDiff +
                ", firstNonZeroIdentical=" + firstIdenticalIdx);

            tctx.fdsp.butterflies_float(out[0], offset, out[1], offset, (short) mtab.size);

            // Debug: check values after butterflies
            float maxLeft = 0, maxRight = 0;
            for (int i = 0; i < mtab.size; i++) {
                if (Math.abs(out[0][offset + i]) > Math.abs(maxLeft)) maxLeft = out[0][offset + i];
                if (Math.abs(out[1][offset + i]) > Math.abs(maxRight)) maxRight = out[1][offset + i];
            }
            logger.log(Level.TRACE, "After butterflies: Left max=" + maxLeft + ", Right max=" + maxRight);
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

        // Debug: spectrum per-channel analysis after dequant
        float dequantMax0 = 0, dequantMax1 = 0;
        int identicalAfterDequant = 0;
        for (int ii = 0; ii < mtab.size; ii++) {
            if (Math.abs(out[ii]) > Math.abs(dequantMax0)) dequantMax0 = out[ii];
            if (channels > 1) {
                if (Math.abs(out[mtab.size + ii]) > Math.abs(dequantMax1)) dequantMax1 = out[mtab.size + ii];
                if (out[ii] == out[mtab.size + ii]) identicalAfterDequant++;
            }
        }
        logger.log(Level.TRACE, "ftype=" + ftype + ", After dequant: ch0 max=" + dequantMax0 +
            ", ch1 max=" + dequantMax1 + ", identical=" + identicalAfterDequant + "/" + mtab.size);

        // Check how many values are non-zero in each channel's region
        int nonZeroCh0 = 0, nonZeroCh1 = 0;
        for (int ii = 0; ii < mtab.size; ii++) {
            if (out[ii] != 0) nonZeroCh0++;
            if (channels > 1 && out[mtab.size + ii] != 0) nonZeroCh1++;
        }
        logger.log(Level.TRACE, "DEQUANT: ftype=" + ftype + ", ch0 nonzero=" + nonZeroCh0 + "/" + mtab.size +
            ", ch1 nonzero=" + nonZeroCh1 + "/" + mtab.size + ", ch0max=" + dequantMax0 + ", ch1max=" + dequantMax1);

        dec_gain(tctx, ftype, gain);

        // Debug: gain values
        logger.log(Level.TRACE, "Gain[0]=" + gain[0] + ", Gain[1]=" + (sub > 1 ? gain[1] : "N/A"));

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

            decode_lsp(tctx, bits.lpc_idx1[i] & 0xff, bits.lpc_idx2[i],
                    bits.lpc_hist_idx[i] & 0xff, lsp, tctx.lsp_hist[i]);

            dec_lpc_spectrum_inv(tctx, lsp, ftype, tctx.tmp_buf);

            for (int j = 0; j < mtab.fmode[ftype.ordinal()].sub; j++) {
                tctx.fdsp.vector_fmul(out, chunk, out, chunk, tctx.tmp_buf, 0, block_size);
                chunk += block_size;
            }
        }

        // Debug: final spectrum before IMDCT
        float finalSpecMax0 = 0, finalSpecMax1 = 0;
        for (int ii = 0; ii < mtab.size; ii++) {
            if (Math.abs(out[ii]) > Math.abs(finalSpecMax0)) finalSpecMax0 = out[ii];
            if (channels > 1 && Math.abs(out[mtab.size + ii]) > Math.abs(finalSpecMax1)) finalSpecMax1 = out[mtab.size + ii];
        }
        logger.log(Level.TRACE, "Final spectrum before IMDCT: ch0 max=" + finalSpecMax0 + ", ch1 max=" + finalSpecMax1);
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
    public static int ff_twinvq_decode_frame(AVCodecContext avctx, AVFrame frame, int[] got_frame_ptr, AVPacket avpkt) {
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

        // Debug: check final output values
        if (out != null) {
            float maxCh0 = 0, maxCh1 = 0;
            for (int i = 0; i < frame.nb_samples; i++) {
                if (Math.abs(out[0][i]) > Math.abs(maxCh0)) maxCh0 = out[0][i];
                if (out.length > 1 && Math.abs(out[1][i]) > Math.abs(maxCh1)) maxCh1 = out[1][i];
            }
            logger.log(Level.TRACE, "Final output: ch0 max=" + maxCh0 + ", ch1 max=" + maxCh1);
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
            logger.log(Level.TRACE, "MDCT init i=" + i + ", bsize=" + bsize + ", scale=" + scale[0]);
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

        ff_init_ff_sine_windows(ff_log2(size_m));
        ff_init_ff_sine_windows(ff_log2(size_s / 2));
        ff_init_ff_sine_windows(ff_log2(mtab.size));

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

        if (ftype == TWINVQ_FT_PPC) {
            size = tctx.avctx.ch_layout.nb_channels;
            block_size = mtab.ppc_shape_len;
        } else {
            size = tctx.avctx.ch_layout.nb_channels * mtab.fmode[ftype.ordinal()].sub;
            block_size = mtab.size / mtab.fmode[ftype.ordinal()].sub;
        }

        // Temporary buffer for permutation operations (reuse tmp_buf's storage as short[])
        short[] tmp_perm = new short[tctx.tmp_buf.length * 2];

        permutate_in_line(tmp_perm, tctx.n_div[ftype.ordinal()], size,
                block_size, tctx.length[ftype.ordinal()],
                tctx.length_change[ftype.ordinal()] & 0xff, ftype);

        transpose_perm(tctx.permut[ftype.ordinal()], tmp_perm, tctx.n_div[ftype.ordinal()],
                tctx.length[ftype.ordinal()], tctx.length_change[ftype.ordinal()] & 0xff);

        linear_perm(tctx.permut[ftype.ordinal()], tctx.permut[ftype.ordinal()], size, size * block_size);

        // Debug: check permutation table range and distribution
        int minPerm = Integer.MAX_VALUE, maxPerm = Integer.MIN_VALUE;
        int ch0Count = 0, ch1Count = 0;
        int threshold = mtab.size; // indices < mtab.size go to ch0, >= mtab.size go to ch1
        for (int i = 0; i < size * block_size; i++) {
            int val = tctx.permut[ftype.ordinal()][i] & 0xffff;
            if (val < minPerm) minPerm = val;
            if (val > maxPerm) maxPerm = val;
            if (val < threshold) ch0Count++;
            else ch1Count++;
        }
        logger.log(Level.TRACE, "Permut table ftype=" + ftype + ", size=" + size + ", block_size=" + block_size +
            ", total=" + (size * block_size) + ", range=[" + minPerm + "," + maxPerm + "]" +
            ", ch0 count=" + ch0Count + " (to ch0 region 0-" + (threshold-1) + ")" +
            ", ch1 count=" + ch1Count + " (to ch1 region " + threshold + "-" + (2*threshold-1) + ")");

        // Print first 20 permutation values to see the pattern
        if (ftype == TWINVQ_FT_LONG) {
            StringBuilder sb = new StringBuilder("Permut LONG first 20: ");
            for (int i = 0; i < 20 && i < size * block_size; i++) {
                sb.append(tctx.permut[ftype.ordinal()][i] & 0xffff).append(" ");
            }
            logger.log(Level.TRACE, sb.toString());

            // Check for duplicate permutation values
            java.util.Set<Integer> seen = new java.util.HashSet<>();
            int duplicates = 0;
            for (int i = 0; i < size * block_size; i++) {
                int val = tctx.permut[ftype.ordinal()][i] & 0xffff;
                if (!seen.add(val)) {
                    duplicates++;
                    if (duplicates <= 5) logger.log(Level.TRACE, "Duplicate permut value: " + val + " at index " + i);
                }
            }
            logger.log(Level.TRACE, "Permut LONG duplicates: " + duplicates);
        }
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
logger.log(Level.TRACE, "rounded_up: " + rounded_up + ", rounded_down: " + rounded_down + ", num_rounded_up: " + num_rounded_up);
        }

        for (int frametype = TWINVQ_FT_SHORT.ordinal(); frametype <= TWINVQ_FT_PPC.ordinal(); frametype++)
            construct_perm_table(tctx, TwinVQFrameType.values()[frametype]);
    }

    /** @override close */
    public static int ff_twinvq_decode_close(AVCodecContext avctx) {
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
