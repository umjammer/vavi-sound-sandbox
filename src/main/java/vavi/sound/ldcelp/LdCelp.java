/*
 */

package vavi.sound.ldcelp;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import vavi.util.Debug;


/**
 * LD-CELP  G.728
 * <p>
 * Low-Delay Code Excitation Linear Prediction speech compression.
 * </p><p>
 * Code edited by Michael Concannon.<br>
 * Based on code written by Alex Zatsman, Analog Devices 1993
 * </p><p>
 * Adapter for Perceptual Weighting Filter
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 040621 nsano initial version <br>
 */
public class LdCelp {

    /** Arrays for band widening: zeros and */
    private final float[] pwf_z_vec = new float[Constants.LPCW + 1];
    /** poles */
    private final float[] pwf_p_vec = new float[Constants.LPCW + 1];
    private final float[] pwf_old_input = new float[Constants.LPCW + Constants.NFRSZ + Constants.NONRW];
    /** Recursive Part */
    private final float[] pwf_rec = new float[Constants.LPCW + 1];

    /**
     * @param z_out zero coefficients
     * @param p_out pole coefficients
     */
    void pwf_adapter(float[] input, float[] z_out, float[] p_out) {
        // autocorrelation coefficients
        float[] acorr = new float[Constants.LPCW + 1];
        float[] lpcoeff = new float[Constants.LPCW + 1];
        float[] temp = new float[Constants.LPCW + 1];

        hybwin(Constants.LPCW,    // lpsize
               Constants.NFRSZ,    // framesize
               Constants.NONRW,    // nrsize -- nonrecursive size
               pwf_old_input,
               input,
               acorr,
               hw_percw,
               pwf_rec,
               0.5f);
        if (levdur(acorr, temp, Constants.LPCW) != 0) {
            RCOPY(temp, 0, lpcoeff, 0, Constants.LPCW + 1);
            bw_expand2(lpcoeff, z_out, p_out,
                       Constants.LPCW, pwf_z_vec, pwf_p_vec);
        }
    }

    /** */
    void init_pwf_adapter(float[] z_co, float[] p_co) {

        float zv = 1.0f;
        float pv = 1.0f;

        for (int i = 0; i <= Constants.LPCW; i++) {
            pwf_z_vec[i] = zv;
            pwf_p_vec[i] = pv;
            zv *= Constants.WZCF;
            pv *= Constants.WPCF;
            z_co[i] = 0.0f;
            p_co[i] = 0.0f;
        }

        p_co[0] = 1.0f;
        z_co[0] = 1.0f;
        ZARR(pwf_old_input);
        ZARR(pwf_rec);
    }

    // Backward Synthesis Filter Adapter --------------------------------------

    private final float[] facv = new float[Constants.LPC + 1];

    private final float[] bsf_old_input = new float[Constants.LPC + Constants.NFRSZ + Constants.NONR];
    private final float[] bsf_rec = new float[Constants.LPC + 1];

    /** */
    void bsf_adapter(float[] input, float[] p_out) {
        float[] old_input = new float[Constants.LPC + Constants.NFRSZ + Constants.NONR];
        // autocorrelation coefficients
        float[] acorr = new float[Constants.LPC + 1];
        float[] lpcoeff = new float[Constants.LPC + 1];
        float[] temp = new float[Constants.LPC + 1];

        hybwin(Constants.LPC,    // lpsize
               Constants.NFRSZ,    // framesize
               Constants.NONR,    // nrsize -- nonrecursive size
               old_input,
               input,
               acorr,
               hw_synth,
               bsf_rec,
               0.75f);

        if (sf_levdur(acorr, temp) != 0) {
            k10 = -acorr[1] / acorr[0];
            RCOPY(temp, 0, lpcoeff, 0, Constants.LPC + 1);
            bw_expand1(lpcoeff, p_out, Constants.LPC, facv);
        }
    }

    // Gain Adapter -----------------------------------------------------------

    /** Array for band widening */
    private static final float[] gain_p_vec = {
        1.0f,
        0.90625f,
        0.8212890625f,
        0.74432373046875f,
        0.67449951171875f,
        0.61126708984375f,
        0.553955078125f,
        0.50201416015625f,
        0.4549560546875f,
        0.41229248046875f,
        0.3736572265625f
    };

    /** Recursive part for Hybrid Window */
    private final float[] g_rec = new float[Constants.LPCLG + 1];
    private final float[] g_old_input = new float[Constants.LPCLG + Constants.NUPDATE + Constants.NONRLG];

    /** recompute lpc_coeff */
    void gain_adapter(float[] log_gain, float[] coeff) {
        // autocorrelation coefficients
        float[] acorr = new float[Constants.LPCLG + 1];
        float[] lpcoeff = new float[Constants.LPCLG + 1];

        float[] temp = new float[Constants.LPCLG + 1];

        hybwin(Constants.LPCLG,     // lpsize
               Constants.NUPDATE,   // framesize
               Constants.NONRLG,    // nrsize -- nonrecursive size
               g_old_input,
               log_gain,
               acorr,
               hw_gain,
               g_rec,
               0.75f);

        if (levdur(acorr, temp, Constants.LPCLG) != 0) {
            System.arraycopy(temp, 1, lpcoeff, 1, Constants.LPCLG);
            bw_expand1(lpcoeff, coeff, Constants.LPCLG, gain_p_vec);
        }
    }

    // Initializations --------------------------------------------------------

    /** */
    void init_bsf_adapter(float[] co) {
        float v = 1.0f;

        for (int i = 0; i <= Constants.LPC; i++) {
            facv[i] = v;
            v *= Constants.FAC;
            co[i] = 0;
        }
        co[0] = 1.0f;
        ZARR(bsf_old_input);
        ZARR(bsf_rec);
    }

    /** */
    void init_gain_adapter (float[] coeff) {
        gain_p_vec[0] = 1.0f;
        coeff[0] = 1.0f;
        coeff[1] = -1.0f;
        for(int i = 0; i < Constants.LPCLG + Constants.NUPDATE + Constants.NONRLG; i++) {
            g_old_input[i] = -Constants.GOFF;
        }
        ZARR(g_rec);
        ZARR(g_old_input);
    }

    // Hybrid Window Module ---------------------------------------------------

    /**
     * Hybrid Window
     *
     * @param lpsize size of OUTPUT (autocorrelation vector)
     * @param framesize size of NEW_INPUT
     * @param nrsize size of non-recursive part.
     * @param old_input buffer for holding old input
     *                  (size LPSIZE + FRAMESIZE + NRSIZE)
     * @param new_input new input, or frame (size FRAMESIZE)
     * @param output autocorrelation vector (size LPSIZE)
     * @param window window coefficients (size LPSIZE+FRAMESIZE+NRSIZE)
     * @param rec recursive part (size LPSIZE)
     * @param decay scaling for the old recursive part.
     */
    private static void hybwin(int lpsize,
                               int framesize,
                               int nrsize,
                               float[] old_input,
                               float[] new_input,
                               float[] output,
                               float[] window,
                               float[] rec,
                               float decay) {

        // M + L
        int N1 = lpsize + framesize;
        // M + N
        int N2 = lpsize + nrsize;
        int N3 = lpsize + framesize + nrsize;
        int i;
        float[] ws = new float[N3];
        float[] tmp1 = new float[lpsize + 1];
        float[] tmp2 = new float[lpsize + 1];

        // shift in INPUT into OLD_INPUT and window it

        for (i = 0; i < N2; i++) {
            old_input[i] = old_input[i + framesize];
        }
        for (i = 0; i < framesize; i++) {
            old_input[N2 + i] = new_input[i];
        }

        VPROD(old_input, window, ws, N3);

        autocorr(ws, tmp1, lpsize, lpsize, N1);

        for (i = 0; i <= lpsize; i++) {
            rec[i] = decay * rec[i] + tmp1[i];
        }

        autocorr(ws, tmp2, lpsize,  N1, N3);

        for (i = 0; i <= lpsize; i++) {
            output[i] = rec[i] + tmp2[i];
        }

        output[0] *= Constants.WNCF;
    }

    // Levinson-Durbin Routines -----------------------------------------------

    /**
     * Levinson-Durbin algorithm
     * return 1 if ok, otherwise 0
     */
    private static int levdur(float[] acorr, float[] coeff, int order) {
        // Local variables
        int minc2;
        float s;
        int ib, mh;
        float at;
        float[] rc = new float[20];
        float alpha;
        float tmp;

        // Parameter adjustments TODO -(1)
//        --acorr;
//        --coeff;

        // check for zero signal or illegal zero-lag autocorrelation
        if ((acorr[1-(1)] <= 0.0f) || (acorr[order + 1-(1)] == 0)) {
            return 0;
        }

        // start durbin's recursion
        rc[1] = -acorr[2-(1)] / acorr[1-(1)];
        coeff[1-(1)] = 1.0f;
        coeff[2-(1)] = rc[1];
        alpha = acorr[1-(1)] + acorr[2-(1)] * rc[1];
        if (alpha <= 0.0f) {
            return 0;
        }
        for (int minc = 2; minc <= order; ++minc) {
            minc2 = minc + 2;
            s = acorr[minc + 1-(1)];
            for (int ip = 2; ip <= minc; ++ip) {
                s += acorr[minc2 - ip-(1)] * coeff[ip-(1)];
            }
            rc[minc] = -s / alpha;
            mh = minc / 2 + 1;
            for (int ip = 2; ip <= mh; ip++) {
                ib = minc2 - ip;
                at = rc[minc] * coeff[ib-(1)];
                at += coeff[ip-(1)];
                tmp = rc[minc] * coeff[ip-(1)];
                coeff[ib-(1)] += tmp;
                coeff[ip-(1)] = at;
            }
            coeff[minc + 1-(1)] = rc[minc];
            alpha += rc[minc] * s;

            // if residual energy less than zero (due to ill-conditioning),
            // return without updating filter coefficients (use old ones).
            if (alpha <= 0.0f) {
                return 0;
            }
        }
        return 1;
    }

    /** */
    private final float[] a10 = new float[11];
    /** */
    private float k10;

