/*
 *                            LD-CELP  G.728
 *
 *    Low-Delay Code Excitation Linear Prediction speech compression.
 *
 *    Code edited by Michael Concannon.
 *    Based on code written by Alex Zatsman, Analog Devices 1993
 */

package vavi.sound.ldcelp;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.util.Arrays;

import vavi.io.LittleEndianDataInputStream;
import vavi.io.LittleEndianDataOutputStream;

import static java.lang.System.getLogger;
import static vavi.sound.ldcelp.Constants.BIG;
import static vavi.sound.ldcelp.Constants.FAC;
import static vavi.sound.ldcelp.Constants.GOFF;
import static vavi.sound.ldcelp.Constants.IDIM;
import static vavi.sound.ldcelp.Constants.KPMIN;
import static vavi.sound.ldcelp.Constants.LPC;
import static vavi.sound.ldcelp.Constants.LPCLG;
import static vavi.sound.ldcelp.Constants.LPCW;
import static vavi.sound.ldcelp.Constants.MAX;
import static vavi.sound.ldcelp.Constants.MIN;
import static vavi.sound.ldcelp.Constants.NCWD;
import static vavi.sound.ldcelp.Constants.NFRSZ;
import static vavi.sound.ldcelp.Constants.NONR;
import static vavi.sound.ldcelp.Constants.NONRLG;
import static vavi.sound.ldcelp.Constants.NONRW;
import static vavi.sound.ldcelp.Constants.NUPDATE;
import static vavi.sound.ldcelp.Constants.WNCF;


/**
 * Low-Delay Code Excitation Linear Prediction speech compression.
 *
 * @author Michael Concannon
 * @author Alex Zatsman, Analog Devices 1993
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 040621 nsano initial version <br>
 */
public class LdCelp {

    private static final Logger logger = getLogger(LdCelp.class.getName());

//#region Adapters

    // Adapter for Perceptual Weighting Filter

    /** Arrays for band widening: zeros and */
    private final float[] pwf_z_vec = new float[LPCW + 1];
    /** poles */
    private final float[] pwf_p_vec = new float[LPCW + 1];
    private final float[] pwf_old_input = new float[LPCW + NFRSZ + NONRW];
    /** Recursive Part */
    private final float[] pwf_rec = new float[LPCW + 1];

    // auto-correlation coefficients
    private final float[] _pwf_acorr = new float[LPCW + 1];
    private final float[] _pwf_lpcoeff = new float[LPCW + 1];
    private final float[] _pwf_temp = new float[LPCW + 1];

    /**
     * Adapter for Perceptual Weighting Filter.
     *
     * @param z_out zero coefficients
     * @param p_out pole coefficients
     */
    void pwf_adapter(float[] input, float[] z_out, float[] p_out) {

        hybwin(LPCW,    // lpsize
               NFRSZ,    // framesize
               NONRW,    // nrsize -- nonrecursive size
               pwf_old_input,
               input,
               _pwf_acorr,
               hw_percw,
               pwf_rec,
               0.5f);
        if (levdur(_pwf_acorr, _pwf_temp, LPCW) != 0) {
            RCOPY(_pwf_temp, 0, _pwf_lpcoeff, 0, LPCW + 1);
            bw_expand2(_pwf_lpcoeff, z_out, p_out, LPCW, pwf_z_vec, pwf_p_vec);
        }
    }

