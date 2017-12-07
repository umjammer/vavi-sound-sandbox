/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.sox;

import vavi.util.Debug;


/**
 * Sound Tools rate change effect file.
 * <p>
 * Spiffy rate changer using Smith & Wesson Bandwidth-Limited Interpolation.
 * The algorithm is described in "Bandlimited Interpolation -
 * Introduction and Algorithm" by Julian O. Smith III.
 * Available on ccrma-ftp.stanford.edu as
 * pub/BandlimitedInterpolation.eps.Z or similar.
 * </p>
 * <p>
 * The latest stand alone version of this algorithm can be found
 * at <a href="ftp://ccrma-ftp.stanford.edu/pub/NeXT/">
 * ftp://ccrma-ftp.stanford.edu/pub/NeXT/</a>
 * under the name of resample-version.number.tar.Z
 * </p>
 * <p>
 * NOTE: There is a newer version of the resample routine then what
 * this file was originally based on.  Those adventurous might be
 * interested in reviewing its improvements and porting it to this
 * version.
 * </p>
 * <p>
 * TODO another idea for improvement...
 * note that upsampling usually doesn't require interpolation,
 * therefore is faster and more accurate than downsampling.
 * Downsampling by an integer factor is also simple, since
 * it just involves decimation if the input is already
 * lowpass-filtered to the output Nyquist frequency.
 * Get the idea? :) SJB: [11/25/99]
 * </p>
 * @author <a href="mailto:andreas@eakaw2.et.tu-dresden.de">Andreas Wilde</a>
 * @author Stan Brooks
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 12. Feb. 1999, Andreas Wilde Various changes, bugfixes(?), increased precision <br>
 *          October 29, 1999 Stan Brooks Fixed bug roll off frequency was wrong, too high by 2 when upsampling,
 *          too low by 2 when downsampling. <br>
 *          2006 nsano ported to java. <br>
 */
public class Resampler {

    private static final int LC = 7;
    private static final int NC = 1 << LC;
    private static final int LA = 16;
    private static final int NA = 1 << LA;
    private static final int LP = LC + LA;
    private static final int NP = 1 << LP;
    private static final int AMASK = NA - 1;
//  private static final int PMASK = NP - 1;

    private static final int MAXNWING = 80 << LC;

    private static final int ISCALE = 0x10000;

    /** largest factor for which exact-coefficients upsampling will be used */
    private static final int NQMAX = 511;

    private static final int ST_SAMPLE_MAX = 0x7fffffff;
    private static final int ST_SAMPLE_MIN = -ST_SAMPLE_MAX - 1;

    /** roll-off frequency */
    private double rollOff;
    /**
     * passband/stopband tuning magic
     * anything <= 2 means Nutall window
     */
    private double beta; 
    /** */
    private int nMult;

    //----

    /** */
    private class ResampleWork {
        /** factor = out / in sample rates */
        double factor;
        /** gcd-reduced input rates */
        float inRate;
        /** gcd-reduced output rates */
        float outRate;
        /** non-zero to use qprodUD quadratic interpolation */
        int quadr;
        /** */
        int nq;
        /** */
        int nWing;
        /** impulse [nWing+1] Filter coefficients */
        double[] imp;
        /** */
        int dhb;
        /** number of past/future samples needed by filter */
        int xh;
        /** xh plus some room for creep */
        int xOff;
        /** x[xp] is position to start filter application */
        int xp;
        /** x[xRead] is start-position to enter new samples */
        int xRead;
        /** Current time/pos in input sample */
        double time;
        /** I/O buffers */
        double[] x, y;
        /** Current time/pos for exact-coeff's method */
        int t;
    }

    /** */
    private ResampleWork work = new ResampleWork();

    /** */
    public Resampler(float inRate, float outRate) {
        // These defaults are conservative with respect to aliasing.
        this(inRate, outRate, 0.80f, 0, 45, 16);
    }

