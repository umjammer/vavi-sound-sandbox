/*
 * iLBC Speech Coder ANSI-C Source Code
 *
 * Copyright (C) The Internet Society (2004). All Rights Reserved.
 */

package vavi.sound.ilbc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;


/**
 * iLBC Speech Coder ANSI-C Source Code
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 060701 nsano initial version <br>
 */
public class Ilbc {

//#region A.6. iLBC_define.h

    /* general codec settings */

//  private static final double FS = 8000.0;
    public static final int BLOCKL_20MS = 160;
    public static final int BLOCKL_30MS = 240;
    private static final int BLOCKL_MAX = 240;
    private static final int NSUB_20MS = 4;
    private static final int NSUB_30MS = 6;
    private static final int NSUB_MAX = 6;
    private static final int NASUB_20MS = 2;
    private static final int NASUB_30MS = 4;
    private static final int NASUB_MAX = 4;
    private static final int SUBL = 40;
    private static final int STATE_LEN = 80;
    private static final int STATE_SHORT_LEN_30MS = 58;
    private static final int STATE_SHORT_LEN_20MS = 57;

    /* LPC settings */

    private static final int LPC_FILTERORDER = 10;
    private static final double LPC_CHIRP_SYNTDENUM = 0.9025;
    private static final double LPC_CHIRP_WEIGHTDENUM = 0.4222;
    private static final int LPC_LOOKBACK = 60;
    private static final int LPC_N_20MS = 1;
    private static final int LPC_N_30MS = 2;
    private static final int LPC_N_MAX = 2;
//  private static final int LPC_ASYMDIFF = 20;
//  private static final double LPC_BW = 60.0;
//  private static final double LPC_WN = 1.0001;
    private static final int LSF_NSPLIT = 3;
    private static final int LSF_NUMBER_OF_STEPS = 4;
    private static final int LPC_HALFORDER = (LPC_FILTERORDER / 2);

    /* cb settings */

    private static final int CB_NSTAGES = 3;
    private static final int CB_EXPAND = 2;
    private static final int CB_MEML = 147;
    private static final int CB_FILTERLEN = 2 * 4;
    private static final int CB_HALFFILTERLEN = 4;
    private static final int CB_RESRANGE = 34;
    private static final double CB_MAXGAIN = 1.3;

    /* enhancer */

    /** block length */
    private static final int ENH_BLOCKL = 80;
    private static final int ENH_BLOCKL_HALF = (ENH_BLOCKL / 2);
    /**
     * 2*ENH_HL+1 is number blocks in said
     * second sequence
     */
    private static final int ENH_HL = 3;
    /**
     * max difference estimated and correct
     * pitch period
     */
    private static final int ENH_SLOP = 2;
    /**
     * pitch-estimates and pitch-locations
     * buffer length
     */
    private static final int ENH_PLOCSL = 20;
    private static final int ENH_OVERHANG = 2;
    /** upsampling rate */
    private static final int ENH_UPS0 = 4;
    /**
     * 2*FLO+1 is the length of each filter
     */
    private static final int ENH_FL0 = 3;
    private static final int ENH_VECTL = (ENH_BLOCKL + 2 * ENH_FL0);
    private static final int ENH_CORRDIM = (2 * ENH_SLOP + 1);
    private static final int ENH_NBLOCKS = (BLOCKL_MAX / ENH_BLOCKL);
    private static final int ENH_NBLOCKS_EXTRA = 5;
    /*
     * ENH_NBLOCKS + ENH_NBLOCKS_EXTRA
     */
    private static final int ENH_NBLOCKS_TOT = 8;
    private static final int ENH_BUFL = (ENH_NBLOCKS_TOT) * ENH_BLOCKL;
    private static final double ENH_ALPHA0 = 0.05;

    /* Down sampling */

    private static final int FILTERORDER_DS = 7;
    private static final int DELAY_DS = 3;
    private static final int FACTOR_DS = 2;

    /* bit stream defs */

    public static final int NO_OF_BYTES_20MS = 38;
    public static final int NO_OF_BYTES_30MS = 50;
    private static final int NO_OF_WORDS_20MS = 19;
    private static final int NO_OF_WORDS_30MS = 25;
//  private static final int STATE_BITS = 3;
//  private static final int BYTE_LEN = 8;
//  private static final int ULP_CLASSES = 3;

    /* help parameters */

    private static final double FLOAT_MAX = 1.0e37;
    private static final double EPS = 2.220446049250313e-016;
    private static final double PI = 3.14159265358979323846;
    private static final int MIN_SAMPLE = -32768;
    private static final int MAX_SAMPLE = 32767;
    private static final double TWO_PI = 6.283185307;
    private static final double PI2 = 0.159154943;

    /** type definition encoder instance */
    private static class ULP {
        /** [6][ULP_CLASSES+2] */
        int[][] lsf_bits;
        /** [ULP_CLASSES+2] */
        int[] start_bits;
        /** [ULP_CLASSES+2] */
        int[] startfirst_bits;
        /** [ULP_CLASSES+2] */
        int[] scale_bits;
        /** [ULP_CLASSES+2] */
        int[] state_bits;
        /** [CB_NSTAGES][ULP_CLASSES+2] */
        int[][] extra_cb_index;
        /** [CB_NSTAGES][ULP_CLASSES+2] */
        int[][] extra_cb_gain;
        /** [NSUB_MAX][CB_NSTAGES][ULP_CLASSES+2] */
        int[][][] cb_index;
        /** [NSUB_MAX][CB_NSTAGES][ULP_CLASSES+2] */
        int[][][] cb_gain;
    }

    /** type definition encoder instance */
    private static class Encoder {
        /** flag for frame size mode */
        int mode;
        /** basic parameters for different frame sizes */
        int blockl;
        int nsub;
        int nasub;
        int no_of_bytes, no_of_words;
        int lpc_n;
        int state_short_len;
        ULP ULP_inst;
        /** analysis filter state */
        final double[] anaMem = new double[LPC_FILTERORDER];
        /** old lsf parameters for interpolation */
        final double[] lsfold = new double[LPC_FILTERORDER];
        final double[] lsfdeqold = new double[LPC_FILTERORDER];
        /* signal buffer for LP analysis */
        final double[] lpc_buffer = new double[LPC_LOOKBACK + BLOCKL_MAX];
        /* state of input HP filter */
        final double[] hpimem = new double[4];
    }

    /** type definition decoder instance */
    static class Decoder {
        /** flag for frame size mode */
        int mode;
        /** basic parameters for different frame sizes */
        int blockl;
        int nsub;
        int nasub;
        int no_of_bytes, no_of_words;
        int lpc_n;
        int state_short_len;
        ULP ULP_inst;
        /** synthesis filter state */
        final double[] syntMem = new double[LPC_FILTERORDER];
        /** old LSF for interpolation */
        final double[] lsfdeqold = new double[LPC_FILTERORDER];
        /** pitch lag estimated in enhancer and used in PLC */
        int last_lag;
        /** PLC state information */
        int prevLag, consPLICount, prevPLI, prev_enh_pl;
        final double[] prevLpc = new double[LPC_FILTERORDER + 1];
        final double[] prevResidual = new double[NSUB_MAX * SUBL];
        double per;
        long seed;
        /** previous synthesis filter parameters */
        final double[] old_syntdenum = new double[(LPC_FILTERORDER + 1) * NSUB_MAX];
        /** state of output HP filter */
        final double[] hpomem = new double[4];
        /** enhancer state information */
        int use_enhancer;
        final double[] enh_buf = new double[ENH_BUFL];
        final double[] enh_period = new double[ENH_NBLOCKS_TOT];
    }

//#endregion

//#region A.8. constants.c

    /* ULP bit allocation */

    /** 20 ms frame */
    private static final ULP ulp_20msTbl = new ULP() {
        {
            /* LSF */
            lsf_bits = new int[][] {
                { 6, 0, 0, 0, 0 }, { 7, 0, 0, 0, 0 }, { 7, 0, 0, 0, 0 },
                { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }
            };
            /* Start state location, gain and samples */
            start_bits = new int[] { 2, 0, 0, 0, 0 };
            startfirst_bits = new int[] { 1, 0, 0, 0, 0 };
            scale_bits = new int[] { 6, 0, 0, 0, 0 };
            state_bits = new int[] { 0, 1, 2, 0, 0 };
            /* extra CB index and extra CB gain */
            extra_cb_index = new int[][] {
                { 6, 0, 1, 0, 0 }, { 0, 0, 7, 0, 0 }, { 0, 0, 7, 0, 0 }
            };
            extra_cb_gain = new int[][] {
                { 2, 0, 3, 0, 0 }, { 1, 1, 2, 0, 0 }, { 0, 0, 3, 0, 0 }
            };
            /* CB index and CB gain */
            cb_index = new int[][][] {
                {{ 7, 0, 1, 0, 0 }, { 0, 0, 7, 0, 0 }, { 0, 0, 7, 0, 0 }},
                {{ 0, 0, 8, 0, 0 }, { 0, 0, 8, 0, 0 }, { 0, 0, 8, 0, 0 }},
                {{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }},
                {{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }}
            };
            cb_gain = new int[][][] {
                {{ 1, 2, 2, 0, 0 }, { 1, 1, 2, 0, 0 }, { 0, 0, 3, 0, 0 }},
                {{ 1, 1, 3, 0, 0 }, { 0, 2, 2, 0, 0 }, { 0, 0, 3, 0, 0 }},
                {{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }},
                {{ 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0 }}
            };
        }
    };

    /** 30 ms frame */
    private static final ULP ulp_30msTbl = new ULP() {
        {
            /* LSF */

            lsf_bits = new int[][] {
                { 6, 0, 0, 0, 0 }, { 7, 0, 0, 0, 0 }, { 7, 0, 0, 0, 0 },
                { 6, 0, 0, 0, 0 }, { 7, 0, 0, 0, 0 }, { 7, 0, 0, 0, 0 }
            };
            /* Start state location, gain and samples */
            start_bits = new int[] { 3, 0, 0, 0, 0 };
            startfirst_bits = new int[] { 1, 0, 0, 0, 0 };
            scale_bits = new int[] { 6, 0, 0, 0, 0 };
            state_bits = new int[] { 0, 1, 2, 0, 0 };
            /* extra CB index and extra CB gain */
            extra_cb_index = new int[][] {
                { 4, 2, 1, 0, 0 }, { 0, 0, 7, 0, 0 }, { 0, 0, 7, 0, 0 }
            };
            extra_cb_gain = new int[][] {
                { 1, 1, 3, 0, 0 }, { 1, 1, 2, 0, 0 }, { 0, 0, 3, 0, 0 }
            };
            /* CB index and CB gain */
            cb_index = new int[][][] {
                {{ 6, 1, 1, 0, 0 }, { 0, 0, 7, 0, 0 }, { 0, 0, 7, 0, 0 }},
                {{ 0, 7, 1, 0, 0 }, { 0, 0, 8, 0, 0 }, { 0, 0, 8, 0, 0 }},
                {{ 0, 7, 1, 0, 0 }, { 0, 0, 8, 0, 0 }, { 0, 0, 8, 0, 0 }},
                {{ 0, 7, 1, 0, 0 }, { 0, 0, 8, 0, 0 }, { 0, 0, 8, 0, 0 }}
            };
            cb_gain = new int[][][] {
                {{ 1, 2, 2, 0, 0 }, { 1, 2, 1, 0, 0 }, { 0, 0, 3, 0, 0 }},
                {{ 0, 2, 3, 0, 0 }, { 0, 2, 2, 0, 0 }, { 0, 0, 3, 0, 0 }},
                {{ 0, 1, 4, 0, 0 }, { 0, 1, 3, 0, 0 }, { 0, 0, 3, 0, 0 }},
                {{ 0, 1, 4, 0, 0 }, { 0, 1, 3, 0, 0 }, { 0, 0, 3, 0, 0 }}
            };
        }
    };

    /* HP Filters */

    private static final double[] hpi_zero_coefsTbl = {
        0.92727436, -1.8544941, 0.92727436
    };

    private static final double[] hpi_pole_coefsTbl = {
        1.0, -1.9059465, 0.9114024
    };

    private static final double[] hpo_zero_coefsTbl = {
        0.93980581, -1.8795834, 0.93980581
    };

    private static final double[] hpo_pole_coefsTbl = {
        1.0, -1.9330735, 0.93589199
    };

    /** LP Filter */
    private static final double[] lpFilt_coefsTbl = { // FILTERORDER_DS
        -0.066650, 0.125000, 0.316650,
        0.414063, 0.316650,
        0.125000, -0.066650
    };

    /* State quantization tables */

    private static final double[] state_sq3Tbl = {
        -3.719849, -2.177490, -1.130005,
        -0.309692, 0.444214, 1.329712,
        2.436279, 3.983887
    };

    private static final double[] state_frgqTbl = {
        1.000085, 1.071695, 1.140395,
        1.206868, 1.277188, 1.351503,
        1.429380, 1.500727, 1.569049,
        1.639599, 1.707071, 1.781531,
        1.840799, 1.901550, 1.956695,
        2.006750, 2.055474, 2.102787,
        2.142819, 2.183592, 2.217962,
        2.257177, 2.295739, 2.332967,
        2.369248, 2.402792, 2.435080,
        2.468598, 2.503394, 2.539284,
        2.572944, 2.605036, 2.636331,
        2.668939, 2.698780, 2.729101,
        2.759786, 2.789834, 2.818679,
        2.848074, 2.877470, 2.906899,
        2.936655, 2.967804, 3.000115,
        3.033367, 3.066355, 3.104231,
        3.141499, 3.183012, 3.222952,
        3.265433, 3.308441, 3.350823,
        3.395275, 3.442793, 3.490801,
        3.542514, 3.604064, 3.666050,
        3.740994, 3.830749, 3.938770,
        4.101764
    };

    /* CB tables */

    private static final int[][] search_rangeTbl = { // 5 x CB_NSTAGES
        { 58, 58, 58 },
        { 108, 44, 44 },
        { 108, 108, 108 },
        { 108, 108, 108 },
        { 108, 108, 108 }
    };

    private static final int stMemLTbl = 85;

    private static final int[] memLfTbl = { // NASUB_MAX
        147, 147, 147, 147
    };

    /* expansion filter(s) */

    private static final double[] cbfiltersTbl = { // CB_FILTERLEN
        -0.034180, 0.108887, -0.184326,
        0.806152, 0.713379, -0.144043,
        0.083740, -0.033691
    };

    /* Gain Quantization */

    private static final double[] gain_sq3Tbl = {
        -1.000000, -0.659973, -0.330017,
        0.000000, 0.250000, 0.500000,
        0.750000, 1.00000
    };

    private static final double[] gain_sq4Tbl = {
        -1.049988, -0.900024, -0.750000,
        -0.599976, -0.450012, -0.299988,
        -0.150024, 0.000000, 0.150024,
        0.299988, 0.450012, 0.599976,
        0.750000, 0.900024, 1.049988,
        1.200012
    };

    private static final double[] gain_sq5Tbl = {
        0.037476, 0.075012, 0.112488,
        0.150024, 0.187500, 0.224976,
        0.262512, 0.299988, 0.337524,
        0.375000, 0.412476, 0.450012,
        0.487488, 0.525024, 0.562500,
        0.599976, 0.637512, 0.674988,
        0.712524, 0.750000, 0.787476,
        0.825012, 0.862488, 0.900024,
        0.937500, 0.974976, 1.012512,
        1.049988, 1.087524, 1.125000,
        1.162476, 1.200012
    };

    /* Enhancer - Upsamling a factor 4 (ENH_UPS0 = 4) */
    private static final double[] polyphaserTbl = { // ENH_UPS0*(2*ENH_FL0+1)
        0.000000, 0.000000, 0.000000, 1.000000,
        0.000000, 0.000000, 0.000000, 0.015625, -0.076904, 0.288330, 0.862061,
        -0.106445, 0.018799, -0.015625, 0.023682, -0.124268, 0.601563, 0.601563,
        -0.124268, 0.023682, -0.023682, 0.018799, -0.106445, 0.862061, 0.288330,
        -0.076904, 0.015625, -0.018799
    };

    private static final double[] enh_plocsTbl = { // ENH_NBLOCKS_TOT
        40.0, 120.0,
        200.0, 280.0, 360.0,
        440.0, 520.0, 600.0
    };

    /* LPC analysis and quantization */

    private static final int[] dim_lsfCbTbl = { // LSF_NSPLIT
        3, 3, 4
    };

    private static final int[] size_lsfCbTbl = { // LSF_NSPLIT
        64, 128, 128
    };

    private static final double[] lsfmeanTbl = { // LPC_FILTERORDER
        0.281738, 0.445801, 0.663330,
        0.962524, 1.251831, 1.533081,
        1.850586, 2.137817, 2.481445,
        2.777344
    };

    private static final double[] lsf_weightTbl_30ms = {
        (1.0 / 2.0), 1.0, (2.0 / 3.0),
        (1.0 / 3.0), 0.0, 0.0
    };

    private static final double[] lsf_weightTbl_20ms = {
        (3.0 / 4.0), (2.0 / 4.0),
        (1.0 / 4.0), (0.0)
    };

    /** Hanning LPC window */
    private static final double[] lpc_winTbl = { // BLOCKL_MAX
        0.000183, 0.000671, 0.001526,
        0.002716, 0.004242, 0.006104,
        0.008301, 0.010834, 0.013702,
        0.016907, 0.020416, 0.024261,
        0.028442, 0.032928, 0.037750,
        0.042877, 0.048309, 0.054047,
        0.060089, 0.066437, 0.073090,
        0.080017, 0.087219, 0.094727,
        0.102509, 0.110535, 0.118835,
        0.127411, 0.136230, 0.145294,
        0.154602, 0.164154, 0.173920,
        0.183899, 0.194122, 0.204529,
        0.215149, 0.225952, 0.236938,
        0.248108, 0.259460, 0.270966,
        0.282654, 0.294464, 0.306396,
        0.318481, 0.330688, 0.343018,
        0.355438, 0.367981, 0.380585,
        0.393280, 0.406067, 0.418884,
        0.431763, 0.444702, 0.457672,
        0.470673, 0.483704, 0.496735,
        0.509766, 0.522797, 0.535828,
        0.548798, 0.561768, 0.574677,
        0.587524, 0.600342, 0.613068,
        0.625732, 0.638306, 0.650787,
        0.663147, 0.675415, 0.687561,
        0.699585, 0.711487, 0.723206,
        0.734802, 0.746216, 0.757477,
        0.768585, 0.779480, 0.790192,
        0.800720, 0.811005, 0.821106,
        0.830994, 0.840668, 0.850067,
        0.859253, 0.868225, 0.876892,
        0.885345, 0.893524, 0.901428,
        0.909058, 0.916412, 0.923492,
        0.930267, 0.936768, 0.942963,
        0.948853, 0.954437, 0.959717,
        0.964691, 0.969360, 0.973694,
        0.977692, 0.981384, 0.984741,
        0.987762, 0.990479, 0.992828,
        0.994873, 0.996552, 0.997925,
        0.998932, 0.999603, 0.999969,
        0.999969, 0.999603, 0.998932,
        0.997925, 0.996552, 0.994873,
        0.992828, 0.990479, 0.987762,
        0.984741, 0.981384, 0.977692,
        0.973694, 0.969360, 0.964691,
        0.959717, 0.954437, 0.948853,
        0.942963, 0.936768, 0.930267,
        0.923492, 0.916412, 0.909058,
        0.901428, 0.893524, 0.885345,
        0.876892, 0.868225, 0.859253,
        0.850067, 0.840668, 0.830994,
        0.821106, 0.811005, 0.800720,
        0.790192, 0.779480, 0.768585,
        0.757477, 0.746216, 0.734802,
        0.723206, 0.711487, 0.699585,
        0.687561, 0.675415, 0.663147,
        0.650787, 0.638306, 0.625732,
        0.613068, 0.600342, 0.587524,
        0.574677, 0.561768, 0.548798,
        0.535828, 0.522797, 0.509766,
        0.496735, 0.483704, 0.470673,
        0.457672, 0.444702, 0.431763,
        0.418884, 0.406067, 0.393280,
        0.380585, 0.367981, 0.355438,
        0.343018, 0.330688, 0.318481,
        0.306396, 0.294464, 0.282654,
        0.270966, 0.259460, 0.248108,
        0.236938, 0.225952, 0.215149,
        0.204529, 0.194122, 0.183899,
        0.173920, 0.164154, 0.154602,
        0.145294, 0.136230, 0.127411,
        0.118835, 0.110535, 0.102509,
        0.094727, 0.087219, 0.080017,
        0.073090, 0.066437, 0.060089,
        0.054047, 0.048309, 0.042877,
        0.037750, 0.032928, 0.028442,
        0.024261, 0.020416, 0.016907,
        0.013702, 0.010834, 0.008301,
        0.006104, 0.004242, 0.002716,
        0.001526, 0.000671, 0.000183
    };

    /** Asymmetric LPC window */
    private static final double[] lpc_asymwinTbl = { // BLOCKL_MAX
        0.000061, 0.000214, 0.000458,
        0.000824, 0.001282, 0.001831,
        0.002472, 0.003235, 0.004120,
        0.005066, 0.006134, 0.007294,
        0.008545, 0.009918, 0.011383,
        0.012939, 0.014587, 0.016357,
        0.018219, 0.020172, 0.022217,
        0.024353, 0.026611, 0.028961,
        0.031372, 0.033905, 0.036530,
        0.039276, 0.042084, 0.044983,
        0.047974, 0.051086, 0.054260,
        0.057526, 0.060883, 0.064331,
        0.067871, 0.071503, 0.075226,
        0.079010, 0.082916, 0.086884,
        0.090942, 0.095062, 0.099304,
        0.103607, 0.107971, 0.112427,
        0.116974, 0.121582, 0.126282,
        0.131073, 0.135895, 0.140839,
        0.145813, 0.150879, 0.156006,
        0.161224, 0.166504, 0.171844,
        0.177246, 0.182709, 0.188263,
        0.193848, 0.199524, 0.205231,
        0.211029, 0.216858, 0.222778,
        0.228729, 0.234741, 0.240814,
        0.246918, 0.253082, 0.259308,
        0.265564, 0.271881, 0.278259,
        0.284668, 0.291107, 0.297607,
        0.304138, 0.310730, 0.317322,
        0.323975, 0.330658, 0.337372,
        0.344147, 0.350922, 0.357727,
        0.364594, 0.371460, 0.378357,
        0.385284, 0.392212, 0.399170,
        0.406158, 0.413177, 0.420197,
        0.427246, 0.434296, 0.441376,
        0.448456, 0.455536, 0.462646,
        0.469757, 0.476868, 0.483978,
        0.491089, 0.498230, 0.505341,
        0.512451, 0.519592, 0.526703,
        0.533813, 0.540924, 0.548004,
        0.555084, 0.562164, 0.569244,
        0.576294, 0.583313, 0.590332,
        0.597321, 0.604309, 0.611267,
        0.618195, 0.625092, 0.631989,
        0.638855, 0.645660, 0.652466,
        0.659241, 0.665985, 0.672668,
        0.679352, 0.685974, 0.692566,
        0.699127, 0.705658, 0.712128,
        0.718536, 0.724945, 0.731262,
        0.737549, 0.743805, 0.750000,
        0.756134, 0.762238, 0.768280,
        0.774261, 0.780182, 0.786072,
        0.791870, 0.797638, 0.803314,
        0.808960, 0.814514, 0.820038,
        0.825470, 0.830841, 0.836151,
        0.841400, 0.846558, 0.851654,
        0.856689, 0.861633, 0.866516,
        0.871338, 0.876068, 0.880737,
        0.885315, 0.889801, 0.894226,
        0.898560, 0.902832, 0.907013,
        0.911102, 0.915100, 0.919037,
        0.922882, 0.926636, 0.930328,
        0.933899, 0.937408, 0.940796,
        0.944122, 0.947357, 0.950470,
        0.953522, 0.956482, 0.959351,
        0.962097, 0.964783, 0.967377,
        0.969849, 0.972229, 0.974518,
        0.976715, 0.978821, 0.980835,
        0.982727, 0.984528, 0.986237,
        0.987854, 0.989380, 0.990784,
        0.992096, 0.993317, 0.994415,
        0.995422, 0.996338, 0.997162,
        0.997864, 0.998474, 0.998962,
        0.999390, 0.999695, 0.999878,
        0.999969, 0.999969, 0.996918,
        0.987701, 0.972382, 0.951050,
        0.923889, 0.891022, 0.852631,
        0.809021, 0.760406, 0.707092,
        0.649445, 0.587799, 0.522491,
        0.453979, 0.382690, 0.309021,
        0.233459, 0.156433, 0.078461
    };