    /** */
    void init_pwf_adapter(float[] z_co, float[] p_co) {

        float zv = 1.0f;
        float pv = 1.0f;

        for (int i = 0; i <= LPCW; i++) {
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

    // Backward Synthesis Filter Adapter ----

    private final float[] facv = new float[LPC + 1];

    private final float[] bsf_old_input = new float[LPC + NFRSZ + NONR];
    private final float[] bsf_rec = new float[LPC + 1];

    private final float[] _bsf_old_input = new float[LPC + NFRSZ + NONR];
    // auto-correlation coefficients
    private final float[] _bsf_acorr = new float[LPC + 1];
    private final float[] _bsf_lpcoeff = new float[LPC + 1];
    private final float[] _bsf_temp = new float[LPC + 1];

    /** Backward Synthesis Filter Adapter */
    void bsf_adapter(float[] input, float[] p_out) {

        hybwin(LPC,       // lpsize
               NFRSZ,     // framesize
               NONR,      // nrsize -- non-recursive size
                _bsf_old_input,
               input,
                _bsf_acorr,
               hw_synth,
               bsf_rec,
               0.75f);

        if (sf_levdur(_bsf_acorr, _bsf_temp) != 0) {
            k10 = -_bsf_acorr[1] / _bsf_acorr[0];
            RCOPY(_bsf_temp, 0, _bsf_lpcoeff, 0, LPC + 1);
            bw_expand1(_bsf_lpcoeff, p_out, LPC, facv);
        }
    }

    // Gain Adapter ----

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
    private final float[] g_rec = new float[LPCLG + 1];
    private final float[] g_old_input = new float[LPCLG + NUPDATE + NONRLG];

    // auto-correlation coefficients
    private final float[] _gain_acorr = new float[LPCLG + 1];
    private final float[] _gain_lpcoeff = new float[LPCLG + 1];

    private final float[] _gain_temp = new float[LPCLG + 1];

    /** recompute lpc_coeff */
    void gain_adapter(float[] log_gain, float[] coeff) {

        hybwin(LPCLG,     // lpsize
               NUPDATE,   // framesize
               NONRLG,    // nrsize -- nonrecursive size
               g_old_input,
               log_gain,
                _gain_acorr,
               hw_gain,
               g_rec,
               0.75f);

        if (levdur(_gain_acorr, _gain_temp, LPCLG) != 0) {
            System.arraycopy(_gain_temp, 1, _gain_lpcoeff, 1, LPCLG);
            bw_expand1(_gain_lpcoeff, coeff, LPCLG, gain_p_vec);
        }
    }

    // Initializations ----

    /** */
    void init_bsf_adapter(float[] co) {
        float v = 1.0f;

        for (int i = 0; i <= LPC; i++) {
            facv[i] = v;
            v *= FAC;
            co[i] = 0;
        }
        co[0] = 1.0f;
        ZARR(bsf_old_input);
        ZARR(bsf_rec);
    }

    /** */
    void init_gain_adapter(float[] coeff) {
        gain_p_vec[0] = 1.0f;
        coeff[0] = 1.0f;
        coeff[1] = -1.0f;
        for(int i = 0; i < LPCLG + NUPDATE + NONRLG; i++) {
            g_old_input[i] = -GOFF;
        }
        ZARR(g_rec);
        ZARR(g_old_input);
    }

    // Hybrid Window Module ----

    /**
     * Hybrid Window
     *
     * @param lpsize size of OUTPUT (auto-correlation vector)
     * @param framesize size of NEW_INPUT
     * @param nrsize size of non-recursive part.
     * @param old_input buffer for holding old input (size LPSIZE + FRAMESIZE + NRSIZE)
     * @param new_input new input, or frame (size FRAMESIZE)
     * @param output auto-correlation vector (size LPSIZE)
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
        float[] ws = new float[N3];
        float[] tmp1 = new float[lpsize + 1];
        float[] tmp2 = new float[lpsize + 1];

        // shift in INPUT into OLD_INPUT and window it

        for (int i = 0; i < N2; i++) {
            old_input[i] = old_input[i + framesize];
        }
        for (int i = 0; i < framesize; i++) {
            old_input[N2 + i] = new_input[i];
        }

        VPROD(old_input, window, ws, N3);

        autocorr(ws, tmp1, lpsize, lpsize, N1);

        for (int i = 0; i <= lpsize; i++) {
            rec[i] = decay * rec[i] + tmp1[i];
        }

        autocorr(ws, tmp2, lpsize, N1, N3);

        for (int i = 0; i <= lpsize; i++) {
            output[i] = rec[i] + tmp2[i];
        }

        output[0] *= WNCF;
    }

    // Levinson-Durbin Routines ----

    private final float[] _levdur_rc = new float[20];

    /**
     * Levinson-Durbin algorithm
     * return 1 if ok, otherwise 0
     */
    private int levdur(float[] acorr, float[] coeff, int order) {
        // Local variables
        int minc2;
        float s;
        int ib, mh;
        float at;
        float alpha;
        float tmp;

        // Parameter adjustments TODO -(1)
//        --acorr;
//        --coeff;

        // check for zero signal or illegal zero-lag auto-correlation
        if ((acorr[1-(1)] <= 0.0f) || (acorr[order + 1-(1)] == 0)) {
            return 0;
        }

        // start Durbin's recursion
        _levdur_rc[1] = -acorr[2-(1)] / acorr[1-(1)];
        coeff[1-(1)] = 1.0f;
        coeff[2-(1)] = _levdur_rc[1];
        alpha = acorr[1-(1)] + acorr[2-(1)] * _levdur_rc[1];
        if (alpha <= 0.0f) {
            return 0;
        }
        for (int minc = 2; minc <= order; ++minc) {
            minc2 = minc + 2;
            s = acorr[minc + 1-(1)];
            for (int ip = 2; ip <= minc; ++ip) {
                s += acorr[minc2 - ip-(1)] * coeff[ip-(1)];
            }
            _levdur_rc[minc] = -s / alpha;
            mh = minc / 2 + 1;
            for (int ip = 2; ip <= mh; ip++) {
                ib = minc2 - ip;
                at = _levdur_rc[minc] * coeff[ib-(1)];
                at += coeff[ip-(1)];
                tmp = _levdur_rc[minc] * coeff[ip-(1)];
                coeff[ib-(1)] += tmp;
                coeff[ip-(1)] = at;
            }
            coeff[minc + 1-(1)] = _levdur_rc[minc];
            alpha += _levdur_rc[minc] * s;

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

        if (acorr[LPC] == 0) {
            return 0;
        }
        float E = acorr[0];
        if (E <= 0) {
            return 0;
        }
        coeff[0] = 1.0f;
        for (int m = 1; m <= LPC; m++) {
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
                int p = 1; // coeff
                int pp = p; // coeff
                int q = m - 1; // coeff
                int qq = q; // coeff

                float x = coeff[p++];
                float y = coeff[q--];
                float t1 = K * x;
                float t2 = K * y;
                for (int j = 2; j <= halfm; j++) {
                    t4 = t2 + x; x = coeff[p++];
                    t3 = t1 + y; y = coeff[q--];
                    t1 = K * x;    coeff[pp++] = t4;
                    t2 = K * y;    coeff[qq--] = t3;
                }
                t3 = t1 + y;
                t4 = t2 + x;
                coeff[pp] = t4;
                coeff[qq] = t3;
            }
            if (m == 10) {
                System.arraycopy(coeff, 0, a10, 0, 10 + 1);
            }
            E = (1 - K * K) * E;
            if (E < 0) {
                return 0;
            }
        }
        return 1;
    }

    // Band Width Expanders ----

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

    /**
     * Poles only
     *
     * @param p_vec 1st vector
     * @param input 2nd vector
     * @param order length
     * @param p_out result
     */
    private static void bw_expand1(float[] input,
                                   float[] p_out,
                                   int order,
                                   float[] p_vec) {

        for (int i = 1; i <= order; i++) {
            p_out[i] = p_vec[i] * input[i];
        }
    }

    /**
     * @param x vector
     * @param m from
     * @param n to
     * @param k vector dimension
     * @param r result
     */
    private static void autocorr(float[] x, float[] r, int k, int m, int n) {

        for (int ii = 0; ii <= k; ii++) {
            r[ii] = 0;
            for (int jj = m; jj < n; jj++) {
                float tmp = x[jj] * x[jj - ii];
                r[ii] += tmp;
            }
        }
    }

//#endregion

//#region CMain

    /** Index of the end of the decoded speech */
    private int dec_end;

    /**
     * @param args -d[p]|-e infile outfile
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            usage();
        }

        LdCelp ldCelp = new LdCelp();
        if (args[0].equals("-e")) {
            String in_file_name = args[1];
            String x_file_name = args[2];
            // output file (codebook indices)
            var eOut = new LittleEndianDataOutputStream(new FileOutputStream(x_file_name));
            // input file
            var eIn = new LittleEndianDataInputStream(new FileInputStream(in_file_name));
            ldCelp.init_encoder();
            short[] in = new short[5];
            short[] out = new short[1];
            while (true) {
                try {
                    for (int i = 0; i < in.length; i++)
                        in[i] = eIn.readShort();
                    ldCelp.encoder(in, out);
                    for (short s : out)
                        eOut.writeShort(s);
                } catch (EOFException e) {
                    break;
                }
            }
            eIn.close();
            eOut.flush();
            eOut.close();
        } else if (args[0].startsWith("-d")) {
            if (args[0].length() > 2 && args[0].charAt(2) == 'p') {
                ldCelp.postfiltering_p = true;
            } else {
                ldCelp.postfiltering_p = false;
            }
            String x_file_name = args[1];
            String out_file_name = args[2];
            // Output file
            var dOut = new LittleEndianDataOutputStream(new FileOutputStream(out_file_name));
            // Input file (codebook indices)
            var dIn = new LittleEndianDataInputStream(new FileInputStream(x_file_name));
            ldCelp.init_decoder();
            short[] in = new short[1];
            short[] out = new short[5];
            while (true) {
                try {
                    for (int i = 0; i < in.length; i++)
                        in[0] = dIn.readShort();
                    ldCelp.decoder(in, out);
                    for (short s : out)
                        dOut.writeShort(s);
                } catch (EOFException e) {
                    break;
                }
            }
            dIn.close();
            dOut.flush();
            dOut.close();
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
    private int vector_end; // thequeue

    /** */
    void encoder(short[] in, short[] out) throws IOException {
        for (int vnum = 0; vnum < (in.length + IDIM - 1) / IDIM;  vnum++) {
            int n = vnum % IDIM == 0 ? IDIM : vnum % IDIM;
            read_sound_buffer(n, in, vnum * IDIM, thequeue, (vnum * IDIM) % QSIZE);
            vector_end = (vnum * IDIM) % QSIZE + n; // thequeue
            encode_vector(false, out, vnum);
            adapt_frame();
        }
    }

    /** */
    private void init_encoder() {
        this.ffase = 1;
        //
        init_pwf_adapter(pwf_z_coeff, pwf_p_coeff);
        _next[PWF_Z_COEFF][0] = _next[PWF_P_COEFF][0] = 1.0f;
        _obsolete_p[PWF_Z_COEFF] = false;
        init_bsf_adapter(sf_coeff);
        _next[SF_COEFF][0] = 1.0f;
        _obsolete_p[SF_COEFF] = false;
        init_gain_adapter(gp_coeff);
        init_gain_buf();
        _next[GP_COEFF][0] = 1.0f;
        _next[GP_COEFF][1] = -1.0f;
        _obsolete_p[GP_COEFF] = false;
        vector_end = 0; // thequeue
        ZARR(imp_resp);
        imp_resp[0] = 1.0f;
        shape_conv(imp_resp, shape_energy);
        //
        Arrays.fill(thequeue, 0);
    }

    private final float[] _encode_vector_zero_response = new float[IDIM];
    private final float[] _encode_vector_weighted_speech = new float[IDIM];
    private final float[] _encode_vector_target = new float[IDIM];
    private final float[] _encode_vector_normtarg = new float[IDIM];
    private final float[] _encode_vector_cb_vec = new float[IDIM];
    private final float[] _encode_vector_pn = new float[IDIM];
    private float _encode_vector_gain = 1.0f;
    private float _encode_vector_scale = 1.0f;

    /** */
    private void encode_vector(boolean ignore, short[] out, int outp) {

        // recently read vector in the queue
        int vector = vector_end - IDIM;
        if (vector < 0) {
            vector += QSIZE;
        }
        // Index of Recently Read Vector
        int vx = vector;
        UPDATE(pwf_z_coeff, PWF_Z_COEFF);    // Copy new coeff if flag set
        UPDATE(pwf_p_coeff, PWF_P_COEFF);
        pwfilter2(thequeue, vector, _encode_vector_weighted_speech);
        UPDATE(sf_coeff, SF_COEFF);
        zresp(_encode_vector_zero_response);
        sub_sig(_encode_vector_weighted_speech, _encode_vector_zero_response, _encode_vector_target);
        UPDATE(gp_coeff, GP_COEFF);
        _encode_vector_gain = predict_gain();
        _encode_vector_scale = 1.0f / _encode_vector_gain;
        sig_scale(_encode_vector_scale, _encode_vector_target, 0, _encode_vector_normtarg, 0);
        UPDATE(imp_resp, IMP_RESP);
        trev_conv(imp_resp, _encode_vector_normtarg, _encode_vector_pn);
        UPDATE(shape_energy, SHAPE_ENERGY);
        // Computed Codebook Index
        int ix = cb_index(_encode_vector_pn);
        out[outp] = (short) ix;
        cb_excitation(ix, _encode_vector_cb_vec);
        sig_scale(_encode_vector_gain, _encode_vector_cb_vec, 0, qspeech, vx);
        // Logarithmic Gain Index
        int lgx = vx / IDIM;
        update_gain(qspeech, vx, log_gains, lgx);
        mem_update(qspeech, vx, synspeech, vx);
        dec_end = vx + IDIM;
        if (dec_end >= QSIZE) {
            dec_end -= QSIZE;
        }
        // declare array and its copy together with a semaphore
        NEXT_FFASE(); // Update vector counter
    }

    private final float[] _adapt_frame_input = new float[NUPDATE * IDIM];
    private final float[] _adapt_frame_synth = new float[NUPDATE * IDIM];
    private final float[] _adapt_frame_lg = new float[NUPDATE];

    /**
     * Update the filter coeff if we are at the correct vector in the frame
     * ffase is the vector count (1-4) within the current frame
     */
    void adapt_frame() {

        // Backward syn. filter coeff update.  Occurs after full frame (before
        // first vector) but not used until the third vector of the frame
        if (ffase == 1) {
            CIRCOPY(_adapt_frame_synth, synspeech, dec_end, NUPDATE * IDIM, QSIZE);
            bsf_adapter(_adapt_frame_synth, _next[SF_COEFF]); // Compute then new coeff
        }

        // Before third vector of frame
        if (ffase == 3) {
            // Copy coeff computed above(2 frames later)
            _obsolete_p[SF_COEFF] = true;
        }

        // Gain coeff update before second vector of frame
        if (ffase == 2) {
            // Index for log_gains, cycle end
            int gx = dec_end / IDIM;
            CIRCOPY(_adapt_frame_lg, log_gains, gx, NUPDATE, QSIZE / IDIM);
            gain_adapter(_adapt_frame_lg, _next[GP_COEFF]);
            _obsolete_p[GP_COEFF] = true;
        }

        if (ffase == 3) {
            CIRCOPY(_adapt_frame_input, thequeue, dec_end, NUPDATE * IDIM, QSIZE);
            pwf_adapter(_adapt_frame_input, _next[PWF_Z_COEFF], _next[PWF_P_COEFF]);
            _obsolete_p[PWF_Z_COEFF] = true;
            _obsolete_p[PWF_P_COEFF] = true;
        }

        if (ffase == 3) {
            iresp_vcalc(_next[SF_COEFF], _next[PWF_Z_COEFF], _next[PWF_P_COEFF], _next[IMP_RESP]);
            shape_conv(_next[IMP_RESP], _next[SHAPE_ENERGY]);
            _obsolete_p[SHAPE_ENERGY] = true;
            _obsolete_p[IMP_RESP] = true;
        }
    }

//#endregion

//#region CodeBook

    private final float[] _iresp_vcalc_temp = new float[IDIM];
    private final float[] _iresp_vcalc_rc = new float[IDIM];

    /** Impulse Response Vector Calculator */
    void iresp_vcalc(float[] sf_co,
                            float[] pwf_z_co,
                            float[] pwf_p_co,
                            float[] h) {

        _iresp_vcalc_temp[0] = _iresp_vcalc_rc[0] = 1.0f;
        for (int k = 1; k < IDIM; k++) {
            float a0 = 0.0f;
            float a1 = 0.0f;
            float a2 = 0.0f;
            for (int i = k; i >= 1; i--) {
                _iresp_vcalc_temp[i] = _iresp_vcalc_temp[i-1];
                _iresp_vcalc_rc[i] = _iresp_vcalc_rc[i-1];
                a0 -= sf_co[i] * _iresp_vcalc_temp[i];
                a1 += pwf_z_co[i] * _iresp_vcalc_temp[i];
                a2 -= pwf_p_co[i] * _iresp_vcalc_rc[i];
            }
            _iresp_vcalc_temp[0] = a0;
            _iresp_vcalc_rc[0] = a0 + a1 + a2;
        }
        for (int k = 0; k < IDIM; k++) {
            h[k] = _iresp_vcalc_rc[IDIM - 1 - k];
        }
    }

    /**
     * Cb_shape Codevector Convolution Module and Energy Table Calculator
     * The output is energy table
     *
     * Unoptimized version -- kept for reference
     */
    static void shape_conv(float[] h, float[] shen) {

        float h0 = h[0];
        float h1 = h[1];
        float h2 = h[2];
        float h3 = h[3];
        float h4 = h[4];

        for (int j = 0; j < NCWD; j++) {
            float energy = 0;
            float tmp = h0 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][1] + h1 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][2] + h1 * cb_shape[j][1] + h2 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][3] + h1 * cb_shape[j][2] + h2 * cb_shape[j][1] +
                  h3 * cb_shape[j][0];
            energy += tmp * tmp;
            tmp = h0 * cb_shape[j][4] + h1 * cb_shape[j][3] + h2 * cb_shape[j][2] +
                  h3 * cb_shape[j][1] + h4 * cb_shape[j][0];
            energy += tmp * tmp;
            shen[j] = energy;
        }
    }

    /** Time Reversed Convolution Module -- Block 13 */
    static void trev_conv(float[] h, float[] target, float[] pn) {

        for (int k = 0; k < IDIM; k++) {
            float tmp = 0.0f;
            for (int j = k; j < IDIM; j++) {
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
        float gain = cb_gain[gx];
        for (int i = 0; i < IDIM; i++) {
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

        float distm = BIG;
        // best shape index
        int is = 0;
        // best gain index
        int ig = 0;

        int shape_ptr = 0; // cb_shape
        int sher_ptr = 0; // shape_energy
        int pb = 0; // pn
        float cgm0 = cb_gain_mid_0;
        float cgm1 = cb_gain_mid_1;
        float cgm2 = cb_gain_mid_2;

        final int minus5 = -5;

        for (int j = 0; j < NCWD; j++) {
            float cor = 0.0f;
            float energy = shape_energy[sher_ptr++];

            float b0 = cgm0 * energy;
            float x = cb_shape[shape_ptr / 5][shape_ptr % 5];
            shape_ptr++;
            float y = pn[pb++];
            float t = x * y;
            x = cb_shape[shape_ptr / 5][shape_ptr % 5];
            shape_ptr++;
            y = pn[pb++];
            cor += t;
            t = x * y;
            x = cb_shape[shape_ptr / 5][shape_ptr % 5];
            shape_ptr++;
            y = pn[pb++];
            cor += t;
            t = x * y;
            x = cb_shape[shape_ptr / 5][shape_ptr % 5];
            shape_ptr++;
            y = pn[pb++];
            cor += t;
            t = x * y;
            x = cb_shape[shape_ptr / 5][shape_ptr % 5];
            shape_ptr++;
            y = pn[pb++];
            cor += t;
            t = x * y;
            cor += t;
            float b1 = cgm1 * energy;

            pb += minus5;
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

    /**
     * @param X dst
     * @param Y src
     */
    private static void RCOPY(float[] X, int xofs, float[] Y, int yofs, int N) {
        System.arraycopy(X, xofs, Y, yofs, N);
    }

    /** */
    private static final float EPSILON = 1.0e-35f;

    // Use hand-pipelined loops for higher speed on 21000

    /** */
    private static float CLIPP(float X, float LOW, float HIGH) {
        return X < LOW ? LOW : Math.min(X, HIGH);
    }

    /** */
    private static void sig_scale(float scale, float[] a, int ap, float[] b, int bp) {
        for (int i = 0; i < IDIM; i++) {
            b[bp + i] = scale * a[ap + i];
        }
    }

    /** */
    private static void sub_sig(float[] A, float[] B, float[] C) {
        for (int i = 0; i < IDIM; i++) {
            C[i] = A[i] - B[i];
        }
    }

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
        if (_obsolete_p[name]) {
            for (int i = YYY.length - 1; i >= 0; i--) {
                YYY[i] = _next[name][i];
            }
            _obsolete_p[name] = false;
        }
    }

    /**
     * Copy L words to X from circular buffer CIRC *ending* at offset EOS.
     * CL is the size of circular buffer CIRC
     *
     * @param X dst
     * @param CIRC circular buffer
     * @param EOS offset
     * @param L length
     * @param CL size of circular buffer
     */
    private static void CIRCOPY(float[] X, float[] CIRC, int EOS, int L, int CL) {
        int i1;
        int i2;
        int lx = 0;
        if (EOS >= L) {
            i1 = EOS - L;
            i2 = CL;
        } else {
            i1 = 0;
            i2 = CL + EOS - L;
        }
        for (int i = i2; i < CL; i++) {
            X[lx++] = CIRC[i];
        }
        for (int i = i1; i < EOS; i++) {
            X[lx++] = CIRC[i];
        }
    }

    synchronized void NEXT_FFASE() { ffase = ffase == 4 ? 1 : ffase + 1; }

//#endregion

//#region DMain

    /** */
    boolean postfiltering_p = false;

    /** Index of the start of the decoded speech vector */
    private int d_vec_start;
    /** Index of the end   of the decoded speech vector */
    private int d_vec_end;
    /** Index of the start of vector being written */
    private int w_vec_start;
    /** Index of the end   of vector being written */
    private int w_vec_end;

    /** */
    void decoder(short[] in, short[] out) {
        assert in.length < QSIZE / IDIM;
        for (int inp = 0; inp < in.length; inp++) {
            if (w_vec_start >= QSIZE) {
                w_vec_start = 0;
            }
            w_vec_end = w_vec_start;
            vector_end = w_vec_end;
            decode_vector(false, in[inp]);
            w_vec_end = w_vec_start + IDIM;
            if (w_vec_end >= QSIZE) {
                w_vec_end = 0;
            }
            write_sound_buffer(IDIM, thequeue, w_vec_end, out, inp * IDIM);
            adapt_decoder();

            w_vec_start += IDIM;
        }
    }

    /** */
    void init_decoder() {
        this.ffase = 1;
        //
        init_bsf_adapter(sf_coeff);
        _next[SF_COEFF][0] = 1.0f;
        _obsolete_p[SF_COEFF] = false;
        init_gain_adapter(gp_coeff);
        init_gain_buf();
        _next[GP_COEFF][0] = 1.0f;
        _next[GP_COEFF][1] = -1.0f;
        _obsolete_p[GP_COEFF] = false;
        init_output();
        vector_end = 0; // thequeue
        //
        Arrays.fill(thequeue, 0);
        w_vec_start = 0;
    }

    private final float[] _decode_vector_zero_response = new float[IDIM];
    private final float[] _decode_vector_cb_vec = new float[IDIM];
    private final float[] _decode_vector_pf_speech = new float[IDIM];
    private float _decode_vector_gain = 1.0f;

    /** @param ix Computed Codebook Index */
    private void decode_vector(boolean ignore, int ix) {
        float[] qs = new float[NUPDATE * IDIM];

        w_vec_end = vector_end;
        d_vec_start = w_vec_end + IDIM;
        if (d_vec_start >= QSIZE) {
            d_vec_start -= QSIZE;
        }

        UPDATE(sf_coeff, SF_COEFF);
        zresp(_decode_vector_zero_response);
        cb_excitation(ix, _decode_vector_cb_vec);
        UPDATE(gp_coeff, GP_COEFF);
        _decode_vector_gain = predict_gain();
        sig_scale(_decode_vector_gain, _decode_vector_cb_vec, 0, qspeech, d_vec_start);
        // Log Gains INdex
        int lgx = d_vec_start / IDIM;
        update_gain(qspeech, d_vec_start, log_gains, lgx);
        mem_update(qspeech, d_vec_start, synspeech, d_vec_start);
        d_vec_end = d_vec_start + IDIM;
        if (d_vec_end >= QSIZE) {
            d_vec_end -= QSIZE;
        }
        if (postfiltering_p) {
            inv_filter(synspeech, d_vec_start);
            if (ffase == 3) {
              CIRCOPY(qs, synspeech, d_vec_end, NUPDATE * IDIM, QSIZE);
              psf_adapter(qs);
            }
            if (ffase == 1) {
                compute_sh_coeff();
            }
            postfilter(synspeech, d_vec_start, _decode_vector_pf_speech, 0);
            RCOPY(_decode_vector_pf_speech, 0, thequeue, d_vec_start, IDIM);
        } else {
            RCOPY(synspeech, d_vec_start, thequeue, d_vec_start, IDIM);
        }
        // declare array and its copy together with a semaphore
        NEXT_FFASE();
    }

    /** */
    void adapt_decoder() {
        float[] synth = new float[NUPDATE * IDIM];
        float[] lg = new float[NUPDATE];

        if (ffase == 1) {
            CIRCOPY(synth, synspeech, d_vec_end, NUPDATE * IDIM, QSIZE);
            bsf_adapter(synth, _next[SF_COEFF]);
        }
        if (ffase == 2) {
            // gain index
            int gx = d_vec_end / IDIM;
            CIRCOPY(lg, log_gains, gx, NUPDATE, QSIZE / IDIM);
            gain_adapter(lg, _next[GP_COEFF]);
            _obsolete_p[GP_COEFF] = true;
        }
        if (ffase == 3) {
            _obsolete_p[SF_COEFF] = true;
        }
    }

//#endregion

//#region Data

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

//#endregion

//#region Fast

    /**
     * Z[i] = X[i] * Y[i]
     *
     * @param X 1st vector
     * @param Y 2nd vector
     * @param N vector dimension
     * @param Z product
     */
    static void VPROD(float[] X, float[] Y, float[] Z, int N) {
        for (int i = 0; i < N; i++) {
            Z[i] = X[i] * Y[i];
        }
    }

    /** */
    static void RSHIFT(float[] START, int LENGTH, int SHIFT) {
        int from = LENGTH - 1;
        int to = LENGTH + SHIFT - 1;
        for (int i = 0; i < LENGTH; i++) {
            START[to--] = START[from--];
        }
    }

    /**
     * Must have at least two elements, i.e. N >= 2.
     * computes sum(x[i] * y[i])
     */
    static float DOTPROD(float[] X, int xp, float[] Y, int yp, int N) {
        float sum = 0;
        for (int i = 0; i < N; i++) {
            sum += X[xp + i] * Y[yp + i];
        }
        return sum;
    }

//#endregion

//#region Filters

    /** */
    private final float[] firmem = new float[LPCW + IDIM];
    /** */
    private final float[] iirmem = new float[LPCW + IDIM];

    /** */
    void pwfilter2(float[] input, int ip, float[] output) {

        RSHIFT(firmem, LPCW, IDIM);
        for (int k = 0; k < IDIM; k++) {
            firmem[k] = input[ip + IDIM - 1 - k];
        }
        RSHIFT(iirmem, LPCW, IDIM);

        for (int k = 0; k < IDIM; k++) {
            // pwf_z_coeff[0] is always 1.0
            float out = firmem[IDIM - 1 - k];
            out += DOTPROD(firmem, IDIM - k, pwf_z_coeff, 1, LPCW);
            out -= DOTPROD(iirmem, IDIM - k, pwf_p_coeff, 1, LPCW);
            iirmem[IDIM - 1 - k] = out;
            output[k] = out;
        }
    }

    // Synthesis and Perceptual Weighting Filter.

    /** */
    final float[] statelpc = new float[LPC + IDIM];
    /** */
    final float[] zirwfir = new float[LPCW];
    /** */
    final float[] zirwiir = new float[LPCW];

    /** Updateable coefficients */
    void sf_zresp(float[] output) {

        // This is un-pipelined version of the above. Kept for reference
        for (int j = LPC - 1; j >= 0; j--) {
            statelpc[j + IDIM] = statelpc[j];
        }
        for (int k = 0; k < IDIM; k++) {
            float out = 0.0f;
            float sj = statelpc[LPC + IDIM - k - 1];
            float aj = sf_coeff[LPC];
            for (int j = LPC - 2; j >= 1; j--) {
                out -= sj * aj;
                sj = statelpc[IDIM - k + j];
                aj = sf_coeff[j + 1];
            }
            output[k] = out - sj * aj - statelpc[IDIM - k] * sf_coeff[1];
            statelpc[IDIM - 1 - k] = output[k];
        }
    }

    /** */
    void pwf_zresp(float[] input, float[] output) {

        // Un-pipelined version, kept for reference
        for (int k = 0; k < IDIM; k++) {
            float tmp = input[k];
            for (int j = LPCW - 1; j >= 1; j--) {
                input[k] += zirwfir[j] * pwf_z_coeff[j + 1];
                zirwfir[j] = zirwfir[j - 1];
            }
            input[k] += zirwfir[0] * pwf_z_coeff[1];
            zirwfir[0] = tmp;
            for (int j = LPCW - 1; j >= 1; j--) {
                input[k] -= zirwiir[j] * pwf_p_coeff[j + 1];
                zirwiir[j] = zirwiir[j - 1];
            }
            output[k] = input[k] - zirwiir[0] * pwf_p_coeff[1];
            zirwiir[0] = output[k];
        }
    }

    /** */
    void zresp(float[] output) {
        float[] temp = new float[IDIM];
        sf_zresp(temp);
        pwf_zresp(temp, output);
    }

    void mem_update(float[] input, int inp, float[] output, int outp) {
        float[] temp = new float[IDIM];
        int t2 = 0; // zirwfir
        zirwfir[t2] = temp[0] = input[inp];
        for (int k = 1; k < IDIM; k++) {
            float a0 = input[inp + k];
            float a1 = 0.0f;
            float a2 = 0.0f;
            for (int i = k; i >= 1; i--) {
                zirwfir[t2 + i] = zirwfir[t2 + i - 1];
                temp[i] = temp[i - 1];
                a0 -= sf_coeff[i] * zirwfir[t2 + i];
                a1 += pwf_z_coeff[i] * zirwfir[t2 + i];
                a2 -= pwf_p_coeff[i] * temp[i];
            }
            zirwfir[t2] = a0;
            temp[0] = a0 + a1 + a2;
        }

        for (int k = 0; k < IDIM; k++) {
            statelpc[k] += zirwfir[t2 + k];
            if (statelpc[k] > MAX) {
                statelpc[k] = MAX;
            }
            else if (statelpc[k] < MIN) {
                statelpc[k] = MIN;
            }
            zirwiir[k] += temp[k];
        }
        System.arraycopy(statelpc, 0, zirwfir, 0, LPCW);
        for (int k = 0; k < IDIM; k++) {
            output[outp + k] = statelpc[IDIM - 1 - k];
        }
    }

    // The Gain Predictor

    private final float[] gain_input = new float[LPCLG];

    /** */
    private static float log_rms(float[] input, int inp) {
        float etrms = 0.0f;
        for (int k = 0; k < IDIM; k++) {
            etrms += input[inp + k] * input[inp + k];
        }
        etrms /= IDIM;
        if (etrms < 1.0f) {
            etrms = 1.0f;
        }
        etrms = 10.0f * (float) Math.log10(etrms);
        return etrms;
    }

    /** */
    float predict_gain() {
        float new_gain = GOFF;
        float temp;

        for (int i = 1; i <= LPCLG; i++) {
            temp = gp_coeff[i] * gain_input[LPCLG - i];
            new_gain -= temp;
        }
        if (new_gain < 0.0f) {
            new_gain = 0.0f;
        }
        if (new_gain > 60.0f) {
            new_gain = 60.0f;
        }
        new_gain = (float) Math.pow(10, 0.05f * new_gain);
        return new_gain;
    }

    /** */
    void update_gain(float[] input, int inp, float[] lgp, int lgpP) {

        lgp[lgpP] = log_rms(input, inp) - GOFF;
        for (int i = 0; i < LPCLG - 1; i++) {
            gain_input[i] = gain_input[i + 1];
        }
        gain_input[LPCLG - 1] = lgp[lgpP];
    }

    /** */
    void init_gain_buf() {

        Arrays.fill(gain_input, -GOFF);
        for (int i = 0; i < QSIZE / IDIM; i++) {
            log_gains[i] = -GOFF;
        }
    }

    // Global ----

    final float[] sf_coeff = new float[LPC + 1];
    final float[] gp_coeff = new float[LPCLG + 1];
    final float[] pwf_z_coeff = new float[LPCW + 1];
    final float[] pwf_p_coeff = new float[LPCW + 1];
    final float[] shape_energy = new float[NCWD];
    final float[] imp_resp = new float[IDIM];

    final float[][] _next = new float[][] {
        new float[LPC + 1],
        new float[LPCLG + 1],
        new float[LPCW + 1],
        new float[LPCW + 1],
        new float[NCWD],
        new float[IDIM]
    };

    static final int SF_COEFF = 0;
    static final int GP_COEFF = 1;
    static final int PWF_Z_COEFF = 2;
    static final int PWF_P_COEFF = 3;
    static final int SHAPE_ENERGY = 4;
    static final int IMP_RESP = 5;

    final boolean[] _obsolete_p = new boolean[6];

    static final int QSIZE = 60;

    /** Synthesized Speech */
    final float[] synspeech = new float[QSIZE];
    /** Quantized  Speech */
    final float[] qspeech = new float[QSIZE];
    /** Logarithm of Gains */
    final float[] log_gains = new float[QSIZE / IDIM];

    volatile int ffase = -4;

    // IOSparc ----

    /** Scaling factor for input */
    float rscale = 0.125f;

    int sound_overflow = 0;

    /** for decoding */
    void init_output() {
        sound_overflow = 0;
    }

    /** Return Number of Samples Read */
    void read_sound_buffer(int n, short[] in, int inp, float[] out, int outp) {
        for (int i = 0; i < n; i++) {
            out[outp + i] = rscale * in[inp + i];
        }
    }

    /** for decoding */
    void write_sound_buffer(int n, float[] in, int inp, short[] out, int outp) {

        for (int i = 0; i < n; i++) {
            float xx = in[inp + i] / rscale;
            if (xx > 0.0) {
                if (xx > 32767.0f) {
                    xx = 32767.0f;
                } else {
                    xx += 0.5f;
                }
            } else {
                if (xx < -32768.0f) {
                    xx = -32768.0f;
                } else {
                    xx -= 0.5f;
                }
            }
            short s = (short) xx;
            out[outp + i] = s;
        }
    }

//#endregion

//#region PostFil

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
    private static final int DPERMIN = (KPMIN / DECIM);
    private static final int SHIFTSZ = (Constants.KPMAX + Constants.NPWSZ - NFRSZ + IDIM);

    /** Post-Filter Memory for syn. sp. */
    private final float[] tap_mem = new float[Constants.KPMAX + Constants.NPWSZ + IDIM];

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
        for (int i = 1; i < IDIM; i++) {
            r += Math.abs(v[offset + i]);
        }
        return r;
    }

    private int _inv_filter_ip = IDIM;
    private final float[] _inv_filter_mem1 = new float[SPORDER + NFRSZ];

    /** Inverse Filter */
    void inv_filter(float[] input, int offset) {

        // Shift in input into mem1
        for (int i = IDIM; i < SPORDER + IDIM; i++) {
            _inv_filter_mem1[i - IDIM] = _inv_filter_mem1[i];
        }
        System.arraycopy(input, offset, _inv_filter_mem1, SPORDER, IDIM);
        for (int k = 0; k < IDIM; k++) {
            float tmp = _inv_filter_mem1[SPORDER + k];
            for (int j = 1; j <= SPORDER; j++) {
                tmp += _inv_filter_mem1[SPORDER + k - j] * a10[j];
            }
            fil_mem[PMSIZE - NFRSZ + _inv_filter_ip + k] = tmp;
        }
        if (_inv_filter_ip == (NFRSZ - IDIM)) {
            _inv_filter_ip = 0;
        } else {
            _inv_filter_ip += IDIM;
        }
    }

    /** Output of long term filter */
    private final float[] _postfilter_temp = new float[IDIM];
    /** Input of short term filter */
    private final float[] _postfilter_temp2 = new float[IDIM];

    // Smoother version of scale
    float _postfilter_scalefil = 1.0f;

    /** */
    void postfilter(float[] input, int inofs, float[] output, int outofs) {
        // Gain of filtered output
        float new_gain;
        // Gain of input
        float input_gain;
        // Scaling factor for gain preservation
        float scale;

        longterm(input, inofs, _postfilter_temp, 0);
        shortterm(_postfilter_temp, _postfilter_temp2);

        // Computed scale for gain preservation

        new_gain = vec_abs(_postfilter_temp2, 0);
        if (new_gain > 1.0) {
            input_gain = vec_abs(input, inofs);
            scale = input_gain / new_gain;
        } else {
            scale = 1.0f;
        }

        // Smooth dOut scale, then scale the output

        for (int i = 0; i < IDIM; i++) {
            _postfilter_scalefil = Constants.AGCFAC * _postfilter_scalefil + (1.0f - Constants.AGCFAC) * scale;
            output[outofs + i] = _postfilter_scalefil * _postfilter_temp2[i];
        }
    }

    private final float[] _longterm_lmemory = new float[Constants.KPMAX];

    /** */
    private void longterm(float[] input, int inofs, float[] output, int outofs) {

        // Add weighted pitch_period-delayed signal

        for (int i = 0; i < IDIM; i++) {
            float out = pitch_tap * _longterm_lmemory[Constants.KPMAX + i - pitch_period];
            out += input[inofs + i];
            output[outofs + i] = pitch_gain * out;
        }

        // Shift-in input to lmemory

        for (int i = 0; i < Constants.KPMAX - IDIM; i++) {
            _longterm_lmemory[i] = _longterm_lmemory[i + IDIM];
        }
        System.arraycopy(input, inofs + 0, _longterm_lmemory, 135, IDIM);
    }

    private final float[] _shortterm_shpmem = new float[SPORDER];
    private final float[] _shortterm_shzmem = new float[SPORDER];

    /**
     * Again, memories (shpmem, shzmem) are in reverse order,
     * i.e. [0] is the oldest.
     */
    private void shortterm(float[] input, float[] output) {

        for (int k = 0; k < IDIM; k++) {

            // FIR Part
            float in = input[k];
            float out = in;
            for (int j = SPORDER - 1; j >= 1; j--) {
                out += _shortterm_shzmem[j] * shzcoef[j + 1];
                _shortterm_shzmem[j] = _shortterm_shzmem[j - 1];
            }
            out += _shortterm_shzmem[0] * shzcoef[1];
            _shortterm_shzmem[0] = in;

            // IIR Part
            for (int j = SPORDER - 1; j >= 1; j--) {
                out -= _shortterm_shpmem[j] * shpcoef[j + 1];
                _shortterm_shpmem[j] = _shortterm_shpmem[j - 1];
            }
            out -= _shortterm_shpmem[0] * shpcoef[1];
            _shortterm_shpmem[0] = out;
            output[k] = out+tiltz*_shortterm_shpmem[1];
        }
    }

    /**
     * Post-filter Adapter
     */
    void psf_adapter(float[] frame) {

        pitch_period = extract_pitch();

        // Compute Pitch Tap
        float corr = 0.0f;
        float corr_per = 0.0f;
        // Shift old memory
        for (int i = 0; i < SHIFTSZ; i++) {
            tap_mem[i] = tap_mem[i + NFRSZ];
        }
        // Shift new frame into memory
        System.arraycopy(frame, 0, tap_mem, 225, NFRSZ);

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
        float best_corr = -BIG;
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
    private static final int DCFRSZ = NFRSZ / DECIM;

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
        float best_corr = -BIG;
        float best_old_corr = -BIG;
        float tap0 = 0.0f;
        float tap1 = 0.0f;
        int old_per = (KPMIN + Constants.KPMAX) >> 1;
        float[] fil_decim_mem = new float[PDMSIZE];
        float[] fil_out_mem = new float[NFRSZ + DECIM];

        // Shift decimated filtered output
        for (int i = DCFRSZ; i < PDMSIZE; i++) {
            fil_decim_mem[i - DCFRSZ] = fil_decim_mem[i];
        }

        // Filter and decimate input

        int decim_phase = 0;
        int dk = 0;
        for (int k = 0; k < NFRSZ; k++) {
            float tmp;
            tmp = fil_mem[PMSIZE - NFRSZ + k] -
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

        // Now fine-tune the best correlation on undecimated domain

        permin = best_dper * DECIM - DECIM + 1;
        permax = best_dper * DECIM + DECIM - 1;
        if (permax > Constants.KPMAX) {
            permax = Constants.KPMAX;
        }
        if (permin < KPMIN) {
            permin = KPMIN;
        }

        best_corr = -BIG;
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
            if (permin < KPMIN) {
                permin = KPMIN;
            }

            best_old_corr = -BIG;
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

        for (int i = NFRSZ; i < PMSIZE; i++) {
            fil_mem[i - NFRSZ] = fil_mem[i];
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

//#endregion
}
