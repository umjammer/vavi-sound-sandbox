/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.ldcelp;


/**
 * LD-CELP G.728
 * <p>
 * Low-Delay Code Excitation Linear Prediction speech compression.
 * <p>
 * Code edited by Michael Concannon.<br>
 * Based on code written by Alex Zatsman, Analog Devices 1993
 * <p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 040621 nsano initial version <br>
 */
final class Constants {

    /** Adaptive Gain Control FACtor */
    static final float AGCFAC = 0.99f;
    static final float FAC = 253.0f / 256.0f;
    static final float FACGP = 29.0f / 32.0f;
    /** nverse if IDIM */
    static final float DIMINV = 0.2f;
    /** size of Speech Vector */
    static final int IDIM = 5;
    /** Gain (Logarithmic) Offset */
    static final int GOFF = 32;
    static final int KPDELTA = 6;
    /** Min Pitch Period ( 400 Hz) */
    static final int KPMIN = 20;
    /** Max Pitch Period (~ 57 Hz) */
    static final int KPMAX = 140;
    /** # of LPC Coeff. in Synthesys Filter */
    static final int LPC = 50;
    /** # of LPC Coeff. in Gain Predictor */
    static final int LPCLG = 10;
    /** # of LPC Coeff. in Weighting Filter */
    static final int LPCW = 10;
    /** Shape Codebook Size */
    static final int NCWD = 128;
    /** Frame Size */
    static final int NFRSZ = 20;

    /** Gain Codebook Size */
    static final int NG = 8;
    /** Size of Nonrecursive Part of Synth. Adapter */
    static final int NONR =  35;
    /** Size of Nonrecursive Part of Gain Adapter */
    static final int NONRLG = 20;
    /** Size of Nonrecursive Part of Weighting Filter */
    static final int NONRW =  30;
    /** Pitch Predictor Window Size */
    static final int NPWSZ = 100;
    /** Predictor Update Period */
    static final int NUPDATE = 4;
    static final float PPFTH = 0.6f;
    static final float PPFZCF = 0.15f;
    static final float SPFPCF = 0.75f;
    static final float SPFZCF = 0.65f;
    static final float TAPTH = 0.4f;
    static final float TILTF = 0.15f;
    static final float WNCF = 257.0f / 256.0f;
    static final float WPCF = 0.6f;
    static final float WZCF = 0.9f;

    static final float BIG = 10.e+30f;

    static final float MAX = 4095.0f;
    static final float MIN = -4095.0f;
}

/* */