    /** Lag window for LPC */
    private static final double[] lpc_lagwinTbl = { // LPC_FILTERORDER + 1
        1.000100, 0.998890, 0.995569, 0.990057, 0.982392,
        0.972623, 0.960816, 0.947047, 0.931405, 0.913989, 0.894909
    };

    /** LSF quantization */
    private static final double[] lsfCbTbl = { // 64 * 3 + 128 * 3 + 128 * 4
        0.155396, 0.273193, 0.451172,
        0.390503, 0.648071, 1.002075,
        0.440186, 0.692261, 0.955688,
        0.343628, 0.642334, 1.071533,
        0.318359, 0.491577, 0.670532,
        0.193115, 0.375488, 0.725708,
        0.364136, 0.510376, 0.658691,
        0.297485, 0.527588, 0.842529,
        0.227173, 0.365967, 0.563110,
        0.244995, 0.396729, 0.636475,
        0.169434, 0.300171, 0.520264,
        0.312866, 0.464478, 0.643188,
        0.248535, 0.429932, 0.626099,
        0.236206, 0.491333, 0.817139,
        0.334961, 0.625122, 0.895752,
        0.343018, 0.518555, 0.698608,
        0.372803, 0.659790, 0.945435,
        0.176880, 0.316528, 0.581421,
        0.416382, 0.625977, 0.805176,
        0.303223, 0.568726, 0.915039,
        0.203613, 0.351440, 0.588135,
        0.221191, 0.375000, 0.614746,
        0.199951, 0.323364, 0.476074,
        0.300781, 0.433350, 0.566895,
        0.226196, 0.354004, 0.507568,
        0.300049, 0.508179, 0.711670,
        0.312012, 0.492676, 0.763428,
        0.329956, 0.541016, 0.795776,
        0.373779, 0.604614, 0.928833,
        0.210571, 0.452026, 0.755249,
        0.271118, 0.473267, 0.662476,
        0.285522, 0.436890, 0.634399,
        0.246704, 0.565552, 0.859009,
        0.270508, 0.406250, 0.553589,
        0.361450, 0.578491, 0.813843,
        0.342651, 0.482788, 0.622437,
        0.340332, 0.549438, 0.743164,
        0.200439, 0.336304, 0.540894,
        0.407837, 0.644775, 0.895142,
        0.294678, 0.454834, 0.699097,
        0.193115, 0.344482, 0.643188,
        0.275757, 0.420776, 0.598755,
        0.380493, 0.608643, 0.861084,
        0.222778, 0.426147, 0.676514,

        0.407471, 0.700195, 1.053101,
        0.218384, 0.377197, 0.669922,
        0.313232, 0.454102, 0.600952,
        0.347412, 0.571533, 0.874146,
        0.238037, 0.405396, 0.729492,
        0.223877, 0.412964, 0.822021,
        0.395264, 0.582153, 0.743896,
        0.247925, 0.485596, 0.720581,
        0.229126, 0.496582, 0.907715,
        0.260132, 0.566895, 1.012695,
        0.337402, 0.611572, 0.978149,
        0.267822, 0.447632, 0.769287,
        0.250610, 0.381714, 0.530029,
        0.430054, 0.805054, 1.221924,
        0.382568, 0.544067, 0.701660,
        0.383545, 0.710327, 1.149170,
        0.271362, 0.529053, 0.775513,
        0.246826, 0.393555, 0.588623,
        0.266846, 0.422119, 0.676758,
        0.311523, 0.580688, 0.838623,
        1.331177, 1.576782, 1.779541,
        1.160034, 1.401978, 1.768188,
        1.161865, 1.525146, 1.715332,
        0.759521, 0.913940, 1.119873,
        0.947144, 1.121338, 1.282471,
        1.015015, 1.557007, 1.804932,
        1.172974, 1.402100, 1.692627,
        1.087524, 1.474243, 1.665405,
        0.899536, 1.105225, 1.406250,
        1.148438, 1.484741, 1.796265,
        0.785645, 1.209839, 1.567749,
        0.867798, 1.166504, 1.450684,
        0.922485, 1.229858, 1.420898,
        0.791260, 1.123291, 1.409546,
        0.788940, 0.966064, 1.340332,
        1.051147, 1.272827, 1.556641,
        0.866821, 1.181152, 1.538818,
        0.906738, 1.373535, 1.607910,
        1.244751, 1.581421, 1.933838,
        0.913940, 1.337280, 1.539673,
        0.680542, 0.959229, 1.662720,
        0.887207, 1.430542, 1.800781,
        0.912598, 1.433594, 1.683960,
        0.860474, 1.060303, 1.455322,
        1.005127, 1.381104, 1.706909,
        0.800781, 1.363892, 1.829102,
        0.781860, 1.124390, 1.505981,
        1.003662, 1.471436, 1.684692,
        0.981323, 1.309570, 1.618042,
        1.228760, 1.554321, 1.756470,
        0.734375, 0.895752, 1.225586, 0.841797, 1.055664, 1.249268, 0.920166, 1.119385, 1.486206, 0.894409, 1.539063,
        1.828979, 1.283691, 1.543335, 1.858276,

        0.676025, 0.933105, 1.490845, 0.821289, 1.491821, 1.739868, 0.923218, 1.144653, 1.580566, 1.057251, 1.345581, 1.635864, 0.888672, 1.074951, 1.353149, 0.942749, 1.195435, 1.505493, 1.492310, 1.788086, 2.039673, 1.070313, 1.634399, 1.860962, 1.253296, 1.488892, 1.686035, 0.647095,
        0.864014, 1.401855, 0.866699, 1.254883, 1.453369, 1.063965, 1.532593, 1.731323, 1.167847, 1.521484, 1.884033, 0.956055, 1.502075, 1.745605, 0.928711, 1.288574, 1.479614, 1.088013, 1.380737, 1.570801, 0.905029, 1.186768, 1.371948, 1.057861, 1.421021, 1.617432, 1.108276, 1.312500,
        1.501465, 0.979492, 1.416992, 1.624268, 1.276001, 1.661011, 2.007935, 0.993042, 1.168579, 1.331665, 0.778198, 0.944946, 1.235962, 1.223755, 1.491333, 1.815674, 0.852661, 1.350464, 1.722290, 1.134766, 1.593140, 1.787354, 1.051392, 1.339722, 1.531006, 0.803589, 1.271240, 1.652100,
        0.755737, 1.143555, 1.639404, 0.700928, 0.837280, 1.130371, 0.942749, 1.197876, 1.669800, 0.993286, 1.378296, 1.566528, 0.801025, 1.095337, 1.298950, 0.739990, 1.032959, 1.383667, 0.845703, 1.072266, 1.543823, 0.915649, 1.072266, 1.224487, 1.021973, 1.226196, 1.481323, 0.999878,
        1.204102, 1.555908, 0.722290, 0.913940, 1.340210, 0.673340, 0.835938, 1.259521, 0.832397, 1.208374, 1.394165, 0.962158, 1.576172, 1.912842, 1.166748, 1.370850, 1.556763, 0.946289, 1.138550, 1.400391, 1.035034, 1.218262, 1.386475, 1.393799, 1.717773, 2.000244, 0.972656, 1.260986,
        1.760620, 1.028198, 1.288452, 1.484619,

        0.773560, 1.258057, 1.756714, 1.080322, 1.328003, 1.742676, 0.823975, 1.450806, 1.917725, 0.859009, 1.016602, 1.191895, 0.843994, 1.131104, 1.645020, 1.189697, 1.702759, 1.894409, 1.346680, 1.763184, 2.066040, 0.980469, 1.253784, 1.441650, 1.338135, 1.641968, 1.932739, 1.223267,
        1.424194, 1.626465, 0.765747, 1.004150, 1.579102, 1.042847, 1.269165, 1.647461, 0.968750, 1.257568, 1.555786, 0.826294, 0.993408, 1.275146, 0.742310, 0.950439, 1.430542, 1.054321, 1.439819, 1.828003, 1.072998, 1.261719, 1.441895, 0.859375, 1.036377, 1.314819, 0.895752, 1.267212,
        1.605591, 0.805420, 0.962891, 1.142334, 0.795654, 1.005493, 1.468506, 1.105347, 1.313843, 1.584839, 0.792236, 1.221802, 1.465698, 1.170532, 1.467651, 1.664063, 0.838257, 1.153198, 1.342163, 0.968018, 1.198242, 1.391235, 1.250122, 1.623535, 1.823608, 0.711670, 1.058350, 1.512085,
        1.204834, 1.454468, 1.739136, 1.137451, 1.421753, 1.620117, 0.820435, 1.322754, 1.578247, 0.798706, 1.005005, 1.213867, 0.980713, 1.324951, 1.512939, 1.112305, 1.438843, 1.735596, 1.135498, 1.356689, 1.635742, 1.101318, 1.387451, 1.686523, 0.849854, 1.276978, 1.523438, 1.377930,
        1.627563, 1.858154, 0.884888, 1.095459, 1.287476, 1.289795, 1.505859, 1.756592, 0.817505, 1.384155, 1.650513, 1.446655, 1.702148, 1.931885, 0.835815, 1.023071, 1.385376, 0.916626, 1.139038, 1.335327, 0.980103, 1.174072, 1.453735, 1.705688, 2.153809, 2.398315, 2.743408, 1.797119,
        2.016846, 2.445679, 2.701904, 1.990356, 2.219116, 2.576416, 2.813477,

        1.849365, 2.190918, 2.611572, 2.835083, 1.657959, 1.854370, 2.159058, 2.726196, 1.437744, 1.897705, 2.253174, 2.655396, 2.028687, 2.247314, 2.542358, 2.875854, 1.736938, 1.922119, 2.185913, 2.743408, 1.521606, 1.870972, 2.526855, 2.786987, 1.841431, 2.050659, 2.463623, 2.857666,
        1.590088, 2.067261, 2.427979, 2.794434, 1.746826, 2.057373, 2.320190, 2.800781, 1.734619, 1.940552, 2.306030, 2.826416, 1.786255, 2.204468, 2.457520, 2.795288, 1.861084, 2.170532, 2.414551, 2.763672, 2.001465, 2.307617, 2.552734, 2.811890, 1.784424, 2.124146, 2.381592, 2.645508,
        1.888794, 2.135864, 2.418579, 2.861206, 2.301147, 2.531250, 2.724976, 2.913086, 1.837769, 2.051270, 2.261963, 2.553223, 2.012939, 2.221191, 2.440186, 2.678101, 1.429565, 1.858276, 2.582275, 2.845703, 1.622803, 1.897705, 2.367310, 2.621094, 1.581543, 1.960449, 2.515869, 2.736450,
        1.419434, 1.933960, 2.394653, 2.746704, 1.721924, 2.059570, 2.421753, 2.769653, 1.911011, 2.220703, 2.461060, 2.740723, 1.581177, 1.860840, 2.516968, 2.874634, 1.870361, 2.098755, 2.432373, 2.656494, 2.059692, 2.279785, 2.495605, 2.729370, 1.815674, 2.181519, 2.451538, 2.680542,
        1.407959, 1.768311, 2.343018, 2.668091, 2.168701, 2.394653, 2.604736, 2.829346, 1.636230, 1.865723, 2.329102, 2.824219, 1.878906, 2.139526, 2.376709, 2.679810, 1.765381, 1.971802, 2.195435, 2.586914, 2.164795, 2.410889, 2.673706, 2.903198, 2.071899, 2.331055, 2.645874, 2.907104,
        2.026001, 2.311523, 2.594849, 2.863892, 1.948975, 2.180786, 2.514893, 2.797852, 1.881836, 2.130859, 2.478149, 2.804199, 2.238159, 2.452759, 2.652832, 2.868286, 1.897949, 2.101685, 2.524292, 2.880127, 1.856445, 2.074585, 2.541016, 2.791748, 1.695557, 2.199097, 2.506226, 2.742676,
        1.612671, 1.877075, 2.435425, 2.732910, 1.568848, 1.786499, 2.194580, 2.768555, 1.953369, 2.164551, 2.486938, 2.874023, 1.388306, 1.725342, 2.384521, 2.771851, 2.115356, 2.337769, 2.592896, 2.864014, 1.905762, 2.111328, 2.363525, 2.789307,

        1.882568, 2.332031, 2.598267, 2.827637, 1.683594, 2.088745, 2.361938, 2.608643, 1.874023, 2.182129, 2.536133, 2.766968, 1.861938, 2.070435, 2.309692, 2.700562, 1.722168, 2.107422, 2.477295, 2.837646, 1.926880, 2.184692, 2.442627, 2.663818, 2.123901, 2.337280, 2.553101, 2.777466,
        1.588135, 1.911499, 2.212769, 2.543945, 2.053955, 2.370850, 2.712158, 2.939941, 2.210449, 2.519653, 2.770386, 2.958618, 2.199463, 2.474731, 2.718262, 2.919922, 1.960083, 2.175415, 2.608032, 2.888794, 1.953735, 2.185181, 2.428223, 2.809570, 1.615234, 2.036499, 2.576538, 2.834595,
        1.621094, 2.028198, 2.431030, 2.664673, 1.824951, 2.267456, 2.514526, 2.747925, 1.994263, 2.229126, 2.475220, 2.833984, 1.746338, 2.011353, 2.588257, 2.826904, 1.562866, 2.135986, 2.471680, 2.687256, 1.748901, 2.083496, 2.460938, 2.686279, 1.758057, 2.131470, 2.636597, 2.891602,
        2.071289, 2.299072, 2.550781, 2.814331, 1.839600, 2.094360, 2.496460, 2.723999, 1.882202, 2.088257, 2.636841, 2.923096, 1.957886, 2.153198, 2.384399, 2.615234, 1.992920, 2.351196, 2.654419, 2.889771, 2.012817, 2.262451, 2.643799, 2.903076, 2.025635, 2.254761, 2.508423, 2.784058,
        2.316040, 2.589355, 2.794189, 2.963623, 1.741211, 2.279541, 2.578491, 2.816284, 1.845337, 2.055786, 2.348511, 2.822021, 1.679932, 1.926514, 2.499756, 2.835693, 1.722534, 1.946899, 2.448486, 2.728760, 1.829834, 2.043213, 2.580444, 2.867676, 1.676636, 2.071655, 2.322510, 2.704834,
        1.791504, 2.113525, 2.469727, 2.784058, 1.977051, 2.215088, 2.497437, 2.726929, 1.800171, 2.106689, 2.357788, 2.738892, 1.827759, 2.170166, 2.525879, 2.852417, 1.918335, 2.132813, 2.488403, 2.728149, 1.916748, 2.225098, 2.542603, 2.857666, 1.761230, 1.976074, 2.507446, 2.884521,
        2.053711, 2.367432, 2.608032, 2.837646, 1.595337, 2.000977, 2.307129, 2.578247, 1.470581, 2.031250, 2.375854, 2.647583, 1.801392, 2.128052, 2.399780, 2.822876, 1.853638, 2.066650, 2.429199, 2.751465, 1.956299, 2.163696, 2.394775, 2.734253,

        1.963623, 2.275757, 2.585327, 2.865234,
        1.887451, 2.105469, 2.331787, 2.587402,
        2.120117, 2.443359, 2.733887, 2.941406,
        1.506348, 1.766968, 2.400513, 2.851807, 1.664551, 1.981079, 2.375732, 2.774414, 1.720703, 1.978882, 2.391479, 2.640991, 1.483398, 1.814819, 2.434448, 2.722290,
        1.769043, 2.136597, 2.563721, 2.774414,
        1.810791, 2.049316, 2.373901, 2.613647,
        1.788330, 2.005981, 2.359131, 2.723145,
        1.785156, 1.993164, 2.399780, 2.832520,
        1.695313, 2.022949, 2.522583, 2.745117,
        1.584106, 1.965576, 2.299927, 2.715576,
        1.894897, 2.249878, 2.655884, 2.897705,
        1.720581, 1.995728, 2.299438, 2.557007,
        1.619385, 2.173950, 2.574219, 2.787964,
        1.883179, 2.220459, 2.474365, 2.825073,
        1.447632, 2.045044, 2.555542, 2.744873,
        1.502686, 2.156616, 2.653320, 2.846558,
        1.711548, 1.944092, 2.282959, 2.685791,
        1.499756, 1.867554, 2.341064, 2.578857,
        1.916870, 2.135132, 2.568237, 2.826050,
        1.498047, 1.711182, 2.223267, 2.755127,
        1.808716, 1.997559, 2.256470, 2.758545,
        2.088501, 2.402710, 2.667358, 2.890259,
        1.545044, 1.819214, 2.324097, 2.692993,
        1.796021, 2.012573, 2.505737, 2.784912,
        1.786499, 2.041748, 2.290405, 2.650757,
        1.938232, 2.264404, 2.529053, 2.796143
    };

//#endregion

//#region A.1. iLBC_test.c

    private static final int ILBCNOOFWORDS_MAX = (NO_OF_BYTES_30MS / 2);

    /**
     * Encoder interface function.
     *
     * @return Number of bytes encoded
     * @param encoder (i/o) Encoder instance
     * @param encoded_data [o] The encoded bytes
     * @param data The signal block to encode
     */
    static int encode(Encoder encoder, byte[] encoded_data, byte[] data) {
        double[] block = new double[BLOCKL_MAX];

        // convert signal to double

        for (int k = 0; k < encoder.blockl; k++) {
            block[k] = (data[k * 2] & 0xff) | ((data[k * 2 + 1] & 0xff) << 8);
        }
//Arrays.stream(block, 0, 64).forEach(System.out::println);

        // do the actual encoding

        iLBC_encode(encoded_data, block, encoder);
//logger.log(Level.TRACE, "\n" + StringUtil.getDump(encoded_data, 64));

        return encoder.no_of_bytes;
    }

    /**
     * Decoder interface function
     *
     * @return Number of decoded samples
     * @param decoder (i/o) Decoder instance
     * @param decoded_data [o] Decoded signal block
     * @param encoded_data Encoded bytes
     * @param mode 0=PL, 1=Normal
     */
    static int decode(Decoder decoder, byte[] decoded_data, byte[] encoded_data, int mode) {
        double[] decblock = new double[BLOCKL_MAX];

        // check if mode is valid

        if (mode < 0 || mode > 1) {
            throw new IllegalArgumentException("ERROR - Wrong mode - 0, 1 allowed: " + mode);
        }

        // do actual decoding of block

        iLBC_decode(decblock, encoded_data, decoder, mode);

        // convert to short

        for (int k = 0; k < decoder.blockl; k++) {
            int dtmp = (int) Math.round(decblock[k]);

            if (dtmp < MIN_SAMPLE) {
                dtmp = MIN_SAMPLE;
            } else if (dtmp > MAX_SAMPLE) {
                dtmp = MAX_SAMPLE;
            }
            decoded_data[k * 2] = (byte) (dtmp & 0xff);
            decoded_data[k * 2 + 1] = (byte) ((dtmp & 0xff00) >> 8);
        }

        return decoder.blockl;
    }

    /**
     * Main program to test iLBC encoding and decoding
     *
     * @param argv 0: modem 1: infile, 2: bytefile, 3: outfile, 4: channel
     *  mode: Frame size for the encoding/decoding, 20 ms or 30 ms
     *  infile: Input file, speech for encoder (16-bit pcm file)
     *  bytefile: Bit stream output from the encoder
     *  outfile: Output file, decoded speech (16-bit pcm file)
     *  channel: Bit error file, optional (16-bit) 1 - Packet received correctly 0 - Packet Lost
     */
    public static void main(String[] argv) throws Exception {

        // Runtime statistics

        long runtime;
        double outTime;

        InputStream iFile, cFile;
        OutputStream oFile, eFile;
        byte[] data = new byte[BLOCKL_MAX * 2];
        byte[] encoded_data = new byte[ILBCNOOFWORDS_MAX * 2], decoded_data = new byte[BLOCKL_MAX * 2];
        int len;
        int pli;
        int blockCount = 0;
        int packetLossCount = 0;

        // Create structs
        Encoder encoder = new Encoder();
        Decoder decoder = new Decoder();

        // get arguments and open files

        if ((argv.length != 4) && (argv.length != 5)) {
            System.err.print("\n*-----------------------------------------------*\n");
            System.err.printf("   %s <20,30> input encoded decoded (channel)\n\n", Ilbc.class.getName());
            System.err.print("   mode    : Frame size for the encoding/decoding\n");
            System.err.print("                 20 - 20 ms\n");
            System.err.print("                 30 - 30 ms\n");
            System.err.print("   input   : Speech for encoder (16-bit pcm file)\n");
            System.err.print("   encoded : Encoded bit stream\n");
            System.err.print("   decoded : Decoded speech (16-bit pcm file)\n");
            System.err.print("   channel : Packet loss pattern, optional (16-bit)\n");
            System.err.print("                  1 - Packet received correctly\n");
            System.err.print("                  0 - Packet Lost\n");
            System.err.print("*-----------------------------------------------*\n\n");
            return;
        }
        int mode = Integer.parseInt(argv[0]);
        if (mode != 20 && mode != 30) {
            throw new IllegalArgumentException(String.format("Wrong mode %s, must be 20, or 30", argv[0]));
        }
        try {
            iFile = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(argv[1]))));
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new IllegalArgumentException(String.format("Cannot open input file %s", argv[1]));
        }
        try {
            eFile = Files.newOutputStream(Paths.get(argv[2]));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot open encoded file %s", argv[2]));
        }
        try {
            oFile = Files.newOutputStream(Paths.get(argv[3]));
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Cannot open decoded file %s", argv[3]));
        }
        if (argv.length == 5) {
            cFile = Files.newInputStream(Paths.get(argv[4]));
        } else {
            cFile = null;
        }

        // print info

        System.err.print("\n");
        System.err.print("*---------------------------------------------------*\n");
        System.err.print("*                                                   *\n");
        System.err.print("*      iLBC test program                            *\n");
        System.err.print("*                                                   *\n");
        System.err.print("*                                                   *\n");
        System.err.print("*---------------------------------------------------*\n");
        System.err.printf("\nMode           : %2d ms\n", mode);
        System.err.printf("Input file     : %s\n", argv[1]);
        System.err.printf("Encoded file   : %s\n", argv[2]);
        System.err.printf("Output file    : %s\n", argv[3]);
        if (argv.length == 5) {
            System.err.printf("Channel file   : %s\n", argv[4]);
        }
        System.err.print("\n");

        // Initialization

        initEncode(encoder, mode);
        initDecode(decoder, mode, 1);

        // Runtime statistics

        long startTime = System.currentTimeMillis();

        // loop over input blocks

        while (iFile.read(data, 0, 2 * encoder.blockl) == encoder.blockl * 2) {

            blockCount++;

            // encoding

            System.err.printf("--- Encoding block %d --- ", blockCount);
            len = encode(encoder, encoded_data, data);
            System.err.print("\n");

            // write byte file

            eFile.write(encoded_data, 0, 1 * len);

            // get channel data if provided
            if (argv.length == 5) {
                byte[] tmp = new byte[2];
                cFile.read(tmp, 0, 2 * 1);
                if ((pli = tmp[0] | tmp[1] << 8) != 0) { // TODO check endian
                    if ((pli != 0) && (pli != 1)) {
                        throw new IllegalStateException("Error in channel file");
                    }
                    if (pli == 0) {
                        // Packet loss -> remove info from frame
                        for (int xx = 0; xx < ILBCNOOFWORDS_MAX; xx++)
                            encoded_data[xx] = 0;
                        packetLossCount++;
                    }
                } else {
                    throw new IllegalStateException("Error. Channel file too short");
                }
            } else {
                pli = 1;
            }

            // decoding

            System.err.printf("--- Decoding block %d --- ", blockCount);

            len = decode(decoder, decoded_data, encoded_data, pli);
            System.err.print("\n");

            // write output file

            oFile.write(decoded_data, 0, 2 * len);
        }

        // Runtime statistics

        runtime = System.currentTimeMillis() - startTime;
        outTime = ((double) blockCount * (double) mode / 1000.0);
        System.out.printf("\n\nLength of speech file: %.1f s\n", outTime);
        System.out.printf("Packet loss          : %.1f%%\n", 100.0 * packetLossCount / blockCount);

        System.out.print("Time to run iLBC     :");
        System.out.printf(" %.1f s (%.1f %% of realtime)\n\n", (double) runtime, (100 * runtime / outTime));

        // close files

        iFile.close();
        eFile.close();
        oFile.close();
        if (argv.length == 5) {
            cFile.close();
        }
    }