    /**
     * Prepare processing.
     * <pre>
     * -qs    quadr = 1
     * -q    rolloff = 0.875
     *        quadr = 1
     *        nMult = 75
     * -ql    rolloff = 0.94
     *        quadr = 1
     *        nMult = 149
     * </pre>
     *
     * @param inRate input rate
     * @param outRate output rate
     * @param rollOff default is 0.8
     * @param quadr default is 0
     * @param nMult default is 45
     * @param beta default is 16
     */
    public Resampler(float inRate,
                     float outRate,
                     float rollOff,
                     int quadr,
                     int nMult,
                     int beta) {

        
        work.quadr = quadr;
        this.nMult = nMult;
        
        if (rollOff <= 0.01 || rollOff >= 1.0) {
            throw new IllegalArgumentException("rolloff should be 0.01 ~ 1.0");
        } else {
            this.rollOff = rollOff;
        }
        
        if (beta <= 2.0) {
            this.beta = 0;
Debug.println("Nuttall window, cutoff " + rollOff);
        } else {
            this.beta = beta;
Debug.println("Kaiser window, cutoff " + rollOff + ", beta " + beta);
        }
        
        if (inRate == outRate) {
            throw new IllegalArgumentException("input, output rates are same");
        }
        
        work.factor = outRate / inRate;
Debug.println("r.factor: " + work.factor);
        
        float gcdRate = getGcdRate(inRate, outRate);
        work.inRate = inRate / gcdRate;
        work.outRate = outRate / gcdRate;
        
        if (work.inRate <= work.outRate && work.outRate <= NQMAX) {
            work.quadr = -1;                // exact coeff's
            work.nq = (int) work.outRate;   // max(inRate, outRate);
        } else {
            work.nq = NC;                   // for now
        }
        
        // nWing: # of filter coeffs in right wing
        work.nWing = work.nq * (this.nMult / 2 + 1) + 1;
        
        work.imp = new double[work.nWing + 2 + 1];
        // need Imp[-1] and Imp[Nwing] for quadratic interpolation
        // returns error # <=0, or adjusted wing-len > 0
        makeFilter(true);
        
Debug.println("nMult: " + nMult + ", nWing: " + work.nWing + ", nq: " + work.nq);
        
        if (work.quadr < 0) {               // exact coeff's method
            work.xh = (int) (work.nWing / work.outRate);
Debug.println("resample: rate ratio " + work.inRate + " : " + work.outRate + ", coeff interpolation not needed");
        } else {
            work.dhb = NP;                  // Fixed-point Filter sampling-time-increment
            if (work.factor < 1.0) {
                work.dhb = (int) (work.factor * NP + 0.5);
            }
            work.xh = (work.nWing << LA) / work.dhb;
            // (xh * dhb) >> LA is max index into imp[]
        }
        
        // reach of LP filter wings + some creeping room
        work.xOff = work.xh + 10;
        
        // Current "now"-sample pointer for input to filter
        work.xp = work.xOff;
        // Position in input array to read into
        work.xRead = work.xOff;
        // Current-time pointer for converter
        work.time = work.xOff;
        if (work.quadr < 0) { // exact coeff's method
            work.t = work.xOff * work.nq;
        }
    }

    /**
     * Gets gcd rate.
     * @param inRate
     * @param outRate
     * @return gcd rate
     */
    private final float getGcdRate(float inRate, float outRate) {
        if (outRate == 0) {
            return inRate;
        } else {
            return getGcdRate(outRate, inRate % outRate);
        }
    }