    /**
     * Levinson-Durbin algorithm  for Synthesis Filter. Its main
     * difference from the above is the fact that it saves 10-th
     * order coefficients for Postfilter, plus some speedup since this
     * is one of the longest routines in the algorithm.
     */
    private int sf_levdur(float[] acorr, float[] coeff) {

        if (acorr[Constants.LPC] == 0) {
            return 0;
        }
        float E = acorr[0];
        if (E <= 0) {
            return 0;
        }
        coeff[0] = 1.0f;
        for (int m = 1; m <= Constants.LPC; m++) {
            float K = -acorr[m];
            if (m > 1) {
                float a1 = acorr[m - 1];
                float c1 = coeff[1];
                float tmp = c1 * a1;
                if (m > 2) {
                    c1 = coeff[2]; a1 = acorr[m-2];
                    for (int j = 3; j <= m - 1; j++) {
                        K -= tmp;
                        tmp = c1 * a1;
                        c1 = coeff[j];
                        a1 = acorr[m - j];
                    }
                    K -= tmp;
                    tmp = c1*a1;
                }
                K -= tmp;
            }
            K = K / E;
            coeff[m] = K;
            int halfm = m >> 1;

            // this is pipelened version of parallel assignment:
            //  coeff[j]   =     coeff[j] + K * coeff[m - j]
            //  coeff[m-j] = K * coeff[j] +     coeff[m - j]

            if (halfm >= 1) {
                float t3;
                float t4;
                int float_pointer_p = 1;
                int float_pointer_pp = float_pointer_p;
                int float_pointer_q = m - 1;
                int float_pointer_qq = float_pointer_q;

                float x = coeff[float_pointer_p++];
                float y = coeff[float_pointer_q--];
                float t1 = K * x;
                float t2 = K * y;
                for (int j = 2; j <= halfm; j++) {
                    t4 = t2 + x; x = coeff[float_pointer_p++];
                    t3 = t1 + y; y = coeff[float_pointer_q--];
                    t1 = K * x;    coeff[float_pointer_pp++] = t4;
                    t2 = K * y;    coeff[float_pointer_qq--] = t3;
                }
                t3 = t1 + y;
                t4 = t2 + x;
                coeff[float_pointer_pp] = t4;
                coeff[float_pointer_qq] = t3;
            }
            if (m == 10) {
                System.arraycopy(coeff, 0, a10, 0, 11);
            }
            E = (1 - K * K) * E;
            if (E < 0) {
                return 0;
            }
        }
        return 1;
    }

    // Band Width Expanders ---------------------------------------------------

    /**
     * Don't have to worry about i=0 -- z_vec[0] and p_vec[0] should stay 1.0.
     */
    private static void  bw_expand2(float[] input,
                                    float[] z_out,
                                    float[] p_out,
                                    int order,
                                    float[] z_vec,
                                    float[] p_vec) {

        for (int i = 1; i <= order; i++) {
            z_out[i] = z_vec[i] * input[i];
        }
        for (int i = 1; i <= order; i++) {
            p_out[i] = p_vec[i] * input[i];
        }
    }

    /** Poles only */
    private static void bw_expand1(float[] input,
                                   float[] p_out,
                                   int order,
                                   float[] p_vec) {

        for (int i = 1; i <= order; i++) {
            p_out[i] = p_vec[i] * input[i];
        }
    }

    /** */
    private static void autocorr(float[] x, float[] r, int k, int m, int n) {

        for (int ii = 0; ii <= k; ii++) {
            r[ii] = 0;
            for (int jj = m; jj < n; jj++) {
                float tmp = x[jj] * x[jj - ii];
                r[ii] += tmp;
            }
        }
    }

    // CMain ----

    /** Index of the end of the decoded speech */
    private int dec_end;
//    /** */
//    private int encoder_done = 0;

    /**
     * @param args -d[p]|-e infile outfile
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            usage();
        }

        LdCelp ldCelp = new LdCelp();
        if (args[0].equals("-e")) {
            ldCelp.ifile_name = args[1];
            ldCelp.xfile_name = args[2];
            ldCelp.ffase.set(1);
            ldCelp.encoder();
        } else if (args[0].startsWith("-d")) {
            if (args[0].length() > 2 && args[0].charAt(2) == 'p') {
                ldCelp.postfiltering_p = true;
            } else {
                ldCelp.postfiltering_p = false;
            }
            ldCelp.xfile_name = args[1];
            ldCelp.ofile_name = args[2];
            ldCelp.ffase.set(1);
            ldCelp.decoder();
        } else {
            usage();
        }
    }

    /** */
    private static void usage() {
        System.err.println("java LdCelp -d[p] <index-file> <audio-file>");
        System.err.println("java LdCelp -e <audio-file> <index-file>");
        System.exit(0);
    }

    /** */
    private final float[] thequeue = new float[QSIZE];
    /** */
    private int float_pointer_vector_end;

    /** */
    void encoder() throws IOException {

        init_encoder();
        Arrays.fill(thequeue, 0);
        for (int vnum = 0; read_sound_buffer(Constants.IDIM, thequeue, (vnum * Constants.IDIM) % QSIZE) > 0; vnum++) {
            float_pointer_vector_end = (vnum * Constants.IDIM) % QSIZE + Constants.IDIM;
            encode_vector(false);
            adapt_frame();
        }
    }

    /** */
    private void init_encoder() {
        init_pwf_adapter(pwf_z_coeff, pwf_p_coeff);
        _next[PWF_Z_COEFF][0] = _next[PWF_P_COEFF][0] = 1.0f;
        _obsolete_p[PWF_Z_COEFF] = 0;
        init_bsf_adapter(sf_coeff);
        _next[SF_COEFF][0] = 1.0f;
        _obsolete_p[SF_COEFF] = 0;
        init_gain_adapter(gp_coeff);
        init_gain_buf();
        _next[GP_COEFF][0] = 1.0f;
        _next[GP_COEFF][1] = -1.0f;
        _obsolete_p[GP_COEFF] = 0;
        init_input();
        float_pointer_vector_end = 0; // thequeue
        ZARR(imp_resp);
        imp_resp[0] = 1.0f;
        shape_conv(imp_resp, shape_energy);
    }

    /** */
    private void encode_vector(boolean ignore) throws IOException {
        // Computed Codebook Index
        int ix;
        // Index of Recently Read Vector
        int vx;
        // Logarithmic Gain Index
        int lgx;

        // recently read vector in the queue
        int float_pointer_vector;

        float[] zero_response = new float[Constants.IDIM];
        float[] weighted_speech = new float[Constants.IDIM];
        float[] target = new float[Constants.IDIM];
        float[] normtarg = new float[Constants.IDIM];
        float[] cb_vec = new float[Constants.IDIM];
        float[] pn = new float[Constants.IDIM];
        float gain = 1.0f;
        float scale = 1.0f;

        float_pointer_vector = float_pointer_vector_end - Constants.IDIM;
        if (float_pointer_vector < 0) {
            float_pointer_vector += QSIZE;
        }
        vx = float_pointer_vector;
        UPDATE(pwf_z_coeff, PWF_Z_COEFF);    // Copy new coeff if flag set
        UPDATE(pwf_p_coeff, PWF_P_COEFF);
        pwfilter2(thequeue, float_pointer_vector, weighted_speech);
        UPDATE(sf_coeff, SF_COEFF);
        zresp(zero_response);
        sub_sig(weighted_speech, zero_response, target);
        UPDATE(gp_coeff, GP_COEFF);
        gain = predict_gain();
        scale = 1.0f / gain;
        scaleSignals(scale, target, 0, normtarg, 0);
        UPDATE(imp_resp, IMP_RESP);
        trev_conv(imp_resp, normtarg, pn);
        UPDATE(shape_energy, SHAPE_ENERGY);
        ix = cb_index(pn);
        put_index(ix);
        cb_excitation(ix, cb_vec);
        scaleSignals(gain, cb_vec, 0, qspeech, vx);
        lgx = vx / Constants.IDIM;
        update_gain(qspeech, vx, log_gains, lgx);
        mem_update(qspeech, vx, synspeech, vx);
        dec_end = vx + Constants.IDIM;
        if (dec_end >= QSIZE) {
            dec_end -= QSIZE;
        }
        // declare array and its copy together with a semafor
        ffase.set(ffase.get() == 4 ? 1 : ffase.get() + 1);    // Update vector counter
    }

    /**
     * Update the filter coeff if we are at the correct vector in the frame
     * ffase is the vector count (1-4) within the current frame
     */
    void adapt_frame() {
        float[] input = new float[Constants.NUPDATE * Constants.IDIM];
        float[] synth = new float[Constants.NUPDATE * Constants.IDIM];
        float[] lg = new float[Constants.NUPDATE];
        // Index for log_gains, cycle end
        int gx;

        // Backward syn. filter coeff update.  Occurs after full frame (before
        // first vector) but not used until the third vector of the frame
        if (ffase.get() == 1) {
            CIRCOPY(synth, synspeech, dec_end,
                           Constants.NUPDATE * Constants.IDIM, QSIZE);
            bsf_adapter(synth, _next[SF_COEFF]); // Compute then new coeff
        }

        // Before third vector of frame
        if (ffase.get() == 3) {
            // Copy coeff computed above(2 frames later)
            _obsolete_p[SF_COEFF] = 1;
        }

        // Gain coeff update before second vector of frame
        if (ffase.get() == 2) {
            gx = dec_end / Constants.IDIM;
            CIRCOPY(lg, log_gains, gx, Constants.NUPDATE,
                           QSIZE / Constants.IDIM);
            gain_adapter(lg, _next[GP_COEFF]);
            _obsolete_p[GP_COEFF] = 1;
        }

        if (ffase.get() == 3) {
            CIRCOPY(input, thequeue, dec_end,
                           Constants.NUPDATE * Constants.IDIM, QSIZE);
            pwf_adapter(input, _next[PWF_Z_COEFF], _next[PWF_P_COEFF]);
            _obsolete_p[PWF_Z_COEFF] = 1;
            _obsolete_p[PWF_P_COEFF] = 1;
        }

        if (ffase.get() == 3) {
            iresp_vcalc(_next[SF_COEFF], _next[PWF_Z_COEFF],
                        _next[PWF_P_COEFF], _next[IMP_RESP]);
            shape_conv(_next[IMP_RESP], _next[SHAPE_ENERGY]);
            _obsolete_p[SHAPE_ENERGY] = 1;
            _obsolete_p[IMP_RESP] = 1;
        }
    }