//#endregion

//#region A.3. iLBC_encode.c

    /**
     * @return [o] Number of bytes encoded
     * @param encoder (i/o) Encoder instance
     * @param mode frame size mode
     */
    private static int initEncode(Encoder encoder, int mode) {
        encoder.mode = mode;
        if (mode == 30) {
            encoder.blockl = BLOCKL_30MS;
            encoder.nsub = NSUB_30MS;
            encoder.nasub = NASUB_30MS;
            encoder.lpc_n = LPC_N_30MS;
            encoder.no_of_bytes = NO_OF_BYTES_30MS;
            encoder.no_of_words = NO_OF_WORDS_30MS;

            encoder.state_short_len = STATE_SHORT_LEN_30MS;
            // ULP init
            encoder.ULP_inst = ulp_30msTbl;
        } else if (mode == 20) {
            encoder.blockl = BLOCKL_20MS;
            encoder.nsub = NSUB_20MS;
            encoder.nasub = NASUB_20MS;
            encoder.lpc_n = LPC_N_20MS;
            encoder.no_of_bytes = NO_OF_BYTES_20MS;
            encoder.no_of_words = NO_OF_WORDS_20MS;
            encoder.state_short_len = STATE_SHORT_LEN_20MS;
            // ULP init
            encoder.ULP_inst = ulp_20msTbl;
        } else {
            throw new IllegalArgumentException("mode: " + mode);
        }

        Arrays.fill(encoder.anaMem, 0);
        System.arraycopy(lsfmeanTbl, 0, encoder.lsfold, 0, LPC_FILTERORDER);
        System.arraycopy(lsfmeanTbl, 0, encoder.lsfdeqold, 0, LPC_FILTERORDER);
        for (int xx = 0; xx < LPC_LOOKBACK + BLOCKL_MAX; xx++)
            encoder.lpc_buffer[xx] = 0;
        for (int xx = 0; xx < 4; xx++)
            encoder.hpimem[xx] = 0;

        return encoder.no_of_bytes;
    }

    /**
     * main encoder function
     * @param bytes [o] encoded data bits iLBC
     * @param block [o] speech vector to encode
     * @param encoder  (i/o) the general encoder state
     */
    private static void iLBC_encode(byte[] bytes, double[] block, Encoder encoder) {

        double[] data = new double[BLOCKL_MAX];
        double[] residual = new double[BLOCKL_MAX], reverseResidual = new double[BLOCKL_MAX];

        int[] start = new int[1];
        int[] idxForMax = new int[1];
        int[] idxVec = new int[STATE_LEN];

        double[] reverseDecresidual = new double[BLOCKL_MAX], mem = new double[CB_MEML];
        int n, k, meml_gotten, Nfor, Nback, i;
        int[] pos = new int[1];
        int[] gain_index = new int[CB_NSTAGES * NASUB_MAX], extra_gain_index = new int[CB_NSTAGES];
        int[] cb_index = new int[CB_NSTAGES * NASUB_MAX], extra_cb_index = new int[CB_NSTAGES];
        int[] lsf_i = new int[LSF_NSPLIT * LPC_N_MAX];
        int[] /* double* */ pbytes = new int[1];
        int diff, start_pos;
        int[] state_first = new int[1];
        double en1, en2;
        int index, ulp;
        int[] firstpart = new int[1];
        int subcount, subframe;
        double[] weightState = new double[LPC_FILTERORDER];
        double[] syntdenum = new double[NSUB_MAX * (LPC_FILTERORDER + 1)];
        double[] weightdenum = new double[NSUB_MAX * (LPC_FILTERORDER + 1)];
        double[] decresidual = new double[BLOCKL_MAX];

        // high pass filtering of input signal if such is not done prior to
        // calling this function

        hpInput(block, encoder.blockl, data, encoder.hpimem);

        // otherwise simply copy

//      System.arraycopy(block, 0, data, 0, iLBCenc_inst.blockl);

        // LPC of hp filtered input data

        LPCencode(syntdenum, weightdenum, lsf_i, data, encoder);

        // inverse filter to get residual

        for (n = 0; n < encoder.nsub; n++) {
            anaFilter(data, n * SUBL, syntdenum, n * (LPC_FILTERORDER + 1), SUBL, residual, n * SUBL, encoder.anaMem);
        }

        // find state location

        start[0] = FrameClassify(encoder, residual);

        // check if state should be in first or last part of the two subframes

        diff = STATE_LEN - encoder.state_short_len;
        en1 = 0;
        index = (start[0] - 1) * SUBL;

        for (i = 0; i < encoder.state_short_len; i++) {
            en1 += residual[index + i] * residual[index + i];
        }
        en2 = 0;
        index = (start[0] - 1) * SUBL + diff;
        for (i = 0; i < encoder.state_short_len; i++) {
            en2 += residual[index + i] * residual[index + i];
        }

        if (en1 > en2) {
            state_first[0] = 1;
            start_pos = (start[0] - 1) * SUBL;
        } else {
            state_first[0] = 0;
            start_pos = (start[0] - 1) * SUBL + diff;
        }

        // scalar quantization of state

        StateSearchW(encoder, residual, start_pos, syntdenum, (start[0] - 1) * (LPC_FILTERORDER + 1), weightdenum, (start[0] - 1) * (LPC_FILTERORDER + 1), idxForMax, idxVec, encoder.state_short_len, state_first[0]);

        StateConstructW(idxForMax[0], idxVec, syntdenum, (start[0] - 1) * (LPC_FILTERORDER + 1), decresidual, start_pos, encoder.state_short_len);

        // predictive quantization in state

        if (state_first[0] != 0) { // put adaptive part in the end

            // setup memory

            for (int xx = 0; xx < CB_MEML - encoder.state_short_len; xx++)
                mem[xx] = 0;
            System.arraycopy(decresidual, start_pos, mem, CB_MEML - encoder.state_short_len, encoder.state_short_len);

            // encode sub-frames

            iCBSearch(encoder, extra_cb_index, 0, extra_gain_index, 0, residual, start_pos + encoder.state_short_len, mem, CB_MEML - stMemLTbl, stMemLTbl, diff, CB_NSTAGES, weightdenum, start[0] * (LPC_FILTERORDER + 1), weightState, 0);

            // construct decoded vector

            iCBConstruct(decresidual, start_pos + encoder.state_short_len, extra_cb_index, 0, extra_gain_index, 0, mem, CB_MEML - stMemLTbl, stMemLTbl, diff, CB_NSTAGES);

        } else { // put adaptive part in the beginning

            // create reversed vectors for prediction

            for (k = 0; k < diff; k++) {
                reverseResidual[k] = residual[(start[0] + 1) * SUBL - 1 - (k + encoder.state_short_len)];
            }

            // setup memory

            meml_gotten = encoder.state_short_len;
            for (k = 0; k < meml_gotten; k++) {
                mem[CB_MEML - 1 - k] = decresidual[start_pos + k];
            }
            for (int xx = 0; xx < CB_MEML - k; xx++)
                mem[xx] = 0;

            // encode sub-frames

            iCBSearch(encoder, extra_cb_index, 0, extra_gain_index, 0, reverseResidual, 0, mem, CB_MEML - stMemLTbl, stMemLTbl, diff, CB_NSTAGES, weightdenum, (start[0] - 1) * (LPC_FILTERORDER + 1), weightState, 0);

            // construct decoded vector

            iCBConstruct(reverseDecresidual, 0, extra_cb_index, 0, extra_gain_index, 0, mem, CB_MEML - stMemLTbl, stMemLTbl, diff, CB_NSTAGES);

            // get decoded residual from reversed vector

            for (k = 0; k < diff; k++) {
                decresidual[start_pos - 1 - k] = reverseDecresidual[k];
            }
        }

        // counter for predicted sub-frames

        subcount = 0;

        // forward prediction of sub-frames

        Nfor = encoder.nsub - start[0] - 1;

        if (Nfor > 0) {

            // setup memory

            for (int xx = 0; xx < CB_MEML - STATE_LEN; xx++)
                mem[xx] = 0;
            System.arraycopy(decresidual, (start[0] - 1) * SUBL, mem, CB_MEML - STATE_LEN, STATE_LEN);
            Arrays.fill(weightState, 0);

            // loop over sub-frames to encode

            for (subframe = 0; subframe < Nfor; subframe++) {

                // encode sub-frame

                iCBSearch(encoder, cb_index, subcount * CB_NSTAGES, gain_index, subcount * CB_NSTAGES, residual, (start[0] + 1 + subframe) * SUBL, mem, CB_MEML - memLfTbl[subcount], memLfTbl[subcount], SUBL, CB_NSTAGES, weightdenum, (start[0] + 1 + subframe) * (LPC_FILTERORDER + 1), weightState, subcount + 1);

                // construct decoded vector

                iCBConstruct(decresidual, (start[0] + 1 + subframe) * SUBL, cb_index, subcount * CB_NSTAGES, gain_index, subcount * CB_NSTAGES, mem, CB_MEML - memLfTbl[subcount], memLfTbl[subcount], SUBL, CB_NSTAGES);

                // update memory

                for (int xx = 0; xx < CB_MEML - SUBL; xx++) {
                    mem[xx] = mem[xx + SUBL];
                }
                System.arraycopy(decresidual, (start[0] + 1 + subframe) * SUBL, mem, CB_MEML - SUBL, SUBL);
                Arrays.fill(weightState, 0);

                subcount++;
            }
        }

        // backward prediction of sub-frames

        Nback = start[0] - 1;

        if (Nback > 0) {

            // create reverse order vectors

            for (n = 0; n < Nback; n++) {
                for (k = 0; k < SUBL; k++) {
                    int i1 = (start[0] - 1) * SUBL - 1 - n * SUBL - k;
                    reverseResidual[n * SUBL + k] = residual[i1];
                    reverseDecresidual[n * SUBL + k] = decresidual[i1];
                }
            }

            // setup memory

            meml_gotten = SUBL * (encoder.nsub + 1 - start[0]);

            if (meml_gotten > CB_MEML) {
                meml_gotten = CB_MEML;
            }
            for (k = 0; k < meml_gotten; k++) {
                mem[CB_MEML - 1 - k] = decresidual[(start[0] - 1) * SUBL + k];
            }
            for (int xx = 0; xx < CB_MEML - k; xx++)
                mem[xx] = 0;
            Arrays.fill(weightState, 0);

            // loop over sub-frames to encode

            for (subframe = 0; subframe < Nback; subframe++) {

                // encode sub-frame

                iCBSearch(encoder, cb_index, subcount * CB_NSTAGES, gain_index, subcount * CB_NSTAGES, reverseResidual, subframe * SUBL, mem, CB_MEML - memLfTbl[subcount], memLfTbl[subcount], SUBL, CB_NSTAGES, weightdenum, (start[0] - 2 - subframe) * (LPC_FILTERORDER + 1), weightState, subcount + 1);

                // construct decoded vector

                iCBConstruct(reverseDecresidual, subframe * SUBL, cb_index, subcount * CB_NSTAGES, gain_index, subcount * CB_NSTAGES, mem, CB_MEML - memLfTbl[subcount], memLfTbl[subcount], SUBL, CB_NSTAGES);

                // update memory

                for (int xx = 0; xx < CB_MEML - SUBL; xx++) {
                    mem[xx] = mem[xx + SUBL];
                }
                System.arraycopy(reverseDecresidual, subframe * SUBL, mem, CB_MEML - SUBL, SUBL);
                Arrays.fill(weightState, 0);

                subcount++;
            }

            // get decoded residual from reversed vector

            for (i = 0; i < SUBL * Nback; i++) {
                decresidual[SUBL * Nback - i - 1] = reverseDecresidual[i];
            }
        }
        // end encoding part

        // adjust index
        index_conv_enc(cb_index);

        // pack bytes

        pbytes[0] = 0; // bytes
        pos[0] = 0;

        // loop over the 3 ULP classes

        for (ulp = 0; ulp < 3; ulp++) {

            // LSF
            for (k = 0; k < LSF_NSPLIT * encoder.lpc_n; k++) {
                packsplit(lsf_i, k, firstpart, lsf_i, k, encoder.ULP_inst.lsf_bits[k][ulp],
                        encoder.ULP_inst.lsf_bits[k][ulp] +
                                encoder.ULP_inst.lsf_bits[k][ulp + 1] +
                                encoder.ULP_inst.lsf_bits[k][ulp + 2]);
                dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.lsf_bits[k][ulp], pos);
            }

            // Start block info

            packsplit(start, 0, firstpart, start, 0, encoder.ULP_inst.start_bits[ulp],
                    encoder.ULP_inst.start_bits[ulp] +
                            encoder.ULP_inst.start_bits[ulp + 1] +
                            encoder.ULP_inst.start_bits[ulp + 2]);
            dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.start_bits[ulp], pos);

            packsplit(state_first, 0, firstpart, state_first, 0, encoder.ULP_inst.startfirst_bits[ulp],
                    encoder.ULP_inst.startfirst_bits[ulp] +
                            encoder.ULP_inst.startfirst_bits[ulp + 1] +
                            encoder.ULP_inst.startfirst_bits[ulp + 2]);
            dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.startfirst_bits[ulp], pos);

            packsplit(idxForMax, 0, firstpart, idxForMax, 0, encoder.ULP_inst.scale_bits[ulp],
                    encoder.ULP_inst.scale_bits[ulp] +
                            encoder.ULP_inst.scale_bits[ulp + 1] +
                            encoder.ULP_inst.scale_bits[ulp + 2]);
            dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.scale_bits[ulp], pos);

            for (k = 0; k < encoder.state_short_len; k++) {
                packsplit(idxVec, k, firstpart, idxVec, k, encoder.ULP_inst.state_bits[ulp],
                        encoder.ULP_inst.state_bits[ulp] +
                                encoder.ULP_inst.state_bits[ulp + 1] +
                                encoder.ULP_inst.state_bits[ulp + 2]);
                dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.state_bits[ulp], pos);
            }

            // 23/22 (20ms/30ms) sample block

            for (k = 0; k < CB_NSTAGES; k++) {
                packsplit(extra_cb_index, k, firstpart, extra_cb_index, k, encoder.ULP_inst.extra_cb_index[k][ulp],
                        encoder.ULP_inst.extra_cb_index[k][ulp] +
                                encoder.ULP_inst.extra_cb_index[k][ulp + 1] +
                                encoder.ULP_inst.extra_cb_index[k][ulp + 2]);
                dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.extra_cb_index[k][ulp], pos);
            }

            for (k = 0; k < CB_NSTAGES; k++) {
                packsplit(extra_gain_index, k, firstpart, extra_gain_index, k, encoder.ULP_inst.extra_cb_gain[k][ulp],
                        encoder.ULP_inst.extra_cb_gain[k][ulp] +
                                encoder.ULP_inst.extra_cb_gain[k][ulp + 1] +
                                encoder.ULP_inst.extra_cb_gain[k][ulp + 2]);
                dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.extra_cb_gain[k][ulp], pos);
            }

            // The two/four (20ms/30ms) 40 sample sub-blocks

            for (i = 0; i < encoder.nasub; i++) {
                for (k = 0; k < CB_NSTAGES; k++) {
                    packsplit(cb_index, i * CB_NSTAGES + k, firstpart, cb_index, i * CB_NSTAGES + k,
                            encoder.ULP_inst.cb_index[i][k][ulp],
                            encoder.ULP_inst.cb_index[i][k][ulp] +
                                    encoder.ULP_inst.cb_index[i][k][ulp + 1] +
                                    encoder.ULP_inst.cb_index[i][k][ulp + 2]);
                    dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.cb_index[i][k][ulp], pos);
                }
            }

            for (i = 0; i < encoder.nasub; i++) {
                for (k = 0; k < CB_NSTAGES; k++) {
                    packsplit(gain_index, i * CB_NSTAGES + k, firstpart, gain_index, i * CB_NSTAGES + k,
                            encoder.ULP_inst.cb_gain[i][k][ulp],
                            encoder.ULP_inst.cb_gain[i][k][ulp] +
                                    encoder.ULP_inst.cb_gain[i][k][ulp + 1] +
                                    encoder.ULP_inst.cb_gain[i][k][ulp + 2]);
                    dopack(bytes, pbytes, firstpart[0], encoder.ULP_inst.cb_gain[i][k][ulp], pos);
                }
            }
        }

        // set the last bit to zero (otherwise the decoder will treat it as a
        // lost frame)
        dopack(bytes, pbytes, 0, 1, pos);
    }

//#endregion