    /**
     * Processed signed long samples from ibuf to obuf.
     * @param iBuf input buffer
     * @return output buffer
     */
    public int[] resample(int[] iBuf) {

        int nOut;

        // constrain amount we actually process
Debug.println("xp: " + work.xp + ", xRead: " + work.xRead + ", iSamp: " + iBuf.length + ", xOff: " + work.xOff);

        int xSize = 2 * work.xOff + iBuf.length;
        int ySize = (int) (iBuf.length * work.factor);
        work.x = new double[xSize];
        work.y = new double[ySize];

        //

        int nProc = work.x.length - work.xp;
Debug.println("nProc 1: " + nProc + ", xSize: " + work.x.length + ", ySize: " + work.y.length);

        int nx = nProc - work.xRead; // space for right-wing future-data
        if (nx <= 0) {
            throw new IllegalStateException("Can not handle this sample rate change. nx not positive: " + nx);
        }
Debug.println("nx " + nx + ", nProc: " + nProc);

        int i;
        if (iBuf.length == 0) {
            for (i = work.xRead; i < nx + work.xRead; i++) {
                work.x[i] = 0;
            }
        } else {
            for (i = work.xRead; i < nx + work.xRead; i++) {
                work.x[i] = (double) iBuf[i - work.xRead] / ISCALE;
            }
        }
        int last = i;
        nProc = last - work.xOff - work.xp;

        if (nProc <= 0) {
            // fill in starting here next time
            work.xRead = last;
            // leave *isamp alone, we consumed it
            return new int[0];
        }
        if (work.quadr < 0) { // exact coeff's method
            nOut = srcEX(nProc);
Debug.println("nProc " + nProc + " --> " + nOut);
            // Move converter nProc samples back in time
            work.t -= nProc * work.outRate;
            // Advance by number of samples processed
            work.xp += nProc;
            // Calc time accumulation in Time
            int creep = (int) (work.t / work.outRate - work.xOff);
            if (creep != 0) {
                work.t -= creep * work.outRate; // Remove time accumulation
                work.xp += creep; // and add it to read pointer
Debug.println("nProc " + nProc + ", creep " + creep);
            }
        } else { // approx coeff's method
            nOut = srcUD(nProc);
Debug.println("nProc " + nProc + " --> " + nOut);
            // Move converter nProc samples back in time
            work.time -= nProc;
            // Advance by number of samples processed
            work.xp += nProc;
            // Calc time accumulation in Time
            int creep = (int) (work.time - work.xOff);
            if (creep != 0) {
                work.time -= creep; // Remove time accumulation
                work.xp += creep; // and add it to read pointer
Debug.println("nProc " + nProc + ", creep " + creep);
            }
        }

        // Copy back portion of input signal that must be re-used
        int k = work.xp - work.xOff;
Debug.println("k: " + k + ", last: " + last);
        for (i = 0; i < last - k; i++) {
            work.x[i] = work.x[i + k];
        }

        // Pos in input buff to read new data into
        work.xRead = i;
        work.xp = work.xOff;

        int[] oBuf = new int[nOut];
        for (i = 0; i < nOut; i++) {
            double ftemp = work.y[i] * ISCALE;

            if (ftemp >= ST_SAMPLE_MAX) {
                oBuf[i] = ST_SAMPLE_MAX;
            } else if (ftemp <= ST_SAMPLE_MIN) {
                oBuf[i] = ST_SAMPLE_MIN;
            } else {
                oBuf[i] = (int) ftemp;
            }
        }

        return oBuf;
    }

    /**
     * Process tail of input samples.
     * @return output 
     */
    public int[] drain() {

//Debug.println("Xoff %d, Xt %d  <--- DRAIN",this.Xoff, this.Xt);

        // stuff end with Xoff zeros
        int[] oBuf = resample(new int[work.xOff]);
Debug.println("DRAIN: " + work.xOff);
        return oBuf;
    }

    /**
     * quadratic interpolation
     * over 90% of CPU time spent in this iprodUD() function
     */
    private double qprodUD(double[] imp,
                           double[] xp,
                           int xp_pointer,
                           int inc,
                           double t0,
                           int dhb,
                           int ct) {

        final double f = 1.0f / (1 << LA);

        int ho = (int) (t0 * dhb);
        ho += (ct - 1) * dhb;           // so double sum starts with smallest coef's
        xp_pointer += (ct - 1) * inc;
        double v = 0;
        do {
            int hoh = ho >> LA;
            double coef = imp[hoh];

            double dm = coef - imp[hoh - 1];
            double dp = imp[hoh + 1] - coef;
            double t = (ho & AMASK) * f;
            coef += ((dp - dm) * t + (dp + dm)) * t * 0.5;

            // filter coef, lower La bits by quadratic interpolation
            v += coef * xp[xp_pointer]; // sum coeff * input sample
            xp_pointer -= inc;          // Input signal step. NO CHECK ON ARRAY BOUNDS
            ho -= dhb;                  // IR step
        } while (--ct != 0);

        return v;
    }

    /** linear interpolation */
    private final double iprodUD(double[] imp,
                                 double[] xp,
                                 int xp_pointer,
                                 int inc,
                                 double t0,
                                 int dhb,
                                 int ct) {

        final double f = 1.0f / (1 << LA);

        int ho = (int) (t0 * dhb);
        ho += (ct - 1) * dhb;           // so double sum starts with smallest coef's
        xp_pointer += (ct - 1) * inc;
        double v = 0;
        do {
            int hoh = ho >> LA;
            double coef = imp[hoh] + (imp[hoh + 1] - imp[hoh]) * (ho & AMASK) * f;
            // filter coef, lower La bits by linear interpolation
            v += coef * xp[xp_pointer]; // sum coeff * input sample
            xp_pointer -= inc;          // Input signal step. NO CHECK ON ARRAY BOUNDS
            ho -= dhb;                  // IR step
        } while (--ct != 0);

        return v;
    }