    // CodeBook ---

    /** */
    static void iresp_vcalc(float[] sf_co,
                            float[] pwf_z_co,
                            float[] pwf_p_co,
                            float[] h) {

        float[] temp = new float[Constants.IDIM];
        float[] rc = new float[Constants.IDIM];
        temp[0] = rc[0] = 1.0f;
        for (int k = 1; k < Constants.IDIM; k++) {
            float a0 = 0.0f;
            float a1 = 0.0f;
            float a2 = 0.0f;
            for (int i = k; i >= 1; i--) {
                temp[i] = temp[i-1];
                rc[i] = rc[i-1];
                a0 -= sf_co[i] * temp[i];
                a1 += pwf_z_co[i] * temp[i];
                a2 -= pwf_p_co[i] * rc[i];
            }
            temp[0] = a0;
            rc[0] = a0 + a1 + a2;
        }
        for (int k = 0; k < Constants.IDIM; k++) {
            h[k] = rc[Constants.IDIM - 1 - k];
        }
    }

    /**
     * Unoptimized version -- kept for reference
     */
    static void shape_conv(float[] h, float[] shen) {

        float h0 = h[0];
        float h1 = h[1];
        float h2 = h[2];
        float h3 = h[3];
        float h4 = h[4];

        for (int j = 0; j < Constants.NCWD; j++) {
            float energy = 0;
            float tmp = h0 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][1] + h1 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][2] + h1 * cb_shape[j][1] +
                  h2 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][3] + h1 * cb_shape[j][2] +
                  h2 * cb_shape[j][1] + h3 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][4] + h1 * cb_shape[j][3] +
                  h2 * cb_shape[j][2] + h3 * cb_shape[j][1] +
                  h4 * cb_shape[j][0];
            energy += tmp * tmp;
            shen[j] = energy;
        }
    }

    /** Time Reversed Convolution Module -- Block 13 */
    static void trev_conv(float[] h, float[] target, float[] pn) {

        for (int k = 0; k < Constants.IDIM; k++) {
            float tmp = 0.0f;
            for (int j = k; j < Constants.IDIM; j++) {
                tmp += target[j] * h[j - k];
            }
            pn[k] = tmp;
        }
    }

    /**
     * Error Calculator and Best Codebook Index Selector
     * Blocks 17 and 18
     */
    static void cb_excitation(int ix, float[] v) {
        int sx = ix >> 3;
        int gx = ix & 7;
//Debug.println("sx: " + sx + ", ix: " + ix + ", gx: " + gx);
        float gain = cb_gain[gx];
        for (int i = 0; i < Constants.IDIM; i++) {
//Debug.println("v: " + i + "/" + v.length);
Debug.println("cb_shape: " + sx + ", " + i + "/" + cb_shape.length + ", " + cb_shape[sx].length);
            v[i] = cb_shape[sx][i] * gain;
        }
    }

    /** */
    private static int GTINC(float a, float b, int x) {
        if (a > b) {
            return x++;
        } else {
            return x;
        }
    }

    /** */
    int cb_index(float[] pn) {

        float distm = Constants.BIG;
        // best shape index
        int is = 0;
        // best gain index
        int ig = 0;

        int float_pointer_shape_ptr = 0;
        int float_pointer_sher_ptr = 0;
        int float_pointer_pb = 0;
        float cgm0 = cb_gain_mid_0;
        float cgm1 = cb_gain_mid_1;
        float cgm2 = cb_gain_mid_2;
        float cor = 0;

        final int minus5 = -5;

        for (int j = 0; j < Constants.NCWD; j++) {
            cor = cor - cor;
            float energy = shape_energy[float_pointer_sher_ptr++];

            float b0 = cgm0 * energy;
            int p = float_pointer_shape_ptr;
            float x = cb_shape[p / 5][p % 5];
            float_pointer_shape_ptr++;
            float y = pn[float_pointer_pb++];
            float t = x * y;
            p = float_pointer_shape_ptr;
            x = cb_shape[p / 5][p % 5];
            float_pointer_shape_ptr++;
            y = pn[float_pointer_pb++];
            cor += t;
            t = x * y;
            p = float_pointer_shape_ptr;
            x = cb_shape[p / 5][p % 5];
            float_pointer_shape_ptr++;
            y = pn[float_pointer_pb++];
            cor += t;
            t = x * y;
            p = float_pointer_shape_ptr;
            x = cb_shape[p / 5][p % 5];
            float_pointer_shape_ptr++;
            y = pn[float_pointer_pb++];
            cor += t;
            t = x * y;
            p = float_pointer_shape_ptr;
            x = cb_shape[p / 5][p % 5];
            float_pointer_shape_ptr++;
            y = pn[float_pointer_pb++];
            cor += t;
            t = x * y;
            cor += t;
            float b1 = cgm1 * energy;

            float_pointer_pb += minus5;
            float b2 = cgm2 * energy;
            // current gain index
            int idxg = 0;
            float pcor = cor;
            if (cor < 0.0) {
                pcor = -cor;
                idxg += 4;
            }
            idxg = GTINC(pcor, b0, idxg);
            idxg = GTINC(pcor, b1, idxg);
            idxg = GTINC(pcor, b2, idxg);

            float g2 = cb_gain2[idxg];
            float gsq = cb_gain_sq[idxg];
            float d = gsq * energy - g2 * cor;

            if (d < distm) {
                ig = idxg;
                is = j;
                distm = d;
            }
        }
        // resulting combined index
        int ichan = (is  << 3) + ig;
        return ichan;
    }

    // Common ----

    /** */
    private static void RCOPY(float[] X, int xofs, float[] Y, int yofs, int N) {
        System.arraycopy(Y, yofs, X, xofs, N);
    }

    /** */
    private static final float EPSILON = 1.0e-35f;

    // Use hand-pipelined loops for higher speed on 21000

    /** */
    private static float CLIPP(float X, float LOW, float HIGH) {
        return ((X) < (LOW) ? (LOW) : Math.min((X), (HIGH)));
    }

    /** */
    private static void scaleSignals(float scale,
                                     float[] a,
                                     int offsetA,
                                     float[] b,
                                     int offsetB) {
        for (int i = 0; i < Constants.IDIM; i++) {
// Debug.println("b: " + b.length);
// Debug.println("bi: " + offsetB + ", " + (offsetB + i));
// Debug.println("a: " + a.length);
// Debug.println("ai: " + offsetA + ", " + (offsetA + i));
            b[offsetB + i] = scale * a[offsetA + i];
        }
    }

    /** */
    private static void sub_sig(float[] A, float[] B, float[] C) {
        for (int i = 0; i < Constants.IDIM; i++) {
            C[i] = A[i] - B[i];
        }
    }

    // Circular Buffer Register numbers for ADSP21000

//    /** */
//    private static final float NPUT = 8;
//    /** */
//    private static final float NGET = 9;

    /** */
    private static void ZARR(float[] A) {
        for (int i = A.length - 1; i >= 0 ; i--) {
            A[i] = 0.0f;
        }
    }

    /**
     * Update obsoleted atomic array
     */
    private void UPDATE(float[] YYY, int name) {
        if (_obsolete_p[name] != 0) {
            for (int i = YYY.length - 1; i >= 0; i--) {
                YYY[i] = _next[name][i];
                _obsolete_p[name] = 0;
            }
        }
    }

    /**
     * Copy L words to X from circular buffer CIRC *ending* at offset EOS.
     * CL is the size of circular buffe CIRC
     */
    private static void CIRCOPY(float[] X, float[] CIRC, int EOS, int L, int CL) {
        int i1;
        int i2;
        int lx = 0;
        if ((EOS) >= (L)) {
            i1 = (EOS) - (L);
            i2 = (CL);
        } else {
            i1 = 0;
            i2 = (CL) + (EOS) - (L);
        }
        for (int i = i2; i < (CL); i++) {
            X[lx++] = CIRC[i];
        }
        for (int i = i1; i < (EOS); i++) {
            X[lx++] = CIRC[i];
        }
    }