//#region A.5. iLBC_decode.c

    /**
     * @return Number of decoded samples
     * @param decoder (i/o) Decoder instance
     * @param mode frame size mode
     * @param use_enhancer 1 to use enhancer 0 to run without enhancer
     */
    static int initDecode(Decoder decoder, int mode, int use_enhancer) {

        decoder.mode = mode;

        if (mode == 30) {
            decoder.blockl = BLOCKL_30MS;
            decoder.nsub = NSUB_30MS;
            decoder.nasub = NASUB_30MS;
            decoder.lpc_n = LPC_N_30MS;
            decoder.no_of_bytes = NO_OF_BYTES_30MS;
            decoder.no_of_words = NO_OF_WORDS_30MS;
            decoder.state_short_len = STATE_SHORT_LEN_30MS;
            // ULP init
            decoder.ULP_inst = ulp_30msTbl;
        } else if (mode == 20) {
            decoder.blockl = BLOCKL_20MS;
            decoder.nsub = NSUB_20MS;
            decoder.nasub = NASUB_20MS;
            decoder.lpc_n = LPC_N_20MS;
            decoder.no_of_bytes = NO_OF_BYTES_20MS;
            decoder.no_of_words = NO_OF_WORDS_20MS;
            decoder.state_short_len = STATE_SHORT_LEN_20MS;
            // ULP init
            decoder.ULP_inst = ulp_20msTbl;
        } else {
            throw new IllegalArgumentException("mode: " + mode);
        }

        Arrays.fill(decoder.syntMem, 0);
        System.arraycopy(lsfmeanTbl, 0, decoder.lsfdeqold, 0, LPC_FILTERORDER);

        for (int xx = 0; xx < (LPC_FILTERORDER + 1) * NSUB_MAX; xx++)
            decoder.old_syntdenum[xx] = 0;
        for (int i = 0; i < NSUB_MAX; i++)
            decoder.old_syntdenum[i * (LPC_FILTERORDER + 1)] = 1.0;

        decoder.last_lag = 20;

        decoder.prevLag = 120;
        decoder.per = 0.0;
        decoder.consPLICount = 0;
        decoder.prevPLI = 0;
        decoder.prevLpc[0] = 1.0;
        for (int xx = 0; xx < LPC_FILTERORDER; xx++)
            decoder.prevLpc[xx + 1] = 0;
        for (int xx = 0; xx < BLOCKL_MAX; xx++)
            decoder.prevResidual[xx] = 0;
        decoder.seed = 777;

        for (int xx = 0; xx < 4; xx++)
            decoder.hpomem[xx] = 0;

        decoder.use_enhancer = use_enhancer;
        Arrays.fill(decoder.enh_buf, 0);
        Arrays.fill(decoder.enh_period, 40.0);

        decoder.prev_enh_pl = 0;

        return decoder.blockl;
    }

    /**
     * frame residual decoder function (subrutine to iLBC_decode)
     *
     * @param decoder (i/o) the decoder state structure
     * @param decresidual [o] decoded residual frame
     * @param start location of start state
     * @param idxForMax codebook index for the maximum value
     * @param idxVec codebook indexes for the samples in the start state
     * @param syntdenum the decoded synthesis filter coefficients
     * @param cb_index the indexes for the adaptive codebook
     * @param gain_index the indexes for the corresponding gains
     * @param extra_cb_index the indexes for the adaptive codebook part of
     *            start state
     * @param extra_gain_index the indexes for the corresponding gains
     * @param state_first 1 if non adaptive part of start state comes first
     *            0 if that part comes last
     */
    private static void decode(Decoder decoder, double[] decresidual, int start, int idxForMax, int[] idxVec,
                        double[] syntdenum, int[] cb_index, int[] gain_index, int[] extra_cb_index,
                        int[] extra_gain_index, int state_first) {
        double[] reverseDecresidual = new double[BLOCKL_MAX], mem = new double[CB_MEML];
        int k, meml_gotten, Nfor, Nback, i;
        int diff, start_pos;
        int subcount, subframe;

        diff = STATE_LEN - decoder.state_short_len;

        if (state_first == 1) {
            start_pos = (start - 1) * SUBL;
        } else {
            start_pos = (start - 1) * SUBL + diff;
        }

        // decode scalar part of start state

        StateConstructW(idxForMax, idxVec, syntdenum, (start - 1) * (LPC_FILTERORDER + 1), decresidual,
                start_pos, decoder.state_short_len);

        if (state_first != 0) { // put adaptive part in the end

            // setup memory

            for (int xx = 0; xx < CB_MEML - decoder.state_short_len; xx++)
                mem[xx] = 0;
            System.arraycopy(decresidual, start_pos, mem, CB_MEML - decoder.state_short_len, decoder.state_short_len);

            // construct decoded vector

            iCBConstruct(decresidual, start_pos + decoder.state_short_len, extra_cb_index, 0,
                    extra_gain_index, 0, mem, CB_MEML - stMemLTbl, stMemLTbl, diff, CB_NSTAGES);

        } else { // put adaptive part in the beginning

            // create reversed vectors for prediction

            for (k = 0; k < diff; k++) {
                reverseDecresidual[k] = decresidual[(start + 1) * SUBL - 1 - (k + decoder.state_short_len)];
            }

            // setup memory

            meml_gotten = decoder.state_short_len;
            for (k = 0; k < meml_gotten; k++) {
                mem[CB_MEML - 1 - k] = decresidual[start_pos + k];
            }
            for (int xx = 0; xx < CB_MEML - k; xx++)
                mem[xx] = 0;

            // construct decoded vector

            iCBConstruct(reverseDecresidual, 0, extra_cb_index, 0, extra_gain_index, 0,
                    mem, CB_MEML - stMemLTbl, stMemLTbl, diff, CB_NSTAGES);

            // get decoded residual from reversed vector

            for (k = 0; k < diff; k++) {
                decresidual[start_pos - 1 - k] = reverseDecresidual[k];
            }
        }

        // counter for predicted sub-frames

        subcount = 0;

        // forward prediction of sub-frames

        Nfor = decoder.nsub - start - 1;

        if (Nfor > 0) {

            // setup memory

            for (int xx = 0; xx < CB_MEML - STATE_LEN; xx++)
                mem[xx] = 0;
            System.arraycopy(decresidual, (start - 1) * SUBL, mem, CB_MEML - STATE_LEN, STATE_LEN);

            // loop over sub-frames to encode

            for (subframe = 0; subframe < Nfor; subframe++) {

                // construct decoded vector

                iCBConstruct(decresidual, (start + 1 + subframe) * SUBL, cb_index, subcount * CB_NSTAGES, gain_index, subcount * CB_NSTAGES, mem, CB_MEML - memLfTbl[subcount], memLfTbl[subcount], SUBL, CB_NSTAGES);

                // update memory

                for (int xx = 0; xx < CB_MEML - SUBL; xx++) {
                    mem[xx] = mem[xx + SUBL];
                }
                System.arraycopy(decresidual, (start + 1 + subframe) * SUBL, mem, CB_MEML - SUBL, SUBL);

                subcount++;
            }
        }

        // backward prediction of sub-frames

        Nback = start - 1;

        if (Nback > 0) {

            // setup memory

            meml_gotten = SUBL * (decoder.nsub + 1 - start);

            if (meml_gotten > CB_MEML) {
                meml_gotten = CB_MEML;
            }
            for (k = 0; k < meml_gotten; k++) {
                mem[CB_MEML - 1 - k] = decresidual[(start - 1) * SUBL + k];
            }
            for (int xx = 0; xx < CB_MEML - k; xx++)
                mem[xx] = 0;

            // loop over subframes to decode

            for (subframe = 0; subframe < Nback; subframe++) {

                // construct decoded vector

                iCBConstruct(reverseDecresidual, subframe * SUBL, cb_index, subcount * CB_NSTAGES, gain_index, subcount * CB_NSTAGES, mem, CB_MEML - memLfTbl[subcount], memLfTbl[subcount], SUBL, CB_NSTAGES);

                // update memory

                for (int xx = 0; xx < CB_MEML - SUBL; xx++) {
                    mem[xx] = mem[xx + SUBL];
                }
                System.arraycopy(reverseDecresidual, subframe * SUBL, mem, CB_MEML - SUBL, SUBL);

                subcount++;
            }

            // get decoded residual from reversed vector

            for (i = 0; i < SUBL * Nback; i++)
                decresidual[SUBL * Nback - i - 1] = reverseDecresidual[i];
        }
    }

    /**
     * main decoder function.
     *
     * @param decblock [o] decoded signal block
     * @param bytes encoded signal bits
     * @param decoder (i/o) the decoder state structure
     * @param mode 0: bad packet, PLC, 1: normal
     */
    private static void iLBC_decode(double[] decblock, byte[] bytes, Decoder decoder, int mode) {
        double[] data = new double[BLOCKL_MAX];
        double[] lsfdeq = new double[LPC_FILTERORDER * LPC_N_MAX];
        double[] PLCresidual = new double[BLOCKL_MAX], PLClpc = new double[LPC_FILTERORDER + 1];
        double[] zeros = new double[BLOCKL_MAX], one = new double[LPC_FILTERORDER + 1];
        int k, i;
        int[] start = new int[1], idxForMax = new int[1];
        int[] pos = new int[1], lastpart = new int[1];
        int ulp;
        int lag, ilag;
        double cc, maxcc;
        int[] idxVec = new int[STATE_LEN];
        int check;
        int[] gain_index = new int[NASUB_MAX * CB_NSTAGES], extra_gain_index = new int[CB_NSTAGES];
        int[] cb_index = new int[CB_NSTAGES * NASUB_MAX], extra_cb_index = new int[CB_NSTAGES];
        int[] lsf_i = new int[LSF_NSPLIT * LPC_N_MAX];
        int[] state_first = new int[1];
        int[] last_bit = new int[1];
        int[] /* double* */ pbytes = new int[1];
        double[] weightdenum = new double[(LPC_FILTERORDER + 1) * NSUB_MAX];
        int order_plus_one;
        double[] syntdenum = new double[NSUB_MAX * (LPC_FILTERORDER + 1)];
        double[] decresidual = new double[BLOCKL_MAX];

        if (mode > 0) { // the data are good

            // decode data

            pbytes[0] = 0; // bytes
            pos[0] = 0;

            // Set everything to zero before decoding

            for (k = 0; k < LSF_NSPLIT * LPC_N_MAX; k++) {
                lsf_i[k] = 0;
            }
            start[0] = 0;
            state_first[0] = 0;
            idxForMax[0] = 0;
            for (k = 0; k < decoder.state_short_len; k++) {
                idxVec[k] = 0;
            }
            for (k = 0; k < CB_NSTAGES; k++) {
                extra_cb_index[k] = 0;
            }
            for (k = 0; k < CB_NSTAGES; k++) {
                extra_gain_index[k] = 0;
            }
            for (i = 0; i < decoder.nasub; i++) {
                for (k = 0; k < CB_NSTAGES; k++) {
                    cb_index[i * CB_NSTAGES + k] = 0;
                }
            }
            for (i = 0; i < decoder.nasub; i++) {
                for (k = 0; k < CB_NSTAGES; k++) {
                    gain_index[i * CB_NSTAGES + k] = 0;
                }
            }

            // loop over ULP classes

            for (ulp = 0; ulp < 3; ulp++) {

                // LSF
                for (k = 0; k < LSF_NSPLIT * decoder.lpc_n; k++) {
                    unpack(bytes, pbytes, lastpart, decoder.ULP_inst.lsf_bits[k][ulp], pos);
                    packcombine(lsf_i, k, lastpart[0], decoder.ULP_inst.lsf_bits[k][ulp]);
                }

                // Start block info

                unpack(bytes, pbytes, lastpart, decoder.ULP_inst.start_bits[ulp], pos);
                packcombine(start, 0, lastpart[0], decoder.ULP_inst.start_bits[ulp]);

                unpack(bytes, pbytes, lastpart, decoder.ULP_inst.startfirst_bits[ulp], pos);
                packcombine(state_first, 0, lastpart[0], decoder.ULP_inst.startfirst_bits[ulp]);

                unpack(bytes, pbytes, lastpart, decoder.ULP_inst.scale_bits[ulp], pos);
                packcombine(idxForMax, 0, lastpart[0], decoder.ULP_inst.scale_bits[ulp]);

                for (k = 0; k < decoder.state_short_len; k++) {
                    unpack(bytes, pbytes, lastpart, decoder.ULP_inst.state_bits[ulp], pos);
                    packcombine(idxVec, k, lastpart[0], decoder.ULP_inst.state_bits[ulp]);
                }

                // 23/22 (20ms/30ms) sample block

                for (k = 0; k < CB_NSTAGES; k++) {
                    unpack(bytes, pbytes, lastpart, decoder.ULP_inst.extra_cb_index[k][ulp], pos);
                    packcombine(extra_cb_index, k, lastpart[0], decoder.ULP_inst.extra_cb_index[k][ulp]);
                }
                for (k = 0; k < CB_NSTAGES; k++) {
                    unpack(bytes, pbytes, lastpart, decoder.ULP_inst.extra_cb_gain[k][ulp], pos);
                    packcombine(extra_gain_index, k, lastpart[0], decoder.ULP_inst.extra_cb_gain[k][ulp]);
                }

                // The two/four (20ms/30ms) 40 sample sub-blocks

                for (i = 0; i < decoder.nasub; i++) {
                    for (k = 0; k < CB_NSTAGES; k++) {
                        unpack(bytes, pbytes, lastpart, decoder.ULP_inst.cb_index[i][k][ulp], pos);
                        packcombine(cb_index, i * CB_NSTAGES + k, lastpart[0], decoder.ULP_inst.cb_index[i][k][ulp]);
                    }
                }

                for (i = 0; i < decoder.nasub; i++) {
                    for (k = 0; k < CB_NSTAGES; k++) {
                        unpack(bytes, pbytes, lastpart, decoder.ULP_inst.cb_gain[i][k][ulp], pos);
                        packcombine(gain_index, i * CB_NSTAGES + k, lastpart[0], decoder.ULP_inst.cb_gain[i][k][ulp]);
                    }
                }
            }
            // Extract last bit. If it is 1 this indicates an empty/lost frame
            unpack(bytes, pbytes, last_bit, 1, pos);

            // Check for bit errors or empty/lost frames
            if (start[0] < 1)
                mode = 0;
            if (decoder.mode == 20 && start[0] > 3)
                mode = 0;
            if (decoder.mode == 30 && start[0] > 5)
                mode = 0;
            if (last_bit[0] == 1)
                mode = 0;

            if (mode == 1) { // No bit errors was detected, continue decoding

                // adjust index
                index_conv_dec(cb_index);

                // decode the lsf

                SimplelsfDEQ(lsfdeq, lsf_i, decoder.lpc_n);
                check = LSF_check(lsfdeq, LPC_FILTERORDER, decoder.lpc_n);
                DecoderInterpolateLSF(syntdenum, weightdenum, lsfdeq, LPC_FILTERORDER, decoder);

                decode(decoder, decresidual, start[0], idxForMax[0], idxVec, syntdenum, cb_index, gain_index, extra_cb_index, extra_gain_index, state_first[0]);

                // preparing the plc for a future loss!

                doThePLC(PLCresidual, PLClpc, 0, decresidual, syntdenum, (LPC_FILTERORDER + 1) * (decoder.nsub - 1), (decoder).last_lag, decoder);

                System.arraycopy(PLCresidual, 0, decresidual, 0, decoder.blockl);
            }
        }

        if (mode == 0) {
            // the data is bad (either a PLC call was made or a severe bit error
            // was detected)

            // packet loss conceal

            one[0] = 1;
            for (int xx = 0; xx < LPC_FILTERORDER; xx++) {
                one[xx + 1] = 0;
            }

            start[0] = 0;

            doThePLC(PLCresidual, PLClpc, 1, zeros, one, 0, (decoder).last_lag, decoder);
            System.arraycopy(PLCresidual, 0, decresidual, 0, decoder.blockl);

            order_plus_one = LPC_FILTERORDER + 1;
            for (i = 0; i < decoder.nsub; i++) {
                System.arraycopy(PLClpc, 0, syntdenum, i * order_plus_one, order_plus_one);
            }
        }

        if (decoder.use_enhancer == 1) {

            // post filtering

            decoder.last_lag = enhancerInterface(data, decresidual, decoder);

            // synthesis filtering

            if (decoder.mode == 20) {
                // Enhancer has 40 samples delay
                i = 0;
                syntFilter(data, i * SUBL, decoder.old_syntdenum, (i + decoder.nsub - 1) * (LPC_FILTERORDER + 1), SUBL, decoder.syntMem);

                for (i = 1; i < decoder.nsub; i++) {
                    syntFilter(data, i * SUBL, syntdenum, (i - 1) * (LPC_FILTERORDER + 1), SUBL, decoder.syntMem);
                }
            } else if (decoder.mode == 30) {
                // Enhancer has 80 samples delay
                for (i = 0; i < 2; i++) {
                    syntFilter(data, i * SUBL, decoder.old_syntdenum, (i + decoder.nsub - 2) * (LPC_FILTERORDER + 1), SUBL, decoder.syntMem);
                }
                for (i = 2; i < decoder.nsub; i++) {
                    syntFilter(data, i * SUBL, syntdenum, (i - 2) * (LPC_FILTERORDER + 1), SUBL, decoder.syntMem);
                }
            }

        } else {

            // Find last lag
            lag = 20;
            maxcc = xCorrCoef(decresidual, BLOCKL_MAX - ENH_BLOCKL, decresidual, BLOCKL_MAX - ENH_BLOCKL - lag, ENH_BLOCKL);

            for (ilag = 21; ilag < 120; ilag++) {
                cc = xCorrCoef(decresidual, BLOCKL_MAX - ENH_BLOCKL, decresidual, BLOCKL_MAX - ENH_BLOCKL - ilag, ENH_BLOCKL);

                if (cc > maxcc) {
                    maxcc = cc;
                    lag = ilag;
                }
            }
            decoder.last_lag = lag;

            // copy data and run synthesis filter

            System.arraycopy(decresidual, 0, data, 0, decoder.blockl);
            for (i = 0; i < decoder.nsub; i++) {
                syntFilter(data, i * SUBL, syntdenum, i * (LPC_FILTERORDER + 1), SUBL, decoder.syntMem);
            }
        }

        // high pass filtering on output if desired, otherwise copy to out

        hpOutput(data, decoder.blockl, decblock, decoder.hpomem);

//      System.arraycopy(data, 0, decblock, 0, iLBCdec_inst.blockl);

        System.arraycopy(syntdenum, 0, decoder.old_syntdenum, 0, decoder.nsub * (LPC_FILTERORDER + 1));

        decoder.prev_enh_pl = 0;

        if (mode == 0) { // PLC was used
            decoder.prev_enh_pl = 1;
        }
    }

//#endregion

//region A.10. anaFilter.c

    /**
     * LP analysis filter.
     *
     * @param In Signal to be filtered
     * @param a LP parameters
     * @param len Length of signal
     * @param Out [o] Filtered signal
     * @param mem (i/o) Filter state
     */
    private static void anaFilter(double[] In, int inP, double[] a, int aP, int len, double[] Out, int outP, double[] mem) {
        int /* double* */ po, pi, pm, pa;

        po = outP; // Out

        // Filter first part using memory from past

        for (int i = 0; i < LPC_FILTERORDER; i++) {
            pi = inP + i; // In
            pm = LPC_FILTERORDER - 1; // mem
            pa = aP; // a
            Out[po] = 0.0;

            for (int j = 0; j <= i; j++) {
                Out[po] += a[pa++] * In[pi--];
            }
            for (int j = i + 1; j < LPC_FILTERORDER + 1; j++) {
                Out[po] += a[pa++] * mem[pm--];
            }
            po++;
        }

        // Filter last part where the state is entirely in the input vector

        for (int i = LPC_FILTERORDER; i < len; i++) {
            pi = inP + i; // In
            pa = aP; // a
            Out[po] = 0.0;
            for (int j = 0; j < LPC_FILTERORDER + 1; j++) {
                Out[po] += a[pa++] * In[pi--];
            }
            po++;
        }

        // Update state vector

        System.arraycopy(In, inP + len - LPC_FILTERORDER, mem, 0, LPC_FILTERORDER);
    }

//#endregion

//region A.12. createCB.c

    /**
     * Construct an additional codebook vector by filtering the initial codebook
     * buffer. This vector is then used to expand the codebook with an
     * additional section.
     *
     * @param cbvectors [o] Codebook vectors for the higher section
     * @param mem Buffer to create codebook vector from
     * @param lMem Length of buffer
     */
    private static void filteredCBvecs(double[] cbvectors, double[] mem, int memP, int lMem) {
        int /* double* */ pp, pp1;
        double[] tempbuff2 = new double[CB_MEML + CB_FILTERLEN];
        int /* double* */ pos;

        for (int xx = 0; xx < CB_HALFFILTERLEN - 1; xx++) {
            tempbuff2[xx] = 0;
        }
        System.arraycopy(mem, memP, tempbuff2, CB_HALFFILTERLEN - 1, lMem);
        for (int xx = 0; xx < CB_HALFFILTERLEN + 1; xx++) {
            tempbuff2[xx + lMem + CB_HALFFILTERLEN - 1] = 0;
        }

        // Create codebook vector for higher section by filtering

        // do filtering
        pos = 0; // cbvectors
        for (int xx = 0; xx < lMem; xx++) {
            cbvectors[pos + xx] = 0;
        }
        for (int k = 0; k < lMem; k++) {
            pp = k; // tempbuff2
            pp1 = CB_FILTERLEN - 1; // cbfiltersTbl
            for (int j = 0; j < CB_FILTERLEN; j++) {
                cbvectors[pos] += (tempbuff2[pp++]) * (cbfiltersTbl[pp1--]);
            }
            pos++;
        }
    }

    /**
     * Search the augmented part of the codebook to find the best measure.
     *
     * @param low Start index for the search
     * @param high End index for the search
     * @param stage Current stage
     * @param startIndex Codebook index for the first aug vector
     * @param target Target vector for encoding
     * @param buffer Pointer to the end of the buffer for augmented codebook
     *            construction
     * @param max_measure (i/o) Currently maximum measure
     * @param best_index [o] Currently the best index
     * @param gain [o] Currently the best gain
     * @param energy [o] Energy of augmented codebook vectors
     * @param invenergy [o] Inv energy of augmented codebook vectors
     */
    private static void searchAugmentedCB(int low, int high, int stage, int startIndex, double[] target, double[] buffer, int bufferP, double[] max_measure, int[] best_index, double[] gain, double[] energy, double[] invenergy) {
        int icount, ilow, j, tmpIndex;
        int /* double* */ pp, ppo, ppi, ppe;
        double crossDot, alfa;
        double weighted, measure, nrjRecursive;
        double ftmp;

        // Compute the energy for the first (low-5) noninterpolated samples
        nrjRecursive = 0.0;
        pp = bufferP - low + 1; // buffer
        for (j = 0; j < low - 5; j++) {
            nrjRecursive += buffer[pp] * buffer[pp];
            pp++;
        }
        ppe = bufferP - low; // buffer

        for (icount = low; icount <= high; icount++) {

            // Index of the codebook vector used for retrieving energy values
            tmpIndex = startIndex + icount - 20;

            ilow = icount - 4;

            // Update the energy recursively to save complexity
            nrjRecursive = nrjRecursive + buffer[ppe] * buffer[ppe];
            ppe--;
            energy[tmpIndex] = nrjRecursive;

            // Compute cross dot product for the first (low-5) samples

            crossDot = 0.0;
            pp = bufferP - icount; // buffer
            for (j = 0; j < ilow; j++) {
                crossDot += target[j] * buffer[pp++];
            }

            // interpolation
            alfa = 0.2;
            ppo = bufferP - 4; // buffer
            ppi = bufferP - icount - 4; // buffer
            for (j = ilow; j < icount; j++) {
                weighted = (1.0 - alfa) * buffer[ppo] + alfa * buffer[ppi];
                ppo++;
                ppi++;
                energy[tmpIndex] += weighted * weighted;
                crossDot += target[j] * weighted;
                alfa += 0.2;
            }

            // Compute energy and cross dot product for the remaining samples
            pp = bufferP - icount; // buffer
            for (j = icount; j < SUBL; j++) {
                energy[tmpIndex] += buffer[pp] * buffer[pp];
                crossDot += target[j] * buffer[pp++];
            }

            if (energy[tmpIndex] > 0.0) {
                invenergy[tmpIndex] = 1.0 / (energy[tmpIndex] + EPS);
            } else {
                invenergy[tmpIndex] = 0.0;
            }

            if (stage == 0) {
                measure = -10000000.0;

                if (crossDot > 0.0) {
                    measure = crossDot * crossDot * invenergy[tmpIndex];
                }
            } else {
                measure = crossDot * crossDot * invenergy[tmpIndex];
            }

            // check if measure is better
            ftmp = crossDot * invenergy[tmpIndex];

            if ((measure > max_measure[0]) && (Math.abs(ftmp) < CB_MAXGAIN)) {

                best_index[0] = tmpIndex;
                max_measure[0] = measure;
                gain[0] = ftmp;
            }
        }
    }

    /**
     * Recreate a specific codebook vector from the augmented part.
     *
     * @param index Index for the augmented vector to be created
     * @param buffer Pointer to the end of the buffer for augmented codebook
     *            construction
     * @param cbVec [o] The construced codebook vector
     */
    private static void createAugmentedVec(int index, double[] buffer, int bufferP, double[] cbVec) {
        int /* double* */ pp, ppo, ppi;

        int ilow = index - 5;

        // copy the first noninterpolated part

        pp = bufferP - index; // buffer
        System.arraycopy(buffer, pp, cbVec, 0, index);

        // interpolation

        double alfa1 = 0.2;
        double alfa = 0.0;
        ppo = bufferP - 5; // buffer
        ppi = bufferP - index - 5; // buffer
        for (int j = ilow; j < index; j++) {
            double weighted = (1.0 - alfa) * buffer[ppo] + alfa * buffer[ppi];
            ppo++;
            ppi++;
            cbVec[j] = weighted;
            alfa += alfa1;
        }

        // copy the second noninterpolated part

        pp = bufferP - index; // buffer
        System.arraycopy(buffer, pp, cbVec, index, SUBL - index);
    }

//#endregion

//#region A.14. doCPLC.c

    /**
     * Compute cross correlation and pitch gain for pitch prediction of last
     * subframe at given lag.
     *
     * @param cc [o] cross correlation coefficient
     * @param gc [o] gain
     * @param pm
     * @param buffer signal buffer
     * @param lag pitch lag
     * @param bLen length of buffer
     * @param sRange correlation search length
     */
    private static void compCorr(double[] cc, double[] gc, double[] pm, double[] buffer, int lag, int bLen, int sRange) {

        // Guard against getting outside buffer
        if ((bLen - sRange - lag) < 0) {
            sRange = bLen - lag;
        }

        double ftmp1 = 0.0;
        double ftmp2 = 0.0;
        double ftmp3 = 0.0;
        for (int i = 0; i < sRange; i++) {
            ftmp1 += buffer[bLen - sRange + i] * buffer[bLen - sRange + i - lag];
            ftmp2 += buffer[bLen - sRange + i - lag] * buffer[bLen - sRange + i - lag];
            ftmp3 += buffer[bLen - sRange + i] * buffer[bLen - sRange + i];
        }

        if (ftmp2 > 0.0) {
            cc[0] = ftmp1 * ftmp1 / ftmp2;
            gc[0] = Math.abs(ftmp1 / ftmp2);
            pm[0] = Math.abs(ftmp1) / (Math.sqrt(ftmp2) * Math.sqrt(ftmp3));
        } else {
            cc[0] = 0.0;
            gc[0] = 0.0;
            pm[0] = 0.0;
        }
    }

    /**
     * Packet loss concealment routine. Conceals a residual signal and LP
     * parameters. If no packet loss, update state.
     *
     * @param PLCresidual [o] concealed residual
     * @param PLClpc [o] concealed LP parameters
     * @param PLI packet loss indicator 0 - no PL, 1 = PL
     * @param decresidual decoded residual
     * @param lpc decoded LPC (only used for no PL)
     * @param inlag pitch lag
     * @param iLBCdec_inst (i/o) decoder instance
     */
    private static void doThePLC(double[] PLCresidual, double[] PLClpc, int PLI, double[] decresidual, double[] lpc, int lpcP, int inlag, Decoder iLBCdec_inst) {
        int lag = 20, randlag;
        double[] gain = new double[1];
        double[] maxcc = new double[1];
        double use_gain;
        double[] gain_comp = new double[1], maxcc_comp = new double[1], per = new double[1];
        double[] max_per = new double[1];
        int i, pick, use_lag;
        double ftmp, pitchfact, energy;
        double[] randvec = new double[BLOCKL_MAX];

        // Packet Loss

        if (PLI == 1) {

            iLBCdec_inst.consPLICount += 1;

            // if previous frame not lost, determine pitch pred. gain

            if (iLBCdec_inst.prevPLI != 1) {

                // Search around the previous lag to find the best pitch period

                lag = inlag - 3;
                compCorr(maxcc, gain, max_per, iLBCdec_inst.prevResidual, lag, iLBCdec_inst.blockl, 60);
                for (i = inlag - 2; i <= inlag + 3; i++) {
                    compCorr(maxcc_comp, gain_comp, per, iLBCdec_inst.prevResidual, i, iLBCdec_inst.blockl, 60);

                    if (maxcc_comp[0] > maxcc[0]) {
                        maxcc[0] = maxcc_comp[0];

                        gain[0] = gain_comp[0];
                        lag = i;
                        max_per[0] = per[0];
                    }
                }

            } else { // previous frame lost, use recorded lag and periodicity
                lag = iLBCdec_inst.prevLag;
                max_per[0] = iLBCdec_inst.per;
            }

            // downscaling

            use_gain = 1.0;
            if (iLBCdec_inst.consPLICount * iLBCdec_inst.blockl > 320) {
                use_gain = 0.9;
            } else if (iLBCdec_inst.consPLICount * iLBCdec_inst.blockl > 2 * 320) {
                use_gain = 0.7;
            } else if (iLBCdec_inst.consPLICount * iLBCdec_inst.blockl > 3 * 320) {
                use_gain = 0.5;
            } else if (iLBCdec_inst.consPLICount * iLBCdec_inst.blockl > 4 * 320) {
                use_gain = 0.0;
            }

            // mix noise and pitch repeatition
            ftmp = Math.sqrt(max_per[0]);
            if (ftmp > 0.7) {
                pitchfact = 1.0;
            } else if (ftmp > 0.4) {
                pitchfact = (ftmp - 0.4) / (0.7 - 0.4);
            } else {
                pitchfact = 0.0;
            }

            // avoid repetition of same pitch cycle
            use_lag = lag;
            if (lag < 80) {
                use_lag = 2 * lag;
            }

            // compute concealed residual

            energy = 0.0;
            for (i = 0; i < iLBCdec_inst.blockl; i++) {

                // noise component

                iLBCdec_inst.seed = (iLBCdec_inst.seed * 69069L + 1) & (0x8000_0000L - 1);
                randlag = (int) (50 + iLBCdec_inst.seed % 70);
                pick = i - randlag;

                if (pick < 0) {
                    randvec[i] = iLBCdec_inst.prevResidual[iLBCdec_inst.blockl + pick];
                } else {
                    randvec[i] = randvec[pick];
                }

                // pitch repeatition component
                pick = i - use_lag;

                if (pick < 0) {
                    PLCresidual[i] = iLBCdec_inst.prevResidual[iLBCdec_inst.blockl + pick];
                } else {
                    PLCresidual[i] = PLCresidual[pick];
                }

                // mix random and periodicity component

                if (i < 80) {
                    PLCresidual[i] = use_gain * (pitchfact * PLCresidual[i] + (1.0 - pitchfact) * randvec[i]);
                } else if (i < 160) {
                    PLCresidual[i] = 0.95 * use_gain * (pitchfact * PLCresidual[i] + (1.0 - pitchfact) * randvec[i]);
                } else {
                    PLCresidual[i] = 0.9 * use_gain * (pitchfact * PLCresidual[i] + (1.0 - pitchfact) * randvec[i]);
                }

                energy += PLCresidual[i] * PLCresidual[i];
            }

            // less than 30 dB, use only noise

            if (Math.sqrt(energy / iLBCdec_inst.blockl) < 30.0) {
                gain[0] = 0.0;
                for (i = 0; i < iLBCdec_inst.blockl; i++) {
                    PLCresidual[i] = randvec[i];
                }
            }

            // use old LPC

            System.arraycopy(iLBCdec_inst.prevLpc, 0, PLClpc, 0, LPC_FILTERORDER + 1);

        } else { // no packet loss, copy input
            System.arraycopy(decresidual, 0, PLCresidual, 0, iLBCdec_inst.blockl);
            System.arraycopy(lpc, lpcP, PLClpc, 0, LPC_FILTERORDER + 1);
            iLBCdec_inst.consPLICount = 0;
        }

        // update state

        if (PLI != 0) {
            iLBCdec_inst.prevLag = lag;
            iLBCdec_inst.per = max_per[0];
        }

        iLBCdec_inst.prevPLI = PLI;
        System.arraycopy(PLClpc, 0, iLBCdec_inst.prevLpc, 0, LPC_FILTERORDER + 1);
        System.arraycopy(PLCresidual, 0, iLBCdec_inst.prevResidual, 0, iLBCdec_inst.blockl);
    }