    /**
     * Sampling rate conversion subroutine
     * From resample: filters.c
     * @param nx
     * @return the number of output samples
     */
    private final int srcUD(int nx) {

        // quadratic or linear interp
        double time = work.time;
        // Output sampling period
        // Step through input signal
        double dt = 1.0f / work.factor;
// Debug.println("factor %f, dt %f, ", factor, dt);
// Debug.println("Time %f, ",this.Time);
        // (Xh * dhb) >> La is max index into imp[]
// Debug.println("ct=" + ct);
// Debug.println("ct=" + (double) this.nWing * NA / this.dhb + " " + this.Xh);
// Debug.println("ct=%ld, T=%.6f, dhb=%6f, dt=%.6f", this.Xh, time - Math.floor(time),(double) this.dhb / NA, dt);
        int y_pointer = 0;
        int n = (int) Math.ceil(nx / dt);
        while (n-- != 0) {
            double v;
            double t = time - Math.floor(time); // fractional part of time
            int xp_pointer = (int) time;        // Ptr to current input sample

            if (work.quadr != 0) {
                // Past inner product:
                // needs NP * nMult in 31 bits
                v = qprodUD(work.imp, work.x, xp_pointer, -1, t, work.dhb, work.xh);
                // Future inner product:
                // prefer even total
                v += qprodUD(work.imp, work.x, xp_pointer + 1, 1, 1.0f - t, work.dhb, work.xh);
            } else {
                v = iprodUD(work.imp, work.x, xp_pointer, -1, t, work.dhb, work.xh);
                v += iprodUD(work.imp, work.x, xp_pointer + 1, 1, 1.0f - t, work.dhb, work.xh);
            }

            if (work.factor < 1) {
                v *= work.factor;
            }
            work.y[y_pointer++] = v;            // Deposit output
            time += dt;                         // Move to next sample by time increment
        }
        work.time = time;
//Debug.println("time " + this.time);
        return y_pointer;                       // Return the number of output samples
    }

    /**
     * exact coeff's
     *
     * @param imp
     * @param xp
     * @param xp_pointer
     * @param inc
     * @param t0
     * @param dhb
     * @param ct
     * @return ???
     */
    private final double prodEX(double[] imp,
                               double[] xp,
                               int xp_pointer,
                               int inc,
                               int t0,
                               int dhb,
                               int ct) {

        // so double sum starts with smallest coef's
        int cp_pointer = (ct - 1) * dhb + t0;
        xp_pointer += (ct - 1) * inc;
        double v = 0;
        do {
            v += imp[cp_pointer] * xp[xp_pointer];  // sum coeff * input sample
            cp_pointer -= dhb;                      // IR step
            xp_pointer -= inc;                      // Input signal step.
        } while (--ct != 0);

        return v;
    }

    /**
     * @param nx 
     * @return the number of output samples
     */
    private final int srcEX(int nx) {

        int time = work.t;
        int a = (int) work.inRate;
        int b = (int) work.outRate;
        int yStart = 0;
        int y = 0;
        int n = (nx * b + (a - 1)) / a;
        while (n-- != 0) {
            int t = time % b;               // fractional part of time
            int xp_pointer = time / b;      // Ptr to current input sample

            // Past inner product:
            double v = prodEX(work.imp, work.x, xp_pointer, -1, t, b, work.xh);
            // Future inner product:
            v += prodEX(work.imp, work.x, xp_pointer + 1, 1, b - t, b, work.xh);

            if (work.factor < 1) {
                v *= work.factor;
            }
            work.y[y++] = v;                   // Deposit output
            time += a;                      // Move to next sample by time increment
        }
        work.t = time;

        return y - yStart;                  // Return the number of output samples
    }