//    /** get queue index of the most recent vector */
//    private float QINDEX() {
//        int qx = float_pointer_vector_end;
//        return qx != 0 ? QSIZE - Constants.IDIM : qx - Constants.IDIM;
//    }

    // DMain ----

    /** */
    private boolean postfiltering_p = false;

    /** Index of the start of the decoded speech vector */
    private int d_vec_start;
    /** Index of the end   of the decoded speech vector */
    private int d_vec_end;
    /** Index of the start of vector being written */
    private int w_vec_start;
    /** Index of the end   of vector being written */
    private int w_vec_end;

    /** */
    private volatile boolean decoder_done = false;

    /** */
    void decoder() throws IOException {
        Arrays.fill(thequeue, 0);
        init_decoder();
        for (w_vec_start = 0; !decoder_done; w_vec_start += Constants.IDIM) {
            if (w_vec_start >= QSIZE) {
                w_vec_start = 0;
            }
            w_vec_end = w_vec_start;
            float_pointer_vector_end = w_vec_end;
            decode_vector(false);
            w_vec_end = w_vec_start + Constants.IDIM;
            if (!decoder_done) {
                if (w_vec_end >= QSIZE) {
                    w_vec_end = 0;
                }
                write_sound_buffer(Constants.IDIM, thequeue, w_vec_end);
                adapt_decoder();
            }
        }
    }

    /** */
    private void init_decoder() {
        init_bsf_adapter(sf_coeff);
        _next[SF_COEFF][0] = 1.0f;
        _obsolete_p[SF_COEFF] = 0;
        init_gain_adapter(gp_coeff);
        init_gain_buf();
        _next[GP_COEFF][0] = 1.0f;
        _next[GP_COEFF][1] = -1.0f;
        _obsolete_p[GP_COEFF] = 0;
        init_output();
        float_pointer_vector_end = 0; // thequeue
    }

    /** */
    private void decode_vector(boolean ignore) throws IOException {
        // Computed Codebook Index
        int ix;
        // Log Gains INdex
        int lgx;
        float[] zero_response = new float[Constants.IDIM];
        float[] cb_vec = new float[Constants.IDIM];
        float[] pf_speech = new float[Constants.IDIM];
        float[] qs = new float[Constants.NUPDATE * Constants.IDIM];

        float gain = 1.0f;
        w_vec_end = float_pointer_vector_end;
        d_vec_start = w_vec_end + Constants.IDIM;
        if (d_vec_start >= QSIZE) {
            d_vec_start -= QSIZE;
        }
        ix = get_index();
//Debug.println("ix: " + ix);
        if (ix < 0) {
Debug.println("decoder_done");
            decoder_done = true; // TODO even though it's eof, just flag it?
        }

        UPDATE(sf_coeff, SF_COEFF);
        zresp(zero_response);
        cb_excitation(ix, cb_vec);
        UPDATE(gp_coeff, GP_COEFF);
        gain = predict_gain();
        scaleSignals(gain, cb_vec, 0, qspeech, d_vec_start);
        lgx = d_vec_start / Constants.IDIM;
        update_gain(qspeech, d_vec_start, log_gains, lgx);
        mem_update(qspeech, d_vec_start, synspeech, d_vec_start);
        d_vec_end = d_vec_start + Constants.IDIM;
        if (d_vec_end >= QSIZE) {
            d_vec_end -= QSIZE;
        }
        if (postfiltering_p) {
            inv_filter(synspeech, d_vec_start);
            if (ffase.get() == 3) {
              CIRCOPY(qs, synspeech, d_vec_end, Constants.NUPDATE * Constants.IDIM, QSIZE);
              psf_adapter(qs);
            }
            if (ffase.get() == 1) {
                compute_sh_coeff();
            }
            postfilter(synspeech, d_vec_start, pf_speech, 0);
            RCOPY(pf_speech, 0, thequeue, d_vec_start, Constants.IDIM);
        } else {
            RCOPY(synspeech, d_vec_start, thequeue, d_vec_start, Constants.IDIM);
        }
        // declare array and its copy together with a semafor
        ffase.set(ffase.get() == 4 ? 1 : ffase.get() + 1);
    }

    /** */
    void adapt_decoder() {
        float[] synth = new float[Constants.NUPDATE * Constants.IDIM];
        float[] lg = new float[Constants.NUPDATE];
        // gain index
        int gx;

        if (ffase.get() == 1) {
            CIRCOPY(synth, synspeech, d_vec_end, Constants.NUPDATE * Constants.IDIM, QSIZE);
            bsf_adapter(synth, _next[SF_COEFF]);
        }
        if (ffase.get() == 2) {
            gx = d_vec_end / Constants.IDIM;
            CIRCOPY(lg, log_gains, gx, Constants.NUPDATE, QSIZE / Constants.IDIM);
            gain_adapter(lg, _next[GP_COEFF]);
            _obsolete_p[GP_COEFF] = 1;
        }
        if (ffase.get() == 3) {
            _obsolete_p[SF_COEFF] = 1;
        }
    }

    // Data ----

    /** */
    private static final float[][] cb_shape = {
        { 0.326171875f, -1.440429688f, -0.612304688f, -0.874023438f, -1.246582031f},
        {-2.457031250f, -2.234863281f, -0.510253906f,  1.419921875f,  1.620117188f},
        {-1.376464844f, -1.307128906f, -0.462890625f, -1.379394531f, -2.172851563f},
        {-3.261230469f, -0.166015625f,  0.723632813f, -0.623046875f,  0.616210938f},
        {-0.274414063f, -3.299316406f,  0.625488281f,  0.087402344f, -0.622070313f},
        {-1.226562500f, -3.481445313f, -2.404785156f,  3.375488281f,  1.177246094f},
        {-1.209960938f, -0.076171875f,  2.286621094f, -1.891113281f,  0.000000000f},
        {-4.007812500f,  1.044921875f, -0.233398438f, -1.359863281f,  0.260253906f},
        { 0.922363281f,  1.347167969f,  0.674316406f, -3.395996094f, -2.887207031f},
        { 2.481445313f, -1.201171875f, -2.821289063f,  0.877441406f,  0.277343750f},
        {-1.078125000f, -1.615722656f, -2.208496094f, -3.044921875f, -3.664550781f},
        {-1.327636719f,  2.127929688f, -1.458984375f, -0.561035156f,  1.300781250f},
        { 0.614746094f,  0.485839844f,  1.323730469f, -1.203125000f, -5.073242188f},
        { 0.840820313f, -3.695800781f, -1.338867188f,  1.060058594f, -1.137207031f},
        { 0.503906250f,  0.364746094f, -0.418945313f, -3.879882813f, -6.270996094f},
        { 1.516601563f,  2.371093750f, -2.047363281f, -1.240722656f,  0.505371094f},
        { 0.909179688f, -0.468750000f, -3.236328125f,  0.200195313f,  2.872070313f},
        {-1.217285156f, -1.283203125f, -1.953125000f, -0.029296875f,  3.516601563f},
        {-1.304687500f,  0.706054688f,  0.750000000f, -1.870605469f,  0.602050781f},
        {-2.588867188f,  3.375000000f,  0.775878906f, -2.044433594f,  1.789550781f},
        {-1.687500000f, -3.989257813f, -3.764160156f,  0.675781250f,  2.293945313f},
        {-2.294433594f, -3.031738281f, -5.457031250f,  3.957031250f,  8.217773438f},
        { 0.454101563f,  3.419921875f,  0.619628906f, -4.383300781f,  1.253417969f},
        { 2.270019531f,  5.763671875f,  1.680175781f, -2.762207031f,  0.585449219f},
        { 1.241210938f, -0.089355469f, -4.325683594f, -3.894531250f,  1.577148438f},
        {-1.402343750f, -0.981933594f, -4.742675781f, -4.094238281f,  6.339355469f},
        { 1.506835938f,  1.044921875f, -1.796875000f, -4.708496094f, -1.414062500f},
        {-3.715332031f,  3.181152344f, -1.114746094f, -1.231445313f,  3.091796875f},
        {-1.627441406f, -2.744140625f, -4.458007813f, -5.435058594f,  2.706542969f},
        {-0.198730469f, -3.281738281f, -8.528320313f, -1.410644531f,  5.648437500f},
        { 1.802734375f,  3.318359375f, -0.127929688f, -5.295898438f, -0.906250000f},
        { 3.552246094f,  6.544921875f, -1.459472656f, -5.173339844f,  2.410156250f},
        { 0.119140625f, -1.083496094f,  1.296875000f,  1.843750000f, -2.642578125f},
        {-1.974121094f, -2.897460938f,  1.040527344f,  0.421386719f, -1.399414063f},
        {-1.612304688f,  0.851074219f, -0.979492188f, -0.062500000f, -1.001953125f},
        {-3.105957031f,  1.631835938f, -0.772949219f, -0.010253906f,  0.557617188f},
        {-1.873535156f, -0.894042969f,  3.123535156f,  1.242675781f, -1.390625000f},
        {-4.556640625f, -3.187500000f,  2.592285156f,  0.969726563f, -1.096191406f},
        {-2.192382813f,  0.365234375f,  0.944824219f, -1.478027344f, -0.240722656f},
        {-4.519042969f,  2.620117188f,  1.559082031f, -2.193847656f,  0.871093750f},
        { 2.335937500f, -0.180664063f,  0.911132813f,  0.516113281f, -0.922363281f},
        { 3.584960938f, -1.313476563f, -1.258300781f,  0.330078125f, -0.298339844f},
        {-0.245117188f,  1.091308594f, -0.903320313f, -0.867675781f, -1.000488281f},
        { 0.493652344f,  1.894531250f, -1.203613281f,  1.078613281f, -0.074218750f},
        { 1.265625000f,  1.381347656f,  2.728515625f,  1.386230469f, -3.567382813f},
        {-1.488769531f, -2.401367188f,  2.907714844f,  4.492675781f, -2.171386719f},
        { 0.340332031f,  1.908203125f,  2.831054688f, -2.173339844f, -2.267578125f},
        {-1.035644531f,  2.658203125f, -1.254882813f,  0.156738281f, -0.586914063f},
        { 1.389648438f, -1.018554688f,  1.724609375f,  0.276367188f, -0.345703125f},
        {-2.089355469f,  0.463867188f,  2.431640625f,  1.830566406f,  0.220703125f},
        {-1.212890625f,  1.709960938f,  0.839355469f, -0.083007813f,  0.116210938f},
        {-1.677246094f,  0.128417969f,  1.032226563f, -0.979003906f,  1.152832031f},
        {-3.583007813f, -0.589843750f,  4.563964844f, -0.593750000f, -1.959472656f},
        {-6.590820313f, -0.214355469f,  3.919921875f, -2.066406250f,  0.176269531f},
        {-1.820800781f,  2.652832031f,  0.978515625f, -2.308105469f, -0.614746094f},
        {-1.946289063f,  3.780761719f,  4.115722656f, -1.802246094f, -0.481933594f},
        { 2.538085938f, -0.206542969f,  0.561523438f, -0.625488281f,  0.398437500f},
        { 3.617675781f,  2.006347656f, -1.928222656f,  1.313476563f,  0.014648438f},
        { 0.608398438f,  1.491699219f, -0.017089844f, -0.668945313f, -0.120117188f},
        {-0.727050781f,  2.751464844f, -0.331054688f, -1.282714844f,  1.547851563f},
        { 2.358398438f, -2.238769531f,  0.980468750f, -0.518554688f,  0.390136719f},
        {-0.062988281f,  0.350097656f,  2.243164063f,  7.293457031f,  5.227539063f},
        { 0.203613281f,  1.347167969f,  0.903320313f, -2.469238281f, -0.562988281f},
        {-1.897949219f,  3.594238281f, -2.816406250f,  2.092285156f,  0.325195313f},
        { 0.704589844f, -0.458007813f,  0.009765625f, -1.034667969f, -0.828613281f},
        {-1.812500000f, -1.661132813f, -1.080078125f,  0.053710938f,  1.042968750f},
        {-1.441406250f,  0.005859375f, -0.765625000f, -1.708984375f, -0.905761719f},
        {-0.642089844f, -0.845214844f,  0.566406250f, -0.272460938f,  0.834472656f},
        { 0.042968750f, -2.230957031f,  0.094726563f, -0.221679688f, -1.443847656f},
        {-1.386230469f, -0.813476563f, -0.133300781f,  1.017578125f, -0.075683594f},
        {-0.092285156f, -1.160156250f,  0.812011719f, -0.507812500f, -1.195800781f},
        {-1.387695313f, -0.668457031f,  0.310546875f, -0.121093750f, -1.307128906f},
        { 0.740722656f,  0.038574219f, -1.471191406f, -1.791503906f, -0.475097656f},
        { 0.934082031f, -1.217285156f, -2.593750000f, -0.365722656f,  0.620605469f},
        {-1.417480469f, -1.623046875f, -1.833984375f, -1.801757813f, -0.893066406f},
        {-1.422363281f, -0.755371094f, -1.347656250f, -0.686523438f,  0.548828125f},
        { 0.900390625f, -0.895507813f,  0.222656250f,  0.344726563f, -2.085937500f},
        { 0.228027344f, -2.078125000f, -0.932128906f,  0.742675781f,  0.553710938f},
        {-0.062011719f, -0.485351563f, -0.311035156f, -0.728027344f, -3.170898438f},
        { 0.426269531f, -0.998535156f, -1.869140625f, -1.363281250f, -0.282226563f},
        { 1.128417969f, -0.887207031f,  1.285156250f, -1.490234375f,  0.960937500f},
        { 0.312988281f,  0.583007813f,  0.924316406f,  2.005371094f,  3.096679688f},
        {-0.021972656f,  0.584960938f,  1.054687500f, -0.707519531f,  1.075683594f},
        {-0.978515625f,  0.836425781f,  1.717773438f,  1.294921875f,  2.075683594f},
        { 1.433593750f, -1.937500000f,  0.625000000f,  0.063964844f, -0.720703125f},
        { 1.380371094f,  0.003906250f, -0.941406250f,  1.297851563f,  1.715332031f},
        { 1.562011719f, -0.398437500f,  1.312011719f, -0.850097656f, -0.687011719f},
        { 1.439453125f,  1.967285156f,  0.192382813f, -0.123535156f,  0.633789063f},
        { 2.092773438f,  0.024902344f, -2.200683594f, -0.015625000f, -0.321777344f},
        { 1.905761719f,  2.756835938f, -2.728515625f, -1.265625000f,  2.786621094f},
        {-0.295898438f,  0.602539063f, -0.784667969f, -2.532714844f,  0.324218750f},
        {-0.256347656f,  1.767578125f, -1.070312500f, -1.233886719f,  0.833496094f},
        { 2.098144531f, -1.587402344f, -1.114746094f,  0.396484375f, -1.105468750f},
        { 2.814941406f,  0.257812500f, -1.604980469f,  0.660156250f,  0.816406250f},
        { 1.335449219f,  0.605957031f, -0.538574219f, -1.598144531f, -1.663574219f},
        { 1.969238281f,  0.804687500f, -1.447753906f, -0.573242188f,  0.705078125f},
        { 0.036132813f,  0.448242188f,  0.976074219f,  0.446777344f, -0.500976563f},
        {-1.218750000f, -0.783691406f,  0.993164063f,  1.440429688f,  0.111816406f},
        {-1.058593750f,  0.994628906f,  0.007324219f, -0.617187500f, -0.101562500f},
        {-1.734375000f,  0.747070313f,  0.283691406f,  0.728027344f,  0.469726563f},
        {-1.275878906f, -1.141601563f,  1.768066406f, -0.726562500f, -1.066894531f},
        {-0.853027344f,  0.039550781f,  2.704101563f,  0.699218750f, -1.102050781f},
        {-0.497558594f,  0.423339844f,  0.104492188f, -1.115234375f, -0.737304688f},
        {-0.822265625f,  1.375000000f, -0.111816406f,  1.245605469f, -0.678222656f},
        { 1.321777344f,  0.246093750f,  0.233886719f,  1.358886719f, -0.492675781f},
        { 1.229003906f, -0.726074219f, -0.779296875f,  0.303222656f,  0.941894531f},
        {-0.072265625f,  1.077148438f, -2.093750000f,  0.630859375f, -0.684082031f},
        {-0.257324219f,  0.606933594f, -1.333496094f,  0.932128906f,  0.625000000f},
        { 1.049316406f, -0.732910156f,  1.800781250f,  0.297851563f, -2.241699219f},
        { 1.614257813f, -1.645019531f,  0.915527344f,  1.775390625f, -0.594238281f},
        { 1.256835938f,  1.227050781f,  0.707519531f, -1.500976563f, -2.431152344f},
        { 0.397460938f,  0.891601563f, -1.219238281f,  2.067382813f, -1.990722656f},
        { 0.812500000f, -0.107421875f,  1.668945313f,  0.489257813f,  0.544433594f},
        { 0.381347656f,  0.809570313f,  1.913574219f,  2.993164063f,  1.533203125f},
        { 0.560546875f,  1.984863281f,  0.740234375f,  0.397949219f,  0.097167969f},
        { 0.581542969f,  1.215332031f,  1.250488281f,  1.182128906f,  1.192871094f},
        { 0.375976563f, -2.888183594f,  2.692871094f, -0.179687500f, -1.562011719f},
        { 0.581054688f,  0.511230469f,  1.827148438f,  3.382324219f, -1.020019531f},
        { 0.142578125f,  1.513183594f,  2.103515625f, -0.370117188f, -1.198730469f},
        { 0.255371094f,  1.914550781f,  1.974609375f,  0.676757813f,  0.041503906f},
        { 2.132324219f,  0.491210938f, -0.611328125f, -0.715820313f, -0.675292969f},
        { 1.880859375f,  0.770996094f, -0.037597656f,  1.007812500f,  0.423828125f},
        { 2.494628906f,  1.425292969f, -0.098632813f,  0.175292969f, -0.248535156f},
        { 1.782226563f,  1.565429688f,  1.124511719f,  0.826660156f,  0.632812500f},
        { 1.418457031f, -1.907714844f,  0.111816406f, -0.583984375f, -1.138671875f},
        { 2.918457031f, -1.750488281f,  0.393066406f,  1.867675781f, -1.532226563f},
        { 1.829101563f, -0.295898438f,  0.025878906f, -0.131347656f, -1.611816406f},
        { 0.295898438f,  0.985351563f, -0.642578125f,  1.984375000f,  0.194335938f}
    };

    /** */
    static final float[] cb_gain = {
        0.515625f,  .90234375f,  1.579101563f,  2.763427734f,
        -0.515625f, -.90234375f, -1.579101563f, -2.763427734f
    };

    /** Double Gains: */
    static final float[] cb_gain2 = {
        1.031250f,  1.8046875f, 3.158203126f,  5.526855468f,
        -1.031250f, -1.8046875f, -3.158203126f, -5.526855468f
    };

    /** Midpoints: */
    static final float[] cb_gain_mid = {
        0.708984375f,  1.240722656f,  2.171264649f, 0f,
        -0.708984375f, -1.240722656f, -2.171264649f, 0f
    };

    /** Squared Gains: */
    static final float[] cb_gain_sq = {
        0.26586914f, 0.814224243f, 2.493561746f, 7.636532841f,
        0.26586914f, 0.814224243f, 2.493561746f, 7.636532841f
    };

    /** */
    static final float[] hw_gain = {
        0.583953857f, 0.605346680f, 0.627502441f, 0.650482178f, 0.674316406f,
        0.699005127f, 0.724578857f, 0.751129150f, 0.778625488f, 0.807128906f,
        0.836669922f, 0.867309570f, 0.899078369f, 0.932006836f, 0.961486816f,
        0.982757568f, 0.995635986f, 1.000000000f, 0.995819092f, 0.983154297f,
        0.962066650f, 0.932769775f, 0.895507812f, 0.850585938f, 0.798400879f,
        0.739379883f, 0.674072266f, 0.602996826f, 0.526763916f, 0.446014404f,
        0.361480713f, 0.273834229f, 0.183868408f, 0.092346191f
    };

    /** */
    static final float[] hw_percw = {
        0.581085205f, 0.591217041f, 0.601562500f, 0.612091064f, 0.622772217f,
        0.633666992f, 0.644744873f, 0.656005859f, 0.667480469f, 0.679138184f,
        0.691009521f, 0.703094482f, 0.715393066f, 0.727874756f, 0.740600586f,
        0.753570557f, 0.766723633f, 0.780120850f, 0.793762207f, 0.807647705f,
        0.821746826f, 0.836120605f, 0.850738525f, 0.865600586f, 0.880737305f,
        0.896148682f, 0.911804199f, 0.927734375f, 0.943939209f, 0.960449219f,
        0.975372314f, 0.986816406f, 0.994720459f, 0.999084473f, 0.999847412f,
        0.997070312f, 0.990722656f, 0.980865479f, 0.967468262f, 0.950622559f,
        0.930389404f, 0.906829834f, 0.880035400f, 0.850097656f, 0.817108154f,
        0.781219482f, 0.742523193f, 0.701171875f, 0.657348633f, 0.611145020f,
        0.562774658f, 0.512390137f, 0.460174561f, 0.406311035f, 0.351013184f,
        0.294433594f, 0.236816406f, 0.178375244f, 0.119262695f, 0.059722900f
    };

    /** */
    static final float[] hw_synth = {
        0.602020264f, 0.606384277f, 0.610748291f, 0.615142822f, 0.619598389f,
        0.624084473f, 0.628570557f, 0.633117676f, 0.637695312f, 0.642272949f,
        0.646911621f, 0.651580811f, 0.656280518f, 0.661041260f, 0.665802002f,
        0.670593262f, 0.675445557f, 0.680328369f, 0.685241699f, 0.690185547f,
        0.695159912f, 0.700164795f, 0.705230713f, 0.710327148f, 0.715454102f,
        0.720611572f, 0.725830078f, 0.731048584f, 0.736328125f, 0.741638184f,
        0.747009277f, 0.752380371f, 0.757812500f, 0.763305664f, 0.768798828f,
        0.774353027f, 0.779937744f, 0.785583496f, 0.791229248f, 0.796936035f,
        0.802703857f, 0.808502197f, 0.814331055f, 0.820220947f, 0.826141357f,
        0.832092285f, 0.838104248f, 0.844146729f, 0.850250244f, 0.856384277f,
        0.862548828f, 0.868774414f, 0.875061035f, 0.881378174f, 0.887725830f,
        0.894134521f, 0.900604248f, 0.907104492f, 0.913635254f, 0.920227051f,
        0.926879883f, 0.933563232f, 0.940307617f, 0.947082520f, 0.953918457f,
        0.960815430f, 0.967742920f, 0.974731445f, 0.981781006f, 0.988861084f,
        0.994842529f, 0.998565674f, 0.999969482f, 0.999114990f, 0.996002197f,
        0.990600586f, 0.982910156f, 0.973022461f, 0.960876465f, 0.946533203f,
        0.930053711f, 0.911437988f, 0.890747070f, 0.868041992f, 0.843322754f,
        0.816680908f, 0.788208008f, 0.757904053f, 0.725891113f, 0.692199707f,
        0.656921387f, 0.620178223f, 0.582000732f, 0.542480469f, 0.501739502f,
        0.459838867f, 0.416900635f, 0.373016357f, 0.328277588f, 0.282775879f,
        0.236663818f, 0.189971924f, 0.142852783f, 0.095428467f, 0.047760010f
    };

    // Data2 ----

    static final float cb_gain_mid_0 = 0.708984375f;
    static final float cb_gain_mid_1 = 1.240722656f;
    static final float cb_gain_mid_2 = 2.171264649f;
    static final float cb_gain_mid_3 = 0f;
    static final float cb_gain_mid_4 = -0.708984375f;
    static final float cb_gain_mid_5 = -1.240722656f;
    static final float cb_gain_mid_6 = -2.171264649f;
    static final float cb_gain_mid_7 = 0f;

    static final float cb_gain2_0 =  1.031250f;
    static final float cb_gain2_1 =  1.846875f;
    static final float cb_gain2_2 =  3.158203f;
    static final float cb_gain2_3 =  5.526855f;
    static final float cb_gain2_4 =  -1.031250f;
    static final float cb_gain2_5 =  -1.846875f;
    static final float cb_gain2_6 =  -3.158203f;
    static final float cb_gain2_7 =  -5.526855f;

    static final float cb_gain_sq_0 = 0.265869f;
    static final float cb_gain_sq_1 = 0.8527368f;
    static final float cb_gain_sq_2 = 2.493562f;
    static final float cb_gain_sq_3 = 7.636533f;
    static final float cb_gain_sq_4 = 0.265869f;
    static final float cb_gain_sq_5 = 0.8527368f;
    static final float cb_gain_sq_6 = 2.493562f;
    static final float cb_gain_sq_7 = 7.636533f;

    // Postfilter coefficients for low-pass = decimating IIR filter

    static final float A1 = -2.34036589f;
    static final float A2 =  2.01190019f;
    static final float A3 = -0.614109218f;

    static final float B0 =  0.0357081667f;
    static final float B1 = -0.0069956244f;
    static final float B2 = -0.0069956244f;
    static final float B3 =  0.0357081667f;

    // Fast ----

    /** */
    static void PROD5(float[] A, float[] B, float R) {
        int float_pointer_pa = 0;
        int float_pointer_pb = 0;
        float x = A[float_pointer_pa++];
        float y = B[float_pointer_pb++];
        R = R - R;
        float t = x * y;
        x = A[float_pointer_pa++];
        y = B[float_pointer_pb++];
        R += t;
        t = x * y;
        x = A[float_pointer_pa++];
        y = B[float_pointer_pb++];
        R += t;
        t = x * y;
        x = A[float_pointer_pa++];
        y = B[float_pointer_pb++];
        R += t;
        t = x * y;
        x = A[float_pointer_pa++];
        y = B[float_pointer_pb++];
        R += t;
        t = x * y;
        R += t;
    }

    /**
     * Autocorrelation : R[0:K] is autocorrelation of X[M:N-1]  i.e.
     * R[k] = Sum X[i]*X[i-k]   for    M<=i<N
     */
    static void AUTOCORR(float[] X, float[] R, int K, int M, int N) {
        for (int ii = 0; ii <= K; ii++) {
            R[ii] = 0;
            for (int jj = M; jj < N; jj++) {
                R[ii] += X[jj] * X[jj - ii];
            }
        }
    }

    /**
     * if AC is autocorrelation of X (as in above) , then
     * R[i] = D * R[i] + AC[i]
     */
    static void RECACORR(float[] X, float[] R, int K, int M, int N, float D) {
        int M1 = M + 1;
        int float_pointer_rp = 0;
        for (int k = 0; k <= K; k++) {
            float t;
            int float_pointer_xip = M;
            int float_pointer_xkp = float_pointer_xip - k;
            float corr = 0;
            float xi = X[float_pointer_xip++];
            float xk = X[float_pointer_xkp++];
            for (int i = M1; i < N; i++) {
                t = xi * xk;
                xi = X[float_pointer_xip++];
                corr += t;
                xk = X[float_pointer_xkp++];
            }
            t = xi * xk;
            corr += t;
            corr = R[float_pointer_rp] * D + corr;
            R[float_pointer_rp++] = corr;
        }
    }

    /**
     * Z[i] = X[i] * Y[i]
     */
    static void VPROD(float[] X, float[] Y, float[] Z, int N) {
        int float_pointer_xp = 0;
        int float_pointer_yp = 0;
        int float_pointer_zp = 0;
        float xi = X[float_pointer_xp++];
        float yi = Y[float_pointer_yp++];
        float zi;
        for (int i = 1; i < N; i++) {
            zi = xi * yi;
            xi = X[float_pointer_xp++];
            yi = Y[float_pointer_yp++];
            Z[float_pointer_zp++] = zi;
        }
        zi = xi * yi;
        Z[float_pointer_zp] = zi;
    }

    /** */
    static void RSHIFT(float[] START, int LENGTH, int SHIFT) {
        int float_pointer_from = LENGTH - 1;
        int float_pointer_to = LENGTH + SHIFT - 1;
        for (int i = 0; i < LENGTH; i++) {
            START[float_pointer_to--] = START[float_pointer_from--];
        }
    }

    /**
     * Must have at least two elements, i.e. N >= 2.
     * computes sum(x[i] * y[i])
     */
    static float DOTPROD(float[] X, int xofs, float[] Y, int yofs, int N) {
        float r = 0;
        int float_pointer_xp = xofs;
        int float_pointer_yp = yofs;
        float x1 = X[float_pointer_xp++];
        float y1 = Y[float_pointer_yp++];
        float t = x1 * y1;
        x1 = X[float_pointer_xp++];
        y1 = Y[float_pointer_yp++];
        for (int i = 0; i < N - 2; i++) {
            r += t;
            t = x1 * y1;
            x1 = X[float_pointer_xp++];
            y1 = Y[float_pointer_yp++];
        }
        r += t;
        t = x1 * y1;
        r += t;
        return r;
    }

    // Filters ----

    /** */
    private final float[] firmem = new float[Constants.LPCW + Constants.IDIM];
    /** */
    private final float[] iirmem = new float[Constants.LPCW + Constants.IDIM];

    /** */
    void pwfilter2(float[] input, int offset, float[] output) {

        RSHIFT(firmem, Constants.LPCW, Constants.IDIM);
        for (int k = 0; k < Constants.IDIM; k++) {
            firmem[k] = input[offset + Constants.IDIM - 1 - k];
        }
        RSHIFT(iirmem, Constants.LPCW, Constants.IDIM);

        for (int k = 0; k < Constants.IDIM; k++) {
            // pwf_z_coeff[0] is always 1.0
            float out = firmem[Constants.IDIM - 1 - k];
            out += DOTPROD(firmem, Constants.IDIM - k, pwf_z_coeff, 1, Constants.LPCW);
            out -= DOTPROD(iirmem, Constants.IDIM - k, pwf_p_coeff, 1, Constants.LPCW);
            iirmem[Constants.IDIM - 1 - k] = out;
            output[k] = out;
        }
    }

    // Synthesis and Perceptual Weighting Filter.

    /** */
    float[] statelpc = new float[Constants.LPC + Constants.IDIM];
    /** */
    float[] zirwfir = new float[Constants.LPCW];
    /** */
    float[] zirwiir = new float[Constants.LPCW];

    /** Updateable coefficients */
    void sf_zresp(float[] output) {

        // This is un-pipelined version of the above. Kept for reference
        for (int j = Constants.LPC - 1; j >= 0; j--) {
            statelpc[j + Constants.IDIM] = statelpc[j];
        }
        for (int k = 0; k < Constants.IDIM; k++) {
            float out = 0.0f, sj, aj;
            sj = statelpc[Constants.LPC + Constants.IDIM - k - 1];
            aj = sf_coeff[Constants.LPC];
            for (int j = Constants.LPC - 2; j >= 1; j--) {
                out -= sj * aj;
                sj = statelpc[Constants.IDIM - k + j];
                aj = sf_coeff[j + 1];
            }
            output[k] = out - sj * aj - statelpc[Constants.IDIM - k] * sf_coeff[1];
            statelpc[Constants.IDIM - 1 - k] = output[k];
        }
    }

    /** */
    void pwf_zresp(float[] input, float[] output) {

        // Un-pipelined version, kept for reference
        for (int k = 0; k < Constants.IDIM; k++) {
            float tmp = input[k];
            for (int j = Constants.LPCW - 1; j >= 1; j--) {
                input[k] += zirwfir[j] * pwf_z_coeff[j + 1];
                zirwfir[j] = zirwfir[j - 1];
            }
            input[k] += zirwfir[0] * pwf_z_coeff[1];
            zirwfir[0] = tmp;
            for (int j = Constants.LPCW - 1; j >= 1; j--) {
                input[k] -= zirwiir[j] * pwf_p_coeff[j + 1];
                zirwiir[j] = zirwiir[j - 1];
            }
            output[k] = input[k] - zirwiir[0] * pwf_p_coeff[1];
            zirwiir[0] = output[k];
        }
    }

    /** */
    void zresp(float[] output) {
        float[] temp = new float[Constants.IDIM];
        sf_zresp(temp);
        pwf_zresp(temp, output);
    }

    void mem_update(float[] input, int inofs, float[] output, int outofs) {
        float[] temp = new float[Constants.IDIM];
        int float_pointer_t2 = 0; // zirwfir
        zirwfir[float_pointer_t2] = temp[0] = input[inofs];
        for (int k = 1; k < Constants.IDIM; k++) {
            float a0 = input[inofs + k];
            float a1 = 0.0f;
            float a2 = 0.0f;
            for (int i = k; i >= 1; i--) {
                zirwfir[float_pointer_t2 + i] = zirwfir[float_pointer_t2 + i - 1];
                temp[i] = temp[i - 1];
                a0 -=   sf_coeff[i] * zirwfir[float_pointer_t2 + i];
                a1 += pwf_z_coeff[i] * zirwfir[float_pointer_t2 + i];
                a2 -= pwf_p_coeff[i] * temp[i];
            }
            zirwfir[float_pointer_t2] = a0;
            temp[0] = a0 + a1 + a2;
        }

        for (int k = 0; k < Constants.IDIM; k++) {
            statelpc[k] += zirwfir[float_pointer_t2 + k];
            if (statelpc[k] > Constants.MAX) {
                statelpc[k] = Constants.MAX;
            }
            else if (statelpc[k] < Constants.MIN) {
                statelpc[k] = Constants.MIN;
            }
            zirwiir[k] += temp[k];
        }
        System.arraycopy(statelpc, 0, zirwfir, 0, Constants.LPCW);
        for (int k = 0; k < Constants.IDIM; k++) {
            output[outofs + k] = statelpc[Constants.IDIM - 1 - k];
        }
    }

    // The Gain Predictor

    private final float[] gain_input = new float[Constants.LPCLG];

    /** */
    private static float log_rms(float[] input, int offset) {
        float etrms = 0.0f;
        for (int k = offset; k < Constants.IDIM; k++) {
            etrms += input[k] * input[k];
        }
        etrms /= Constants.IDIM;
        if (etrms < 1.0) {
            etrms = 1.0f;
        }
        etrms = 10.0f * (float) (Math.log(etrms) / Math.log(10));
        return etrms;
    }

    /** */
    float predict_gain() {
        float new_gain = Constants.GOFF;
        float temp;

        for (int i = 1; i <= Constants.LPCLG; i++) {
            temp = gp_coeff[i] * gain_input[Constants.LPCLG - i];
            new_gain -= temp;
        }
        if (new_gain < 0.0) {
            new_gain = 0.0f;
        }
        if (new_gain > 60.0) {
            new_gain = 60.0f;
        }
        new_gain = (float) Math.pow(10, 0.05f * new_gain);
        return new_gain;
    }

    /** */
    void update_gain(float[] input,
                     int offset,
                     float[] lgp,
                     int float_pointer_lgp) {

        lgp[float_pointer_lgp] = log_rms(input, offset) - Constants.GOFF;
        for (int i = 0; i < Constants.LPCLG - 1; i++) {
            gain_input[i] = gain_input[i + 1];
        }
        gain_input[Constants.LPCLG - 1] = lgp[float_pointer_lgp];
    }

    /** */
    void init_gain_buf() {

        Arrays.fill(gain_input, -Constants.GOFF);
        for (int i = 0; i < QSIZE / Constants.IDIM; i++) {
            log_gains[i] = -Constants.GOFF;
        }
    }

    // Global ----

    float[] sf_coeff = new float[Constants.LPC + 1];
    float[] gp_coeff = new float[Constants.LPCLG + 1];
    float[] pwf_z_coeff = new float[Constants.LPCW + 1];
    float[] pwf_p_coeff = new float[Constants.LPCW + 1];
    float[] shape_energy = new float[Constants.NCWD];
    float[] imp_resp = new float[Constants.IDIM];

    float[][] _next = new float[][] {
        new float[Constants.LPC + 1],
        new float[Constants.LPCLG + 1],
        new float[Constants.LPCW + 1],
        new float[Constants.LPCW + 1],
        new float[Constants.NCWD],
        new float[Constants.IDIM]
    };

    static final int SF_COEFF = 0;
    static final int GP_COEFF = 1;
    static final int PWF_Z_COEFF = 2;
    static final int PWF_P_COEFF = 3;
    static final int SHAPE_ENERGY = 4;
    static final int IMP_RESP = 5;

    int[] _obsolete_p = new int[6];

    static final int QSIZE = 60;

    /** Synthesized Speech */
    float[] synspeech = new float[QSIZE];
    /** Quantized  Speech */
    float[] qspeech = new float[QSIZE];
    /** Logarithm of Gains */
    float[] log_gains = new float[QSIZE / Constants.IDIM];

    AtomicInteger ffase = new AtomicInteger(-4);

    // IOSparc ----

    /** Scaling factor for input */
    float rscale = 0.125f;

    String xfile_name;

    /** output file (codebook indices) */
    FileOutputStream oxfd;
    /** input file */
    FileInputStream ifd;

    /** */
    String ifile_name;

    /** */
    void init_input() {
        try {
            ifd = new FileInputStream(ifile_name);
        } catch (IOException e) {
System.err.println("Can't open \"" + ifile_name + "\"\n");
            System.exit(1);
        }
        try {
            oxfd = new FileOutputStream(xfile_name);
        } catch (IOException e) {
System.err.println("Can't open \"" + xfile_name + "\"\n");
        }
    }

    /** */
    void put_index(int x) throws IOException {
        oxfd.write((x & 0xff00) >> 8);
        oxfd.write( x & 0x00ff);
    }

    /** */
    String ofile_name;

    /** Outpu file */
    private FileOutputStream ofd;
    /** Input file (codebook indices) */
    private FileInputStream ixfd;
    int sound_overflow = 0;

    /** */
    void init_output() {
        sound_overflow = 0;
        try {
            ofd = new FileOutputStream(ofile_name);
        } catch (IOException e) {
System.err.println("Can't open \"" + ofile_name + "\" for output\n");
            System.exit(1);
        }
        try {
            ixfd = new FileInputStream(xfile_name);
        } catch (IOException e) {
System.err.println("Can't open \"" + xfile_name + "\"\n");
            System.exit(3);
        }
    }

    /** */
    int get_index() throws IOException {
        int c1 = ixfd.read();
        int c2 = ixfd.read();
        return (short) (c1 << 8 | c2);
    }

    /** Return Number of Samples Read */
    int read_sound_buffer(int n, float[] buf, int offset) throws IOException {
        int c = 0;

        for (int i = 0; i < n; i++) {
            int c1 = ifd.read();
            int c2 = ifd.read();
            if (c1 == -1 || c2 == -1) {
                break;
            }
            int s = (c1 << 8) | c2;
            buf[offset + c++] = rscale * s;
        }
        return c;
    }

    /** */
    void write_sound_buffer(int n, float[] buf, int offset)
        throws IOException {

        for (int i = 0; i < n; i++) {
            float xx = buf[offset + i] / rscale;
            if (xx > 0.0) {
                if (xx > 32767.0) {
                    xx = 32767.0f;
                } else {
                    xx += 0.5f;
                }
            } else {
                if (xx < -32768.0) {
                    xx = -32768.0f;
                } else {
                    xx -= 0.5f;
                }
            }
            int s = (int) xx;
            ofd.write((s & 0xff00) >> 8);
            ofd.write( s & 0x00ff);
        }
    }

    // PostFil ----

    // Parameters from the adapter:

    /** */
    private static final int SPORDER = 10;

    /** */
    private static final int DECIM = 4;
    /** Postfilter Memory SIZE */
    private static final int PMSIZE = (Constants.NPWSZ + Constants.KPMAX);
    /** Post. Decim. Memory SIZE */
    private static final int PDMSIZE = (PMSIZE / DECIM);
    /** Max. Decimated Period */
    private static final int DPERMAX = (Constants.KPMAX / DECIM);
    /** Min. Decimated Period */
    private static final int DPERMIN = (Constants.KPMIN / DECIM);
    private static final int SHIFTSZ = (Constants.KPMAX + Constants.NPWSZ - Constants.NFRSZ + Constants.IDIM);

    /** Post-Filter Memory for syn. sp. */
    private final float[] tap_mem = new float[Constants.KPMAX + Constants.NPWSZ + Constants.IDIM];

    /** */
    int pitch_period = 50;

    /** */
    float pitch_tap = 0.0f;

    /** */
    float pitch_gain = 1f;

    /** Precomputed Scales for IIR coefficients */
    private static final float[] shzscale = {
        1.0f,
        0.6500244140625f,
        0.4224853515625f,
        0.27459716796875f,
        0.17852783203125f,
        0.11602783203125f,
        0.075439453125f,
        0.04901123046875f,
        0.0318603515625f,
        0.02069091796875f,
        0.01348876953125f
    };

    /** Precomputed Scales for FIR Coefficients */
    private static final float[] shpscale = {
        1.0f,
        0.75f,
        0.5625f,
        0.421875f,
        0.31640625f,
        0.2373046875f,
        0.177978515625f,
        0.13348388671875f,
        0.10009765625f,
        0.0750732421875f,
        0.05633544921875f
    };

    /** Short Term Filter (Poles/IIR) Coefficients */
    private final float[] shpcoef = new float[SPORDER + 1];
    /** Short Term Filter (Zeros/FIR) Coefficients */
    private final float[] shzcoef = new float[SPORDER + 1];
    private float tiltz;
    /** Post-Filter Memory for residual */
    private final float[] fil_mem = new float[PMSIZE];

    /**
     * Compute sum of absolute values of vector V
     */
    private static float vec_abs(float[] v, int offset) {
        float r = Math.abs(v[offset]);
        for (int i = 1; i < Constants.IDIM; i++) {
            r += Math.abs(v[offset + i]);
        }
        return r;
    }

    /** Inverse Filter */
    void inv_filter(float[] input, int offset) {
        int ip = Constants.IDIM;
        float[] mem1 = new float[SPORDER + Constants.NFRSZ];

        // Shift in input into mem1
        for (int i = Constants.IDIM; i < SPORDER + Constants.IDIM; i++) {
            mem1[i - Constants.IDIM] = mem1[i];
        }
        System.arraycopy(input, offset + 0, mem1, 10, Constants.IDIM);
        for (int k = 0; k < Constants.IDIM; k++) {
            float tmp = mem1[SPORDER+k];
            for (int j = 1; j <= SPORDER; j++) {
                tmp += mem1[SPORDER + k - j] * a10[j];
            }
            fil_mem[PMSIZE - Constants.NFRSZ + ip + k] = tmp;
        }
        if (ip == (Constants.NFRSZ - Constants.IDIM)) {
            ip = 0;
        } else {
            ip += Constants.IDIM;
        }
    }

    /** */
    void postfilter(float[] input, int inofs, float[] output, int outofs) {

        // Output of long term filter
        float[] temp = new float[Constants.IDIM];
        // Input of short term filter
        float[] temp2 = new float[Constants.IDIM];
        // Gain of filtered output
        float new_gain;
        // Gain of input
        float input_gain;
        // Scaling factor for gain preservation
        float scale;

        // Smoother version of scale
        float scalefil = 1.0f;

        longterm(input, inofs, temp, 0);
        shortterm(temp, temp2);

        // Computed scale for gain preservation

        new_gain = vec_abs(temp2, 0);
        if (new_gain > 1.0) {
            input_gain = vec_abs(input, inofs);
            scale = input_gain / new_gain;
        } else {
            scale = 1.0f;
        }

        // Smooth out scale, then scale the output

        for (int i = 0; i < Constants.IDIM; i++) {
            scalefil = Constants.AGCFAC * scalefil + (1.0f - Constants.AGCFAC) * scale;
            output[outofs + i] = scalefil * temp2[i];
        }
    }

    /** */
    private void longterm(float[] input, int inofs, float[] output, int outofs) {

        float[] lmemory = new float[Constants.KPMAX];

        // Add weighted pitch_period-delayed signal

        for (int i = 0; i < Constants.IDIM; i++) {
            float out = pitch_tap * lmemory[Constants.KPMAX + i - pitch_period];
            out += input[inofs + i];
            output[outofs + i] = pitch_gain * out;
        }

        // Shift-in input to lmemory

        for (int i = 0; i < Constants.KPMAX - Constants.IDIM; i++) {
            lmemory[i] = lmemory[i + Constants.IDIM];
        }
        System.arraycopy(input, inofs + 0, lmemory, 135, Constants.IDIM);
    }

    /**
     * Again, memories (shpmem, shzmem) are in reverse order,
     * i.e. [0] is the oldest.
     */
    private void shortterm(float[] input, float[] output) {

        float[] shpmem = new float[SPORDER];
        float[] shzmem = new float[SPORDER];

        for (int k = 0; k < Constants.IDIM; k++) {

            // FIR Part
            float in = input[k];
            float out = in;
            for (int j = SPORDER - 1; j >= 1; j--) {
                out += shzmem[j] * shzcoef[j + 1];
                shzmem[j] = shzmem[j - 1];
            }
            out += shzmem[0] * shzcoef[1];
            shzmem[0] = in;

            // IIR Part
            for (int j = SPORDER - 1; j >= 1; j--) {
                out -= shpmem[j] * shpcoef[j + 1];
                shpmem[j] = shpmem[j - 1];
            }
            out -= shpmem[0] * shpcoef[1];
            shpmem[0] = out;
            output[k] = out+tiltz*shpmem[1];
        }
    }

    /**
     * Postfilter Adapter
     */
    void psf_adapter(float[] frame) {

        pitch_period = extract_pitch();

        // Compute Pitch Tap
        float corr = 0.0f;
        float corr_per = 0.0f;
        // Shift old memory
        for (int i = 0; i < SHIFTSZ; i++) {
            tap_mem[i] = tap_mem[i + Constants.NFRSZ];
        }
        // Shift new frame into memory
        System.arraycopy(frame, 0, tap_mem, 225, Constants.NFRSZ);

        for (int i = Constants.KPMAX - pitch_period;
             i < (Constants.KPMAX - pitch_period + Constants.NPWSZ);
             i++) {

            corr     += tap_mem[i] * tap_mem[i];
            corr_per += tap_mem[i] * tap_mem[i + pitch_period];
        }
        if ((corr < EPSILON) && (corr > -EPSILON)) {
            pitch_tap = 0.0f;
        } else {
            pitch_tap = corr_per / corr;
        }

        // Compute Long Term Coefficients

        if (pitch_tap > 1) {
            pitch_tap = 1.0f;
        }
        if (pitch_tap < Constants.PPFTH) {
            pitch_tap = 0.0f;
        }
        pitch_tap = Constants.PPFZCF * pitch_tap;
        pitch_gain = 1.0f / (1.0f + pitch_tap);
    }

    /** Compute Short Term Coefficients */
    void compute_sh_coeff() {
        for (int i = 1; i <= SPORDER; i++) {
            shzcoef[i] = shzscale[i] * a10[i];
            shpcoef[i] = shpscale[i] * a10[i];
        }
        tiltz = Constants.TILTF * k10;
    }

    /** */
    private static int best_period(float[] buffer,
                                   int buflen,
                                   int pmin,
                                   int pmax) {
        int best_per = -1;
        float best_corr = -Constants.BIG;
        for (int per = pmin; per < pmax; per++) {
            float corr = 0.0f;
            for (int i = pmax; i < buflen; i++) {
                corr += buffer[i] * buffer[i - per];
            }
            if (corr > best_corr) {
                best_corr = corr;
                best_per  = per;
            }
        }
        return best_per;
    }

    /** size of decimated frame */
    private static final int DCFRSZ = Constants.NFRSZ / DECIM;

    /** */
    private int extract_pitch() {
        // Best Period (undecimated)
        int best_per = Constants.KPMAX;
        // Best Decimated Period
        int best_dper = Constants.KPMAX / DECIM;
        // Best Old Period
        int best_old_per = Constants.KPMAX;
        // Limits for search of best period
        int permin;
        int permax;
        float best_corr = -Constants.BIG;
        float best_old_corr = -Constants.BIG;
        float tap0 = 0.0f;
        float tap1 = 0.0f;
        int old_per = (Constants.KPMIN + Constants.KPMAX) >> 1;
        float[] fil_decim_mem = new float[PDMSIZE];
        float[] fil_out_mem = new float[Constants.NFRSZ + DECIM];

        // Shift decimated filtered output
//Debug.println("DCFRSZ: " + DCFRSZ + ", PDMSIZE: " + PDMSIZE);
        for (int i = DCFRSZ; i < PDMSIZE; i++) {
// Debug.println("fil_decim_mem: " + (i - DCFRSZ) + "/" + fil_decim_mem.length);
// Debug.println("fil_out_mem: " + i + "/" + fil_out_mem.length);
            fil_decim_mem[i - DCFRSZ] = fil_decim_mem[i];
        }

        // Filter and  decimate  input

        int decim_phase = 0;
        int dk = 0;
        for (int k = 0; k < Constants.NFRSZ; k++) {
            float tmp;
            tmp = fil_mem[PMSIZE - Constants.NFRSZ + k] -
                A1 * fil_out_mem[2] -
                A2 * fil_out_mem[1] -
                A3 * fil_out_mem[0];
            decim_phase++;
            if (decim_phase == 4) {
                fil_decim_mem[PDMSIZE - DCFRSZ + dk] =
                    B0 * tmp +
                    B1 * fil_out_mem[2] +
                    B2 * fil_out_mem[1] +
                    B3 * fil_out_mem[0];
                decim_phase = 0;
                dk++;
            }
            fil_out_mem[0] = fil_out_mem[1];
            fil_out_mem[1] = fil_out_mem[2];
            fil_out_mem[2] = tmp;
        }

        // Find the best Correlation in decimated domain:

        best_dper = best_period(fil_decim_mem, PDMSIZE, DPERMIN, DPERMAX);

        // Now fine-tune the best correlation on undecimated  domain

        permin = best_dper * DECIM - DECIM + 1;
        permax = best_dper * DECIM + DECIM - 1;
        if (permax > Constants.KPMAX) {
            permax = Constants.KPMAX;
        }
        if (permin < Constants.KPMIN) {
            permin = Constants.KPMIN;
        }

        best_corr = -Constants.BIG;
        for (int per = permin; per <= permax; per++) {
            float corr = 0.0f;
            for (int i = 1, j = (per + 1); i <= Constants.NPWSZ; i++, j++) {
                corr += fil_mem[PMSIZE - i] * fil_mem[PMSIZE - j];
            }
            if (corr > best_corr) {
                best_corr = corr;
                best_per = per;
            }
        }

        // If we are not exceeding old period by too much, we have a float
        // period and not a multiple

        permax = old_per + Constants.KPDELTA;
        if (best_per > permax) {

            // Now compute the best period around the old period

            permin = old_per - Constants.KPDELTA;
            if (permin < Constants.KPMIN) {
                permin = Constants.KPMIN;
            }

            best_old_corr = -Constants.BIG;
            for (int per = permin; per <= permax; per++) {
                float corr = 0.0f;
                for (int i = 1, j = (per + 1); i <= Constants.NPWSZ; i++, j++) {
                    corr += fil_mem[PMSIZE - i] * fil_mem[PMSIZE - j];
                }
                if (corr > best_old_corr) {
                    best_old_corr = corr;
                    best_old_per = per;
                }
            }

            // Compute the tap

            float s0 = 0.0f;
            float s1 = 0.0f;
            for (int i = 1; i <= Constants.NPWSZ; i++) {
                s0 += fil_mem[PMSIZE - i - best_per] *
                      fil_mem[PMSIZE - i - best_per];
                s1 += fil_mem[PMSIZE - i - best_old_per] *
                      fil_mem[PMSIZE - i - best_old_per];
            }
            if (!((s0 < EPSILON) && (s0 > -EPSILON))) {
                tap0 = best_corr / s0;
            }
            if (!((s1 < EPSILON) && (s1 > -EPSILON))) {
                tap1 = best_old_corr / s1;
            }
            tap0 = CLIPP(tap0, 0.0f, 1.0f);
            tap1 = CLIPP(tap1, 0.0f, 1.0f);
            if (tap1 > Constants.TAPTH * tap0) {
                best_per = best_old_per;
            }
        }

        // Shift fil_mem

        for (int i = Constants.NFRSZ; i < PMSIZE; i++) {
            fil_mem[i - Constants.NFRSZ] = fil_mem[i];
        }

        old_per = best_per;
        return best_per;
    }

    /** */
    static void init_postfilter() {
        shzscale[0] = shpscale[0] = 1.0f;
        for (int i = 1; i <= SPORDER; i++) {
            shzscale[i] = Constants.SPFZCF * shzscale[i - 1];
            shpscale[i] = Constants.SPFPCF * shpscale[i - 1];
        }
    }
}