//#endregion

//#region A.16. enhancer.c

    /**
     * Find index in array such that the array element with said index is the
     * element of said array closest to "value" according to the squared-error
     * criterion.
     *
     * @param index [o] index of array element closest to value
     * @param array data array
     * @param value value
     * @param arlength dimension of data array
     */
    private static void NearestNeighbor(int[] index, int indexP, double[] array, double value, int arlength) {

        double crit = array[0] - value;
        double bestcrit = crit * crit;
        index[indexP] = 0;
        for (int i = 1; i < arlength; i++) {
            crit = array[i] - value;
            crit = crit * crit;

            if (crit < bestcrit) {
                bestcrit = crit;
                index[indexP] = i;
            }
        }
    }

    /**
     * compute cross correlation between sequences.
     *
     * @param corr [o] correlation of seq1 and seq2
     * @param seq1 first sequence
     * @param dim1 dimension first seq1
     * @param seq2 second sequence
     * @param dim2 dimension seq2
     */
    private static void mycorr1(double[] corr, int corrP, double[] seq1, int seq1P, int dim1, double[] seq2, int seq2P, int dim2) {

        for (int i = 0; i <= dim1 - dim2; i++) {
            if ((corrP+i) < corr.length) // BUG in ILBC ???
                corr[corrP+i] = 0.0;
            for (int j = 0; j < dim2; j++) {
                corr[corrP+i] += seq1[seq1P + i + j] * seq2[seq2P + j];
            }
        }
    }

    /**
     * upsample finite array assuming zeros outside bounds.
     *
     * @param useq1 [o] upsampled output sequence
     * @param seq1 unupsampled sequence
     * @param dim1 dimension seq1
     * @param hfl polyphase filter length=2*hfl+1
     */
    private static void enh_upsample(double[] useq1, double[] seq1, int dim1, int hfl) {
        int /* double* */ pu, ps;
        int i, j, k, q, filterlength, hfl2;
        int[] polyp = new int[ENH_UPS0]; // pointers to, ENH_UPS0 polyphase columns
        int /* final double* */ pp;

        // define pointers for filter

        filterlength = 2 * hfl + 1;

        if (filterlength > dim1) {
            hfl2 = (dim1 / 2);
            for (j = 0; j < ENH_UPS0; j++) {
                polyp[j] = j * filterlength + hfl - hfl2; // polyphaserTbl
            }
            hfl = hfl2;
            filterlength = 2 * hfl + 1;
        } else {
            for (j = 0; j < ENH_UPS0; j++) {
                polyp[j] = j * filterlength; // polyphaserTbl
            }
        }

        // filtering: filter overhangs left side of sequence

        pu = 0; // useq1
        for (i = hfl; i < filterlength; i++) {
            for (j = 0; j < ENH_UPS0; j++) {
                useq1[pu] = 0.0;
                pp = polyp[j];
                ps = i; // seq1
                for (k = 0; k <= i; k++) {
                    useq1[pu] += seq1[ps--] * polyphaserTbl[pp++];
                }
                pu++;
            }
        }

        // filtering: simple convolution=inner products

        for (i = filterlength; i < dim1; i++) {

            for (j = 0; j < ENH_UPS0; j++) {
                useq1[pu] = 0.0;
                pp = polyp[j];
                ps = i; // seq1
                for (k = 0; k < filterlength; k++) {
                    useq1[pu] += seq1[ps--] * polyphaserTbl[pp++];
                }
                pu++;
            }
        }

        // filtering: filter overhangs right side of sequence

        for (q = 1; q <= hfl; q++) {
            for (j = 0; j < ENH_UPS0; j++) {
                useq1[pu] = 0.0;
                pp = polyp[j] + q;
                ps = dim1 - 1; // seq1
                for (k = 0; k < filterlength - q; k++) {
                    useq1[pu] += seq1[ps--] * polyphaserTbl[pp++];
                }
                pu++;
            }
        }
    }

    /**
     * find segment starting near idata+estSegPos that has the highest correlation
     * with idata+centerStartPos through idata+centerStartPos+ENH_BLOCKL-1
     * segment is found at a resolution of ENH_UPSO times the original of the
     * original sampling rate
     *
     * @param seg [o] segment array
     * @param updStartPos [o] updated start point
     * @param idata original data buffer
     * @param idatal dimension of idata
     * @param centerStartPos beginning center segment
     * @param estSegPos estimated beginning other segment
     * @param period estimated pitch period
     */
    private static void refiner(double[] seg, int segP, double[] updStartPos, int uP, double[] idata, int idatal, int centerStartPos, double estSegPos, double period) {
        int estSegPosRounded, searchSegStartPos, searchSegEndPos, corrdim;
        int tloc, tloc2, i, st, en, fraction;
        double[] vect = new double[ENH_VECTL], corrVec = new double[ENH_CORRDIM];
        double maxv;
        double[] corrVecUps = new double[ENH_CORRDIM * ENH_UPS0];

        // defining array bounds

        estSegPosRounded = (int) (estSegPos - 0.5);

        searchSegStartPos = estSegPosRounded - ENH_SLOP;

        if (searchSegStartPos < 0) {
            searchSegStartPos = 0;
        }
        searchSegEndPos = estSegPosRounded + ENH_SLOP;

        if (searchSegEndPos + ENH_BLOCKL >= idatal) {
            searchSegEndPos = idatal - ENH_BLOCKL - 1;
        }
        corrdim = searchSegEndPos - searchSegStartPos + 1;

        // compute upsampled correlation (corr33) and find location of max

        mycorr1(corrVec, 0, idata, searchSegStartPos, corrdim + ENH_BLOCKL - 1, idata, centerStartPos, ENH_BLOCKL);
        enh_upsample(corrVecUps, corrVec, corrdim, ENH_FL0);
        tloc = 0;
        maxv = corrVecUps[0];
        for (i = 1; i < ENH_UPS0 * corrdim; i++) {

            if (corrVecUps[i] > maxv) {
                tloc = i;
                maxv = corrVecUps[i];
            }
        }

        // make vector can be upsampled without ever running outside bounds

        updStartPos[uP] = searchSegStartPos + (double) tloc / (double) ENH_UPS0 + 1.0;
        tloc2 = tloc / ENH_UPS0;

        if (tloc > tloc2 * ENH_UPS0) {
            tloc2++;
        }
        st = searchSegStartPos + tloc2 - ENH_FL0;

        if (st < 0) {
            for (int xx = 0; xx < -st; xx++)
                vect[xx] = 0;
            System.arraycopy(idata, 0, vect, -st, ENH_VECTL + st);
        } else {

            en = st + ENH_VECTL;

            if (en > idatal) {
                System.arraycopy(idata, st, vect, 0, ENH_VECTL - (en - idatal));
                for (int xx = 0; xx < en - idatal; xx++)
                    vect[xx + ENH_VECTL - (en - idatal)] = 0;
            } else {
                System.arraycopy(idata, st, vect, 0, ENH_VECTL);
            }
        }
        fraction = tloc2 * ENH_UPS0 - tloc;

        // compute the segment (this is actually a convolution)

        mycorr1(seg, segP, vect, 0, ENH_VECTL, polyphaserTbl, (2 * ENH_FL0 + 1) * fraction, 2 * ENH_FL0 + 1);
    }

    /**
     * find the smoothed output data
     *
     * @param odata [o] smoothed output
     * @param sseq said second sequence of waveforms
     * @param hl 2*hl+1 is sseq dimension
     * @param alpha0 max smoothing energy fraction
     */
    private static void smath(double[] odata, int odataP, double[] sseq, int hl, double alpha0) {
        double w00, w10, w11, A, B, C, err, errs;
        int /* double* */ psseq;
        double[] surround = new double[BLOCKL_MAX]; // shape contributed by other than current
        double[] wt = new double[2 * ENH_HL + 1]; // waveform weighting to get surround shape
        double denom;

        // create shape of contribution from all waveforms except the current
        // one

        for (int i = 1; i <= 2 * hl + 1; i++) {
            wt[i - 1] = 0.5 * (1 - Math.cos(2 * PI * i / (2 * hl + 2)));
        }
        wt[hl] = 0.0; // for clarity, not used
        for (int i = 0; i < ENH_BLOCKL; i++) {
            surround[i] = sseq[i] * wt[0];
        }

        for (int k = 1; k < hl; k++) {
            psseq = k * ENH_BLOCKL; // sseq
            for (int i = 0; i < ENH_BLOCKL; i++) {
                surround[i] += sseq[psseq + i] * wt[k];
            }
        }
        for (int k = hl + 1; k <= 2 * hl; k++) {
            psseq = k * ENH_BLOCKL; // sseq
            for (int i = 0; i < ENH_BLOCKL; i++) {
                surround[i] += sseq[psseq + i] * wt[k];
            }
        }

        // compute some inner products

        w00 = w10 = w11 = 0.0;
        psseq = hl * ENH_BLOCKL; // sseq, current block
        for (int i = 0; i < ENH_BLOCKL; i++) {
            w00 += sseq[psseq + i] * sseq[psseq + i];
            w11 += surround[i] * surround[i];
            w10 += surround[i] * sseq[psseq + i];
        }

        if (Math.abs(w11) < 1.0) {
            w11 = 1.0;
        }
        C = Math.sqrt(w00 / w11);

        // first try enhancement without power-constraint

        errs = 0.0;
        psseq = hl * ENH_BLOCKL; // sseq
        for (int i = 0; i < ENH_BLOCKL; i++) {
            odata[odataP+i] = C * surround[i];
            err = sseq[psseq + i] - odata[odataP+i];
            errs += err * err;
        }

        // if constraint violated by first try, add constraint

        if (errs > alpha0 * w00) {
            if (w00 < 1) {
                w00 = 1;
            }
            denom = (w11 * w00 - w10 * w10) / (w00 * w00);

            if (denom > 0.0001) { // eliminates numerical problems for if smooth

                A = Math.sqrt((alpha0 - alpha0 * alpha0 / 4) / denom);
                B = -alpha0 / 2 - A * w10 / w00;
                B = B + 1;
            } else { // essentially no difference between cycles; smoothing not needed
                A = 0.0;
                B = 1.0;
            }

            // create smoothed sequence

            psseq = hl * ENH_BLOCKL; // sseq
            for (int i = 0; i < ENH_BLOCKL; i++) {
                odata[odataP+i] = A * surround[i] + B * sseq[psseq + i];
            }
        }
    }

    /**
     * get the pitch-synchronous sample sequence
     *
     * @param sseq [o] the pitch-synchronous sequence
     * @param idata original data
     * @param idatal dimension of data
     * @param centerStartPos where current block starts
     * @param period rough-pitch-period array
     * @param plocs where periods of period array are taken
     * @param periodl dimension period array
     * @param hl 2*hl+1 is the number of sequences
     */
    private static void getsseq(double[] sseq, double[] idata, int idatal, int centerStartPos, double[] period, double[] plocs, int periodl, int hl) {
        int i, centerEndPos, q;
        double[] blockStartPos = new double[2 * ENH_HL + 1];
        int[] lagBlock = new int[2 * ENH_HL + 1];
        double[] plocs2 = new double[ENH_PLOCSL];
        int /* double* */ psseq;

        centerEndPos = centerStartPos + ENH_BLOCKL - 1;

        // present

        NearestNeighbor(lagBlock, hl, plocs, 0.5 * (centerStartPos + centerEndPos), periodl);

        blockStartPos[hl] = centerStartPos;

        psseq = ENH_BLOCKL * hl; // sseq
        System.arraycopy(idata, centerStartPos, sseq, psseq, ENH_BLOCKL);

        // past

        for (q = hl - 1; q >= 0; q--) {
            blockStartPos[q] = blockStartPos[q + 1] - period[lagBlock[q + 1]];
            NearestNeighbor(lagBlock, q, plocs, blockStartPos[q] + ENH_BLOCKL_HALF - period[lagBlock[q + 1]], periodl);

            if (blockStartPos[q] - ENH_OVERHANG >= 0) {
                refiner(sseq, q * ENH_BLOCKL, blockStartPos, q, idata, idatal, centerStartPos, blockStartPos[q], period[lagBlock[q + 1]]);
            } else {
                psseq = q * ENH_BLOCKL; // sseq
                for (int xx = 0; xx < ENH_BLOCKL; xx++) {
                    sseq[psseq + xx] = 0;
                }
            }
        }

        // future

        for (i = 0; i < periodl; i++) {
            plocs2[i] = plocs[i] - period[i];
        }
        for (q = hl + 1; q <= 2 * hl; q++) {
            NearestNeighbor(lagBlock, q, plocs2, blockStartPos[q - 1] + ENH_BLOCKL_HALF, periodl);

            blockStartPos[q] = blockStartPos[q - 1] + period[lagBlock[q]];
            if (blockStartPos[q] + ENH_BLOCKL + ENH_OVERHANG < idatal) {
                refiner(sseq, ENH_BLOCKL * q, blockStartPos, q, idata, idatal, centerStartPos, blockStartPos[q], period[lagBlock[q]]);
            } else {
                psseq = q * ENH_BLOCKL; // sseq
                for (int xx = 0; xx < ENH_BLOCKL; xx++) {
                    sseq[psseq + xx] = 0;
                }
            }
        }
    }

    /**
     * perform enhancement on idata+centerStartPos through
     * idata+centerStartPos+ENH_BLOCKL-1
     *
     * @param odata [o] smoothed block, dimension blockl
     * @param idata data buffer used for enhancing
     * @param idatal dimension idata
     * @param centerStartPos first sample current block within idata
     * @param alpha0 max correction-energy-fraction (in [0,1])
     * @param period pitch period array
     * @param plocs locations where period array values valid
     * @param periodl dimension of period and plocs
     */
    private static void enhancer(double[] odata, int odataP, double[] idata, int idatal, int centerStartPos, double alpha0, double[] period, double[] plocs, int periodl) {
        double[] sseq = new double[(2 * ENH_HL + 1) * ENH_BLOCKL];

        // get said second sequence of segments

        getsseq(sseq, idata, idatal, centerStartPos, period, plocs, periodl, ENH_HL);

        // compute the smoothed output from said second sequence

        smath(odata, odataP, sseq, ENH_HL, alpha0);
    }

    /**
     * cross correlation
     *
     * @param target first array
     * @param regressor second array
     * @param subl dimension arrays
     */
    private static double xCorrCoef(double[] target, int targetP, double[] regressor, int regressorP, int subl) {

        double ftmp1 = 0.0;
        double ftmp2 = 0.0;
        for (int i = 0; i < subl; i++) {
            ftmp1 += target[targetP+i] * regressor[regressorP+i];
            ftmp2 += regressor[regressorP+i] * regressor[regressorP+i];
        }

        if (ftmp1 > 0.0) {
            return ftmp1 * ftmp1 / ftmp2;
        } else {
            return 0.0;
        }
    }

    /**
     * interface for enhancer
     *
     * @param out [o] enhanced signal
     * @param in unenhanced signal
     * @param iLBCdec_inst buffers etc
     */
    private static int enhancerInterface(double[] out, double[] in, Decoder iLBCdec_inst) {
        double[] enh_buf, enh_period;
        int iblock, isample;
        int lag = 0, ilag, i, ioffset;
        double cc, maxcc;
        double ftmp1, ftmp2;
        int /* double* */ inPtr, enh_bufPtr1, enh_bufPtr2;
        double[] plc_pred = new double[ENH_BLOCKL];

        double[] lpState = new double[6], downsampled = new double[(ENH_NBLOCKS * ENH_BLOCKL + 120) / 2];
        int inLen = ENH_NBLOCKS * ENH_BLOCKL + 120;
        int start, plc_blockl, inlag;

        enh_buf = iLBCdec_inst.enh_buf;
        enh_period = iLBCdec_inst.enh_period;

        System.arraycopy(enh_buf, iLBCdec_inst.blockl, enh_buf, 0, (ENH_BUFL - iLBCdec_inst.blockl));

        System.arraycopy(in, 0, enh_buf, ENH_BUFL - iLBCdec_inst.blockl, iLBCdec_inst.blockl);

        if (iLBCdec_inst.mode == 30) {
            plc_blockl = ENH_BLOCKL;
        } else {
            plc_blockl = 40;
        }

        // when 20 ms frame, move processing one block
        ioffset = 0;
        if (iLBCdec_inst.mode == 20) {
            ioffset = 1;
        }

        i = 3 - ioffset;
        for (int xx = 0; xx < ENH_NBLOCKS_TOT - i; xx++) {
            enh_period[xx] = enh_period[i + xx];
        }

        // Set state information to the 6 samples right before the samples to be
        // downsampled.

        System.arraycopy(enh_buf, (ENH_NBLOCKS_EXTRA + ioffset) * ENH_BLOCKL - 126, lpState, 0, 6);

        // Down sample a factor 2 to save computations

        DownSample(enh_buf, (ENH_NBLOCKS_EXTRA + ioffset) * ENH_BLOCKL - 120, lpFilt_coefsTbl, inLen - ioffset * ENH_BLOCKL, lpState, downsampled);

        // Estimate the pitch in the down sampled domain.
        for (iblock = 0; iblock < ENH_NBLOCKS - ioffset; iblock++) {

            lag = 10;
            maxcc = xCorrCoef(downsampled, 60 + iblock * ENH_BLOCKL_HALF, downsampled, 60 + iblock * ENH_BLOCKL_HALF - lag, ENH_BLOCKL_HALF);
            for (ilag = 11; ilag < 60; ilag++) {
                cc = xCorrCoef(downsampled, 60 + iblock * ENH_BLOCKL_HALF, downsampled, 60 + iblock * ENH_BLOCKL_HALF - ilag, ENH_BLOCKL_HALF);

                if (cc > maxcc) {
                    maxcc = cc;
                    lag = ilag;
                }
            }

            // Store the estimated lag in the non-downsampled domain
            enh_period[iblock + ENH_NBLOCKS_EXTRA + ioffset] = (double) lag * 2;
        }

        // PLC was performed on the previous packet
        if (iLBCdec_inst.prev_enh_pl == 1) {

            inlag = (int) enh_period[ENH_NBLOCKS_EXTRA + ioffset];

            lag = inlag - 1;
            maxcc = xCorrCoef(in, 0, in, lag, plc_blockl);
            for (ilag = inlag; ilag <= inlag + 1; ilag++) {
                cc = xCorrCoef(in, 0, in, ilag, plc_blockl);

                if (cc > maxcc) {
                    maxcc = cc;
                    lag = ilag;
                }
            }

            enh_period[ENH_NBLOCKS_EXTRA + ioffset - 1] = lag;

            // compute new concealed residual for the old lookahead, mix the
            // forward PLC with a backward PLC from the new frame

            inPtr = lag - 1; // in

            enh_bufPtr1 = plc_blockl - 1; // plc_pred

            start = Math.min(lag, plc_blockl);

            for (isample = start; isample > 0; isample--) {
                plc_pred[enh_bufPtr1--] = in[inPtr--];
            }

            enh_bufPtr2 = ENH_BUFL - 1 - iLBCdec_inst.blockl; // enh_buf
            for (isample = (plc_blockl - 1 - lag); isample >= 0; isample--) {
                plc_pred[enh_bufPtr1--] = enh_buf[enh_bufPtr2--];
            }

            // limit energy change
            ftmp2 = 0.0;
            ftmp1 = 0.0;
            for (i = 0; i < plc_blockl; i++) {
                ftmp2 += enh_buf[ENH_BUFL - 1 - iLBCdec_inst.blockl - i] * enh_buf[ENH_BUFL - 1 - iLBCdec_inst.blockl - i];
                ftmp1 += plc_pred[i] * plc_pred[i];
            }
            ftmp1 = Math.sqrt(ftmp1 / plc_blockl);
            ftmp2 = Math.sqrt(ftmp2 / plc_blockl);
            if (ftmp1 > 2.0 * ftmp2 && ftmp1 > 0.0) {
                for (i = 0; i < plc_blockl - 10; i++) {
                    plc_pred[i] *= 2.0 * ftmp2 / ftmp1;
                }
                for (i = plc_blockl - 10; i < plc_blockl; i++) {
                    plc_pred[i] *= (i - plc_blockl + 10) * (1.0 - 2.0 * ftmp2 / ftmp1) / (10) +

                    2.0 * ftmp2 / ftmp1;
                }
            }

            enh_bufPtr1 = ENH_BUFL - 1 - iLBCdec_inst.blockl; // enh_buf
            for (i = 0; i < plc_blockl; i++) {
                ftmp1 = (double) (i + 1) / (double) (plc_blockl + 1);
                enh_buf[enh_bufPtr1] *= ftmp1;
                enh_buf[enh_bufPtr1] += (1.0 - ftmp1) * plc_pred[plc_blockl - 1 - i];
                enh_bufPtr1--;
            }
        }

        if (iLBCdec_inst.mode == 20) {
            // Enhancer with 40 samples delay
            for (iblock = 0; iblock < 2; iblock++) {
                enhancer(out, iblock * ENH_BLOCKL, enh_buf, ENH_BUFL, (5 + iblock) * ENH_BLOCKL + 40, ENH_ALPHA0, enh_period, enh_plocsTbl, ENH_NBLOCKS_TOT);
            }
        } else if (iLBCdec_inst.mode == 30) {
            // Enhancer with 80 samples delay
            for (iblock = 0; iblock < 3; iblock++) {
                enhancer(out, iblock * ENH_BLOCKL, enh_buf, ENH_BUFL, (4 + iblock) * ENH_BLOCKL, ENH_ALPHA0, enh_period, enh_plocsTbl, ENH_NBLOCKS_TOT);
            }
        }

        return lag * 2;
    }

//#endregion