    /**
     * @throw IllegalArgumentException
     */
    private final void makeFilter(boolean normalize) {

        if (work.nWing > MAXNWING) {             // Check for valid parameters
            throw new IllegalArgumentException("nWing > " + MAXNWING);
        }
        if (rollOff <= 0 || rollOff > 1) {
            throw new IllegalArgumentException("rollOff: " + rollOff);
        }

        // it does help accuracy a bit to have the window stop at
        // a zero-crossing of the sinc function
        int mWing = (int) (Math.floor(work.nWing / (work.nq / rollOff)) * (work.nq / rollOff) + 0.5);
        if (mWing == 0) {
            throw new IllegalArgumentException("mWing is 0");
        }

        double[] impR = lpFilter(mWing);

        // Design a Nuttall or Kaiser windowed Sinc low-pass filter

        if (normalize) {                    // 'correct' the DC gain of the lowpass filter
            double dcGain = 0;
            int dh = work.nq;                    // Filter sampling period for factors >= 1
            for (int i = dh; i < mWing; i += dh) {
                dcGain += impR[i];
            }
            dcGain = 2 * dcGain + impR[0];  // DC gain of real coefficients
// Debug.println("dCgain err=%.12f", dCgain - 1.0);

            dcGain = 1.0f / dcGain;
            for (int i = 0; i < mWing; i++) {
                work.imp[i] = impR[i] * dcGain;
            }
        } else {
            for (int i = 0; i < mWing; i++) {
                work.imp[i] = impR[i];
            }
        }

        for (int i = mWing; i <= work.nWing; i++) {
            work.imp[i] = 0;
        }
        // imp[mWing] and imp[-1] needed for quadratic interpolation
//      imp[-1] = imp[1]; // TODO -1 ???
    }

    /*
     * reference: "Digital Filters, 2nd edition"
     * R.W. Hamming, pp. 178-179
     */

    /** Max error acceptable in iZero */
    private static final double iZeroEPSILON = 1e-21f;

    /**
     * Computes the 0th order modified bessel function of the first kind.
     * (Needed to compute Kaiser window).
     */
    private final double iZero(double x) {
        
        double sum = 1;
        double u = 1;
        double n = 1;
        double halfx = x / 2.0f;
        do {
            double temp = halfx / n;
            n += 1;
            temp *= temp;
            u *= temp;
            sum += u;
        } while (u >= iZeroEPSILON * sum);
        
        return sum;
    }

    /**
     * Computes the coeffs of a Kaiser-windowed low pass filter with
     * the following characteristics:
     * <p>
     * beta trades the rejection of the lowpass filter against the transition
     *    width from passband to stopband.  Larger beta means a slower
     *    transition and greater stopband rejection.  See Rabiner and Gold
     *    (Theory and Application of DSP) under Kaiser windows for more about
     *    beta.  The following table from Rabiner and Gold gives some feel
     *    for the effect of beta:
     * </p>
     * <p>
     * All ripples in dB, width of transition band = D * N
     * where N = window length
     * </p>
     * <pre>
     *           BETA    D       PB RIP   SB RIP
     *           2.120   1.50  +-0.27      -30
     *           3.384   2.23    0.0864    -40
     *           4.538   2.93    0.0274    -50
     *           5.658   3.62    0.00868   -60
     *           6.764   4.32    0.00275   -70
     *           7.865   5.0     0.000868  -80
     *           8.960   5.7     0.000275  -90
     *           10.056  6.4     0.000087  -100
     * </pre>
     *
     * @param mWing half the window length in number of coeffs
     * @return array in which to store computed coeffs
     */
    private final double[] lpFilter(int mWing) {

        double[] c = new double[mWing];

        // Calculate filter coeffs:
        c[0] = rollOff;
        for (int i = 1; i < mWing; i++) {
            double x = Math.PI * i / work.nq;
            c[i] = Math.sin(x * rollOff) / x;
        }

        if (beta > 2) {
            // Apply Kaiser window to filter coeffs:
            double iBeta = 1.0f / iZero(beta);
            for (int i = 1; i < mWing; i++) {
                double x = (double) i / mWing;
                c[i] *= iZero(beta * Math.sqrt(1.0 - x * x)) * iBeta;
            }
        } else {
            // Apply Nuttall window:
            for (int i = 0; i < mWing; i++) {
                double x = Math.PI * i / mWing;
                c[i] *= 0.36335819 + 0.4891775 * Math.cos(x) + 0.1365995 * Math.cos(2 * x) + 0.0106411 * Math.cos(3 * x);
            }
        }

        return c;
    }
}

/* */