//#region A.18. filter.c

    /**
     * all-pole filter
     *
     * @param InOut (i/o) on entrance InOut[-orderCoef] to InOut[-1] contain the
     *            state of the filter (delayed samples). InOut[0] to
     *            InOut[lengthInOut-1] contain the filter input, on en exit
     *            InOut[-orderCoef] to InOut[-1] is unchanged and InOut[0] to
     *            InOut[lengthInOut-1] contain filtered samples
     * @param Coef filter coefficients, Coef[0] is assumed to be 1.0
     * @param lengthInOut number of input/output samples
     * @param orderCoef number of filter coefficients
     */
    private static void AllPoleFilter(double[] InOut, int iop, double[] Coef, int coefP, int lengthInOut, int orderCoef) {
        for (int n = 0; n < lengthInOut; n++) {
            for (int k = 1; k <= orderCoef; k++) {
                InOut[iop] -= Coef[coefP + k] * InOut[iop - k];
            }
            iop++;
        }
    }

    /**
     * all-zero filter.
     *
     * @param In In[0] to In[lengthInOut-1] contain filter input samples
     * @param Coef filter coefficients (Coef[0] is assumed to be 1.0)
     * @param lengthInOut number of input/output samples
     * @param orderCoef number of filter coefficients
     * @param Out (i/o) on entrance Out[-orderCoef] to Out[-1] contain the
     *            filter state, on exit Out[0] to Out[lengthInOut-1] contain
     *            filtered samples
     */
    private static void AllZeroFilter(double[] In, int ip, double[] Coef, int lengthInOut, int orderCoef, double[] Out, int op) {
        for (int n = 0; n < lengthInOut; n++) {
            Out[op] = Coef[0] * In[ip];
            for (int k = 1; k <= orderCoef; k++) {
                Out[op] += Coef[k] * In[ip - k];
            }
            op++;
            ip++;
        }
    }

    /**
     * pole-zero filter
     *
     * @param In In[0] to In[lengthInOut-1] contain filter input samples
     *            In[-orderCoef] to In[-1] contain state of all-zero section
     * @param ZeroCoef filter coefficients for all-zero section (ZeroCoef[0]
     *            is assumed to be 1.0)
     * @param PoleCoef filter coefficients for all-pole section (ZeroCoef[0]
     *            is assumed to be 1.0)
     * @param lengthInOut number of input/output samples
     * @param orderCoef number of filter coefficients
     * @param Out (i/o) on entrance Out[-orderCoef] to Out[-1] contain state of
     *            all-pole section. On exit Out[0] to Out[lengthInOut-1] contain
     *            filtered samples
     */
    private static void ZeroPoleFilter(double[] In, int inP, double[] ZeroCoef, double[] PoleCoef, int poleCoefP, int lengthInOut,
                                       int orderCoef, double[] Out, int outP) {
        AllZeroFilter(In, inP, ZeroCoef, lengthInOut, orderCoef, Out, outP);
        AllPoleFilter(Out, outP, PoleCoef, poleCoefP, lengthInOut, orderCoef);
    }

    /**
     * downsample (LP filter and decimation)
     *
     * @param In input samples
     * @param Coef filter coefficients
     * @param lengthIn number of input samples
     * @param state filter state
     * @param Out [o] downsampled output
     */
    private static void DownSample(double[] In, int inP, double[] Coef, int lengthIn, double[] state, double[] Out) {
        int /* double* */ Out_ptr = 0; // Out
        int /* double* */ Coef_ptr, In_ptr;
        int /* double* */ state_ptr;

        // LP filter and decimate at the same time

        for (int i = DELAY_DS; i < lengthIn; i += FACTOR_DS) {
            Coef_ptr = 0; // Coef
            In_ptr = inP + i; // In
            state_ptr = FILTERORDER_DS - 2; // state

            double o = 0.0;

            int stop = (i < FILTERORDER_DS) ? i + 1 : FILTERORDER_DS;

            for (int j = 0; j < stop; j++) {
                o += Coef[Coef_ptr++] * (In[In_ptr--]);
            }
            for (int j = i + 1; j < FILTERORDER_DS; j++) {
                o += Coef[Coef_ptr++] * (state[state_ptr--]);
            }

            Out[Out_ptr++] = o;
        }

        // Get the last part (use zeros as input for the future)

        for (int i = (lengthIn + FACTOR_DS); i < (lengthIn + DELAY_DS); i += FACTOR_DS) {

            double o = 0.0;

            if (i < lengthIn) {
                Coef_ptr = 0; // Coef
                In_ptr = inP + i; // In
                for (int j = 0; j < FILTERORDER_DS; j++) {
                    o += Coef[Coef_ptr++] * (Out[Out_ptr--]);
                }
            } else {
                Coef_ptr = i - lengthIn; // Coef
                In_ptr = inP + lengthIn - 1; // In
                for (int j = 0; j < FILTERORDER_DS - (i - lengthIn); j++) {
                    o += Coef[Coef_ptr++] * (In[In_ptr--]);
                }
            }
            Out[Out_ptr++] = o;
        }
    }

    // A.20. FrameClassify.c

    /**
     * Classification of subframes to localize start state
     *
     * @return index to the max-energy sub-frame
     * @param iLBCenc_inst (i/o) the encoder state structure
     * @param residual lpc residual signal
     */
    private static int FrameClassify(Encoder iLBCenc_inst, double[] residual) {
        double max_ssqEn;
        double[] fssqEn = new double[NSUB_MAX], bssqEn = new double[NSUB_MAX];
        int /* double* */ pp;
        int n, l, max_ssqEn_n;
        double[] ssqEn_win = { // NSUB_MAX-1
            0.8, 0.9, 1.0, 0.9, 0.8
        };
        double[] sampEn_win = {
            1.0 / 6.0, 2.0 / 6.0, 3.0 / 6.0, 4.0 / 6.0, 5.0 / 6.0
        };

        // init the front and back energies to zero

        // Calculate front of first seqence

        n = 0;
        pp = 0; // residual
        for (l = 0; l < 5; l++) {
            fssqEn[n] += sampEn_win[l] * residual[pp] * residual[pp];
            pp++;
        }
        for (l = 5; l < SUBL; l++) {
            fssqEn[n] += residual[pp] * residual[pp];
            pp++;
        }

        // Calculate front and back of all middle sequences

        for (n = 1; n < iLBCenc_inst.nsub - 1; n++) {
            pp = n * SUBL; // residual
            for (l = 0; l < 5; l++) {
                fssqEn[n] += sampEn_win[l] * residual[pp] * residual[pp];
                bssqEn[n] += residual[pp] * residual[pp];
                pp++;
            }
            for (l = 5; l < SUBL - 5; l++) {
                fssqEn[n] += residual[pp] * residual[pp];
                bssqEn[n] += residual[pp] * residual[pp];
                pp++;
            }
            for (l = SUBL - 5; l < SUBL; l++) {
                fssqEn[n] += residual[pp] * residual[pp];
                bssqEn[n] += sampEn_win[SUBL - l - 1] * residual[pp] * residual[pp];
                pp++;
            }
        }

        // Calculate back of last seqence

        n = iLBCenc_inst.nsub - 1;
        pp = n * SUBL; // residual
        for (l = 0; l < SUBL - 5; l++) {
            bssqEn[n] += residual[pp] * residual[pp];
            pp++;
        }
        for (l = SUBL - 5; l < SUBL; l++) {
            bssqEn[n] += sampEn_win[SUBL - l - 1] * residual[pp] * residual[pp];
            pp++;
        }

        // find the index to the weighted 80 sample with most energy

        if (iLBCenc_inst.mode == 20)
            l = 1;
        else
            l = 0;

        max_ssqEn = (fssqEn[0] + bssqEn[1]) * ssqEn_win[l];
        max_ssqEn_n = 1;
        for (n = 2; n < iLBCenc_inst.nsub; n++) {

            l++;
            double v = (fssqEn[n - 1] + bssqEn[n]) * ssqEn_win[l];
            if (v > max_ssqEn) {
                max_ssqEn = v;
                max_ssqEn_n = n;
            }
        }

        return max_ssqEn_n;
    }

//#endregion

//#region A.22. gainquant.c

    /**
     * quantizer for the gain in the gain-shape coding of residual
     *
     * @return quantized gain value
     * @param in gain value
     * @param maxIn maximum of gain value
     * @param cblen number of quantization indices
     * @param index [o] quantization index
     */
    private static double gainquant(double in, double maxIn, int cblen, int[] index, int indexP) {
        double[] cb;

        // ensure a lower bound on the scaling factor

        double scale = maxIn;

        if (scale < 0.1) {
            scale = 0.1;
        }

        // select the quantization table

        if (cblen == 8) {
            cb = gain_sq3Tbl;
        } else if (cblen == 16) {
            cb = gain_sq4Tbl;
        } else {
            cb = gain_sq5Tbl;
        }

        // select the best index in the quantization table

        double minmeasure = 10000000.0;
        int tindex = 0;
        for (int i = 0; i < cblen; i++) {

            double measure = (in - scale * cb[i]) * (in - scale * cb[i]);

            if (measure < minmeasure) {
                tindex = i;
                minmeasure = measure;
            }
        }
        index[indexP+0] = tindex;

        // return the quantized value

        return scale * cb[tindex];
    }

    /**
     * decoder for quantized gains in the gain-shape coding of residual
     *
     * @return quantized gain value
     * @param index quantization index
     * @param maxIn maximum of unquantized gain
     * @param cblen number of quantization indices
     */
    private static double gaindequant(int index, double maxIn, int cblen) {

        // obtain correct scale factor

        double scale = Math.abs(maxIn);

        if (scale < 0.1) {
            scale = 0.1;
        }

        // select the quantization table and return the decoded value

        if (cblen == 8) {
            return scale * gain_sq3Tbl[index];
        } else if (cblen == 16) {
            return scale * gain_sq4Tbl[index];
        } else if (cblen == 32) {
            return scale * gain_sq5Tbl[index];
        }

        return 0.0;
    }

//#endregion

//#region A.24. getCBvec.c

    /**
     * Construct codebook vector for given index.
     *
     * @param cbvec [o] Constructed codebook vector
     * @param mem Codebook buffer
     * @param index Codebook index
     * @param lMem Length of codebook buffer
     * @param cbveclen Codebook vector length
     */
    private static void getCBvec(double[] cbvec, double[] mem, int memP, int index, int lMem, int cbveclen) {
        int j, k, n, memInd, sFilt;
        double[] tmpbuf = new double[CB_MEML];
        int base_size;
        int ilow, ihigh;
        double alfa, alfa1;

        // Determine size of codebook sections

        base_size = lMem - cbveclen + 1;

        if (cbveclen == SUBL) {
            base_size += cbveclen / 2;
        }

        // No filter . First codebook section

        if (index < lMem - cbveclen + 1) {

            // first non-interpolated vectors

            k = index + cbveclen;
            // get vector
            System.arraycopy(mem, memP + lMem - k, cbvec, 0, cbveclen);

        } else if (index < base_size) {

            k = 2 * (index - (lMem - cbveclen + 1)) + cbveclen;

            ihigh = k / 2;
            ilow = ihigh - 5;

            // Copy first noninterpolated part

            System.arraycopy(mem, memP + lMem - k / 2, cbvec, 0, ilow);

            // interpolation

            alfa1 = 0.2;
            alfa = 0.0;
            for (j = ilow; j < ihigh; j++) {
                cbvec[j] = (1.0 - alfa) * mem[memP + lMem - k / 2 + j] + alfa * mem[memP + lMem - k + j];

                alfa += alfa1;
            }

            // Copy second noninterpolated part

            System.arraycopy(mem, memP + lMem - k + ihigh, cbvec, ihigh, cbveclen - ihigh);

        } else { // Higher codebook section based on filtering

            // first non-interpolated vectors

            if (index - base_size < lMem - cbveclen + 1) {
                double[] tempbuff2 = new double[CB_MEML + CB_FILTERLEN + 1];
                int /* double* */ pos;
                int /* double* */ pp, pp1;

                for (int xx = 0; xx < CB_HALFFILTERLEN; xx++) {
                    tempbuff2[xx] = 0;
                }
                System.arraycopy(mem, memP, tempbuff2, CB_HALFFILTERLEN, lMem);
                for (int xx = 0; xx < CB_HALFFILTERLEN + 1; xx++) {
                    tempbuff2[xx + lMem + CB_HALFFILTERLEN] = 0;
                }

                k = index - base_size + cbveclen;
                sFilt = lMem - k;
                memInd = sFilt + 1 - CB_HALFFILTERLEN;

                // do filtering
                pos = 0; // cbvec
                for (int xx = 0; xx < cbveclen; xx++) {
                    cbvec[pos + xx] = 0;
                }
                for (n = 0; n < cbveclen; n++) {
                    pp = memInd + n + CB_HALFFILTERLEN; // tempbuff2
                    pp1 = CB_FILTERLEN - 1; // cbfiltersTbl
                    for (j = 0; j < CB_FILTERLEN; j++) {
                        cbvec[pos] += tempbuff2[pp++] * (cbfiltersTbl[pp1--]);
                    }
                    pos++;
                }
            } else { // interpolated vectors

                double[] tempbuff2 = new double[CB_MEML + CB_FILTERLEN + 1];

                int /* double* */ pos;
                int /* double* */ pp, pp1;

                for (int xx = 0; xx < CB_HALFFILTERLEN; xx++) {
                    tempbuff2[xx] = 0;
                }
                System.arraycopy(mem, memP, tempbuff2, CB_HALFFILTERLEN, lMem);
                for (int xx = 0; xx < CB_HALFFILTERLEN + 1; xx++) {
                    tempbuff2[xx + lMem + CB_HALFFILTERLEN] = 0;
                }

                k = 2 * (index - base_size - (lMem - cbveclen + 1)) + cbveclen;
                sFilt = lMem - k;
                memInd = sFilt + 1 - CB_HALFFILTERLEN;

                // do filtering
                pos = sFilt; // tmpbuf
                for (int xx = 0; xx < k; xx++) {
                    tmpbuf[pos + xx] = 0;
                }
                for (int i = 0; i < k; i++) {
                    pp = memInd + i + CB_HALFFILTERLEN; // tempbuff2
                    pp1 = CB_FILTERLEN - 1; // cbfiltersTbl
                    for (j = 0; j < CB_FILTERLEN; j++) {
                        (tmpbuf[pos]) += (tempbuff2[pp++]) * (cbfiltersTbl[pp1--]);
                    }
                    pos++;
                }

                ihigh = k / 2;
                ilow = ihigh - 5;

                // Copy first noninterpolated part

                System.arraycopy(tmpbuf, lMem - k / 2, cbvec, 0, ilow);

                // interpolation

                alfa1 = 0.2;
                alfa = 0.0;
                for (j = ilow; j < ihigh; j++) {
                    cbvec[j] = (1.0 - alfa) * tmpbuf[lMem - k / 2 + j] + alfa * tmpbuf[lMem - k + j];
                    alfa += alfa1;
                }

                // Copy second noninterpolated part

                System.arraycopy(tmpbuf, lMem - k + ihigh, cbvec, ihigh, cbveclen - ihigh);
            }
        }
    }

//#endregion

//#region A.26. helpfun.c

    /**
     * calculation of auto correlation
     *
     * @param r [o] auto-correlation vector
     * @param x data vector
     * @param N length of data vector
     * @param order largest lag for calculated auto-correlations
     */
    private static void autocorr(double[] r, double[] x, int N, int order) {

        for (int lag = 0; lag <= order; lag++) {
            double sum = 0;
            for (int n = 0; n < N - lag; n++) {
                sum += x[n] * x[n + lag];
            }
            r[lag] = sum;
        }
    }

    /**
     * window multiplication
     *
     * @param z [o] the windowed data
     * @param x the original data vector
     * @param y the window
     * @param N length of all vectors
     */
    private static void window(double[] z, double[] x, double[] y, int yP, int N) {

        for (int i = 0; i < N; i++) {
            z[i] = x[i] * y[yP + i];
        }
    }

    /**
     * levinson-durbin solution for lpc coefficients
     *
     * @param a [o] lpc coefficient vector starting with 1.0
     * @param k [o] reflection coefficients
     * @param r autocorrelation vector
     * @param order order of lpc filter
     */
    private static void levdurb(double[] a, double[] k, double[] r, int order) {
        double sum, alpha;
        int m, m_h, i;

        a[0] = 1.0;

        if (r[0] < EPS) { // if r[0] <= 0, set LPC coeff. to zero
            for (i = 0; i < order; i++) {
                k[i] = 0;
                a[i + 1] = 0;
            }
        } else {
            a[1] = k[0] = -r[1] / r[0];
            alpha = r[0] + r[1] * k[0];
            for (m = 1; m < order; m++) {
                sum = r[m + 1];
                for (i = 0; i < m; i++) {
                    sum += a[i + 1] * r[m - i];
                }

                k[m] = -sum / alpha;
                alpha += k[m] * sum;
                m_h = (m + 1) >> 1;
                for (i = 0; i < m_h; i++) {
                    sum = a[i + 1] + k[m] * a[m - i];
                    a[m - i] += k[m] * a[i + 1];
                    a[i + 1] = sum;
                }
                a[m + 1] = k[m];
            }
        }
    }

    /**
     * interpolation between vectors
     *
     * @param out [o] the interpolated vector
     * @param in1 the first vector for the interpolation
     * @param in2 the second vector for the interpolation
     * @param coef interpolation weights
     * @param length length of all vectors
     */
    private static void interpolate(double[] out, double[] in1, double[] in2, int in2P, double coef, int length) {

        double invcoef = 1.0 - coef;
        for (int i = 0; i < length; i++) {
            out[i] = coef * in1[i] + invcoef * in2[in2P + i];
        }
    }

    /**
     * lpc bandwidth expansion
     *
     * @param out [o] the bandwidth expanded lpc coefficients
     * @param in the lpc coefficients before bandwidth expansion
     * @param coef the bandwidth expansion factor
     * @param length the length of lpc coefficient vectors
     */
    private static void bwexpand(double[] out, int outP, double[] in, double coef, int length) {

        double chirp = coef;

        out[outP] = in[0];
        for (int i = 1; i < length; i++) {
            out[outP + i] = chirp * in[i];
            chirp *= coef;
        }
    }

    /**
     * vector quantization
     *
     * @param Xq [o] the quantized vector
     * @param index [o] the quantization index
     * @param CB the vector quantization codebook
     * @param X the vector to quantize
     * @param n_cb the number of vectors in the codebook
     * @param dim the dimension of all vectors
     */
    private static void vq(double[] Xq, int xqP, int[] index, int indexP, double[] CB, int cbP, double[] X, int xP, int n_cb, int dim) {
        double dist, tmp, mindist;

        int pos = 0;
        mindist = FLOAT_MAX;
        int minindex = 0;
        for (int j = 0; j < n_cb; j++) {
            dist = X[xP] - CB[cbP + pos];
            dist *= dist;
            for (int i = 1; i < dim; i++) {
                tmp = X[xP + i] - CB[cbP + pos + i];
                dist += tmp * tmp;
            }

            if (dist < mindist) {
                mindist = dist;
                minindex = j;
            }
            pos += dim;
        }
        for (int i = 0; i < dim; i++) {
            Xq[xqP + i] = CB[cbP + minindex * dim + i];
        }
        index[indexP] = minindex;
    }

    /**
     * split vector quantization
     *
     * @param qX [o] the quantized vector
     * @param index [o] a vector of indexes for all vector codebooks in the
     *            split
     * @param X the vector to quantize
     * @param CB the quantizer codebook
     * @param nsplit the number of vector splits
     * @param dim the dimension of X and qX
     * @param cbsize the number of vectors in the codebook
     */
    private static void SplitVQ(double[] qX, int qxP, int[] index, int indexP, double[] X, int xP, double[] CB, int nsplit, int[] dim, int[] cbsize) {

        int cb_pos = 0;
        int X_pos = 0;
        for (int i = 0; i < nsplit; i++) {
            vq(qX, qxP + X_pos, index, i + indexP, CB, cb_pos, X, xP + X_pos, cbsize[i], dim[i]);
            X_pos += dim[i];
            cb_pos += dim[i] * cbsize[i];
        }
    }

    /**
     * scalar quantization
     *
     * @param xq [o] the quantized value
     * @param index [o] the quantization index
     * @param x the value to quantize
     * @param cb the quantization codebook
     * @param cb_size the size of the quantization codebook
     */
    private static void sort_sq(double[] xq, int[] index, double x, double[] cb, int cb_size) {

        if (x <= cb[0]) {
            index[0] = 0;
            xq[0] = cb[0];
        } else {
            int i = 0;
            while ((x > cb[i]) && i < cb_size - 1) {
                i++;
            }

            if (x > ((cb[i] + cb[i - 1]) / 2)) {
                index[0] = i;
                xq[0] = cb[i];
            } else {
                index[0] = i - 1;
                xq[0] = cb[i - 1];
            }
        }
    }

    /**
     * check for stability of lsf coefficients
     *
     * @return [o] 1 for stable lsf vectors and 0 for nonstable ones
     * @param lsf a table of lsf vectors
     * @param dim the dimension of each lsf vector
     * @param NoAn the number of lsf vectors in the table
     */
    private static int LSF_check(double[] lsf, int dim, int NoAn) {
        int Nit = 2, change = 0;
        final double eps = 0.039; /* 50 Hz */
        final double eps2 = 0.0195;
        final double maxlsf = 3.14; /* 4000 Hz */
        final double minlsf = 0.01; /* 0 Hz */

        /* LSF separation check */

        for (int n = 0; n < Nit; n++) { // Run through a couple of times
            for (int m = 0; m < NoAn; m++) { // Number of analyses per frame
                for (int k = 0; k < (dim - 1); k++) {
                    int pos = m * dim + k;

                    if ((lsf[pos + 1] - lsf[pos]) < eps) {

                        if (lsf[pos + 1] < lsf[pos]) {
//                          double tmp = lsf[pos + 1];
                            lsf[pos + 1] = lsf[pos] + eps2;
                            lsf[pos] = lsf[pos + 1] - eps2;
                        } else {
                            lsf[pos] -= eps2;
                            lsf[pos + 1] += eps2;
                        }
                        change = 1;
                    }

                    if (lsf[pos] < minlsf) {
                        lsf[pos] = minlsf;
                        change = 1;
                    }

                    if (lsf[pos] > maxlsf) {
                        lsf[pos] = maxlsf;
                        change = 1;
                    }
                }
            }
        }

        return change;
    }

//#endregion

//#region A.28. hpInput.c

    /**
     * Input high-pass filter
     *
     * @param In vector to filter
     * @param len length of vector to filter
     * @param Out [o] the resulting filtered vector
     * @param mem (i/o) the filter state
     */
    private static void hpInput(double[] In, int len, double[] Out, double[] mem) {

        // all-zero section

        int pi = 0; // In
        int po = 0; // Out
        for (int i = 0; i < len; i++) {
            Out[po] = hpi_zero_coefsTbl[0] * (In[pi]);
            Out[po] += hpi_zero_coefsTbl[1] * mem[0];
            Out[po] += hpi_zero_coefsTbl[2] * mem[1];

            mem[1] = mem[0];
            mem[0] = In[pi];
            po++;
            pi++;

        }

        // all-pole section

        po = 0; // Out
        for (int i = 0; i < len; i++) {
            Out[po] -= hpi_pole_coefsTbl[1] * mem[2];
            Out[po] -= hpi_pole_coefsTbl[2] * mem[3];

            mem[3] = mem[2];
            mem[2] = Out[po];
            po++;
        }
    }

//#endregion

//#region A.30. hpOutput.c

    /**
     * Output high-pass filter
     *
     * @param In vector to filter
     * @param len length of vector to filter
     * @param Out [o] the resulting filtered vector
     * @param mem (i/o) the filter state
     */
    private static void hpOutput(double[] In, int len, double[] Out, double[] mem) {

        // all-zero section

        int pi = 0; // In
        int po = 0; // Out
        for (int i = 0; i < len; i++) {
            Out[po] = hpo_zero_coefsTbl[0] * (In[pi]);
            Out[po] += hpo_zero_coefsTbl[1] * mem[0];
            Out[po] += hpo_zero_coefsTbl[2] * mem[1];

            mem[1] = mem[0];
            mem[0] = In[pi];
            po++;
            pi++;
        }

        // all-pole section

        po = 0; // Out
        for (int i = 0; i < len; i++) {
            Out[po] -= hpo_pole_coefsTbl[1] * mem[2];
            Out[po] -= hpo_pole_coefsTbl[2] * mem[3];

            mem[3] = mem[2];
            mem[2] = Out[po];
            po++;
        }
    }

//#endregion

//#region A.32. iCBConstruct.c

    /**
     * Convert the codebook indexes to make the search easier
     *
     * @param index (i/o) Codebook indexes
     */
    private static void index_conv_enc(int[] index) {

        for (int k = 1; k < CB_NSTAGES; k++) {

            if ((index[k] >= 108) && (index[k] < 172)) {
                index[k] -= 64;
            } else if (index[k] >= 236) {
                index[k] -= 128;
            } else {
                // ERROR
            }
        }
    }

    /**
     * @param index (i/o) Codebook indexes
     */
    private static void index_conv_dec(int[] index) {

        for (int k = 1; k < CB_NSTAGES; k++) {

            if ((index[k] >= 44) && (index[k] < 108)) {
                index[k] += 64;
            } else if ((index[k] >= 108) && (index[k] < 128)) {
                index[k] += 128;
            } else {
                // ERROR
            }
        }
    }

    /**
     * Construct decoded vector from codebook and gains.
     *
     * @param decvector [o] Decoded vector
     * @param index Codebook indices
     * @param gain_index Gain quantization indices
     * @param mem Buffer for codevector construction
     * @param lMem Length of buffer
     * @param veclen Length of vector
     * @param nStages Number of codebook stages
     */
    private static void iCBConstruct(double[] decvector, int decvectorP, int[] index, int indexP, int[] gain_index, int gain_indexP, double[] mem, int memP, int lMem, int veclen, int nStages) {

        double[] gain = new double[CB_NSTAGES];
        double[] cbvec = new double[SUBL];

        // gain de-quantization

        gain[0] = gaindequant(gain_index[gain_indexP], 1.0f, 32);
        if (nStages > 1) {
            gain[1] = gaindequant(gain_index[gain_indexP+1], Math.abs(gain[0]), 16);
        }
        if (nStages > 2) {
            gain[2] = gaindequant(gain_index[gain_indexP+2], Math.abs(gain[1]), 8);
        }

        // codebook vector construction and construction of total vector

        getCBvec(cbvec, mem, memP, index[indexP], lMem, veclen);
        for (int j = 0; j < veclen; j++) {
            decvector[decvectorP+j] = gain[0] * cbvec[j];
        }
        if (nStages > 1) {
            for (int k = 1; k < nStages; k++) {
                getCBvec(cbvec, mem, memP, index[indexP+k], lMem, veclen);
                for (int j = 0; j < veclen; j++) {
                    decvector[decvectorP+j] += gain[k] * cbvec[j];
                }
            }
        }
    }

//#endregion

//#region A.34. iCBSearch.c

    /**
     * Search routine for codebook encoding and gain quantization.
     *
     * @param encoder the encoder state structure
     * @param index [o] Codebook indices
     * @param gain_index [o] Gain quantization indices
     * @param intarget Target vector for encoding
     * @param mem Buffer for codebook construction
     * @param lMem Length of buffer
     * @param lTarget Length of vector
     * @param nStages Number of codebook stages
     * @param weightDenum weighting filter coefficients
     * @param weightState weighting filter state
     * @param block the sub-block number
     */
    private static void iCBSearch(Encoder encoder,
                                  int[] index, int indexP, int[] gain_index, int gain_indexP,
                                  double[] intarget, int intargetP, double[] mem, int memP, int lMem,
                                  int lTarget, int nStages,
                                  double[] weightDenum, int weightDenumP, double[] weightState, int block) {
        int i, j, icount, stage;
        int[] best_index = new int[1];
        int range, counter;
        double[] max_measure = new double[1], gain = new double[1];
        double measure, crossDot, ftmp;
        double[] gains = new double[CB_NSTAGES];
        double[] target = new double[SUBL];
        int base_index, sInd, eInd, base_size;
        int sIndAug = 0, eIndAug = 0;
        double[] buf = new double[CB_MEML + SUBL + 2 * LPC_FILTERORDER];
        double[] invenergy = new double[CB_EXPAND * 128], energy = new double[CB_EXPAND * 128];
        int /* double * */pp, ppi = 0, ppo = 0, ppe = 0;
        double[] cbvectors = new double[CB_MEML];
        double tene, cene;
        double[] cvec = new double[SUBL];
        double[] aug_vec = new double[SUBL];
        double[] bufP;

        // Determine size of codebook sections

        base_size = lMem - lTarget + 1;

        if (lTarget == SUBL) {
            base_size = lMem - lTarget + 1 + lTarget / 2;
        }

        // setup buffer for weighting

        System.arraycopy(weightState, 0, buf, 0, LPC_FILTERORDER);
        System.arraycopy(mem, memP, buf, LPC_FILTERORDER, lMem);
        System.arraycopy(intarget, intargetP, buf, LPC_FILTERORDER + lMem, lTarget);

        // weighting

        AllPoleFilter(buf, LPC_FILTERORDER, weightDenum, weightDenumP, lMem + lTarget, LPC_FILTERORDER);

        // Construct the codebook and target needed

        System.arraycopy(buf, LPC_FILTERORDER + lMem, target, 0, lTarget);

        tene = 0.0;

        for (i = 0; i < lTarget; i++) {
            tene += target[i] * target[i];
        }

        // Prepare search over one more codebook section. This section is
        // created by filtering the original buffer with a filter.

        filteredCBvecs(cbvectors, buf, LPC_FILTERORDER, lMem);

        // The Main Loop over stages

        for (stage = 0; stage < nStages; stage++) {

            range = search_rangeTbl[block][stage];

            // initialize search measure

            max_measure[0] = -10000000.0;
            gain[0] = 0.0;
            best_index[0] = 0;

            // Compute cross dot product between the target and the CB memory

            crossDot = 0.0;
            pp = LPC_FILTERORDER + lMem - lTarget; // buf
            for (j = 0; j < lTarget; j++) {
                crossDot += target[j] * (buf[pp++]);
            }

            if (stage == 0) {

                // Calculate energy in the first block of 'lTarget' samples.
                ppe = 0; // energy
                ppi = LPC_FILTERORDER + lMem - lTarget - 1; // buf
                ppo = LPC_FILTERORDER + lMem - 1; // buf

                energy[ppe] = 0.0;
                pp = LPC_FILTERORDER + lMem - lTarget; // buf
                for (j = 0; j < lTarget; j++) {
                    energy[ppe] += (buf[pp]) * (buf[pp++]);
                }

                if (energy[ppe] > 0.0) {
                    invenergy[0] = 1.0 / (energy[ppe] + EPS);
                } else {
                    invenergy[0] = 0.0;

                }
                ppe++;

                measure = -10000000.0;

                if (crossDot > 0.0) {
                    measure = crossDot * crossDot * invenergy[0];
                }
            } else {
                measure = crossDot * crossDot * invenergy[0];
            }

            // check if measure is better
            ftmp = crossDot * invenergy[0];

            if ((measure > max_measure[0]) && (Math.abs(ftmp) < CB_MAXGAIN)) {
                best_index[0] = 0;
                max_measure[0] = measure;
                gain[0] = ftmp;
            }

            // loop over the main first codebook section, full search

            for (icount = 1; icount < range; icount++) {

                // calculate measure

                crossDot = 0.0;
                pp = LPC_FILTERORDER + lMem - lTarget - icount; // buf

                for (j = 0; j < lTarget; j++) {
                    crossDot += target[j] * (buf[pp++]);
                }

                if (stage == 0) {
                    energy[ppe++] = energy[icount - 1] + (buf[ppi]) * (buf[ppi]) - (buf[ppo]) * (buf[ppo]);
                    ppo--;
                    ppi--;

                    if (energy[icount] > 0.0) {
                        invenergy[icount] = 1.0 / (energy[icount] + EPS);
                    } else {
                        invenergy[icount] = 0.0;
                    }

                    measure = -10000000.0;

                    if (crossDot > 0.0) {
                        measure = crossDot * crossDot * invenergy[icount];
                    }
                } else {
                    measure = crossDot * crossDot * invenergy[icount];
                }

                // check if measure is better
                ftmp = crossDot * invenergy[icount];

                if ((measure > max_measure[0]) && (Math.abs(ftmp) < CB_MAXGAIN)) {
                    best_index[0] = icount;
                    max_measure[0] = measure;
                    gain[0] = ftmp;
                }
            }

            // Loop over augmented part in the first codebook section, full
            // search. The vectors are interpolated.

            if (lTarget == SUBL) {

                // Search for best possible cb vector and compute the
                // CB-vectors' energy.
                searchAugmentedCB(20, 39, stage, base_size - lTarget / 2, target, buf, LPC_FILTERORDER + lMem, max_measure, best_index, gain, energy, invenergy);
            }

            // set search range for following codebook sections

            base_index = best_index[0];

            // unrestricted search

            if (CB_RESRANGE == -1) {
                sInd = 0;
                eInd = range - 1;
                sIndAug = 20;
                eIndAug = 39;
            }

            // restricted search around best index from first codebook section

            else {
                // Initialize search indices
                sIndAug = 0;
                eIndAug = 0;
                sInd = base_index - CB_RESRANGE / 2;
                eInd = sInd + CB_RESRANGE;

                if (lTarget == SUBL) {

                    if (sInd < 0) {

                        sIndAug = 40 + sInd;
                        eIndAug = 39;
                        sInd = 0;

                    } else if (base_index < (base_size - 20)) {

                        if (eInd > range) {
                            sInd -= (eInd - range);
                            eInd = range;
                        }
                    } else { // base_index >= (base_size-20)

                        if (sInd < (base_size - 20)) {
                            sIndAug = 20;
                            sInd = 0;
                            eInd = 0;
                            eIndAug = 19 + CB_RESRANGE;

                            if (eIndAug > 39) {
                                eInd = eIndAug - 39;
                                eIndAug = 39;
                            }
                        } else {
                            sIndAug = 20 + sInd - (base_size - 20);
                            eIndAug = 39;
                            sInd = 0;
                            eInd = CB_RESRANGE - (eIndAug - sIndAug + 1);
                        }
                    }

                } else { // lTarget = 22 or 23

                    if (sInd < 0) {
                        eInd -= sInd;

                        sInd = 0;
                    }

                    if (eInd > range) {
                        sInd -= (eInd - range);
                        eInd = range;
                    }
                }
            }

            // search of higher codebook section

            // index search range
            counter = sInd;
            sInd += base_size;
            eInd += base_size;

            if (stage == 0) {
                ppe = base_size; // energy
                energy[ppe] = 0.0;

                pp = lMem - lTarget; // cbvectors
                for (j = 0; j < lTarget; j++) {
                    energy[ppe] += (cbvectors[pp]) * (cbvectors[pp++]);
                }

                ppi = lMem - 1 - lTarget; // cbvectors
                ppo = lMem - 1; // cbvectors

                for (j = 0; j < (range - 1); j++) {
                    energy[ppe + 1] = energy[ppe] + (cbvectors[ppi]) * (cbvectors[ppi]) - (cbvectors[ppo]) * (cbvectors[ppo]);
                    ppo--;
                    ppi--;
                    ppe++;
                }
            }

            // loop over search range

            for (icount = sInd; icount < eInd; icount++) {

                // calculate measure

                crossDot = 0.0;
                pp = lMem - (counter++) - lTarget; // cbvectors

                for (j = 0; j < lTarget; j++) {

                    crossDot += target[j] * (cbvectors[pp++]);
                }

                if (energy[icount] > 0.0) {
                    invenergy[icount] = 1.0 / (energy[icount] + EPS);
                } else {
                    invenergy[icount] = 0.0;
                }

                if (stage == 0) {

                    measure = -10000000.0;

                    if (crossDot > 0.0) {
                        measure = crossDot * crossDot * invenergy[icount];
                    }
                } else {
                    measure = crossDot * crossDot * invenergy[icount];
                }

                // check if measure is better
                ftmp = crossDot * invenergy[icount];

                if ((measure > max_measure[0]) && (Math.abs(ftmp) < CB_MAXGAIN)) {
                    best_index[0] = icount;
                    max_measure[0] = measure;
                    gain[0] = ftmp;
                }
            }

            // Search the augmented CB inside the limited range.

            if ((lTarget == SUBL) && (sIndAug != 0)) {
                searchAugmentedCB(sIndAug, eIndAug, stage, 2 * base_size - 20, target, cbvectors, lMem, max_measure, best_index, gain, energy, invenergy);
            }

            // record best index

            index[indexP + stage] = best_index[0];

            // gain quantization

            if (stage == 0) {

                if (gain[0] < 0.0) {
                    gain[0] = 0.0;
                }

                if (gain[0] > CB_MAXGAIN) {
                    gain[0] = CB_MAXGAIN;
                }
                gain[0] = gainquant(gain[0], 1.0f, 32, gain_index, gain_indexP + stage);
            } else {
                if (stage == 1) {
                    gain[0] = gainquant(gain[0], Math.abs(gains[stage - 1]), 16, gain_index, gain_indexP + stage);
                } else {
                    gain[0] = gainquant(gain[0], Math.abs(gains[stage - 1]), 8, gain_index, gain_indexP + stage);
                }
            }

            // Extract the best (according to measure) codebook vector

            if (lTarget == (STATE_LEN - encoder.state_short_len)) {

                if (index[indexP + stage] < base_size) {
                    pp = LPC_FILTERORDER + lMem - lTarget - index[indexP + stage]; // buf
                    bufP = buf;
                } else {
                    pp = lMem - lTarget - index[indexP + stage] + base_size; // cbvectors
                    bufP = cbvectors;
                }
            } else {

                if (index[indexP + stage] < base_size) {
                    if (index[indexP + stage] < (base_size - 20)) {
                        pp = LPC_FILTERORDER + lMem - lTarget - index[indexP + stage]; // buf
                        bufP = buf;
                    } else {
                        createAugmentedVec(index[indexP + stage] - base_size + 40, buf, LPC_FILTERORDER + lMem, aug_vec);
                        pp = 0; // aug_vec
                        bufP = aug_vec;
                    }
                } else {
                    int filterno, position;

                    filterno = index[indexP + stage] / base_size;
                    position = index[indexP + stage] - filterno * base_size;

                    if (position < (base_size - 20)) {
                        pp = filterno * lMem - lTarget - index[indexP + stage] + filterno * base_size; // cbvectors
                        bufP = cbvectors;
                    } else {
                        createAugmentedVec(index[indexP + stage] - (filterno + 1) * base_size + 40, cbvectors, filterno * lMem, aug_vec);
                        pp = 0; // aug_vec
                        bufP = aug_vec;
                    }
                }
            }

            // Subtract the best codebook vector, according to measure, from the
            // target vector

            for (j = 0; j < lTarget; j++) {
                cvec[j] += gain[0] * (bufP[pp]);
                target[j] -= gain[0] * (bufP[pp++]);
            }

            // record quantized gain

            gains[stage] = gain[0];

        } // end of Main Loop. for (stage=0;...

        // Gain adjustment for energy matching
        cene = 0.0;
        for (i = 0; i < lTarget; i++) {
            cene += cvec[i] * cvec[i];
        }
        j = gain_index[gain_indexP];

        for (i = gain_index[gain_indexP]; i < 32; i++) {
            ftmp = cene * gain_sq5Tbl[i] * gain_sq5Tbl[i];

            if ((ftmp < (tene * gains[0] * gains[0])) && (gain_sq5Tbl[j] < (2.0 * gains[0]))) {
                j = i;
            }
        }
        gain_index[gain_indexP] = j;
    }

//#endregion

//#region A.36. LPCdecode.c

    /**
     * interpolation of lsf coefficients for the decoder
     *
     * @param a [o] lpc coefficients for a sub-frame
     * @param lsf1 first lsf coefficient vector
     * @param lsf2 second lsf coefficient vector
     * @param coef interpolation weight
     * @param length length of lsf vectors
     */
    private static void LSFinterpolate2a_dec(double[] a, double[] lsf1, double[] lsf2, int lsf2P, double coef, int length) {
        double[] lsftmp = new double[LPC_FILTERORDER];

        interpolate(lsftmp, lsf1, lsf2, lsf2P, coef, length);
        lsf2a(a, lsftmp);
    }

    /**
     * obtain dequantized lsf coefficients from quantization index
     *
     * @param lsfdeq [o] dequantized lsf coefficients
     * @param index quantization index
     * @param lpc_n number of LPCs
     */
    private static void SimplelsfDEQ(double[] lsfdeq, int[] index, int lpc_n) {

        // decode first LSF

        int pos = 0;
        int cb_pos = 0;
        for (int i = 0; i < LSF_NSPLIT; i++) {
            for (int j = 0; j < dim_lsfCbTbl[i]; j++) {
                lsfdeq[pos + j] = lsfCbTbl[cb_pos + index[i] * dim_lsfCbTbl[i] + j];
            }
            pos += dim_lsfCbTbl[i];
            cb_pos += size_lsfCbTbl[i] * dim_lsfCbTbl[i];
        }

        if (lpc_n > 1) {

            // decode last LSF

            pos = 0;
            cb_pos = 0;
            for (int i = 0; i < LSF_NSPLIT; i++) {
                for (int j = 0; j < dim_lsfCbTbl[i]; j++) {
                    lsfdeq[LPC_FILTERORDER + pos + j] = lsfCbTbl[cb_pos + (index[LSF_NSPLIT + i]) * dim_lsfCbTbl[i] + j];
                }
                pos += dim_lsfCbTbl[i];
                cb_pos += size_lsfCbTbl[i] * dim_lsfCbTbl[i];
            }
        }
    }

    /**
     * obtain synthesis and weighting filters form lsf coefficients
     *
     * @param syntdenum [o] synthesis filter coefficients
     * @param weightdenum [o] weighting denumerator coefficients
     * @param lsfdeq dequantized lsf coefficients
     * @param length length of lsf coefficient vector
     * @param iLBCdec_inst the decoder state structure
     */
    private static void DecoderInterpolateLSF(double[] syntdenum, double[] weightdenum, double[] lsfdeq, int length, Decoder iLBCdec_inst) {
        int pos, lp_length;
        double[] lp = new double[LPC_FILTERORDER + 1];
        int /* double* */ lsfdeq2;

        lsfdeq2 = length; // lsfdeq
        lp_length = length + 1;

        if (iLBCdec_inst.mode == 30) {
            // sub-frame 1: Interpolation between old and first

            LSFinterpolate2a_dec(lp, iLBCdec_inst.lsfdeqold, lsfdeq, 0, lsf_weightTbl_30ms[0], length);
            System.arraycopy(lp, 0, syntdenum, 0, lp_length);
            bwexpand(weightdenum, 0, lp, LPC_CHIRP_WEIGHTDENUM, lp_length);

            // sub-frames 2 to 6: interpolation between first and last LSF

            pos = lp_length;
            for (int i = 1; i < 6; i++) {
                LSFinterpolate2a_dec(lp, lsfdeq, lsfdeq, lsfdeq2, lsf_weightTbl_30ms[i], length);
                System.arraycopy(lp, 0, syntdenum, pos, lp_length);
                bwexpand(weightdenum, pos, lp, LPC_CHIRP_WEIGHTDENUM, lp_length);
                pos += lp_length;
            }
        } else {
            pos = 0;
            for (int i = 0; i < iLBCdec_inst.nsub; i++) {
                LSFinterpolate2a_dec(lp, iLBCdec_inst.lsfdeqold, lsfdeq, 0, lsf_weightTbl_20ms[i], length);
                System.arraycopy(lp, 0, syntdenum, pos, lp_length);
                bwexpand(weightdenum, pos, lp, LPC_CHIRP_WEIGHTDENUM, lp_length);
                pos += lp_length;
            }
        }

        // update memory

        if (iLBCdec_inst.mode == 30) {
            System.arraycopy(lsfdeq, lsfdeq2, iLBCdec_inst.lsfdeqold, 0, length);
        } else {
            System.arraycopy(lsfdeq, 0, iLBCdec_inst.lsfdeqold, 0, length);
        }
    }

//#endregion

//#region A.38. LPCencode.c

    /**
     * lpc analysis (subrutine to LPCencode)
     *
     * @param lsf [o] lsf coefficients
     * @param data new data vector
     * @param iLBCenc_inst (i/o) the encoder state structure
     */
    private static void SimpleAnalysis(double[] lsf, double[] data, Encoder iLBCenc_inst) {
        double[] temp = new double[BLOCKL_MAX], lp = new double[LPC_FILTERORDER + 1];
        double[] lp2 = new double[LPC_FILTERORDER + 1];
        double[] r = new double[LPC_FILTERORDER + 1];

        int is = LPC_LOOKBACK + BLOCKL_MAX - iLBCenc_inst.blockl;
        System.arraycopy(data, 0, iLBCenc_inst.lpc_buffer, is, iLBCenc_inst.blockl);

        // No lookahead, last window is asymmetric

        for (int k = 0; k < iLBCenc_inst.lpc_n; k++) {

            is = LPC_LOOKBACK;

            if (k < (iLBCenc_inst.lpc_n - 1)) {
                window(temp, lpc_winTbl, iLBCenc_inst.lpc_buffer, 0, BLOCKL_MAX);
            } else {
                window(temp, lpc_asymwinTbl, iLBCenc_inst.lpc_buffer, is, BLOCKL_MAX);
            }

            autocorr(r, temp, BLOCKL_MAX, LPC_FILTERORDER);
            window(r, r, lpc_lagwinTbl, 0, LPC_FILTERORDER + 1);

            levdurb(lp, temp, r, LPC_FILTERORDER);
            bwexpand(lp2, 0, lp, LPC_CHIRP_SYNTDENUM, LPC_FILTERORDER + 1);

            a2lsf(lsf, k * LPC_FILTERORDER, lp2);
        }
        is = LPC_LOOKBACK + BLOCKL_MAX - iLBCenc_inst.blockl;
        System.arraycopy(iLBCenc_inst.lpc_buffer, LPC_LOOKBACK + BLOCKL_MAX - is, iLBCenc_inst.lpc_buffer, 0, is);
    }

    /**
     * lsf interpolator and conversion from lsf to a coefficients (subrutine to
     * SimpleInterpolateLSF)
     *
     * @param a [o] lpc coefficients
     * @param lsf1 first set of lsf coefficients
     * @param lsf2 second set of lsf coefficients
     * @param coef weighting coefficient to use between lsf1 and lsf2
     * @param length length of coefficient vectors
     */
    private static void LSFinterpolate2a_enc(double[] a, double[] lsf1, double[] lsf2, int lsf2P, double coef, int length) {
        double[] lsftmp = new double[LPC_FILTERORDER];

        interpolate(lsftmp, lsf1, lsf2, lsf2P, coef, length);
        lsf2a(a, lsftmp);
    }

    /**
     * lsf interpolator (subrutine to LPCencode)
     *
     * @param syntdenum [o] the synthesis filter denominator resulting from the
     *            quantized interpolated lsf
     * @param weightdenum [o] the weighting filter denominator resulting from
     *            the unquantized interpolated lsf
     * @param lsf the unquantized lsf coefficients
     * @param lsfdeq the dequantized lsf coefficients
     * @param lsfold the unquantized lsf coefficients of the previous signal
     *            frame
     * @param lsfdeqold the dequantized lsf coefficients of the previous
     *            signal frame
     * @param length should equate LPC_FILTERORDER
     * @param iLBCenc_inst (i/o) the encoder state structure
     */
    private static void SimpleInterpolateLSF(double[] syntdenum, double[] weightdenum, double[] lsf, double[] lsfdeq, double[] lsfold, double[] lsfdeqold, int length, Encoder iLBCenc_inst) {
        int pos, lp_length;
        double[] lp = new double[LPC_FILTERORDER + 1];
        int /* double* */ lsf2, lsfdeq2;

        lsf2 = length; // lsf
        lsfdeq2 = length; // lsfdeq
        lp_length = length + 1;

        if (iLBCenc_inst.mode == 30) {
            // sub-frame 1: Interpolation between old and first set of lsf
            // coefficients

            LSFinterpolate2a_enc(lp, lsfdeqold, lsfdeq, 0, lsf_weightTbl_30ms[0], length);
            System.arraycopy(lp, 0, syntdenum, 0, lp_length);
            LSFinterpolate2a_enc(lp, lsfold, lsf, 0, lsf_weightTbl_30ms[0], length);
            bwexpand(weightdenum, 0, lp, LPC_CHIRP_WEIGHTDENUM, lp_length);

            // sub-frame 2 to 6: Interpolation between first and second set of
            // lsf coefficients

            pos = lp_length;
            for (int i = 1; i < iLBCenc_inst.nsub; i++) {
                LSFinterpolate2a_enc(lp, lsfdeq, lsfdeq, lsfdeq2, lsf_weightTbl_30ms[i], length);
                System.arraycopy(lp, 0, syntdenum, pos, lp_length);

                LSFinterpolate2a_enc(lp, lsf, lsf, lsf2, lsf_weightTbl_30ms[i], length);
                bwexpand(weightdenum, pos, lp, LPC_CHIRP_WEIGHTDENUM, lp_length);
                pos += lp_length;
            }
        } else {
            pos = 0;
            for (int i = 0; i < iLBCenc_inst.nsub; i++) {
                LSFinterpolate2a_enc(lp, lsfdeqold, lsfdeq, 0, lsf_weightTbl_20ms[i], length);
                System.arraycopy(lp, 0, syntdenum, pos, lp_length);
                LSFinterpolate2a_enc(lp, lsfold, lsf, 0, lsf_weightTbl_20ms[i], length);
                bwexpand(weightdenum, pos, lp, LPC_CHIRP_WEIGHTDENUM, lp_length);
                pos += lp_length;
            }
        }

        // update memory

        if (iLBCenc_inst.mode == 30) {
            System.arraycopy(lsf, lsf2, lsfold, 0, length);
            System.arraycopy(lsfdeq, lsfdeq2, lsfdeqold, 0, length);
        } else {
            System.arraycopy(lsf, 0, lsfold, 0, length);
            System.arraycopy(lsfdeq, 0, lsfdeqold, 0, length);
        }
    }

    /**
     * lsf quantizer (subrutine to LPCencode)
     *
     * @param lsfdeq [o] dequantized lsf coefficients (dimension FILTERORDER)
     * @param index [o] quantization index
     * @param lsf the lsf coefficient vector to be quantized (dimension
     *            FILTERORDER )
     * @param lpc_n number of lsf sets to quantize
     */
    private static void SimplelsfQ(double[] lsfdeq, int[] index, double[] lsf, int lpc_n) {
        // Quantize first LSF with memoryless split VQ
        SplitVQ(lsfdeq, 0, index, 0, lsf, 0, lsfCbTbl, LSF_NSPLIT, dim_lsfCbTbl, size_lsfCbTbl);

        if (lpc_n == 2) {
            // Quantize second LSF with memoryless split VQ
            SplitVQ(lsfdeq, LPC_FILTERORDER, index, LSF_NSPLIT, lsf, LPC_FILTERORDER, lsfCbTbl, LSF_NSPLIT, dim_lsfCbTbl, size_lsfCbTbl);
        }
    }

    /**
     * lpc encoder
     *
     * @param syntdenum (i/o) synthesis filter coefficients before/after
     *            encoding
     * @param weightdenum (i/o) weighting denumerator coefficients before/after
     *            encoding
     * @param lsf_index [o] lsf quantization index
     * @param data lsf coefficients to quantize
     * @param iLBCenc_inst (i/o) the encoder state structure
     */
    private static void LPCencode(double[] syntdenum, double[] weightdenum, int[] lsf_index, double[] data, Encoder iLBCenc_inst) {
        double[] lsf = new double[LPC_FILTERORDER * LPC_N_MAX];
        double[] lsfdeq = new double[LPC_FILTERORDER * LPC_N_MAX];
        int change = 0;

        SimpleAnalysis(lsf, data, iLBCenc_inst);
        SimplelsfQ(lsfdeq, lsf_index, lsf, iLBCenc_inst.lpc_n);
        change = LSF_check(lsfdeq, LPC_FILTERORDER, iLBCenc_inst.lpc_n);
        SimpleInterpolateLSF(syntdenum, weightdenum, lsf, lsfdeq, iLBCenc_inst.lsfold, iLBCenc_inst.lsfdeqold, LPC_FILTERORDER, iLBCenc_inst);
    }

//#endregion

//#region A.40. lsf.c

    /**
     * conversion from lpc coefficients to lsf coefficients
     *
     * @param freq [o] lsf coefficients
     * @param a lpc coefficients
     */
    private static void a2lsf(double[] freq, int freqP, double[] a) {
        double[] steps = { // LSF_NUMBER_OF_STEPS
            0.00635, 0.003175, 0.0015875, 0.00079375
        };
        double step;
        int step_idx;
        int lsp_index;
        double[] p = new double[LPC_HALFORDER];
        double[] q = new double[LPC_HALFORDER];
        double[] p_pre = new double[LPC_HALFORDER];
        double[] q_pre = new double[LPC_HALFORDER];
        final int old_p = 0, old_q = 1;
        int old;
        double[] olds = new double[2];
        double[] pq_coef;
        double omega, old_omega;
        double hlp, hlp1, hlp2, hlp3, hlp4, hlp5;

        for (int i = 0; i < LPC_HALFORDER; i++) {
            p[i] = -1.0 * (a[i + 1] + a[LPC_FILTERORDER - i]);
            q[i] = a[LPC_FILTERORDER - i] - a[i + 1];
        }

        p_pre[0] = -1.0 - p[0];
        p_pre[1] = -p_pre[0] - p[1];
        p_pre[2] = -p_pre[1] - p[2];
        p_pre[3] = -p_pre[2] - p[3];
        p_pre[4] = -p_pre[3] - p[4];
        p_pre[4] = p_pre[4] / 2;

        q_pre[0] = 1.0 - q[0];
        q_pre[1] = q_pre[0] - q[1];
        q_pre[2] = q_pre[1] - q[2];
        q_pre[3] = q_pre[2] - q[3];
        q_pre[4] = q_pre[3] - q[4];
        q_pre[4] = q_pre[4] / 2;

        omega = 0.0;
        old_omega = 0.0;

        olds[old_p] = FLOAT_MAX;
        olds[old_q] = FLOAT_MAX;

        // Here we loop through lsp_index to find all the LPC_FILTERORDER roots
        // for omega.

        for (lsp_index = 0; lsp_index < LPC_FILTERORDER; lsp_index++) {

            // Depending on lsp_index being even or odd, we alternatively solve
            // the roots for the two LSP equations.

            if ((lsp_index & 0x1) == 0) {
                pq_coef = p_pre;
                old = old_p;
            } else {
                pq_coef = q_pre;
                old = old_q;
            }

            // Start with low resolution grid

            for (step_idx = 0, step = steps[step_idx]; step_idx < LSF_NUMBER_OF_STEPS;) {

                // cos(10piw) + pq(0)cos(8piw) + pq(1)cos(6piw) + pq(2)cos(4piw) +
                // pq(3)cod(2piw) + pq(4)

                hlp = Math.cos(omega * TWO_PI);
                hlp1 = 2.0 * hlp + pq_coef[0];
                hlp2 = 2.0 * hlp * hlp1 - 1.0 + pq_coef[1];
                hlp3 = 2.0 * hlp * hlp2 - hlp1 + pq_coef[2];
                hlp4 = 2.0 * hlp * hlp3 - hlp2 + pq_coef[3];
                hlp5 = hlp * hlp4 - hlp3 + pq_coef[4];

                if (((hlp5 * (olds[old])) <= 0.0) || (omega >= 0.5)) {

                    if (step_idx == (LSF_NUMBER_OF_STEPS - 1)) {

                        if (Math.abs(hlp5) >= Math.abs(olds[old])) {
                            freq[freqP+lsp_index] = omega - step;
                        } else {
                            freq[freqP+lsp_index] = omega;
                        }

                        if ((olds[old]) >= 0.0) {
                            olds[old] = -1.0 * FLOAT_MAX;
                        } else {
                            olds[old] = FLOAT_MAX;
                        }

                        omega = old_omega;
                        step_idx = 0;

                        step_idx = LSF_NUMBER_OF_STEPS;
                    } else {

                        if (step_idx == 0) {
                            old_omega = omega;
                        }

                        step_idx++;
                        omega -= steps[step_idx];

                        // Go back one grid step

                        step = steps[step_idx];
                    }
                } else {

                    // increment omega until they are of different sign, and we
                    // know there is at least one root between omega and
                    // old_omega
                    olds[old] = hlp5;
                    omega += step;
                }
            }
        }

        for (int i = 0; i < LPC_FILTERORDER; i++) {
            freq[freqP+i] = freq[freqP+i] * TWO_PI;
        }
    }

    /**
     * conversion from lsf coefficients to lpc coefficients
     *
     * @param a_coef [o] lpc coefficients
     * @param freq lsf coefficients
     */
    private static void lsf2a(double[] a_coef, double[] freq) {
        double hlp;
        double[] p = new double[LPC_HALFORDER], q = new double[LPC_HALFORDER];
        double[] a = new double[LPC_HALFORDER + 1], a1 = new double[LPC_HALFORDER], a2 = new double[LPC_HALFORDER];
        double[] b = new double[LPC_HALFORDER + 1], b1 = new double[LPC_HALFORDER], b2 = new double[LPC_HALFORDER];

        for (int i = 0; i < LPC_FILTERORDER; i++) {
            freq[i] = freq[i] * PI2;
        }

        // Check input for ill-conditioned cases. This part is not found in the
        // TIA standard. It involves the following 2 IF blocks. If "freq" is
        // judged ill-conditioned, then we first modify freq[0] and
        // freq[LPC_HALFORDER-1] (normally LPC_HALFORDER = 10 for LPC
        // applications), then we adjust the other "freq" values slightly

        if ((freq[0] <= 0.0) || (freq[LPC_FILTERORDER - 1] >= 0.5)) {

            if (freq[0] <= 0.0) {
                freq[0] = 0.022;
            }

            if (freq[LPC_FILTERORDER - 1] >= 0.5) {
                freq[LPC_FILTERORDER - 1] = 0.499;
            }

            hlp = (freq[LPC_FILTERORDER - 1] - freq[0]) / (LPC_FILTERORDER - 1);

            for (int i = 1; i < LPC_FILTERORDER; i++) {
                freq[i] = freq[i - 1] + hlp;
            }
        }

        for (int xx = 0; xx < LPC_HALFORDER + 1; xx++) {
            a[xx] = 0;
        }
        for (int xx = 0; xx < LPC_HALFORDER + 1; xx++) {
            b[xx] = 0;
        }

        // p[i] and q[i] compute cos(2*pi*omega_{2j}) and cos(2*pi*omega_{2j-1}
        // in eqs. 4.2.2.2-1 and 4.2.2.2-2. Note that for this code p[i]
        // specifies the coefficients used in .Q_A(z) while q[i] specifies the
        // coefficients used in .P_A(z)

        for (int i = 0; i < LPC_HALFORDER; i++) {
            p[i] = Math.cos(TWO_PI * freq[2 * i]);
            q[i] = Math.cos(TWO_PI * freq[2 * i + 1]);
        }

        a[0] = 0.25;
        b[0] = 0.25;

        for (int i = 0; i < LPC_HALFORDER; i++) {
            a[i + 1] = a[i] - 2 * p[i] * a1[i] + a2[i];
            b[i + 1] = b[i] - 2 * q[i] * b1[i] + b2[i];
            a2[i] = a1[i];
            a1[i] = a[i];
            b2[i] = b1[i];
            b1[i] = b[i];
        }

        for (int j = 0; j < LPC_FILTERORDER; j++) {

            if (j == 0) {
                a[0] = 0.25;
                b[0] = -0.25;
            } else {
                a[0] = b[0] = 0.0;
            }

            for (int i = 0; i < LPC_HALFORDER; i++) {
                a[i + 1] = a[i] - 2 * p[i] * a1[i] + a2[i];
                b[i + 1] = b[i] - 2 * q[i] * b1[i] + b2[i];
                a2[i] = a1[i];
                a1[i] = a[i];
                b2[i] = b1[i];
                b1[i] = b[i];
            }

            a_coef[j + 1] = 2 * (a[LPC_HALFORDER] + b[LPC_HALFORDER]);
        }

        a_coef[0] = 1.0;
    }

//#endregion

//#region A.42. packing.c

    /**
     * splitting an integer into first most significant bits and remaining least
     * significant bits
     *
     * @param index the value to split
     * @param firstpart [o] the value specified by most significant bits
     * @param rest [o] the value specified by least significant bits
     * @param bitno_firstpart number of bits in most significant part
     * @param bitno_total number of bits in full range of value
     */
    private static void packsplit(int[] index, int indexP, int[] firstpart, int[] rest, int restP, int bitno_firstpart, int bitno_total) {
        int bitno_rest = bitno_total - bitno_firstpart;

        firstpart[0] = index[indexP] >>> bitno_rest;
        rest[restP] = index[indexP] - (firstpart[0] << bitno_rest);
    }

    /**
     * combining a value corresponding to msb's with a value corresponding to
     * lsb's
     *
     * @param index (i/o) the msb value in the combined value out
     * @param rest the lsb value
     * @param bitno_rest the number of bits in the lsb part
     */
    private static void packcombine(int[] index, int indexP, int rest, int bitno_rest) {
        index[indexP] = index[indexP] << bitno_rest;
        index[indexP] += rest;
    }

    /**
     * packing of bits into bitstream, i.e., vector of bytes
     *
     * @param bitstream (i/o) on entrance pointer to place in bitstream to pack
     *            new data, on exit pointer to place in bitstream to pack future
     *            data
     * @param index the value to pack
     * @param bitno the number of bits that the value will fit within
     * @param pos (i/o) write position in the current byte
     */
    private static void dopack(byte[] bitstream, int[] bP, int index, int bitno, int[] pos) {
        int posLeft;

        // Clear the bits before starting in a new byte
        if (pos[0] == 0) {
            bitstream[bP[0]] = 0;
        }

        while (bitno > 0) {

            // Jump to the next byte if end of this byte is reached

            if (pos[0] == 8) {
                pos[0] = 0;
                bP[0]++; // (*bitstream)++;
                bitstream[bP[0]] = 0;
            }

            posLeft = 8 - pos[0];

            // Insert index into the bitstream

            if (bitno <= posLeft) {
                bitstream[bP[0]] = (byte) (bitstream[bP[0]] | (byte) (index << (posLeft - bitno)));
                pos[0] += bitno;
                bitno = 0;
            } else {
                bitstream[bP[0]] = (byte) (bitstream[bP[0]] | (byte) (index >> (bitno - posLeft)));

                pos[0] = 8;
                index -= ((index >> (bitno - posLeft)) << (bitno - posLeft));

                bitno -= posLeft;
            }
        }
    }

    /**
     * unpacking of bits from bitstream, i.e., vector of bytes
     *
     * @param bitstream (i/o) on entrance pointer to place in bitstream to
     *            unpack new data from, on exit pointer to place in bitstream to
     *            unpack future data from
     * @param index [o] resulting value
     * @param bitno number of bits used to represent the value
     * @param pos (i/o) read position in the current byte
     */
    private static void unpack(byte[] bitstream, int[] bP, int[] index, int bitno, int[] pos) {
        int BitsLeft;

        index[0] = 0;
        while (bitno > 0) {

            // move forward in bitstream when the end of the byte is reached

            if (pos[0] == 8) {
                pos[0] = 0;
                bP[0]++; // (*bitstream)++;
            }

            BitsLeft = 8 - (pos[0]);

            // Extract bits to index

            if (BitsLeft >= bitno) {
                index[0] += ((((bitstream[bP[0]]) << (pos[0])) & 0xFF) >> (8 - bitno));

                pos[0] += bitno;
                bitno = 0;
            } else {

                if ((8 - bitno) > 0) {
                    index[0] += ((((bitstream[bP[0]]) << (pos[0])) & 0xFF) >> (8 - bitno));
                    pos[0] = 8;
                } else {
                    index[0] += ((((bitstream[bP[0]]) << (pos[0])) & 0xFF) << (bitno - 8));
                    pos[0] = 8;
                }
                bitno -= BitsLeft;
            }
        }
    }

//#endregion

//#region A.44. StateConstructW.c

    /**
     * decoding of the start state
     *
     * @param idxForMax 6-bit index for the quantization of max amplitude
     * @param idxVec vector of quantization indexes
     * @param syntDenum synthesis filter denumerator
     * @param out [o] the decoded state vector
     * @param len length of a state vector
     */
    private static void StateConstructW(int idxForMax, int[] idxVec, double[] syntDenum, int syntDenumP, double[] out, int outP, int len) {
        double maxVal;
        double[] tmpbuf = new double[LPC_FILTERORDER + 2 * STATE_LEN];
        int /* double* */ tmp;
        double[] numerator = new double[LPC_FILTERORDER + 1];
        double[] foutbuf = new double[LPC_FILTERORDER + 2 * STATE_LEN];
        int /* double* */ fout;

        // decoding of the maximum value

        maxVal = state_frgqTbl[idxForMax];
        maxVal = Math.pow(10, maxVal) / 4.5;

        // initialization of buffers and coefficients

        for (int xx = 0; xx < LPC_FILTERORDER; xx++) {
            tmpbuf[xx] = 0;
        }
        for (int xx = 0; xx < LPC_FILTERORDER; xx++) {
            foutbuf[xx] = 0;
        }
        for (int k = 0; k < LPC_FILTERORDER; k++) {
            numerator[k] = syntDenum[syntDenumP+LPC_FILTERORDER - k];
        }
        numerator[LPC_FILTERORDER] = syntDenum[syntDenumP+0];
        tmp = LPC_FILTERORDER; // tmpbuf
        fout = LPC_FILTERORDER; // foutbuf

        // decoding of the sample values

        for (int k = 0; k < len; k++) {
            int tmpi = len - 1 - k;
            // maxVal = 1 / scal
            tmpbuf[tmp + k] = maxVal * state_sq3Tbl[idxVec[tmpi]];
        }

        // circular convolution with all-pass filter

        for (int xx = 0; xx < len; xx++) {
            tmpbuf[tmp + len + xx] = 0;
        }
        ZeroPoleFilter(tmpbuf, tmp, numerator, syntDenum, syntDenumP, 2 * len, LPC_FILTERORDER, foutbuf, fout);
        for (int k = 0; k < len; k++) {
            out[outP+k] = foutbuf[fout + len - 1 - k] + foutbuf[fout + 2 * len - 1 - k];
        }
    }

//#endregion

//#region A.46. StateSearchW.c

    /**
     * predictive noise shaping encoding of scaled start state (subrutine for
     * StateSearchW)
     *
     * @param iLBCenc_inst Encoder instance
     * @param in vector to encode
     * @param syntDenum denominator of synthesis filter
     * @param weightDenum denominator of weighting filter
     * @param out [o] vector of quantizer indexes
     * @param len length of vector to encode and vector of quantizer indexes
     * @param state_first position of start state in the 80 vec
     */
    private static void AbsQuantW(Encoder iLBCenc_inst, double[] in, int inP, double[] syntDenum, int syntDenumP, double[] weightDenum, int weightDenumP, int[] out, int len, int state_first) {
        int /* double* */ syntOut;
        double[] syntOutBuf = new double[LPC_FILTERORDER + STATE_SHORT_LEN_30MS];
        double toQ;
        double[] xq = new double[1];
        int[] index = new int[1];

        // initialization of buffer for filtering

        for (int xx = 0; xx < LPC_FILTERORDER; xx++) {
            syntOutBuf[xx] = 0;
        }

        // initialization of pointer for filtering

        syntOut = LPC_FILTERORDER; // syntOutBuf

        // synthesis and weighting filters on input

        if (state_first != 0) {
            AllPoleFilter(in, inP, weightDenum, weightDenumP, SUBL, LPC_FILTERORDER);
        } else {
            AllPoleFilter(in, inP, weightDenum, weightDenumP, iLBCenc_inst.state_short_len - SUBL, LPC_FILTERORDER);
        }

        // encoding loop
        for (int n = 0; n < len; n++) {

            // time update of filter coefficients

            if ((state_first != 0) && (n == SUBL)) {
                syntDenumP += (LPC_FILTERORDER + 1);
                weightDenumP += (LPC_FILTERORDER + 1);

                // synthesis and weighting filters on input
                AllPoleFilter(in, inP + n, weightDenum, weightDenumP, len - n, LPC_FILTERORDER);

            } else if ((state_first == 0) && (n == (iLBCenc_inst.state_short_len - SUBL))) {
                syntDenumP += (LPC_FILTERORDER + 1);
                weightDenumP += (LPC_FILTERORDER + 1);

                // synthesis and weighting filters on input
                AllPoleFilter(in, inP + n, weightDenum, weightDenumP, len - n, LPC_FILTERORDER);
            }

            // prediction of synthesized and weighted input

            syntOutBuf[syntOut + n] = 0.0;
            AllPoleFilter(syntOutBuf, syntOut+n, weightDenum, weightDenumP, 1, LPC_FILTERORDER);

            // quantization

            toQ = in[inP + n] - syntOutBuf[syntOut + n];

            sort_sq(xq, index, toQ, state_sq3Tbl, 8);
            out[n] = index[0];
            syntOutBuf[syntOut + n] = state_sq3Tbl[out[n]];

            // update of the prediction filter

            AllPoleFilter(syntOutBuf, syntOut+n, weightDenum, weightDenumP, 1, LPC_FILTERORDER);
        }
    }

    /**
     * encoding of start state
     *
     * @param iLBCenc_inst Encoder instance
     * @param residual target residual vector
     * @param syntDenum lpc synthesis filter
     * @param weightDenum weighting filter denuminator
     * @param idxForMax [o] quantizer index for maximum amplitude
     * @param idxVec [o] vector of quantization indexes
     * @param len length of all vectors
     * @param state_first position of start state in the 80 vec
     */
    private static void StateSearchW(Encoder iLBCenc_inst, double[] residual, int residualP, double[] syntDenum, int syntDenumP, double[] weightDenum, int weightDenumP, int[] idxForMax, int[] idxVec, int len, int state_first) {
        double[] dtmp = new double[1];
        double maxVal;
        double[] tmpbuf = new double[LPC_FILTERORDER + 2 * STATE_SHORT_LEN_30MS];
        int /* double* */ tmp;
        double[] numerator = new double[1 + LPC_FILTERORDER];
        double[] foutbuf = new double[LPC_FILTERORDER + 2 * STATE_SHORT_LEN_30MS];
        int /* double* */ fout;
        int k;
        double qmax, scal;

        // initialization of buffers and filter coefficients

        for (int xx = 0; xx < LPC_FILTERORDER; xx++) {
            tmpbuf[xx] = 0;
        }
        for (int xx = 0; xx < LPC_FILTERORDER; xx++) {
            foutbuf[xx] = 0;
        }
        for (k = 0; k < LPC_FILTERORDER; k++) {
            numerator[k] = syntDenum[syntDenumP+LPC_FILTERORDER - k];
        }
        numerator[LPC_FILTERORDER] = syntDenum[syntDenumP+0];
        tmp = LPC_FILTERORDER; // tmpbuf
        fout = LPC_FILTERORDER; // foutbuf

        // circular convolution with the all-pass filter

        System.arraycopy(residual, residualP, tmpbuf, tmp, len);
        for (int xx = 0; xx < len; xx++) {
            tmpbuf[tmp + len + xx] = 0;
        }
        ZeroPoleFilter(tmpbuf, tmp, numerator, syntDenum, syntDenumP, 2 * len, LPC_FILTERORDER, foutbuf, fout);
        for (k = 0; k < len; k++) {
            foutbuf[fout + k] += foutbuf[fout + k + len];
        }

        // identification of the maximum amplitude value

        maxVal = foutbuf[fout + 0];
        for (k = 1; k < len; k++) {

            if (foutbuf[fout + k] * foutbuf[fout + k] > maxVal * maxVal) {
                maxVal = foutbuf[fout + k];
            }
        }
        maxVal = Math.abs(maxVal);

        // encoding of the maximum amplitude value

        if (maxVal < 10.0) {
            maxVal = 10.0;
        }
        maxVal = Math.log10(maxVal);
        sort_sq(dtmp, idxForMax, maxVal, state_frgqTbl, 64);

        // decoding of the maximum amplitude representation value, and
        // corresponding scaling of start state

        maxVal = state_frgqTbl[idxForMax[0]];
        qmax = Math.pow(10, maxVal);
        scal = (4.5) / qmax;
        for (k = 0; k < len; k++) {
            foutbuf[fout + k] *= scal;
        }

        // predictive noise shaping encoding of scaled start state

        AbsQuantW(iLBCenc_inst, foutbuf, fout, syntDenum, syntDenumP, weightDenum, weightDenumP, idxVec, len, state_first);
    }

//#endregion

//#region A.48. syntFilter.c

    /**
     * LP synthesis filter.
     *
     * @param out (i/o) Signal to be filtered
     * @param a LP parameters
     * @param len Length of signal
     * @param mem (i/o) Filter state
     */
    private static void syntFilter(double[] out, int oP, double[] a, int aP, int len, double[] mem) {
        int /* double* */ po, pi, pa, pm;

        po = oP; // out

        // Filter first part using memory from past

        for (int i = 0; i < LPC_FILTERORDER; i++) {
            pi = oP + i - 1; //
            pa = aP + 1; // a
            pm = LPC_FILTERORDER - 1; // mem
            for (int j = 1; j <= i; j++) {
                out[po] -= a[pa++] * out[pi--];
            }
            for (int j = i + 1; j < LPC_FILTERORDER + 1; j++) {
                out[po] -= a[pa++] * mem[pm--];
            }
            po++;
        }

        // Filter last part where the state is entirely in the output vector

        for (int i = LPC_FILTERORDER; i < len; i++) {
            pi = oP + i - 1; // out
            pa = aP + 1; // a
            for (int j = 1; j < LPC_FILTERORDER + 1; j++) {
                out[po] -= a[pa++] * out[pi--];
            }
            po++;
        }

        // Update state vector

        System.arraycopy(out, oP + len - LPC_FILTERORDER, mem, 0, LPC_FILTERORDER);
    }

//#endregion
}
